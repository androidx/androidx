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

import androidx.xr.scenecore.runtime.Dimensions
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FakeSubspaceNodeEntityTest {
    private lateinit var underTest: FakeSubspaceNodeEntity

    @Test
    fun constructor_returnInitialValues() {
        // Arrange
        val expectedSize = Dimensions(3f, 2f, 1f)

        // Act
        underTest = FakeSubspaceNodeEntity(size = expectedSize)

        // Assert
        // Default size of FakeSubspaceNodeEntity is (2f, 1f, 0f)
        assertThat(underTest.size).isEqualTo(expectedSize)
    }
}
