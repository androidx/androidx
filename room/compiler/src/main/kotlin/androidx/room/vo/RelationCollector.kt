/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.vo

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.parser.ParsedQuery
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SqlParser
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_QUERY_RESULT_ADAPTER
import androidx.room.processor.ProcessorErrors.relationAffinityMismatch
import androidx.room.solver.CodeGenScope
import androidx.room.solver.query.result.RowAdapter
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.verifier.DatabaseVerificaitonErrors
import androidx.room.writer.QueryWriter
import androidx.room.writer.RelationCollectorMethodWriter
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import stripNonJava
import java.util.ArrayList
import java.util.HashSet
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Internal class that is used to manage fetching 1/N to N relationships.
 */
data class RelationCollector(val relation: Relation,
                             val affinity: SQLTypeAffinity,
                             val mapTypeName: ParameterizedTypeName,
                             val keyTypeName: TypeName,
                             val collectionTypeName: ParameterizedTypeName,
                             val queryWriter: QueryWriter,
                             val rowAdapter: RowAdapter,
                             val loadAllQuery: ParsedQuery) {
    // set when writing the code generator in writeInitCode
    lateinit var varName: String

    fun writeInitCode(scope: CodeGenScope) {
        val tmpVar = scope.getTmpVar(
                "_collection${relation.field.getPath().stripNonJava().capitalize()}")
        scope.builder().addStatement("final $T $L = new $T()", mapTypeName, tmpVar, mapTypeName)
        varName = tmpVar
    }

    // called after reading each item to extract the key if it exists
    fun writeReadParentKeyCode(cursorVarName: String, itemVar: String,
                               fieldsWithIndices: List<FieldWithIndex>, scope: CodeGenScope) {
        val indexVar = fieldsWithIndices.firstOrNull {
            it.field === relation.parentField
        }?.indexVar
        scope.builder().apply {
            readKey(
                    cursorVarName = cursorVarName,
                    indexVar = indexVar,
                    scope = scope
            ) { tmpVar ->
                val tmpCollectionVar = scope.getTmpVar("_tmpCollection")
                addStatement("$T $L = $L.get($L)", collectionTypeName, tmpCollectionVar,
                        varName, tmpVar)
                beginControlFlow("if($L == null)", tmpCollectionVar).apply {
                    addStatement("$L = new $T()", tmpCollectionVar, collectionTypeName)
                    addStatement("$L.put($L, $L)", varName, tmpVar, tmpCollectionVar)
                }
                endControlFlow()
                // set it on the item
                relation.field.setter.writeSet(itemVar, tmpCollectionVar, this)
            }
        }
    }

    fun writeCollectionCode(scope: CodeGenScope) {
        val method = scope.writer
                .getOrCreateMethod(RelationCollectorMethodWriter(this))
        scope.builder().apply {
            addStatement("$N($L)", method, varName)
        }
    }

    fun readKey(cursorVarName: String, indexVar: String?, scope: CodeGenScope,
                postRead: CodeBlock.Builder.(String) -> Unit) {
        val cursorGetter = when (affinity) {
            SQLTypeAffinity.INTEGER -> "getLong"
            SQLTypeAffinity.REAL -> "getDouble"
            SQLTypeAffinity.TEXT -> "getString"
            SQLTypeAffinity.BLOB -> "getBlob"
            else -> {
                "getString"
            }
        }
        scope.builder().apply {
            beginControlFlow("if (!$L.isNull($L))", cursorVarName, indexVar).apply {
                val tmpVar = scope.getTmpVar("_tmpKey")
                addStatement("final $T $L = $L.$L($L)", keyTypeName,
                        tmpVar, cursorVarName, cursorGetter, indexVar)
                this.postRead(tmpVar)
            }
            endControlFlow()
        }
    }

    companion object {
        fun createCollectors(
                baseContext: Context,
                relations: List<Relation>
        ): List<RelationCollector> {
            return relations.map { relation ->
                // decide on the affinity
                val context = baseContext.fork(relation.field.element)
                val parentAffinity = relation.parentField.cursorValueReader?.affinity()
                val childAffinity = relation.entityField.cursorValueReader?.affinity()
                val affinity = if (parentAffinity != null && parentAffinity == childAffinity) {
                    parentAffinity
                } else {
                    context.logger.w(Warning.RELATION_TYPE_MISMATCH, relation.field.element,
                            relationAffinityMismatch(
                                    parentColumn = relation.parentField.columnName,
                                    childColumn = relation.entityField.columnName,
                                    parentAffinity = parentAffinity,
                                    childAffinity = childAffinity))
                    SQLTypeAffinity.TEXT
                }
                val keyType = keyTypeFor(context, affinity)
                val collectionTypeName = if (relation.field.typeName is ParameterizedTypeName) {
                    val paramType = relation.field.typeName as ParameterizedTypeName
                    if (paramType.rawType == CommonTypeNames.LIST) {
                        ParameterizedTypeName.get(ClassName.get(ArrayList::class.java),
                                relation.pojoTypeName)
                    } else if (paramType.rawType == CommonTypeNames.SET) {
                        ParameterizedTypeName.get(ClassName.get(HashSet::class.java),
                                relation.pojoTypeName)
                    } else {
                        ParameterizedTypeName.get(ClassName.get(ArrayList::class.java),
                                relation.pojoTypeName)
                    }
                } else {
                    ParameterizedTypeName.get(ClassName.get(ArrayList::class.java),
                            relation.pojoTypeName)
                }

                val canUseArrayMap = context.processingEnv.elementUtils
                        .getTypeElement(AndroidTypeNames.ARRAY_MAP.toString()) != null
                val mapClass = if (canUseArrayMap) {
                    AndroidTypeNames.ARRAY_MAP
                } else {
                    ClassName.get(java.util.HashMap::class.java)
                }
                val tmpMapType = ParameterizedTypeName.get(mapClass, keyType, collectionTypeName)
                val keyTypeMirror = keyTypeMirrorFor(context, affinity)
                val set = context.processingEnv.elementUtils.getTypeElement("java.util.Set")
                val keySet = context.processingEnv.typeUtils.getDeclaredType(set, keyTypeMirror)
                val loadAllQuery = relation.createLoadAllSql()
                val parsedQuery = SqlParser.parse(loadAllQuery)
                context.checker.check(parsedQuery.errors.isEmpty(), relation.field.element,
                        parsedQuery.errors.joinToString("\n"))
                if (parsedQuery.errors.isEmpty()) {
                    val resultInfo = context.databaseVerifier?.analyze(loadAllQuery)
                    parsedQuery.resultInfo = resultInfo
                    if (resultInfo?.error != null) {
                        context.logger.e(relation.field.element,
                                DatabaseVerificaitonErrors.cannotVerifyQuery(resultInfo.error))
                    }
                }
                val resultInfo = parsedQuery.resultInfo

                val queryParam = QueryParameter(
                        name = RelationCollectorMethodWriter.KEY_SET_VARIABLE,
                        sqlName = RelationCollectorMethodWriter.KEY_SET_VARIABLE,
                        type = keySet,
                        queryParamAdapter =
                                context.typeAdapterStore.findQueryParameterAdapter(keySet))
                val queryWriter = QueryWriter(
                        parameters = listOf(queryParam),
                        sectionToParamMapping = listOf(Pair(parsedQuery.bindSections.first(),
                                queryParam)),
                        query = parsedQuery
                )

                // row adapter that matches full response
                fun getDefaultRowAdapter(): RowAdapter? {
                    return context.typeAdapterStore.findRowAdapter(relation.pojoType, parsedQuery)
                }
                val rowAdapter = if (relation.projection.size == 1 && resultInfo != null &&
                        (resultInfo.columns.size == 1 || resultInfo.columns.size == 2)) {
                    // check for a column adapter first
                    val cursorReader = context.typeAdapterStore.findCursorValueReader(
                            relation.pojoType, resultInfo.columns.first().type)
                    if (cursorReader == null) {
                        getDefaultRowAdapter()
                    } else {
                        context.logger.d("Choosing cursor adapter for the return value since" +
                                " the query returns only 1 or 2 columns and there is a cursor" +
                                " adapter for the return type.")
                        SingleColumnRowAdapter(cursorReader)
                    }
                } else {
                    getDefaultRowAdapter()
                }

                if (rowAdapter == null) {
                    context.logger.e(relation.field.element, CANNOT_FIND_QUERY_RESULT_ADAPTER)
                    null
                } else {
                    RelationCollector(
                            relation = relation,
                            affinity = affinity,
                            mapTypeName = tmpMapType,
                            keyTypeName = keyType,
                            collectionTypeName = collectionTypeName,
                            queryWriter = queryWriter,
                            rowAdapter = rowAdapter,
                            loadAllQuery = parsedQuery
                    )
                }
            }.filterNotNull()
        }

        private fun keyTypeMirrorFor(context: Context, affinity: SQLTypeAffinity): TypeMirror {
            val types = context.processingEnv.typeUtils
            val elements = context.processingEnv.elementUtils
            return when (affinity) {
                SQLTypeAffinity.INTEGER -> elements.getTypeElement("java.lang.Long").asType()
                SQLTypeAffinity.REAL -> elements.getTypeElement("java.lang.Double").asType()
                SQLTypeAffinity.TEXT -> context.COMMON_TYPES.STRING
                SQLTypeAffinity.BLOB -> types.getArrayType(types.getPrimitiveType(TypeKind.BYTE))
                else -> {
                    context.COMMON_TYPES.STRING
                }
            }
        }

        private fun keyTypeFor(context: Context, affinity: SQLTypeAffinity): TypeName {
            return when (affinity) {
                SQLTypeAffinity.INTEGER -> TypeName.LONG.box()
                SQLTypeAffinity.REAL -> TypeName.DOUBLE.box()
                SQLTypeAffinity.TEXT -> TypeName.get(String::class.java)
                SQLTypeAffinity.BLOB -> ArrayTypeName.of(TypeName.BYTE)
                else -> {
                    // no affinity select from type
                    context.COMMON_TYPES.STRING.typeName()
                }
            }
        }
    }
}
