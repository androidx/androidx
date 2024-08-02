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

import androidx.room.BuiltInTypeConverters
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XNullability
import androidx.room.ext.CollectionTypeNames.ARRAY_MAP
import androidx.room.ext.CollectionTypeNames.LONG_SPARSE_ARRAY
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.CommonTypeNames.ARRAY_LIST
import androidx.room.ext.CommonTypeNames.HASH_MAP
import androidx.room.ext.CommonTypeNames.HASH_SET
import androidx.room.ext.KotlinCollectionMemberNames
import androidx.room.ext.KotlinCollectionMemberNames.MUTABLE_LIST_OF
import androidx.room.ext.KotlinCollectionMemberNames.MUTABLE_SET_OF
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.parser.ParsedQuery
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SqlParser
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.processor.ProcessorErrors.ISSUE_TRACKER_LINK
import androidx.room.processor.ProcessorErrors.relationAffinityMismatch
import androidx.room.processor.ProcessorErrors.relationJunctionChildAffinityMismatch
import androidx.room.processor.ProcessorErrors.relationJunctionParentAffinityMismatch
import androidx.room.solver.CodeGenScope
import androidx.room.solver.query.parameter.QueryParameterAdapter
import androidx.room.solver.query.result.RowAdapter
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.solver.types.CursorValueReader
import androidx.room.verifier.DatabaseVerificationErrors
import androidx.room.writer.QueryWriter
import androidx.room.writer.RelationCollectorFunctionWriter
import androidx.room.writer.RelationCollectorFunctionWriter.Companion.PARAM_CONNECTION_VARIABLE
import java.util.Locale

/** Internal class that is used to manage fetching 1/N to N relationships. */
data class RelationCollector(
    val relation: Relation,
    // affinity between relation fields
    val affinity: SQLTypeAffinity,
    // concrete map type name to store relationship
    val mapTypeName: XTypeName,
    // map key type name, not the same as the parent or entity field type
    val keyTypeName: XTypeName,
    // map value type name, it is assignable to the @Relation field
    val relationTypeName: XTypeName,
    // query writer for the relating entity query
    val queryWriter: QueryWriter,
    // key reader for the parent field
    val parentKeyColumnReader: CursorValueReader,
    // key reader for the entity field
    val entityKeyColumnReader: CursorValueReader,
    // adapter for the relating pojo
    val rowAdapter: RowAdapter,
    // parsed relating entity query
    val loadAllQuery: ParsedQuery,
    // true if `relationTypeName` is a Collection, when it is `relationTypeName` is always non null.
    val relationTypeIsCollection: Boolean
) {
    // TODO(b/319660042): Remove once migration to driver API is done.
    fun isMigratedToDriver(): Boolean = rowAdapter.isMigratedToDriver()

    // variable name of map containing keys to relation collections, set when writing the code
    // generator in writeInitCode
    private lateinit var varName: String

    fun writeInitCode(scope: CodeGenScope) {
        varName =
            scope.getTmpVar(
                "_collection${relation.field.getPath().stripNonJava().capitalize(Locale.US)}"
            )
        scope.builder.apply {
            if (
                language == CodeLanguage.JAVA ||
                    mapTypeName.rawTypeName == ARRAY_MAP ||
                    mapTypeName.rawTypeName == LONG_SPARSE_ARRAY
            ) {
                addLocalVariable(
                    name = varName,
                    typeName = mapTypeName,
                    assignExpr = XCodeBlock.ofNewInstance(language, mapTypeName)
                )
            } else {
                addLocalVal(
                    name = varName,
                    typeName = mapTypeName,
                    "%M()",
                    KotlinCollectionMemberNames.MUTABLE_MAP_OF
                )
            }
        }
    }

    // called to extract the key if it exists and adds it to the map of relations to fetch.
    fun writeReadParentKeyCode(
        cursorVarName: String,
        fieldsWithIndices: List<FieldWithIndex>,
        scope: CodeGenScope
    ) {
        val indexVar = fieldsWithIndices.firstOrNull { it.field === relation.parentField }?.indexVar
        checkNotNull(indexVar) {
            "Expected an index var for a column named '${relation.parentField.columnName}' to " +
                "query the '${relation.pojoType}' @Relation but didn't. Please file a bug at " +
                ISSUE_TRACKER_LINK
        }
        scope.builder.apply {
            readKey(cursorVarName, indexVar, parentKeyColumnReader, scope) { tmpVar ->
                // for relation collection put an empty collections in the map, otherwise put nulls
                if (relationTypeIsCollection) {
                    beginControlFlow("if (!%L.containsKey(%L))", varName, tmpVar).apply {
                        val newEmptyCollection =
                            when (language) {
                                CodeLanguage.JAVA ->
                                    XCodeBlock.ofNewInstance(language, relationTypeName)
                                CodeLanguage.KOTLIN ->
                                    XCodeBlock.of(
                                        language = language,
                                        "%M()",
                                        if (relationTypeName == CommonTypeNames.MUTABLE_SET) {
                                            MUTABLE_SET_OF
                                        } else {
                                            MUTABLE_LIST_OF
                                        }
                                    )
                            }
                        addStatement("%L.put(%L, %L)", varName, tmpVar, newEmptyCollection)
                    }
                    endControlFlow()
                } else {
                    addStatement("%L.put(%L, null)", varName, tmpVar)
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
        val indexVar = fieldsWithIndices.firstOrNull { it.field === relation.parentField }?.indexVar
        checkNotNull(indexVar) {
            "Expected an index var for a column named '${relation.parentField.columnName}' to " +
                "query the '${relation.pojoType}' @Relation but didn't. Please file a bug at " +
                ISSUE_TRACKER_LINK
        }
        val tmpVarNameSuffix = if (relationTypeIsCollection) "Collection" else ""
        val tmpRelationVar =
            scope.getTmpVar(
                "_tmp${relation.field.name.stripNonJava().capitalize(Locale.US)}$tmpVarNameSuffix"
            )
        scope.builder.apply {
            addLocalVariable(name = tmpRelationVar, typeName = relationTypeName)
            readKey(
                cursorVarName = cursorVarName,
                indexVar = indexVar,
                keyReader = parentKeyColumnReader,
                scope = scope,
                onKeyReady = { tmpKeyVar ->
                    if (relationTypeIsCollection) {
                        // For Kotlin use getValue() as get() return a nullable value, when the
                        // relation is a collection the map is pre-filled with empty collection
                        // values for all keys, so this is safe. Special case for LongSParseArray
                        // since it does not have a getValue() from Kotlin.
                        val usingLongSparseArray = mapTypeName.rawTypeName == LONG_SPARSE_ARRAY
                        when (language) {
                            CodeLanguage.JAVA ->
                                addStatement("%L = %L.get(%L)", tmpRelationVar, varName, tmpKeyVar)
                            CodeLanguage.KOTLIN ->
                                if (usingLongSparseArray) {
                                    addStatement(
                                        "%L = checkNotNull(%L.get(%L))",
                                        tmpRelationVar,
                                        varName,
                                        tmpKeyVar
                                    )
                                } else {
                                    addStatement(
                                        "%L = %L.getValue(%L)",
                                        tmpRelationVar,
                                        varName,
                                        tmpKeyVar
                                    )
                                }
                        }
                    } else {
                        addStatement("%L = %L.get(%L)", tmpRelationVar, varName, tmpKeyVar)
                        if (language == CodeLanguage.KOTLIN && relation.field.nonNull) {
                            beginControlFlow("if (%L == null)", tmpRelationVar)
                            addStatement(
                                "error(%S)",
                                "Relationship item '${relation.field.name}' was expected to" +
                                    " be NON-NULL but is NULL in @Relation involving " +
                                    "a parent column named '${relation.parentField.columnName}' and " +
                                    "entityColumn named '${relation.entityField.columnName}'."
                            )
                            endControlFlow()
                        }
                    }
                },
                onKeyUnavailable = {
                    if (relationTypeIsCollection) {
                        val newEmptyCollection =
                            when (language) {
                                CodeLanguage.JAVA ->
                                    XCodeBlock.ofNewInstance(language, relationTypeName)
                                CodeLanguage.KOTLIN ->
                                    XCodeBlock.of(
                                        language = language,
                                        "%M()",
                                        if (relationTypeName == CommonTypeNames.MUTABLE_SET) {
                                            MUTABLE_SET_OF
                                        } else {
                                            MUTABLE_LIST_OF
                                        }
                                    )
                            }
                        addStatement("%L = %L", tmpRelationVar, newEmptyCollection)
                    } else {
                        addStatement("%L = null", tmpRelationVar)
                    }
                }
            )
        }
        return tmpRelationVar to relation.field
    }

    // called to write the invocation to the fetch relationship method
    fun writeFetchRelationCall(scope: CodeGenScope) {
        val method =
            scope.writer.getOrCreateFunction(
                RelationCollectorFunctionWriter(this, scope.useDriverApi)
            )
        scope.builder.apply {
            if (scope.useDriverApi) {
                addStatement("%L(%L, %L)", method.name, PARAM_CONNECTION_VARIABLE, varName)
            } else {
                addStatement("%L(%L)", method.name, varName)
            }
        }
    }

    // called to read key and call `onKeyReady` to write code once it is successfully read
    fun readKey(
        cursorVarName: String,
        indexVar: String,
        keyReader: CursorValueReader,
        scope: CodeGenScope,
        onKeyReady: XCodeBlock.Builder.(String) -> Unit
    ) {
        readKey(cursorVarName, indexVar, keyReader, scope, onKeyReady, null)
    }

    // called to read key and call `onKeyReady` to write code once it is successfully read and
    // `onKeyUnavailable` if the key is unavailable (missing column due to bad projection).
    private fun readKey(
        cursorVarName: String,
        indexVar: String,
        keyReader: CursorValueReader,
        scope: CodeGenScope,
        onKeyReady: XCodeBlock.Builder.(String) -> Unit,
        onKeyUnavailable: (XCodeBlock.Builder.() -> Unit)?,
    ) {
        scope.builder.apply {
            val tmpVar = scope.getTmpVar("_tmpKey")
            addLocalVariable(tmpVar, keyReader.typeMirror().asTypeName())
            keyReader.readFromCursor(tmpVar, cursorVarName, indexVar, scope)
            if (keyReader.typeMirror().nullability == XNullability.NONNULL) {
                onKeyReady(tmpVar)
            } else {
                beginControlFlow("if (%L != null)", tmpVar)
                onKeyReady(tmpVar)
                if (onKeyUnavailable != null) {
                    nextControlFlow("else")
                    onKeyUnavailable()
                }
                endControlFlow()
            }
        }
    }

    /**
     * Adapter for binding a LongSparseArray keys into query arguments. This special adapter is only
     * used for binding the relationship query whose keys have INTEGER affinity.
     */
    private class LongSparseArrayKeyQueryParameterAdapter : QueryParameterAdapter(true) {
        override fun bindToStmt(
            inputVarName: String,
            stmtVarName: String,
            startIndexVarName: String,
            scope: CodeGenScope
        ) {
            scope.builder.apply {
                val itrIndexVar = "i"
                val itrItemVar = scope.getTmpVar("_item")
                when (language) {
                    CodeLanguage.JAVA ->
                        beginControlFlow(
                            "for (int %L = 0; %L < %L.size(); i++)",
                            itrIndexVar,
                            itrIndexVar,
                            inputVarName
                        )
                    CodeLanguage.KOTLIN ->
                        beginControlFlow("for (%L in 0 until %L.size())", itrIndexVar, inputVarName)
                }.apply {
                    addLocalVal(
                        itrItemVar,
                        XTypeName.PRIMITIVE_LONG,
                        "%L.keyAt(%L)",
                        inputVarName,
                        itrIndexVar
                    )
                    addStatement("%L.bindLong(%L, %L)", stmtVarName, startIndexVarName, itrItemVar)
                    addStatement("%L++", startIndexVarName)
                }
                endControlFlow()
            }
        }

        override fun getArgCount(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
            scope.builder.addLocalVal(
                outputVarName,
                XTypeName.PRIMITIVE_INT,
                "%L.size()",
                inputVarName
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
            return relations
                .map { relation ->
                    val context =
                        baseContext.fork(
                            element = relation.field.element,
                            forceSuppressedWarnings = setOf(Warning.CURSOR_MISMATCH),
                            forceBuiltInConverters =
                                BuiltInConverterFlags.DEFAULT.copy(
                                    byteBuffer = BuiltInTypeConverters.State.ENABLED
                                )
                        )
                    val affinity = affinityFor(context, relation)
                    val keyTypeName = keyTypeFor(context, affinity)
                    val (relationTypeName, isRelationCollection) =
                        relationTypeFor(context, relation)
                    val tmpMapTypeName =
                        temporaryMapTypeFor(context, affinity, keyTypeName, relationTypeName)

                    val loadAllQuery = relation.createLoadAllSql()
                    val parsedQuery = SqlParser.parse(loadAllQuery)
                    context.checker.check(
                        parsedQuery.errors.isEmpty(),
                        relation.field.element,
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

                    val usingLongSparseArray = tmpMapTypeName.rawTypeName == LONG_SPARSE_ARRAY
                    val queryParam =
                        if (usingLongSparseArray) {
                            val longSparseArrayElement =
                                context.processingEnv.requireTypeElement(
                                    LONG_SPARSE_ARRAY.canonicalName
                                )
                            QueryParameter(
                                name = RelationCollectorFunctionWriter.PARAM_MAP_VARIABLE,
                                sqlName = RelationCollectorFunctionWriter.PARAM_MAP_VARIABLE,
                                type = longSparseArrayElement.type,
                                queryParamAdapter = LONG_SPARSE_ARRAY_KEY_QUERY_PARAM_ADAPTER
                            )
                        } else {
                            val keyTypeMirror = context.processingEnv.requireType(keyTypeName)
                            val set = context.processingEnv.requireTypeElement(CommonTypeNames.SET)
                            val keySet = context.processingEnv.getDeclaredType(set, keyTypeMirror)
                            QueryParameter(
                                name = RelationCollectorFunctionWriter.KEY_SET_VARIABLE,
                                sqlName = RelationCollectorFunctionWriter.KEY_SET_VARIABLE,
                                type = keySet,
                                queryParamAdapter =
                                    context.typeAdapterStore.findQueryParameterAdapter(
                                        typeMirror = keySet,
                                        isMultipleParameter = true
                                    )
                            )
                        }

                    val queryWriter =
                        QueryWriter(
                            parameters = listOf(queryParam),
                            sectionToParamMapping =
                                listOf(Pair(parsedQuery.bindSections.first(), queryParam)),
                            query = parsedQuery
                        )

                    val parentKeyColumnReader =
                        context.typeAdapterStore.findCursorValueReader(
                            output =
                                context.processingEnv.requireType(keyTypeName).let {
                                    if (!relation.parentField.nonNull) it.makeNullable() else it
                                },
                            affinity = affinity
                        )
                    val entityKeyColumnReader =
                        context.typeAdapterStore.findCursorValueReader(
                            output =
                                context.processingEnv.requireType(keyTypeName).let { keyType ->
                                    if (!relation.entityField.nonNull) keyType.makeNullable()
                                    else keyType
                                },
                            affinity = affinity
                        )
                    // We should always find a readers since key types all have built in converters
                    check(parentKeyColumnReader != null && entityKeyColumnReader != null) {
                        "Missing one of the relation key value reader for type $keyTypeName"
                    }

                    // row adapter that matches full response
                    fun getDefaultRowAdapter(): RowAdapter? {
                        return context.typeAdapterStore.findRowAdapter(
                            relation.pojoType,
                            parsedQuery
                        )
                    }
                    val rowAdapter =
                        if (
                            relation.projection.size == 1 &&
                                resultInfo != null &&
                                (resultInfo.columns.size == 1 || resultInfo.columns.size == 2)
                        ) {
                            // check for a column adapter first
                            val cursorReader =
                                context.typeAdapterStore.findCursorValueReader(
                                    relation.pojoType,
                                    resultInfo.columns.first().type
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
                            ProcessorErrors.cannotFindQueryResultAdapter(
                                relation.pojoType.asTypeName().toString(context.codeLanguage)
                            )
                        )
                        null
                    } else {
                        RelationCollector(
                            relation = relation,
                            affinity = affinity,
                            mapTypeName = tmpMapTypeName,
                            keyTypeName = keyTypeName,
                            relationTypeName = relationTypeName,
                            queryWriter = queryWriter,
                            parentKeyColumnReader = parentKeyColumnReader,
                            entityKeyColumnReader = entityKeyColumnReader,
                            rowAdapter = rowAdapter,
                            loadAllQuery = parsedQuery,
                            relationTypeIsCollection = isRelationCollection
                        )
                    }
                }
                .filterNotNull()
        }

        // Gets and check the affinity of the relating columns.
        private fun affinityFor(context: Context, relation: Relation): SQLTypeAffinity {
            fun checkAffinity(
                first: SQLTypeAffinity?,
                second: SQLTypeAffinity?,
                onAffinityMismatch: () -> Unit
            ) =
                if (first != null && first == second) {
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
                        Warning.RELATION_TYPE_MISMATCH,
                        relation.field.element,
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
                        Warning.RELATION_TYPE_MISMATCH,
                        relation.field.element,
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
                        Warning.RELATION_TYPE_MISMATCH,
                        relation.field.element,
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
        private fun relationTypeFor(context: Context, relation: Relation) =
            relation.field.type.let { fieldType ->
                if (fieldType.typeArguments.isNotEmpty()) {
                    val rawType = fieldType.rawType
                    val setType = context.processingEnv.requireType(CommonTypeNames.SET)
                    val paramTypeName =
                        if (setType.rawType.isAssignableFrom(rawType)) {
                            when (context.codeLanguage) {
                                CodeLanguage.KOTLIN ->
                                    CommonTypeNames.MUTABLE_SET.parametrizedBy(
                                        relation.pojoTypeName
                                    )
                                CodeLanguage.JAVA -> HASH_SET.parametrizedBy(relation.pojoTypeName)
                            }
                        } else {
                            when (context.codeLanguage) {
                                CodeLanguage.KOTLIN ->
                                    CommonTypeNames.MUTABLE_LIST.parametrizedBy(
                                        relation.pojoTypeName
                                    )
                                CodeLanguage.JAVA ->
                                    ARRAY_LIST.parametrizedBy(relation.pojoTypeName)
                            }
                        }
                    paramTypeName to true
                } else {
                    relation.pojoTypeName.copy(nullable = true) to false
                }
            }

        // Gets the type name of the temporary key map.
        private fun temporaryMapTypeFor(
            context: Context,
            affinity: SQLTypeAffinity,
            keyTypeName: XTypeName,
            valueTypeName: XTypeName,
        ): XTypeName {
            val canUseLongSparseArray =
                context.processingEnv.findTypeElement(LONG_SPARSE_ARRAY.canonicalName) != null
            val canUseArrayMap =
                context.processingEnv.findTypeElement(ARRAY_MAP.canonicalName) != null &&
                    context.isAndroidOnlyTarget()
            return when {
                canUseLongSparseArray && affinity == SQLTypeAffinity.INTEGER ->
                    LONG_SPARSE_ARRAY.parametrizedBy(valueTypeName)
                canUseArrayMap -> ARRAY_MAP.parametrizedBy(keyTypeName, valueTypeName)
                else ->
                    when (context.codeLanguage) {
                        CodeLanguage.JAVA -> HASH_MAP.parametrizedBy(keyTypeName, valueTypeName)
                        CodeLanguage.KOTLIN ->
                            CommonTypeNames.MUTABLE_MAP.parametrizedBy(keyTypeName, valueTypeName)
                    }
            }
        }

        // Gets the type name of the relationship key.
        private fun keyTypeFor(context: Context, affinity: SQLTypeAffinity): XTypeName {
            val canUseLongSparseArray =
                context.processingEnv.findTypeElement(LONG_SPARSE_ARRAY.canonicalName) != null
            return when (affinity) {
                SQLTypeAffinity.INTEGER ->
                    if (canUseLongSparseArray) {
                        XTypeName.PRIMITIVE_LONG
                    } else {
                        XTypeName.BOXED_LONG
                    }
                SQLTypeAffinity.REAL -> XTypeName.BOXED_DOUBLE
                SQLTypeAffinity.TEXT -> CommonTypeNames.STRING
                SQLTypeAffinity.BLOB -> CommonTypeNames.BYTE_BUFFER
                else -> {
                    // no affinity default to String
                    CommonTypeNames.STRING
                }
            }
        }
    }
}
