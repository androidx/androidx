/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.integration.testapp.library

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInstruction
import androidx.appfunctions.AppFunctionIntValueConstraint
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionStringValueConstraint

@Suppress("UNUSED_PARAMETER")
class TestFunctions2 {
    /**
     * Concatenates the two given strings.
     *
     * @param str1 The first string.
     * @param str2 The second string.
     * @return The result of concatenating the two strings.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun concat(appFunctionContext: AppFunctionContext, str1: String, str2: String) = str1 + str2

    @AppFunction
    fun functionWithSerializableParameter(
        appFunctionContext: AppFunctionContext,
        exampleSerializable: ExampleSerializable,
        genericSerializable: GenericSerializable<Int>,
    ) {}

    @AppFunction
    @AppFunctionIntValueConstraint(enumValues = [10, 20])
    fun enumValueFunction(
        appFunctionContext: AppFunctionContext,
        @AppFunctionIntValueConstraint(enumValues = [0, 1]) intEnum: Int,
        @AppFunctionStringValueConstraint(enumValues = ["A", "B"]) stringEnum: String,
    ): Int = 10

    /** @param arg2 This arg2 shouldn't be overridden */
    @AppFunction(isDescribedByKDoc = true)
    @AppFunctionInstruction("instruction for function")
    fun functionWithInstruction(
        appFunctionContext: AppFunctionContext,
        @AppFunctionInstruction("instruction for param1") param1: String,
        arg2: String,
    ): @AppFunctionInstruction("instruction for return") String = param1 + arg2

    @AppFunction
    @AppFunctionInstruction("instruction for function without kdoc")
    fun functionWithInstructionWithoutKdoc(
        appFunctionContext: AppFunctionContext,
        @AppFunctionInstruction("instruction for param1 without kdoc") param1: String,
        arg2: String,
    ): @AppFunctionInstruction("instruction for return without kdoc") String = param1 + arg2
}

/** AppFunctionSerializable in non-root library. */
@AppFunctionInstruction("Instruction for ExampleSerializable.")
@AppFunctionSerializable(isDescribedByKDoc = true)
class ExampleSerializable(
    /** Int property of ExampleSerializable. */
    @AppFunctionInstruction("Instruction for intProperty.") val intProperty: Int
)

/** Example parameterized AppFunctionSerializable in another package. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class GenericSerializable<T>(
    /** Value property of GenericSerializable. */
    val value: T
)
