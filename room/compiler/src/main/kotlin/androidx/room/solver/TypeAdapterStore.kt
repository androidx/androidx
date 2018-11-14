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

import androidx.room.ext.CommonTypeNames
import androidx.room.ext.GuavaBaseTypeNames
import androidx.room.ext.isAssignableWithoutVariance
import androidx.room.ext.isEntityElement
import androidx.room.ext.typeName
import androidx.room.parser.ParsedQuery
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.Context
import androidx.room.processor.EntityProcessor
import androidx.room.processor.FieldProcessor
import androidx.room.processor.PojoProcessor
import androidx.room.solver.binderprovider.CursorQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceFactoryQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.GuavaListenableFutureQueryResultBinderProvider
import androidx.room.solver.binderprovider.InstantQueryResultBinderProvider
import androidx.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxFlowableQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxMaybeQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxObservableQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxSingleQueryResultBinderProvider
import androidx.room.solver.query.parameter.ArrayQueryParameterAdapter
import androidx.room.solver.query.parameter.BasicQueryParameterAdapter
import androidx.room.solver.query.parameter.CollectionQueryParameterAdapter
import androidx.room.solver.query.parameter.QueryParameterAdapter
import androidx.room.solver.query.result.ArrayQueryResultAdapter
import androidx.room.solver.query.result.EntityRowAdapter
import androidx.room.solver.query.result.GuavaOptionalQueryResultAdapter
import androidx.room.solver.query.result.InstantQueryResultBinder
import androidx.room.solver.query.result.ListQueryResultAdapter
import androidx.room.solver.query.result.OptionalQueryResultAdapter
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.solver.query.result.QueryResultAdapter
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.query.result.RowAdapter
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.solver.query.result.SingleEntityQueryResultAdapter
import androidx.room.solver.types.BoxedBooleanToBoxedIntConverter
import androidx.room.solver.types.BoxedPrimitiveColumnTypeAdapter
import androidx.room.solver.types.ByteArrayColumnTypeAdapter
import androidx.room.solver.types.ColumnTypeAdapter
import androidx.room.solver.types.CompositeAdapter
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.CursorValueReader
import androidx.room.solver.types.NoOpConverter
import androidx.room.solver.types.PrimitiveBooleanToIntConverter
import androidx.room.solver.types.PrimitiveColumnTypeAdapter
import androidx.room.solver.types.StatementValueBinder
import androidx.room.solver.types.StringColumnTypeAdapter
import androidx.room.solver.types.TypeConverter
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.solver.shortcut.result.InsertMethodAdapter
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.solver.shortcut.binder.InstantDeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.InstantInsertMethodBinder
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InstantDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.InstantInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCompletableDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCompletableInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxMaybeDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxMaybeInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxSingleDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxSingleInsertMethodBinderProvider
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.annotations.VisibleForTesting
import java.util.LinkedList
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

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
            return TypeAdapterStore(context = context,
                    columnTypeAdapters = store.columnTypeAdapters,
                    typeConverters = store.typeConverters)
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
                    .createBoxedPrimitiveAdapters(context.processingEnv, primitives)
                    .forEach(::addColumnAdapter)
            addColumnAdapter(StringColumnTypeAdapter(context.processingEnv))
            addColumnAdapter(ByteArrayColumnTypeAdapter(context.processingEnv))
            PrimitiveBooleanToIntConverter.create(context.processingEnv).forEach(::addTypeConverter)
            BoxedBooleanToBoxedIntConverter.create(context.processingEnv)
                    .forEach(::addTypeConverter)
            return TypeAdapterStore(context = context, columnTypeAdapters = adapters,
                    typeConverters = converters)
        }
    }

    val queryResultBinderProviders = listOf(
            CursorQueryResultBinderProvider(context),
            LiveDataQueryResultBinderProvider(context),
            GuavaListenableFutureQueryResultBinderProvider(context),
            RxFlowableQueryResultBinderProvider(context),
            RxObservableQueryResultBinderProvider(context),
            RxMaybeQueryResultBinderProvider(context),
            RxSingleQueryResultBinderProvider(context),
            DataSourceQueryResultBinderProvider(context),
            DataSourceFactoryQueryResultBinderProvider(context),
            InstantQueryResultBinderProvider(context)
    )

    val insertBinderProviders = listOf(
            RxSingleInsertMethodBinderProvider(context),
            RxMaybeInsertMethodBinderProvider(context),
            RxCompletableInsertMethodBinderProvider(context),
            GuavaListenableFutureInsertMethodBinderProvider(context),
            InstantInsertMethodBinderProvider(context)
    )

    val deleteOrUpdateBinderProvider = listOf(
            RxSingleDeleteOrUpdateMethodBinderProvider(context),
            RxMaybeDeleteOrUpdateMethodBinderProvider(context),
            RxCompletableDeleteOrUpdateMethodBinderProvider(context),
            GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(context),
            InstantDeleteOrUpdateMethodBinderProvider(context)
    )

    // type mirrors that be converted into columns w/o an extra converter
    private val knownColumnTypeMirrors by lazy {
        columnTypeAdapters.map { it.out }
    }

    /**
     * Searches 1 way to bind a value into a statement.
     */
    fun findStatementValueBinder(
        input: TypeMirror,
        affinity: SQLTypeAffinity?
    ): StatementValueBinder? {
        if (input.kind == TypeKind.ERROR) {
            return null
        }
        val adapter = findDirectAdapterFor(input, affinity)
        if (adapter != null) {
            return adapter
        }
        val targetTypes = targetTypeMirrorsFor(affinity)
        val binder = findTypeConverter(input, targetTypes) ?: return null
        // columnAdapter should not be null but we are receiving errors on crash in `first()` so
        // this safeguard allows us to dispatch the real problem to the user (e.g. why we couldn't
        // find the right adapter)
        val columnAdapter = getAllColumnAdapters(binder.to).firstOrNull() ?: return null
        return CompositeAdapter(input, columnAdapter, binder, null)
    }

    /**
     * Returns which entities targets the given affinity.
     */
    private fun targetTypeMirrorsFor(affinity: SQLTypeAffinity?): List<TypeMirror> {
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
    fun findCursorValueReader(output: TypeMirror, affinity: SQLTypeAffinity?): CursorValueReader? {
        if (output.kind == TypeKind.ERROR) {
            return null
        }
        val adapter = findColumnTypeAdapter(output, affinity)
        if (adapter != null) {
            // two way is better
            return adapter
        }
        // we could not find a two way version, search for anything
        val targetTypes = targetTypeMirrorsFor(affinity)
        val converter = findTypeConverter(targetTypes, output) ?: return null
        return CompositeAdapter(output,
                getAllColumnAdapters(converter.from).first(), null, converter)
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
                val types = context.processingEnv.typeUtils
                typeConverters.firstOrNull {
                    types.isSameType(it.from, converter.to) && types
                            .isSameType(it.to, converter.from)
                }
            }
        }
    }

    /**
     * Finds a two way converter, if you need 1 way, use findStatementValueBinder or
     * findCursorValueReader.
     */
    fun findColumnTypeAdapter(out: TypeMirror, affinity: SQLTypeAffinity?): ColumnTypeAdapter? {
        if (out.kind == TypeKind.ERROR) {
            return null
        }
        val adapter = findDirectAdapterFor(out, affinity)
        if (adapter != null) {
            return adapter
        }
        val targetTypes = targetTypeMirrorsFor(affinity)
        val intoStatement = findTypeConverter(out, targetTypes) ?: return null
        // ok found a converter, try the reverse now
        val fromCursor = reverse(intoStatement) ?: findTypeConverter(intoStatement.to, out)
        ?: return null
        return CompositeAdapter(out, getAllColumnAdapters(intoStatement.to).first(), intoStatement,
                fromCursor)
    }

    private fun findDirectAdapterFor(
        out: TypeMirror,
        affinity: SQLTypeAffinity?
    ): ColumnTypeAdapter? {
        return getAllColumnAdapters(out).firstOrNull {
            affinity == null || it.typeAffinity == affinity
        }
    }

    fun findTypeConverter(input: TypeMirror, output: TypeMirror): TypeConverter? {
        return findTypeConverter(listOf(input), listOf(output))
    }

    fun findDeleteOrUpdateMethodBinder(typeMirror: TypeMirror): DeleteOrUpdateMethodBinder {
        val adapter = findDeleteOrUpdateAdapter(typeMirror)
        return if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)
            deleteOrUpdateBinderProvider.first {
                it.matches(declared)
            }.provide(declared)
        } else {
            InstantDeleteOrUpdateMethodBinder(adapter)
        }
    }

    fun findInsertMethodBinder(
        typeMirror: TypeMirror,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder {
        return if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)
            insertBinderProviders.first {
                it.matches(declared)
            }.provide(declared, params)
        } else {
            InstantInsertMethodBinder(findInsertAdapter(typeMirror, params))
        }
    }

    fun findQueryResultBinder(typeMirror: TypeMirror, query: ParsedQuery): QueryResultBinder {
        return if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)
            return queryResultBinderProviders.first {
                it.matches(declared)
            }.provide(declared, query)
        } else {
            InstantQueryResultBinder(findQueryResultAdapter(typeMirror, query))
        }
    }

    fun findDeleteOrUpdateAdapter(typeMirror: TypeMirror): DeleteOrUpdateMethodAdapter? {
        return DeleteOrUpdateMethodAdapter.create(typeMirror)
    }

    fun findInsertAdapter(
        typeMirror: TypeMirror,
        params: List<ShortcutQueryParameter>
    ): InsertMethodAdapter? {
        return InsertMethodAdapter.create(typeMirror, params)
    }

    fun findQueryResultAdapter(typeMirror: TypeMirror, query: ParsedQuery): QueryResultAdapter? {
        if (typeMirror.kind == TypeKind.ERROR) {
            return null
        }
        if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)

            if (declared.typeArguments.isEmpty()) {
                val rowAdapter = findRowAdapter(typeMirror, query) ?: return null
                return SingleEntityQueryResultAdapter(rowAdapter)
            } else if (
                    context.processingEnv.typeUtils.erasure(typeMirror).typeName() ==
                    GuavaBaseTypeNames.OPTIONAL) {
                // Handle Guava Optional by unpacking its generic type argument and adapting that.
                // The Optional adapter will reappend the Optional type.
                val typeArg = declared.typeArguments.first()
                val rowAdapter = findRowAdapter(typeArg, query) ?: return null
                return GuavaOptionalQueryResultAdapter(SingleEntityQueryResultAdapter(rowAdapter))
            } else if (
                    context.processingEnv.typeUtils.erasure(typeMirror).typeName() ==
                    CommonTypeNames.OPTIONAL) {
                // Handle java.util.Optional similarly.
                val typeArg = declared.typeArguments.first()
                val rowAdapter = findRowAdapter(typeArg, query) ?: return null
                return OptionalQueryResultAdapter(SingleEntityQueryResultAdapter(rowAdapter))
            } else if (MoreTypes.isTypeOf(java.util.List::class.java, typeMirror)) {
                val typeArg = declared.typeArguments.first()
                val rowAdapter = findRowAdapter(typeArg, query) ?: return null
                return ListQueryResultAdapter(rowAdapter)
            }
            return null
        } else if (typeMirror is ArrayType && typeMirror.componentType.kind != TypeKind.BYTE) {
            val rowAdapter =
                    findRowAdapter(typeMirror.componentType, query) ?: return null
            return ArrayQueryResultAdapter(rowAdapter)
        } else {
            val rowAdapter = findRowAdapter(typeMirror, query) ?: return null
            return SingleEntityQueryResultAdapter(rowAdapter)
        }
    }

    /**
     * Find a converter from cursor to the given type mirror.
     * If there is information about the query result, we try to use it to accept *any* POJO.
     */
    fun findRowAdapter(typeMirror: TypeMirror, query: ParsedQuery): RowAdapter? {
        if (typeMirror.kind == TypeKind.ERROR) {
            return null
        }
        if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)
            if (declared.typeArguments.isNotEmpty()) {
                // TODO one day support this
                return null
            }
            val resultInfo = query.resultInfo

            val (rowAdapter, rowAdapterLogs) = if (resultInfo != null && query.errors.isEmpty() &&
                    resultInfo.error == null) {
                // if result info is not null, first try a pojo row adapter
                context.collectLogs { subContext ->
                    val pojo = PojoProcessor.createFor(
                            context = subContext,
                            element = MoreTypes.asTypeElement(typeMirror),
                            bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                            parent = null
                    ).process()
                    PojoRowAdapter(
                            context = subContext,
                            info = resultInfo,
                            pojo = pojo,
                            out = typeMirror)
                }
            } else {
                Pair(null, null)
            }

            if (rowAdapter == null && query.resultInfo == null) {
                // we don't know what query returns. Check for entity.
                val asElement = MoreTypes.asElement(typeMirror)
                if (asElement.isEntityElement()) {
                    return EntityRowAdapter(EntityProcessor(
                            context = context,
                            element = MoreElements.asType(asElement)
                    ).process())
                }
            }

            if (rowAdapter != null && rowAdapterLogs?.hasErrors() != true) {
                rowAdapterLogs?.writeTo(context.processingEnv)
                return rowAdapter
            }

            if ((resultInfo?.columns?.size ?: 1) == 1) {
                val singleColumn = findCursorValueReader(typeMirror,
                        resultInfo?.columns?.get(0)?.type)
                if (singleColumn != null) {
                    return SingleColumnRowAdapter(singleColumn)
                }
            }
            // if we tried, return its errors
            if (rowAdapter != null) {
                rowAdapterLogs?.writeTo(context.processingEnv)
                return rowAdapter
            }
            if (query.runtimeQueryPlaceholder) {
                // just go w/ pojo and hope for the best. this happens for @RawQuery where we
                // try to guess user's intention and hope that their query fits the result.
                val pojo = PojoProcessor.createFor(
                        context = context,
                        element = MoreTypes.asTypeElement(typeMirror),
                        bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                        parent = null
                ).process()
                return PojoRowAdapter(
                        context = context,
                        info = null,
                        pojo = pojo,
                        out = typeMirror)
            }
            return null
        } else {
            val singleColumn = findCursorValueReader(typeMirror, null) ?: return null
            return SingleColumnRowAdapter(singleColumn)
        }
    }

    fun findQueryParameterAdapter(typeMirror: TypeMirror): QueryParameterAdapter? {
        if (MoreTypes.isType(typeMirror) &&
                (MoreTypes.isTypeOf(java.util.List::class.java, typeMirror) ||
                        MoreTypes.isTypeOf(java.util.Set::class.java, typeMirror))) {
            val declared = MoreTypes.asDeclared(typeMirror)
            val binder = findStatementValueBinder(declared.typeArguments.first(),
                    null)
            if (binder != null) {
                return CollectionQueryParameterAdapter(binder)
            } else {
                // maybe user wants to convert this collection themselves. look for a match
                val collectionBinder = findStatementValueBinder(typeMirror, null) ?: return null
                return BasicQueryParameterAdapter(collectionBinder)
            }
        } else if (typeMirror is ArrayType && typeMirror.componentType.kind != TypeKind.BYTE) {
            val component = typeMirror.componentType
            val binder = findStatementValueBinder(component, null) ?: return null
            return ArrayQueryParameterAdapter(binder)
        } else {
            val binder = findStatementValueBinder(typeMirror, null) ?: return null
            return BasicQueryParameterAdapter(binder)
        }
    }

    private fun findTypeConverter(input: TypeMirror, outputs: List<TypeMirror>): TypeConverter? {
        return findTypeConverter(listOf(input), outputs)
    }

    private fun findTypeConverter(input: List<TypeMirror>, output: TypeMirror): TypeConverter? {
        return findTypeConverter(input, listOf(output))
    }

    private fun findTypeConverter(
        inputs: List<TypeMirror>,
        outputs: List<TypeMirror>
    ): TypeConverter? {
        if (inputs.isEmpty()) {
            return null
        }
        val types = context.processingEnv.typeUtils
        inputs.forEach { input ->
            if (outputs.any { output -> types.isSameType(input, output) }) {
                return NoOpConverter(input)
            }
        }

        val excludes = arrayListOf<TypeMirror>()

        val queue = LinkedList<TypeConverter>()
        fun exactMatch(candidates: List<TypeConverter>): TypeConverter? {
            return candidates.firstOrNull {
                outputs.any { output -> types.isAssignableWithoutVariance(output, it.to) }
            }
        }
        inputs.forEach { input ->
            val candidates = getAllTypeConverters(input, excludes)
            val match = exactMatch(candidates)
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
            val match = exactMatch(candidates)
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

    private fun getAllColumnAdapters(input: TypeMirror): List<ColumnTypeAdapter> {
        return columnTypeAdapters.filter {
            context.processingEnv.typeUtils.isSameType(input, it.out)
        }
    }

    /**
     * Returns all type converters that can receive input type and return into another type.
     * The returned list is ordered by priority such that if we have an exact match, it is
     * prioritized.
     */
    private fun getAllTypeConverters(input: TypeMirror, excludes: List<TypeMirror>):
            List<TypeConverter> {
        val types = context.processingEnv.typeUtils
        // for input, check assignability because it defines whether we can use the method or not.
        // for excludes, use exact match
        return typeConverters.filter { converter ->
            types.isAssignable(input, converter.from) &&
                    !excludes.any { types.isSameType(it, converter.to) }
        }.sortedByDescending {
            // if it is the same, prioritize
            if (types.isSameType(it.from, input)) {
                2
            } else {
                1
            }
        }
    }
}
