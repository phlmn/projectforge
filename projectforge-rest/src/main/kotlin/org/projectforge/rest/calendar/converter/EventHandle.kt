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

package org.projectforge.rest.calendar.converter

import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.ical.EventHandleError
import org.projectforge.business.teamcal.event.ical.HandleMethod
import org.projectforge.business.teamcal.event.model.CalEventDO

import java.util.ArrayList

open class EventHandle(var event: CalEventDO?, var calendar: TeamCalDO?, var method: HandleMethod?) {
    var eventInDB: CalEventDO? = null
    var isProcess: Boolean = false

    private var errors: MutableList<EventHandleError>? = null
    private var warnings: MutableList<EventHandleError>? = null

    init {
        this.isProcess = false
        this.errors = ArrayList()
        this.warnings = ArrayList()
    }

    fun isValid(ignoreWarnings: Boolean): Boolean {
        return this.errors!!.isEmpty() && (ignoreWarnings || this.warnings!!.isEmpty())
    }

    fun addError(error: EventHandleError) {
        this.errors!!.add(error)
    }

    fun getErrors(): List<EventHandleError>? {
        return errors
    }

    fun setError(errors: MutableList<EventHandleError>) {
        this.errors = errors
    }

    fun addWarning(warnings: EventHandleError) {
        this.warnings!!.add(warnings)
    }

    fun getWarnings(): List<EventHandleError>? {
        return warnings
    }

    fun setWarnings(warnings: MutableList<EventHandleError>) {
        this.warnings = warnings
    }
}
