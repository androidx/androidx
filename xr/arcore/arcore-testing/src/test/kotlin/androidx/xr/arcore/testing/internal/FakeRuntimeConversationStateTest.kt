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

package androidx.xr.arcore.testing.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRuntimeConversationStateTest {

    private lateinit var fakeConversationSceneSignal: FakeRuntimeConversationState

    @Before
    fun setUp() {
        fakeConversationSceneSignal = FakeRuntimeConversationState()
    }

    @Test
    fun defaultState_conversationState_isZero() {
        assertThat(fakeConversationSceneSignal.confidence).isEqualTo(0f)
        assertThat(fakeConversationSceneSignal.type).isEqualTo(0)
    }

    @Test
    fun setConversationState_updatesInternalState() {
        val mockConfidence = 0.95f
        val mockType = 2

        fakeConversationSceneSignal.confidence = mockConfidence
        fakeConversationSceneSignal.type = mockType

        assertThat(fakeConversationSceneSignal).isNotNull()
        assertThat(fakeConversationSceneSignal.confidence).isEqualTo(mockConfidence)
        assertThat(fakeConversationSceneSignal.type).isEqualTo(mockType)
    }
}
