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

package androidx.text.vertical

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmphasisSpanTest {

    @Test
    fun parcelable_parcelRoundTrip() {
        val original =
            EmphasisSpan(
                style = EmphasisStyle.Triangle,
                isFilled = false,
                position = AnnotationPosition.After,
                scale = 0.7f,
            )

        // WHEN we write it to a Parcel and read it back
        val restored = parcelRoundTrip(original, EmphasisSpan.CREATOR)

        // THEN the restored object must be identical
        assertThat(restored.style).isEqualTo(original.style)
        assertThat(restored.isFilled).isEqualTo(original.isFilled)
        assertThat(restored.position).isEqualTo(original.position)
        assertThat(restored.scale).isEqualTo(original.scale)
    }
}
