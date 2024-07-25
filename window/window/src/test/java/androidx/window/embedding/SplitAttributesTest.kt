/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.embedding

import android.graphics.Color
import androidx.window.core.WindowStrictModeException
import androidx.window.embedding.DividerAttributes.DraggableDividerAttributes
import androidx.window.embedding.DividerAttributes.FixedDividerAttributes
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.BOTTOM_TO_TOP
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LEFT_TO_RIGHT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.RIGHT_TO_LEFT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.TOP_TO_BOTTOM
import androidx.window.embedding.SplitAttributes.SplitType
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EQUAL
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EXPAND
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_HINGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test class to verify [SplitAttributes] */
@RunWith(RobolectricTestRunner::class)
class SplitAttributesTest {
    @Test
    fun testSplitAttributesEquals() {
        val attrs1 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_EQUAL)
                .setLayoutDirection(LOCALE)
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .build()
        val attrs2 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(LOCALE)
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .build()
        val attrs3 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(TOP_TO_BOTTOM)
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .build()
        val attrs4 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(TOP_TO_BOTTOM)
                .setAnimationBackground(
                    EmbeddingAnimationBackground.createColorBackground(Color.GREEN)
                )
                .build()
        val attrs5 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(TOP_TO_BOTTOM)
                .setAnimationBackground(
                    EmbeddingAnimationBackground.createColorBackground(Color.GREEN)
                )
                .build()

        assertNotEquals(attrs1, attrs2)
        assertNotEquals(attrs1.hashCode(), attrs2.hashCode())

        assertNotEquals(attrs2, attrs3)
        assertNotEquals(attrs2.hashCode(), attrs3.hashCode())

        assertNotEquals(attrs3, attrs1)
        assertNotEquals(attrs3.hashCode(), attrs1.hashCode())

        assertNotEquals(attrs3, attrs4)
        assertNotEquals(attrs3.hashCode(), attrs4.hashCode())

        assertEquals(attrs4, attrs5)
        assertEquals(attrs4.hashCode(), attrs5.hashCode())
    }

    @Test
    fun testSplitAttributesEquals_withDividerAttributes() {
        // No divider
        val attrs1 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_EQUAL)
                .setLayoutDirection(LOCALE)
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .build()

        // Fixed divider
        val attrs2 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_EQUAL)
                .setLayoutDirection(LOCALE)
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .setDividerAttributes(FixedDividerAttributes.Builder().build())
                .build()

        // Draggable divider
        val attrs3 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_EQUAL)
                .setLayoutDirection(LOCALE)
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .setDividerAttributes(DraggableDividerAttributes.Builder().build())
                .build()

        // Draggable divider same as attrs3
        val attrs4 =
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_EQUAL)
                .setLayoutDirection(LOCALE)
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .setDividerAttributes(DraggableDividerAttributes.Builder().build())
                .build()

        // No divider vs fixed divider
        assertNotEquals(attrs1, attrs2)
        assertNotEquals(attrs1.hashCode(), attrs2.hashCode())

        // Fixed divider vs draggable divider
        assertNotEquals(attrs2, attrs3)
        assertNotEquals(attrs2.hashCode(), attrs3.hashCode())

        // Same draggable divider
        assertEquals(attrs3, attrs4)
        assertEquals(attrs3.hashCode(), attrs4.hashCode())
    }

    @Test
    fun testTypesEquals() {
        val splitTypes =
            arrayOf(
                SPLIT_TYPE_EQUAL,
                SPLIT_TYPE_EXPAND,
                SPLIT_TYPE_HINGE,
            )

        for ((i, type1) in splitTypes.withIndex()) {
            for ((j, type2) in splitTypes.withIndex()) {
                if (i == j) {
                    assertEquals(type1, type2)
                    assertEquals(type1.hashCode(), type2.hashCode())
                } else {
                    assertNotEquals(type1, type2)
                    assertNotEquals(type1.hashCode(), type2.hashCode())
                }
            }
        }

        assertEquals(
            "Two SplitTypes must regarded as equal if their ratios are the same.",
            SPLIT_TYPE_EQUAL,
            SplitType.ratio(0.5f)
        )
        assertEquals(SPLIT_TYPE_EQUAL.hashCode(), SplitType.ratio(0.5f).hashCode())
    }

    @Test
    fun testSplitRatioRatio() {
        assertThrows(WindowStrictModeException::class.java) { SplitType.ratio(-0.01f) }
        assertThrows(WindowStrictModeException::class.java) { SplitType.ratio(0.0f) }
        SplitType.ratio(0.001f)
        SplitType.ratio(0.5f)
        SplitType.ratio(0.999f)
        assertThrows(WindowStrictModeException::class.java) { SplitType.ratio(1.0f) }
        assertThrows(WindowStrictModeException::class.java) { SplitType.ratio(1.1f) }
    }

    @Test
    fun testLayoutDirectionEquals() {
        val layoutDirectionList =
            arrayOf(
                LOCALE,
                LEFT_TO_RIGHT,
                RIGHT_TO_LEFT,
                TOP_TO_BOTTOM,
                BOTTOM_TO_TOP,
            )

        for ((i, layoutDirection1) in layoutDirectionList.withIndex()) {
            for ((j, layoutDirection2) in layoutDirectionList.withIndex()) {
                if (i == j) {
                    assertEquals(layoutDirection1, layoutDirection2)
                    assertEquals(layoutDirection1.hashCode(), layoutDirection2.hashCode())
                } else {
                    assertNotEquals(layoutDirection1, layoutDirection2)
                    assertNotEquals(layoutDirection1.hashCode(), layoutDirection2.hashCode())
                }
            }
        }
    }
}
