/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.tooling

import androidx.compose.runtime.Composer
import androidx.compose.ui.ExperimentalComposeUiApi
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.ceil

/**
 * A utility object to invoke composable function by its name and containing class.
 */
@Deprecated("Use androidx.compose.runtime.reflect.ComposableMethodInvoker instead")
@ExperimentalComposeUiApi
object ComposableInvoker {

    /**
     *  Compares the parameter types taken from the composable method and checks if they are all compatible with
     *  the types taken from the PreviewParameterProvider.
     *
     *  @param composableMethodTypes types of the Composable Method
     *  @param previewParameterTypes types defined in the PreviewParameterProvider
     *  @return true if every `composableMethodTypes[n]` are equal or assignable to `previewParameterTypes[n]`.
     */
    private fun areParameterTypesCompatible(
        composableMethodTypes: Array<Class<*>>,
        previewParameterTypes: Array<Class<*>>
    ): Boolean = composableMethodTypes.size == previewParameterTypes.size &&
            composableMethodTypes.mapIndexed { index, clazz ->
                val composableParameterType = previewParameterTypes[index]
                // We can't use [isAssignableFrom] if we have java primitives.
                // Java primitives aren't equal to Java classes:
                // comparing int with kotlin.Int or java.lang.Integer will return false.
                // However, if we convert them both to a KClass they can be compared:
                // int and java.lang.Integer will be both converted to Int
                // see more: https://docs.oracle.com/javase/6/docs/api/java/lang/Class.html#isAssignableFrom(java.lang.Class)
                clazz.kotlin == composableParameterType.kotlin ||
                    clazz.isAssignableFrom(composableParameterType)
            }.all { it }

    /**
     * Takes the declared methods and accounts for compatible types so the signature does
     * not need to exactly match. This allows finding method calls that use subclasses as parameters
     * instead of the exact types.
     *
     * @return the compatible [Method] with the name [methodName]
     * @throws NoSuchMethodException if the method is not found
     */
    private fun Array<Method>.findCompatibleComposeMethod(
        methodName: String,
        vararg args: Class<*>
    ): Method = firstOrNull {
            (methodName == it.name || it.name.startsWith("$methodName-")) &&
                // Methods with inlined classes as parameter will have the name mangled
                // so we need to check for methodName-xxxx as well
                areParameterTypesCompatible(it.parameterTypes, arrayOf(*args))
        } ?: throw NoSuchMethodException("$methodName not found")

    private inline fun <reified T> T.dup(count: Int): Array<T> {
        return (0 until count).map { this }.toTypedArray()
    }

    /**
     * Find the given method by name. If the method has parameters, this function will try to find
     * the version that accepts default parameters.
     *
     * @return null if the composable method is not found. Returns the [Method] otherwise.
     */
    private fun Class<*>.findComposableMethod(
        methodName: String,
        vararg previewParamArgs: Any?
    ): Method? {
        val argsArray: Array<Class<out Any>> =
            previewParamArgs.mapNotNull { it?.javaClass }.toTypedArray()
        return try {
            // without defaults
            val changedParamsCount = changedParamCount(argsArray.size, 0)
            val changedParams = Int::class.java.dup(changedParamsCount)
            declaredMethods.findCompatibleComposeMethod(
                methodName,
                *argsArray,
                Composer::class.java, // composer param
                *changedParams // changed param
            )
        } catch (e: ReflectiveOperationException) {
            try {
                declaredMethods.find {
                    it.name == methodName ||
                        // Methods with inlined classes as parameter will have the name mangled
                        // so we need to check for methodName-xxxx as well
                        it.name.startsWith("$methodName-")
                }
            } catch (e: ReflectiveOperationException) {
                null
            }
        }
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
        "char" -> 0.toChar()
        else -> null
    }

    /**
     * Calls the method on the given [instance]. If the method accepts default values, this function
     * will call it with the correct options set.
     */
    @Suppress("BanUncheckedReflection")
    private fun Method.invokeComposableMethod(
        instance: Any?,
        composer: Composer,
        vararg args: Any?
    ): Any? {
        val composerIndex = parameterTypes.indexOfLast { it == Composer::class.java }
        val realParams = composerIndex
        val thisParams = if (instance != null) 1 else 0
        val changedParams = changedParamCount(realParams, thisParams)
        val totalParamsWithoutDefaults = realParams +
            1 + // composer
            changedParams
        val totalParams = parameterTypes.size
        val isDefault = totalParams != totalParamsWithoutDefaults
        val defaultParams = if (isDefault)
            defaultParamCount(realParams)
        else
            0

        check(
            realParams +
                1 + // composer
                changedParams +
                defaultParams ==
                totalParams
        ) { "params don't add up to total params" }

        val changedStartIndex = composerIndex + 1
        val defaultStartIndex = changedStartIndex + changedParams

        val arguments = Array(totalParams) { idx ->
            when (idx) {
                // pass in "empty" value for all real parameters since we will be using defaults.
                in 0 until realParams -> args.getOrElse(idx) {
                    parameterTypes[idx].getDefaultValue()
                }
                // the composer is the first synthetic parameter
                composerIndex -> composer
                // since this is the root we don't need to be anything unique. 0 should suffice.
                // changed parameters should be 0 to indicate "uncertain"
                in changedStartIndex until defaultStartIndex -> 0
                // Default values mask, all parameters set to use defaults
                in defaultStartIndex until totalParams -> 0b111111111111111111111.toInt()
                else -> error("Unexpected index")
            }
        }
        return invoke(instance, *arguments)
    }

    private const val SLOTS_PER_INT = 10
    private const val BITS_PER_INT = 31

    private fun changedParamCount(realValueParams: Int, thisParams: Int): Int {
        if (realValueParams == 0) return 1
        val totalParams = realValueParams + thisParams
        return ceil(
            totalParams.toDouble() / SLOTS_PER_INT.toDouble()
        ).toInt()
    }

    private fun defaultParamCount(realValueParams: Int): Int {
        return ceil(
            realValueParams.toDouble() / BITS_PER_INT.toDouble()
        ).toInt()
    }

    /**
     * Invokes the given [methodName] belonging to the given [className]. The [methodName] is
     * expected to be a Composable function.
     * This method [args] will be forwarded to the Composable function.
     */
    @ExperimentalComposeUiApi
    fun invokeComposable(
        className: String,
        methodName: String,
        composer: Composer,
        vararg args: Any?
    ) {
        try {
            val composableClass = Class.forName(className)
            val method = composableClass.findComposableMethod(methodName, *args)
                ?: throw NoSuchMethodException("Composable $className.$methodName not found")
            method.isAccessible = true

            if (Modifier.isStatic(method.modifiers)) {
                // This is a top level or static method
                method.invokeComposableMethod(null, composer, *args)
            } else {
                // The method is part of a class. We try to instantiate the class with an empty
                // constructor.
                val instance = composableClass.getConstructor().newInstance()
                method.invokeComposableMethod(instance, composer, *args)
            }
        } catch (e: Exception) {
            PreviewLogger.logWarning("Failed to invoke Composable Method '$className.$methodName'")
            throw e
        }
    }
}
