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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.*
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        getList(pagesRest: AbstractPagesRest<O, DTO, B>,
                baseDao: BaseDao<O>,
                magicFilter: MagicFilter)
        : ResultSet<O> {
    magicFilter.sortAndLimitMaxRowsWhileSelect = true
    val queryFilter = QueryFilter()
    val customResultFilters = pagesRest.preProcessMagicFilter(queryFilter, magicFilter)
    MagicFilterProcessor.doIt(baseDao.doClass, magicFilter, queryFilter)
    pagesRest.postProcessMagicFilter(queryFilter, magicFilter)
    val list = baseDao.getList(queryFilter, customResultFilters)
    val resultSet = ResultSet(pagesRest.filterList(list, magicFilter), list.size)
    return resultSet
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        saveOrUpdate(request: HttpServletRequest,
                     baseDao: BaseDao<O>,
                     obj: O,
                     postData: PostData<DTO>,
                     pagesRest: AbstractPagesRest<O, DTO, B>,
                     validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {

    try {
        if (!validationErrorsList.isNullOrEmpty()) {
            // Validation error occurred:
            return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
        }
        val isNew = obj.id == null || obj.created == null // obj.created is needed for KundeDO (id isn't null for inserting new customers).
        pagesRest.beforeSaveOrUpdate(request, obj, postData)
        pagesRest.beforeDatabaseAction(request, obj, postData, if (obj.id != null) OperationType.UPDATE else OperationType.INSERT)
        baseDao.saveOrUpdate(obj) ?: obj.id
        pagesRest.afterSaveOrUpdate(obj, postData)
        if (isNew) {
            return ResponseEntity(pagesRest.afterSave(obj, postData), HttpStatus.OK)
        } else {
            return ResponseEntity(pagesRest.afterUpdate(obj, postData), HttpStatus.OK)
        }
    } catch (ex: Exception) {
        return handleExcepction("Error while trying to save/update object '${obj::class.java}' with id #${obj.id}", ex)
    }
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        undelete(request: HttpServletRequest,
                 baseDao: BaseDao<O>,
                 obj: O,
                 postData: PostData<DTO>,
                 pagesRest: AbstractPagesRest<O, DTO, B>,
                 validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.beforeDatabaseAction(request, obj, postData, OperationType.UNDELETE)
            pagesRest.beforeUndelete(request, obj, postData)
            baseDao.undelete(obj)
            return ResponseEntity(pagesRest.afterUndelete(obj, postData), HttpStatus.OK)
        }
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    } catch (ex: Exception) {
        return handleExcepction("Error while trying to undelete object '${obj::class.java}' with id #${obj.id}", ex)
    }
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        markAsDeleted(request: HttpServletRequest,
                      baseDao: BaseDao<O>,
                      obj: O,
                      postData: PostData<DTO>,
                      pagesRest: AbstractPagesRest<O, DTO, B>,
                      validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.beforeDatabaseAction(request, obj, postData, OperationType.DELETE)
            pagesRest.beforeMarkAsDeleted(request, obj, postData)
            baseDao.markAsDeleted(obj)
            return ResponseEntity(pagesRest.afterMarkAsDeleted(obj, postData), HttpStatus.OK)
        }
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    } catch (ex: Exception) {
        return handleExcepction("Error while trying to mark object '${obj::class.java}' as deleted with id #${obj.id}", ex)
    }
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        delete(request: HttpServletRequest,
               baseDao: BaseDao<O>,
               obj: O,
               postData: PostData<DTO>,
               pagesRest: AbstractPagesRest<O, DTO, B>,
               validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.beforeDatabaseAction(request, obj, postData, OperationType.DELETE)
            pagesRest.beforeDelete(request, obj, postData)
            baseDao.delete(obj)
            return ResponseEntity(pagesRest.afterDelete(obj, postData), HttpStatus.OK)
        }
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    } catch (ex: Exception) {
        return handleExcepction("Error while trying to delete object '${obj::class.java}' with id #${obj.id}", ex)
    }
}

private fun handleExcepction(msg: String, ex: Exception): ResponseEntity<ResponseAction> {
    if (ex is UserException) {
        log.error("$msg: message='${ex.i18nKey}', params='${ex.msgParams?.joinToString() { it.toString() }}'")
        val error = ValidationError(translateMsg(ex), messageId = ex.i18nKey)
        if (!ex.causedByField.isNullOrBlank()) error.fieldId = ex.causedByField
        val errors = listOf(error)
        return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
    } else {
        log.error("$msg: message='${ex.message}'", ex)
        val error = ValidationError(ex.message)
        val errors = listOf(error)
        return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
    }
}
