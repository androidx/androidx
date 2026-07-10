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

import androidx.appfunctions.AppFunctionOneOfType
import androidx.appfunctions.AppFunctionSerializable

/** I am a KDoc for OneOfSealedClass */
@AppFunctionSerializable(isDescribedByKDoc = true)
@AppFunctionOneOfType(
    matchOneOf =
        [NonSealedOneOfSerializable.ASubclass::class, NonSealedOneOfSerializable.BSubclass::class]
)
abstract class NonSealedOneOfSerializable {

    abstract val interfaceProperty: String

    /** I am a KDoc for OneOfSealedClass.ASubclass */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    class ASubclass(override val interfaceProperty: String, val str: String) :
        NonSealedOneOfSerializable()

    /** I am a KDoc for OneOfSealedClass.BSubclass */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    class BSubclass(override val interfaceProperty: String, val integer: Int) :
        NonSealedOneOfSerializable()

    /** I am a KDoc for OneOfSealedClass.CSubclass */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    class CSubclass(override val interfaceProperty: String, val integer: Int) :
        NonSealedOneOfSerializable()
}
