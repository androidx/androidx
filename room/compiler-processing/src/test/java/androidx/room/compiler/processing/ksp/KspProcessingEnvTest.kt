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
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.TypeName
import org.junit.Test

class KspProcessingEnvTest {
    @Test
    fun wrapPrimitives() {
        // make sure nullable primitives turns into the boxed version of it.
        // and non-null turns into the primitive version of it.
        runKspTest(sources = emptyList()) { invocation ->
            val intType = invocation.kspResolver.builtIns.intType
            val kspProcessingEnv = invocation.processingEnv as KspProcessingEnv
            kspProcessingEnv.wrap(
                ksType = intType,
                allowPrimitives = true
            ).let {
                assertThat(it.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                assertThat(it.typeName).isEqualTo(TypeName.INT)
            }
            kspProcessingEnv.wrap(
                ksType = intType,
                allowPrimitives = false
            ).let {
                assertThat(it.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                assertThat(it.typeName).isEqualTo(TypeName.INT.box())
            }
            kspProcessingEnv.wrap(
                ksType = intType.makeNullable(),
                allowPrimitives = true
            ).let {
                assertThat(it.nullability).isEqualTo(
                    XNullability.NULLABLE
                )
                assertThat(it.typeName).isEqualTo(TypeName.INT.box())
            }
        }
    }
}
