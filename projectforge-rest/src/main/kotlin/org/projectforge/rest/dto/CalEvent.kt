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

package org.projectforge.rest.dto

import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import net.fortuna.ical4j.model.property.RRule
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.RecurrenceMonthMode
import org.projectforge.business.teamcal.event.TeamEventRecurrenceData
import org.projectforge.business.teamcal.event.model.*
import org.projectforge.framework.calendar.ICal4JUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.RecurrenceFrequency
import java.sql.Timestamp
import java.util.*
import javax.persistence.Transient

class CalEvent(
        var seriesModificationMode: SeriesModificationMode? = null,
        /**
         * The selected event of a series (if any).
         */
        var selectedSeriesEvent: ICalendarEvent? = null,
        override var subject: String? = null,
        override var location: String? = null,
        override var allDay: Boolean = false,
        override var startDate: Timestamp? = null,
        override var endDate: Timestamp? = null,
        var lastEmail: Timestamp? = null,
        var dtStamp: Timestamp? = null,
        var calendar: TeamCalDO? = null,
        var recurrenceRule: String? = null,
        var recurrenceExDate: String? = null,
        var recurrenceReferenceDate: String? = null,
        var recurrenceReferenceId: String? = null,
        var recurrenceUntil: Date? = null,
        override var note: String? = null,
        var attendees: MutableSet<TeamEventAttendeeDO>? = null,
        var ownership: Boolean? = null,
        var organizer: String? = null,
        var organizerAdditionalParams: String? = null,
        var sequence: Int? = 0,
        override var uid: String? = null,
        var reminderDuration: Int? = null,
        var reminderDurationUnit: ReminderDurationUnit? = null,
        var reminderActionType: ReminderActionType? = null,
        var attachments: MutableSet<TeamEventAttachmentDO>? = null,
        var creator: PFUserDO? = null) : BaseDTO<CalEventDO>(), ICalendarEvent {

    val hasRecurrence: Boolean
        get() = !recurrenceRule.isNullOrBlank()

    /**
     * Adds a new ExDate to this event.
     *
     * @param date
     * @return this for chaining.
     */
    fun addRecurrenceExDate(date: Date?): CalEvent {
        if (date == null) {
            return this
        }
        val exDate: String = ICal4JUtils.asICalDateString(date, DateHelper.UTC, allDay)
        if (recurrenceExDate == null || recurrenceExDate!!.isEmpty()) {
            recurrenceExDate = exDate
        } else if (!recurrenceExDate!!.contains(exDate)) {
            // Add this ExDate only if not yet added:
            recurrenceExDate = "$recurrenceExDate,$exDate"
        }
        return this
    }

    fun setRecurrence(rRule: RRule?): CalEvent {
        if (rRule == null || rRule.recur == null) {
            this.recurrenceRule = null
            this.recurrenceUntil = null

            return this
        }

        val recur = rRule.recur

        if (recur.until != null) {
            this.recurrenceUntil = recur.until
        } else {
            this.recurrenceUntil = null
        }

        this.recurrenceRule = rRule.value

        return this
    }

    /**
     * @param recurrenceData
     * @return this for chaining.
     */
    @Transient
    fun setRecurrence(recurrenceData: TeamEventRecurrenceData?): CalEvent {
        if (recurrenceData == null || recurrenceData.frequency == null || recurrenceData.frequency == RecurrenceFrequency.NONE) {
            this.recurrenceRule = null
            this.recurrenceUntil = null

            return this
        }

        if (!recurrenceData.isCustomized) {
            recurrenceData.interval = 1
        }

        val recur = Recur()
        recur.interval = recurrenceData.interval
        recur.frequency = ICal4JUtils.getCal4JFrequencyString(recurrenceData.frequency)

        if (recurrenceData.frequency == RecurrenceFrequency.WEEKLY) {
            val weekdays = recurrenceData.weekdays
            for (i in 0..6) {
                if (weekdays[i]) {
                    when (i) {
                        0 -> recur.dayList.add(WeekDay.MO)
                        1 -> recur.dayList.add(WeekDay.TU)
                        2 -> recur.dayList.add(WeekDay.WE)
                        3 -> recur.dayList.add(WeekDay.TH)
                        4 -> recur.dayList.add(WeekDay.FR)
                        5 -> recur.dayList.add(WeekDay.SA)
                        6 -> recur.dayList.add(WeekDay.SU)
                    }
                }
            }
        } else if (recurrenceData.frequency == RecurrenceFrequency.MONTHLY) {
            if (recurrenceData.monthMode == RecurrenceMonthMode.EACH) {
                val monthdays = recurrenceData.monthdays
                for (i in 0..30) {
                    if (monthdays[i]) {
                        recur.monthDayList.add(i + 1)
                    }
                }
            } else if (recurrenceData.monthMode == RecurrenceMonthMode.ATTHE) {
                val offset = ICal4JUtils.getOffsetForRecurrenceFrequencyModeOne(recurrenceData.modeOneMonth)
                val weekDays = ICal4JUtils.getDayListForRecurrenceFrequencyModeTwo(recurrenceData.modeTwoMonth)
                for (weekDay in weekDays) {
                    recur.dayList.add(WeekDay(weekDay, offset))
                }
            }
        } else if (recurrenceData.frequency == RecurrenceFrequency.YEARLY) {
            val months = recurrenceData.months
            for (i in 0..11) {
                if (months[i]) {
                    recur.monthList.add(i + 1)
                }
            }
            if (recurrenceData.isYearMode) {
                val offset = ICal4JUtils.getOffsetForRecurrenceFrequencyModeOne(recurrenceData.modeOneYear)
                val weekDays = ICal4JUtils.getDayListForRecurrenceFrequencyModeTwo(recurrenceData.modeTwoYear)
                for (weekDay in weekDays) {
                    recur.dayList.add(WeekDay(weekDay, offset))
                }
            }
        }
        // Set until
        if (recurrenceData.until != null) {
            if (this.allDay) {
                // just use date, no time
                val untilICal4J = net.fortuna.ical4j.model.Date(recurrenceData.until)
                recur.until = untilICal4J
                this.recurrenceUntil = recurrenceData.until
            } else {
                this.recurrenceUntil = this.fixUntilInRecur(recur, recurrenceData.until, recurrenceData.timeZone)
            }
        } else {
            this.recurrenceUntil = null
        }

        val rrule = RRule(recur)

        this.recurrenceRule = rrule.value

        return this
    }

    private fun fixUntilInRecur(recur: Recur, until: Date, timezone: TimeZone): Date {
        // until in RecurrenceData is always in UTC!
        val calUntil = Calendar.getInstance(DateHelper.UTC)
        val calStart = Calendar.getInstance(timezone)

        calUntil.time = until
        //    calStart.setTime(this.startDate);

        // update date of start date to until date
        calStart.set(Calendar.YEAR, calUntil.get(Calendar.YEAR))
        calStart.set(Calendar.DAY_OF_YEAR, calUntil.get(Calendar.DAY_OF_YEAR))

        // set until to last limit of day in user time
        calStart.set(Calendar.HOUR_OF_DAY, 23)
        calStart.set(Calendar.MINUTE, 59)
        calStart.set(Calendar.SECOND, 59)
        calStart.set(Calendar.MILLISECOND, 0)

        // update recur until
        val untilICal4J = DateTime(calStart.time)
        untilICal4J.isUtc = true
        recur.until = untilICal4J

        // return new until date for DB usage
        return calStart.time
    }

    @Transient
    fun mustIncSequence(other: CalEvent): Boolean {
        if (allDay != other.allDay) {
            return true
        }

        if (endDate == null) {
            if (other.endDate != null) {
                return true
            }
        } else if (!endDate!!.equals(other.endDate)) {
            return true
        }
        if (location == null) {
            if (other.location != null) {
                return true
            }
        } else if (location != other.location) {
            return true
        }
        if (note == null) {
            if (other.note != null) {
                return true
            }
        } else if (note != other.note) {
            return true
        }
        if (recurrenceExDate == null) {
            if (other.recurrenceExDate != null) {
                return true
            }
        } else if (recurrenceExDate != other.recurrenceExDate) {
            return true
        }
        if (recurrenceRule == null) {
            if (other.recurrenceRule != null) {
                return true
            }
        } else if (recurrenceRule != other.recurrenceRule) {
            return true
        }
        if (recurrenceUntil == null) {
            if (other.recurrenceUntil != null) {
                return true
            }
        } else if (recurrenceUntil != other.recurrenceUntil) {
            return true
        }
        if (organizer == null) {
            if (other.organizer != null) {
                return true
            }
        } else if (organizer != other.organizer) {
            return true
        }
        if (organizerAdditionalParams == null) {
            if (other.organizerAdditionalParams != null) {
                return true
            }
        } else if (organizerAdditionalParams != other.organizerAdditionalParams) {
            return true
        }
        if (startDate == null) {
            if (other.startDate != null) {
                return true
            }
        } else if (!startDate!!.equals(other.startDate)) {
            return true
        }
        if (subject == null) {
            if (other.subject != null) {
                return true
            }
        } else if (subject != other.subject) {
            return true
        }
        if (attendees == null || attendees!!.isEmpty()) {
            if (other.attendees != null && other.attendees!!.isNotEmpty()) {
                return true
            }
        } else if (attendees != other.attendees) {
            return true
        }
        if (attachments == null || attachments!!.isEmpty()) {
            if (other.attachments != null && other.attachments!!.isNotEmpty()) {
                return true
            }
        } else if (attachments != other.attachments) {
            return true
        }

        return false
    }

    @Transient
    fun getRecurrenceData(timezone: TimeZone): TeamEventRecurrenceData {
        val recurrenceData = TeamEventRecurrenceData(timezone)
        return recurrenceData
        //TODO No recurrenceObject available
        /*val recur = this.recurrenceObject?: return recurrenceData

        recurrenceData.interval = if (recur.interval == -1) 1 else recur.interval

        if (this.recurrenceUntil != null) {
            // transform until to timezone
            if (this.allDay) {
                recurrenceData.until = this.recurrenceUntil
            } else {
                // determine last possible event in event time zone (owner time zone)
                val calUntil = Calendar.getInstance(this.timeZone)
                val calStart = Calendar.getInstance(this.timeZone)

                calUntil.time = this.recurrenceUntil!!
                calStart.time = this.startDate!!

                calStart.set(Calendar.YEAR, calUntil.get(Calendar.YEAR))
                calStart.set(Calendar.DAY_OF_YEAR, calUntil.get(Calendar.DAY_OF_YEAR))

                if (calStart.after(calUntil)) {
                    calStart.add(Calendar.DAY_OF_YEAR, -1)
                }

                // move to target time zone and transform to UTC
                val calTimeZone = Calendar.getInstance(timezone)
                val calUTC = Calendar.getInstance(DateHelper.UTC)

                calTimeZone.time = calStart.time

                calUTC.set(Calendar.YEAR, calTimeZone.get(Calendar.YEAR))
                calUTC.set(Calendar.DAY_OF_YEAR, calTimeZone.get(Calendar.DAY_OF_YEAR))
                calUTC.set(Calendar.HOUR_OF_DAY, 0)
                calUTC.set(Calendar.MINUTE, 0)
                calUTC.set(Calendar.SECOND, 0)
                calUTC.set(Calendar.MILLISECOND, 0)

                recurrenceData.until = calUTC.time
            }
        }
        recurrenceData.frequency = ICal4JUtils.getFrequency(recur)

        if (recurrenceData.frequency == RecurrenceFrequency.WEEKLY) {
            val weekdays = recurrenceData.weekdays
            for (wd in recur.dayList) {
                recurrenceData.isCustomized = true
                when {
                    wd.day == WeekDay.MO.day -> weekdays[0] = true
                    wd.day == WeekDay.TU.day -> weekdays[1] = true
                    wd.day == WeekDay.WE.day -> weekdays[2] = true
                    wd.day == WeekDay.TH.day -> weekdays[3] = true
                    wd.day == WeekDay.FR.day -> weekdays[4] = true
                    wd.day == WeekDay.SA.day -> weekdays[5] = true
                    wd.day == WeekDay.SU.day -> weekdays[6] = true
                }
            }
            recurrenceData.weekdays = weekdays
        }
        if (recurrenceData.frequency == RecurrenceFrequency.MONTHLY) {
            recurrenceData.monthMode = RecurrenceMonthMode.NONE
            val monthdays = recurrenceData.monthdays
            for (day in recur.monthDayList) {
                recurrenceData.isCustomized = true
                recurrenceData.monthMode = RecurrenceMonthMode.EACH
                monthdays[day!! - 1] = true
            }
            recurrenceData.monthdays = monthdays

            var offset = 0
            if (recur.dayList.size == 1) {
                offset = recur.dayList[0].offset
            } else if (recur.dayList.size > 1 && recur.setPosList.size != 0) {
                offset = recur.setPosList[0]
            }
            if (recur.dayList.size != 0) {
                recurrenceData.isCustomized = true
                recurrenceData.monthMode = RecurrenceMonthMode.ATTHE
                recurrenceData.modeOneMonth = ICal4JUtils.getRecurrenceFrequencyModeOneByOffset(offset)
                recurrenceData.modeTwoMonth = ICal4JUtils.getRecurrenceFrequencyModeTwoForDay(recur.dayList)
            }
        }
        if (recurrenceData.frequency == RecurrenceFrequency.YEARLY) {
            val months = recurrenceData.months
            for (day in recur.monthList) {
                recurrenceData.isCustomized = true

                months[day!! - 1] = true
            }
            recurrenceData.months = months

            var offset = 0
            if (recur.dayList.size == 1) {
                offset = recur.dayList[0].offset
            } else if (recur.dayList.size > 1 && recur.setPosList.size != 0) {
                offset = recur.setPosList[0]
            }
            if (recur.dayList.size != 0) {
                recurrenceData.isCustomized = true
                recurrenceData.isYearMode = true
                recurrenceData.modeOneYear = ICal4JUtils.getRecurrenceFrequencyModeOneByOffset(offset)
                recurrenceData.modeTwoYear = ICal4JUtils.getRecurrenceFrequencyModeTwoForDay(recur.dayList)
            }
        }
        return recurrenceData*/
    }

    fun ensureAttendees(): MutableSet<TeamEventAttendeeDO> {
        if (this.attendees == null) {
            this.attendees = HashSet()
        }
        return this.attendees!!
    }

    fun addAttendee(attendee: TeamEventAttendeeDO): CalEvent {
        ensureAttendees()
        var number: Short = 1
        for (pos in attendees!!) {
            if (pos.number!! >= number) {
                number = pos.number!!
                number++
            }
        }
        attendee.number = number
        this.attendees!!.add(attendee)
        return this
    }
}
