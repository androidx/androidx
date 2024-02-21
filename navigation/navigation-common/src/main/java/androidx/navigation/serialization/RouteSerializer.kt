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

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package androidx.navigation.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.capturedKClass

/**
 * Generates a route pattern for use in Navigation functions such as [::navigate] from
 * a serializer of class T where T is a concrete class or object.
 *
 * The generated route pattern contains the path, path args, and query args. Non-nullable arg types
 * are appended as path args, while nullable arg types are appended as query args.
 */
internal fun <T> KSerializer<T>.generateRoutePattern(): String {
    // abstract class
    if (this is PolymorphicSerializer) {
        throw IllegalArgumentException(
            "Cannot generate route pattern from polymorphic class " +
                "${descriptor.capturedKClass?.simpleName}. Routes can only be generated from " +
                "concrete classes or objects."
        )
    }

    val path = descriptor.serialName

    var pathArg = ""
    var queryArg = ""

    // TODO refactor to use RouteBuilder when implementing route with args to ensure
    // same logic for both route generation
    for (i in 0 until descriptor.elementsCount) {
        val argName = descriptor.getElementName(i)
        // If it has default value, from the perspective of DeepLinks this arg is not
        // a core arg and so we append it as a query
        if (descriptor.isElementOptional(i)) {
            val symbol = if (queryArg.isEmpty()) "?" else "&"
            queryArg += "$symbol$argName={$argName}"
        } else {
            pathArg += "/{$argName}"
        }
    }

    return path + pathArg + queryArg
}
