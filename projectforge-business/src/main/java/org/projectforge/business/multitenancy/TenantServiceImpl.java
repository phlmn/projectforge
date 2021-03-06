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

package org.projectforge.business.multitenancy;

import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class TenantServiceImpl implements TenantService
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TenantServiceImpl.class);

  private static final int DEFAULT_TENANT_ID = 1;

  private static Boolean hasTenants = null;

  @Autowired
  private TenantDao tenantDao;

  @Override
  public TenantDO getDefaultTenant()
  {
    if (tenantDao.tenantTableExists()) {
      return tenantDao.internalGetById(DEFAULT_TENANT_ID);
    }
    log.warn("Tenant table not exists. Return null for default tenant.");
    return null;
  }

  @Override
  public boolean isMultiTenancyAvailable()
  {
    return tenantDao.tenantTableExists()
        && GlobalConfiguration.getInstance().isMultiTenancyConfigured()
        && hasTenants();
  }

  @Override
  public boolean hasTenants()
  {
    if (hasTenants == null) {
      hasTenants = tenantDao.hasTenants();
    }
    return hasTenants;
  }

  @Override
  public void resetTenantTableStatus()
  {
    hasTenants = null;
    tenantDao.resetTenantTableStatus();
  }

  @Override
  public String getUsernameCommaList(TenantDO tenant)
  {
    if (tenant.getAssignedUsers() == null) {
      return "";
    }
    final List<String> list = new ArrayList<>();
    for (final PFUserDO user : tenant.getAssignedUsers()) {
      if (user != null) {
        list.add(user.getUsername());
      }
    }
    String usernames = StringHelper.listToString(list, ", ", true);
    return usernames;
  }

  @Override
  public TenantDO getTenant(final Integer id)
  {
    if (id == null) {
      return null;
    }
    return tenantDao.internalGetById(id);
  }

  /**
   * @param userId
   * @return the tenants
   */
  @Override
  public Collection<TenantDO> getTenantsOfUser(final Integer userId)
  {
    Collection<TenantDO> tenant2User = new ArrayList<>();
    List<TenantDO> allTenants = tenantDao.internalLoadAll();
    for (TenantDO tenant : allTenants) {
      Set<PFUserDO> assignedUsers = tenant.getAssignedUsers();
      if (assignedUsers != null) {
        for (PFUserDO user : assignedUsers) {
          if (user.getId().equals(userId)) {
            tenant2User.add(tenant);
          }
        }
      }
    }
    return tenant2User;
  }

  /**
   * @return the tenants
   */
  @Override
  public Collection<TenantDO> getTenantsOfLoggedInUser()
  {
    return getTenantsOfUser(ThreadLocalUserContext.getUserId());
  }

  @Override
  public boolean isUserAssignedToTenant(final Integer tenantId, final Integer userId)
  {
    if (tenantId == null || userId == null) {
      return false;
    }
    final TenantDO tenant = getTenant(tenantId);
    //TODO Wegen lazy loading klappt der nachfolgende Aufruf nicht
    //https://team.micromata.de/jira/browse/PROJECTFORGE-1850
    //    if (getDefaultTenant().getId().equals(tenant.getId())) {
    //      return true;
    //    }
    return isUserAssignedToTenant(tenant, userId);
  }

  /**
   * @param tenant
   * @param userId
   * @return true if tenant is not null and not deleted and the given user is assigned to the given tenant. Otherwise
   * false.
   */
  @Override
  public boolean isUserAssignedToTenant(final TenantDO tenant, final Integer userId)
  {
    if (tenant == null || tenant.isDeleted() || tenant.getAssignedUsers() == null) {
      return false;
    }

    return tenant
        .getAssignedUsers()
        .stream()
        .anyMatch(user -> user.getId().equals(userId));
  }

  @Override
  public Collection<TenantDO> getAllTenants()
  {
    return tenantDao.internalLoadAll();
  }

  @Override
  public boolean hasSelectAccess(PFUserDO loggedInUser, TenantDO tenant, boolean b)
  {
    return tenantDao.hasUserSelectAccess(loggedInUser, tenant, b);
  }

  @Override
  public String getLogName(final TenantDO tenant)
  {
    final TenantDO t = getTenant(tenant.getId());
    return "#" + t.getId() + " '" + t.getShortName() + "'";
  }

  /**
   * @param list
   * @return csv list of tenants.
   */
  @Override
  public String getTenantShortNames(final Collection<TenantDO> list)
  {
    if (list == null || list.size() == 0) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    String separator = "";
    for (final TenantDO tenant : list) {
      sb.append(separator).append(tenant.getShortName());
      separator = ", ";
    }
    return sb.toString();
  }

}
