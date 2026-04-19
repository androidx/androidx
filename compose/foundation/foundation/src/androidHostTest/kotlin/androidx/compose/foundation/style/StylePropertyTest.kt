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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StylePropertyTest {
    @Test
    fun can_create_style_properties() {
        val properties = StyleProperties()
        assertNotNull(properties)
    }

    @Test
    fun style_properties_have_correct_default_values() {
        val properties = StyleProperties()
        assertEquals(0f, properties.contentPaddingStart)
        assertEquals(0f, properties.contentPaddingEnd)
        assertEquals(0f, properties.contentPaddingTop)
        assertEquals(0f, properties.contentPaddingBottom)
        assertEquals(0f, properties.externalPaddingStart)
        assertEquals(0f, properties.externalPaddingEnd)
        assertEquals(0f, properties.externalPaddingTop)
        assertEquals(0f, properties.externalPaddingBottom)
        assertEquals(0f, properties.borderWidth)
        assertTrue(properties.width.isNaN())
        assertTrue(properties.height.isNaN())
        assertTrue(properties.widthFraction.isNaN())
        assertTrue(properties.heightFraction.isNaN())
        assertEquals(0f, properties.left)
        assertEquals(0f, properties.right)
        assertEquals(0f, properties.top)
        assertEquals(0f, properties.bottom)
        assertTrue(properties.minWidth.isNaN())
        assertTrue(properties.minHeight.isNaN())
        assertTrue(properties.maxWidth.isNaN())
        assertTrue(properties.maxHeight.isNaN())
        assertEquals(Color.Black, properties.borderColor)
        assertNull(properties.borderBrush)
        assertEquals(Color.Transparent, properties.backgroundColor)
        assertNull(properties.backgroundBrush)
        assertEquals(Color.Unspecified, properties.foregroundColor)
        assertNull(properties.foregroundBrush)
        assertFalse(properties.clip)
        assertEquals(RectangleShape, properties.shape)
        assertNull(properties.dropShadow)
        assertNull(properties.innerShadow)
        assertEquals(1.0f, properties.alpha)
        assertEquals(1.0f, properties.scaleX)
        assertEquals(1.0f, properties.scaleY)
        assertEquals(0f, properties.translationX)
        assertEquals(0f, properties.translationY)
        assertEquals(0f, properties.rotationX)
        assertEquals(0f, properties.rotationY)
        assertEquals(0f, properties.rotationZ)
        assertEquals(TransformOrigin.Center.pivotFractionX, properties.transformOriginX)
        assertEquals(TransformOrigin.Center.pivotFractionY, properties.transformOriginY)
        assertEquals(1.0f, properties.cameraDistance)
        assertEquals(0f, properties.zIndex)
        assertEquals(Color.Unspecified, properties.contentColor)
        assertNull(properties.contentBrush)
        assertNull(properties.fontFamily)
        assertNull(properties.textIndent)
        assertEquals(TextUnit.Unspecified, properties.fontSize)
        assertEquals(TextUnit.Unspecified, properties.lineHeight)
        assertEquals(TextUnit.Unspecified, properties.letterSpacing)
        assertEquals(BaselineShift.Unspecified, properties.baselineShift)
        assertEquals(LineBreak.Unspecified, properties.lineBreak)
        assertEquals(FontStyle.Normal, properties.fontStyle)
        assertEquals(TextAlign.Unspecified, properties.textAlign)
        assertEquals(TextDirection.Unspecified, properties.textDirection)
        assertEquals(Hyphens.Unspecified, properties.hyphens)
        assertEquals(FontWeight.Normal, properties.fontWeight)
    }

    @Test
    fun can_set_property_content_padding_start() {
        val properties = StyleProperties()
        val expected = 10f
        properties.contentPaddingStart(expected)
        assertTrue(properties.onlyHasId(ContentPaddingStartId))
        assertEquals(expected, properties.contentPaddingStart)
        assertEquals(InnerLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_content_padding_end() {
        val properties = StyleProperties()
        val expected = 10f
        properties.contentPaddingEnd(expected)
        assertTrue(properties.onlyHasId(ContentPaddingEndId))
        assertEquals(expected, properties.contentPaddingEnd)
        assertEquals(InnerLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_content_padding_top() {
        val properties = StyleProperties()
        val expected = 10f
        properties.contentPaddingTop(expected)
        assertTrue(properties.onlyHasId(ContentPaddingTopId))
        assertEquals(expected, properties.contentPaddingTop)
        assertEquals(InnerLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_content_padding_bottom() {
        val properties = StyleProperties()
        val expected = 10f
        properties.contentPaddingBottom(expected)
        assertTrue(properties.onlyHasId(ContentPaddingBottomId))
        assertEquals(expected, properties.contentPaddingBottom)
        assertEquals(InnerLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_external_padding_start() {
        val properties = StyleProperties()
        val expected = 10f
        properties.externalPaddingStart(expected)
        assertTrue(properties.onlyHasId(ExternalPaddingStartId))
        assertEquals(expected, properties.externalPaddingStart)
        assertEquals(OuterLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_external_padding_end() {
        val properties = StyleProperties()
        val expected = 10f
        properties.externalPaddingEnd(expected)
        assertTrue(properties.onlyHasId(ExternalPaddingEndId))
        assertEquals(expected, properties.externalPaddingEnd)
        assertEquals(OuterLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_external_padding_top() {
        val properties = StyleProperties()
        val expected = 10f
        properties.externalPaddingTop(expected)
        assertTrue(properties.onlyHasId(ExternalPaddingTopId))
        assertEquals(expected, properties.externalPaddingTop)
        assertEquals(OuterLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_external_padding_bottom() {
        val properties = StyleProperties()
        val expected = 10f
        properties.externalPaddingBottom(expected)
        assertTrue(properties.onlyHasId(ExternalPaddingBottomId))
        assertEquals(expected, properties.externalPaddingBottom)
        assertEquals(OuterLayoutFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_border_width() {
        val properties = StyleProperties()
        val expected = 10f
        properties.borderWidth(expected)
        assertTrue(properties.onlyHasId(BorderWidthId))
        assertEquals(expected, properties.borderWidth)
        assertEquals(InnerLayoutFlag or DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_border_color() {
        val properties = StyleProperties()
        val expected = Color.Blue
        properties.borderColor(expected)
        assertTrue(properties.onlyHasId(BorderColorId))
        assertEquals(expected, properties.borderColor)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_border_brush() {
        val properties = StyleProperties()
        val expected = SolidColor(Color.Green)
        properties.borderBrush(expected)
        assertTrue(properties.onlyHasId(BorderBrushId))
        assertEquals(expected, properties.borderBrush)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_background_color() {
        val properties = StyleProperties()
        val expected = Color.Blue
        properties.backgroundColor(expected)
        assertTrue(properties.onlyHasId(BackgroundColorId))
        assertEquals(expected, properties.backgroundColor)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_background_brush() {
        val properties = StyleProperties()
        val expected = SolidColor(Color.Green)
        properties.backgroundBrush(expected)
        assertTrue(properties.onlyHasId(BackgroundBrushId))
        assertEquals(expected, properties.backgroundBrush)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_foreground_color() {
        val properties = StyleProperties()
        val expected = Color.Blue
        properties.foregroundColor(expected)
        assertTrue(properties.onlyHasId(ForegroundColorId))
        assertEquals(expected, properties.foregroundColor)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_foreground_brush() {
        val properties = StyleProperties()
        val expected = SolidColor(Color.Green)
        properties.foregroundBrush(expected)
        assertTrue(properties.onlyHasId(ForegroundBrushId))
        assertEquals(expected, properties.foregroundBrush)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_shape() {
        val properties = StyleProperties()
        val expected = RoundedCornerShape(1)
        properties.shape(expected)
        assertTrue(properties.onlyHasId(ShapeId))
        assertEquals(expected, properties.shape)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_drop_shadow() {
        val properties = StyleProperties()
        val expected = Shadow()
        properties.dropShadow(expected)
        assertTrue(properties.onlyHasId(DropShadowId))
        assertEquals(expected, properties.dropShadow)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_inner_shadow() {
        val properties = StyleProperties()
        val expected = Shadow()
        properties.innerShadow(expected)
        assertTrue(properties.onlyHasId(InnerShadowId))
        assertEquals(expected, properties.innerShadow)
        assertEquals(DrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_content_color() {
        val properties = StyleProperties()
        val expected = Color.Blue
        properties.contentColor(expected)
        assertTrue(properties.onlyHasId(ContentColorId))
        assertEquals(expected, properties.contentColor)
        assertEquals(TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_content_brush() {
        val properties = StyleProperties()
        val expected = SolidColor(Color.Green)
        properties.contentBrush(expected)
        assertTrue(properties.onlyHasId(ContentBrushId))
        assertEquals(expected, properties.contentBrush)
        assertEquals(TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_font_family() {
        val properties = StyleProperties()
        val expected = FontFamily.Serif
        properties.fontFamily(expected)
        assertTrue(properties.onlyHasId(FontFamilyId))
        assertEquals(expected, properties.fontFamily)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_text_indent() {
        val properties = StyleProperties()
        val expected = TextIndent(1.sp, 2.sp)
        properties.textIndent(expected)
        assertTrue(properties.onlyHasId(TextIndentId))
        assertEquals(expected, properties.textIndent)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_font_size() {
        val properties = StyleProperties()
        val expected = 10.sp
        properties.fontSize(expected)
        assertTrue(properties.onlyHasId(FontSizeId))
        assertEquals(expected, properties.fontSize)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_line_height() {
        val properties = StyleProperties()
        val expected = 10.sp
        properties.lineHeight(expected)
        assertTrue(properties.onlyHasId(LineHeightId))
        assertEquals(expected, properties.lineHeight)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_letter_spacing() {
        val properties = StyleProperties()
        val expected = 10.sp
        properties.letterSpacing(expected)
        assertTrue(properties.onlyHasId(LetterSpacingId))
        assertEquals(expected, properties.letterSpacing)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_line_break() {
        val properties = StyleProperties()
        val expected = LineBreak.Paragraph
        properties.lineBreak(expected)
        assertTrue(properties.onlyHasId(LineBreakId))
        assertEquals(expected, properties.lineBreak)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_font_style() {
        val properties = StyleProperties()
        val expected = FontStyle.Italic
        properties.fontStyle(expected)
        assertTrue(properties.onlyHasId(FontStyleId))
        assertEquals(expected, properties.fontStyle)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_text_align() {
        val properties = StyleProperties()
        val expected = TextAlign.Center
        properties.textAlign(expected)
        assertTrue(properties.onlyHasId(TextAlignId))
        assertEquals(expected, properties.textAlign)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_text_direction() {
        val properties = StyleProperties()
        val expected = TextDirection.Rtl
        properties.textDirection(expected)
        assertTrue(properties.onlyHasId(TextDirectionId))
        assertEquals(expected, properties.textDirection)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_hyphens() {
        val properties = StyleProperties()
        val expected = Hyphens.Auto
        properties.hyphens(expected)
        assertTrue(properties.onlyHasId(HyphensId))
        assertEquals(expected, properties.hyphens)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_font_weight() {
        val properties = StyleProperties()
        val expected = FontWeight.Bold
        properties.fontWeight(expected)
        assertTrue(properties.onlyHasId(FontWeightId))
        assertEquals(expected, properties.fontWeight)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun can_set_property_font_synthesis() {
        val properties = StyleProperties()
        val expected = FontSynthesis.Weight
        properties.fontSynthesis(expected)
        assertTrue(properties.onlyHasId(FontSynthesisId))
        assertEquals(expected, properties.fontSynthesis)
        assertEquals(TextLayoutFlag or TextDrawFlag, properties.phaseFlags)
    }

    @Test
    fun color_brush_override_border() {
        val properties = StyleProperties()
        val color = Color.Blue
        val brush = SolidColor(Color.Red)
        properties.borderBrush(brush)
        assertTrue(properties.onlyHasId(BorderBrushId))
        properties.borderColor(color)
        assertTrue(properties.onlyHasId(BorderColorId))
    }

    @Test
    fun color_brush_override_background() {
        val properties = StyleProperties()
        val color = Color.Blue
        val brush = SolidColor(Color.Red)
        properties.backgroundBrush(brush)
        assertTrue(properties.onlyHasId(BackgroundBrushId))
        properties.backgroundColor(color)
        assertTrue(properties.onlyHasId(BackgroundColorId))
    }

    @Test
    fun color_brush_override_foreground() {
        val properties = StyleProperties()
        val color = Color.Blue
        val brush = SolidColor(Color.Red)
        properties.foregroundBrush(brush)
        assertTrue(properties.onlyHasId(ForegroundBrushId))
        properties.foregroundColor(color)
        assertTrue(properties.onlyHasId(ForegroundColorId))
    }

    @Test
    fun color_brush_override_content() {
        val properties = StyleProperties()
        val color = Color.Blue
        val brush = SolidColor(Color.Red)
        properties.contentBrush(brush)
        assertTrue(properties.onlyHasId(ContentBrushId))
        properties.contentColor(color)
        assertTrue(properties.onlyHasId(ContentColorId))
    }
}

private fun StyleProperties.onlyHasId(primitivesId: Byte): Boolean {
    for (i in 0 until PrimitivePropertyCount) {
        val id = i.toByte()
        if (hasId(id) != (primitivesId == id)) return false
    }
    for (id in FirstObjectProperty..LastObjectProperty) {
        if (hasId(id)) return false
    }
    return true
}

private fun StyleProperties.onlyHasId(objectId: Int): Boolean {
    for (i in 0 until PrimitivePropertyCount) {
        val id = i.toByte()
        if (hasId(id)) return false
    }
    for (id in FirstObjectProperty..LastObjectProperty) {
        if (hasId(id) != (id == objectId)) return false
    }
    return true
}
