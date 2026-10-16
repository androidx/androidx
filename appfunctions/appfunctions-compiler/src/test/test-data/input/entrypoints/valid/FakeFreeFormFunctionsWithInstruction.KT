package com.testdata

import androidx.appfunctions.AppFunctionInstruction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FakeFreeFormFunctionsWithInstructionService", "app_functions_v2")
abstract class FakeFreeFormFunctionsWithInstruction : AppFunctionService() {
    /**
     * This is a cool function but we want to override it.
     *
     * @param arg1 The first argument but we want to override it.
     * @param arg2 The second argument which shouldn't be overridden.
     * @return The return value but we want to override it.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    @AppFunctionInstruction("Overridden function description")
    fun myInstructionFunction(
        @AppFunctionInstruction("Overridden param description") arg1: Int,
        arg2: Int,
        arg3: SerializableWithKDocAndInstruction,
        arg4: SerializableWithOnlyInstruction,
    ): @AppFunctionInstruction("Overridden return description") String {
        return (arg1 + arg2).toString()
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    @AppFunctionInstruction("Overridden function description without kdoc")
    fun myInstructionFunctionWithoutKdoc(
        @AppFunctionInstruction("Overridden param description without kdoc") arg1: Int,
        arg3: SerializableWithKDocAndInstruction,
        arg4: SerializableWithOnlyInstruction,
    ): @AppFunctionInstruction("Overridden return description without kdoc") String {
        return arg1.toString()
    }
}

/** SerializableWithKDocAndInstruction KDoc. */
@AppFunctionSerializable(isDescribedByKDoc = true)
@AppFunctionInstruction("SerializableWithKDocAndInstruction instruction")
data class SerializableWithKDocAndInstruction(
    /** SerializableWithKDocAndInstruction#data KDoc */
    @AppFunctionInstruction("SerializableWithKDocAndInstruction#data instruction")
    val data: String
)

@AppFunctionSerializable(isDescribedByKDoc = false)
@AppFunctionInstruction("SerializableWithOnlyInstruction instruction")
data class SerializableWithOnlyInstruction(
    @AppFunctionInstruction("SerializableWithOnlyInstruction#data instruction")
    val data: String
)
