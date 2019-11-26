/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.calendar

import net.fortuna.ical4j.model.property.RRule
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.business.teamcal.TeamCalConfig
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.CalEventDao
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.model.CalEventDO
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.calendar.CalendarUtils
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.dto.CalEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.*
import javax.persistence.NoResultException
import javax.persistence.NonUniqueResultException

@Repository
open class CalEventDaoCopy : BaseDao<CalEventDO>(CalEventDO::class.java) {

    @Autowired
    private val emgrFac: PfEmgrFactory? = null

    @Autowired
    private val tenantServ: TenantService? = null

    private val icsConverter: ICSConverter = ICSConverter()

    init {
        userRightId = UserRightId.CALENDAR_EVENT
    }

    fun getByUid(calendarId: Int?, uid: String): CalEventDO? {
        return this.getByUid(calendarId, uid, true)
    }

    fun getByUid(calendarId: Int?, uid: String?, excludeDeleted: Boolean): CalEventDO? {
        if (uid == null) {
            return null
        }

        val sqlQuery = StringBuilder()
        val params = ArrayList<Any>()

        sqlQuery.append("select e from CalEventDO e where e.uid = :uid AND e.tenant = :tenant")

        params.add("uid")
        params.add(uid)
        params.add("tenant")
        params.add(if (ThreadLocalUserContext.getUser() != null) ThreadLocalUserContext.getUser()!!.tenant else tenantServ!!.defaultTenant)

        if (excludeDeleted) {
            sqlQuery.append(" AND e.deleted = :deleted")
            params.add("deleted")
            params.add(false)
        }

        // workaround to still handle old requests
        if (calendarId != null) {
            sqlQuery.append(" AND e.calendar.id = :calendarId")
            params.add("calendarId")
            params.add(calendarId)
        }

        try {
            return emgrFac!!.runRoTrans { emgr -> emgr.selectSingleAttached(CalEventDO::class.java, sqlQuery.toString(), *params.toTypedArray()) }
        } catch (e: NoResultException) {
            return null
        } catch (e: NonUniqueResultException) {
            return null
        }

    }

    override fun onChange(obj: CalEventDO, dbObj: CalEventDO) {
        handleSeriesUpdates(obj)
        // only increment sequence if PF has ownership!

        val calendarEvent = icsConverter.convertToCalEvent(obj)
        val calendarEventdb = icsConverter.convertToCalEvent(dbObj)

        if (calendarEvent.ownership != null && calendarEvent.ownership == false) {
            return
        }

        // compute diff
        if (calendarEvent.mustIncSequence(calendarEventdb)) {
            if (calendarEvent.sequence == null) {
                calendarEvent.sequence = 0
            } else {
                calendarEvent.sequence = calendarEvent.sequence!! + 1
            }

            if (calendarEvent.dtStamp == null || calendarEvent.dtStamp!!.equals(calendarEventdb.dtStamp)) {
                calendarEvent.dtStamp = Timestamp(System.currentTimeMillis())
            }
        }

        // TODO: Does this work?
        obj.icsData = icsConverter.convertToDO(calendarEvent).icsData
    }

    private fun getUntilDate(untilUTC: Date): Date {
        // move one day to past, the TeamEventDO will post process this value while setting
        return Date(untilUTC.time - 24 * 60 * 60 * 1000)
    }

    /**
     * Handles updates of series element (if any) for future and single events of a series.
     *
     * @param event
     */
    private fun handleSeriesUpdates(event: CalEventDO) {
        val selectedEvent = event.removeTransientAttribute(ATTR_SELECTED_ELEMENT) as ICalendarEvent // Must be removed, otherwise save below will handle this attrs again.
        val mode = event.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE) as SeriesModificationMode
        if (selectedEvent == null || mode == null || mode === SeriesModificationMode.ALL) {
            // Nothing to do.
            return
        }

        var newEvent = icsConverter.convertToCalEvent(event)
        newEvent.sequence = 0
        val masterEvent = getById(event.id)
        event.copyValuesFrom(masterEvent) // Restore db fields of master event. Do only modify single or future events.
        val masterCalEvent = icsConverter.convertToCalEvent(masterEvent)
        if (mode == SeriesModificationMode.FUTURE) {
            val recurrenceData = masterCalEvent.getRecurrenceData(ThreadLocalUserContext.getTimeZone())
            // Set the end date of the master date one day before current date and save this event.
            recurrenceData.until = getUntilDate(selectedEvent.startDate!!)
            newEvent.setRecurrence(recurrenceData)
            if (log.isDebugEnabled) {
                log.debug("Recurrence until date of master entry will be set to: " + DateHelper.formatAsUTC(recurrenceData.until))
                log.debug("The new event is: $newEvent")
            }
        } else if (mode == SeriesModificationMode.SINGLE) { // only current date
            // Add current date to the master date as exclusion date and save this event (without recurrence settings).
            newEvent.addRecurrenceExDate(selectedEvent.startDate)
            if (newEvent.hasRecurrence) {
                log.warn("User tries to modifiy single event of a series, the given recurrence is ignored.")
            }
            newEvent.setRecurrence(RRule()) // User only wants to modify single event, ignore recurrence.
            if (log.isDebugEnabled) {
                log.debug("Recurrency ex date of master entry is now added: "
                        + DateHelper.formatAsUTC(selectedEvent.startDate)
                        + ". The new string is: "
                        + newEvent.recurrenceExDate)
                log.debug("The new event is: $newEvent")
            }
        }

        // TODO: Does this work?
        save(icsConverter.convertToDO(newEvent))
    }

    /**
     * Handles deletion of series element (if any) for future and single events of a series.
     */
    override fun internalMarkAsDeleted(obj: CalEventDO) {
        val selectedEvent = obj.removeTransientAttribute(ATTR_SELECTED_ELEMENT) as ICalendarEvent // Must be removed, otherwise update below will handle this attrs again.
        val mode = obj.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE) as SeriesModificationMode
        if (selectedEvent == null || mode == null || mode === SeriesModificationMode.ALL) {
            // Nothing to do special:
            super.internalMarkAsDeleted(obj)
            return
        }
        val masterEvent = getById(obj.id)
        obj.copyValuesFrom(masterEvent) // Restore db fields of master event. Do only modify single or future events.

        val calendarEvent = icsConverter.convertToCalEvent(obj)

        if (mode === SeriesModificationMode.FUTURE) {
            val recurrenceData = calendarEvent.getRecurrenceData (ThreadLocalUserContext.getTimeZone());
            val recurrenceUntil = getUntilDate (selectedEvent.startDate!!)
            recurrenceData.until = recurrenceUntil
            calendarEvent.setRecurrence(recurrenceData)
        } else if (mode === SeriesModificationMode.SINGLE) { // only current date
            Validate.notNull(selectedEvent)
            calendarEvent.addRecurrenceExDate(selectedEvent.startDate)
        }

        //TODO
        obj.icsData = icsConverter.convertToDO(calendarEvent).icsData
        update(obj)
    }

    override fun newInstance(): CalEventDO? {
        return null
    }

    /**
     * This method also returns recurrence events outside the time period of the given filter but affecting the
     * time-period (e. g. older recurrence events without end date or end date inside or after the given time period). If
     * calculateRecurrenceEvents is true, only the recurrence events inside the given time-period are returned, if false
     * only the origin recurrence event (may-be outside the given time-period) is returned.
     *
     * @param filter
     * @param calculateRecurrenceEvents If true, recurrence events inside the given time-period are calculated.
     * @return list of team events (same as [.getList] but with all calculated and matching
     * recurrence events (if calculateRecurrenceEvents is true). Origin events are of type [TeamEventDO],
     * calculated events of type [ICalendarEvent].
     */
    fun getEventList(filter: TeamEventFilter, calculateRecurrenceEvents: Boolean): List<ICalendarEvent> {
        val result = ArrayList<ICalendarEvent>()
        var list: List<CalEventDO>? = getList(filter)
        if (CollectionUtils.isNotEmpty(list)) {
            for (eventDO in list!!) {
                result.add(eventDO)
            }
        }
        val teamEventFilter = filter.clone().setOnlyRecurrence(true)
        val qFilter = buildQueryFilter(teamEventFilter)
        list = getList(qFilter)
        list = selectUnique(list)
        val timeZone = ThreadLocalUserContext.getTimeZone()
        if (list != null) {
            for (eventDO in list) {
                if (!calculateRecurrenceEvents) {
                    result.add(eventDO)
                    continue
                }
            }
        }
        return result
    }

    /**
     * Sets midnight (UTC) of all day events.
     */
    override fun onSaveOrModify(event: CalEventDO) {
        super.onSaveOrModify(event)
        Validate.notNull<TeamCalDO>(event.calendar)

        if (event.allDay) {
            if (event.endDate!!.time < event.startDate!!.time) {
                throw UserException("plugins.teamcal.event.duration.error") // "Duration of time sheet must be at minimum 60s!
            }
        } else if (event.endDate!!.time - event.startDate!!.time < 60000) {
            throw UserException("plugins.teamcal.event.duration.error") // "Duration of time sheet must be at minimum 60s!
            // Or, end date is before start date.
        }

        // If is all day event, set start and stop to midnight
        if (event.allDay) {
            val startDate = event.startDate
            if (startDate != null) {
                event.setStartDate(CalendarUtils.getUTCMidnightTimestamp(startDate))
            }
            val endDate = event.startDate
            if (endDate != null) {
                event.setEndDate(CalendarUtils.getUTCMidnightTimestamp(endDate))
            }
        }
    }

    override fun onSave(event: CalEventDO) {
        // create uid if empty
        if (StringUtils.isBlank(event.uid)) {
            event.setUid(TeamCalConfig.get().createEventUid())
        }
    }

    private fun buildQueryFilter(filter: TeamEventFilter): QueryFilter {
        val queryFilter = QueryFilter(filter)
        val cals = filter.teamCals
        if (CollectionUtils.isNotEmpty(cals)) {
            queryFilter.add(QueryFilter.isIn<Any>("calendar.id", cals))
        } else if (filter.teamCalId != null) {
            queryFilter.add(QueryFilter.eq("calendar.id", filter.teamCalId!!))
        }
        // Following period extension is needed due to all day events which are stored in UTC. The additional events in the result list not
        // matching the time period have to be removed by caller!
        var startDate: Date? = filter.startDate
        if (startDate != null) {
            startDate = Date(startDate.time - ONE_DAY)
        }
        var endDate: Date? = filter.endDate
        if (endDate != null) {
            endDate = Date(endDate.time + ONE_DAY)
        }
        // limit events to load to chosen date view.
        if (startDate != null && endDate != null) {
            queryFilter.add(QueryFilter.or(
                    QueryFilter.or(QueryFilter.between("startDate", startDate, endDate),
                            QueryFilter.between("endDate", startDate, endDate)),
                    // get events whose duration overlap with chosen duration.
                    QueryFilter.and(QueryFilter.le("startDate", startDate), QueryFilter.ge("endDate", endDate))))

        } else if (endDate != null) {
            queryFilter.add(QueryFilter.le("startDate", endDate))
        }
        queryFilter.addOrder(SortProperty.desc("startDate"))
        return queryFilter
    }

    companion object {
        /**
         * For storing the selected element of the series in the transient attribute map for correct handling in [.onDelete]
         * and [.onSaveOrModify] of series (all, future, selected).
         */
        const val ATTR_SELECTED_ELEMENT = "selectedSeriesElement"

        /**
         * For series elements: what to modify in [.onDelete] and [.onSaveOrModify] of series (all, future, selected)?
         */
        const val ATTR_SERIES_MODIFICATION_MODE = "seriesModificationMode"

        private val log = org.slf4j.LoggerFactory.getLogger(CalEventDaoCopy::class.java)

        private const val ONE_DAY = (1000 * 60 * 60 * 24).toLong()
    }

}
