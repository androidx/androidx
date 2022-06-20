/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.complications

import android.graphics.RectF
import androidx.wear.watchface.complications.data.ComplicationType
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@org.junit.runner.RunWith(SharedRobolectricTestRunner::class)
class ComplicationSlotBoundsTest {
    @Test
    public fun createFromPartialMap() {
        val complicationSlotBounds = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.1f, 0.2f, 0.3f, 0.4f),
                ComplicationType.LONG_TEXT to RectF(0.5f, 0.6f, 0.7f, 0.8f)
            ),
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.4f, 0.3f, 0.2f, 0.1f)
            )
        )

        val bounds = complicationSlotBounds.perComplicationTypeBounds

        // SHORT_TEXT and LONG_TEXT should match the input bounds
        Truth.assertThat(bounds[ComplicationType.SHORT_TEXT])
            .isEqualTo(RectF(0.1f, 0.2f, 0.3f, 0.4f))
        Truth.assertThat(bounds[ComplicationType.LONG_TEXT])
            .isEqualTo(RectF(0.5f, 0.6f, 0.7f, 0.8f))

        // All other types should have been backfilled with an empty rect.
        for (type in ComplicationType.values()) {
            if (type != ComplicationType.SHORT_TEXT && type != ComplicationType.LONG_TEXT) {
                Truth.assertThat(bounds[type]).isEqualTo(RectF())
            }
        }

        val margins = complicationSlotBounds.perComplicationTypeMargins

        // SHORT_TEXT should match the input margin.
        Truth.assertThat(margins[ComplicationType.SHORT_TEXT])
            .isEqualTo(RectF(0.4f, 0.3f, 0.2f, 0.1f))

        // All other types should have been backfilled with an empty rect.
        for (type in ComplicationType.values()) {
            if (type != ComplicationType.SHORT_TEXT) {
                Truth.assertThat(margins[type]).isEqualTo(RectF())
            }
        }
    }

    @Test
    fun equality() {
        val complicationSlotBoundsA = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.1f, 0.2f, 0.3f, 0.4f),
                ComplicationType.LONG_TEXT to RectF(0.5f, 0.6f, 0.7f, 0.8f)
            ),
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.4f, 0.3f, 0.2f, 0.1f)
            )
        )

        val complicationSlotBoundsB = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.1f, 0.2f, 0.3f, 0.4f),
                ComplicationType.LONG_TEXT to RectF(0.5f, 0.6f, 0.7f, 0.8f)
            ),
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.4f, 0.3f, 0.2f, 0.1f)
            )
        )

        val complicationSlotBoundsC = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(3f, 2f, 1f, 0f),
                ComplicationType.LONG_TEXT to RectF(0.5f, 0.6f, 0.7f, 0.8f)
            ),
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.4f, 0.3f, 0.2f, 0.1f)
            )
        )

        val complicationSlotBoundsD = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.1f, 0.2f, 0.3f, 0.4f),
                ComplicationType.LONG_TEXT to RectF(3f, 2f, 1f, 0f)
            ),
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.4f, 0.3f, 0.2f, 0.1f)
            )
        )

        val complicationSlotBoundsE = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.1f, 0.2f, 0.3f, 0.4f),
                ComplicationType.LONG_TEXT to RectF(0.5f, 0.6f, 0.7f, 0.8f)
            ),
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(3f, 2f, 1f, 0f)
            )
        )

        val complicationSlotBoundsF = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.1f, 0.2f, 0.3f, 0.4f)
            ),
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(3f, 2f, 1f, 0f)
            )
        )

        val complicationSlotBoundsG = ComplicationSlotBounds.createFromPartialMap(
            mapOf(
                ComplicationType.SHORT_TEXT to RectF(0.1f, 0.2f, 0.3f, 0.4f),
                ComplicationType.LONG_TEXT to RectF(0.5f, 0.6f, 0.7f, 0.8f)
            ),
            emptyMap()
        )

        assertThat(complicationSlotBoundsA).isEqualTo(complicationSlotBoundsB)
        assertThat(complicationSlotBoundsA).isNotEqualTo(complicationSlotBoundsC)
        assertThat(complicationSlotBoundsA).isNotEqualTo(complicationSlotBoundsD)
        assertThat(complicationSlotBoundsA).isNotEqualTo(complicationSlotBoundsE)
        assertThat(complicationSlotBoundsA).isNotEqualTo(complicationSlotBoundsF)
        assertThat(complicationSlotBoundsA).isNotEqualTo(complicationSlotBoundsG)
    }
}