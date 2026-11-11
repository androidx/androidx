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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRotatableComponentTest {
    @Test
    fun reset_incrementsResetCount() {
        val underTest = FakeRotatableComponent()

        assertThat(underTest.resetCount).isEqualTo(0)

        underTest.reset()
        assertThat(underTest.resetCount).isEqualTo(1)

        underTest.reset()
        assertThat(underTest.resetCount).isEqualTo(2)
    }

    @Test
    fun fakeSceneRuntimeCreateRotatableComponent_returnsComponentWithWorkingResetCount() {
        val runtime = FakeSceneRuntime()
        val rotatableComponent = runtime.createRotatableComponent()

        assertThat(rotatableComponent.resetCount).isEqualTo(0)

        rotatableComponent.reset()
        assertThat(rotatableComponent.resetCount).isEqualTo(1)
    }
}
