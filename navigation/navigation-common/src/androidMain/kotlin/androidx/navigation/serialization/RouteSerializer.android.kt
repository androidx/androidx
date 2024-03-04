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

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.serialization.UNKNOWN.isPrimitive
import kotlin.reflect.KType
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
    assertNotAbstractClass {
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
        val type = descriptor.getElementDescriptor(i).getNavType()
        /**
         * Path args if all conditions met:
         * 1. is primitive - arrays need repeated arg names & custom classes need to be parsed to
         * json string
         * 2. not optional (has no default value) - from perspective of DeepLinking, args with
         * default values are not a core arg
         */
        if (type.isPrimitive() && !descriptor.isElementOptional(i)) {
            pathArg += "/{$argName}"
        } else {
            val symbol = if (queryArg.isEmpty()) "?" else "&"
            queryArg += "$symbol$argName={$argName}"
        }
    }

    return path + pathArg + queryArg
}

/**
 * Returns a list of [NamedNavArgument].
 *
 * By default this method only supports conversion to NavTypes that are declared in
 * [NavType.Companion] class. To convert non-natively supported types, the custom NavType must be
 * provided via [typeMap].
 *
 * Short summary of NavArgument generation principles:
 * 1. NavArguments will only be generated on variables with kotlin backing fields
 * 2. Arg Name is based on variable name
 * 3. Nullability is based on variable Type's nullability
 * 4. defaultValuePresent is based on whether variable has default value
 *
 * This generator does not check for validity as a NavType.
 * This means if a NavType is not nullable (i.e. Int), and the KType was Int?, it relies on the
 * navArgument builder to throw exception.
 *
 * @param [typeMap] A mapping of KType to the custom NavType<*>. For example given
 * an argument of "val userId: UserId", the map should
 * contain [typeOf<UserId>() to MyNavType]. Custom NavTypes take priority over native
 * NavTypes. This means you can override native NavTypes such as [NavType.IntType] with your own
 * implementation of NavType<Int>.
 */
internal fun <T> KSerializer<T>.generateNavArguments(
    typeMap: Map<KType, NavType<*>>? = null
): List<NamedNavArgument> {
    assertNotAbstractClass {
        throw IllegalArgumentException(
            "Cannot generate NavArguments for polymorphic serializer $this. Arguments " +
                "can only be generated from concrete classes or objects."
        )
    }

    return List(descriptor.elementsCount) { index ->
        val name = descriptor.getElementName(index)
        navArgument(name) {
            val element = descriptor.getElementDescriptor(index)
            val isNullable = element.isNullable
            val customType = typeMap?.keys
                ?.find { kType -> element.matchKType(kType) }
                ?.let { typeMap[it] }
            type = customType ?: element.getNavType()
            if (type == UNKNOWN) {
                throw IllegalArgumentException(
                    "Cannot cast $name of type ${element.serialName} to a NavType. Make sure " +
                        "to provide custom NavType for this argument."
                )
            }
            nullable = isNullable
            if (descriptor.isElementOptional(index)) {
                // Navigation mostly just cares about defaultValuePresent state for
                // non-nullable args to verify DeepLinks at a later stage.
                // We know that non-nullable types cannot have null values, so it is
                // safe to mark this as true without knowing actual value.
                unknownDefaultValuePresent = true
            }
        }
    }
}

private fun <T> KSerializer<T>.assertNotAbstractClass(handler: () -> Unit) {
    // abstract class
    if (this is PolymorphicSerializer) {
        handler()
    }
}
