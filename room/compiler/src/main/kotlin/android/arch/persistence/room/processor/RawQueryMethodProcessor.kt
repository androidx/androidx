/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.arch.persistence.room.processor

import android.arch.persistence.room.RawQuery
import android.arch.persistence.room.Transaction
import android.arch.persistence.room.ext.SupportDbTypeNames
import android.arch.persistence.room.ext.hasAnnotation
import android.arch.persistence.room.ext.toListOfClassTypes
import android.arch.persistence.room.ext.typeName
import android.arch.persistence.room.parser.SqlParser
import android.arch.persistence.room.vo.Entity
import android.arch.persistence.room.vo.RawQueryMethod
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class RawQueryMethodProcessor(
        baseContext: Context,
        val containing: DeclaredType,
        val executableElement: ExecutableElement) {
    val context = baseContext.fork(executableElement)
    fun process(): RawQueryMethod {
        val types = context.processingEnv.typeUtils
        val asMember = types.asMemberOf(containing, executableElement)
        val executableType = MoreTypes.asExecutable(asMember)

        val annotation = MoreElements.getAnnotationMirror(executableElement,
                RawQuery::class.java).orNull()
        context.checker.check(annotation != null, executableElement,
                ProcessorErrors.MISSING_RAWQUERY_ANNOTATION)

        val returnTypeName = TypeName.get(executableType.returnType)
        context.checker.notUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS)
        val observedEntities = processObservedTables()
        val query = SqlParser.rawQueryForTables(
                observedEntities.map { it.tableName }.toSet())
        // build the query but don't calculate result info since we just guessed it.
        val resultBinder = context.typeAdapterStore
                .findQueryResultBinder(executableType.returnType, query)

        val runtimeQueryParam = findRuntimeQueryParameter()
        val inTransaction = executableElement.hasAnnotation(Transaction::class)
        val rawQueryMethod = RawQueryMethod(
                element = executableElement,
                name = executableElement.simpleName.toString(),
                observedEntities = observedEntities,
                returnType = executableType.returnType,
                runtimeQueryParam = runtimeQueryParam,
                inTransaction = inTransaction,
                queryResultBinder = resultBinder
        )
        context.checker.check(rawQueryMethod.returnsValue, executableElement,
                ProcessorErrors.RAW_QUERY_BAD_RETURN_TYPE)
        return rawQueryMethod
    }

    private fun processObservedTables(): List<Entity> {
        val annotation = MoreElements
                .getAnnotationMirror(executableElement,
                        android.arch.persistence.room.RawQuery::class.java)
                .orNull() ?: return emptyList()
        val entityList = AnnotationMirrors.getAnnotationValue(annotation, "observedEntities")
        return entityList
                .toListOfClassTypes()
                .map {
                    EntityProcessor(
                            baseContext = context,
                            element = MoreTypes.asTypeElement(it)
                    ).process()
                }
    }

    private fun findRuntimeQueryParameter(): RawQueryMethod.RuntimeQueryParameter? {
        val types = context.processingEnv.typeUtils
        if (executableElement.parameters.size == 1 && !executableElement.isVarArgs) {
            val param = MoreTypes.asMemberOf(
                    types,
                    containing,
                    executableElement.parameters[0])
            val elementUtils = context.processingEnv.elementUtils
            val supportQueryType = elementUtils
                    .getTypeElement(SupportDbTypeNames.QUERY.toString()).asType()
            val isSupportSql = types.isAssignable(param, supportQueryType)
            if (isSupportSql) {
                return RawQueryMethod.RuntimeQueryParameter(
                        paramName = executableElement.parameters[0].simpleName.toString(),
                        type = supportQueryType.typeName())
            }
            val stringType = elementUtils.getTypeElement("java.lang.String").asType()
            val isString = types.isAssignable(param, stringType)
            if (isString) {
                return RawQueryMethod.RuntimeQueryParameter(
                        paramName = executableElement.parameters[0].simpleName.toString(),
                        type = stringType.typeName())
            }
        }
        context.logger.e(executableElement, ProcessorErrors.RAW_QUERY_BAD_PARAMS)
        return null
    }
}