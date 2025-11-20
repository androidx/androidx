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

package androidx.xr.scenecore.testing

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeComponentTest {
    private lateinit var underTest: FakeComponent

    @Before
    fun setUp() {
        underTest = FakeComponent()
    }

    @Test
    fun onAttach_addsEntityToList() {
        assertThat(underTest.canBeAttached).isTrue()

        val entity1 = FakeEntity()
        // The component is attachable by default.
        assertThat(underTest.onAttach(entity1)).isTrue()
        // Simulates that the component can be attached multiple times.
        assertThat(underTest.onAttach(entity1)).isTrue()

        val entity2 = FakeEntity()
        // Simulates that the component can be attached multiple times, even to different entity.
        assertThat(underTest.onAttach(entity2)).isTrue()

        underTest.canBeAttached = false
        assertThat(underTest.onAttach(entity1)).isFalse()

        underTest.onDetach(entity1)
        // After detaching, the component becomes attachable.
        assertThat(underTest.onAttach(entity1)).isTrue()
    }
}
