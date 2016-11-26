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

import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.type.TypeMirror

/**
 * Holds all type adapters and can create on demand composite type adapters to convert a type into a
 * database column.
 */
class TypeAdapterStore(val roundEnv: RoundEnvironment,
                       val processingEnvironment: ProcessingEnvironment) {
    private val columnTypeAdapters = arrayListOf<ColumnTypeAdapter>()
    private val typeAdapters = arrayListOf<TypeConverter>()

    init {
        PrimitiveColumnTypeAdapter.createPrimitiveAdapters(processingEnvironment).forEach {
            addColumnAdapter(it)
        }
        addColumnAdapter(StringColumnTypeAdapter(processingEnvironment))
        addTypeAdapter(IntListConverter.create(processingEnvironment))
        addTypeAdapter(PrimitiveBooleanToIntConverter(processingEnvironment))
    }

    fun getTypeAdapters(input : TypeMirror, excludes : Set<TypeMirror>) : List<TypeConverter>? {
        // TODO optimize
        return if (findColumnAdapters(input).isNotEmpty()) {
            emptyList()
        } else {
            val candidate = findTypeAdapters(input)
                    .filterNot { excludes.contains(it.to) }
                    .map {
                        Pair(it, getTypeAdapters(it.to, excludes + it.from))
                    }
                    .filterNot { it.second == null }
                    .sortedBy { it.second!!.size }
                    .firstOrNull()
            return if (candidate == null) {
                null
            } else {
                listOf(candidate.first) + candidate.second!!
            }
        }
    }

    fun getAdapterFor(out : TypeMirror) : ColumnTypeAdapter? {
        val adapters = findColumnAdapters(out)
        if (adapters.isNotEmpty()) {
            return adapters.last()
        }
        val typeAdapters = getTypeAdapters(out, setOf(out))
        return if (typeAdapters == null) {
            null
        } else {
            return CompositeAdapter(out, findColumnAdapters(typeAdapters.last().to).last(),
                    typeAdapters)
        }
    }

    fun addTypeAdapter(converter: TypeConverter) {
        typeAdapters.add(converter)
        typeAdapters.add(ReverseTypeConverter(converter))
    }

    fun addColumnAdapter(adapter: ColumnTypeAdapter) {
        columnTypeAdapters.add(adapter)
    }

    fun findColumnAdapters(input : TypeMirror) : List<ColumnTypeAdapter> {
        return columnTypeAdapters.filter {
            processingEnvironment.typeUtils.isSameType(input, it.out)
        }
    }

    fun findTypeAdapters(input : TypeMirror) : List<TypeConverter> {
        return typeAdapters.filter {
            processingEnvironment.typeUtils.isSameType(input, it.from)
        }
    }
}
