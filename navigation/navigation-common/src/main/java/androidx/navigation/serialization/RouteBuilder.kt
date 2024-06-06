/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalSerializationApi::class)

package androidx.navigation.serialization

import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer

// TODO("Delete entire class when RouteFilled has also migrated to the new RouteBuilder")
/** Builds navigation routes from a destination class or instance. */
internal sealed class OldRouteBuilder<T> private constructor() {
    /**
     * Builds a route filled with argument values
     *
     * @param serializer The serializer for destination instance that you need to build the route
     *   for.
     * @param typeMap A map of argument name to the NavArgument of all serializable fields in this
     *   destination instance
     */
    class Filled<T>(serializer: KSerializer<T>, private val typeMap: Map<String, NavType<Any?>>) :
        OldRouteBuilder<T>() {

        private val builder = Builder(serializer, typeMap)
        private var elementIndex = -1

        /** Set index of the argument that is currently getting encoded */
        fun setElementIndex(idx: Int) {
            elementIndex = idx
        }

        /** Adds argument value to the url */
        fun addArg(value: Any?) {
            builder.apply(elementIndex) { name, type, paramType ->
                val parsedValue =
                    if (type is CollectionNavType) {
                        type.serializeAsValues(value)
                    } else {
                        listOf(type.serializeAsValue(value))
                    }
                when (paramType) {
                    ParamType.PATH -> {
                        // path arguments should be a single string value of primitive types
                        require(parsedValue.size == 1) {
                            "Expected one value for argument $name, found ${parsedValue.size}" +
                                "values instead."
                        }
                        addPath(parsedValue.first())
                    }
                    ParamType.QUERY -> parsedValue.forEach { addQuery(name, it) }
                }
            }
        }

        fun build(): String = builder.build()
    }

    enum class ParamType {
        PATH,
        QUERY
    }

    /** Internal builder that generates the final url output */
    private class Builder<T> {
        private val serializer: KSerializer<T>
        private val typeMap: Map<String, NavType<Any?>>
        private val path: String
        private var pathArgs = ""
        private var queryArgs = ""

        constructor(serializer: KSerializer<T>, typeMap: Map<String, NavType<Any?>>) {
            this.serializer = serializer
            this.typeMap = typeMap
            path = serializer.descriptor.serialName
        }

        constructor(path: String, serializer: KSerializer<T>, typeMap: Map<String, NavType<Any?>>) {
            this.serializer = serializer
            this.typeMap = typeMap
            this.path = path
        }

        /** Returns final route */
        fun build() = path + pathArgs + queryArgs

        /** Append string to the route's (url) path */
        fun addPath(path: String) {
            pathArgs += "/$path"
        }

        /** Append string to the route's (url) query parameter */
        fun addQuery(name: String, value: String) {
            val symbol = if (queryArgs.isEmpty()) "?" else "&"
            queryArgs += "$symbol$name=$value"
        }

        fun apply(
            index: Int,
            block: Builder<T>.(name: String, type: NavType<Any?>, paramType: ParamType) -> Unit
        ) {
            val descriptor = serializer.descriptor
            val elementName = descriptor.getElementName(index)
            val type = typeMap[elementName]
            checkNotNull(type) {
                "Cannot find NavType for argument $elementName. Please provide NavType through" +
                    "typeMap."
            }
            val paramType = computeParamType(index, type)
            this.block(elementName, type, paramType)
        }

        /**
         * Given the descriptor of [T], computes the [ParamType] of the element (argument) at
         * [index].
         *
         * Query args if either conditions met:
         * 1. has default value
         * 2. is of [CollectionNavType]
         */
        private fun computeParamType(index: Int, type: NavType<Any?>) =
            if (type is CollectionNavType || serializer.descriptor.isElementOptional(index)) {
                ParamType.QUERY
            } else {
                ParamType.PATH
            }
    }
}

/** Builds navigation routes from a destination class or instance. */
internal class RouteBuilder<T> {
    private val serializer: KSerializer<T>
    private val path: String
    private var pathArgs = ""
    private var queryArgs = ""

    /**
     * Create a builder that builds a route URL
     *
     * @param serializer The serializer for destination type T (class, object etc.) to build the
     *   route for.
     */
    constructor(serializer: KSerializer<T>) {
        this.serializer = serializer
        path = serializer.descriptor.serialName
    }

    /**
     * Create a builder that builds a route URL
     *
     * @param path The base uri path to which arguments are appended
     * @param serializer The serializer for destination type T (class, object etc.) to build the
     *   route for.
     */
    constructor(path: String, serializer: KSerializer<T>) {
        this.serializer = serializer
        this.path = path
    }

    /** Returns final route */
    fun build() = path + pathArgs + queryArgs

    /** Append string to the route's (url) path */
    private fun addPath(path: String) {
        pathArgs += "/$path"
    }

    /** Append string to the route's (url) query parameter */
    private fun addQuery(name: String, value: String) {
        val symbol = if (queryArgs.isEmpty()) "?" else "&"
        queryArgs += "$symbol$name=$value"
    }

    fun appendPattern(index: Int, name: String, type: NavType<Any?>) {
        val paramType = computeParamType(index, type)
        when (paramType) {
            ParamType.PATH -> addPath("{$name}")
            ParamType.QUERY -> addQuery(name, "{$name}")
        }
    }

    /**
     * Given the descriptor of [T], computes the [ParamType] of the element (argument) at [index].
     *
     * Query args if either conditions met:
     * 1. has default value
     * 2. is of [CollectionNavType]
     */
    private fun computeParamType(index: Int, type: NavType<Any?>) =
        if (type is CollectionNavType || serializer.descriptor.isElementOptional(index)) {
            ParamType.QUERY
        } else {
            ParamType.PATH
        }

    private enum class ParamType {
        PATH,
        QUERY
    }
}
