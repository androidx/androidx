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

import androidx.annotation.RestrictTo
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.serializer

/**
 * Generates a route pattern for use in Navigation functions such as [::navigate] from a serializer
 * of class T where T is a concrete class or object.
 *
 * The generated route pattern contains the path, path args, and query args. See
 * [RouteBuilder.computeParamType] for logic on how parameter type (path or query) is computed.
 *
 * @param [typeMap] A mapping of KType to the custom NavType<*>. For example given an argument of
 *   "val userId: UserId", the map should contain [typeOf<UserId>() to MyNavType].
 * @param [path] The base path to append arguments to. If null, base path defaults to
 *   [KSerializer.descriptor].serialName.
 */
internal fun <T> KSerializer<T>.generateRoutePattern(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    path: String? = null,
): String {
    assertNotAbstractClass {
        throw IllegalArgumentException(
            "Cannot generate route pattern from polymorphic class " +
                "${descriptor.capturedKClass?.simpleName}. Routes can only be generated from " +
                "concrete classes or objects."
        )
    }
    val builder =
        if (path != null) {
            RouteBuilder(path, this)
        } else {
            RouteBuilder(this)
        }
    forEachIndexed(typeMap) { index, argName, navType ->
        builder.appendPattern(index, argName, navType)
    }
    return builder.build()
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
 * This generator does not check for validity as a NavType. This means if a NavType is not nullable
 * (i.e. Int), and the KType was Int?, it relies on the navArgument builder to throw exception.
 *
 * @param [typeMap] A mapping of KType to the custom NavType<*>. For example given an argument of
 *   "val userId: UserId", the map should contain [typeOf<UserId>() to MyNavType]. Custom NavTypes
 *   take priority over native NavTypes. This means you can override native NavTypes such as
 *   [NavType.IntType] with your own implementation of NavType<Int>.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> KSerializer<T>.generateNavArguments(
    typeMap: Map<KType, NavType<*>> = emptyMap()
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
            type =
                element.computeNavType(typeMap)
                    ?: throw IllegalArgumentException(
                        unknownNavTypeErrorMessage(
                            name,
                            element.serialName,
                            this@generateNavArguments.descriptor.serialName,
                            typeMap.toString()
                        )
                    )
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

/**
 * Generates a route filled in with argument value for use in Navigation functions such as
 * [::navigate] from a destination instance of type T.
 *
 * The generated route pattern contains the path, path args, and query args. See
 * [RouteBuilder.computeParamType] for logic on how parameter type (path or query) is computed.
 */
@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : Any> generateRouteWithArgs(route: T, typeMap: Map<String, NavType<Any?>>): String {
    val serializer = route::class.serializer()
    val argMap: Map<String, List<String>> = RouteEncoder(serializer, typeMap).encodeToArgMap(route)
    val builder = RouteBuilder(serializer)
    serializer.forEachIndexed(typeMap) { index, argName, navType ->
        val value = argMap[argName]!!
        builder.appendArg(index, argName, navType, value)
    }
    return builder.build()
}

private fun <T> KSerializer<T>.assertNotAbstractClass(handler: () -> Unit) {
    // abstract class
    if (this is PolymorphicSerializer) {
        handler()
    }
}

/**
 * Computes and return the [NavType] based on the SerialDescriptor of a class type.
 *
 * Match priority:
 * 1. Match with custom NavType provided in [typeMap]
 * 2. Match to a built-in NavType such as [NavType.IntType], [NavType.BoolArrayType] etc.
 */
@Suppress("UNCHECKED_CAST")
private fun SerialDescriptor.computeNavType(typeMap: Map<KType, NavType<*>>): NavType<Any?>? {
    val customType =
        typeMap.keys.find { kType -> matchKType(kType) }?.let { typeMap[it] } as? NavType<Any?>
    val result = customType ?: getNavType()
    return if (result == UNKNOWN) null else result as NavType<Any?>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> KSerializer<T>.generateHashCode(): Int {
    var hash = descriptor.serialName.hashCode()
    for (i in 0 until descriptor.elementsCount) {
        hash = 31 * hash + descriptor.getElementName(i).hashCode()
    }
    return hash
}

@JvmName("forEachIndexedKType")
private fun <T> KSerializer<T>.forEachIndexed(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    operation: (index: Int, argName: String, navType: NavType<Any?>) -> Unit
) {
    for (i in 0 until descriptor.elementsCount) {
        val argName = descriptor.getElementName(i)

        val navType =
            descriptor.getElementDescriptor(i).computeNavType(typeMap)
                ?: throw IllegalArgumentException(
                    unknownNavTypeErrorMessage(
                        argName,
                        descriptor.getElementDescriptor(i).serialName,
                        descriptor.serialName,
                        typeMap.toString()
                    )
                )
        operation(i, argName, navType)
    }
}

@JvmName("forEachIndexedName")
private fun <T> KSerializer<T>.forEachIndexed(
    typeMap: Map<String, NavType<Any?>>,
    operation: (index: Int, argName: String, navType: NavType<Any?>) -> Unit
) {
    for (i in 0 until descriptor.elementsCount) {
        val argName = descriptor.getElementName(i)
        val navType = typeMap[argName]
        checkNotNull(navType) { "Cannot locate NavType for argument [$argName]" }
        operation(i, argName, navType)
    }
}

private fun unknownNavTypeErrorMessage(
    fieldName: String,
    fieldType: String,
    className: String,
    typeMap: String
) =
    "Route $className could not find any NavType for argument $fieldName " +
        "of type $fieldType - typeMap received was $typeMap"
