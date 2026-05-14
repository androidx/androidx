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

package androidx.room3.vo

import androidx.room3.BuiltInTypeConverters
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.CollectionTypeNames.ARRAY_MAP
import androidx.room3.ext.CollectionTypeNames.LONG_SPARSE_ARRAY
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.KotlinCollectionMemberNames
import androidx.room3.ext.KotlinCollectionMemberNames.MUTABLE_LIST_OF
import androidx.room3.ext.KotlinCollectionMemberNames.MUTABLE_SET_OF
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.RoomTypeNames.BYTE_ARRAY_WRAPPER
import androidx.room3.ext.capitalize
import androidx.room3.ext.stripNonJava
import androidx.room3.parser.ParsedQuery
import androidx.room3.parser.SQLTypeAffinity
import androidx.room3.parser.SqlParser
import androidx.room3.processor.Context
import androidx.room3.processor.ProcessorErrors
import androidx.room3.processor.ProcessorErrors.ISSUE_TRACKER_LINK
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.query.parameter.CompositeKeyQueryParameterAdapter
import androidx.room3.solver.query.parameter.LongSparseArrayKeyQueryParameterAdapter
import androidx.room3.solver.query.result.RowAdapter
import androidx.room3.solver.query.result.SingleColumnRowAdapter
import androidx.room3.solver.types.StatementValueReader
import androidx.room3.verifier.DatabaseVerificationErrors
import androidx.room3.writer.RelationCollectorFunctionWriter
import androidx.room3.writer.RelationCollectorFunctionWriter.Companion.PARAM_CONNECTION_VARIABLE
import androidx.room3.writer.RelationQueryWriter
import java.util.Locale

/** Internal class that is used to manage fetching 1/N to N relationships. */
data class RelationCollector(
    val relation: Relation,
    // affinities between relation properties
    val affinities: List<SQLTypeAffinity>,
    // concrete map type name to store relationship
    val mapTypeName: XTypeName,
    // map key type name, not the same as the parent or entity field type
    val keyTypeName: XTypeName,
    // map value type name, it is assignable to the @Relation field
    val relationTypeName: XTypeName,
    // query writer for the relating entity query
    val relationQueryWriter: RelationQueryWriter,
    // key readers for the parent properties
    val parentKeyColumnReaders: List<StatementValueReader>,
    // key readers for the entity properties
    val entityKeyColumnReaders: List<StatementValueReader>,
    // adapter for the relating data class
    val rowAdapter: RowAdapter,
    // parsed relating entity query
    val loadAllQuery: ParsedQuery,
    // true if `relationTypeName` is a Collection, when it is `relationTypeName` is always non null.
    val relationTypeIsCollection: Boolean,
) {
    // variable name of map containing keys to relation collections, set when writing the code
    // generator in writeInitCode
    private lateinit var varName: String

    fun writeInitCode(scope: CodeGenScope) {
        varName =
            scope.getTmpVar(
                "_collection${relation.property.getPath().stripNonJava().capitalize(Locale.US)}"
            )
        scope.builder.applyTo { language ->
            if (
                mapTypeName.rawTypeName == ARRAY_MAP || mapTypeName.rawTypeName == LONG_SPARSE_ARRAY
            ) {
                addLocalVariable(
                    name = varName,
                    typeName = mapTypeName,
                    assignExpr = XCodeBlock.ofNewInstance(mapTypeName),
                )
            } else {
                addLocalVal(
                    name = varName,
                    typeName = mapTypeName,
                    "%M()",
                    KotlinCollectionMemberNames.MUTABLE_MAP_OF,
                )
            }
        }
    }

    // called to extract the key if it exists and adds it to the map of relations to fetch.
    fun writeReadParentKeyCode(
        stmtVarName: String,
        propertiesWithIndices: List<PropertyWithIndex>,
        scope: CodeGenScope,
    ) {
        val indexVars = getIndexVars(propertiesWithIndices)
        scope.builder.apply {
            readKey(stmtVarName, indexVars, parentKeyColumnReaders, scope) { tmpVar ->
                // for relation collection put an empty collections in the map, otherwise put nulls
                if (relationTypeIsCollection) {
                    beginControlFlow("if (!%L.containsKey(%L))", varName, tmpVar).apply {
                        val newEmptyCollection =
                            if (relationTypeName.rawTypeName == CommonTypeNames.MUTABLE_SET) {
                                XCodeBlock.of("%M()", MUTABLE_SET_OF)
                            } else {
                                XCodeBlock.of("%M()", MUTABLE_LIST_OF)
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
        stmtVarName: String,
        propertiesWithIndices: List<PropertyWithIndex>,
        scope: CodeGenScope,
    ): Pair<String, Property> {
        val indexVars = getIndexVars(propertiesWithIndices)
        val tmpVarNameSuffix = if (relationTypeIsCollection) "Collection" else ""
        val tmpRelationVar =
            scope.getTmpVar(
                "_tmp${relation.property.name.stripNonJava().capitalize(Locale.US)}$tmpVarNameSuffix"
            )
        scope.builder.apply {
            addLocalVariable(name = tmpRelationVar, typeName = relationTypeName)
            readKey(
                stmtVarName = stmtVarName,
                indexVars = indexVars,
                keyReaders = parentKeyColumnReaders,
                scope = scope,
                onKeyReady = { tmpKeyVar ->
                    if (relationTypeIsCollection) {
                        // For Kotlin use getValue() as get() return a nullable value, when the
                        // relation is a collection the map is pre-filled with empty collection
                        // values for all keys, so this is safe. Special case for LongSparseArray
                        // since it does not have a getValue() from Kotlin.
                        val usingLongSparseArray = mapTypeName.rawTypeName == LONG_SPARSE_ARRAY
                        if (usingLongSparseArray) {
                            addStatement(
                                "%L = checkNotNull(%L.get(%L))",
                                tmpRelationVar,
                                varName,
                                tmpKeyVar,
                            )
                        } else {
                            addStatement("%L = %L.getValue(%L)", tmpRelationVar, varName, tmpKeyVar)
                        }
                    } else {
                        addStatement("%L = %L.get(%L)", tmpRelationVar, varName, tmpKeyVar)
                        if (relation.property.nonNull) {
                            applyTo(CodeLanguage.KOTLIN) {
                                beginControlFlow("if (%L == null)", tmpRelationVar)
                                val parentColumnNames =
                                    relation.parentProperties.joinToString(separator = ", ") {
                                        "'${it.columnName}'"
                                    }
                                val entityColumnNames =
                                    relation.entityProperties.joinToString(separator = ", ") {
                                        "'${it.columnName}'"
                                    }
                                addStatement(
                                    "error(%S)",
                                    "Relationship item '${relation.property.name}' was expected to" +
                                        " be NON-NULL but is NULL in @Relation involving " +
                                        "parent columns named $parentColumnNames and " +
                                        "entityColumns named $entityColumnNames'.",
                                )
                                endControlFlow()
                            }
                        }
                    }
                },
                onKeyUnavailable = {
                    if (relationTypeIsCollection) {
                        val newEmptyCollection =
                            if (relationTypeName.rawTypeName == CommonTypeNames.MUTABLE_SET) {
                                XCodeBlock.of("%M()", MUTABLE_SET_OF)
                            } else {
                                XCodeBlock.of("%M()", MUTABLE_LIST_OF)
                            }
                        addStatement("%L = %L", tmpRelationVar, newEmptyCollection)
                    } else {
                        addStatement("%L = null", tmpRelationVar)
                    }
                },
            )
        }
        return tmpRelationVar to relation.property
    }

    private fun getIndexVars(propertiesWithIndices: List<PropertyWithIndex>) =
        relation.parentProperties.map { prop ->
            val indexVar = propertiesWithIndices.firstOrNull { it.property === prop }?.indexVar
            checkNotNull(indexVar) {
                "Expected an index var for a column named '${prop.columnName}' to " +
                    "query the '${relation.dataClassType}' @Relation but didn't." +
                    "Please file a bug at $ISSUE_TRACKER_LINK"
            }
            indexVar
        }

    // called to write the invocation to the fetch relationship function
    fun writeFetchRelationCall(scope: CodeGenScope) {
        val function = scope.writer.getOrCreateFunction(RelationCollectorFunctionWriter(this))
        scope.builder.apply {
            addStatement("%L(%L, %L)", function.name, PARAM_CONNECTION_VARIABLE, varName)
        }
    }

    // called to read key and call `onKeyReady` to write code once it is successfully read
    fun readKey(
        stmtVarName: String,
        indexVars: List<String>,
        keyReaders: List<StatementValueReader>,
        scope: CodeGenScope,
        onKeyReady: XCodeBlock.Builder.(String) -> Unit,
    ) {
        readKey(stmtVarName, indexVars, keyReaders, scope, onKeyReady, null)
    }

    // called to read key and call `onKeyReady` to write code once it is successfully read and
    // `onKeyUnavailable` if the key is unavailable (missing column due to bad projection).
    private fun readKey(
        stmtVarName: String,
        indexVars: List<String>,
        keyReaders: List<StatementValueReader>,
        scope: CodeGenScope,
        onKeyReady: XCodeBlock.Builder.(String) -> Unit,
        onKeyUnavailable: (XCodeBlock.Builder.() -> Unit)?,
    ) {
        if (keyReaders.size == 1) {
            readSingleKey(
                stmtVarName,
                indexVars.single(),
                keyReaders.single(),
                scope,
                onKeyReady,
                onKeyUnavailable,
            )
        } else {
            readCompositeKey(
                stmtVarName,
                indexVars,
                keyReaders,
                scope,
                onKeyReady,
                onKeyUnavailable,
            )
        }
    }

    private fun readSingleKey(
        stmtVarName: String,
        indexVar: String,
        reader: StatementValueReader,
        scope: CodeGenScope,
        onKeyReady: XCodeBlock.Builder.(String) -> Unit,
        onKeyUnavailable: (XCodeBlock.Builder.() -> Unit)?,
    ) {
        scope.builder.apply {
            val tmpVar = scope.getTmpVar("_tmpKey")
            addLocalVariable(tmpVar, reader.typeMirror().asTypeName())
            reader.readFromStatement(tmpVar, stmtVarName, indexVar, scope)
            if (reader.typeMirror().nullability == XNullability.NONNULL) {
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

    private fun readCompositeKey(
        stmtVarName: String,
        indexVars: List<String>,
        keyReaders: List<StatementValueReader>,
        scope: CodeGenScope,
        onKeyReady: XCodeBlock.Builder.(String) -> Unit,
        onKeyUnavailable: (XCodeBlock.Builder.() -> Unit)?,
    ) {
        scope.builder.apply {
            val tmpVars =
                keyReaders.mapIndexed { index, reader ->
                    val tmpVar = scope.getTmpVar("_tmpPartialKey_$index")
                    addLocalVariable(tmpVar, reader.typeMirror().asTypeName())
                    reader.readFromStatement(tmpVar, stmtVarName, indexVars[index], scope)
                    tmpVar
                }
            val nonNullConditions =
                tmpVars.mapIndexedNotNull { index, tmpVar ->
                    if (keyReaders[index].typeMirror().nullability != XNullability.NONNULL) {
                        "$tmpVar != null"
                    } else {
                        null
                    }
                }

            fun createCompositeKey(builder: XCodeBlock.Builder): String {
                val keyVar = scope.getTmpVar("_compositeKey")
                builder.apply {
                    when (tmpVars.size) {
                        2 ->
                            addLocalVal(
                                keyVar,
                                keyTypeName,
                                "%T(%L, %L)",
                                KotlinTypeNames.PAIR,
                                tmpVars[0],
                                tmpVars[1],
                            )
                        3 ->
                            addLocalVal(
                                keyVar,
                                keyTypeName,
                                "%T(%L, %L, %L)",
                                KotlinTypeNames.TRIPLE,
                                tmpVars[0],
                                tmpVars[1],
                                tmpVars[2],
                            )
                        else ->
                            addLocalVal(
                                keyVar,
                                keyTypeName,
                                "%M(%L)",
                                KotlinCollectionMemberNames.LIST_OF,
                                tmpVars.joinToString(", "),
                            )
                    }
                }
                return keyVar
            }

            if (nonNullConditions.isEmpty()) {
                val keyVar = createCompositeKey(this)
                onKeyReady(keyVar)
            } else {
                beginControlFlow("if (%L)", nonNullConditions.joinToString(" && "))
                val keyVar = createCompositeKey(this)
                onKeyReady(keyVar)
                if (onKeyUnavailable != null) {
                    nextControlFlow("else")
                    onKeyUnavailable()
                }
                endControlFlow()
            }
        }
    }

    companion object {

        fun createCollectors(
            baseContext: Context,
            relations: List<Relation>,
        ): List<RelationCollector> = relations.mapNotNull { createCollector(baseContext, it) }

        private fun createCollector(baseContext: Context, relation: Relation): RelationCollector? {
            val context =
                baseContext.fork(
                    element = relation.property.element,
                    forceSuppressedWarnings = setOf(Warning.QUERY_MISMATCH),
                    forceBuiltInConverters =
                        BuiltInConverterFlags.DEFAULT.copy(
                            byteBuffer = BuiltInTypeConverters.State.ENABLED
                        ),
                )
            val canUseLongSparseArray =
                context.processingEnv.findTypeElement(LONG_SPARSE_ARRAY.canonicalName) != null &&
                    !relation.hasCompositeKey
            val affinities = affinityFor(context, relation)
            val affinityTypeNames = affinities.map { singleKeyTypeFor(canUseLongSparseArray, it) }
            val affinityTypes = affinityTypeNames.map { context.processingEnv.requireType(it) }
            val keyTypeName = keyTypeFor(affinityTypeNames)
            val (relationTypeName, isRelationCollection) = relationTypeFor(context, relation)
            val tmpMapTypeName =
                temporaryMapTypeFor(context, affinities, keyTypeName, relationTypeName)
            val queryParam = queryParameterFor(context, tmpMapTypeName, affinities, affinityTypes)
            val relationQueryWriter = RelationQueryWriter(relation, queryParam)
            val loadAllQuery = relationQueryWriter.createLoadAllSql()
            val parsedQuery = SqlParser.parse(loadAllQuery)
            context.checker.check(
                parsedQuery.errors.isEmpty(),
                relation.property.element,
                parsedQuery.errors.joinToString("\n"),
            )
            if (parsedQuery.errors.isEmpty()) {
                val resultInfo = context.databaseVerifier?.analyze(loadAllQuery)
                parsedQuery.resultInfo = resultInfo
                if (resultInfo?.error != null) {
                    context.logger.e(
                        relation.property.element,
                        DatabaseVerificationErrors.cannotVerifyQuery(resultInfo.error),
                    )
                }
            }
            val resultInfo = parsedQuery.resultInfo

            val parentKeyColumnReaders =
                relation.parentProperties.mapIndexed { index, prop ->
                    context.typeAdapterStore.findStatementValueReader(
                        output =
                            affinityTypes[index].let {
                                if (!prop.nonNull) it.makeNullable() else it
                            },
                        affinity = affinities[index],
                    )
                }
            val entityKeyColumnReaders =
                relation.entityProperties.mapIndexed { index, prop ->
                    context.typeAdapterStore.findStatementValueReader(
                        output =
                            affinityTypes[index].let {
                                if (!prop.nonNull) it.makeNullable() else it
                            },
                        affinity = affinities[index],
                    )
                }
            if (
                parentKeyColumnReaders.any { it == null } ||
                    entityKeyColumnReaders.any { it == null }
            ) {
                // We couldn't find readers. This might be due to affinity mismatch.
                // We already reported a warning if it was affinity mismatch.
                // Let's skip this relation collector instead of crashing.
                return null
            }

            // row adapter that matches full response
            fun getDefaultRowAdapter(): RowAdapter? {
                return context.typeAdapterStore.findRowAdapter(relation.dataClassType, parsedQuery)
            }

            val rowAdapter =
                if (
                    relation.projection.size == 1 &&
                        resultInfo != null &&
                        resultInfo.columns.isNotEmpty()
                ) {
                    // check for a column adapter first
                    val statementReader =
                        context.typeAdapterStore.findStatementValueReader(
                            relation.dataClassType,
                            resultInfo.columns.first().type,
                        )
                    if (statementReader == null) {
                        getDefaultRowAdapter()
                    } else {
                        SingleColumnRowAdapter(statementReader)
                    }
                } else {
                    getDefaultRowAdapter()
                }

            if (rowAdapter == null) {
                context.logger.e(
                    relation.property.element,
                    ProcessorErrors.cannotFindQueryResultAdapter(
                        relation.dataClassType.asTypeName().toString(context.codeLanguage)
                    ),
                )
                return null
            } else {
                return RelationCollector(
                    relation = relation,
                    affinities = affinities,
                    mapTypeName = tmpMapTypeName,
                    keyTypeName = keyTypeName,
                    relationTypeName = relationTypeName,
                    relationQueryWriter = relationQueryWriter,
                    parentKeyColumnReaders = parentKeyColumnReaders.requireNoNulls(),
                    entityKeyColumnReaders = entityKeyColumnReaders.requireNoNulls(),
                    rowAdapter = rowAdapter,
                    loadAllQuery = parsedQuery,
                    relationTypeIsCollection = isRelationCollection,
                )
            }
        }

        // Gets and check the affinity of the relating columns.
        private fun affinityFor(context: Context, relation: Relation): List<SQLTypeAffinity> {
            fun checkAffinity(
                first: SQLTypeAffinity?,
                second: SQLTypeAffinity?,
                onAffinityMismatch: () -> Unit,
            ) =
                if (first != null && first == second) {
                    first
                } else {
                    onAffinityMismatch()
                    SQLTypeAffinity.TEXT
                }

            return if (relation.junction != null) {
                relation.entityProperties.forEachIndexed { index, childProp ->
                    val childAffinity = childProp.statementValueReader?.affinity()
                    val junctionChildProp = relation.junction.entityProperties[index]
                    val junctionChildAffinity = junctionChildProp.statementValueReader?.affinity()
                    checkAffinity(childAffinity, junctionChildAffinity) {
                        context.logger.w(
                            Warning.RELATION_TYPE_MISMATCH,
                            relation.property.element,
                            ProcessorErrors.relationJunctionChildAffinityMismatch(
                                childColumn = childProp.columnName,
                                junctionChildColumn = junctionChildProp.columnName,
                                childAffinity = childAffinity,
                                junctionChildAffinity = junctionChildAffinity,
                            ),
                        )
                    }
                }
                relation.parentProperties.mapIndexed { index, parentProp ->
                    val parentAffinity = parentProp.statementValueReader?.affinity()
                    val junctionParentProp = relation.junction.parentProperties[index]
                    val junctionParentAffinity = junctionParentProp.statementValueReader?.affinity()
                    checkAffinity(parentAffinity, junctionParentAffinity) {
                        context.logger.w(
                            Warning.RELATION_TYPE_MISMATCH,
                            relation.property.element,
                            ProcessorErrors.relationJunctionParentAffinityMismatch(
                                parentColumn = parentProp.columnName,
                                junctionParentColumn = junctionParentProp.columnName,
                                parentAffinity = parentAffinity,
                                junctionParentAffinity = junctionParentAffinity,
                            ),
                        )
                    }
                }
            } else {
                relation.parentProperties.mapIndexed { index, parentProp ->
                    val parentAffinity = parentProp.statementValueReader?.affinity()
                    val childProp = relation.entityProperties[index]
                    val childAffinity = childProp.statementValueReader?.affinity()
                    checkAffinity(parentAffinity, childAffinity) {
                        context.logger.w(
                            Warning.RELATION_TYPE_MISMATCH,
                            relation.property.element,
                            ProcessorErrors.relationAffinityMismatch(
                                parentColumn = parentProp.columnName,
                                childColumn = childProp.columnName,
                                parentAffinity = parentAffinity,
                                childAffinity = childAffinity,
                            ),
                        )
                    }
                }
            }
        }

        // Gets the resulting relation type name. (i.e. @Relation property type name.)
        private fun relationTypeFor(context: Context, relation: Relation) =
            relation.property.type.let { propertyType ->
                if (propertyType.typeArguments.isNotEmpty()) {
                    val rawType = propertyType.rawType
                    val setType = context.processingEnv.requireType(CommonTypeNames.MUTABLE_SET)
                    val paramTypeName =
                        if (rawType.isAssignableFrom(setType.rawType)) {
                            CommonTypeNames.MUTABLE_SET.parametrizedBy(relation.dataClassTypeName)
                        } else {
                            CommonTypeNames.MUTABLE_LIST.parametrizedBy(relation.dataClassTypeName)
                        }
                    paramTypeName to true
                } else {
                    relation.dataClassTypeName.copy(nullable = true) to false
                }
            }

        // Gets the type name of the temporary key map.
        private fun temporaryMapTypeFor(
            context: Context,
            affinities: List<SQLTypeAffinity>,
            keyTypeName: XTypeName,
            valueTypeName: XTypeName,
        ): XTypeName {
            val canUseLongSparseArray =
                context.processingEnv.findTypeElement(LONG_SPARSE_ARRAY.canonicalName) != null
            val canUseArrayMap =
                context.processingEnv.findTypeElement(ARRAY_MAP.canonicalName) != null &&
                    context.isAndroidOnlyTarget()
            val singleAffinity = affinities.singleOrNull()
            return when {
                canUseLongSparseArray && singleAffinity == SQLTypeAffinity.INTEGER ->
                    LONG_SPARSE_ARRAY.parametrizedBy(valueTypeName)
                canUseArrayMap && singleAffinity != null ->
                    ARRAY_MAP.parametrizedBy(keyTypeName, valueTypeName)
                else -> CommonTypeNames.MUTABLE_MAP.parametrizedBy(keyTypeName, valueTypeName)
            }
        }

        // Gets the type name of the relationship key as a whole, for single key relations, it is
        // the same as the single column, for composite keys it is optimized to use Pair or Triple
        // when composite keys are double or triple respectively, otherwise fallback to List<Any?>
        private fun keyTypeFor(types: List<XTypeName>): XTypeName {
            return when (types.size) {
                1 -> types.single()
                2 -> KotlinTypeNames.PAIR.parametrizedBy(types[0], types[1])
                3 -> KotlinTypeNames.TRIPLE.parametrizedBy(types[0], types[1], types[2])
                else -> CommonTypeNames.LIST.parametrizedBy(XTypeName.ANY_WILDCARD)
            }
        }

        // Gets the type name of a single relationship key column
        private fun singleKeyTypeFor(
            canUseLongSparseArray: Boolean,
            affinity: SQLTypeAffinity,
        ): XTypeName {
            return when (affinity) {
                SQLTypeAffinity.INTEGER ->
                    if (canUseLongSparseArray) {
                        XTypeName.PRIMITIVE_LONG
                    } else {
                        XTypeName.BOXED_LONG
                    }
                SQLTypeAffinity.REAL -> XTypeName.BOXED_DOUBLE
                SQLTypeAffinity.TEXT -> CommonTypeNames.STRING
                SQLTypeAffinity.BLOB -> BYTE_ARRAY_WRAPPER
                else -> {
                    // no affinity default to String
                    CommonTypeNames.STRING
                }
            }
        }

        // Gets the query parameters and adapter for the generated relationship query.
        private fun queryParameterFor(
            context: Context,
            tmpMapTypeName: XTypeName,
            affinities: List<SQLTypeAffinity>,
            affinityTypes: List<XType>,
        ): QueryParameter {
            return if (tmpMapTypeName.rawTypeName == LONG_SPARSE_ARRAY) {
                val longSparseArrayElement =
                    context.processingEnv.requireTypeElement(LONG_SPARSE_ARRAY.canonicalName)
                QueryParameter(
                    name = RelationCollectorFunctionWriter.PARAM_MAP_VARIABLE,
                    sqlName = RelationCollectorFunctionWriter.PARAM_MAP_VARIABLE,
                    type = longSparseArrayElement.type,
                    queryParamAdapter = LongSparseArrayKeyQueryParameterAdapter(),
                )
            } else {
                val keyTypeMirror =
                    when (affinityTypes.size) {
                        1 -> affinityTypes.single()
                        2 -> {
                            val pairElement =
                                context.processingEnv.requireTypeElement(KotlinTypeNames.PAIR)
                            context.processingEnv.getDeclaredType(
                                pairElement,
                                *affinityTypes.toTypedArray(),
                            )
                        }
                        3 -> {
                            val tripleElement =
                                context.processingEnv.requireTypeElement(KotlinTypeNames.TRIPLE)
                            context.processingEnv.getDeclaredType(
                                tripleElement,
                                *affinityTypes.toTypedArray(),
                            )
                        }
                        else -> {
                            val listElement =
                                context.processingEnv.requireTypeElement(CommonTypeNames.LIST)
                            val anyType =
                                context.processingEnv.requireType(
                                    KotlinTypeNames.ANY.copy(nullable = true)
                                )
                            context.processingEnv.getDeclaredType(listElement, anyType)
                        }
                    }
                val set = context.processingEnv.requireTypeElement(CommonTypeNames.SET)
                val keySet = context.processingEnv.getDeclaredType(set, keyTypeMirror)
                val queryParamAdapter =
                    if (affinityTypes.size > 1) {
                        val valueBinders =
                            affinityTypes.mapIndexedNotNull { index, type ->
                                context.typeAdapterStore.findStatementValueBinder(
                                    type,
                                    affinities[index],
                                )
                            }
                        CompositeKeyQueryParameterAdapter(valueBinders)
                    } else {
                        context.typeAdapterStore.findQueryParameterAdapter(
                            typeMirror = keySet,
                            isMultipleParameter = true,
                        )
                    }
                QueryParameter(
                    name = RelationCollectorFunctionWriter.KEY_SET_VARIABLE,
                    sqlName = RelationCollectorFunctionWriter.KEY_SET_VARIABLE,
                    type = keySet,
                    queryParamAdapter = queryParamAdapter,
                )
            }
        }
    }
}
