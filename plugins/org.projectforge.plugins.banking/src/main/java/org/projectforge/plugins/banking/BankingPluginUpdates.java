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

package org.projectforge.plugins.banking;

import org.projectforge.continuousdb.*;
import org.projectforge.framework.persistence.database.DatabaseService;

/**
 * Contains the initial data-base set-up script and later all update scripts if any data-base schema updates are
 * required by any later release of this to-do plugin. <br/>
 * This is a part of the convenient auto update functionality of ProjectForge. You only have to insert update methods
 * here for any further release (with e. g. required data-base modifications). ProjectForge will do the rest.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class BankingPluginUpdates
{
  static DatabaseService databaseService;

  final static Class<?>[] doClasses = new Class<?>[] { //
      BankAccountDO.class, BankAccountBalanceDO.class, BankAccountRecordDO.class };

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(BankingPlugin.BANK_ACCOUNT_ID, "2012-01-21", "Adds tables T_PLUGIN_BANK_ACCOUNT_*.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (databaseService.doTablesExist(BankAccountDO.class)) {
          // Check only the oldest table.
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          // The oldest table doesn't exist, therefore the plugin has to initialized completely.
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        // Create initial data-base table:
        new SchemaGenerator(databaseService).add(doClasses).createSchema();
        databaseService.createMissingIndices();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
