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
import androidx.room.processor.ProcessorErrors.cannotFindQueryResultAdapter
import androidx.room.processor.ProcessorErrors.relationAffinityMismatch
import androidx.room.solver.CodeGenScope
import androidx.room.solver.query.parameter.QueryParameterAdapter
import androidx.room.solver.query.result.RowAdapter
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.verifier.DatabaseVerificaitonErrors
import androidx.room.writer.QueryWriter
import androidx.room.writer.RelationCollectorMethodWriter
import com.google.auto.common.MoreTypes
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
data class RelationCollector(
    val relation: Relation,
    val affinity: SQLTypeAffinity,
    val mapTypeName: ParameterizedTypeName,
    val keyTypeName: TypeName,
    val collectionTypeName: ParameterizedTypeName,
    val queryWriter: QueryWriter,
    val rowAdapter: RowAdapter,
    val loadAllQuery: ParsedQuery
) {
    // variable name of map containing keys to relation collections, set when writing the code
    // generator in writeInitCode
    lateinit var varName: String

    fun writeInitCode(scope: CodeGenScope) {
        varName = scope.getTmpVar(
                "_collection${relation.field.getPath().stripNonJava().capitalize()}")
        scope.builder().apply {
            addStatement("final $T $L = new $T()", mapTypeName, varName, mapTypeName)
        }
    }

    // called to extract the key if it exists and adds it to the map of relations to fetch.
    fun writeReadParentKeyCode(
        cursorVarName: String,
        fieldsWithIndices: List<FieldWithIndex>,
        scope: CodeGenScope
    ) {
        val indexVar = fieldsWithIndices.firstOrNull {
            it.field === relation.parentField
        }?.indexVar
        scope.builder().apply {
            readKey(cursorVarName, indexVar, scope) { tmpVar ->
                val tmpCollectionVar = scope.getTmpVar(
                        "_tmp${relation.field.name.stripNonJava().capitalize()}Collection")
                addStatement("$T $L = $L.get($L)", collectionTypeName, tmpCollectionVar,
                        varName, tmpVar)
                beginControlFlow("if ($L == null)", tmpCollectionVar).apply {
                    addStatement("$L = new $T()", tmpCollectionVar, collectionTypeName)
                    addStatement("$L.put($L, $L)", varName, tmpVar, tmpCollectionVar)
                }
                endControlFlow()
            }
        }
    }

    // called to extract key and relation collection, defaulting to empty collection if not found
    fun writeReadCollectionIntoTmpVar(
        cursorVarName: String,
        fieldsWithIndices: List<FieldWithIndex>,
        scope: CodeGenScope
    ): Pair<String, Field> {
        val indexVar = fieldsWithIndices.firstOrNull {
            it.field === relation.parentField
        }?.indexVar
        val tmpCollectionVar = scope.getTmpVar(
                "_tmp${relation.field.name.stripNonJava().capitalize()}Collection")
        scope.builder().apply {
            addStatement("$T $L = null", collectionTypeName, tmpCollectionVar)
            readKey(cursorVarName, indexVar, scope) { tmpVar ->
                addStatement("$L = $L.get($L)", tmpCollectionVar, varName, tmpVar)
            }
            beginControlFlow("if ($L == null)", tmpCollectionVar).apply {
                addStatement("$L = new $T()", tmpCollectionVar, collectionTypeName)
            }
            endControlFlow()
        }
        return tmpCollectionVar to relation.field
    }

    fun writeCollectionCode(scope: CodeGenScope) {
        val method = scope.writer
                .getOrCreateMethod(RelationCollectorMethodWriter(this))
        scope.builder().apply {
            addStatement("$N($L)", method, varName)
        }
    }

    fun readKey(
        cursorVarName: String,
        indexVar: String?,
        scope: CodeGenScope,
        postRead: CodeBlock.Builder.(String) -> Unit
    ) {
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
            val keyType = if (mapTypeName.rawType == AndroidTypeNames.LONG_SPARSE_ARRAY) {
                keyTypeName.unbox()
            } else {
                keyTypeName
            }
            val tmpVar = scope.getTmpVar("_tmpKey")
            if (relation.parentField.nonNull) {
                addStatement("final $T $L = $L.$L($L)",
                        keyType, tmpVar, cursorVarName, cursorGetter, indexVar)
                this.postRead(tmpVar)
            } else {
                beginControlFlow("if (!$L.isNull($L))", cursorVarName, indexVar).apply {
                    addStatement("final $T $L = $L.$L($L)",
                            keyType, tmpVar, cursorVarName, cursorGetter, indexVar)
                    this.postRead(tmpVar)
                }
                endControlFlow()
            }
        }
    }

    /**
     * Adapter for binding a LongSparseArray keys into query arguments. This special adapter is only
     * used for binding the relationship query who's keys have INTEGER affinity.
     */
    private class LongSparseArrayKeyQueryParameterAdapter : QueryParameterAdapter(true) {
        override fun bindToStmt(
            inputVarName: String,
            stmtVarName: String,
            startIndexVarName: String,
            scope: CodeGenScope
        ) {
            scope.builder().apply {
                val itrIndexVar = "i"
                val itrItemVar = scope.getTmpVar("_item")
                beginControlFlow("for (int $L = 0; $L < $L.size(); i++)",
                        itrIndexVar, itrIndexVar, inputVarName).apply {
                    addStatement("long $L = $L.keyAt($L)", itrItemVar, inputVarName, itrIndexVar)
                    addStatement("$L.bindLong($L, $L)", stmtVarName, startIndexVarName, itrItemVar)
                    addStatement("$L ++", startIndexVarName)
                }
                endControlFlow()
            }
        }

        override fun getArgCount(
            inputVarName: String,
            outputVarName: String,
            scope: CodeGenScope
        ) {
            scope.builder().addStatement("final $T $L = $L.size()",
                    TypeName.INT, outputVarName, inputVarName)
        }
    }

    companion object {

        private val LONG_SPARSE_ARRAY_KEY_QUERY_PARAM_ADAPTER =
                LongSparseArrayKeyQueryParameterAdapter()

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

                val canUseLongSparseArray = context.processingEnv.elementUtils
                        .getTypeElement(AndroidTypeNames.LONG_SPARSE_ARRAY.toString()) != null
                val canUseArrayMap = context.processingEnv.elementUtils
                        .getTypeElement(AndroidTypeNames.ARRAY_MAP.toString()) != null
                val tmpMapType = when {
                    canUseLongSparseArray && affinity == SQLTypeAffinity.INTEGER -> {
                        ParameterizedTypeName.get(AndroidTypeNames.LONG_SPARSE_ARRAY,
                                collectionTypeName)
                    }
                    canUseArrayMap -> {
                        ParameterizedTypeName.get(AndroidTypeNames.ARRAY_MAP,
                                keyType, collectionTypeName)
                    }
                    else -> {
                        ParameterizedTypeName.get(ClassName.get(java.util.HashMap::class.java),
                                keyType, collectionTypeName)
                    }
                }

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

                val usingLongSparseArray = tmpMapType.rawType == AndroidTypeNames.LONG_SPARSE_ARRAY
                val queryParam = if (usingLongSparseArray) {
                    val longSparseArrayElement = context.processingEnv.elementUtils
                            .getTypeElement(AndroidTypeNames.LONG_SPARSE_ARRAY.toString())
                    QueryParameter(
                            name = RelationCollectorMethodWriter.PARAM_MAP_VARIABLE,
                            sqlName = RelationCollectorMethodWriter.PARAM_MAP_VARIABLE,
                            type = MoreTypes.asDeclared(longSparseArrayElement.asType()),
                            queryParamAdapter = LONG_SPARSE_ARRAY_KEY_QUERY_PARAM_ADAPTER
                    )
                } else {
                    val keyTypeMirror = keyTypeMirrorFor(context, affinity)
                    val set = context.processingEnv.elementUtils.getTypeElement("java.util.Set")
                    val keySet = context.processingEnv.typeUtils.getDeclaredType(set, keyTypeMirror)
                    QueryParameter(
                            name = RelationCollectorMethodWriter.KEY_SET_VARIABLE,
                            sqlName = RelationCollectorMethodWriter.KEY_SET_VARIABLE,
                            type = keySet,
                            queryParamAdapter = context.typeAdapterStore.findQueryParameterAdapter(
                                    keySet)
                    )
                }

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
                    context.logger.e(relation.field.element,
                        cannotFindQueryResultAdapter(relation.pojoType.toString()))
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
