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

package org.projectforge.rest.calendar;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.CalEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.ical.ICalHandler;
import org.projectforge.business.teamcal.event.model.*;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.model.rest.CalendarEventObject;
import org.projectforge.rest.dto.CalEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CalEventServiceCopy
{
  @Autowired
  private AddressDao addressDao;

  @Autowired
  private CalEventDaoCopy calEventDao;

  @Autowired
  private TeamCalCache teamCalCache;

  @Autowired
  private TeamEventAttendeeDao teamEventAttendeeDao;

  @Autowired
  private UserService userService;

  private ICSConverter icsConverter = new ICSConverter();

  public CalEventDaoCopy getTeamEventICSDao(){
    return calEventDao;
  }

  public void saveOrUpdate(CalEventDO teamEvent) {
    calEventDao.saveOrUpdate(teamEvent);
  }

  public void markAsDeleted(CalEventDO teamEvent) {
    calEventDao.markAsDeleted(teamEvent);
  }

  public void undelete(CalEventDO teamEvent) {
    calEventDao.undelete(teamEvent);
  }

  public void save(CalEventDO teamEvent) {
    calEventDao.save(teamEvent);
  }

  public void update(CalEventDO teamEvent) {
    calEventDao.update(teamEvent);
  }

  public void update(CalEventDO teamEvent, boolean checkAccess) {
    calEventDao.internalUpdate(teamEvent, checkAccess);
  }

  public boolean checkAndSendMail(final CalEventDO event, final TeamEventDiffType diffType) { return false; }

  public boolean checkAndSendMail(final CalEventDO eventNew, final CalEventDO eventOld) { return false; }

  public CalEventDO findByUid(Integer calendarId, String reqEventUid, boolean excludeDeleted) {
    return calEventDao.getByUid(calendarId, reqEventUid, excludeDeleted);
  }

  public List<CalEventDO> getTeamEventICSDOList(TeamEventFilter filter) {
    return calEventDao.getList(filter);
  }

  public CalEventDO getTeamEventICSDO (CalendarEventObject calendarEventObject) {
    CalEventDO calEventDO = new CalEventDO();
    calEventDO.setCalendar(teamCalCache.getCalendar(calendarEventObject.getCalendarId()));
    calEventDO.setStartDate((Timestamp) calendarEventObject.getStartDate());
    calEventDO.setEndDate((Timestamp) calendarEventObject.getEndDate());
    calEventDO.setLocation(calendarEventObject.getLocation());
    calEventDO.setNote(calendarEventObject.getNote());
    calEventDO.setSubject(calendarEventObject.getSubject());
    calEventDO.setUid(calendarEventObject.getUid());
    calEventDO.setIcsData(calendarEventObject.getIcsData());
    return calEventDO;
  }

  public ICSHandler getEventHandler(final TeamCalDO defaultCalendar)
  {
    return new ICSHandler(this, defaultCalendar);
  }

  public List<ICalendarEvent> getEventList(TeamEventFilter filter, boolean calculateRecurrenceEvents)
  {
    return calEventDao.getEventList(filter, calculateRecurrenceEvents);
  }

  public List<TeamEventAttendeeDO> getAddressesAndUserAsAttendee()
  {
    List<TeamEventAttendeeDO> resultList = new ArrayList<>();
    List<AddressDO> allAddressList = addressDao.internalLoadAllNotDeleted();
    List<PFUserDO> allUserList = userService.getAllActiveUsers();
    Set<Integer> addedUserIds = new HashSet<>();
    for (AddressDO singleAddress : allAddressList) {
      if (!StringUtils.isBlank(singleAddress.getEmail())) {
        TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
        attendee.setStatus(TeamEventAttendeeStatus.IN_PROCESS);
        attendee.setAddress(singleAddress);
        PFUserDO userWithSameMail = allUserList.stream()
            .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().equals(singleAddress.getEmail().toLowerCase())).findFirst().orElse(null);
        if (userWithSameMail != null && !addedUserIds.contains(userWithSameMail.getId())) {
          attendee.setUser(userWithSameMail);
          addedUserIds.add(userWithSameMail.getId());
        }
        resultList.add(attendee);
      }
    }
    for (PFUserDO u : allUserList) {
      if (!addedUserIds.contains(u.getId())) {
        TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
        attendee.setStatus(TeamEventAttendeeStatus.IN_PROCESS);
        attendee.setUser(u);
        resultList.add(attendee);
      }
    }
    return resultList;
  }

  public void assignAttendees(CalEventDO data, Set<TeamEventAttendeeDO> itemsToAssign, Set<TeamEventAttendeeDO> itemsToUnassign)
  {
    CalEvent dataEvent = icsConverter.convertToCalEvent(data);
    for (TeamEventAttendeeDO assignAttendee : itemsToAssign) {
      if (assignAttendee.getId() == null || assignAttendee.getId() < 0) {
        assignAttendee.setId(null);
        if (assignAttendee.getStatus() == null) {
          assignAttendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
        }
        dataEvent.addAttendee(assignAttendee);
        teamEventAttendeeDao.internalSave(assignAttendee);
      }
    }

    if (dataEvent.getAttendees() != null && itemsToUnassign != null && itemsToUnassign.size() > 0) {
      dataEvent.getAttendees().removeAll(itemsToUnassign);
      for (TeamEventAttendeeDO deleteAttendee : itemsToUnassign) {
        teamEventAttendeeDao.internalMarkAsDeleted(deleteAttendee);
      }
    }

    data.setIcsData(icsConverter.convertToDO(dataEvent).getIcsData());

    calEventDao.update(data);
  }

  public void updateAttendees(CalEvent event, Set<TeamEventAttendeeDO> attendeesOldState)
  {
    final Set<TeamEventAttendeeDO> attendeesNewState = event.getAttendees();

    // new list is empty -> delete all
    if (attendeesNewState == null || attendeesNewState.isEmpty()) {
      if (attendeesOldState != null && !attendeesOldState.isEmpty()) {
        for (TeamEventAttendeeDO attendee : attendeesOldState) {
          teamEventAttendeeDao.internalMarkAsDeleted(attendee);
        }
      }

      return;
    }

    // old list is empty -> insert all
    if (attendeesOldState == null || attendeesOldState.isEmpty()) {
      for (TeamEventAttendeeDO attendee : attendeesNewState) {
        // save new attendee
        attendee.setId(null);
        if (attendee.getStatus() == null) {
          attendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
        }

        teamEventAttendeeDao.internalSave(attendee);
      }

      return;
    }

    // compute diff
    for (TeamEventAttendeeDO attendee : attendeesNewState) {
      boolean found = false;
      String eMail = attendee.getAddress() != null ? attendee.getAddress().getEmail() : attendee.getUrl();

      if (eMail == null) {
        // should not occur
        continue;
      }

      for (TeamEventAttendeeDO attendeeOld : attendeesOldState) {
        String eMailOld = attendeeOld.getAddress() != null ? attendeeOld.getAddress().getEmail() : attendeeOld.getUrl();

        if (eMail.equals(eMailOld)) {
          found = true;

          // update values
          attendee.setPk(attendeeOld.getPk());
          attendee.setComment(attendeeOld.getComment());
          attendee.setCommentOfAttendee(attendeeOld.getCommentOfAttendee());
          attendee.setLoginToken(attendeeOld.getLoginToken());
          attendee.setNumber(attendeeOld.getNumber());
          attendee.setAddress(attendeeOld.getAddress());
          attendee.setUser(attendeeOld.getUser());

          teamEventAttendeeDao.internalSave(attendee);

          break;
        }
      }

      if (!found) {
        // save new attendee
        attendee.setId(null);
        if (attendee.getStatus() == null) {
          attendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
        }
        teamEventAttendeeDao.internalSave(attendee);
      }
    }

    for (TeamEventAttendeeDO attendee : attendeesOldState) {
      boolean found = false;
      String eMail = attendee.getAddress() != null ? attendee.getAddress().getEmail() : attendee.getUrl();

      for (TeamEventAttendeeDO attendeeNew : attendeesNewState) {
        String eMailNew = attendeeNew.getAddress() != null ? attendeeNew.getAddress().getEmail() : attendeeNew.getUrl();

        if (eMail.equals(eMailNew)) {
          found = true;
          break;
        }
      }

      if (!found) {
        // delete attendee
        teamEventAttendeeDao.internalMarkAsDeleted(attendee);
      }
    }
  }
}
