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

package androidx.a2ui.model.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiUserActionTest {

    @Test
    fun eventAction_equalsAndHashCode_contracts() {
        com.google.common.testing
            .EqualsTester()
            .addEqualityGroup(
                A2uiEventAction("s1", "c1", 100L, "e1", mapOf("k" to "v")),
                A2uiEventAction("s1", "c1", 100L, "e1", mapOf("k" to "v")),
            )
            .addEqualityGroup(A2uiEventAction("s2", "c1", 100L, "e1", mapOf("k" to "v")))
            .addEqualityGroup(A2uiEventAction("s1", "c2", 100L, "e1", mapOf("k" to "v")))
            .addEqualityGroup(A2uiEventAction("s1", "c1", 200L, "e1", mapOf("k" to "v")))
            .addEqualityGroup(A2uiEventAction("s1", "c1", 100L, "e2", mapOf("k" to "v")))
            .addEqualityGroup(A2uiEventAction("s1", "c1", 100L, "e1", mapOf("k2" to "v2")))
            .testEquals()
    }

    @Test
    fun eventAction_toString_returnsExpectedFormat() {
        val action = A2uiEventAction("s1", "c1", 100L, "e1", mapOf("k" to "v"))
        assertThat(action.toString())
            .isEqualTo(
                "A2uiEventAction(surfaceId=s1, componentId=c1, timestamp=100, eventName=e1, context={k=v})"
            )
    }

    @Test
    fun functionCallAction_equalsAndHashCode_contracts() {
        com.google.common.testing
            .EqualsTester()
            .addEqualityGroup(
                A2uiFunctionCallAction("s1", "c1", 100L, "f1", mapOf("k" to "v")),
                A2uiFunctionCallAction("s1", "c1", 100L, "f1", mapOf("k" to "v")),
            )
            .addEqualityGroup(A2uiFunctionCallAction("s2", "c1", 100L, "f1", mapOf("k" to "v")))
            .addEqualityGroup(A2uiFunctionCallAction("s1", "c2", 100L, "f1", mapOf("k" to "v")))
            .addEqualityGroup(A2uiFunctionCallAction("s1", "c1", 200L, "f1", mapOf("k" to "v")))
            .addEqualityGroup(A2uiFunctionCallAction("s1", "c1", 100L, "f2", mapOf("k" to "v")))
            .addEqualityGroup(A2uiFunctionCallAction("s1", "c1", 100L, "f1", mapOf("k2" to "v2")))
            .testEquals()
    }

    @Test
    fun functionCallAction_toString_returnsExpectedFormat() {
        val action = A2uiFunctionCallAction("s1", "c1", 100L, "f1", mapOf("k" to "v"))
        assertThat(action.toString())
            .isEqualTo(
                "A2uiFunctionCallAction(surfaceId=s1, componentId=c1, timestamp=100, functionName=f1, args={k=v})"
            )
    }
}
