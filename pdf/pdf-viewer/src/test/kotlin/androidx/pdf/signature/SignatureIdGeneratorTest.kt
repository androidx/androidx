/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.signature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SignatureIdGeneratorTest {

    @Test
    fun generateSignatureId_createsUniqueIds() {
        val id1 = SignatureIdGenerator.generateSignatureId()
        val id2 = SignatureIdGenerator.generateSignatureId()

        assertNotEquals("Successive calls must generate unique IDs", id1, id2)
    }

    @Test
    fun generateSignatureId_createsManyUniqueIds() {
        val generatedIds = mutableSetOf<String>()
        val iterations = 1000

        for (i in 0 until iterations) {
            generatedIds.add(SignatureIdGenerator.generateSignatureId())
        }

        assertEquals(
            "All $iterations generated IDs must be completely unique",
            iterations,
            generatedIds.size,
        )
    }
}
