package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

/**
 * Tests the generation of descriptions elements for app functions' parameters.
 */
@AppFunctionServiceEntryPoint("FakeFreeFormFunctionsService", "app_functions_v2")
abstract class FakeFreeFormFunctions : AppFunctionService() {
    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param intParam an input integer. Adding more text here to test multi-line description:
     * Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
     * labore et dolore magna aliqua.
     * @param stringListParam an input String list.
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_isDescribedByKDoc_true(
        intParam: Int,
        stringListParam: List<String>,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * This kdoc will not be used in any of the metadata descriptions since `isDescribedByKDoc` is
     * set to false.
     *
     * @param intParam an input integer.
     * @return a string.
     * @throws IllegalArgumentException if the inputString is in an incorrect format.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun fakeFreeFormFunction_isDescribedByKDoc_false(
        intParam: Int,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param invalidParam Invalid param. Should be ignored.
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_invalidParam_ignored(
        intParam: Int,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param paramWithEmptyDescription
     *
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_emptyDescription_ignored(
        paramWithEmptyDescription: Int,
    ): String {
        return "response"
    }

    /**
     * A fake free-form AppFunction to test descriptions in parameter metadata.
     *
     * @param intParam an input integer.
     * @fakeCustomTag Custom tag's content.
     * @return a string.
     * @throws IllegalArgumentException if the parameters have an incorrect format.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun fakeFreeFormFunction_onlyKnownTagsSkipped(
        intParam: Int,
    ): String {
        return "response"
    }
}
