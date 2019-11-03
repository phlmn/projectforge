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

package org.projectforge.framework.persistence.api.impl

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import javax.persistence.criteria.Predicate

internal class DBQueryBuilderByCriteria<O : ExtendedBaseDO<Int>>(
        private val baseDao: BaseDao<O>,
        private val queryFilter: QueryFilter
) {
    private var _ctx: DBCriteriaContext<O>? = null
    private val ctx: DBCriteriaContext<O>
        get() {
            if (_ctx == null) {
                val cb = baseDao.session.criteriaBuilder
                val cr = cb.createQuery(baseDao.doClass)
                _ctx = DBCriteriaContext(cb, cr, cr.from(baseDao.doClass))
                initJoinSets()
            }
            return _ctx!!
        }

    /**
     * predicates for criteria search.
     */
    private val predicates = mutableListOf<Predicate>()
    private val order = mutableListOf<javax.persistence.criteria.Order>()

    fun add(matcher: DBPredicate) {
        predicates.add(matcher.asPredicate(ctx))
    }

    fun createResultIterator(resultPredicates: List<DBPredicate>): DBResultIterator<O> {
        return DBCriteriaResultIterator(baseDao.session, ctx.cr.select(ctx.root).where(*predicates.toTypedArray()).orderBy(*order.toTypedArray()), resultPredicates)
    }

    fun addOrder(sortBy: SortBy) {
        order.add(
                if (sortBy.ascending)
                    ctx.cb.asc(ctx.root.get<Any>(sortBy.field))
                else
                    ctx.cb.desc(ctx.root.get<Any>(sortBy.field))
        )
    }

    private fun initJoinSets() {
        queryFilter.joinList.forEach {
            ctx.addJoin(it)
        }
    }
}
