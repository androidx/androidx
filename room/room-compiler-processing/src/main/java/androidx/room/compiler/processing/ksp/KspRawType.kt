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

import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.rawTypeName

internal class KspRawType constructor(private val original: KspType) : XRawType {
    private val ksType by lazy { original.ksType.starProjection().makeNotNullable() }

    override val typeName by lazy { xTypeName.java }

    private val xTypeName: XTypeName by lazy {
        XTypeName(
            original.asTypeName().java.rawTypeName(),
            original.asTypeName().kotlin.rawTypeName(),
            original.nullability
        )
    }

    override fun asTypeName() = xTypeName

    override fun isAssignableFrom(other: XRawType): Boolean {
        check(other is KspRawType)
        return ksType.isAssignableFrom(other.ksType)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || xTypeName == (other as? XRawType)?.asTypeName()
    }

    override fun hashCode(): Int {
        return xTypeName.hashCode()
    }

    override fun toString(): String {
        return xTypeName.kotlin.toString()
    }
}
