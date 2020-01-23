/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.tooling.preview

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Suffix added by the Kotlin compiler to a synthetic method generated for functions accepting
 * default parameters. The parameter with the $default suffix will take two additional
 * parameters, the second to last being a bitmask indicating which parameters contain default
 * values and the last one being an unused Object and hence set to null in this code.
 */
private const val DEFAULT_SUFFIX = "\$default"

/**
 * Find the given method by name. If the method has parameters, this function will try to find
 * the version that accepts default parameters.
 */
private fun Class<*>.findComposableMethod(methodName: String): Method {
    val method = try {
        // Simple case, no default parameters
        getDeclaredMethod(methodName)
    } catch (e: ReflectiveOperationException) {
        try {
            val defaultMethodName = "$methodName$DEFAULT_SUFFIX"
            declaredMethods.find { it.name == defaultMethodName }
        } catch (e: ReflectiveOperationException) {
            null
        }
    } ?: throw NoSuchMethodException("$name.$methodName")

    return method
}

/**
 * Returns the default value for the [Class] type. This will be 0 for numeric types, false for
 * boolean, '0' for char and null for object references.
 */
private fun Class<*>.getDefaultValue(): Any? = when (name) {
    "int" -> 0.toInt()
    "short" -> 0.toShort()
    "byte" -> 0.toByte()
    "long" -> 0.toLong()
    "double" -> 0.toDouble()
    "float" -> 0.toFloat()
    "boolean" -> false
    "char" -> '0'
    else -> null
}

/**
 * Returns true if the [Method] is the synthetic method generated for handling default parameters
 */
private fun Method.isDefaultMethod() = name.endsWith(DEFAULT_SUFFIX)

/**
 * Calls the method on the given [instance]. If the method accepts default values, this function
 * will call it with the correct options set.
 */
private fun Method.invokeComposableMethod(instance: Any?): Any? {
    if (parameterTypes.isEmpty()) {
        // Simple case, no default parameters, just call
        return invoke(instance)
    }

    assert(isDefaultMethod())
    val nParameters = parameterTypes.size
    val args = Array(nParameters) { idx ->
        when (idx) {
            // Default values mask, all parameters set to use defaults
            nParameters - 2 -> 0xFFFFFFFF.toInt()
            else -> parameterTypes[idx].getDefaultValue()
        }
    }

    return invoke(instance, *args)
}

/**
 * Invokes the given [methodName] belonging to the given [className] via reflection. The
 * [methodName] is expected to be a Composable function.
 */
internal fun invokeComposableViaReflection(className: String, methodName: String) {
    try {
        val composableClass = Class.forName(className)
        val method = composableClass.findComposableMethod(methodName)

        method.isAccessible = true

        if (Modifier.isStatic(method.modifiers)) {
            // This is a top level or static method
            method.invokeComposableMethod(null)
        } else {
            // The method is part of a class. We try to instantiate the class with an empty
            // constructor.
            val instance = composableClass.getConstructor().newInstance()
            method.invokeComposableMethod(instance)
        }
    } catch (e: ReflectiveOperationException) {
        throw ClassNotFoundException("Composable Method not found", e)
    }
}