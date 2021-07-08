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

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isEnum
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.GuavaBaseTypeNames
import androidx.room.ext.isEntityElement
import androidx.room.ext.isNotByte
import androidx.room.ext.isNotKotlinUnit
import androidx.room.ext.isNotVoid
import androidx.room.ext.isNotVoidObject
import androidx.room.parser.ParsedQuery
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.Context
import androidx.room.processor.EntityProcessor
import androidx.room.processor.FieldProcessor
import androidx.room.processor.PojoProcessor
import androidx.room.solver.binderprovider.CoroutineFlowResultBinderProvider
import androidx.room.solver.binderprovider.CursorQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceFactoryQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.GuavaListenableFutureQueryResultBinderProvider
import androidx.room.solver.binderprovider.InstantQueryResultBinderProvider
import androidx.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import androidx.room.solver.binderprovider.PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxCallableQueryResultBinderProvider
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
import androidx.room.solver.query.result.GuavaOptionalQueryResultAdapter
import androidx.room.solver.query.result.ImmutableListQueryResultAdapter
import androidx.room.solver.query.result.ImmutableMapQueryResultAdapter
import androidx.room.solver.query.result.ListQueryResultAdapter
import androidx.room.solver.query.result.MapQueryResultAdapter
import androidx.room.solver.query.result.OptionalQueryResultAdapter
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.solver.query.result.QueryResultAdapter
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.query.result.RowAdapter
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.solver.query.result.SingleEntityQueryResultAdapter
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.solver.shortcut.binderprovider.DeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InstantDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InstantInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableInsertMethodBinderProvider
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.solver.shortcut.result.InsertMethodAdapter
import androidx.room.solver.types.BoxedBooleanToBoxedIntConverter
import androidx.room.solver.types.BoxedPrimitiveColumnTypeAdapter
import androidx.room.solver.types.ByteArrayColumnTypeAdapter
import androidx.room.solver.types.ByteBufferColumnTypeAdapter
import androidx.room.solver.types.ColumnTypeAdapter
import androidx.room.solver.types.CompositeAdapter
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.CursorValueReader
import androidx.room.solver.types.EnumColumnTypeAdapter
import androidx.room.solver.types.NoOpConverter
import androidx.room.solver.types.PrimitiveBooleanToIntConverter
import androidx.room.solver.types.PrimitiveColumnTypeAdapter
import androidx.room.solver.types.StatementValueBinder
import androidx.room.solver.types.StringColumnTypeAdapter
import androidx.room.solver.types.TypeConverter
import androidx.room.vo.ShortcutQueryParameter
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java.util.LinkedList

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
/**
 * Holds all type adapters and can create on demand composite type adapters to convert a type into a
 * database column.
 */
class TypeAdapterStore private constructor(
    val context: Context,
    /**
     * first type adapter has the highest priority
     */
    private val columnTypeAdapters: List<ColumnTypeAdapter>,
    /**
     * first converter has the highest priority
     */
    private val typeConverters: List<TypeConverter>
) {

    companion object {
        fun copy(context: Context, store: TypeAdapterStore): TypeAdapterStore {
            return TypeAdapterStore(
                context = context,
                columnTypeAdapters = store.columnTypeAdapters,
                typeConverters = store.typeConverters
            )
        }

        fun create(context: Context, vararg extras: Any): TypeAdapterStore {
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

            val primitives = PrimitiveColumnTypeAdapter
                .createPrimitiveAdapters(context.processingEnv)
            primitives.forEach(::addColumnAdapter)
            BoxedPrimitiveColumnTypeAdapter
                .createBoxedPrimitiveAdapters(primitives)
                .forEach(::addColumnAdapter)
            StringColumnTypeAdapter.create(context.processingEnv).forEach(::addColumnAdapter)
            ByteArrayColumnTypeAdapter.create(context.processingEnv).forEach(::addColumnAdapter)
            ByteBufferColumnTypeAdapter.create(context.processingEnv).forEach(::addColumnAdapter)
            PrimitiveBooleanToIntConverter.create(context.processingEnv).forEach(::addTypeConverter)
            BoxedBooleanToBoxedIntConverter.create(context.processingEnv)
                .forEach(::addTypeConverter)
            return TypeAdapterStore(
                context = context, columnTypeAdapters = adapters,
                typeConverters = converters
            )
        }
    }

    val queryResultBinderProviders: List<QueryResultBinderProvider> =
        mutableListOf<QueryResultBinderProvider>().apply {
            add(CursorQueryResultBinderProvider(context))
            add(LiveDataQueryResultBinderProvider(context))
            add(GuavaListenableFutureQueryResultBinderProvider(context))
            addAll(RxQueryResultBinderProvider.getAll(context))
            addAll(RxCallableQueryResultBinderProvider.getAll(context))
            add(DataSourceQueryResultBinderProvider(context))
            add(DataSourceFactoryQueryResultBinderProvider(context))
            add(PagingSourceQueryResultBinderProvider(context))
            add(CoroutineFlowResultBinderProvider(context))
            add(InstantQueryResultBinderProvider(context))
        }

    val preparedQueryResultBinderProviders: List<PreparedQueryResultBinderProvider> =
        mutableListOf<PreparedQueryResultBinderProvider>().apply {
            addAll(RxPreparedQueryResultBinderProvider.getAll(context))
            add(GuavaListenableFuturePreparedQueryResultBinderProvider(context))
            add(InstantPreparedQueryResultBinderProvider(context))
        }

    val insertBinderProviders: List<InsertMethodBinderProvider> =
        mutableListOf<InsertMethodBinderProvider>().apply {
            addAll(RxCallableInsertMethodBinderProvider.getAll(context))
            add(GuavaListenableFutureInsertMethodBinderProvider(context))
            add(InstantInsertMethodBinderProvider(context))
        }

    val deleteOrUpdateBinderProvider: List<DeleteOrUpdateMethodBinderProvider> =
        mutableListOf<DeleteOrUpdateMethodBinderProvider>().apply {
            addAll(RxCallableDeleteOrUpdateMethodBinderProvider.getAll(context))
            add(GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(context))
            add(InstantDeleteOrUpdateMethodBinderProvider(context))
        }

    // type mirrors that be converted into columns w/o an extra converter
    private val knownColumnTypeMirrors by lazy {
        columnTypeAdapters.map { it.out }
    }

    /**
     * Searches 1 way to bind a value into a statement.
     */
    fun findStatementValueBinder(
        input: XType,
        affinity: SQLTypeAffinity?
    ): StatementValueBinder? {
        if (input.isError()) {
            return null
        }
        val adapter = findDirectAdapterFor(input, affinity)
        if (adapter != null) {
            return adapter
        }

        fun findTypeConverterAdapter(): ColumnTypeAdapter? {
            val targetTypes = targetTypeMirrorsFor(affinity)
            val binder = findTypeConverter(input, targetTypes) ?: return null
            // columnAdapter should not be null but we are receiving errors on crash in `first()` so
            // this safeguard allows us to dispatch the real problem to the user (e.g. why we couldn't
            // find the right adapter)
            val columnAdapter = getAllColumnAdapters(binder.to).firstOrNull() ?: return null
            return CompositeAdapter(input, columnAdapter, binder, null)
        }

        val adapterByTypeConverter = findTypeConverterAdapter()
        if (adapterByTypeConverter != null) {
            return adapterByTypeConverter
        }
        val enumAdapter = createEnumTypeAdapter(input)
        if (enumAdapter != null) {
            return enumAdapter
        }
        return null
    }

    /**
     * Returns which entities targets the given affinity.
     */
    private fun targetTypeMirrorsFor(affinity: SQLTypeAffinity?): List<XType> {
        val specifiedTargets = affinity?.getTypeMirrors(context.processingEnv)
        return if (specifiedTargets == null || specifiedTargets.isEmpty()) {
            knownColumnTypeMirrors
        } else {
            specifiedTargets
        }
    }

    /**
     * Searches 1 way to read it from cursor
     */
    fun findCursorValueReader(output: XType, affinity: SQLTypeAffinity?): CursorValueReader? {
        if (output.isError()) {
            return null
        }
        val adapter = findColumnTypeAdapter(output, affinity, skipEnumConverter = true)
        if (adapter != null) {
            // two way is better
            return adapter
        }

        fun findTypeConverterAdapter(): ColumnTypeAdapter? {
            val targetTypes = targetTypeMirrorsFor(affinity)
            val converter = findTypeConverter(targetTypes, output) ?: return null
            return CompositeAdapter(
                output,
                getAllColumnAdapters(converter.from).first(), null, converter
            )
        }

        // we could not find a two way version, search for anything
        val typeConverterAdapter = findTypeConverterAdapter()
        if (typeConverterAdapter != null) {
            return typeConverterAdapter
        }

        val enumAdapter = createEnumTypeAdapter(output)
        if (enumAdapter != null) {
            return enumAdapter
        }

        return null
    }

    /**
     * Tries to reverse the converter going through the same nodes, if possible.
     */
    @VisibleForTesting
    fun reverse(converter: TypeConverter): TypeConverter? {
        return when (converter) {
            is NoOpConverter -> converter
            is CompositeTypeConverter -> {
                val r1 = reverse(converter.conv1) ?: return null
                val r2 = reverse(converter.conv2) ?: return null
                CompositeTypeConverter(r2, r1)
            }
            else -> {
                typeConverters.firstOrNull {
                    it.from.isSameType(converter.to) &&
                        it.to.isSameType(converter.from)
                }
            }
        }
    }

    /**
     * Finds a two way converter, if you need 1 way, use findStatementValueBinder or
     * findCursorValueReader.
     */
    fun findColumnTypeAdapter(
        out: XType,
        affinity: SQLTypeAffinity?,
        skipEnumConverter: Boolean
    ): ColumnTypeAdapter? {
        if (out.isError()) {
            return null
        }
        val adapter = findDirectAdapterFor(out, affinity)
        if (adapter != null) {
            return adapter
        }

        fun findTypeConverterAdapter(): ColumnTypeAdapter? {
            val targetTypes = targetTypeMirrorsFor(affinity)
            val intoStatement = findTypeConverter(out, targetTypes) ?: return null
            // ok found a converter, try the reverse now
            val fromCursor = reverse(intoStatement)
                ?: findTypeConverter(intoStatement.to, out) ?: return null
            return CompositeAdapter(
                out, getAllColumnAdapters(intoStatement.to).first(), intoStatement, fromCursor
            )
        }

        val adapterByTypeConverter = findTypeConverterAdapter()
        if (adapterByTypeConverter != null) {
            return adapterByTypeConverter
        }
        if (!skipEnumConverter) {
            val enumAdapter = createEnumTypeAdapter(out)
            if (enumAdapter != null) {
                return enumAdapter
            }
        }
        return null
    }

    private fun createEnumTypeAdapter(type: XType): ColumnTypeAdapter? {
        val typeElement = type.typeElement ?: return null
        if (typeElement.isEnum()) {
            return EnumColumnTypeAdapter(typeElement)
        }
        return null
    }

    private fun findDirectAdapterFor(
        out: XType,
        affinity: SQLTypeAffinity?
    ): ColumnTypeAdapter? {
        return getAllColumnAdapters(out).firstOrNull {
            affinity == null || it.typeAffinity == affinity
        }
    }

    fun findTypeConverter(input: XType, output: XType): TypeConverter? {
        return findTypeConverter(listOf(input), listOf(output))
    }

    fun findDeleteOrUpdateMethodBinder(typeMirror: XType): DeleteOrUpdateMethodBinder {
        return deleteOrUpdateBinderProvider.first {
            it.matches(typeMirror)
        }.provide(typeMirror)
    }

    fun findInsertMethodBinder(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder {
        return insertBinderProviders.first {
            it.matches(typeMirror)
        }.provide(typeMirror, params)
    }

    fun findQueryResultBinder(typeMirror: XType, query: ParsedQuery): QueryResultBinder {
        return queryResultBinderProviders.first {
            it.matches(typeMirror)
        }.provide(typeMirror, query)
    }

    fun findPreparedQueryResultBinder(
        typeMirror: XType,
        query: ParsedQuery
    ): PreparedQueryResultBinder {
        return preparedQueryResultBinderProviders.first {
            it.matches(typeMirror)
        }.provide(typeMirror, query)
    }

    fun findPreparedQueryResultAdapter(typeMirror: XType, query: ParsedQuery) =
        PreparedQueryResultAdapter.create(typeMirror, query.type)

    fun findDeleteOrUpdateAdapter(typeMirror: XType): DeleteOrUpdateMethodAdapter? {
        return DeleteOrUpdateMethodAdapter.create(typeMirror)
    }

    fun findInsertAdapter(
        typeMirror: XType,
        params: List<ShortcutQueryParameter>
    ): InsertMethodAdapter? {
        return InsertMethodAdapter.create(typeMirror, params)
    }

    fun findQueryResultAdapter(typeMirror: XType, query: ParsedQuery): QueryResultAdapter? {
        if (typeMirror.isError()) {
            return null
        }
        // TODO: (b/192068912) Refactor the following since this if-else cascade has gotten large
        if (typeMirror.isArray() && typeMirror.componentType.isNotByte()) {
            val rowAdapter =
                findRowAdapter(typeMirror.componentType, query) ?: return null
            return ArrayQueryResultAdapter(rowAdapter)
        } else if (typeMirror.typeArguments.isEmpty()) {
            val rowAdapter = findRowAdapter(typeMirror, query) ?: return null
            return SingleEntityQueryResultAdapter(rowAdapter)
        } else if (typeMirror.rawType.typeName == GuavaBaseTypeNames.OPTIONAL) {
            // Handle Guava Optional by unpacking its generic type argument and adapting that.
            // The Optional adapter will reappend the Optional type.
            val typeArg = typeMirror.typeArguments.first()
            // use nullable when finding row adapter as non-null adapters might return
            // default values
            val rowAdapter = findRowAdapter(typeArg.makeNullable(), query) ?: return null
            return GuavaOptionalQueryResultAdapter(
                typeArg = typeArg,
                resultAdapter = SingleEntityQueryResultAdapter(rowAdapter)
            )
        } else if (typeMirror.rawType.typeName == CommonTypeNames.OPTIONAL) {
            // Handle java.util.Optional similarly.
            val typeArg = typeMirror.typeArguments.first()
            // use nullable when finding row adapter as non-null adapters might return
            // default values
            val rowAdapter = findRowAdapter(typeArg.makeNullable(), query) ?: return null
            return OptionalQueryResultAdapter(
                typeArg = typeArg,
                resultAdapter = SingleEntityQueryResultAdapter(rowAdapter)
            )
        } else if (typeMirror.isTypeOf(ImmutableList::class)) {
            val typeArg = typeMirror.typeArguments.first().extendsBoundOrSelf()
            val rowAdapter = findRowAdapter(typeArg, query) ?: return null
            return ImmutableListQueryResultAdapter(
                typeArg = typeArg,
                rowAdapter = rowAdapter
            )
        } else if (typeMirror.isTypeOf(java.util.List::class)) {
            val typeArg = typeMirror.typeArguments.first().extendsBoundOrSelf()
            val rowAdapter = findRowAdapter(typeArg, query) ?: return null
            return ListQueryResultAdapter(
                typeArg = typeArg,
                rowAdapter = rowAdapter
            )
        } else if (typeMirror.isTypeOf(ImmutableMap::class)) {
            val keyTypeArg = typeMirror.typeArguments[0].extendsBoundOrSelf()
            val valueTypeArg = typeMirror.typeArguments[1].extendsBoundOrSelf()

            // Create a type mirror for a regular Map in order to use MapQueryResultAdapter. This
            // avoids code duplication as Immutable Map can be initialized by creating an immutable
            // copy of a regular map.
            val mapType = context.processingEnv.getDeclaredType(
                context.processingEnv.requireTypeElement(Map::class),
                keyTypeArg,
                valueTypeArg
            )
            val resultAdapter = findQueryResultAdapter(mapType, query = query) ?: return null
            return ImmutableMapQueryResultAdapter(
                keyTypeArg = keyTypeArg,
                valueTypeArg = valueTypeArg,
                resultAdapter = resultAdapter
            )
        } else if (typeMirror.isTypeOf(java.util.Map::class)) {
            // TODO: Handle nested collection values in the map
            // TODO: Verify that hashCode() and equals() are declared by the keyTypeArg

            val keyTypeArg = typeMirror.typeArguments[0].extendsBoundOrSelf()
            val mapValueTypeArg = typeMirror.typeArguments[1].extendsBoundOrSelf()

            if (mapValueTypeArg.typeElement == null) {
                context.logger.e(
                    "Multimap 'value' collection type argument does not represent a class. " +
                        "Found $mapValueTypeArg."
                )
                return null
            }

            val collectionTypeRaw = context.COMMON_TYPES.READONLY_COLLECTION.rawType

            if (collectionTypeRaw.isAssignableFrom(mapValueTypeArg.rawType)) {
                // The Map's value type argument is assignable to a Collection, we need to make
                // sure it is either a list or a set.
                if (
                    mapValueTypeArg.isTypeOf(java.util.List::class) ||
                    mapValueTypeArg.isTypeOf(java.util.Set::class)
                ) {
                    val valueTypeArg =
                        mapValueTypeArg.typeArguments.single().extendsBoundOrSelf()
                    return MapQueryResultAdapter(
                        keyTypeArg = keyTypeArg,
                        valueTypeArg = valueTypeArg,
                        keyRowAdapter = findRowAdapter(keyTypeArg, query) ?: return null,
                        valueRowAdapter = findRowAdapter(valueTypeArg, query) ?: return null,
                        valueCollectionType = mapValueTypeArg
                    )
                } else {
                    context.logger.e(
                        "Multimap 'value' collection type must be a List or Set. Found " +
                            "${mapValueTypeArg.typeName}."
                    )
                }
            } else {
                return MapQueryResultAdapter(
                    keyTypeArg = keyTypeArg,
                    valueTypeArg = mapValueTypeArg,
                    keyRowAdapter = findRowAdapter(keyTypeArg, query) ?: return null,
                    valueRowAdapter = findRowAdapter(mapValueTypeArg, query) ?: return null,
                    valueCollectionType = null
                )
            }
        }
        return null
    }

    /**
     * Find a converter from cursor to the given type mirror.
     * If there is information about the query result, we try to use it to accept *any* POJO.
     */
    fun findRowAdapter(typeMirror: XType, query: ParsedQuery): RowAdapter? {
        if (typeMirror.isError()) {
            return null
        }
        val typeElement = typeMirror.typeElement
        if (typeElement != null && !typeMirror.typeName.isPrimitive) {
            if (typeMirror.typeArguments.isNotEmpty()) {
                // TODO one day support this
                return null
            }
            val resultInfo = query.resultInfo

            val (rowAdapter, rowAdapterLogs) = if (resultInfo != null && query.errors.isEmpty() &&
                resultInfo.error == null
            ) {
                // if result info is not null, first try a pojo row adapter
                context.collectLogs { subContext ->
                    val pojo = PojoProcessor.createFor(
                        context = subContext,
                        element = typeElement,
                        bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                        parent = null
                    ).process()
                    PojoRowAdapter(
                        context = subContext,
                        info = resultInfo,
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
                        EntityProcessor(
                            context = context,
                            element = typeElement
                        ).process()
                    )
                }
            }

            if (rowAdapter != null && rowAdapterLogs?.hasErrors() != true) {
                rowAdapterLogs?.writeTo(context)
                return rowAdapter
            }

            if ((resultInfo?.columns?.size ?: 1) == 1) {
                val singleColumn = findCursorValueReader(
                    typeMirror,
                    resultInfo?.columns?.get(0)?.type
                )
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
            if (query.resultInfo == null &&
                typeMirror.isNotVoid() &&
                typeMirror.isNotVoidObject() &&
                typeMirror.isNotKotlinUnit()
            ) {
                val pojo = PojoProcessor.createFor(
                    context = context,
                    element = typeElement,
                    bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                    parent = null
                ).process()
                return PojoRowAdapter(
                    context = context,
                    info = null,
                    pojo = pojo,
                    out = typeMirror
                )
            }
            return null
        } else {
            val singleColumn = findCursorValueReader(typeMirror, null) ?: return null
            return SingleColumnRowAdapter(singleColumn)
        }
    }

    fun findQueryParameterAdapter(
        typeMirror: XType,
        isMultipleParameter: Boolean
    ): QueryParameterAdapter? {
        if (context.COMMON_TYPES.READONLY_COLLECTION.rawType.isAssignableFrom(typeMirror)) {
            val typeArg = typeMirror.typeArguments.first().extendsBoundOrSelf()
            // An adapter for the collection type arg wrapped in the built-in collection adapter.
            val wrappedCollectionAdapter = findStatementValueBinder(typeArg, null)?.let {
                CollectionQueryParameterAdapter(it)
            }
            // An adapter for the collection itself, likely a user provided type converter for the
            // collection.
            val directCollectionAdapter = findStatementValueBinder(typeMirror, null)?.let {
                BasicQueryParameterAdapter(it)
            }
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
            return ArrayQueryParameterAdapter(binder)
        } else {
            val binder = findStatementValueBinder(typeMirror, null) ?: return null
            return BasicQueryParameterAdapter(binder)
        }
    }

    private fun findTypeConverter(input: XType, outputs: List<XType>): TypeConverter? {
        return findTypeConverter(listOf(input), outputs)
    }

    private fun findTypeConverter(input: List<XType>, output: XType): TypeConverter? {
        return findTypeConverter(input, listOf(output))
    }

    private fun findTypeConverter(
        inputs: List<XType>,
        outputs: List<XType>
    ): TypeConverter? {
        if (inputs.isEmpty()) {
            return null
        }
        inputs.forEach { input ->
            if (outputs.any { output -> input.isSameType(output) }) {
                return NoOpConverter(input)
            }
        }

        val excludes = arrayListOf<XType>()

        val queue = LinkedList<TypeConverter>()
        fun List<TypeConverter>.findMatchingConverter(): TypeConverter? {
            // We prioritize exact match over assignable. To do that, this variable keeps any
            // assignable match and if we cannot find exactly same type match, we'll return the
            // assignable match.
            var assignableMatchFallback: TypeConverter? = null
            this.forEach { converter ->
                outputs.forEach { output ->
                    if (output.isSameType(converter.to)) {
                        return converter
                    } else if (assignableMatchFallback == null &&
                        output.isAssignableFrom(converter.to)
                    ) {
                        // if we don't find exact match, we'll return this.
                        assignableMatchFallback = converter
                    }
                }
            }
            return assignableMatchFallback
        }
        inputs.forEach { input ->
            val candidates = getAllTypeConverters(input, excludes)
            val match = candidates.findMatchingConverter()
            if (match != null) {
                return match
            }
            candidates.forEach {
                excludes.add(it.to)
                queue.add(it)
            }
        }
        excludes.addAll(inputs)
        while (queue.isNotEmpty()) {
            val prev = queue.pop()
            val from = prev.to
            val candidates = getAllTypeConverters(from, excludes)
            val match = candidates.findMatchingConverter()
            if (match != null) {
                return CompositeTypeConverter(prev, match)
            }
            candidates.forEach {
                excludes.add(it.to)
                queue.add(CompositeTypeConverter(prev, it))
            }
        }
        return null
    }

    private fun getAllColumnAdapters(input: XType): List<ColumnTypeAdapter> {
        return columnTypeAdapters.filter {
            input.isSameType(it.out)
        }
    }

    /**
     * Returns all type converters that can receive input type and return into another type.
     * The returned list is ordered by priority such that if we have an exact match, it is
     * prioritized.
     */
    private fun getAllTypeConverters(input: XType, excludes: List<XType>): List<TypeConverter> {
        // for input, check assignability because it defines whether we can use the method or not.
        // for excludes, use exact match
        return typeConverters.filter { converter ->
            converter.from.isAssignableFrom(input) &&
                !excludes.any { it.isSameType(converter.to) }
        }.sortedByDescending {
            // if it is the same, prioritize
            if (it.from.isSameType(input)) {
                2
            } else {
                1
            }
        }
    }
}
