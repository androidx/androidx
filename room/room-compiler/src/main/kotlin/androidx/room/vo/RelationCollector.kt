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

import androidx.room.compiler.processing.XType
import androidx.room.ext.CollectionTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.parser.ParsedQuery
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SqlParser
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors.cannotFindQueryResultAdapter
import androidx.room.processor.ProcessorErrors.relationAffinityMismatch
import androidx.room.processor.ProcessorErrors.relationJunctionChildAffinityMismatch
import androidx.room.processor.ProcessorErrors.relationJunctionParentAffinityMismatch
import androidx.room.solver.CodeGenScope
import androidx.room.solver.query.parameter.QueryParameterAdapter
import androidx.room.solver.query.result.RowAdapter
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.verifier.DatabaseVerificationErrors
import androidx.room.writer.QueryWriter
import androidx.room.writer.RelationCollectorMethodWriter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.HashSet
import java.util.Locale

/**
 * Internal class that is used to manage fetching 1/N to N relationships.
 */
data class RelationCollector(
    val relation: Relation,
    val affinity: SQLTypeAffinity,
    val mapTypeName: ParameterizedTypeName,
    val keyTypeName: TypeName,
    val relationTypeName: TypeName,
    val queryWriter: QueryWriter,
    val rowAdapter: RowAdapter,
    val loadAllQuery: ParsedQuery,
    val relationTypeIsCollection: Boolean
) {
    // variable name of map containing keys to relation collections, set when writing the code
    // generator in writeInitCode
    lateinit var varName: String

    fun writeInitCode(scope: CodeGenScope) {
        varName = scope.getTmpVar(
            "_collection${relation.field.getPath().stripNonJava().capitalize(Locale.US)}"
        )
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
                if (relationTypeIsCollection) {
                    val tmpCollectionVar = scope.getTmpVar(
                        "_tmp${relation.field.name.stripNonJava().capitalize(Locale.US)}Collection"
                    )
                    addStatement(
                        "$T $L = $L.get($L)", relationTypeName, tmpCollectionVar,
                        varName, tmpVar
                    )
                    beginControlFlow("if ($L == null)", tmpCollectionVar).apply {
                        addStatement("$L = new $T()", tmpCollectionVar, relationTypeName)
                        addStatement("$L.put($L, $L)", varName, tmpVar, tmpCollectionVar)
                    }
                    endControlFlow()
                } else {
                    addStatement("$L.put($L, null)", varName, tmpVar)
                }
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
        val tmpvarNameSuffix = if (relationTypeIsCollection) "Collection" else ""
        val tmpRelationVar = scope.getTmpVar(
            "_tmp${relation.field.name.stripNonJava().capitalize(Locale.US)}$tmpvarNameSuffix"
        )
        scope.builder().apply {
            addStatement("$T $L = null", relationTypeName, tmpRelationVar)
            readKey(cursorVarName, indexVar, scope) { tmpVar ->
                addStatement("$L = $L.get($L)", tmpRelationVar, varName, tmpVar)
            }
            if (relationTypeIsCollection) {
                beginControlFlow("if ($L == null)", tmpRelationVar).apply {
                    addStatement("$L = new $T()", tmpRelationVar, relationTypeName)
                }
                endControlFlow()
            }
        }
        return tmpRelationVar to relation.field
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
            val keyType = if (mapTypeName.rawType == CollectionTypeNames.LONG_SPARSE_ARRAY) {
                keyTypeName.unbox()
            } else {
                keyTypeName
            }
            val tmpVar = scope.getTmpVar("_tmpKey")
            fun addKeyReadStatement() {
                if (keyTypeName == TypeName.get(ByteBuffer::class.java)) {
                    addStatement(
                        "final $T $L = $T.wrap($L.$L($L))",
                        keyType, tmpVar, keyTypeName, cursorVarName, cursorGetter, indexVar
                    )
                } else {
                    addStatement(
                        "final $T $L = $L.$L($L)",
                        keyType, tmpVar, cursorVarName, cursorGetter, indexVar
                    )
                }
                this.postRead(tmpVar)
            }
            if (relation.parentField.nonNull) {
                addKeyReadStatement()
            } else {
                beginControlFlow("if (!$L.isNull($L))", cursorVarName, indexVar).apply {
                    addKeyReadStatement()
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
                beginControlFlow(
                    "for (int $L = 0; $L < $L.size(); i++)",
                    itrIndexVar, itrIndexVar, inputVarName
                ).apply {
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
            scope.builder().addStatement(
                "final $T $L = $L.size()",
                TypeName.INT, outputVarName, inputVarName
            )
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
                val context = baseContext.fork(
                    element = relation.field.element,
                    forceSuppressedWarnings = setOf(Warning.CURSOR_MISMATCH)
                )
                val affinity = affinityFor(context, relation)
                val keyType = keyTypeFor(context, affinity)
                val (relationTypeName, isRelationCollection) = relationTypeFor(relation)
                val tmpMapType = temporaryMapTypeFor(context, affinity, keyType, relationTypeName)

                val loadAllQuery = relation.createLoadAllSql()
                val parsedQuery = SqlParser.parse(loadAllQuery)
                context.checker.check(
                    parsedQuery.errors.isEmpty(), relation.field.element,
                    parsedQuery.errors.joinToString("\n")
                )
                if (parsedQuery.errors.isEmpty()) {
                    val resultInfo = context.databaseVerifier?.analyze(loadAllQuery)
                    parsedQuery.resultInfo = resultInfo
                    if (resultInfo?.error != null) {
                        context.logger.e(
                            relation.field.element,
                            DatabaseVerificationErrors.cannotVerifyQuery(resultInfo.error)
                        )
                    }
                }
                val resultInfo = parsedQuery.resultInfo

                val usingLongSparseArray =
                    tmpMapType.rawType == CollectionTypeNames.LONG_SPARSE_ARRAY
                val queryParam = if (usingLongSparseArray) {
                    val longSparseArrayElement = context.processingEnv
                        .requireTypeElement(CollectionTypeNames.LONG_SPARSE_ARRAY)
                    QueryParameter(
                        name = RelationCollectorMethodWriter.PARAM_MAP_VARIABLE,
                        sqlName = RelationCollectorMethodWriter.PARAM_MAP_VARIABLE,
                        type = longSparseArrayElement.type,
                        queryParamAdapter = LONG_SPARSE_ARRAY_KEY_QUERY_PARAM_ADAPTER
                    )
                } else {
                    val keyTypeMirror = keyTypeMirrorFor(context, affinity)
                    val set = context.processingEnv.requireTypeElement("java.util.Set")
                    val keySet = context.processingEnv.getDeclaredType(set, keyTypeMirror)
                    QueryParameter(
                        name = RelationCollectorMethodWriter.KEY_SET_VARIABLE,
                        sqlName = RelationCollectorMethodWriter.KEY_SET_VARIABLE,
                        type = keySet,
                        queryParamAdapter = context.typeAdapterStore.findQueryParameterAdapter(
                            typeMirror = keySet,
                            isMultipleParameter = true
                        )
                    )
                }

                val queryWriter = QueryWriter(
                    parameters = listOf(queryParam),
                    sectionToParamMapping = listOf(
                        Pair(
                            parsedQuery.bindSections.first(),
                            queryParam
                        )
                    ),
                    query = parsedQuery
                )

                // row adapter that matches full response
                fun getDefaultRowAdapter(): RowAdapter? {
                    return context.typeAdapterStore.findRowAdapter(relation.pojoType, parsedQuery)
                }
                val rowAdapter = if (relation.projection.size == 1 && resultInfo != null &&
                    (resultInfo.columns.size == 1 || resultInfo.columns.size == 2)
                ) {
                    // check for a column adapter first
                    val cursorReader = context.typeAdapterStore.findCursorValueReader(
                        relation.pojoType, resultInfo.columns.first().type
                    )
                    if (cursorReader == null) {
                        getDefaultRowAdapter()
                    } else {
                        SingleColumnRowAdapter(cursorReader)
                    }
                } else {
                    getDefaultRowAdapter()
                }

                if (rowAdapter == null) {
                    context.logger.e(
                        relation.field.element,
                        cannotFindQueryResultAdapter(relation.pojoType.typeName)
                    )
                    null
                } else {
                    RelationCollector(
                        relation = relation,
                        affinity = affinity,
                        mapTypeName = tmpMapType,
                        keyTypeName = keyType,
                        relationTypeName = relationTypeName,
                        queryWriter = queryWriter,
                        rowAdapter = rowAdapter,
                        loadAllQuery = parsedQuery,
                        relationTypeIsCollection = isRelationCollection
                    )
                }
            }.filterNotNull()
        }

        // Gets and check the affinity of the relating columns.
        private fun affinityFor(context: Context, relation: Relation): SQLTypeAffinity {
            fun checkAffinity(
                first: SQLTypeAffinity?,
                second: SQLTypeAffinity?,
                onAffinityMismatch: () -> Unit
            ) = if (first != null && first == second) {
                first
            } else {
                onAffinityMismatch()
                SQLTypeAffinity.TEXT
            }

            val parentAffinity = relation.parentField.cursorValueReader?.affinity()
            val childAffinity = relation.entityField.cursorValueReader?.affinity()
            val junctionParentAffinity =
                relation.junction?.parentField?.cursorValueReader?.affinity()
            val junctionChildAffinity =
                relation.junction?.entityField?.cursorValueReader?.affinity()
            return if (relation.junction != null) {
                checkAffinity(childAffinity, junctionChildAffinity) {
                    context.logger.w(
                        Warning.RELATION_TYPE_MISMATCH, relation.field.element,
                        relationJunctionChildAffinityMismatch(
                            childColumn = relation.entityField.columnName,
                            junctionChildColumn = relation.junction.entityField.columnName,
                            childAffinity = childAffinity,
                            junctionChildAffinity = junctionChildAffinity
                        )
                    )
                }
                checkAffinity(parentAffinity, junctionParentAffinity) {
                    context.logger.w(
                        Warning.RELATION_TYPE_MISMATCH, relation.field.element,
                        relationJunctionParentAffinityMismatch(
                            parentColumn = relation.parentField.columnName,
                            junctionParentColumn = relation.junction.parentField.columnName,
                            parentAffinity = parentAffinity,
                            junctionParentAffinity = junctionParentAffinity
                        )
                    )
                }
            } else {
                checkAffinity(parentAffinity, childAffinity) {
                    context.logger.w(
                        Warning.RELATION_TYPE_MISMATCH, relation.field.element,
                        relationAffinityMismatch(
                            parentColumn = relation.parentField.columnName,
                            childColumn = relation.entityField.columnName,
                            parentAffinity = parentAffinity,
                            childAffinity = childAffinity
                        )
                    )
                }
            }
        }

        // Gets the resulting relation type name. (i.e. the Pojo's @Relation field type name.)
        private fun relationTypeFor(relation: Relation) =
            if (relation.field.typeName is ParameterizedTypeName) {
                val paramType = relation.field.typeName as ParameterizedTypeName
                val paramTypeName = if (paramType.rawType == CommonTypeNames.LIST) {
                    ParameterizedTypeName.get(
                        ClassName.get(ArrayList::class.java),
                        relation.pojoTypeName
                    )
                } else if (paramType.rawType == CommonTypeNames.SET) {
                    ParameterizedTypeName.get(
                        ClassName.get(HashSet::class.java),
                        relation.pojoTypeName
                    )
                } else {
                    ParameterizedTypeName.get(
                        ClassName.get(ArrayList::class.java),
                        relation.pojoTypeName
                    )
                }
                paramTypeName to true
            } else {
                relation.pojoTypeName to false
            }

        // Gets the type name of the temporary key map.
        private fun temporaryMapTypeFor(
            context: Context,
            affinity: SQLTypeAffinity,
            keyType: TypeName,
            relationTypeName: TypeName
        ): ParameterizedTypeName {
            val canUseLongSparseArray = context.processingEnv
                .findTypeElement(CollectionTypeNames.LONG_SPARSE_ARRAY) != null
            val canUseArrayMap = context.processingEnv
                .findTypeElement(CollectionTypeNames.ARRAY_MAP) != null
            return when {
                canUseLongSparseArray && affinity == SQLTypeAffinity.INTEGER -> {
                    ParameterizedTypeName.get(
                        CollectionTypeNames.LONG_SPARSE_ARRAY,
                        relationTypeName
                    )
                }
                canUseArrayMap -> {
                    ParameterizedTypeName.get(
                        CollectionTypeNames.ARRAY_MAP,
                        keyType, relationTypeName
                    )
                }
                else -> {
                    ParameterizedTypeName.get(
                        ClassName.get(java.util.HashMap::class.java),
                        keyType, relationTypeName
                    )
                }
            }
        }

        // Gets the type mirror of the relationship key.
        private fun keyTypeMirrorFor(context: Context, affinity: SQLTypeAffinity): XType {
            val processingEnv = context.processingEnv
            return when (affinity) {
                SQLTypeAffinity.INTEGER -> processingEnv.requireType("java.lang.Long")
                SQLTypeAffinity.REAL -> processingEnv.requireType("java.lang.Double")
                SQLTypeAffinity.TEXT -> context.COMMON_TYPES.STRING
                SQLTypeAffinity.BLOB -> processingEnv.requireType("java.nio.ByteBuffer")
                else -> {
                    context.COMMON_TYPES.STRING
                }
            }
        }

        // Gets the type name of the relationship key.
        private fun keyTypeFor(context: Context, affinity: SQLTypeAffinity): TypeName {
            return when (affinity) {
                SQLTypeAffinity.INTEGER -> TypeName.LONG.box()
                SQLTypeAffinity.REAL -> TypeName.DOUBLE.box()
                SQLTypeAffinity.TEXT -> TypeName.get(String::class.java)
                SQLTypeAffinity.BLOB -> TypeName.get(ByteBuffer::class.java)
                else -> {
                    // no affinity select from type
                    context.COMMON_TYPES.STRING.typeName
                }
            }
        }
    }
}
