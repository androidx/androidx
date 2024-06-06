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

    fun appendArg(index: Int, name: String, type: NavType<Any?>, value: List<String>) {
        val paramType = computeParamType(index, type)
        when (paramType) {
            ParamType.PATH -> {
                // path arguments should be a single string value of primitive types
                require(value.size == 1) {
                    "Expected one value for argument $name, found ${value.size}" + "values instead."
                }
                addPath(value.first())
            }
            ParamType.QUERY -> value.forEach { addQuery(name, it) }
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
