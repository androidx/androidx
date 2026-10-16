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

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("OneOfFunctionsService", "app_functions_v2")
abstract class OneOfFunctions : AppFunctionService() {

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun oneOfSealedParam(
        oneOfSealedInterface: OneOfSealedInterface,
        oneOfSealedInterfaceNullable: OneOfSealedInterface?,
        oneOfSealedInterfaceList: List<OneOfSealedInterface>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun oneOfSealedReturn(): OneOfSealedClass =
        OneOfSealedClass.ASubclass("", "")

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun oneOfSealedListReturn(): List<OneOfSealedClass> =
        emptyList()

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun oneOfSealedNestedSerializable(
        oneOfSealedNestedSerializable: OneOfSealedNestedSerializable,
    ) = OneOfSealedNestedSerializable(BSubclass("", 0))

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun oneOfNonSealedOneOfSerializableParam(
        nonSealedOneOfSerializable: NonSealedOneOfSerializable,
        nonSealedOneOfSerializableNullable: NonSealedOneOfSerializable?,
        nonSealedOneOfSerializableList: List<NonSealedOneOfSerializable>,
    ) {}
}

@AppFunctionSerializable(isDescribedByKDoc = false)
data class OneOfSealedNestedSerializable(val sealedInterface: OneOfSealedInterface)
