/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.query.result.MultimapQueryResultAdapter.MapType.Companion.isSparseArray
import androidx.room.vo.ColumnIndexVar

/**
 * This is an intermediary adapter class that enables nested multimap return types in DAOs.
 *
 * The [MapValueResultAdapter] sealed class is extended by 2 classes, [NestedMapValueResultAdapter]
 * and [EndMapValueResultAdapter]. These adapters are wrappers for the adapters at different levels
 * of nested maps. Each level of nesting of a map is represented by a [NestedMapValueResultAdapter],
 * except the innermost level which is represented by an [EndMapValueResultAdapter].
 *
 * For example, if a DAO method returns a `Map<A, Map<B, Map<C, D>>>`, `Map<C, D>` is represented
 * by an [EndMapValueResultAdapter], and the outer 2 levels are represented by a
 * [NestedMapValueResultAdapter] each.
 *
 * A [NestedMapValueResultAdapter] can wrap either another [NestedMapValueResultAdapter] or an
 * [EndMapValueResultAdapter], whereas an [EndMapValueResultAdapter] does not wrap another adapter
 * and only contains row adapters for the innermost map.
 */
sealed class MapValueResultAdapter(
    val rowAdapters: List<RowAdapter>
) {

    /**
     * True if this adapters requires key checking due to its values being passed by reference.
     */
    abstract fun requiresContainsKeyCheck(): Boolean

    /**
     * Left-Hand-Side of a Map value type arg initialization.
     */
    abstract fun getDeclarationTypeName(): XTypeName

    /**
     * Right-Hand-Side of a Map value type arg initialization.
     */
    abstract fun getInstantiationTypeName(): XTypeName

    abstract fun convert(
        scope: CodeGenScope,
        valuesVarName: String,
        cursorVarName: String,
        dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?,
        genPutValueCode: (String, Boolean) -> Unit = { _, _ -> }
    )

    abstract fun generateContinueColumnCheck(
        scope: CodeGenScope,
        cursorVarName: String,
        dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?
    )

    /**
     * A [NestedMapValueResultAdapter] contains the key information and the value map information
     * of any level of a nested map that is not the innermost "End" map.
     *
     * The [convert] function implementation for a [NestedMapValueResultAdapter] generates code that
     * resolves the key of the map and delegates to the value map's [NestedMapValueResultAdapter] or
     * [EndMapValueResultAdapter] (based on the level of nesting) to resolve the value map
     * conversion.
     */
    class NestedMapValueResultAdapter(
        private val keyRowAdapter: RowAdapter,
        private val keyTypeArg: XType,
        private val mapType: MultimapQueryResultAdapter.MapType,
        private val mapValueResultAdapter: MapValueResultAdapter
    ) : MapValueResultAdapter(
        rowAdapters = listOf(keyRowAdapter) + mapValueResultAdapter.rowAdapters
    ) {

        private val keyTypeName = keyTypeArg.asTypeName()

        override fun requiresContainsKeyCheck(): Boolean = true

        override fun getDeclarationTypeName() = when (val typeOfMap = this.mapType) {
            MultimapQueryResultAdapter.MapType.DEFAULT,
            MultimapQueryResultAdapter.MapType.ARRAY_MAP ->
                typeOfMap.className.parametrizedBy(
                    keyTypeName,
                    mapValueResultAdapter.getDeclarationTypeName()
                )

            MultimapQueryResultAdapter.MapType.LONG_SPARSE,
            MultimapQueryResultAdapter.MapType.INT_SPARSE ->
                typeOfMap.className.parametrizedBy(
                    mapValueResultAdapter.getDeclarationTypeName()
                )
        }

        override fun getInstantiationTypeName() = when (val typeOfMap = this.mapType) {
            MultimapQueryResultAdapter.MapType.DEFAULT ->
                // LinkedHashMap is used as impl to preserve key ordering for ordered
                // query results.
                CommonTypeNames.LINKED_HASH_MAP.parametrizedBy(
                    keyTypeName,
                    mapValueResultAdapter.getDeclarationTypeName()
                )

            MultimapQueryResultAdapter.MapType.ARRAY_MAP ->
                typeOfMap.className.parametrizedBy(
                    keyTypeName,
                    mapValueResultAdapter.getDeclarationTypeName()
                )

            MultimapQueryResultAdapter.MapType.LONG_SPARSE,
            MultimapQueryResultAdapter.MapType.INT_SPARSE ->
                typeOfMap.className.parametrizedBy(
                    mapValueResultAdapter.getDeclarationTypeName()
                )
        }

        override fun convert(
            scope: CodeGenScope,
            valuesVarName: String,
            cursorVarName: String,
            dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?,
            genPutValueCode: (String, Boolean) -> Unit
        ) {
            scope.builder.apply {
                // Read map key
                val tmpKeyVarName = scope.getTmpVar("_key")
                addLocalVariable(tmpKeyVarName, keyTypeArg.asTypeName())
                keyRowAdapter.convert(tmpKeyVarName, cursorVarName, scope)

                // Generate map key check if the next value adapter is by reference
                // (nested map case or collection end value)
                @Suppress("NAME_SHADOWING") // On purpose to avoid miss using param
                val valuesVarName = if (mapValueResultAdapter.requiresContainsKeyCheck()) {
                    scope.getTmpVar("_values").also { tmpValuesVarName ->
                        addLocalVariable(
                            tmpValuesVarName,
                            mapValueResultAdapter.getDeclarationTypeName()
                        )
                        if (mapType.isSparseArray()) {
                            beginControlFlow(
                                "if (%L.get(%L) != null)",
                                valuesVarName,
                                tmpKeyVarName
                            )
                        } else {
                            beginControlFlow(
                                "if (%L.containsKey(%L))",
                                valuesVarName,
                                tmpKeyVarName
                            )
                        }.apply {
                            val getFunction = when (language) {
                                CodeLanguage.JAVA ->
                                    "get"
                                CodeLanguage.KOTLIN ->
                                    if (mapType.isSparseArray()) "get" else "getValue"
                            }
                            addStatement(
                                "%L = %L.%L(%L)",
                                tmpValuesVarName,
                                valuesVarName,
                                getFunction,
                                tmpKeyVarName
                            )
                        }.nextControlFlow("else").apply {
                            addStatement(
                                "%L = %L",
                                tmpValuesVarName,
                                XCodeBlock.ofNewInstance(
                                    language,
                                    mapValueResultAdapter.getInstantiationTypeName()
                                )
                            )
                            addStatement(
                                "%L.put(%L, %L)",
                                valuesVarName,
                                tmpKeyVarName,
                                tmpValuesVarName
                            )
                        }.endControlFlow()

                        // Perform key columns null check, in a nested mapping we still add
                        // the key with an empty map as the value entry.
                        mapValueResultAdapter.generateContinueColumnCheck(
                            scope,
                            cursorVarName,
                            dupeColumnsIndexAdapter
                        )
                    }
                } else {
                    valuesVarName
                }
                @Suppress("NAME_SHADOWING") // On purpose, to avoid using param
                val genPutValueCode: (String, Boolean) -> Unit = { tmpValueVarName, doKeyCheck ->
                    if (doKeyCheck) {
                        // For consistency purposes, in the one-to-one object mapping case, if
                        // multiple values are encountered for the same key, we will only
                        // consider the first ever encountered mapping.
                        if (mapType.isSparseArray()) {
                            beginControlFlow(
                                "if (%L.get(%L) == null)",
                                valuesVarName, tmpKeyVarName
                            )
                        } else {
                            beginControlFlow(
                                "if (!%L.containsKey(%L))",
                                valuesVarName, tmpKeyVarName
                            )
                        }.apply {
                            addStatement(
                                "%L.put(%L, %L)",
                                valuesVarName, tmpKeyVarName, tmpValueVarName
                            )
                        }.endControlFlow()
                    } else {
                        addStatement(
                            "%L.put(%L, %L)",
                            valuesVarName, tmpKeyVarName, tmpValueVarName
                        )
                    }
                }
                mapValueResultAdapter.convert(
                    scope = scope,
                    valuesVarName = valuesVarName,
                    cursorVarName = cursorVarName,
                    dupeColumnsIndexAdapter = dupeColumnsIndexAdapter,
                    genPutValueCode = genPutValueCode
                )
            }
        }

        override fun generateContinueColumnCheck(
            scope: CodeGenScope,
            cursorVarName: String,
            dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?
        ) {
            scope.builder.add(
                getContinueColumnNullCheck(
                    language = scope.language,
                    cursorVarName = cursorVarName,
                    rowAdapter = keyRowAdapter,
                    dupeColumnsIndexAdapter = dupeColumnsIndexAdapter
                )
            )
        }
    }

    /**
     * An [EndMapValueResultAdapter] contains only the value information regarding the innermost
     * map of the returned nested map.
     *
     * The [convert] function implementation for an [EndMapValueResultAdapter] uses the value row
     * adapter to innermost value map's value, regardless of whether it is a collection type or not.
     */
    class EndMapValueResultAdapter(
        private val valueRowAdapter: RowAdapter,
        private val valueTypeArg: XType,
        private val valueCollectionType: MultimapQueryResultAdapter.CollectionValueType?
    ) : MapValueResultAdapter(
        rowAdapters = listOf(valueRowAdapter)
    ) {
        override fun requiresContainsKeyCheck(): Boolean = valueCollectionType != null

        // The type name of the concrete result map value
        // For Map<Foo, Bar> it is Bar
        // For Map<Foo, List<Bar> it is ArrayList<Bar>
        override fun getDeclarationTypeName(): XTypeName {
            return valueCollectionType?.className?.parametrizedBy(valueTypeArg.asTypeName())
                ?: valueTypeArg.asTypeName()
        }

        // The type name of the result map value
        // For Map<Foo, Bar> it is Bar
        // for Map<Foo, List<Bar> it is List<Bar>
        override fun getInstantiationTypeName(): XTypeName {
            return when (valueCollectionType) {
                MultimapQueryResultAdapter.CollectionValueType.LIST ->
                    CommonTypeNames.ARRAY_LIST.parametrizedBy(valueTypeArg.asTypeName())
                MultimapQueryResultAdapter.CollectionValueType.SET ->
                    CommonTypeNames.HASH_SET.parametrizedBy(valueTypeArg.asTypeName())
                else ->
                    valueTypeArg.asTypeName()
            }
        }

        override fun convert(
            scope: CodeGenScope,
            valuesVarName: String,
            cursorVarName: String,
            dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?,
            genPutValueCode: (String, Boolean) -> Unit
        ) {
            scope.builder.apply {
                val tmpValueVarName = scope.getTmpVar("_value")

                // If we have a collection type, then this means that we have a 1-to-many mapping
                // as opposed to a 1-to-many mapping.
                if (valueCollectionType != null) {
                    addLocalVariable(
                        tmpValueVarName,
                        valueTypeArg.asTypeName()
                    )
                    valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
                    addStatement("%L.add(%L)", valuesVarName, tmpValueVarName)
                } else {
                    check(valueRowAdapter is QueryMappedRowAdapter)
                    val valueIndexVars =
                        dupeColumnsIndexAdapter?.getIndexVarsForMapping(valueRowAdapter.mapping)
                            ?: valueRowAdapter.getDefaultIndexAdapter().getIndexVars()
                    val columnNullCheckCodeBlock = getColumnNullCheckCode(
                        language = scope.language,
                        cursorVarName = cursorVarName,
                        indexVars = valueIndexVars
                    )

                    // Perform value columns null check, in a 1-to-1 mapping we still add the key
                    // with a null value entry if permitted.
                    beginControlFlow("if (%L)", columnNullCheckCodeBlock).apply {
                        if (
                            language == CodeLanguage.KOTLIN &&
                            valueTypeArg.nullability == XNullability.NONNULL
                        ) {
                            addStatement(
                                "error(%S)",
                                "The column(s) of the map value object of type " +
                                    "'$valueTypeArg' are NULL but the map's value type " +
                                    "argument expect it to be NON-NULL"
                            )
                        } else {
                            genPutValueCode.invoke("null", false)
                            addStatement("continue")
                        }
                    }.endControlFlow()

                    addLocalVariable(tmpValueVarName, valueTypeArg.asTypeName())
                    valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
                    genPutValueCode.invoke(tmpValueVarName, true)
                }
            }
        }

        override fun generateContinueColumnCheck(
            scope: CodeGenScope,
            cursorVarName: String,
            dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?
        ) {
            scope.builder.add(
                getContinueColumnNullCheck(
                    language = scope.language,
                    cursorVarName = cursorVarName,
                    rowAdapter = valueRowAdapter,
                    dupeColumnsIndexAdapter = dupeColumnsIndexAdapter
                )
            )
        }
    }

    /**
     * Utility method that returns a code block containing the code expression that verifies if all
     * matched fields are null.
     */
    protected fun getContinueColumnNullCheck(
        language: CodeLanguage,
        rowAdapter: RowAdapter,
        cursorVarName: String,
        dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?
    ) = XCodeBlock.builder(language).apply {
        check(rowAdapter is QueryMappedRowAdapter)
        val valueIndexVars =
            dupeColumnsIndexAdapter?.getIndexVarsForMapping(rowAdapter.mapping)
                ?: rowAdapter.getDefaultIndexAdapter().getIndexVars()
        val columnNullCheckCodeBlock = getColumnNullCheckCode(
            language = language,
            cursorVarName = cursorVarName,
            indexVars = valueIndexVars
        )
        beginControlFlow("if (%L)", columnNullCheckCodeBlock).apply {
            addStatement("continue")
        }.endControlFlow()
    }.build()

    /**
     * Generates a code expression that verifies if all matched fields are null.
     */
    protected fun getColumnNullCheckCode(
        language: CodeLanguage,
        cursorVarName: String,
        indexVars: List<ColumnIndexVar>
    ) = XCodeBlock.builder(language).apply {
        val space = when (language) {
            CodeLanguage.JAVA -> "%W"
            CodeLanguage.KOTLIN -> " "
        }
        val conditions = indexVars.map {
            XCodeBlock.of(
                language,
                "%L.isNull(%L)",
                cursorVarName,
                it.indexVar
            )
        }
        val placeholders = conditions.joinToString(separator = "$space&&$space") { "%L" }
        add(placeholders, *conditions.toTypedArray())
    }.build()
}
