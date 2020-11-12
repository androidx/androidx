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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.testcode.KotlinTestClass
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KotlinMetadataTest {
    @Test
    fun readWithMetadata() {
        val source = Source.kotlin(
            "Dummy.kt",
            """
            class Dummy
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement(KotlinTestClass::class)
            element.getMethod("mySuspendMethod").apply {
                assertThat(parameters).hasSize(2)
                assertThat(getParameter("param1").type.typeName)
                    .isEqualTo(String::class.typeName())
                assertThat(isSuspendFunction()).isTrue()
            }
        }
    }
}
