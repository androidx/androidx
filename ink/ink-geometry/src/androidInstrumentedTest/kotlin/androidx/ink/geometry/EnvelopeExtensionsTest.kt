/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class EnvelopeExtensionsTest {

    @Test
    fun getBoundsRectF_whenNoBounds_returnsFalseAndDoesNotModifyOutParameter() {
        val envelope = BoxAccumulator()

        val outRect = RectF(1F, 2F, 3F, 4F)
        assertThat(envelope.getBounds(outRect)).isFalse()
        assertThat(outRect).isEqualTo(RectF(1F, 2F, 3F, 4F))
    }

    @Test
    fun getBoundsRectF_whenHasBounds_returnsTrueAndOverwritesOutParameter() {
        val envelope =
            BoxAccumulator()
                .add(MutableBox().populateFromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f)))

        val outRect = RectF(5F, 6F, 7F, 8F)
        assertThat(envelope.getBounds(outRect)).isTrue()
        assertThat(outRect).isEqualTo(RectF(1F, 2F, 3F, 4F))
    }
}
