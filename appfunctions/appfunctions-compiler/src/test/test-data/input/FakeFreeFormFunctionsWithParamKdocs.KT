package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction

/**
 * Tests the generation of descriptions elements for app functions' parameters.
 */
class FakeFreeFormFunctions {
    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param intParam an input integer. Adding more text here to test multi-line description:
     * Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
     * labore et dolore magna aliqua.
     * @param stringListParam an input String list.
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunction(isDescribedByKdoc = true)
    fun fakeFreeFormFunction_isDescribedByKdoc_true(
        appFunctionContext: AppFunctionContext,
        intParam: Int,
        stringListParam: List<String>,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * This kdoc will not be used in any of the metadata descriptions since `isDescribedByKdoc` is
     * set to false.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param intParam an input integer.
     * @return a string.
     * @throws IllegalArgumentException if the inputString is in an incorrect format.
     */
    @AppFunction(isDescribedByKdoc = false)
    fun fakeFreeFormFunction_isDescribedByKdoc_false(
        appFunctionContext: AppFunctionContext,
        intParam: Int,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param invalidParam Invalid param. Should be ignored.
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunction(isDescribedByKdoc = true)
    fun fakeFreeFormFunction_invalidParam_ignored(
        appFunctionContext: AppFunctionContext,
        intParam: Int,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param paramWithEmptyDescription
     *
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunction(isDescribedByKdoc = true)
    fun fakeFreeFormFunction_emptyDescription_ignored(
        appFunctionContext: AppFunctionContext,
        paramWithEmptyDescription: Int,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param intParam an input integer.
     * @fakeCustomTag Custom tag's content.
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunction(isDescribedByKdoc = true)
    fun fakeFreeFormFunction_onlyKnownTagsSkipped(
        appFunctionContext: AppFunctionContext,
        intParam: Int,
    ): String {
        return "response"
    }
}
