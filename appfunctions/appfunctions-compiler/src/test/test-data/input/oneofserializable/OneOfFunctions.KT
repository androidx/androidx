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

package com.testdata.oneofserializable

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction

class OneOfFunctions {

    @AppFunction
    fun oneOfSealedParam(
        appFunctionContext: AppFunctionContext,
        oneOfSealedInterface: OneOfSealedInterface,
        oneOfSealedInterfaceList: List<OneOfSealedInterface>,
    ) {}

    @AppFunction
    fun oneOfSealedReturn(appFunctionContext: AppFunctionContext): OneOfSealedClass =
        OneOfSealedClass.ASubclass("", "")

    @AppFunction
    fun oneOfSealedListReturn(appFunctionContext: AppFunctionContext): List<OneOfSealedClass> =
        emptyList()

    @AppFunction
    fun oneOfSealedNestedSerializable(
        appFunctionContext: AppFunctionContext,
        oneOfSealedNestedSerializable: OneOfSealedNestedSerializable,
    ) = OneOfSealedNestedSerializable(BSubclass("", 0))
}

@AppFunctionSerializable
data class OneOfSealedNestedSerializable(val sealedInterface: OneOfSealedInterface)
