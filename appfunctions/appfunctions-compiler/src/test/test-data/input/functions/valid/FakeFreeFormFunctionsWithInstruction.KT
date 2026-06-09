package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInstruction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunction

class FakeFreeFormFunctionsWithInstruction {
    /**
     * This is a cool function but we want to override it.
     *
     * @param arg1 The first argument but we want to override it.
     * @param arg2 The second argument which shouldn't be overridden.
     * @return The return value but we want to override it.
     */
    @AppFunction(isDescribedByKDoc = true)
    @AppFunctionInstruction("Overridden function description")
    fun myInstructionFunction(
        appFunctionContext: AppFunctionContext,
        @AppFunctionInstruction("Overridden param description") arg1: Int,
        arg2: Int,
        arg3: SerializableWithKDocAndInstruction,
        arg4: SerializableWithOnlyInstruction,
    ): @AppFunctionInstruction("Overridden return description") String {
        return (arg1 + arg2).toString()
    }

    @AppFunction
    @AppFunctionInstruction("Overridden function description without kdoc")
    fun myInstructionFunctionWithoutKdoc(
        appFunctionContext: AppFunctionContext,
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

@AppFunctionSerializable
@AppFunctionInstruction("SerializableWithOnlyInstruction instruction")
data class SerializableWithOnlyInstruction(
    @AppFunctionInstruction("SerializableWithOnlyInstruction#data instruction")
    val data: String
)
