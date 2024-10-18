/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.runtime.saveable.SaverScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.fail
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnStateSaverTest {
    private val allowingScope = SaverScope { true }

    @Test
    fun saveAndRestoreState() {
        val original = TransformingLazyColumnState(2, 10)
        val saved = with(TransformingLazyColumnState.Saver) { allowingScope.save(original) }
        if (saved == null) {
            fail("Saved state should not be null")
        }
        val restored =
            TransformingLazyColumnState.Saver.restore(saved)
                ?: fail("Restored state should not be null")

        assertThat(restored.anchorItemIndex).isEqualTo(original.anchorItemIndex)
        assertThat(restored.anchorItemScrollOffset).isEqualTo(original.anchorItemScrollOffset)
    }

    @Test
    fun saveAndRestoreStateInitialState() {
        val original = TransformingLazyColumnState(0, 0)
        val saved = with(TransformingLazyColumnState.Saver) { allowingScope.save(original) }
        if (saved == null) {
            fail("Saved state should not be null")
        }
        val restored =
            TransformingLazyColumnState.Saver.restore(saved)
                ?: fail("Restored state should not be null")

        assertThat(restored.anchorItemIndex).isEqualTo(original.anchorItemIndex)
        assertThat(restored.anchorItemScrollOffset).isEqualTo(original.anchorItemScrollOffset)
    }
}
