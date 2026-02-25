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

package androidx.wear.protolayout.layout

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContainerTest {
    @Test
    fun box_inflates() {
        val box =
            box(
                TEST_ELEMENT1,
                TEST_ELEMENT2,
                width = expand(),
                height = expand(),
                horizontalAlignment = HORIZONTAL_ALIGN_END,
                verticalAlignment = VERTICAL_ALIGN_BOTTOM,
                modifier = MODIFIER,
            )

        assertThat(box.width!!.toContainerDimensionProto())
            .isEqualTo(expand().toContainerDimensionProto())
        assertThat(box.height!!.toContainerDimensionProto())
            .isEqualTo(expand().toContainerDimensionProto())
        assertThat(box.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_END)
        assertThat(box.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_BOTTOM)
        assertThat(box.modifiers!!.toProto()).isEqualTo(MODIFIER.toProtoLayoutModifiers().toProto())
        assertThat(box.contents).hasSize(2)
        assertThat(box.contents[0]!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT1.toLayoutElementProto())
        assertThat(box.contents[1]!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT2.toLayoutElementProto())
    }

    @Test
    fun column_inflates() {
        val column =
            column(
                TEST_ELEMENT1,
                TEST_ELEMENT2,
                width = expand(),
                height = expand(),
                horizontalAlignment = HORIZONTAL_ALIGN_END,
                modifier = MODIFIER,
            )

        assertThat(column.width!!.toContainerDimensionProto())
            .isEqualTo(expand().toContainerDimensionProto())
        assertThat(column.height!!.toContainerDimensionProto())
            .isEqualTo(expand().toContainerDimensionProto())
        assertThat(column.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_END)
        assertThat(column.modifiers!!.toProto())
            .isEqualTo(MODIFIER.toProtoLayoutModifiers().toProto())
        assertThat(column.contents).hasSize(2)
        assertThat(column.contents[0]!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT1.toLayoutElementProto())
        assertThat(column.contents[1]!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT2.toLayoutElementProto())
    }

    @Test
    fun row_inflates() {
        val row =
            row(
                TEST_ELEMENT1,
                TEST_ELEMENT2,
                width = expand(),
                height = expand(),
                verticalAlignment = VERTICAL_ALIGN_BOTTOM,
                modifier = MODIFIER,
            )

        assertThat(row.width!!.toContainerDimensionProto())
            .isEqualTo(expand().toContainerDimensionProto())
        assertThat(row.height!!.toContainerDimensionProto())
            .isEqualTo(expand().toContainerDimensionProto())
        assertThat(row.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_BOTTOM)
        assertThat(row.modifiers!!.toProto()).isEqualTo(MODIFIER.toProtoLayoutModifiers().toProto())
        assertThat(row.contents).hasSize(2)
        assertThat(row.contents[0]!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT1.toLayoutElementProto())
        assertThat(row.contents[1]!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT2.toLayoutElementProto())
    }

    @Test
    fun spacer_inflates() {
        val spacer = spacer(width = expand(), height = expand(), modifier = MODIFIER)

        assertThat(spacer.width!!.toSpacerDimensionProto())
            .isEqualTo(expand().toSpacerDimensionProto())
        assertThat(spacer.height!!.toSpacerDimensionProto())
            .isEqualTo(expand().toSpacerDimensionProto())
        assertThat(spacer.modifiers!!.toProto())
            .isEqualTo(MODIFIER.toProtoLayoutModifiers().toProto())
    }

    private companion object {
        val TEST_ELEMENT1 = basicText("Test 1".layoutString)
        val TEST_ELEMENT2 = basicText("Test 2".layoutString)
        val MODIFIER = LayoutModifier.background(Color.YELLOW.argb)
    }
}
