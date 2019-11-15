package org.projectforge.rest.dto

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.mockito.Mockito.mock
import org.projectforge.business.teamcal.event.model.CalEventDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.calendar.ICSParser
import org.projectforge.rest.calendar.ICalConverterStore
import org.projectforge.rest.calendar.ICalGenerator
import java.util.*

class CalEventTest {

    @Test
    fun dtoTest(){
        val calEventDO = CalEventDO()
        calEventDO.icsData =
                "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "PRODID:http://www.example.com/calendarapplication/\n" +
                "METHOD:PUBLISH\n" +
                "BEGIN:VEVENT\n" +
                "UID:461092315540@example.com\n" +
                "ORGANIZER;CN=\"Alice Balder, Example Inc.\":MAILTO:alice@example.com\n" +
                "LOCATION:Irgendwo\n" +
                "GEO:48.85299;2.36885\n" +
                "SUMMARY:Eine Kurzinfo\n" +
                "DESCRIPTION:Beschreibung des Termines\n" +
                "CLASS:PUBLIC\n" +
                "DTSTART:20060910T220000Z\n" +
                "DTEND:20060919T215900Z\n" +
                "DTSTAMP:20060812T125900Z\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR"

        var calEvent = CalEvent()
        val parser = ICSParser.parseAllFields()
        parser.parse(calEventDO.icsData)

        assertNotNull(parser.extractedEvents)

        if(parser.extractedEvents!!.isNotEmpty()){
            calEvent = parser.extractedEvents!![0]
        }

        assertEquals(calEvent.subject, "Eine Kurzinfo")
        assertEquals(calEvent.location, "Irgendwo")
        assertEquals(calEvent.note, "Beschreibung des Termines")
        assertEquals(calEvent.organizer, "MAILTO:alice@example.com")
        assertEquals(calEvent.organizerAdditionalParams, "CN=\"Alice Balder, Example Inc.\"")

        var generator = mock(ICalGenerator::class.java)
        generator.exportsVEvent = ArrayList(ICalConverterStore.FULL_LIST)
        generator.setContext(PFUserDO(), TimeZone.getDefault(), Locale.GERMAN)
        generator = generator.reset()
        generator.addEvent(calEvent)
        generator.calendarAsByteStream.toString()

    }
}