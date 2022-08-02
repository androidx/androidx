/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XExecutableType
import androidx.room.compiler.processing.XType

internal abstract class KspExecutableType(
    val env: KspProcessingEnv,
    open val origin: KspExecutableElement,
    val containing: KspType?
) : XExecutableType {
    override val parameterTypes: List<XType> by lazy {
        if (containing == null) {
            origin.parameters.map {
                it.type
            }
        } else {
            origin.parameters.map {
                it.asMemberOf(containing)
            }
        }
    }

    override val thrownTypes: List<XType>
        // The thrown types are the same as on the origin since those can't change
        get() = origin.thrownTypes

    override fun isSameType(other: XExecutableType): Boolean {
        return env.isSameType(this, other)
    }
}
