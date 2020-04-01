/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Projekt
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/project")
class ProjektPagesRest
    : AbstractDTOPagesRest<ProjektDO, Projekt, ProjektDao>(
        ProjektDao::class.java,
        "fibu.projekt.title") {

    override fun transformFromDB(obj: ProjektDO, editMode: Boolean): Projekt {
        val projekt = Projekt(null, obj.displayName)
        projekt.copyFrom(obj)
        projekt.kost = obj.kost
        projekt.nummernkreis = obj.nummernkreis
        projekt.bereich = obj.bereich
        projekt.kunde?.copyFrom(obj.kunde!!)
        return projekt
    }

    override fun transformForDB(dto: Projekt): ProjektDO {
        val projektDO = ProjektDO()
        dto.copyTo(projektDO)
        return projektDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "kost", "identifier", "kunde.name", "name", "kunde.division", "task", "konto", "status", "projektManagerGroup", "internKost2_4", "description"))
        layout.getTableColumnById("konto").formatter = Formatter.KONTO
        layout.getTableColumnById("task").formatter = Formatter.TASK_PATH
        layout.getTableColumnById("projektManagerGroup").formatter = Formatter.GROUP
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Projekt, userAccess: UILayout.UserAccess): UILayout {
        val konto = UIInput("konto", lc, tooltip = "fibu.projekt.konto.tooltip")

        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(UICustomized("cost.number24"))
                                .add(UISelect.createCustomerSelect(lc, "kunde", false, "fibu.kunde"))
                                .add(konto)
                                .add(lc, "name", "identifier", "task")
                                .add(UISelect.createGroupSelect(lc, "projektManagerGroup", false, "fibu.projekt.projektManagerGroup"))
                                .add(lc, "projectManager", "headOfBusinessManager", "description")
                                .add(UILabel("TODO: Kost 2 Types"))))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
