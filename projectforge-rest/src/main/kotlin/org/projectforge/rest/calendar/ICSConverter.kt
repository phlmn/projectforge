package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.event.CalEventDao
import org.projectforge.business.teamcal.event.model.CalEventDO
import org.projectforge.rest.dto.CalEvent

open class ICSConverter {

    fun convertToCalEvent(obj: CalEventDO): CalEvent {
        var calendarEvent = CalEvent()
        val parser = ICSParser()
        if (obj.icsData != null) {
            parser.parse(obj.icsData)
            if (parser.extractedEvents!!.isNotEmpty()) {
                calendarEvent = parser.extractedEvents!![0]
            }
        }

        return calendarEvent
    }

    fun convertToDO(dto: CalEvent): CalEventDO {
        val calendarEventDO = CalEventDO()

        dto.copyTo(calendarEventDO)

        val generator = ICSGenerator()
        generator.addEvent(dto)
        calendarEventDO.icsData = generator.calendarAsByteStream.toString()

        if (dto.selectedSeriesEvent != null) {
            calendarEventDO.setTransientAttribute(CalEventDao.ATTR_SELECTED_ELEMENT, dto.selectedSeriesEvent)
            calendarEventDO.setTransientAttribute(CalEventDao.ATTR_SERIES_MODIFICATION_MODE, dto.seriesModificationMode)
        }
        return calendarEventDO
    }
}