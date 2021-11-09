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

import androidx.room.compiler.processing.XNullability
import com.google.devtools.ksp.symbol.KSType
import com.squareup.javapoet.TypeName

/**
 * Representation of `void` in KSP.
 *
 * By default, kotlin.Unit is a valid type in jvm and does not get auto-converted to void (unlike
 * kotlin.Int etc). For those cases, KspProcessingEnv uses this type to properly represent java
 * void in Kotlin so that Room can generate the correct java code.
 */
internal class KspVoidType(
    env: KspProcessingEnv,
    ksType: KSType,
    val boxed: Boolean
) : KspType(env, ksType) {
    override val typeName: TypeName
        get() = if (boxed || nullability == XNullability.NULLABLE) {
            TypeName.VOID.box()
        } else {
            TypeName.VOID
        }

    override fun boxed(): KspType {
        return if (boxed) {
            this
        } else {
            KspVoidType(
                env = env,
                ksType = ksType,
                boxed = true
            )
        }
    }

    override fun copyWithNullability(nullability: XNullability): KspType {
        return KspVoidType(
            env = env,
            ksType = ksType.withNullability(nullability),
            boxed = boxed || nullability == XNullability.NULLABLE
        )
    }
}
