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

package com.android.support.room.solver

import com.android.support.room.Entity
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.processor.Context
import com.android.support.room.solver.query.parameter.ArrayQueryParameterAdapter
import com.android.support.room.solver.query.parameter.BasicQueryParameterAdapter
import com.android.support.room.solver.query.parameter.CollectionQueryParameterAdapter
import com.android.support.room.solver.query.parameter.QueryParameterAdapter
import com.android.support.room.solver.query.result.ArrayQueryResultAdapter
import com.android.support.room.solver.query.result.ListQueryResultAdapter
import com.android.support.room.solver.query.result.EntityRowAdapter
import com.android.support.room.solver.query.result.QueryResultAdapter
import com.android.support.room.solver.query.result.RowAdapter
import com.android.support.room.solver.query.result.SingleColumnRowAdapter
import com.android.support.room.solver.query.result.SingleEntityQueryResultAdapter
import com.android.support.room.solver.types.BoxedBooleanToBoxedIntConverter
import com.android.support.room.solver.types.BoxedPrimitiveColumnTypeAdapter
import com.android.support.room.solver.types.BoxedPrimitiveToStringConverter
import com.android.support.room.solver.types.ColumnTypeAdapter
import com.android.support.room.solver.types.CompositeAdapter
import com.android.support.room.solver.types.CompositeTypeConverter
import com.android.support.room.solver.types.IntListConverter
import com.android.support.room.solver.types.NoOpConverter
import com.android.support.room.solver.types.PrimitiveBooleanToIntConverter
import com.android.support.room.solver.types.PrimitiveColumnTypeAdapter
import com.android.support.room.solver.types.PrimitiveToStringConverter
import com.android.support.room.solver.types.ReverseTypeConverter
import com.android.support.room.solver.types.StringColumnTypeAdapter
import com.android.support.room.solver.types.TypeConverter
import com.google.auto.common.MoreTypes
import com.google.common.annotations.VisibleForTesting
import java.util.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
/**
 * Holds all type adapters and can create on demand composite type adapters to convert a type into a
 * database column.
 */
class TypeAdapterStore(val context : Context,
                       @VisibleForTesting vararg extras : Any) {
    private val columnTypeAdapters : List<ColumnTypeAdapter>
    private val typeConverters : List<TypeConverter>

    init {
        val adapters = arrayListOf<ColumnTypeAdapter>()
        val converters = arrayListOf<TypeConverter>()
        extras.forEach {
            when(it) {
                is TypeConverter -> converters.add(it)
                is ColumnTypeAdapter -> adapters.add(it)
                else -> throw IllegalArgumentException("unknown extra")
            }
        }
        fun addTypeConverter(converter: TypeConverter) {
            converters.add(converter)
            converters.add(ReverseTypeConverter(converter))
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
        addTypeConverter(IntListConverter.create(context.processingEnv))
        addTypeConverter(PrimitiveBooleanToIntConverter(context.processingEnv))
        PrimitiveToStringConverter
                .createPrimitives(context)
                .forEach(::addTypeConverter)
        BoxedPrimitiveToStringConverter
                .createBoxedPrimitives(context)
                .forEach(::addTypeConverter)
        addTypeConverter(BoxedBooleanToBoxedIntConverter(context.processingEnv))
        columnTypeAdapters = adapters
        typeConverters = converters
    }

    // type mirrors that be converted into columns w/o an extra converter
    private val knownColumnTypeMirrors by lazy {
        columnTypeAdapters.map { it.out }
    }

    fun findColumnTypeAdapter(out: TypeMirror): ColumnTypeAdapter? {
        val adapters = getAllColumnAdapters(out)
        if (adapters.isNotEmpty()) {
            return adapters.last()
        }
        val converter = findTypeConverter(out, knownColumnTypeMirrors)
        if (converter != null) {
            return CompositeAdapter(out, getAllColumnAdapters(converter.to).first(), converter)
        }
        return null
    }

    fun findTypeConverter(input: TypeMirror, output: TypeMirror): TypeConverter? {
        return findTypeConverter(input, listOf(output))
    }

    fun findQueryResultAdapter(typeMirror: TypeMirror) : QueryResultAdapter? {
        if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)
            if (declared.typeArguments.isEmpty()) {
                val rowAdapter = findRowAdapter(typeMirror) ?: return null
                return SingleEntityQueryResultAdapter(rowAdapter)
            }
            // TODO make this flexible so that things like LiveData, Rx can work
            if (MoreTypes.isTypeOf(java.util.List::class.java, typeMirror)) {
                val typeArg = declared.typeArguments.first()
                val rowAdapter = findRowAdapter(typeArg) ?: return null
                return ListQueryResultAdapter(rowAdapter)
            }
            return null
        } else if (typeMirror.kind == TypeKind.ARRAY) {
            val array = MoreTypes.asArray(typeMirror)
            val rowAdapter = findRowAdapter(array.componentType) ?: return null
            return ArrayQueryResultAdapter(rowAdapter)
        } else {
            val rowAdapter = findRowAdapter(typeMirror) ?: return null
            return SingleEntityQueryResultAdapter(rowAdapter)
        }
    }

    private fun findRowAdapter(typeMirror: TypeMirror) : RowAdapter? {
        if (typeMirror.kind == TypeKind.DECLARED) {
            val declared = MoreTypes.asDeclared(typeMirror)
            if (declared.typeArguments.isNotEmpty()) {
                // TODO one day support this
                return null
            }
            val asElement = MoreTypes.asElement(typeMirror)
            if (asElement.hasAnnotation(Entity::class)) {
                return EntityRowAdapter(typeMirror)
            }
            val singleColumn = findColumnTypeAdapter(typeMirror)

            if (singleColumn != null) {
                return SingleColumnRowAdapter(singleColumn)
            }

            // TODO we can allow any class actually but need a proper API for that to avoid false
            // positives
            return null
        } else {
            val singleColumn = findColumnTypeAdapter(typeMirror) ?: return null
            return SingleColumnRowAdapter(singleColumn)
        }
    }

    fun findQueryParameterAdapter(typeMirror : TypeMirror) : QueryParameterAdapter? {
        if (MoreTypes.isType(typeMirror)
                && (MoreTypes.isTypeOf(java.util.List::class.java, typeMirror)
                        || MoreTypes.isTypeOf(java.util.Set::class.java, typeMirror))) {
            val declared = MoreTypes.asDeclared(typeMirror)
            val converter = findTypeConverter(declared.typeArguments.first(),
                    context.COMMON_TYPES.STRING)
                    ?: return null
            return CollectionQueryParameterAdapter(converter)
        } else if (typeMirror is ArrayType) {
            val component = typeMirror.componentType
            val converter = findTypeConverter(component, context.COMMON_TYPES.STRING)
                    ?: return null
            return ArrayQueryParameterAdapter(converter)
        } else {
            val converter = findTypeConverter(typeMirror, context.COMMON_TYPES.STRING)
                    ?: return null
            return BasicQueryParameterAdapter(converter)
        }
    }

    private fun findTypeConverter(input: TypeMirror, outputs: List<TypeMirror>): TypeConverter? {
        val types = context.processingEnv.typeUtils
        // if same type, return no-op
        if (outputs.any { types.isSameType(input, it) }) {
            return NoOpConverter(input)
        }
        val excludes = arrayListOf<TypeMirror>()
        excludes.add(input)
        val queue = LinkedList<TypeConverter>()
        do {
            val prev = if (queue.isEmpty()) null else queue.pop()
            val from = prev?.to ?: input
            val candidates = getAllTypeConverters(from, excludes)
            val match = candidates.firstOrNull {
                outputs.any { output -> types.isSameType(output, it.to) } }
            if (match != null) {
                return if (prev == null) match else CompositeTypeConverter(prev, match)
            }
            candidates.forEach {
                excludes.add(it.to)
                queue.add(
                        if (prev == null) it else CompositeTypeConverter(prev, it)
                )
            }
        } while (queue.isNotEmpty())
        return null
    }

    private fun getAllColumnAdapters(input: TypeMirror): List<ColumnTypeAdapter> {
        return columnTypeAdapters.filter {
            context.processingEnv.typeUtils.isSameType(input, it.out)
        }
    }

    private fun getAllTypeConverters(input: TypeMirror, excludes : List<TypeMirror>):
            List<TypeConverter> {
        val types = context.processingEnv.typeUtils
        return typeConverters.filter { converter ->
            types.isSameType(input, converter.from) &&
                    !excludes.any { types.isSameType(it, converter.to) }
        }
    }
}
