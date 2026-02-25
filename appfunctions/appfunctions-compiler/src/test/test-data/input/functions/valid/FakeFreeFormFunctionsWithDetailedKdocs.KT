package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction

/**
 * Tests the generation of descriptions elements for app functions and serializable classes.
 */
class FakeFreeFormFunctions {
    /**
     * A fake free-form AppFunction to test the description element in the function's generated
     * metadata xml.
     *
     * This kdoc consists of multiple paragraphs and is used as the function's description to the
     * agent.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param serializableParam an input AppFunctionSerializable.
     * @return The result string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_isDescribedByKDoc_true(
        appFunctionContext: AppFunctionContext,
        serializableParam: InputToFakeFreeFormFunction,
    ): String {
        return serializableParam.data
    }

    /**
     * A fake free-form AppFunction to test the description elements in the function generated
     * metadata xml. This version uses a derived serializable class.
     *
     * This kdoc consists of multiple paragraphs and is used as the function's description to the
     * agent.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param input an input AppFunctionSerializable.
     * @return The result string.
     * @throws IllegalArgumentException if the inputString is in an incorrect format.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_isDescribedByKDoc_withDerivedSerializable_true(
        appFunctionContext: AppFunctionContext,
        input: DerivedInputToFakeFreeFormFunction,
    ): String {
        return input.data
    }

    /**
     * A fake free-form AppFunction to test the description element in the function's generated
     * metadata xml.
     *
     * This kdoc will not be used as the description since `isDescribedByKDoc` is set to false.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param input an input AppFunctionSerializable.
     * @return The result string.
     * @throws IllegalArgumentException if the inputString is in an incorrect format.
     */
    @AppFunction(isDescribedByKDoc = false)
    fun fakeFreeFormFunction_isDescribedByKDoc_false(
        appFunctionContext: AppFunctionContext,
        input: InputToFakeFreeFormFunction,
    ): String {
        return input.data
    }

    /** Tests the description elements for serializable's parametrized properties. */
    @AppFunction(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_withParametrizedTypes(
        appFunctionContext: AppFunctionContext,
        parametrizedTypesInput: InputWithParametrizedTypes,
    ) {}

    /** Tests the description elements for serializable's repeated properties. */
    @AppFunction(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_withRepeatedTypes(
        appFunctionContext: AppFunctionContext,
        parametrizedTypesInput: InputWithRepeatedTypes,
    ) {}

    /** Tests that the input class description is empty when class's isDescribedByKDoc = false. */
    @AppFunction(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_inputClassDescribedByKdoc_false(
        appFunctionContext: AppFunctionContext,
        notDescribedByKdoc: NotDescribedByKdoc,
    ) {}
}

/** A test input class to the free-form function. */
@AppFunctionSerializable(isDescribedByKDoc = true)
open class InputToFakeFreeFormFunction(
    /** A string param. */
    open val data: String
)

/** A child class of [InputToFakeFreeFormFunction]. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class DerivedInputToFakeFreeFormFunction(
    /** A derived string param. */
    override val data: String,
    /** An int param. */
    val anotherData: Int,
) : InputToFakeFreeFormFunction(data)

/** An input class with parametrized types */
@AppFunctionSerializable(isDescribedByKDoc = true)
class InputWithParametrizedTypes(
    /** Parametrized primitive param. */
    val parametrizedInt: Optional<Int>,
    /** Parametrized nullable primitive param. */
    val parametrizedNullableInt: Optional<Int?>,
    /** Parametrized entity param. */
    val parametrizedSerializable: Optional<InputToFakeFreeFormFunction>,
    /** Parametrized nullable entity param. */
    val parametrizedNullableSerializable: Optional<InputToFakeFreeFormFunction?>,
)

/** An input class with repeated params. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class InputWithRepeatedTypes(
    /** Primitive array param. */
    val intArray: IntArray,
    /** String list param. */
    val stringList: List<String>,
    /** Serializable list param. */
    val serializableList: List<InputToFakeFreeFormFunction>,
)

/** A generic serializable class. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Optional<T>(
    /** The value. */
    val value: T
)

/** Class description. Should be ignored. */
@AppFunctionSerializable(isDescribedByKDoc = false)
class NotDescribedByKdoc(
    /** Property description. Should be ignored. */
    val param: Int
)
