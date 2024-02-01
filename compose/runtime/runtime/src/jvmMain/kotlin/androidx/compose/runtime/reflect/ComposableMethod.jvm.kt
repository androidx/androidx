/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("ComposableMethodKt")

package androidx.compose.runtime.reflect

import androidx.compose.runtime.Composer
import androidx.compose.runtime.internal.SLOTS_PER_INT
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import kotlin.math.ceil

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
 * Structure intended to be used exclusively by [getComposableInfo].
 */
internal data class ComposableInfo(
    val isComposable: Boolean,
    val realParamsCount: Int,
    val changedParams: Int,
    val defaultParams: Int
)

/**
 * Checks whether the method is Composable function and returns result along with the real
 * parameters count and changed parameter count (if composable) and packed in a structure.
 */
private fun Method.getComposableInfo(): ComposableInfo {
    val realParamsCount = parameterTypes.indexOfLast { it == Composer::class.java }
    if (realParamsCount == -1) {
        return ComposableInfo(false, parameterTypes.size, 0, 0)
    }
    val thisParams = if (Modifier.isStatic(this.modifiers)) 0 else 1
    val changedParams = changedParamCount(realParamsCount, thisParams)
    val totalParamsWithoutDefaults = realParamsCount +
        1 + // composer
        changedParams
    val totalParams = parameterTypes.size
    val isDefault = totalParams != totalParamsWithoutDefaults
    val defaultParams = if (isDefault)
        defaultParamCount(realParamsCount)
    else
        0
    return ComposableInfo(
        totalParamsWithoutDefaults + defaultParams == totalParams,
        realParamsCount,
        changedParams,
        defaultParams
    )
}

/**
 * Returns the default value for the [Class] type. This will be 0 for numeric types, false for
 * boolean and null for object references.
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
 * Represents the @Composable method.
 */
class ComposableMethod internal constructor(
    private val method: Method,
    private val composableInfo: ComposableInfo
    ) {
    /**
     * Returns the backing [Method].
     */
    fun asMethod() = method

    /**
     * Returns the count of method parameters excluding the utility Compose-specific parameters.
     */
    val parameterCount
        get() = composableInfo.realParamsCount

    /**
     * Returns method parameters excluding the utility Compose-specific parameters.
     */
    val parameters: Array<Parameter>
        @Suppress("ClassVerificationFailure", "NewApi")
        get() = method.parameters.copyOfRange(0, composableInfo.realParamsCount)

    /**
     * Returns method parameters types excluding the utility Compose-specific parameters.
     */
    val parameterTypes: Array<Class<*>>
        get() = method.parameterTypes.copyOfRange(0, composableInfo.realParamsCount)

    /**
     * Calls the Composable method on the given [instance]. If the method accepts default values,
     * this function will call it with the correct options set.
     */
    @Suppress("BanUncheckedReflection", "ListIterator")
    operator fun invoke(composer: Composer, instance: Any?, vararg args: Any?): Any? {
        val (_, realParamsCount, changedParams, defaultParams) = composableInfo

        val totalParams = method.parameterTypes.size
        val changedStartIndex = realParamsCount + 1
        val defaultStartIndex = changedStartIndex + changedParams

        val defaultsMasks = Array(defaultParams) { index ->
            val start = index * BITS_PER_INT
            val end = minOf(start + BITS_PER_INT, realParamsCount)
            val useDefault =
                (start until end).map { if (it >= args.size || args[it] == null) 1 else 0 }
            val mask = useDefault.foldIndexed(0) { i, mask, default -> mask or (default shl i) }
            mask
        }

        val arguments = Array(totalParams) { idx ->
            when (idx) {
                // pass in "empty" value for all real parameters since we will be using defaults.
                in 0 until realParamsCount -> args.getOrElse(idx) {
                    method.parameterTypes[idx].getDefaultValue()
                }
                // the composer is the first synthetic parameter
                realParamsCount -> composer
                // since this is the root we don't need to be anything unique. 0 should suffice.
                // changed parameters should be 0 to indicate "uncertain"
                changedStartIndex -> 0
                in changedStartIndex + 1 until defaultStartIndex -> 0
                // Default values mask, all parameters set to use defaults
                in defaultStartIndex until totalParams -> defaultsMasks[idx - defaultStartIndex]
                else -> error("Unexpected index")
            }
        }
        return method.invoke(instance, *arguments)
    }

    override fun equals(other: Any?) = when (other) {
        is ComposableMethod -> method == other.method
        else -> false
    }

    override fun hashCode() = method.hashCode()
}

fun Method.asComposableMethod(): ComposableMethod? {
    val composableInfo = getComposableInfo()
    if (composableInfo.isComposable) {
        return ComposableMethod(this, composableInfo)
    }
    return null
}

private inline fun <reified T> T.dup(count: Int): Array<T> {
    return (0 until count).map { this }.toTypedArray()
}

/**
 * Find the given @Composable method by name.
 */
@Throws(NoSuchMethodException::class)
fun Class<*>.getDeclaredComposableMethod(methodName: String, vararg args: Class<*>):
    ComposableMethod {
    val changedParams = changedParamCount(args.size, 0)
    val method = try {
        // without defaults
        getDeclaredMethod(
            methodName,
            *args,
            Composer::class.java, // composer param
            *Int::class.java.dup(changedParams) // changed params
        )
    } catch (e: ReflectiveOperationException) {
        val defaultParams = defaultParamCount(args.size)
        try {
            getDeclaredMethod(
                methodName,
                *args,
                Composer::class.java, // composer param
                *Int::class.java.dup(changedParams), // changed param
                *Int::class.java.dup(defaultParams) // default param
            )
        } catch (e2: ReflectiveOperationException) {
            null
        }
    } ?: throw NoSuchMethodException("$name.$methodName")

    return method.asComposableMethod()!!
}
