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

package android.arch.persistence.room.solver

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ext.hasAnnotation
import android.arch.persistence.room.parser.ParsedQuery
import android.arch.persistence.room.parser.SQLTypeAffinity
import android.arch.persistence.room.processor.Context
import android.arch.persistence.room.processor.EntityProcessor
import android.arch.persistence.room.processor.FieldProcessor
import android.arch.persistence.room.processor.PojoProcessor
import android.arch.persistence.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import android.arch.persistence.room.solver.binderprovider.CursorQueryResultBinderProvider
import android.arch.persistence.room.solver.binderprovider.FlowableQueryResultBinderProvider
import android.arch.persistence.room.solver.binderprovider.InstantQueryResultBinderProvider
import android.arch.persistence.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import android.arch.persistence.room.solver.binderprovider.LivePagedListQueryResultBinderProvider
import android.arch.persistence.room.solver.binderprovider.RxMaybeQueryResultBinderProvider
import android.arch.persistence.room.solver.binderprovider.RxSingleQueryResultBinderProvider
import android.arch.persistence.room.solver.query.parameter.ArrayQueryParameterAdapter
import android.arch.persistence.room.solver.query.parameter.BasicQueryParameterAdapter
import android.arch.persistence.room.solver.query.parameter.CollectionQueryParameterAdapter
import android.arch.persistence.room.solver.query.parameter.QueryParameterAdapter
import android.arch.persistence.room.solver.query.result.ArrayQueryResultAdapter
import android.arch.persistence.room.solver.query.result.EntityRowAdapter
import android.arch.persistence.room.solver.query.result.InstantQueryResultBinder
import android.arch.persistence.room.solver.query.result.ListQueryResultAdapter
import android.arch.persistence.room.solver.query.result.PojoRowAdapter
import android.arch.persistence.room.solver.query.result.QueryResultAdapter
import android.arch.persistence.room.solver.query.result.QueryResultBinder
import android.arch.persistence.room.solver.query.result.RowAdapter
import android.arch.persistence.room.solver.query.result.SingleColumnRowAdapter
import android.arch.persistence.room.solver.query.result.SingleEntityQueryResultAdapter
import android.arch.persistence.room.solver.types.BoxedBooleanToBoxedIntConverter
import android.arch.persistence.room.solver.types.BoxedPrimitiveColumnTypeAdapter
import android.arch.persistence.room.solver.types.ByteArrayColumnTypeAdapter
import android.arch.persistence.room.solver.types.ColumnTypeAdapter
import android.arch.persistence.room.solver.types.CompositeAdapter
import android.arch.persistence.room.solver.types.CompositeTypeConverter
import android.arch.persistence.room.solver.types.CursorValueReader
import android.arch.persistence.room.solver.types.NoOpConverter
import android.arch.persistence.room.solver.types.PrimitiveBooleanToIntConverter
import android.arch.persistence.room.solver.types.PrimitiveColumnTypeAdapter
import android.arch.persistence.room.solver.types.StatementValueBinder
import android.arch.persistence.room.solver.types.StringColumnTypeAdapter
import android.arch.persistence.room.solver.types.TypeConverter
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.annotations.VisibleForTesting
import java.util.LinkedList;
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
/**
 * Holds all type adapters and can create on demand composite type adapters to convert a type into a
 * database column.
 */
class TypeAdapterStore private constructor(val context: Context,
                                           /**
                                            * first type adapter has the highest priority
                                            */
                                           private val columnTypeAdapters: List<ColumnTypeAdapter>,
                                           /**
                                            * first converter has the highest priority
                                            */
                                           private val typeConverters: List<TypeConverter>) {


    companion object {
        fun copy(context: Context, store: TypeAdapterStore): TypeAdapterStore {
            return TypeAdapterStore(context = context,
                    columnTypeAdapters = store.columnTypeAdapters,
                    typeConverters = store.typeConverters)
        }

        fun create(context: Context, @VisibleForTesting vararg extras: Any): TypeAdapterStore {
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
            FlowableQueryResultBinderProvider(context),
            RxMaybeQueryResultBinderProvider(context),
            RxSingleQueryResultBinderProvider(context),
            DataSourceQueryResultBinderProvider(context),
            LivePagedListQueryResultBinderProvider(context),
            InstantQueryResultBinderProvider(context)
    )

    // type mirrors that be converted into columns w/o an extra converter
    private val knownColumnTypeMirrors by lazy {
        columnTypeAdapters.map { it.out }
    }

    /**
     * Searches 1 way to bind a value into a statement.
     */
    fun findStatementValueBinder(input: TypeMirror, affinity: SQLTypeAffinity?)
            : StatementValueBinder? {
        if (input.kind == TypeKind.ERROR) {
            return null
        }
        val adapter = findDirectAdapterFor(input, affinity)
        if (adapter != null) {
            return adapter
        }
        val targetTypes = targetTypeMirrorsFor(affinity)
        val binder = findTypeConverter(input, targetTypes) ?: return null
        return CompositeAdapter(input, getAllColumnAdapters(binder.to).first(), binder, null)
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
    fun findColumnTypeAdapter(out: TypeMirror, affinity: SQLTypeAffinity?)
            : ColumnTypeAdapter? {
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

    private fun findDirectAdapterFor(out: TypeMirror, affinity: SQLTypeAffinity?)
            : ColumnTypeAdapter? {
        val adapter = getAllColumnAdapters(out).firstOrNull {
            affinity == null || it.typeAffinity == affinity
        }
        return adapter
    }

    fun findTypeConverter(input: TypeMirror, output: TypeMirror): TypeConverter? {
        return findTypeConverter(listOf(input), listOf(output))
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

    fun findQueryResultAdapter(typeMirror: TypeMirror, query: ParsedQuery)
            : QueryResultAdapter? {
        if (typeMirror.kind == TypeKind.ERROR) {
            return null
        }
        if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)
            if (declared.typeArguments.isEmpty()) {
                val rowAdapter = findRowAdapter(typeMirror, query) ?: return null
                return SingleEntityQueryResultAdapter(rowAdapter)
            }
            if (MoreTypes.isTypeOf(java.util.List::class.java, typeMirror)) {
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
    @VisibleForTesting
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

            val (rowAdapter, rowAdapterLogs) = if (resultInfo != null && query.errors.isEmpty()
                    && resultInfo.error == null) {
                // if result info is not null, first try a pojo row adapter
                context.collectLogs { subContext ->
                    val pojo = PojoProcessor(
                            baseContext = subContext,
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
                if (asElement.hasAnnotation(Entity::class)) {
                    return EntityRowAdapter(EntityProcessor(context,
                            MoreElements.asType(asElement)).process())
                }
            }

            if (rowAdapter != null && !(rowAdapterLogs?.hasErrors() ?: false)) {
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
            return null
        } else {
            val singleColumn = findCursorValueReader(typeMirror, null) ?: return null
            return SingleColumnRowAdapter(singleColumn)
        }
    }

    fun findQueryParameterAdapter(typeMirror: TypeMirror): QueryParameterAdapter? {
        if (MoreTypes.isType(typeMirror)
                && (MoreTypes.isTypeOf(java.util.List::class.java, typeMirror)
                || MoreTypes.isTypeOf(java.util.Set::class.java, typeMirror))) {
            val declared = MoreTypes.asDeclared(typeMirror)
            val binder = findStatementValueBinder(declared.typeArguments.first(),
                    null) ?: return null
            return CollectionQueryParameterAdapter(binder)
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

    private fun findTypeConverter(inputs: List<TypeMirror>, outputs: List<TypeMirror>)
            : TypeConverter? {
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
        fun exactMatch(candidates: List<TypeConverter>)
                : TypeConverter? {
            return candidates.firstOrNull {
                outputs.any { output -> types.isSameType(output, it.to) }
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

    private fun getAllTypeConverters(input: TypeMirror, excludes: List<TypeMirror>):
            List<TypeConverter> {
        val types = context.processingEnv.typeUtils
        return typeConverters.filter { converter ->
            types.isSameType(input, converter.from) &&
                    !excludes.any { types.isSameType(it, converter.to) }
        }
    }
}
