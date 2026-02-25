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

package androidx.room3.solver

import androidx.annotation.VisibleForTesting
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.isArray
import androidx.room3.compiler.processing.isEnum
import androidx.room3.compiler.processing.isKotlinUnit
import androidx.room3.ext.CollectionTypeNames.ARRAY_MAP
import androidx.room3.ext.CollectionTypeNames.INT_SPARSE_ARRAY
import androidx.room3.ext.CollectionTypeNames.LONG_SPARSE_ARRAY
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.GuavaTypeNames
import androidx.room3.ext.getValueClassUnderlyingInfo
import androidx.room3.ext.isByteBuffer
import androidx.room3.ext.isEntityElement
import androidx.room3.ext.isNotByte
import androidx.room3.ext.isNotKotlinUnit
import androidx.room3.ext.isNotVoid
import androidx.room3.ext.isNotVoidObject
import androidx.room3.ext.isUUID
import androidx.room3.parser.ParsedQuery
import androidx.room3.parser.SQLTypeAffinity
import androidx.room3.processor.Context
import androidx.room3.processor.DataClassProcessor
import androidx.room3.processor.EntityProcessor
import androidx.room3.processor.ProcessorErrors
import androidx.room3.processor.ProcessorErrors.DO_NOT_USE_GENERIC_IMMUTABLE_MULTIMAP
import androidx.room3.processor.ProcessorErrors.invalidQueryForSingleColumnArray
import androidx.room3.processor.PropertyProcessor
import androidx.room3.solver.binderprovider.CoroutineFlowResultBinderProvider
import androidx.room3.solver.binderprovider.DaoReturnTypeQueryResultBinderProvider
import androidx.room3.solver.binderprovider.InstantQueryResultBinderProvider
import androidx.room3.solver.binderprovider.ListenableFuturePagingSourceQueryResultBinderProvider
import androidx.room3.solver.binderprovider.PagingSourceQueryResultBinderProvider
import androidx.room3.solver.binderprovider.RxJava3PagingSourceQueryResultBinderProvider
import androidx.room3.solver.binderprovider.SuspendResultBinderProvider
import androidx.room3.solver.prepared.binder.PreparedQueryResultBinder
import androidx.room3.solver.prepared.binderprovider.GuavaListenableFuturePreparedQueryResultBinderProvider
import androidx.room3.solver.prepared.binderprovider.InstantPreparedQueryResultBinderProvider
import androidx.room3.solver.prepared.binderprovider.PreparedQueryResultBinderProvider
import androidx.room3.solver.prepared.binderprovider.RxPreparedQueryResultBinderProvider
import androidx.room3.solver.prepared.result.PreparedQueryResultAdapter
import androidx.room3.solver.query.parameter.ArrayQueryParameterAdapter
import androidx.room3.solver.query.parameter.BasicQueryParameterAdapter
import androidx.room3.solver.query.parameter.CollectionQueryParameterAdapter
import androidx.room3.solver.query.parameter.QueryParameterAdapter
import androidx.room3.solver.query.result.ArrayQueryResultAdapter
import androidx.room3.solver.query.result.DataClassRowAdapter
import androidx.room3.solver.query.result.EntityRowAdapter
import androidx.room3.solver.query.result.GuavaImmutableMultimapQueryResultAdapter
import androidx.room3.solver.query.result.GuavaOptionalQueryResultAdapter
import androidx.room3.solver.query.result.ImmutableListQueryResultAdapter
import androidx.room3.solver.query.result.ImmutableMapQueryResultAdapter
import androidx.room3.solver.query.result.ListQueryResultAdapter
import androidx.room3.solver.query.result.MapQueryResultAdapter
import androidx.room3.solver.query.result.MapValueResultAdapter
import androidx.room3.solver.query.result.MultimapQueryResultAdapter
import androidx.room3.solver.query.result.MultimapQueryResultAdapter.Companion.getMapColumnName
import androidx.room3.solver.query.result.MultimapQueryResultAdapter.Companion.validateMapKeyTypeArg
import androidx.room3.solver.query.result.MultimapQueryResultAdapter.Companion.validateMapValueTypeArg
import androidx.room3.solver.query.result.MultimapQueryResultAdapter.MapType.Companion.isSparseArray
import androidx.room3.solver.query.result.OptionalQueryResultAdapter
import androidx.room3.solver.query.result.QueryResultAdapter
import androidx.room3.solver.query.result.QueryResultBinder
import androidx.room3.solver.query.result.RowAdapter
import androidx.room3.solver.query.result.SingleColumnRowAdapter
import androidx.room3.solver.query.result.SingleItemQueryResultAdapter
import androidx.room3.solver.query.result.SingleNamedColumnRowAdapter
import androidx.room3.solver.shortcut.binder.DeleteOrUpdateFunctionBinder
import androidx.room3.solver.shortcut.binder.InsertOrUpsertFunctionBinder
import androidx.room3.solver.shortcut.binderprovider.DeleteOrUpdateFunctionBinderProvider
import androidx.room3.solver.shortcut.binderprovider.GuavaListenableFutureDeleteOrUpdateFunctionBinderProvider
import androidx.room3.solver.shortcut.binderprovider.GuavaListenableFutureInsertOrUpsertFunctionBinderProvider
import androidx.room3.solver.shortcut.binderprovider.InsertOrUpsertFunctionBinderProvider
import androidx.room3.solver.shortcut.binderprovider.InstantDeleteOrUpdateFunctionBinderProvider
import androidx.room3.solver.shortcut.binderprovider.InstantInsertOrUpsertFunctionBinderProvider
import androidx.room3.solver.shortcut.binderprovider.RxCallableDeleteOrUpdateFunctionBinderProvider
import androidx.room3.solver.shortcut.binderprovider.RxCallableInsertOrUpsertFunctionBinderProvider
import androidx.room3.solver.shortcut.result.DeleteOrUpdateFunctionAdapter
import androidx.room3.solver.shortcut.result.InsertOrUpsertFunctionAdapter
import androidx.room3.solver.types.BoxedBooleanToBoxedIntConverter
import androidx.room3.solver.types.BoxedPrimitiveColumnTypeAdapter
import androidx.room3.solver.types.ByteArrayColumnTypeAdapter
import androidx.room3.solver.types.ByteArrayWrapperColumnTypeAdapter
import androidx.room3.solver.types.ByteBufferColumnTypeAdapter
import androidx.room3.solver.types.ColumnTypeAdapter
import androidx.room3.solver.types.CompositeAdapter
import androidx.room3.solver.types.DaoReturnTypeConverter
import androidx.room3.solver.types.EnumColumnTypeAdapter
import androidx.room3.solver.types.PrimitiveBooleanToIntConverter
import androidx.room3.solver.types.PrimitiveColumnTypeAdapter
import androidx.room3.solver.types.StatementValueBinder
import androidx.room3.solver.types.StatementValueReader
import androidx.room3.solver.types.StringColumnTypeAdapter
import androidx.room3.solver.types.TypeConverter
import androidx.room3.solver.types.UuidColumnTypeAdapter
import androidx.room3.solver.types.ValueClassConverterWrapper
import androidx.room3.vo.BuiltInConverterFlags
import androidx.room3.vo.ShortcutQueryParameter
import androidx.room3.vo.Warning
import androidx.room3.vo.isEnabled
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
    private val builtInConverterFlags: BuiltInConverterFlags,
    private val daoReturnTypeConverters: List<DaoReturnTypeConverter>,
) {

    companion object {
        fun copy(context: Context, store: TypeAdapterStore): TypeAdapterStore {
            return TypeAdapterStore(
                context = context,
                columnTypeAdapters = store.columnTypeAdapters,
                typeConverterStore = store.typeConverterStore,
                builtInConverterFlags = store.builtInConverterFlags,
                daoReturnTypeConverters = store.daoReturnTypeConverters,
            )
        }

        fun create(
            context: Context,
            builtInConverterFlags: BuiltInConverterFlags,
            vararg extras: Any,
        ): TypeAdapterStore {
            val adapters = arrayListOf<ColumnTypeAdapter>()
            val converters = arrayListOf<TypeConverter>()
            val daoReturnTypeConverters = arrayListOf<DaoReturnTypeConverter>()
            fun addAny(extra: Any?) {
                when (extra) {
                    is TypeConverter -> converters.add(extra)
                    is ColumnTypeAdapter -> adapters.add(extra)
                    is List<*> -> extra.forEach(::addAny)
                    is DaoReturnTypeConverter -> daoReturnTypeConverters.add(extra)
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
                        knownColumnTypes = adapters.map { it.out },
                    ),
                builtInConverterFlags = builtInConverterFlags,
                daoReturnTypeConverters = daoReturnTypeConverters,
            )
        }
    }

    private val coroutineQueryResultBinderProviders =
        mutableListOf<QueryResultBinderProvider>().apply {
            addAll(
                daoReturnTypeConverters
                    .filter { it.isSuspend }
                    .map { DaoReturnTypeQueryResultBinderProvider(context, it) }
            )
            add(SuspendResultBinderProvider(context))
        }

    private val queryResultBinderProviders: List<QueryResultBinderProvider> =
        mutableListOf<QueryResultBinderProvider>().apply {
            add(RxJava3PagingSourceQueryResultBinderProvider(context))
            add(ListenableFuturePagingSourceQueryResultBinderProvider(context))
            add(PagingSourceQueryResultBinderProvider(context))
            add(CoroutineFlowResultBinderProvider(context))
            addAll(
                daoReturnTypeConverters
                    .filterNot { it.isSuspend }
                    .map { DaoReturnTypeQueryResultBinderProvider(context, it) }
            )
            add(InstantQueryResultBinderProvider(context))
        }

    private val preparedQueryResultBinderProviders: List<PreparedQueryResultBinderProvider> =
        mutableListOf<PreparedQueryResultBinderProvider>().apply {
            addAll(RxPreparedQueryResultBinderProvider.getAll(context))
            add(GuavaListenableFuturePreparedQueryResultBinderProvider(context))
            add(InstantPreparedQueryResultBinderProvider(context))
        }

    private val insertOrUpsertBinderProviders: List<InsertOrUpsertFunctionBinderProvider> =
        mutableListOf<InsertOrUpsertFunctionBinderProvider>().apply {
            addAll(RxCallableInsertOrUpsertFunctionBinderProvider.getAll(context))
            add(GuavaListenableFutureInsertOrUpsertFunctionBinderProvider(context))
            add(InstantInsertOrUpsertFunctionBinderProvider(context))
        }

    private val deleteOrUpdateBinderProvider: List<DeleteOrUpdateFunctionBinderProvider> =
        mutableListOf<DeleteOrUpdateFunctionBinderProvider>().apply {
            addAll(RxCallableDeleteOrUpdateFunctionBinderProvider.getAll(context))
            add(GuavaListenableFutureDeleteOrUpdateFunctionBinderProvider(context))
            add(InstantDeleteOrUpdateFunctionBinderProvider(context))
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
                    columnTypes = targetTypes,
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

    /** Searches 1 way to read it from a statement */
    fun findStatementValueReader(output: XType, affinity: SQLTypeAffinity?): StatementValueReader? {
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
                typeConverterStore.findConverterFromStatement(
                    columnTypes = targetTypes,
                    output = output,
                ) ?: return null
            return CompositeAdapter(
                output,
                getAllColumnAdapters(converter.from).first(),
                null,
                converter,
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
     * findStatementValueReader.
     */
    fun findColumnTypeAdapter(
        out: XType,
        affinity: SQLTypeAffinity?,
        skipDefaultConverter: Boolean,
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
                    columnTypes = targetTypes,
                ) ?: return null
            // ok found a converter, try the reverse now
            val fromStmt =
                typeConverterStore.reverse(intoStatement)
                    ?: typeConverterStore.findTypeConverter(intoStatement.to, out)
                    ?: return null
            return CompositeAdapter(
                out,
                getAllColumnAdapters(intoStatement.to).first(),
                intoStatement,
                fromStmt,
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
        affinity: SQLTypeAffinity?,
    ): ColumnTypeAdapter? {
        val typeElement = type.typeElement
        if (typeElement?.isValueClass() == true) {
            // Extract the type value of the Value class element
            val underlyingInfo = typeElement.getValueClassUnderlyingInfo()
            if (underlyingInfo.constructor.isPrivate() || underlyingInfo.getter == null) {
                return null
            }
            val underlyingTypeColumnAdapter =
                findColumnTypeAdapter(
                    // Find an adapter for the non-null underlying type, nullability will be handled
                    // by the value class adapter.
                    out =
                        try {
                            // Workaround for KSP2
                            underlyingInfo.parameter.asMemberOf(type).makeNonNullable()
                        } catch (ex: Throwable) {
                            underlyingInfo.parameter.type.makeNonNullable()
                        },
                    affinity = affinity,
                    skipDefaultConverter = false,
                ) ?: return null

            return ValueClassConverterWrapper(
                valueTypeColumnAdapter = underlyingTypeColumnAdapter,
                affinity = underlyingTypeColumnAdapter.typeAffinity,
                out = type,
                valuePropertyName = underlyingInfo.parameter.name,
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

    fun findDeleteOrUpdateFunctionBinder(typeMirror: XType): DeleteOrUpdateFunctionBinder {
        return deleteOrUpdateBinderProvider.first { it.matches(typeMirror) }.provide(typeMirror)
    }

    fun findInsertFunctionBinder(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>,
    ): InsertOrUpsertFunctionBinder {
        return insertOrUpsertBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, params, false)
    }

    fun findUpsertFunctionBinder(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>,
    ): InsertOrUpsertFunctionBinder {
        return insertOrUpsertBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, params, true)
    }

    fun findQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit = {},
    ): QueryResultBinder {
        return findQueryResultBinder(typeMirror, query, TypeAdapterExtras().apply(extrasCreator))
    }

    fun findQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras,
    ): QueryResultBinder {
        return queryResultBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, query, extras)
    }

    fun findCoroutineQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras,
    ): QueryResultBinder {
        return coroutineQueryResultBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, query, extras)
    }

    fun findPreparedQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery,
    ): PreparedQueryResultBinder {
        return preparedQueryResultBinderProviders
            .first { it.matches(typeMirror) }
            .provide(typeMirror, query)
    }

    fun findPreparedQueryResultAdapter(typeMirror: XType, query: ParsedQuery) =
        PreparedQueryResultAdapter.create(typeMirror, query.type)

    fun findDeleteOrUpdateAdapter(typeMirror: XType): DeleteOrUpdateFunctionAdapter? {
        return DeleteOrUpdateFunctionAdapter.create(typeMirror)
    }

    fun findInsertAdapter(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>,
    ): InsertOrUpsertFunctionAdapter? {
        return InsertOrUpsertFunctionAdapter.createInsert(context, typeMirror, params)
    }

    fun findUpsertAdapter(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>,
    ): InsertOrUpsertFunctionAdapter? {
        return InsertOrUpsertFunctionAdapter.createUpsert(context, typeMirror, params)
    }

    fun findQueryResultAdapter(
        typeMirror: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit = {},
    ): QueryResultAdapter? {
        return findQueryResultAdapter(typeMirror, query, TypeAdapterExtras().apply(extrasCreator))
    }

    fun findQueryResultAdapter(
        typeMirror: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras,
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
                        componentType.boxed().makeNonNullable(),
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
                resultAdapter = SingleItemQueryResultAdapter(rowAdapter),
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
                resultAdapter = SingleItemQueryResultAdapter(rowAdapter),
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
                    valueTypeArg,
                )

            val resultAdapter = findQueryResultAdapter(mapType, query, extras) ?: return null
            return ImmutableMapQueryResultAdapter(
                context = context,
                parsedQuery = query,
                keyTypeArg = keyTypeArg,
                valueTypeArg = valueTypeArg,
                resultAdapter = resultAdapter,
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

            val mapKeyColumn = getMapColumnName(context, query, keyTypeArg)
            val mapValueColumn = getMapColumnName(context, query, valueTypeArg)

            val keyRowAdapter =
                findRowAdapter(typeMirror = keyTypeArg, query = query, columnName = mapKeyColumn)
                    ?: return null

            val valueRowAdapter =
                findRowAdapter(
                    typeMirror = valueTypeArg,
                    query = query,
                    columnName = mapValueColumn,
                ) ?: return null

            validateMapKeyTypeArg(
                context = context,
                keyTypeArg = keyTypeArg,
                keyReader = findStatementValueReader(keyTypeArg, null),
                keyColumnName = mapKeyColumn,
            )
            validateMapValueTypeArg(
                context = context,
                valueTypeArg = valueTypeArg,
                valueReader = findStatementValueReader(valueTypeArg, null),
                valueColumnName = mapValueColumn,
            )
            return GuavaImmutableMultimapQueryResultAdapter(
                context = context,
                parsedQuery = query,
                keyTypeArg = keyTypeArg,
                valueTypeArg = valueTypeArg,
                keyRowAdapter = keyRowAdapter,
                valueRowAdapter = valueRowAdapter,
                immutableClassName = immutableClassName,
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

            val mapKeyColumn =
                getMapColumnName(
                    context = context,
                    query = query,
                    // If the map is a SparseArray get the key column info from the declared type
                    // itself
                    type = if (mapType.isSparseArray()) typeMirror else keyTypeArg,
                )

            val keyRowAdapter =
                findRowAdapter(typeMirror = keyTypeArg, query = query, columnName = mapKeyColumn)
                    ?: return null

            validateMapKeyTypeArg(
                context = context,
                keyTypeArg = keyTypeArg,
                keyReader = findStatementValueReader(keyTypeArg, null),
                keyColumnName = mapKeyColumn,
            )

            val mapValueResultAdapter =
                findMapValueResultAdapter(query = query, mapValueTypeArg = mapValueTypeArg)
                    ?: return null
            return MapQueryResultAdapter(
                context = context,
                parsedQuery = query,
                mapValueResultAdapter =
                    MapValueResultAdapter.NestedMapValueResultAdapter(
                        keyRowAdapter = keyRowAdapter,
                        keyTypeArg = keyTypeArg,
                        mapType = mapType,
                        mapValueResultAdapter = mapValueResultAdapter,
                    ),
            )
        }
        return null
    }

    private fun checkTypeNullability(
        searchingType: XType,
        extras: TypeAdapterExtras,
        typeKeyword: String = "Collection",
        arrayComponentType: XType? = null,
    ) {
        if (context.codeLanguage != CodeLanguage.KOTLIN) {
            return
        }

        val collectionType: XType =
            extras.getData(ObservableQueryResultBinderProvider.OriginalTypeArg::class)?.original
                ?: searchingType

        if (collectionType.nullability == XNullability.NULLABLE) {
            context.logger.w(
                Warning.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE,
                ProcessorErrors.nullableCollectionOrArrayReturnTypeInDaoFunction(
                    searchingType.asTypeName().toString(context.codeLanguage),
                    typeKeyword,
                ),
            )
        }

        // Since Array has typeArg in the componentType and not typeArguments, need a special check.
        if (arrayComponentType != null && arrayComponentType.nullability == XNullability.NULLABLE) {
            context.logger.w(
                Warning.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE,
                ProcessorErrors.nullableComponentInDaoFunctionReturnType(
                    searchingType.asTypeName().toString(context.codeLanguage)
                ),
            )
            return
        }

        collectionType.typeArguments.forEach { typeArg ->
            if (typeArg.nullability == XNullability.NULLABLE) {
                context.logger.w(
                    Warning.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE,
                    ProcessorErrors.nullableComponentInDaoFunctionReturnType(
                        searchingType.asTypeName().toString(context.codeLanguage)
                    ),
                )
            }
        }
    }

    private fun findMapValueResultAdapter(
        query: ParsedQuery,
        mapValueTypeArg: XType,
    ): MapValueResultAdapter? {
        val collectionTypeRaw =
            context.processingEnv.requireType(CommonTypeNames.COLLECTION).rawType
        if (collectionTypeRaw.isAssignableFrom(mapValueTypeArg.rawType)) {
            // The Map's value type argument is assignable to a Collection, we need to make
            // sure it is either a List or a Set.
            val listTypeRaw =
                context.processingEnv.requireType(CommonTypeNames.MUTABLE_LIST).rawType
            val setTypeRaw = context.processingEnv.requireType(CommonTypeNames.MUTABLE_SET).rawType
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
            val mapValueColumnName = getMapColumnName(context, query, valueTypeArg)

            val valueRowAdapter =
                findRowAdapter(
                    typeMirror = valueTypeArg,
                    query = query,
                    columnName = mapValueColumnName,
                ) ?: return null

            validateMapValueTypeArg(
                context = context,
                valueTypeArg = valueTypeArg,
                valueReader = findStatementValueReader(valueTypeArg, null),
                valueColumnName = mapValueColumnName,
            )

            return MapValueResultAdapter.EndMapValueResultAdapter(
                valueRowAdapter = valueRowAdapter,
                valueTypeArg = valueTypeArg,
                valueCollectionType = collectionValueType,
            )
        } else if (mapValueTypeArg.isTypeOf(java.util.Map::class)) {
            val keyTypeArg = mapValueTypeArg.typeArguments[0].extendsBoundOrSelf()
            val valueTypeArg = mapValueTypeArg.typeArguments[1].extendsBoundOrSelf()

            val keyRowAdapter =
                findRowAdapter(
                    typeMirror = keyTypeArg,
                    query = query,
                    columnName = getMapColumnName(context, query, keyTypeArg),
                ) ?: return null
            val valueMapAdapter =
                findMapValueResultAdapter(query = query, mapValueTypeArg = valueTypeArg)
                    ?: return null
            return MapValueResultAdapter.NestedMapValueResultAdapter(
                keyRowAdapter = keyRowAdapter,
                keyTypeArg = keyTypeArg,
                mapType = MultimapQueryResultAdapter.MapType.DEFAULT,
                mapValueResultAdapter = valueMapAdapter,
            )
        } else {
            val mapValueColumnName = getMapColumnName(context, query, mapValueTypeArg)
            val valueRowAdapter =
                findRowAdapter(
                    typeMirror = mapValueTypeArg,
                    query = query,
                    columnName = mapValueColumnName,
                ) ?: return null

            validateMapValueTypeArg(
                context = context,
                valueTypeArg = mapValueTypeArg,
                valueReader = findStatementValueReader(mapValueTypeArg, null),
                valueColumnName = mapValueColumnName,
            )
            return MapValueResultAdapter.EndMapValueResultAdapter(
                valueRowAdapter = valueRowAdapter,
                valueTypeArg = mapValueTypeArg,
                valueCollectionType = null,
            )
        }
    }

    /**
     * Find a converter from statement to the given type mirror. If there is information about the
     * query result, we try to use it to accept *any* data class.
     */
    fun findRowAdapter(
        typeMirror: XType,
        query: ParsedQuery,
        columnName: String? = null,
    ): RowAdapter? {
        if (typeMirror.isError()) {
            return null
        }

        val typeElement = typeMirror.typeElement
        if (
            typeElement != null &&
                !typeMirror.asTypeName().isPrimitive &&
                !typeMirror.isKotlinUnit()
        ) {
            if (typeMirror.typeArguments.isNotEmpty()) {
                // TODO one day support this
                return null
            }
            val resultInfo = query.resultInfo

            val (rowAdapter, rowAdapterLogs) =
                if (resultInfo != null && query.errors.isEmpty() && resultInfo.error == null) {
                    // if result info is not null, first try a data class row adapter
                    context.collectLogs { subContext ->
                        val dataClass =
                            DataClassProcessor.createFor(
                                    context = subContext,
                                    element = typeElement,
                                    bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                                    parent = null,
                                )
                                .process()
                        DataClassRowAdapter(
                            context = subContext,
                            info = resultInfo,
                            query = query,
                            dataClass = dataClass,
                            out = typeMirror,
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
                        out = typeMirror,
                    )
                }
            }

            if (rowAdapter != null && rowAdapterLogs?.hasErrors() != true) {
                rowAdapterLogs?.writeTo(context)
                return rowAdapter
            }

            if (columnName != null) {
                val singleNamedColumn =
                    findStatementValueReader(
                        typeMirror,
                        query.resultInfo?.columns?.find { it.name == columnName }?.type,
                    )
                if (singleNamedColumn != null) {
                    return SingleNamedColumnRowAdapter(singleNamedColumn, columnName)
                }
            }

            if ((resultInfo?.columns?.size ?: 1) == 1) {
                val singleColumn =
                    findStatementValueReader(typeMirror, resultInfo?.columns?.get(0)?.type)
                if (singleColumn != null) {
                    return SingleColumnRowAdapter(singleColumn)
                }
            }
            // if we tried, return its errors
            if (rowAdapter != null) {
                rowAdapterLogs?.writeTo(context)
                return rowAdapter
            }

            // use data class adapter as a last resort.
            // this happens when @RawQuery or @SkipVerification is used.
            if (
                query.resultInfo == null &&
                    typeMirror.isNotVoid() &&
                    typeMirror.isNotVoidObject() &&
                    typeMirror.isNotKotlinUnit()
            ) {
                val dataClass =
                    DataClassProcessor.createFor(
                            context = context,
                            element = typeElement,
                            bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                            parent = null,
                        )
                        .process()
                return DataClassRowAdapter(
                    context = context,
                    info = null,
                    query = query,
                    dataClass = dataClass,
                    out = typeMirror,
                )
            }
            return null
        } else {
            if (columnName != null) {
                val singleNamedColumn =
                    findStatementValueReader(
                        typeMirror,
                        query.resultInfo?.columns?.find { it.name == columnName }?.type,
                    )
                if (singleNamedColumn != null) {
                    return SingleNamedColumnRowAdapter(singleNamedColumn, columnName)
                }
            }
            val singleColumn = findStatementValueReader(typeMirror, null) ?: return null
            return SingleColumnRowAdapter(singleColumn)
        }
    }

    fun findQueryParameterAdapter(
        typeMirror: XType,
        isMultipleParameter: Boolean,
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
