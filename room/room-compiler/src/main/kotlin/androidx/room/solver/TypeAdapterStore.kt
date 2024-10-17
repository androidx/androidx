/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.solver

import androidx.annotation.VisibleForTesting
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isEnum
import androidx.room.ext.CollectionTypeNames.ARRAY_MAP
import androidx.room.ext.CollectionTypeNames.INT_SPARSE_ARRAY
import androidx.room.ext.CollectionTypeNames.LONG_SPARSE_ARRAY
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.GuavaTypeNames
import androidx.room.ext.getValueClassUnderlyingInfo
import androidx.room.ext.isByteBuffer
import androidx.room.ext.isEntityElement
import androidx.room.ext.isNotByte
import androidx.room.ext.isNotKotlinUnit
import androidx.room.ext.isNotVoid
import androidx.room.ext.isNotVoidObject
import androidx.room.ext.isUUID
import androidx.room.parser.ParsedQuery
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.Context
import androidx.room.processor.EntityProcessor
import androidx.room.processor.FieldProcessor
import androidx.room.processor.PojoProcessor
import androidx.room.processor.ProcessorErrors
import androidx.room.processor.ProcessorErrors.DO_NOT_USE_GENERIC_IMMUTABLE_MULTIMAP
import androidx.room.processor.ProcessorErrors.invalidQueryForSingleColumnArray
import androidx.room.solver.binderprovider.CoroutineFlowResultBinderProvider
import androidx.room.solver.binderprovider.CursorQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceFactoryQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.GuavaListenableFutureQueryResultBinderProvider
import androidx.room.solver.binderprovider.InstantQueryResultBinderProvider
import androidx.room.solver.binderprovider.ListenableFuturePagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import androidx.room.solver.binderprovider.PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxJava2PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxJava3PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxLambdaQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxQueryResultBinderProvider
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder
import androidx.room.solver.prepared.binderprovider.GuavaListenableFuturePreparedQueryResultBinderProvider
import androidx.room.solver.prepared.binderprovider.InstantPreparedQueryResultBinderProvider
import androidx.room.solver.prepared.binderprovider.PreparedQueryResultBinderProvider
import androidx.room.solver.prepared.binderprovider.RxPreparedQueryResultBinderProvider
import androidx.room.solver.prepared.result.PreparedQueryResultAdapter
import androidx.room.solver.query.parameter.ArrayQueryParameterAdapter
import androidx.room.solver.query.parameter.BasicQueryParameterAdapter
import androidx.room.solver.query.parameter.CollectionQueryParameterAdapter
import androidx.room.solver.query.parameter.QueryParameterAdapter
import androidx.room.solver.query.result.ArrayQueryResultAdapter
import androidx.room.solver.query.result.EntityRowAdapter
import androidx.room.solver.query.result.GuavaImmutableMultimapQueryResultAdapter
import androidx.room.solver.query.result.GuavaOptionalQueryResultAdapter
import androidx.room.solver.query.result.ImmutableListQueryResultAdapter
import androidx.room.solver.query.result.ImmutableMapQueryResultAdapter
import androidx.room.solver.query.result.ListQueryResultAdapter
import androidx.room.solver.query.result.MapQueryResultAdapter
import androidx.room.solver.query.result.MapValueResultAdapter
import androidx.room.solver.query.result.MultimapQueryResultAdapter
import androidx.room.solver.query.result.MultimapQueryResultAdapter.Companion.getMapColumnName
import androidx.room.solver.query.result.MultimapQueryResultAdapter.Companion.validateMapKeyTypeArg
import androidx.room.solver.query.result.MultimapQueryResultAdapter.Companion.validateMapValueTypeArg
import androidx.room.solver.query.result.MultimapQueryResultAdapter.MapType.Companion.isSparseArray
import androidx.room.solver.query.result.OptionalQueryResultAdapter
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.solver.query.result.QueryResultAdapter
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.query.result.RowAdapter
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.solver.query.result.SingleItemQueryResultAdapter
import androidx.room.solver.query.result.SingleNamedColumnRowAdapter
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.InsertOrUpsertMethodBinder
import androidx.room.solver.shortcut.binderprovider.DeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureInsertOrUpsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InsertOrUpsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InstantDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InstantInsertOrUpsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableInsertOrUpsertMethodBinderProvider
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.solver.shortcut.result.InsertOrUpsertMethodAdapter
import androidx.room.solver.types.BoxedBooleanToBoxedIntConverter
import androidx.room.solver.types.BoxedPrimitiveColumnTypeAdapter
import androidx.room.solver.types.ByteArrayColumnTypeAdapter
import androidx.room.solver.types.ByteArrayWrapperColumnTypeAdapter
import androidx.room.solver.types.ByteBufferColumnTypeAdapter
import androidx.room.solver.types.ColumnTypeAdapter
import androidx.room.solver.types.CompositeAdapter
import androidx.room.solver.types.CursorValueReader
import androidx.room.solver.types.EnumColumnTypeAdapter
import androidx.room.solver.types.PrimitiveBooleanToIntConverter
import androidx.room.solver.types.PrimitiveColumnTypeAdapter
import androidx.room.solver.types.StatementValueBinder
import androidx.room.solver.types.StringColumnTypeAdapter
import androidx.room.solver.types.TypeConverter
import androidx.room.solver.types.UuidColumnTypeAdapter
import androidx.room.solver.types.ValueClassConverterWrapper
import androidx.room.vo.BuiltInConverterFlags
import androidx.room.vo.MapInfo
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.vo.Warning
import androidx.room.vo.isEnabled
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSetMultimap

/**
 * Holds all type adapters and can create on demand composite type adapters to convert a type into a
 * database column.
 */
class TypeAdapterStore
private constructor(
    val context: Context,
    /** first type adapter has the highest priority */
    private val columnTypeAdapters: List<ColumnTypeAdapter>,
    @get:VisibleForTesting internal val typeConverterStore: TypeConverterStore,
    private val builtInConverterFlags: BuiltInConverterFlags
) {

    companion object {
        fun copy(context: Context, store: TypeAdapterStore): TypeAdapterStore {
            return TypeAdapterStore(
                context = context,
                columnTypeAdapters = store.columnTypeAdapters,
                typeConverterStore = store.typeConverterStore,
                builtInConverterFlags = store.builtInConverterFlags
            )
        }

        fun create(
            context: Context,
            builtInConverterFlags: BuiltInConverterFlags,
            vararg extras: Any
        ): TypeAdapterStore {
            val adapters = arrayListOf<ColumnTypeAdapter>()
            val converters = arrayListOf<TypeConverter>()
            fun addAny(extra: Any?) {
                when (extra) {
                    is TypeConverter -> converters.add(extra)
                    is ColumnTypeAdapter -> adapters.add(extra)
                    is List<*> -> extra.forEach(::addAny)
                    else -> throw IllegalArgumentException("unknown extra $extra")
                }
            }

            extras.forEach(::addAny)
            fun addTypeConverter(converter: TypeConverter) {
                converters.add(converter)
            }

            fun addColumnAdapter(adapter: ColumnTypeAdapter) {
                adapters.add(adapter)
            }

            val primitives =
                PrimitiveColumnTypeAdapter.createPrimitiveAdapters(context.processingEnv)
            primitives.forEach(::addColumnAdapter)
            BoxedPrimitiveColumnTypeAdapter.createBoxedPrimitiveAdapters(primitives)
                .forEach(::addColumnAdapter)
            StringColumnTypeAdapter.create(context.processingEnv).forEach(::addColumnAdapter)
            ByteArrayColumnTypeAdapter.create(context.processingEnv).forEach(::addColumnAdapter)
            ByteArrayWrapperColumnTypeAdapter.create(context.processingEnv)
                .forEach(::addColumnAdapter)
            PrimitiveBooleanToIntConverter.create(context.processingEnv).forEach(::addTypeConverter)
            // null aware converter is able to automatically null wrap converters so we don't
            // need this as long as we are running in KSP
            BoxedBooleanToBoxedIntConverter.create(context.processingEnv)
                .forEach(::addTypeConverter)
            return TypeAdapterStore(
                context = context,
                columnTypeAdapters = adapters,
                typeConverterStore =
                    TypeConverterStore.create(
                        context = context,
                        typeConverters = converters,
                        knownColumnTypes = adapters.map { it.out }
                    ),
                builtInConverterFlags = builtInConverterFlags
            )
        }
    }

    private val queryResultBinderProviders: List<QueryResultBinderProvider> =
        mutableListOf<QueryResultBinderProvider>().apply {
            add(CursorQueryResultBinderProvider(context))
            add(LiveDataQueryResultBinderProvider(context))
            add(GuavaListenableFutureQueryResultBinderProvider(context))
            addAll(RxQueryResultBinderProvider.getAll(context))
            addAll(RxLambdaQueryResultBinderProvider.getAll(context))
            add(DataSourceQueryResultBinderProvider(context))
            add(DataSourceFactoryQueryResultBinderProvider(context))
            add(RxJava2PagingSourceQueryResultBinderProvider(context))
            add(RxJava3PagingSourceQueryResultBinderProvider(context))
            add(ListenableFuturePagingSourceQueryResultBinderProvider(context))
            add(PagingSourceQueryResultBinderProvider(context))
            add(CoroutineFlowResultBinderProvider(context))
            add(InstantQueryResultBinderProvider(context))
        }

    private val preparedQueryResultBinderProviders: List<PreparedQueryResultBinderProvider> =
        mutableListOf<PreparedQueryResultBinderProvider>().apply {
            addAll(RxPreparedQueryResultBinderProvider.getAll(context))
            add(GuavaListenableFuturePreparedQueryResultBinderProvider(context))
            add(InstantPreparedQueryResultBinderProvider(context))
        }

    private val insertOrUpsertBinderProviders: List<InsertOrUpsertMethodBinderProvider> =
        mutableListOf<InsertOrUpsertMethodBinderProvider>().apply {
            addAll(RxCallableInsertOrUpsertMethodBinderProvider.getAll(context))
            add(GuavaListenableFutureInsertOrUpsertMethodBinderProvider(context))
            add(InstantInsertOrUpsertMethodBinderProvider(context))
        }

    private val deleteOrUpdateBinderProvider: List<DeleteOrUpdateMethodBinderProvider> =
        mutableListOf<DeleteOrUpdateMethodBinderProvider>().apply {
            addAll(RxCallableDeleteOrUpdateMethodBinderProvider.getAll(context))
            add(GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(context))
            add(InstantDeleteOrUpdateMethodBinderProvider(context))
        }

    /** Searches 1 way to bind a value into a statement. */
    fun findStatementValueBinder(input: XType, affinity: SQLTypeAffinity?): StatementValueBinder? {
        if (input.isError()) {
            return null
        }
        val adapter = findDirectAdapterFor(input, affinity)
        if (adapter != null) {
            return adapter
        }

        fun findTypeConverterAdapter(): ColumnTypeAdapter? {
            val targetTypes = affinity?.getTypeMirrors(context.processingEnv)
            val binder =
                typeConverterStore.findConverterIntoStatement(
                    input = input,
                    columnTypes = targetTypes
                ) ?: return null
            // columnAdapter should not be null but we are receiving errors on crash in `first()` so
            // this safeguard allows us to dispatch the real problem to the user (e.g. why we
            // couldn't
            // find the right adapter)
            val columnAdapter = getAllColumnAdapters(binder.to).firstOrNull() ?: return null
            return CompositeAdapter(input, columnAdapter, binder, null)
        }

        val adapterByTypeConverter = findTypeConverterAdapter()
        if (adapterByTypeConverter != null) {
            return adapterByTypeConverter
        }
        val defaultAdapter = createDefaultTypeAdapter(input, affinity)
        if (defaultAdapter != null) {
            return defaultAdapter
        }
        return null
    }

    /** Searches 1 way to read it from cursor */
    fun findCursorValueReader(output: XType, affinity: SQLTypeAffinity?): CursorValueReader? {
        if (output.isError()) {
            return null
        }
        val adapter = findColumnTypeAdapter(output, affinity, skipDefaultConverter = true)
        if (adapter != null) {
            // two way is better
            return adapter
        }

        fun findTypeConverterAdapter(): ColumnTypeAdapter? {
            val targetTypes = affinity?.getTypeMirrors(context.processingEnv)
            val converter =
                typeConverterStore.findConverterFromCursor(
                    columnTypes = targetTypes,
                    output = output
                ) ?: return null
            return CompositeAdapter(
                output,
                getAllColumnAdapters(converter.from).first(),
                null,
                converter
            )
        }

        // we could not find a two way version, search for anything
        val typeConverterAdapter = findTypeConverterAdapter()
        if (typeConverterAdapter != null) {
            return typeConverterAdapter
        }

        val defaultAdapter = createDefaultTypeAdapter(output, affinity)
        if (defaultAdapter != null) {
            return defaultAdapter
        }

        return null
    }

    /**
     * Finds a two way converter, if you need 1 way, use findStatementValueBinder or
     * findCursorValueReader.
     */
    fun findColumnTypeAdapter(
        out: XType,
        affinity: SQLTypeAffinity?,
        skipDefaultConverter: Boolean
    ): ColumnTypeAdapter? {
        if (out.isError()) {
            return null
        }
        val adapter = findDirectAdapterFor(out, affinity)
        if (adapter != null) {
            return adapter
        }

        fun findTypeConverterAdapter(): ColumnTypeAdapter? {
            val targetTypes = affinity?.getTypeMirrors(context.processingEnv)
            val intoStatement =
                typeConverterStore.findConverterIntoStatement(
                    input = out,
                    columnTypes = targetTypes
                ) ?: return null
            // ok found a converter, try the reverse now
            val fromCursor =
                typeConverterStore.reverse(intoStatement)
                    ?: typeConverterStore.findTypeConverter(intoStatement.to, out)
                    ?: return null
            return CompositeAdapter(
                out,
                getAllColumnAdapters(intoStatement.to).first(),
                intoStatement,
                fromCursor
            )
        }

        val adapterByTypeConverter = findTypeConverterAdapter()
        if (adapterByTypeConverter != null) {
            return adapterByTypeConverter
        }

        if (!skipDefaultConverter) {
            val defaultAdapter = createDefaultTypeAdapter(out, affinity)
            if (defaultAdapter != null) {
                return defaultAdapter
            }
        }
        return null
    }

    private fun createDefaultTypeAdapter(
        type: XType,
        affinity: SQLTypeAffinity?
    ): ColumnTypeAdapter? {
        val typeElement = type.typeElement
        if (typeElement?.isValueClass() == true) {
            // Extract the type value of the Value class element
            val underlyingInfo = typeElement.getValueClassUnderlyingInfo()
            if (underlyingInfo.constructor.isPrivate() || underlyingInfo.field.getter == null) {
                return null
            }
            val underlyingTypeColumnAdapter =
                findColumnTypeAdapter(
                    // Find an adapter for the non-null underlying type, nullability will be handled
                    // by the value class adapter.
                    out = underlyingInfo.parameter.asMemberOf(type).makeNonNullable(),
                    affinity = affinity,
                    skipDefaultConverter = false
                ) ?: return null

            return ValueClassConverterWrapper(
                valueTypeColumnAdapter = underlyingTypeColumnAdapter,
                affinity = underlyingTypeColumnAdapter.typeAffinity,
                out = type,
                valuePropertyName = underlyingInfo.parameter.name
            )
        }
        return when {
            builtInConverterFlags.enums.isEnabled() && typeElement?.isEnum() == true ->
                EnumColumnTypeAdapter(typeElement, type)
            builtInConverterFlags.uuid.isEnabled() && type.isUUID() -> UuidColumnTypeAdapter(type)
            builtInConverterFlags.byteBuffer.isEnabled() && type.isByteBuffer() ->
                ByteBufferColumnTypeAdapter(type)
            else -> null
        }
    }

    private fun findDirectAdapterFor(out: XType, affinity: SQLTypeAffinity?): ColumnTypeAdapter? {
        return getAllColumnAdapters(out).firstOrNull {
            affinity == null || it.typeAffinity == affinity
        }
    }

    fun findDeleteOrUpdateMethodBinder(typeMirror: XType): DeleteOrUpdateMethodBinder {
        return deleteOrUpdateBinderProvider.first { it.matches(typeMirror) }.provide(typeMirror)
    }

    fun findInsertMethodBinder(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>
    ): InsertOrUpsertMethodBinder {
        return insertOrUpsertBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, params, false)
    }

    fun findUpsertMethodBinder(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>
    ): InsertOrUpsertMethodBinder {
        return insertOrUpsertBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, params, true)
    }

    fun findQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit = {}
    ): QueryResultBinder {
        return findQueryResultBinder(typeMirror, query, TypeAdapterExtras().apply(extrasCreator))
    }

    fun findQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras
    ): QueryResultBinder {
        return queryResultBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, query, extras)
    }

    fun findPreparedQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery
    ): PreparedQueryResultBinder {
        return preparedQueryResultBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, query)
    }

    fun findPreparedQueryResultAdapter(typeMirror: XType, query: ParsedQuery) =
        PreparedQueryResultAdapter.create(typeMirror, query.type)

    fun findDeleteOrUpdateAdapter(typeMirror: XType): DeleteOrUpdateMethodAdapter? {
        return DeleteOrUpdateMethodAdapter.create(typeMirror)
    }

    fun findInsertAdapter(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>
    ): InsertOrUpsertMethodAdapter? {
        return InsertOrUpsertMethodAdapter.createInsert(context, typeMirror, params)
    }

    fun findUpsertAdapter(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>
    ): InsertOrUpsertMethodAdapter? {
        return InsertOrUpsertMethodAdapter.createUpsert(context, typeMirror, params)
    }

    fun findQueryResultAdapter(
        typeMirror: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit = {}
    ): QueryResultAdapter? {
        return findQueryResultAdapter(typeMirror, query, TypeAdapterExtras().apply(extrasCreator))
    }

    fun findQueryResultAdapter(
        typeMirror: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras
    ): QueryResultAdapter? {
        if (typeMirror.isError()) {
            return null
        }

        // TODO: (b/192068912) Refactor the following since this if-else cascade has gotten large
        if (typeMirror.isArray() && typeMirror.componentType.isNotByte()) {
            val componentType = typeMirror.componentType
            checkTypeNullability(typeMirror, extras, "Array", arrayComponentType = componentType)
            val isSingleColumnArray =
                componentType.asTypeName().isPrimitive || componentType.isTypeOf(String::class)
            val queryResultInfo = query.resultInfo
            if (
                isSingleColumnArray && queryResultInfo != null && queryResultInfo.columns.size > 1
            ) {
                context.logger.e(
                    invalidQueryForSingleColumnArray(
                        typeMirror.asTypeName().toString(context.codeLanguage)
                    )
                )
                return null
            }

            // Create a type mirror for a regular List in order to use ListQueryResultAdapter. This
            // avoids code duplication as an Array can be initialized using a list.
            val listType =
                context.processingEnv
                    .getDeclaredType(
                        context.processingEnv.requireTypeElement(List::class),
                        componentType.boxed().makeNonNullable()
                    )
                    .makeNonNullable()

            val listResultAdapter =
                findQueryResultAdapter(typeMirror = listType, query = query, extras = extras)
                    ?: return null

            return ArrayQueryResultAdapter(typeMirror, listResultAdapter as ListQueryResultAdapter)
        } else if (typeMirror.typeArguments.isEmpty()) {
            val rowAdapter = findRowAdapter(typeMirror, query) ?: return null
            return SingleItemQueryResultAdapter(rowAdapter)
        } else if (typeMirror.rawType.asTypeName() == GuavaTypeNames.OPTIONAL) {
            checkTypeNullability(typeMirror, extras, "Optional")
            // Handle Guava Optional by unpacking its generic type argument and adapting that.
            // The Optional adapter will re-append the Optional type.
            val typeArg = typeMirror.typeArguments.first()
            // use nullable when finding row adapter as non-null adapters might return
            // default values
            val rowAdapter = findRowAdapter(typeArg.makeNullable(), query) ?: return null
            return GuavaOptionalQueryResultAdapter(
                typeArg = typeArg,
                resultAdapter = SingleItemQueryResultAdapter(rowAdapter)
            )
        } else if (typeMirror.rawType.asTypeName() == CommonTypeNames.OPTIONAL) {
            checkTypeNullability(typeMirror, extras, "Optional")

            // Handle java.util.Optional similarly.
            val typeArg = typeMirror.typeArguments.first()
            // use nullable when finding row adapter as non-null adapters might return
            // default values
            val rowAdapter = findRowAdapter(typeArg.makeNullable(), query) ?: return null
            return OptionalQueryResultAdapter(
                typeArg = typeArg,
                resultAdapter = SingleItemQueryResultAdapter(rowAdapter)
            )
        } else if (typeMirror.isTypeOf(ImmutableList::class)) {
            checkTypeNullability(typeMirror, extras)

            val typeArg = typeMirror.typeArguments.first().extendsBoundOrSelf()
            val rowAdapter = findRowAdapter(typeArg, query) ?: return null
            return ImmutableListQueryResultAdapter(typeArg = typeArg, rowAdapter = rowAdapter)
        } else if (typeMirror.isTypeOf(java.util.List::class)) {
            checkTypeNullability(typeMirror, extras)
            val typeArg = typeMirror.typeArguments.first().extendsBoundOrSelf()
            val rowAdapter = findRowAdapter(typeArg, query) ?: return null
            return ListQueryResultAdapter(typeArg = typeArg, rowAdapter = rowAdapter)
        } else if (typeMirror.isTypeOf(ImmutableMap::class)) {
            val keyTypeArg = typeMirror.typeArguments[0].extendsBoundOrSelf()
            val valueTypeArg = typeMirror.typeArguments[1].extendsBoundOrSelf()
            checkTypeNullability(typeMirror, extras)

            // Create a type mirror for a regular Map in order to use MapQueryResultAdapter. This
            // avoids code duplication as Immutable Map can be initialized by creating an immutable
            // copy of a regular map.
            val mapType =
                context.processingEnv.getDeclaredType(
                    context.processingEnv.requireTypeElement(Map::class),
                    keyTypeArg,
                    valueTypeArg
                )

            val resultAdapter = findQueryResultAdapter(mapType, query, extras) ?: return null
            return ImmutableMapQueryResultAdapter(
                context = context,
                parsedQuery = query,
                keyTypeArg = keyTypeArg,
                valueTypeArg = valueTypeArg,
                resultAdapter = resultAdapter
            )
        } else if (
            typeMirror.isTypeOf(ImmutableSetMultimap::class) ||
                typeMirror.isTypeOf(ImmutableListMultimap::class) ||
                typeMirror.isTypeOf(ImmutableMultimap::class)
        ) {
            val keyTypeArg = typeMirror.typeArguments[0].extendsBoundOrSelf()
            val valueTypeArg = typeMirror.typeArguments[1].extendsBoundOrSelf()
            checkTypeNullability(typeMirror, extras)

            if (valueTypeArg.typeElement == null) {
                context.logger.e(
                    "Guava multimap 'value' type argument does not represent a class. " +
                        "Found $valueTypeArg."
                )
                return null
            }

            val immutableClassName =
                if (typeMirror.isTypeOf(ImmutableListMultimap::class)) {
                    GuavaTypeNames.IMMUTABLE_LIST_MULTIMAP
                } else if (typeMirror.isTypeOf(ImmutableSetMultimap::class)) {
                    GuavaTypeNames.IMMUTABLE_SET_MULTIMAP
                } else {
                    // Return type is base class ImmutableMultimap which is not recommended.
                    context.logger.e(DO_NOT_USE_GENERIC_IMMUTABLE_MULTIMAP)
                    return null
                }

            // Get @MapInfo info if any (this might be null)
            val mapInfo = extras.getData(MapInfo::class)
            val mapKeyColumn = getMapColumnName(context, query, keyTypeArg)
            val mapValueColumn = getMapColumnName(context, query, valueTypeArg)
            if (mapInfo != null && (mapKeyColumn != null || mapValueColumn != null)) {
                context.logger.e(ProcessorErrors.CANNOT_USE_MAP_COLUMN_AND_MAP_INFO_SIMULTANEOUSLY)
            }

            val mappedKeyColumnName = mapKeyColumn ?: mapInfo?.keyColumnName
            val mappedValueColumnName = mapValueColumn ?: mapInfo?.valueColumnName

            val keyRowAdapter =
                findRowAdapter(
                    typeMirror = keyTypeArg,
                    query = query,
                    columnName = mappedKeyColumnName
                ) ?: return null

            val valueRowAdapter =
                findRowAdapter(
                    typeMirror = valueTypeArg,
                    query = query,
                    columnName = mappedValueColumnName
                ) ?: return null

            validateMapKeyTypeArg(
                context = context,
                keyTypeArg = keyTypeArg,
                keyReader = findCursorValueReader(keyTypeArg, null),
                keyColumnName = mappedKeyColumnName
            )
            validateMapValueTypeArg(
                context = context,
                valueTypeArg = valueTypeArg,
                valueReader = findCursorValueReader(valueTypeArg, null),
                valueColumnName = mappedValueColumnName
            )
            return GuavaImmutableMultimapQueryResultAdapter(
                context = context,
                parsedQuery = query,
                keyTypeArg = keyTypeArg,
                valueTypeArg = valueTypeArg,
                keyRowAdapter = keyRowAdapter,
                valueRowAdapter = valueRowAdapter,
                immutableClassName = immutableClassName
            )
        } else if (
            typeMirror.isTypeOf(java.util.Map::class) ||
                typeMirror.rawType.asTypeName().equalsIgnoreNullability(ARRAY_MAP) ||
                typeMirror.rawType.asTypeName().equalsIgnoreNullability(LONG_SPARSE_ARRAY) ||
                typeMirror.rawType.asTypeName().equalsIgnoreNullability(INT_SPARSE_ARRAY)
        ) {
            val mapType =
                when (typeMirror.rawType.asTypeName()) {
                    LONG_SPARSE_ARRAY -> MultimapQueryResultAdapter.MapType.LONG_SPARSE
                    INT_SPARSE_ARRAY -> MultimapQueryResultAdapter.MapType.INT_SPARSE
                    ARRAY_MAP -> MultimapQueryResultAdapter.MapType.ARRAY_MAP
                    else -> MultimapQueryResultAdapter.MapType.DEFAULT
                }
            val keyTypeArg =
                when (mapType) {
                    MultimapQueryResultAdapter.MapType.LONG_SPARSE ->
                        context.processingEnv.requireType(XTypeName.PRIMITIVE_LONG)
                    MultimapQueryResultAdapter.MapType.INT_SPARSE ->
                        context.processingEnv.requireType(XTypeName.PRIMITIVE_INT)
                    else -> typeMirror.typeArguments[0].extendsBoundOrSelf()
                }
            checkTypeNullability(typeMirror, extras)

            val mapValueTypeArg =
                if (mapType.isSparseArray()) {
                    typeMirror.typeArguments[0].extendsBoundOrSelf()
                } else {
                    typeMirror.typeArguments[1].extendsBoundOrSelf()
                }

            if (mapValueTypeArg.typeElement == null) {
                context.logger.e(
                    "Multimap 'value' collection type argument does not represent a class. " +
                        "Found $mapValueTypeArg."
                )
                return null
            }

            // Get @MapInfo info if any (this might be null)
            val mapInfo = extras.getData(MapInfo::class)
            val mapColumn = getMapColumnName(context, query, keyTypeArg)
            if (mapInfo != null && mapColumn != null) {
                context.logger.e(ProcessorErrors.CANNOT_USE_MAP_COLUMN_AND_MAP_INFO_SIMULTANEOUSLY)
            }

            val mappedKeyColumnName = mapColumn ?: mapInfo?.keyColumnName
            val keyRowAdapter =
                findRowAdapter(
                    typeMirror = keyTypeArg,
                    query = query,
                    columnName = mappedKeyColumnName
                ) ?: return null

            validateMapKeyTypeArg(
                context = context,
                keyTypeArg = keyTypeArg,
                keyReader = findCursorValueReader(keyTypeArg, null),
                keyColumnName = mappedKeyColumnName
            )

            val mapValueResultAdapter =
                findMapValueResultAdapter(
                    query = query,
                    mapInfo = mapInfo,
                    mapValueTypeArg = mapValueTypeArg
                ) ?: return null
            return MapQueryResultAdapter(
                context = context,
                parsedQuery = query,
                mapValueResultAdapter =
                    MapValueResultAdapter.NestedMapValueResultAdapter(
                        keyRowAdapter = keyRowAdapter,
                        keyTypeArg = keyTypeArg,
                        mapType = mapType,
                        mapValueResultAdapter = mapValueResultAdapter
                    )
            )
        }
        return null
    }

    private fun checkTypeNullability(
        searchingType: XType,
        extras: TypeAdapterExtras,
        typeKeyword: String = "Collection",
        arrayComponentType: XType? = null
    ) {
        if (context.codeLanguage != CodeLanguage.KOTLIN) {
            return
        }

        val collectionType: XType =
            extras.getData(ObservableQueryResultBinderProvider.OriginalTypeArg::class)?.original
                ?: searchingType

        if (collectionType.nullability != XNullability.NONNULL) {
            context.logger.w(
                Warning.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE,
                ProcessorErrors.nullableCollectionOrArrayReturnTypeInDaoMethod(
                    searchingType.asTypeName().toString(context.codeLanguage),
                    typeKeyword
                )
            )
        }

        // Since Array has typeArg in the componentType and not typeArguments, need a special check.
        if (arrayComponentType != null && arrayComponentType.nullability != XNullability.NONNULL) {
            context.logger.w(
                Warning.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE,
                ProcessorErrors.nullableComponentInDaoMethodReturnType(
                    searchingType.asTypeName().toString(context.codeLanguage)
                )
            )
            return
        }

        collectionType.typeArguments.forEach { typeArg ->
            if (typeArg.nullability != XNullability.NONNULL) {
                context.logger.w(
                    Warning.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE,
                    ProcessorErrors.nullableComponentInDaoMethodReturnType(
                        searchingType.asTypeName().toString(context.codeLanguage)
                    )
                )
            }
        }
    }

    private fun findMapValueResultAdapter(
        query: ParsedQuery,
        mapInfo: MapInfo?,
        mapValueTypeArg: XType
    ): MapValueResultAdapter? {
        val collectionTypeRaw =
            context.processingEnv.requireType(CommonTypeNames.COLLECTION).rawType
        if (collectionTypeRaw.isAssignableFrom(mapValueTypeArg.rawType)) {
            // The Map's value type argument is assignable to a Collection, we need to make
            // sure it is either a list or a set.
            val listTypeRaw = context.processingEnv.requireType(CommonTypeNames.LIST).rawType
            val setTypeRaw = context.processingEnv.requireType(CommonTypeNames.SET).rawType
            val collectionValueType =
                when {
                    mapValueTypeArg.rawType.isAssignableFrom(listTypeRaw) ->
                        MultimapQueryResultAdapter.CollectionValueType.LIST
                    mapValueTypeArg.rawType.isAssignableFrom(setTypeRaw) ->
                        MultimapQueryResultAdapter.CollectionValueType.SET
                    else -> {
                        context.logger.e(
                            ProcessorErrors.valueCollectionMustBeListOrSetOrMap(
                                mapValueTypeArg.asTypeName().toString(context.codeLanguage)
                            )
                        )
                        return null
                    }
                }

            val valueTypeArg = mapValueTypeArg.typeArguments.single().extendsBoundOrSelf()
            val mapColumnName = getMapColumnName(context, query, valueTypeArg)
            if (mapColumnName != null && mapInfo != null) {
                context.logger.e(ProcessorErrors.CANNOT_USE_MAP_COLUMN_AND_MAP_INFO_SIMULTANEOUSLY)
            }

            val mappedValueColumnName = mapColumnName ?: mapInfo?.valueColumnName
            val valueRowAdapter =
                findRowAdapter(
                    typeMirror = valueTypeArg,
                    query = query,
                    columnName = mappedValueColumnName
                ) ?: return null

            validateMapValueTypeArg(
                context = context,
                valueTypeArg = valueTypeArg,
                valueReader = findCursorValueReader(valueTypeArg, null),
                valueColumnName = mappedValueColumnName
            )

            return MapValueResultAdapter.EndMapValueResultAdapter(
                valueRowAdapter = valueRowAdapter,
                valueTypeArg = valueTypeArg,
                valueCollectionType = collectionValueType
            )
        } else if (mapValueTypeArg.isTypeOf(java.util.Map::class)) {
            val keyTypeArg = mapValueTypeArg.typeArguments[0].extendsBoundOrSelf()
            val valueTypeArg = mapValueTypeArg.typeArguments[1].extendsBoundOrSelf()

            val keyRowAdapter =
                findRowAdapter(
                    typeMirror = keyTypeArg,
                    query = query,
                    // No need to account for @MapInfo since nested maps did not support
                    // this now deprecated annotation anyway.
                    columnName = getMapColumnName(context, query, keyTypeArg)
                ) ?: return null
            val valueMapAdapter =
                findMapValueResultAdapter(
                    query = query,
                    mapInfo = mapInfo,
                    mapValueTypeArg = valueTypeArg
                ) ?: return null
            return MapValueResultAdapter.NestedMapValueResultAdapter(
                keyRowAdapter = keyRowAdapter,
                keyTypeArg = keyTypeArg,
                mapType = MultimapQueryResultAdapter.MapType.DEFAULT,
                mapValueResultAdapter = valueMapAdapter
            )
        } else {
            val mappedValueColumnName =
                getMapColumnName(context, query, mapValueTypeArg) ?: mapInfo?.valueColumnName
            val valueRowAdapter =
                findRowAdapter(
                    typeMirror = mapValueTypeArg,
                    query = query,
                    columnName = mappedValueColumnName
                ) ?: return null

            validateMapValueTypeArg(
                context = context,
                valueTypeArg = mapValueTypeArg,
                valueReader = findCursorValueReader(mapValueTypeArg, null),
                valueColumnName = mappedValueColumnName
            )
            return MapValueResultAdapter.EndMapValueResultAdapter(
                valueRowAdapter = valueRowAdapter,
                valueTypeArg = mapValueTypeArg,
                valueCollectionType = null
            )
        }
    }

    /**
     * Find a converter from cursor to the given type mirror. If there is information about the
     * query result, we try to use it to accept *any* POJO.
     */
    fun findRowAdapter(
        typeMirror: XType,
        query: ParsedQuery,
        columnName: String? = null
    ): RowAdapter? {
        if (typeMirror.isError()) {
            return null
        }

        val typeElement = typeMirror.typeElement
        if (typeElement != null && !typeMirror.asTypeName().isPrimitive) {
            if (typeMirror.typeArguments.isNotEmpty()) {
                // TODO one day support this
                return null
            }
            val resultInfo = query.resultInfo

            val (rowAdapter, rowAdapterLogs) =
                if (resultInfo != null && query.errors.isEmpty() && resultInfo.error == null) {
                    // if result info is not null, first try a pojo row adapter
                    context.collectLogs { subContext ->
                        val pojo =
                            PojoProcessor.createFor(
                                    context = subContext,
                                    element = typeElement,
                                    bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                                    parent = null
                                )
                                .process()
                        PojoRowAdapter(
                            context = subContext,
                            info = resultInfo,
                            query = query,
                            pojo = pojo,
                            out = typeMirror
                        )
                    }
                } else {
                    Pair(null, null)
                }

            if (rowAdapter == null && query.resultInfo == null) {
                // we don't know what query returns. Check for entity.
                if (typeElement.isEntityElement()) {
                    return EntityRowAdapter(
                        entity =
                            EntityProcessor(context = context, element = typeElement).process(),
                        out = typeMirror
                    )
                }
            }

            if (rowAdapter != null && rowAdapterLogs?.hasErrors() != true) {
                rowAdapterLogs?.writeTo(context)
                return rowAdapter
            }

            if (columnName != null) {
                val singleNamedColumn =
                    findCursorValueReader(
                        typeMirror,
                        query.resultInfo?.columns?.find { it.name == columnName }?.type
                    )
                if (singleNamedColumn != null) {
                    return SingleNamedColumnRowAdapter(singleNamedColumn, columnName)
                }
            }

            if ((resultInfo?.columns?.size ?: 1) == 1) {
                val singleColumn =
                    findCursorValueReader(typeMirror, resultInfo?.columns?.get(0)?.type)
                if (singleColumn != null) {
                    return SingleColumnRowAdapter(singleColumn)
                }
            }
            // if we tried, return its errors
            if (rowAdapter != null) {
                rowAdapterLogs?.writeTo(context)
                return rowAdapter
            }

            // use pojo adapter as a last resort.
            // this happens when @RawQuery or @SkipVerification is used.
            if (
                query.resultInfo == null &&
                    typeMirror.isNotVoid() &&
                    typeMirror.isNotVoidObject() &&
                    typeMirror.isNotKotlinUnit()
            ) {
                val pojo =
                    PojoProcessor.createFor(
                            context = context,
                            element = typeElement,
                            bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                            parent = null
                        )
                        .process()
                return PojoRowAdapter(
                    context = context,
                    info = null,
                    query = query,
                    pojo = pojo,
                    out = typeMirror
                )
            }
            return null
        } else {
            if (columnName != null) {
                val singleNamedColumn =
                    findCursorValueReader(
                        typeMirror,
                        query.resultInfo?.columns?.find { it.name == columnName }?.type
                    )
                if (singleNamedColumn != null) {
                    return SingleNamedColumnRowAdapter(singleNamedColumn, columnName)
                }
            }
            val singleColumn = findCursorValueReader(typeMirror, null) ?: return null
            return SingleColumnRowAdapter(singleColumn)
        }
    }

    fun findQueryParameterAdapter(
        typeMirror: XType,
        isMultipleParameter: Boolean
    ): QueryParameterAdapter? {
        val collectionType = context.processingEnv.requireType(CommonTypeNames.COLLECTION)
        if (collectionType.rawType.isAssignableFrom(typeMirror)) {
            val typeArg = typeMirror.typeArguments.first().extendsBoundOrSelf()
            // An adapter for the collection type arg wrapped in the built-in collection adapter.
            val wrappedCollectionAdapter =
                findStatementValueBinder(typeArg, null)?.let {
                    CollectionQueryParameterAdapter(it, typeMirror.nullability)
                }
            // An adapter for the collection itself, likely a user provided type converter for the
            // collection.
            val directCollectionAdapter =
                findStatementValueBinder(typeMirror, null)?.let { BasicQueryParameterAdapter(it) }
            // Prioritize built-in collection adapters when finding an adapter for a multi-value
            // binding param since it is likely wrong to use a collection to single value converter
            // for an expression that takes in multiple values.
            return if (isMultipleParameter) {
                wrappedCollectionAdapter ?: directCollectionAdapter
            } else {
                directCollectionAdapter ?: wrappedCollectionAdapter
            }
        } else if (typeMirror.isArray() && typeMirror.componentType.isNotByte()) {
            val component = typeMirror.componentType
            val binder = findStatementValueBinder(component, null) ?: return null
            return ArrayQueryParameterAdapter(binder, typeMirror.nullability)
        } else {
            val binder = findStatementValueBinder(typeMirror, null) ?: return null
            return BasicQueryParameterAdapter(binder)
        }
    }

    private fun getAllColumnAdapters(input: XType): List<ColumnTypeAdapter> {
        return columnTypeAdapters.filter { input.isSameType(it.out) }
    }
}
