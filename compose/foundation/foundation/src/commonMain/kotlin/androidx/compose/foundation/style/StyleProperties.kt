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

@file:Suppress("NOTHING_TO_INLINE", "SameParameterValue")

package androidx.compose.foundation.style

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.LightingColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.shadow.lerp
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.lerp
import kotlin.jvm.JvmField

internal const val InnerLayoutFlag: Int = 1 shl 0
internal const val DrawFlag: Int = 1 shl 1
internal const val LayerFlag: Int = 1 shl 2
internal const val OuterLayoutFlag: Int = 1 shl 3
internal const val TextLayoutFlag: Int = 1 shl 4
internal const val TextDrawFlag: Int = 1 shl 5

internal const val InheritedFlags: Int = TextLayoutFlag or TextDrawFlag

internal const val PhaseFlagMask =
    InnerLayoutFlag or DrawFlag or LayerFlag or OuterLayoutFlag or TextDrawFlag or TextLayoutFlag

// Text Enum Packing (offset, length)
// ====
//  0,  2 bits: fontStyle
//  2,  3 bits: textAlign
//  4,  3 bits: textDirection
//  8,  2 bits: hyphens
// 10,  4 bits: fontSynthesis
// 14,  3 bits: textDecoration
// 17, 10 bits: fontWeight
internal const val FontStyleShift: Int = 0
internal const val TextAlignShift: Int = 2
internal const val TextDirectionShift: Int = 4
internal const val HyphensShift: Int = 8
internal const val FontSynthesisShift: Int = 10
internal const val TextDecorationShift: Int = 14
internal const val FontWeightShift: Int = 17

internal const val FontStyleMask: Int = 0b11 shl FontStyleShift
internal const val TextAlignMask: Int = 0b111 shl TextAlignShift
internal const val TextDirectionMask: Int = 0b111 shl TextDirectionShift
internal const val HyphensMask: Int = 0b11 shl HyphensShift
internal const val FontSynthesisMask: Int = 0b1111 shl FontSynthesisShift
internal const val TextDecorationMask: Int = 0b111 shl TextDecorationShift
internal const val FontWeightMask: Int = 0b11_1111_1111 shl FontWeightShift

// Primitive property ids. These are Byte to ensure that a primitive id  is not used where an
// object id is expected (and vis versa).
internal const val ContentPaddingStartId: Byte = 0
internal const val ContentPaddingEndId: Byte = 1
internal const val ContentPaddingTopId: Byte = 2
internal const val ContentPaddingBottomId: Byte = 3
internal const val ExternalPaddingStartId: Byte = 4
internal const val ExternalPaddingEndId: Byte = 5
internal const val ExternalPaddingTopId: Byte = 6
internal const val ExternalPaddingBottomId: Byte = 7
internal const val BorderWidthId: Byte = 8
internal const val WidthId: Byte = 9
internal const val HeightId: Byte = 10
internal const val WidthFractionId: Byte = 11
internal const val HeightFractionId: Byte = 12
internal const val LeftId: Byte = 13
internal const val TopId: Byte = 14
internal const val RightId: Byte = 15
internal const val BottomId: Byte = 16
internal const val MinWidthId: Byte = 17
internal const val MinHeightId: Byte = 18
internal const val MaxWidthId: Byte = 19
internal const val MaxHeightId: Byte = 20
internal const val AlphaId: Byte = 21
internal const val ScaleXId: Byte = 22
internal const val ScaleYId: Byte = 23
internal const val TranslationXId: Byte = 24
internal const val TranslationYId: Byte = 25
internal const val RotationXId: Byte = 26
internal const val RotationYId: Byte = 27
internal const val RotationZId: Byte = 28
internal const val TransformOriginXId: Byte = 29
internal const val TransformOriginYId: Byte = 30
internal const val ClipId: Byte = 31
internal const val ZIndexId: Byte = 32
internal const val CameraDistanceId: Byte = 33
internal const val BackgroundColorId: Byte = 34
internal const val BorderColorId: Byte = 35
internal const val ForegroundColorId: Byte = 36
internal const val ContentColorId: Byte = 37
internal const val TextDecorationId: Byte = 38
internal const val FontWeightId: Byte = 39
internal const val FontStyleId: Byte = 40
internal const val TextAlignId: Byte = 41
internal const val TextDirectionId: Byte = 42
internal const val BaselineShiftId: Byte = 43
internal const val HyphensId: Byte = 44
internal const val FontSynthesisId: Byte = 45
internal const val FontSizeId: Byte = 46
internal const val LineHeightId: Byte = 47
internal const val LetterSpacingId: Byte = 48
internal const val LineBreakId: Byte = 49
internal const val PrimitivePropertyCount = 50

internal const val TextEnumMask =
    (1L shl FontSizeId.toInt()) or
        (1L shl TextDecorationId.toInt()) or
        (1L shl TextAlignId.toInt()) or
        (1L shl TextDirectionId.toInt()) or
        (1L shl HyphensId.toInt()) or
        (1L shl FontStyleId.toInt()) or
        (1L shl FontSynthesisId.toInt()) or
        (1L shl FontWeightId.toInt())

// Object ids
// These are type Int to using an object id when a primitive id is expected
// The value of these start where the primitive properties leave off ensure they
// avoid conflicting values. As the values are unique between them the value itself can be used
// as a key in, for example, identifying an animation for the property.
internal const val FirstObjectProperty = PrimitivePropertyCount
internal const val BorderBrushId = FirstObjectProperty + 0
internal const val BackgroundBrushId = FirstObjectProperty + 1
internal const val ForegroundBrushId = FirstObjectProperty + 2
internal const val ShapeId = FirstObjectProperty + 3
internal const val ColorFilterId = FirstObjectProperty + 4
internal const val DropShadowId = FirstObjectProperty + 5
internal const val InnerShadowId = FirstObjectProperty + 6
internal const val ContentBrushId = FirstObjectProperty + 7
internal const val FontFamilyId = FirstObjectProperty + 8
internal const val TextMotionId = FirstObjectProperty + 9
internal const val TextIndentId = FirstObjectProperty + 10
internal const val LastObjectProperty = TextIndentId
internal const val ObjectPropertyCount = LastObjectProperty - FirstObjectProperty + 1
internal const val PropertyCount = PrimitivePropertyCount + ObjectPropertyCount
private val propertyFlags = IntArray(PropertyCount)

internal const val BrushPropertiesMask =
    (1 shl (BorderBrushId - FirstObjectProperty)) or
        (1 shl (BackgroundBrushId - FirstObjectProperty)) or
        (1 shl (ForegroundBrushId - FirstObjectProperty)) or
        (1 shl (ContentBrushId - FirstObjectProperty))

private inline fun Byte.flag(flags: Int) {
    propertyFlags[this.toInt()] = propertyFlags[this.toInt()] or flags
}

private inline fun Int.flag(flags: Int) {
    propertyFlags[this] = propertyFlags[this] or flags
}

private fun buildFlags() {
    ContentPaddingTopId.flag(InnerLayoutFlag)
    ContentPaddingBottomId.flag(InnerLayoutFlag)
    ContentPaddingStartId.flag(InnerLayoutFlag)
    ContentPaddingEndId.flag(InnerLayoutFlag)
    ExternalPaddingTopId.flag(OuterLayoutFlag)
    ExternalPaddingBottomId.flag(OuterLayoutFlag)
    ExternalPaddingStartId.flag(OuterLayoutFlag)
    ExternalPaddingEndId.flag(OuterLayoutFlag)
    BorderWidthId.flag(DrawFlag or InnerLayoutFlag)
    BorderColorId.flag(DrawFlag)
    BorderBrushId.flag(DrawFlag)
    WidthId.flag(OuterLayoutFlag)
    HeightId.flag(OuterLayoutFlag)
    WidthFractionId.flag(OuterLayoutFlag)
    HeightFractionId.flag(OuterLayoutFlag)
    LeftId.flag(OuterLayoutFlag)
    TopId.flag(OuterLayoutFlag)
    RightId.flag(OuterLayoutFlag)
    BottomId.flag(OuterLayoutFlag)
    MinWidthId.flag(OuterLayoutFlag)
    MinHeightId.flag(OuterLayoutFlag)
    MaxWidthId.flag(OuterLayoutFlag)
    MaxHeightId.flag(OuterLayoutFlag)
    AlphaId.flag(LayerFlag)
    ScaleXId.flag(LayerFlag)
    ScaleYId.flag(LayerFlag)
    TranslationXId.flag(LayerFlag)
    TranslationYId.flag(LayerFlag)
    TransformOriginXId.flag(LayerFlag)
    TransformOriginYId.flag(LayerFlag)
    RotationXId.flag(LayerFlag)
    RotationYId.flag(LayerFlag)
    RotationZId.flag(LayerFlag)
    ZIndexId.flag(LayerFlag)
    BackgroundColorId.flag(DrawFlag)
    BackgroundBrushId.flag(DrawFlag)
    ForegroundColorId.flag(DrawFlag)
    ForegroundBrushId.flag(DrawFlag)
    ClipId.flag(LayerFlag)
    ShapeId.flag(DrawFlag)
    ColorFilterId.flag(LayerFlag)
    DropShadowId.flag(DrawFlag)
    InnerShadowId.flag(DrawFlag)
    ContentColorId.flag(TextDrawFlag)
    ContentBrushId.flag(TextDrawFlag)
    FontFamilyId.flag(TextLayoutFlag or TextDrawFlag)
    TextMotionId.flag(TextLayoutFlag or TextDrawFlag)
    TextIndentId.flag(TextLayoutFlag or TextDrawFlag)
    FontSizeId.flag(TextLayoutFlag or TextDrawFlag)
    LineHeightId.flag(TextLayoutFlag or TextDrawFlag)
    LetterSpacingId.flag(TextLayoutFlag or TextDrawFlag)
    BaselineShiftId.flag(TextLayoutFlag or TextDrawFlag)
    LineBreakId.flag(TextLayoutFlag or TextDrawFlag)
    FontWeightId.flag(TextLayoutFlag or TextDrawFlag)
    FontStyleId.flag(TextLayoutFlag or TextDrawFlag)
    TextAlignId.flag(TextLayoutFlag or TextDrawFlag)
    TextDirectionId.flag(TextLayoutFlag or TextDrawFlag)
    HyphensId.flag(TextLayoutFlag or TextDrawFlag)
    FontSynthesisId.flag(TextLayoutFlag or TextDrawFlag)
    TextDecorationId.flag(TextLayoutFlag or TextDrawFlag)
}

@Suppress("UNUSED") private var flagsBuild = buildFlags()

private fun primitiveFlagsOf(flag: Int): Long {
    var flags = 0L
    for (id in 0 until PrimitivePropertyCount) {
        if (flag and propertyFlags[id] != 0) {
            flags = flags.withId(id.toByte())
        }
    }
    return flags
}

private fun objectFlagsOf(flag: Int): Int {
    var flags = 0
    for (id in FirstObjectProperty..LastObjectProperty) {
        if (flag and propertyFlags[id] != 0) {
            flags = flags.withId(id)
        }
    }
    return flags
}

// TODO: Refactor as const val
internal val InnerLayoutPrimitiveFlags = primitiveFlagsOf(InnerLayoutFlag)
internal val OuterLayoutPrimitiveFlags = primitiveFlagsOf(OuterLayoutFlag)
internal val DrawPrimitiveFlags = primitiveFlagsOf(DrawFlag)
internal val LayerPrimitiveFlags = primitiveFlagsOf(LayerFlag)
internal val TextDrawPrimitiveFlags = primitiveFlagsOf(TextDrawFlag)
internal val TextLayoutPrimitiveFlags = primitiveFlagsOf(TextLayoutFlag)

internal val InnerLayoutObjectFlags = objectFlagsOf(InnerLayoutFlag)
internal val OuterLayoutObjectFlags = objectFlagsOf(OuterLayoutFlag)
internal val DrawObjectFlags = objectFlagsOf(DrawFlag)
internal val LayerObjectFlags = objectFlagsOf(LayerFlag)
internal val TextDrawObjectFlags = objectFlagsOf(TextDrawFlag)
internal val TextLayoutObjectFlags = objectFlagsOf(TextLayoutFlag)

internal fun primitivePhaseFlagsOf(primitivesSet: Long) =
    (if (primitivesSet and InnerLayoutPrimitiveFlags != 0L) InnerLayoutFlag else 0) or
        (if (primitivesSet and OuterLayoutPrimitiveFlags != 0L) OuterLayoutFlag else 0) or
        (if (primitivesSet and DrawPrimitiveFlags != 0L) DrawFlag else 0) or
        (if (primitivesSet and LayerPrimitiveFlags != 0L) LayerFlag else 0) or
        (if (primitivesSet and TextDrawPrimitiveFlags != 0L) TextDrawFlag else 0) or
        (if (primitivesSet and TextLayoutPrimitiveFlags != 0L) TextLayoutFlag else 0)

internal fun objectPhaseFlagsOf(objectsSet: Int) =
    (if (objectsSet and InnerLayoutObjectFlags != 0) InnerLayoutFlag else 0) or
        (if (objectsSet and OuterLayoutObjectFlags != 0) OuterLayoutFlag else 0) or
        (if (objectsSet and DrawObjectFlags != 0) DrawFlag else 0) or
        (if (objectsSet and LayerObjectFlags != 0) LayerFlag else 0) or
        (if (objectsSet and TextDrawObjectFlags != 0) TextDrawFlag else 0) or
        (if (objectsSet and TextLayoutObjectFlags != 0) TextLayoutFlag else 0)

internal inline fun primitivesSetForFlags(flags: Int) =
    (if (flags and InnerLayoutFlag != 0) InnerLayoutPrimitiveFlags else 0) or
        (if (flags and OuterLayoutFlag != 0) OuterLayoutPrimitiveFlags else 0) or
        (if (flags and DrawFlag != 0) DrawPrimitiveFlags else 0) or
        (if (flags and LayerFlag != 0) LayerPrimitiveFlags else 0) or
        (if (flags and TextDrawFlag != 0) TextDrawPrimitiveFlags else 0) or
        (if (flags and TextLayoutFlag != 0) TextLayoutPrimitiveFlags else 0)

internal inline fun objectsSetForFlags(flags: Int) =
    (if (flags and InnerLayoutFlag != 0) InnerLayoutObjectFlags else 0) or
        (if (flags and OuterLayoutFlag != 0) OuterLayoutObjectFlags else 0) or
        (if (flags and DrawFlag != 0) DrawObjectFlags else 0) or
        (if (flags and LayerFlag != 0) LayerObjectFlags else 0) or
        (if (flags and TextDrawFlag != 0) TextDrawObjectFlags else 0) or
        (if (flags and TextLayoutFlag != 0) TextLayoutObjectFlags else 0)

@ExperimentalFoundationStyleApi
internal class StyleProperties {
    @JvmField internal var primitivesSet: Long = 0
    @JvmField internal var objectsSet: Int = 0

    @JvmField internal var contentPaddingStart: Float = 0f
    @JvmField internal var contentPaddingEnd: Float = 0f
    @JvmField internal var contentPaddingTop: Float = 0f
    @JvmField internal var contentPaddingBottom: Float = 0f
    @JvmField internal var externalPaddingStart: Float = 0f
    @JvmField internal var externalPaddingEnd: Float = 0f
    @JvmField internal var externalPaddingTop: Float = 0f
    @JvmField internal var externalPaddingBottom: Float = 0f
    @JvmField internal var borderWidth: Float = 0f
    @JvmField internal var width: Float = Float.NaN
    @JvmField internal var height: Float = Float.NaN
    @JvmField internal var widthFraction: Float = Float.NaN
    @JvmField internal var heightFraction: Float = Float.NaN
    @JvmField internal var left: Float = 0f
    @JvmField internal var top: Float = 0f
    @JvmField internal var right: Float = 0f
    @JvmField internal var bottom: Float = 0f
    @JvmField internal var minHeight: Float = Float.NaN
    @JvmField internal var maxHeight: Float = Float.NaN
    @JvmField internal var minWidth: Float = Float.NaN
    @JvmField internal var maxWidth: Float = Float.NaN

    // draw properties
    internal var borderColor: Color = Color.Black
    @JvmField internal var borderBrush: Brush? = null
    internal var backgroundColor: Color = Color.Transparent // white?
    @JvmField internal var backgroundBrush: Brush? = null
    internal var foregroundColor: Color = Color.Unspecified
    @JvmField internal var foregroundBrush: Brush? = null
    @JvmField internal var clip: Boolean = false
    @JvmField internal var shape: Shape = RectangleShape
    @JvmField internal var dropShadow: Any? = null
    @JvmField internal var innerShadow: Any? = null

    // layer properties
    @JvmField internal var alpha: Float = 1.0f
    @JvmField internal var scaleX: Float = 1.0f
    @JvmField internal var scaleY: Float = 1.0f
    @JvmField internal var translationX: Float = 0f
    @JvmField internal var translationY: Float = 0f
    @JvmField internal var rotationX: Float = 0f
    @JvmField internal var rotationY: Float = 0f
    @JvmField internal var rotationZ: Float = 0f
    @JvmField internal var transformOriginX: Float = TransformOrigin.Center.pivotFractionX
    @JvmField internal var transformOriginY: Float = TransformOrigin.Center.pivotFractionY
    @JvmField internal var cameraDistance: Float = 1.0f
    @JvmField internal var zIndex: Float = 0f
    @JvmField internal var colorFilter: ColorFilter? = null

    // text style, affects draw only
    internal var contentColor: Color = Color.Unspecified
    internal var contentBrush: Brush? = null

    // text style, affects text layout
    @JvmField internal var fontFamily: FontFamily? = null
    @JvmField internal var textMotion: TextMotion = TextMotion.Static
    @JvmField internal var textIndent: TextIndent? = null
    internal var fontSize: TextUnit = TextUnit.Unspecified
    internal var lineHeight: TextUnit = TextUnit.Unspecified
    internal var letterSpacing: TextUnit = TextUnit.Unspecified
    internal var baselineShift: BaselineShift = BaselineShift.Unspecified
    internal var lineBreak: LineBreak = LineBreak.Unspecified
    // This is several text related "enum" style values which we are packing
    // into a single Int for efficiency.
    @JvmField internal var textEnums: Int = 0

    internal fun clear() {
        EmptyStyleProperties.copyInto(this)
    }

    internal fun copy(): StyleProperties = StyleProperties().also { copyInto(it) }

    internal fun copyInto(target: StyleProperties) {
        target.primitivesSet = primitivesSet
        target.objectsSet = objectsSet
        target.left = left
        target.top = top
        target.right = right
        target.bottom = bottom
        target.minHeight = minHeight
        target.maxHeight = maxHeight
        target.minWidth = minWidth
        target.maxWidth = maxWidth
        target.contentPaddingStart = contentPaddingStart
        target.contentPaddingEnd = contentPaddingEnd
        target.contentPaddingTop = contentPaddingTop
        target.contentPaddingBottom = contentPaddingBottom
        target.externalPaddingStart = externalPaddingStart
        target.externalPaddingEnd = externalPaddingEnd
        target.externalPaddingTop = externalPaddingTop
        target.externalPaddingBottom = externalPaddingBottom
        target.borderWidth = borderWidth
        target.shape = shape
        target.alpha = alpha
        target.scaleX = scaleX
        target.scaleY = scaleY
        target.translationX = translationX
        target.translationY = translationY
        target.rotationX = rotationX
        target.rotationY = rotationY
        target.rotationZ = rotationZ
        target.transformOriginX = transformOriginX
        target.transformOriginY = transformOriginY
        target.zIndex = zIndex
        target.colorFilter = colorFilter
        target.cameraDistance = cameraDistance
        target.borderColor = borderColor
        target.borderBrush = borderBrush
        target.backgroundColor = backgroundColor
        target.backgroundBrush = backgroundBrush
        target.foregroundColor = foregroundColor
        target.foregroundBrush = foregroundBrush
        target.dropShadow = dropShadow
        target.innerShadow = innerShadow
        target.clip = clip
        target.width = width
        target.height = height
        target.widthFraction = widthFraction
        target.heightFraction = heightFraction
        target.contentColor = contentColor
        target.contentBrush = contentBrush
        target.fontFamily = fontFamily
        target.textMotion = textMotion
        target.textIndent = textIndent
        target.fontSize = fontSize
        target.lineHeight = lineHeight
        target.letterSpacing = letterSpacing
        target.baselineShift = baselineShift
        target.lineBreak = lineBreak
        target.textEnums = textEnums
    }

    internal fun copyInto(target: StyleProperties, flags: Int) {
        val primitivesSet = primitivesSet and primitiveFlagsOf(flags)
        val objectsSet = objectsSet and objectsSetForFlags(flags)
        if (primitivesSet != 0L) {
            if (primitivesSet.hasId(LeftId)) target.left(left)
            if (primitivesSet.hasId(TopId)) target.top(top)
            if (primitivesSet.hasId(RightId)) target.right(right)
            if (primitivesSet.hasId(BottomId)) target.bottom(bottom)
            if (primitivesSet.hasId(MinHeightId)) target.minHeight(minHeight)
            if (primitivesSet.hasId(MaxHeightId)) target.maxHeight(maxHeight)
            if (primitivesSet.hasId(MinWidthId)) target.minWidth(minWidth)
            if (primitivesSet.hasId(MaxWidthId)) target.maxWidth(maxWidth)
            if (primitivesSet.hasId(ContentPaddingStartId))
                target.contentPaddingStart(contentPaddingStart)
            if (primitivesSet.hasId(ContentPaddingEndId))
                target.contentPaddingEnd(contentPaddingEnd)
            if (primitivesSet.hasId(ContentPaddingTopId))
                target.contentPaddingTop(contentPaddingTop)
            if (primitivesSet.hasId(ContentPaddingBottomId))
                target.contentPaddingBottom(contentPaddingBottom)
            if (primitivesSet.hasId(ExternalPaddingStartId))
                target.externalPaddingStart(externalPaddingStart)
            if (primitivesSet.hasId(ExternalPaddingEndId))
                target.externalPaddingEnd(externalPaddingEnd)
            if (primitivesSet.hasId(ExternalPaddingTopId))
                target.externalPaddingTop(externalPaddingTop)
            if (primitivesSet.hasId(ExternalPaddingBottomId))
                target.externalPaddingBottom(externalPaddingBottom)
            if (primitivesSet.hasId(BorderWidthId)) target.borderWidth(borderWidth)
            if (primitivesSet.hasId(AlphaId)) target.alpha(alpha)
            if (primitivesSet.hasId(ScaleXId)) target.scaleX(scaleX)
            if (primitivesSet.hasId(ScaleYId)) target.scaleY(scaleY)
            if (primitivesSet.hasId(TranslationXId)) target.translationX(translationX)
            if (primitivesSet.hasId(TranslationYId)) target.translationY(translationY)
            if (primitivesSet.hasId(RotationXId)) target.rotationX(rotationX)
            if (primitivesSet.hasId(RotationYId)) target.rotationY(rotationY)
            if (primitivesSet.hasId(RotationZId)) target.rotationZ(rotationZ)
            if (primitivesSet.hasId(TransformOriginXId)) target.transformOriginX(transformOriginX)
            if (primitivesSet.hasId(TransformOriginYId)) target.transformOriginY(transformOriginY)
            if (primitivesSet.hasId(ZIndexId)) target.zIndex(zIndex)
            if (primitivesSet.hasId(CameraDistanceId)) target.cameraDistance(cameraDistance)
            if (primitivesSet.hasId(BorderColorId)) target.borderColor(borderColor)
            if (primitivesSet.hasId(BackgroundColorId)) target.backgroundColor(backgroundColor)
            if (primitivesSet.hasId(ForegroundColorId)) target.foregroundColor(foregroundColor)
            if (primitivesSet.hasId(ClipId)) target.clip(clip)
            if (primitivesSet.hasId(WidthId)) target.width(width)
            if (primitivesSet.hasId(HeightId)) target.height(height)
            if (primitivesSet.hasId(WidthFractionId)) target.widthFraction(widthFraction)
            if (primitivesSet.hasId(HeightFractionId)) target.heightFraction(heightFraction)
            if (primitivesSet.hasId(ContentColorId)) target.contentColor(contentColor)
            if (primitivesSet.hasId(LineHeightId)) target.lineHeight(lineHeight)
            if (primitivesSet.hasId(LetterSpacingId)) target.letterSpacing(letterSpacing)
            if (primitivesSet.hasId(BaselineShiftId)) target.baselineShift(baselineShift)
            if (primitivesSet.hasId(LineBreakId)) target.lineBreak(lineBreak)
            if (primitivesSet and TextEnumMask != 0L) {
                if (primitivesSet.hasId(TextDecorationId)) target.textDecoration(textDecoration)
                if (primitivesSet.hasId(FontSizeId)) target.fontSize(fontSize)
                if (primitivesSet.hasId(TextAlignId)) target.textAlign(textAlign)
                if (primitivesSet.hasId(TextDirectionId)) target.textDirection(textDirection)
                if (primitivesSet.hasId(HyphensId)) target.hyphens(hyphens)
                if (primitivesSet.hasId(FontSynthesisId)) target.fontSynthesis(fontSynthesis)
                if (primitivesSet.hasId(FontWeightId)) target.fontWeight(fontWeight)
                if (primitivesSet.hasId(FontStyleId)) target.fontStyle(fontStyle)
            }
        }
        if (objectsSet != 0) {
            if (objectsSet.hasId(ShapeId)) target.shape(shape)
            if (objectsSet.hasId(ColorFilterId)) target.colorFilter(colorFilter)
            if (objectsSet.hasId(BorderBrushId)) target.borderBrush(borderBrush)
            if (objectsSet.hasId(BackgroundBrushId)) target.backgroundBrush(backgroundBrush)
            if (objectsSet.hasId(ForegroundBrushId)) target.foregroundBrush(foregroundBrush)
            if (objectsSet.hasId(DropShadowId)) target.dropShadow(dropShadow)
            if (objectsSet.hasId(InnerShadowId)) target.innerShadow(innerShadow)
            if (objectsSet.hasId(ContentBrushId)) target.contentBrush(contentBrush)
            if (objectsSet.hasId(FontFamilyId)) target.fontFamily(fontFamily)
            if (objectsSet.hasId(TextMotionId)) target.textMotion(textMotion)
            if (objectsSet.hasId(TextIndentId)) target.textIndent(textIndent!!)
        }
    }

    /**
     * Given another StyleProperties instance, this function will return a bitmask of the property
     * phases should be invalidated due to the differences between the two.
     *
     * The passed in [filterFlags] parameter is a bitmask of the same format which can be used to
     * limit which categories of properties this function will actually check for differences of.
     */
    internal fun diff(other: StyleProperties, filterFlags: Int = PhaseFlagMask): Int {
        // Compute the set of primitive properties set by both styles
        val bothPrimitiveSet = primitivesSet and other.primitivesSet

        // Compute properties that are only set by this and or the other.
        val exclusivePrimitiveSet = primitivesSet xor other.primitivesSet

        // Compute the set of objects properties set by both styles
        val bothObjectsSet = objectsSet and other.objectsSet

        // Compute the properties exclusively set by this style or the other
        val exclusiveObjectsSet = objectsSet xor other.objectsSet

        // Set change to those phases which are invalidated because this or the other adds or
        // removes the setting of the value.
        var change =
            primitivePhaseFlagsOf(exclusivePrimitiveSet) or objectPhaseFlagsOf(exclusiveObjectsSet)

        // Return early if we know, without comparing values, all phases in the filter are affected.
        if (change and filterFlags == filterFlags) return change

        val primitivesToCheck =
            primitivesSet and bothPrimitiveSet and primitivesSetForFlags(filterFlags)
        val objectsToCheck = objectsSet and bothObjectsSet and objectsSetForFlags(filterFlags)

        // If we don't have anything direct comparisons to make return
        if (primitivesToCheck == 0L && objectsToCheck == 0) return change

        if (
            filterFlags and InnerLayoutFlag != 0 &&
                primitivesToCheck and InnerLayoutPrimitiveFlags != 0L
        ) {
            if (
                compareProperty(
                    primitivesToCheck,
                    ContentPaddingStartId,
                    contentPaddingStart,
                    other.contentPaddingStart,
                ) ||
                    compareProperty(
                        primitivesToCheck,
                        ContentPaddingEndId,
                        contentPaddingEnd,
                        other.contentPaddingEnd,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        ContentPaddingTopId,
                        contentPaddingTop,
                        other.contentPaddingTop,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        ContentPaddingBottomId,
                        contentPaddingBottom,
                        other.contentPaddingBottom,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        BorderWidthId,
                        borderWidth,
                        other.borderWidth,
                    )
            ) {
                change = change or InnerLayoutFlag
            }
        }

        if (
            filterFlags and OuterLayoutFlag != 0 &&
                primitivesToCheck and OuterLayoutPrimitiveFlags != 0L
        ) {
            if (
                compareProperty(primitivesToCheck, WidthId, width, other.width) ||
                    compareProperty(primitivesToCheck, HeightId, height, other.height) ||
                    compareProperty(
                        primitivesToCheck,
                        WidthFractionId,
                        widthFraction,
                        other.widthFraction,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        HeightFractionId,
                        heightFraction,
                        other.heightFraction,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        ExternalPaddingStartId,
                        externalPaddingStart,
                        other.externalPaddingStart,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        ExternalPaddingEndId,
                        externalPaddingEnd,
                        other.externalPaddingEnd,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        ExternalPaddingTopId,
                        externalPaddingTop,
                        other.externalPaddingTop,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        ExternalPaddingBottomId,
                        externalPaddingBottom,
                        other.externalPaddingBottom,
                    ) ||
                    compareProperty(primitivesToCheck, LeftId, left, other.left) ||
                    compareProperty(primitivesToCheck, TopId, top, other.top) ||
                    compareProperty(primitivesToCheck, RightId, right, other.right) ||
                    compareProperty(primitivesToCheck, BottomId, bottom, other.bottom) ||
                    compareProperty(primitivesToCheck, MinHeightId, minHeight, other.minHeight) ||
                    compareProperty(primitivesToCheck, MinWidthId, minWidth, other.minWidth) ||
                    compareProperty(primitivesToCheck, MaxHeightId, maxHeight, other.maxHeight) ||
                    compareProperty(primitivesToCheck, MaxWidthId, maxWidth, other.maxWidth)
            ) {
                change = change or OuterLayoutFlag
            }
        }

        if (filterFlags and OuterLayoutFlag != 0) {
            if (primitivesToCheck and DrawPrimitiveFlags != 0L) {
                if (
                    compareProperty(
                        primitivesToCheck,
                        BorderWidthId,
                        borderWidth,
                        other.borderWidth,
                    ) ||
                        compareProperty(
                            primitivesToCheck,
                            BorderColorId,
                            borderColor,
                            other.borderColor,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            BackgroundColorId,
                            backgroundColor,
                            other.backgroundColor,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            ForegroundColorId,
                            foregroundColor,
                            other.foregroundColor,
                        )
                ) {
                    change = change or DrawFlag
                }
            }
            if (objectsToCheck and DrawObjectFlags != 0) {
                if (
                    compareProperty(
                        objectsToCheck,
                        BorderBrushId,
                        borderBrush,
                        other.borderBrush,
                    ) ||
                        compareProperty(
                            objectsToCheck,
                            BackgroundBrushId,
                            backgroundBrush,
                            other.backgroundBrush,
                        ) ||
                        compareProperty(
                            objectsToCheck,
                            ForegroundBrushId,
                            foregroundBrush,
                            other.foregroundBrush,
                        ) ||
                        compareProperty(
                            objectsToCheck,
                            InnerShadowId,
                            innerShadow,
                            other.innerShadow,
                        ) ||
                        compareProperty(
                            objectsToCheck,
                            DropShadowId,
                            dropShadow,
                            other.dropShadow,
                        ) ||
                        compareProperty(objectsToCheck, ShapeId, shape, other.shape)
                ) {
                    change = change or DrawFlag
                }
            }
        }

        if (filterFlags and LayerFlag != 0 && primitivesToCheck and LayerPrimitiveFlags != 0L) {
            if (
                compareProperty(primitivesToCheck, AlphaId, alpha, other.alpha) ||
                    compareProperty(primitivesToCheck, ScaleXId, scaleX, other.scaleX) ||
                    compareProperty(primitivesToCheck, ScaleYId, scaleY, other.scaleY) ||
                    compareProperty(
                        primitivesToCheck,
                        TranslationXId,
                        translationX,
                        other.translationX,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        TranslationYId,
                        translationY,
                        other.translationY,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        TransformOriginXId,
                        transformOriginX,
                        other.transformOriginX,
                    ) ||
                    compareProperty(
                        primitivesToCheck,
                        TransformOriginYId,
                        transformOriginY,
                        other.transformOriginY,
                    ) ||
                    compareProperty(primitivesToCheck, RotationXId, rotationX, other.rotationX) ||
                    compareProperty(primitivesToCheck, RotationYId, rotationY, other.rotationY) ||
                    compareProperty(primitivesToCheck, RotationZId, rotationZ, other.rotationZ) ||
                    compareProperty(primitivesToCheck, ZIndexId, zIndex, other.zIndex) ||
                    compareProperty(primitivesToCheck, ClipId, clip, other.clip)
            ) {
                change = change or LayerFlag
            }
        }

        if (filterFlags and LayerFlag != 0 && objectsToCheck and LayerObjectFlags != 0) {
            if (compareProperty(objectsToCheck, ColorFilterId, colorFilter, other.colorFilter)) {
                change = change or LayerFlag
            }
        }

        // Special case for shape, it should invalidate the layer flag if either this or other
        // has layer properties set even if they are the same. The layer needs to be rebuilt for the
        // shape change even if only the shape changed.
        if (filterFlags and (LayerFlag or DrawFlag) != 0) {
            val hasLayoutProperties = hasLayerProperties() || other.hasLayerProperties()
            if (
                hasLayoutProperties && compareProperty(objectsToCheck, ShapeId, shape, other.shape)
            ) {
                change = change or LayerFlag
            }
        }

        if (filterFlags and TextDrawFlag != 0) {
            if (
                compareProperty(primitivesToCheck, ContentColorId, contentColor, other.contentColor)
            ) {
                // TODO: we could include TextDecoration here but it's part of textEnums so we would
                //  have to break it out.
                change = change or TextDrawFlag
            }

            if (compareProperty(objectsToCheck, ContentBrushId, contentBrush, other.contentBrush)) {
                change = change or TextDrawFlag
            }
        }

        if (filterFlags and (TextLayoutFlag or TextDrawFlag) != 0) {
            if (primitivesToCheck and (TextLayoutPrimitiveFlags or TextDrawPrimitiveFlags) != 0L) {
                if (
                    compareProperty(primitivesToCheck, FontSizeId, fontSize, other.fontSize) ||
                        compareProperty(
                            primitivesToCheck,
                            LineHeightId,
                            lineHeight,
                            other.lineHeight,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            LetterSpacingId,
                            letterSpacing,
                            other.letterSpacing,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            BaselineShiftId,
                            baselineShift,
                            other.baselineShift,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            LineBreakId,
                            lineBreak,
                            other.lineBreak,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            FontStyleId,
                            fontStyle,
                            other.fontStyle,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            FontWeightId,
                            fontWeight,
                            other.fontWeight,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            TextAlignId,
                            textAlign,
                            other.textAlign,
                        ) ||
                        compareProperty(
                            primitivesToCheck,
                            TextDirectionId,
                            textDirection,
                            other.textDirection,
                        ) ||
                        compareProperty(primitivesToCheck, HyphensId, hyphens, other.hyphens) ||
                        compareProperty(
                            primitivesToCheck,
                            FontSynthesisId,
                            fontSynthesis,
                            other.fontSynthesis,
                        )
                ) {
                    change = change or TextLayoutFlag or TextDrawFlag
                }
            }

            if (objectsToCheck and (TextLayoutObjectFlags or TextDrawObjectFlags) != 0) {
                if (
                    compareProperty(objectsToCheck, FontFamilyId, fontFamily, other.fontFamily) ||
                        compareProperty(
                            objectsToCheck,
                            TextMotionId,
                            textMotion,
                            other.textMotion,
                        ) ||
                        compareProperty(objectsToCheck, TextIndentId, textIndent, other.textIndent)
                ) {
                    change = change or TextLayoutFlag or TextDrawFlag
                }
            }
        }

        return change
    }

    fun hasLayerProperties(): Boolean =
        primitivesSet and LayerPrimitiveFlags != 0L || objectsSet and LayerObjectFlags != 0

    fun hasId(primitiveId: Byte) = primitivesSet.hasId(primitiveId)

    fun hasId(objectId: Int) = objectsSet.hasId(objectId)

    // Style property setters
    fun contentPaddingStart(value: Float) {
        primitivesSet = primitivesSet.withId(ContentPaddingStartId)
        contentPaddingStart = value
    }

    fun contentPaddingEnd(value: Float) {
        primitivesSet = primitivesSet.withId(ContentPaddingEndId)
        contentPaddingEnd = value
    }

    fun contentPaddingTop(value: Float) {
        primitivesSet = primitivesSet.withId(ContentPaddingTopId)
        contentPaddingTop = value
    }

    fun contentPaddingBottom(value: Float) {
        primitivesSet = primitivesSet.withId(ContentPaddingBottomId)
        contentPaddingBottom = value
    }

    // externalPadding
    fun externalPaddingStart(value: Float) {
        primitivesSet = primitivesSet.withId(ExternalPaddingStartId)
        externalPaddingStart = value
    }

    fun externalPaddingEnd(value: Float) {
        primitivesSet = primitivesSet.withId(ExternalPaddingEndId)
        externalPaddingEnd = value
    }

    fun externalPaddingTop(value: Float) {
        primitivesSet = primitivesSet.withId(ExternalPaddingTopId)
        externalPaddingTop = value
    }

    fun externalPaddingBottom(value: Float) {
        primitivesSet = primitivesSet.withId(ExternalPaddingBottomId)
        externalPaddingBottom = value
    }

    // border
    fun borderWidth(value: Float) {
        primitivesSet = primitivesSet.withId(BorderWidthId)
        borderWidth = value
    }

    fun borderColor(value: Color) {
        primitivesSet = primitivesSet.withId(BorderColorId)
        objectsSet = objectsSet.withoutId(BorderBrushId)
        borderColor = value
        borderBrush = null
    }

    fun borderBrush(value: Brush?) {
        primitivesSet = primitivesSet.withoutId(BorderColorId)
        objectsSet =
            if (value != null) objectsSet.withId(BorderBrushId)
            else objectsSet.withoutId(BorderBrushId)
        borderBrush = value
        borderColor = Color.Unspecified
    }

    // size
    fun width(value: Float) {
        primitivesSet = primitivesSet.withId(WidthId).withoutId(WidthFractionId)
        width = value
        widthFraction = Float.NaN
    }

    fun height(value: Float) {
        primitivesSet = primitivesSet.withId(HeightId).withoutId(HeightFractionId)
        height = value
        heightFraction = Float.NaN
    }

    fun widthFraction(fraction: Float) {
        primitivesSet = primitivesSet.withoutId(WidthId).withId(WidthFractionId)
        widthFraction = fraction
        width = Float.NaN
    }

    fun heightFraction(fraction: Float) {
        primitivesSet = primitivesSet.withoutId(HeightId).withId(HeightFractionId)
        heightFraction = fraction
        height = Float.NaN
    }

    fun left(value: Float) {
        primitivesSet = primitivesSet.withId(LeftId)
        left = value
    }

    fun top(value: Float) {
        primitivesSet = primitivesSet.withId(TopId)
        top = value
    }

    fun right(value: Float) {
        primitivesSet = primitivesSet.withId(RightId)
        right = value
    }

    fun bottom(value: Float) {
        primitivesSet = primitivesSet.withId(BottomId)
        bottom = value
    }

    fun minWidth(value: Float) {
        primitivesSet = primitivesSet.withId(MinWidthId)
        minWidth = value
    }

    fun minHeight(value: Float) {
        primitivesSet = primitivesSet.withId(MinHeightId)
        minHeight = value
    }

    fun maxWidth(value: Float) {
        primitivesSet = primitivesSet.withId(MaxWidthId)
        maxWidth = value
    }

    fun maxHeight(value: Float) {
        primitivesSet = primitivesSet.withId(MaxHeightId)
        maxHeight = value
    }

    // layer properties
    fun alpha(value: Float) {
        primitivesSet = primitivesSet.withId(AlphaId)
        alpha = value
    }

    fun scaleX(value: Float) {
        primitivesSet = primitivesSet.withId(ScaleXId)
        scaleX = value
    }

    fun scaleY(value: Float) {
        primitivesSet = primitivesSet.withId(ScaleYId)
        scaleY = value
    }

    // TODO: dp-based translation
    fun translationX(value: Float) {
        primitivesSet = primitivesSet.withId(TranslationXId)
        translationX = value
    }

    fun translationY(value: Float) {
        primitivesSet = primitivesSet.withId(TranslationYId)
        translationY = value
    }

    fun translation(x: Float, y: Float) {
        translationX(x)
        translationY(y)
    }

    fun translation(offset: Offset) {
        translationX(offset.x)
        translationY(offset.y)
    }

    fun rotationX(value: Float) {
        primitivesSet = primitivesSet.withId(RotationXId)
        rotationX = value
    }

    fun rotationY(value: Float) {
        primitivesSet = primitivesSet.withId(RotationYId)
        rotationY = value
    }

    fun rotationZ(value: Float) {
        primitivesSet = primitivesSet.withId(RotationZId)
        rotationZ = value
    }

    fun transformOriginX(value: Float) {
        primitivesSet = primitivesSet.withId(TransformOriginXId)
        transformOriginX = value
    }

    fun transformOriginY(value: Float) {
        primitivesSet = primitivesSet.withId(TransformOriginYId)
        transformOriginY = value
    }

    fun clip(value: Boolean) {
        primitivesSet = primitivesSet.withId(ClipId)
        clip = value
    }

    fun zIndex(value: Float) {
        primitivesSet = primitivesSet.withId(ZIndexId)
        zIndex = value
    }

    fun colorFilter(value: ColorFilter?) {
        objectsSet = objectsSet.withId(ColorFilterId)
        colorFilter = value
    }

    fun cameraDistance(value: Float) {
        primitivesSet = primitivesSet.withId(CameraDistanceId)
        cameraDistance = value
    }

    //    var renderEffect: RenderEffect? = null
    // TODO: need a TransformOrigin.Unspecified
    //    var transformOrigin: TransformOrigin = Float.NaN

    // draw properties
    fun backgroundColor(color: Color) {
        primitivesSet = primitivesSet.withId(BackgroundColorId)
        objectsSet = objectsSet.withoutId(BackgroundBrushId)
        backgroundColor = color
        backgroundBrush = null
    }

    fun backgroundBrush(value: Brush?) {
        primitivesSet = primitivesSet.withoutId(BackgroundColorId)
        objectsSet =
            if (value != null) objectsSet.withId(BackgroundBrushId)
            else objectsSet.withoutId(BackgroundBrushId)
        backgroundBrush = value
        backgroundColor = Color.Unspecified
    }

    fun foregroundColor(value: Color) {
        primitivesSet = primitivesSet.withId(ForegroundColorId)
        objectsSet = objectsSet.withoutId(ForegroundBrushId)
        foregroundColor = value
        foregroundBrush = null
    }

    fun foregroundBrush(value: Brush?) {
        primitivesSet = primitivesSet.withoutId(ForegroundColorId)
        objectsSet =
            if (value != null) objectsSet.withId(ForegroundBrushId)
            else objectsSet.withoutId(ForegroundBrushId)
        foregroundBrush = value
        foregroundColor = Color.Unspecified
    }

    // TODO: consider borderRadius?
    fun shape(value: Shape) {
        objectsSet = objectsSet.withId(ShapeId)
        shape = value
    }

    fun dropShadow(value: Any?) {
        objectsSet =
            if (value != null) objectsSet.withId(DropShadowId)
            else objectsSet.withoutId(DropShadowId)
        dropShadow = value
    }

    fun innerShadow(value: Any?) {
        objectsSet =
            if (value != null) objectsSet.withId(InnerShadowId)
            else objectsSet.withoutId(InnerShadowId)
        innerShadow = value
    }

    fun contentColor(value: Color) {
        primitivesSet = primitivesSet.withId(ContentColorId)
        objectsSet = objectsSet.withoutId(ContentBrushId)
        contentColor = value
        contentBrush = null
    }

    fun contentBrush(value: Brush?) {
        primitivesSet = primitivesSet.withoutId(ContentColorId)
        objectsSet =
            if (value != null) objectsSet.withId(ContentBrushId)
            else objectsSet.withoutId(ContentBrushId)
        contentBrush = value
        contentColor = Color.Unspecified
    }

    fun textDecoration(value: TextDecoration) {
        primitivesSet = primitivesSet.withId(TextDecorationId)
        val bits = 0b100 or value.mask
        textEnums = textEnums.setBits(TextDecorationMask, TextDecorationShift, bits)
    }

    fun fontFamily(value: FontFamily?) {
        objectsSet = objectsSet.withId(FontFamilyId)
        fontFamily = value
    }

    fun textMotion(value: TextMotion) {
        objectsSet = objectsSet.withId(TextMotionId)
        textMotion = value
    }

    fun textIndent(value: TextIndent) {
        objectsSet = objectsSet.withId(TextIndentId)
        textIndent = value
    }

    fun fontSize(value: TextUnit) {
        primitivesSet = primitivesSet.withId(FontSizeId)
        fontSize = value
    }

    fun lineHeight(value: TextUnit) {
        primitivesSet = primitivesSet.withId(LineHeightId)
        lineHeight = value
    }

    fun letterSpacing(value: TextUnit) {
        primitivesSet = primitivesSet.withId(LetterSpacingId)
        letterSpacing = value
    }

    fun baselineShift(value: BaselineShift) {
        primitivesSet = primitivesSet.withId(BaselineShiftId)
        baselineShift = value
    }

    fun lineBreak(value: LineBreak) {
        primitivesSet = primitivesSet.withId(LineBreakId)
        lineBreak = value
    }

    fun fontStyle(value: FontStyle) {
        // TODO: do we need FontStyle.Unspecified?
        primitivesSet = primitivesSet.withId(FontStyleId)
        textEnums = textEnums.setBits(FontStyleMask, FontStyleShift, 0b10 or value.value)
    }

    fun textAlign(value: TextAlign) {
        primitivesSet = primitivesSet.withId(TextAlignId)
        textEnums = textEnums.setBits(TextAlignMask, TextAlignShift, value.value)
    }

    fun textDirection(value: TextDirection) {
        primitivesSet = primitivesSet.withId(TextDirectionId)
        textEnums = textEnums.setBits(TextDirectionMask, TextDirectionShift, value.value)
    }

    fun hyphens(value: Hyphens) {
        primitivesSet = primitivesSet.withId(HyphensId)
        textEnums = textEnums.setBits(HyphensMask, HyphensShift, value.value)
    }

    fun fontWeight(value: FontWeight) {
        primitivesSet = primitivesSet.withId(FontWeightId)
        // this is between [0,1000], so fits into 10 bits. 0 is "unspecified"
        textEnums = textEnums.setBits(FontWeightMask, FontWeightShift, value.weight)
    }

    // TODO: should this just be part of font family definition?
    fun fontSynthesis(value: FontSynthesis) {
        primitivesSet = primitivesSet.withId(FontSynthesisId)
        textEnums = textEnums.setBits(FontSynthesisMask, FontSynthesisShift, value.value)
    }

    internal val phaseFlags: Int
        get() = primitivePhaseFlagsOf(primitivesSet) or objectPhaseFlagsOf(objectsSet)

    internal val fontStyle: FontStyle
        get() =
            if (
                primitivesSet.hasId(FontStyleId) &&
                    textEnums.getBits(FontStyleMask, FontStyleShift) and 0b1 == 1
            )
                FontStyle.Italic
            else FontStyle.Normal

    internal val textAlign: TextAlign
        get() =
            if (primitivesSet.hasId(TextAlignId))
                TextAlign.valueOf(textEnums.getBits(TextAlignMask, TextAlignShift))
            else TextAlign.Unspecified

    internal val textDirection: TextDirection
        get() =
            if (primitivesSet.hasId(TextDirectionId))
                TextDirection.valueOf(textEnums.getBits(TextDirectionMask, TextDirectionShift))
            else TextDirection.Unspecified

    internal val hyphens: Hyphens
        get() =
            if (primitivesSet.hasId(HyphensId))
                Hyphens.valueOf(textEnums.getBits(HyphensMask, HyphensShift))
            else Hyphens.Unspecified

    internal val fontWeight: FontWeight
        get() =
            if (primitivesSet.hasId(FontWeightId))
                FontWeight(textEnums.getBits(FontWeightMask, FontWeightShift))
            else FontWeight.Normal

    internal val fontSynthesis: FontSynthesis
        get() =
            if (primitivesSet.hasId(FontSynthesisId))
                FontSynthesis.valueOf(
                    textEnums.getBits(FontSynthesisMask, FontSynthesisShift) and 0b1111
                )
            else FontSynthesis.None

    internal val textDecoration: TextDecoration
        get() =
            if (primitivesSet.hasId(TextDecorationId))
                TextDecoration.valueOf(
                    textEnums.getBits(TextDecorationMask, TextDecorationShift) and 0b11
                )
            else TextDecoration.None

    fun valueElements(): List<ValueElement> =
        mutableListOf<ValueElement>().apply {
            val primitivesSet = primitivesSet
            val objectsSet = objectsSet
            fun add(name: String, value: Any?) = add(ValueElement(name, value))
            if (primitivesSet.hasId(ContentPaddingStartId))
                add("contentPaddingStart", contentPaddingStart)
            if (primitivesSet.hasId(ContentPaddingEndId))
                add("contentPaddingEnd", contentPaddingEnd)
            if (primitivesSet.hasId(ContentPaddingTopId))
                add("contentPaddingTop", contentPaddingTop)
            if (primitivesSet.hasId(ContentPaddingBottomId))
                add("contentPaddingBottom", contentPaddingBottom)
            if (primitivesSet.hasId(ExternalPaddingStartId))
                add("externalPaddingStart", externalPaddingStart)
            if (primitivesSet.hasId(ExternalPaddingEndId))
                add("externalPaddingEnd", externalPaddingEnd)
            if (primitivesSet.hasId(ExternalPaddingTopId))
                add("externalPaddingTop", externalPaddingTop)
            if (primitivesSet.hasId(ExternalPaddingBottomId))
                add("externalPaddingBottom", externalPaddingBottom)
            if (primitivesSet.hasId(BorderWidthId)) add("borderWidth", borderWidth)
            if (primitivesSet.hasId(WidthId)) add("width", width)
            if (primitivesSet.hasId(HeightId)) add("height", height)
            if (primitivesSet.hasId(WidthFractionId)) add("widthFraction", widthFraction)
            if (primitivesSet.hasId(HeightFractionId)) add("heightFraction", heightFraction)
            if (primitivesSet.hasId(AlphaId)) add("alpha", alpha)
            if (primitivesSet.hasId(ScaleXId)) add("scaleX", scaleX)
            if (primitivesSet.hasId(ScaleYId)) add("scaleY", scaleY)
            if (primitivesSet.hasId(TranslationXId)) add("translationX", translationX)
            if (primitivesSet.hasId(TranslationYId)) add("translationY", translationY)
            if (primitivesSet.hasId(RotationXId)) add("rotationX", rotationX)
            if (primitivesSet.hasId(RotationYId)) add("rotationY", rotationY)
            if (primitivesSet.hasId(RotationZId)) add("rotationZ", rotationZ)
            if (primitivesSet.hasId(TransformOriginXId)) add("transformOriginX", transformOriginX)
            if (primitivesSet.hasId(TransformOriginYId)) add("transformOriginY", transformOriginY)
            if (primitivesSet.hasId(ZIndexId)) add("zIndex", zIndex)
            if (objectsSet.hasId(ColorFilterId)) add("colorFilter", colorFilter)
            if (primitivesSet.hasId(CameraDistanceId)) add("cameraDistance", cameraDistance)
            if (primitivesSet.hasId(BorderColorId)) add("borderColor", borderColor)
            if (objectsSet.hasId(BorderBrushId)) add("borderBrush", borderBrush)
            if (primitivesSet.hasId(BackgroundColorId)) add("backgroundColor", backgroundColor)
            if (objectsSet.hasId(BackgroundBrushId)) add("backgroundBrush", backgroundBrush)
            if (objectsSet.hasId(ForegroundBrushId)) add("foregroundBrush", foregroundBrush)
            if (primitivesSet.hasId(ClipId)) add("clip", clip)
            if (objectsSet.hasId(ShapeId)) add("shape", shape)
            if (primitivesSet.hasId(ContentColorId)) add("contentColor", contentColor)
            if (objectsSet.hasId(ContentBrushId)) add("contentBrush", contentBrush)
            if (objectsSet.hasId(FontFamilyId)) add("fontFamily", fontFamily)
            if (objectsSet.hasId(TextMotionId)) add("textMotion", textMotion)
            if (objectsSet.hasId(TextIndentId)) add("textIndent", textIndent)
            if (primitivesSet.hasId(FontSizeId)) add("fontSize", fontSize)
            if (primitivesSet.hasId(LineHeightId)) add("lineHeight", lineHeight)
            if (primitivesSet.hasId(LetterSpacingId)) add("letterSpacing", letterSpacing)
            if (primitivesSet.hasId(BaselineShiftId)) add("baselineShift", baselineShift)
            if (primitivesSet.hasId(LineBreakId)) add("lineBreak", lineBreak)
            if (primitivesSet.hasId(TextAlignId)) add("textAlign", textAlign)
            if (primitivesSet.hasId(TextDirectionId)) add("textDirection", textDirection)
            if (primitivesSet.hasId(HyphensId)) add("hyphens", hyphens)
            if (primitivesSet.hasId(FontStyleId)) add("fontStyle", fontStyle)
            if (primitivesSet.hasId(FontWeightId)) add("fontWeight", fontWeight)
            if (primitivesSet.hasId(FontSynthesisId)) add("fontSynthesis", fontSynthesis)
            if (primitivesSet.hasId(TextDecorationId)) add("textDecoration", textDecoration)
        }
}

@ExperimentalFoundationStyleApi
internal fun lerpOuterLayout(
    primitivesSet: Long,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    with(result) {
        if (primitivesSet.hasId(ExternalPaddingStartId)) {
            val t = animations.timeOf(ExternalPaddingStartId)
            externalPaddingStart(lerpMaybeNan(a.externalPaddingStart, b.externalPaddingStart, t))
        }
        if (primitivesSet.hasId(ExternalPaddingEndId)) {
            val t = animations.timeOf(ExternalPaddingEndId)
            externalPaddingEnd(lerpMaybeNan(a.externalPaddingEnd, b.externalPaddingEnd, t))
        }
        if (primitivesSet.hasId(ExternalPaddingTopId)) {
            val t = animations.timeOf(ExternalPaddingTopId)
            externalPaddingTop(lerpMaybeNan(a.externalPaddingTop, b.externalPaddingTop, t))
        }
        if (primitivesSet.hasId(ExternalPaddingBottomId)) {
            val t = animations.timeOf(ExternalPaddingBottomId)
            externalPaddingBottom(lerpMaybeNan(a.externalPaddingBottom, b.externalPaddingBottom, t))
        }
        if (primitivesSet.hasId(LeftId)) {
            val t = animations.timeOf(LeftId)
            left(lerpMaybeNan(a.left, b.left, t))
        }
        if (primitivesSet.hasId(TopId)) {
            val t = animations.timeOf(TopId)
            top(lerpMaybeNan(a.top, b.top, t))
        }
        if (primitivesSet.hasId(RightId)) {
            val t = animations.timeOf(RightId)
            right(lerpMaybeNan(a.right, b.right, t))
        }
        if (primitivesSet.hasId(BottomId)) {
            val t = animations.timeOf(BottomId)
            bottom(lerpMaybeNan(a.bottom, b.bottom, t))
        }
        if (primitivesSet.hasId(WidthId)) {
            val t = animations.timeOf(WidthId)
            width(lerpMaybeNan(a.width, b.width, t))
        }
        if (primitivesSet.hasId(HeightId)) {
            val t = animations.timeOf(HeightId)
            height(lerpMaybeNan(a.height, b.height, t))
        }
        if (primitivesSet.hasId(WidthFractionId)) {
            val t = animations.timeOf(WidthFractionId)
            widthFraction(lerpMaybeNan(a.widthFraction, b.widthFraction, t))
        }
        if (primitivesSet.hasId(HeightFractionId)) {
            val t = animations.timeOf(HeightFractionId)
            heightFraction(lerpMaybeNan(a.heightFraction, b.heightFraction, t))
        }
        if (primitivesSet.hasId(MinWidthId)) {
            val t = animations.timeOf(MinWidthId)
            minWidth(lerpMaybeNan(a.minWidth, b.minWidth, t))
        }
        if (primitivesSet.hasId(MaxWidthId)) {
            val t = animations.timeOf(MaxWidthId)
            maxWidth(lerpMaybeNan(a.maxWidth, b.maxWidth, t))
        }
        if (primitivesSet.hasId(MinHeightId)) {
            val t = animations.timeOf(MinHeightId)
            minHeight(lerpMaybeNan(a.minHeight, b.minHeight, t))
        }
        if (primitivesSet.hasId(MaxHeightId)) {
            val t = animations.timeOf(MaxHeightId)
            maxHeight(lerpMaybeNan(a.maxHeight, b.maxHeight, t))
        }
    }
}

@ExperimentalFoundationStyleApi
internal fun lerpInnerLayout(
    primitivesSet: Long,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    with(result) {
        if (primitivesSet.hasId(ContentPaddingStartId)) {
            val t = animations.timeOf(ContentPaddingStartId)
            contentPaddingStart(lerp(a.contentPaddingStart, b.contentPaddingStart, t))
        }
        if (primitivesSet.hasId(ContentPaddingEndId)) {
            val t = animations.timeOf(ContentPaddingEndId)
            contentPaddingEnd(lerp(a.contentPaddingEnd, b.contentPaddingEnd, t))
        }
        if (primitivesSet.hasId(ContentPaddingTopId)) {
            val t = animations.timeOf(ContentPaddingTopId)
            contentPaddingTop(lerp(a.contentPaddingTop, b.contentPaddingTop, t))
        }
        if (primitivesSet.hasId(ContentPaddingBottomId)) {
            val t = animations.timeOf(ContentPaddingBottomId)
            contentPaddingBottom(lerp(a.contentPaddingBottom, b.contentPaddingBottom, t))
        }
    }
}

@ExperimentalFoundationStyleApi
internal fun lerpDraw(
    primitivesSet: Long,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    with(result) {
        if (primitivesSet.hasId(BorderWidthId)) {
            val t = animations.timeOf(BorderWidthId)
            borderWidth(lerp(a.borderWidth, b.borderWidth, t))
        }
        if (primitivesSet.hasId(BorderColorId)) {
            val t = animations.timeOf(BorderColorId)
            borderColor(androidx.compose.ui.graphics.lerp(a.borderColor, b.borderColor, t))
        }
        if (primitivesSet.hasId(BackgroundColorId)) {
            val t = animations.timeOf(BackgroundColorId)
            backgroundColor(
                androidx.compose.ui.graphics.lerp(a.backgroundColor, b.backgroundColor, t)
            )
        }
        if (primitivesSet.hasId(ForegroundColorId)) {
            val t = animations.timeOf(ForegroundColorId)
            foregroundColor(
                androidx.compose.ui.graphics.lerp(a.foregroundColor, b.foregroundColor, t)
            )
        }
    }
}

@ExperimentalFoundationStyleApi
internal fun lerpDraw(
    objectsSet: Int,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    // TODO: optimize this function more
    with(result) {
        if (objectsSet.hasId(BorderBrushId)) {
            val t = animations.timeOf(BorderBrushId)
            borderBrush(lerp(a.borderBrush, a.borderColor, b.borderBrush, b.borderColor, t))
        }
        if (objectsSet.hasId(BackgroundBrushId)) {
            val t = animations.timeOf(BackgroundBrushId)
            backgroundBrush(
                lerp(a.backgroundBrush, a.backgroundColor, b.backgroundBrush, b.backgroundColor, t)
            )
        }
        if (objectsSet.hasId(ForegroundBrushId)) {
            val t = animations.timeOf(ForegroundBrushId)
            foregroundBrush(
                lerp(a.foregroundBrush, a.foregroundColor, b.foregroundBrush, b.foregroundColor, t)
            )
        }
        if (objectsSet.hasId(InnerShadowId)) {
            val t = animations.timeOf(InnerShadowId)
            innerShadow(lerpShadows(a.innerShadow, b.innerShadow, t))
        }
        if (objectsSet.hasId(DropShadowId)) {
            val t = animations.timeOf(DropShadowId)
            dropShadow(lerpShadows(a.dropShadow, b.dropShadow, t))
        }
        if (objectsSet.hasId(ShapeId)) {
            val t = animations.timeOf(ShapeId)
            shape(lerp(a.shape, b.shape, t))
        }
    }
}

@ExperimentalFoundationStyleApi
internal fun lerpLayer(
    primitivesSet: Long,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    // TODO: optimize this function more
    with(result) {
        if (primitivesSet.hasId(AlphaId)) {
            val t = animations.timeOf(AlphaId)
            alpha(lerp(a.alpha, b.alpha, t))
        }
        if (primitivesSet.hasId(ScaleXId)) {
            val t = animations.timeOf(ScaleXId)
            scaleX(lerp(a.scaleX, b.scaleX, t))
        }
        if (primitivesSet.hasId(ScaleYId)) {
            val t = animations.timeOf(ScaleYId)
            scaleY(lerp(a.scaleY, b.scaleY, t))
        }
        if (primitivesSet.hasId(TranslationXId)) {
            val t = animations.timeOf(TranslationXId)
            translationX(lerp(a.translationX, b.translationX, t))
        }
        if (primitivesSet.hasId(TranslationYId)) {
            val t = animations.timeOf(TranslationYId)
            translationY(lerp(a.translationY, b.translationY, t))
        }
        if (primitivesSet.hasId(RotationXId)) {
            val t = animations.timeOf(RotationXId)
            rotationX(lerp(a.rotationX, b.rotationX, t))
        }
        if (primitivesSet.hasId(RotationYId)) {
            val t = animations.timeOf(RotationYId)
            rotationY(lerp(a.rotationY, b.rotationY, t))
        }
        if (primitivesSet.hasId(RotationZId)) {
            val t = animations.timeOf(RotationZId)
            rotationZ(lerp(a.rotationZ, b.rotationZ, t))
        }
        if (primitivesSet.hasId(TransformOriginXId)) {
            val t = animations.timeOf(TransformOriginXId)
            translationX(lerp(a.transformOriginX, b.transformOriginX, t))
        }
        if (primitivesSet.hasId(TransformOriginYId)) {
            val t = animations.timeOf(TransformOriginYId)
            translationY(lerp(a.transformOriginY, b.transformOriginY, t))
        }
        if (primitivesSet.hasId(ZIndexId)) {
            val t = animations.timeOf(ZIndexId)
            zIndex(lerp(a.zIndex, b.zIndex, t))
        }
        if (primitivesSet.hasId(ClipId)) {
            val t = animations.timeOf(ClipId)
            clip(if (t < 0.5f) a.clip else b.clip)
        }
    }
}

@ExperimentalFoundationStyleApi
internal fun lerpLayer(
    objectsSet: Int,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    if (objectsSet.hasId(ColorFilterId)) {
        val t = animations.timeOf(ColorFilterId)
        result.colorFilter(lerp(a.colorFilter, b.colorFilter, t))
    }
}

@ExperimentalFoundationStyleApi
internal fun lerpTextLayout(
    objectsSet: Int,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    with(result) {
        animateAB(a, b, FontFamilyId, objectsSet, animations, { fontFamily }, { fontFamily(it) })
        animateAB(a, b, TextMotionId, objectsSet, animations, { textMotion }, { textMotion(it) })
        animateAB(a, b, TextIndentId, objectsSet, animations, { textIndent!! }, { textIndent(it) })
    }
}

@ExperimentalFoundationStyleApi
internal fun lerpTextLayout(
    primitivesSet: Long,
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    result: StyleProperties,
) {
    // TODO: optimize this function more
    with(result) {
        animateAB(
            a,
            b,
            TextDecorationId,
            primitivesSet,
            animations,
            { textDecoration },
            { textDecoration(it) },
        )
        animateAB(a, b, FontSizeId, primitivesSet, animations, { fontSize }, { fontSize(it) })
        animateAB(a, b, LineHeightId, primitivesSet, animations, { lineHeight }, { lineHeight(it) })
        animateAB(
            a,
            b,
            LetterSpacingId,
            primitivesSet,
            animations,
            { letterSpacing },
            { letterSpacing(it) },
        )
        animateAB(
            a,
            b,
            BaselineShiftId,
            primitivesSet,
            animations,
            { baselineShift },
            { baselineShift(it) },
        )
        animateAB(a, b, LineBreakId, primitivesSet, animations, { lineBreak }, { lineBreak(it) })
        animateAB(a, b, TextAlignId, primitivesSet, animations, { textAlign }, { textAlign(it) })
        animateAB(
            a,
            b,
            TextDirectionId,
            primitivesSet,
            animations,
            { textDirection },
            { textDirection(it) },
        )
        animateAB(a, b, HyphensId, primitivesSet, animations, { hyphens }, { hyphens(it) })
        animateAB(
            a,
            b,
            FontSynthesisId,
            primitivesSet,
            animations,
            { fontSynthesis },
            { fontSynthesis(it) },
        )
        animateAB(a, b, FontWeightId, primitivesSet, animations, { fontWeight }, { fontWeight(it) })
        animateAB(a, b, FontStyleId, primitivesSet, animations, { fontStyle }, { fontStyle(it) })
    }
}

@ExperimentalFoundationStyleApi
private inline fun <T> StyleProperties.animateAB(
    a: StyleProperties,
    b: StyleProperties,
    propertyId: Byte,
    primitivesSet: Long,
    animations: StyleAnimations,
    get: StyleProperties.() -> T,
    set: StyleProperties.(T) -> Unit,
) {
    if (primitivesSet.hasId(propertyId)) {
        if (a.primitivesSet.hasId(propertyId)) {
            if (b.primitivesSet.hasId(propertyId)) {
                val t = animations.timeOf(propertyId)
                set(if (t < 0.5) a.get() else b.get())
            } else {
                set(a.get())
            }
        } else {
            set(b.get())
        }
    }
}

@ExperimentalFoundationStyleApi
private inline fun <T> StyleProperties.animateAB(
    a: StyleProperties,
    b: StyleProperties,
    propertyId: Int,
    objectsSet: Int,
    animations: StyleAnimations,
    get: StyleProperties.() -> T,
    set: StyleProperties.(T) -> Unit,
) {
    if (objectsSet.hasId(propertyId)) {
        if (a.objectsSet.hasId(propertyId)) {
            if (b.objectsSet.hasId(propertyId)) {
                val t = animations.timeOf(propertyId)
                set(if (t < 0.5) get(a) else get(b))
            } else {
                set(get(a))
            }
        } else {
            set(get(b))
        }
    }
}

internal fun removeColorForBrushProperties(primitivesSet: Long, objectsSet: Int): Long {
    var result = primitivesSet
    if (result.hasId(BorderColorId) && objectsSet.hasId(BorderBrushId))
        result = result.withoutId(BorderColorId)
    if (result.hasId(ContentColorId) && objectsSet.hasId(ContentBrushId))
        result = result.withoutId(ContentColorId)
    if (result.hasId(BackgroundColorId) && objectsSet.hasId(BackgroundBrushId))
        result = result.withoutId(BackgroundColorId)
    if (result.hasId(ForegroundColorId) && objectsSet.hasId(ForegroundBrushId))
        result = result.withoutId(ForegroundColorId)
    return result
}

@ExperimentalFoundationStyleApi
internal fun lerp(
    a: StyleProperties,
    b: StyleProperties,
    animations: StyleAnimations,
    primitivesSet: Long,
    objectsSet: Int,
    result: StyleProperties,
) {
    val effectiveObjectsSet = (a.objectsSet or b.objectsSet) and objectsSet
    val effectivePrimitivesSet =
        removeColorForBrushProperties(
            (a.primitivesSet or b.primitivesSet) and primitivesSet,
            effectiveObjectsSet,
        )
    if (effectivePrimitivesSet == 0L && effectiveObjectsSet == 0) {
        // Nothing to do
        return
    }

    // Animate the properties.
    if (effectivePrimitivesSet and OuterLayoutPrimitiveFlags != 0L) {
        lerpOuterLayout(effectivePrimitivesSet, a, b, animations, result)
    }
    if (effectivePrimitivesSet and InnerLayoutPrimitiveFlags != 0L) {
        lerpInnerLayout(effectivePrimitivesSet, a, b, animations, result)
    }
    if (effectivePrimitivesSet and DrawPrimitiveFlags != 0L) {
        lerpDraw(effectivePrimitivesSet, a, b, animations, result)
    }
    if (effectiveObjectsSet and DrawObjectFlags != 0) {
        lerpDraw(effectiveObjectsSet, a, b, animations, result)
    }
    if (effectivePrimitivesSet and LayerPrimitiveFlags != 0L) {
        lerpLayer(effectivePrimitivesSet, a, b, animations, result)
    }
    if (effectiveObjectsSet and LayerObjectFlags != 0) {
        lerpLayer(effectiveObjectsSet, a, b, animations, result)
    }
    if (effectivePrimitivesSet.hasId(ContentColorId)) {
        val t = animations.timeOf(ContentColorId)
        result.contentColor(androidx.compose.ui.graphics.lerp(a.contentColor, b.contentColor, t))
    }
    if (effectiveObjectsSet.hasId(ContentBrushId)) {
        val t = animations.timeOf(ContentBrushId)
        result.contentBrush(lerp(a.contentBrush, a.contentColor, b.contentBrush, b.contentColor, t))
    }
    if (effectivePrimitivesSet and TextLayoutPrimitiveFlags != 0L) {
        lerpTextLayout(effectivePrimitivesSet, a, b, animations, result)
    }
    if (effectiveObjectsSet and TextLayoutObjectFlags != 0) {
        lerpTextLayout(effectiveObjectsSet, a, b, animations, result)
    }
}

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val EmptyStyleProperties = StyleProperties()

private inline fun lerpMaybeNan(a: Float, b: Float, t: Float): Float {
    val aNan = a.isNaN()
    val bNan = b.isNaN()
    val next = (1 - t) * a + t * b
    return if (aNan) b else if (bNan) a else next
}

private fun lerpShadows(a: Any?, b: Any?, t: Float): Any? {
    if (a == null && b == null) {
        return null
    }

    val aIsArray = a is Array<*>
    val bIsArray = b is Array<*>

    if (!aIsArray && !bIsArray) {
        return lerp(a as? Shadow, b as? Shadow, t)
    }

    @Suppress("UNCHECKED_CAST")
    return lerpArrayShadows(
        if (aIsArray) a as Array<Shadow> else arrayOf(a as Shadow),
        if (bIsArray) b as Array<Shadow> else arrayOf(b as Shadow),
        t,
    )
}

private fun lerpArrayShadows(a: Array<Shadow>, b: Array<Shadow>, t: Float): Array<Shadow?> {
    val maxSize = maxOf(a.size, b.size)
    val result = Array<Shadow?>(maxSize) { null }
    for (i in 0 until maxSize) {
        val left = a.getOrNull(i)
        val right = b.getOrNull(i)
        result[i] = lerp(left, right, t)
    }
    return result
}

private fun lerp(
    leftBrush: Brush?,
    leftColor: Color,
    rightBrush: Brush?,
    rightColor: Color,
    t: Float,
): Brush? {
    var a: Brush? = leftBrush
    var b: Brush? = rightBrush
    if (leftBrush == null && rightBrush == null) {
        return null
    } else if (leftBrush == null) {
        a = SolidColor(leftColor)
    } else if (rightBrush == null) {
        b = SolidColor(rightColor)
    }
    return Interpolatable.lerp(a, b, t) as? Brush ?: if (t < 0.5) a else b
}

private fun lerp(a: Shape, b: Shape, t: Float): Shape =
    when (t) {
        0f -> a
        1f -> b
        else -> Interpolatable.lerp(a, b, t) as? Shape ?: if (t < 0.5) a else b
    }

private fun lerp(a: ColorFilter?, b: ColorFilter?, fraction: Float): ColorFilter? {
    if (a is BlendModeColorFilter && b is BlendModeColorFilter) {
        return lerp(a, b, fraction)
    }
    if (a is LightingColorFilter && b is LightingColorFilter) {
        return lerp(a, b, fraction)
    }

    return if (fraction <= 0.5f) a else b
}

private fun lerp(
    start: BlendModeColorFilter,
    stop: BlendModeColorFilter,
    fraction: Float,
): ColorFilter {
    val mode = if (fraction <= 0.5f) start.blendMode else stop.blendMode
    return ColorFilter.tint(
        color = androidx.compose.ui.graphics.lerp(start.color, stop.color, fraction),
        blendMode = mode,
    )
}

private fun lerp(
    start: LightingColorFilter,
    stop: LightingColorFilter,
    fraction: Float,
): ColorFilter {
    return ColorFilter.lighting(
        multiply = androidx.compose.ui.graphics.lerp(start.multiply, stop.multiply, fraction),
        add = androidx.compose.ui.graphics.lerp(start.add, stop.add, fraction),
    )
}

private inline fun Int.getBits(mask: Int, shift: Int): Int {
    return (this and mask) shr shift
}

private inline fun Int.setBits(mask: Int, shift: Int, value: Int): Int {
    return (this and mask.inv()) or ((value shl shift) and mask)
}

private inline fun Byte.shiftOffset() = this.toInt()

private inline fun Int.shiftOffset() = this - FirstObjectProperty

private inline fun Long.hasId(primitiveId: Byte) = this and (1L shl primitiveId.shiftOffset()) != 0L

private inline fun Int.hasId(objectId: Int) = this and (1 shl objectId.shiftOffset()) != 0

internal inline fun Long.withId(primitiveId: Byte) = this or (1L shl primitiveId.shiftOffset())

private inline fun Long.withoutId(primitiveId: Byte) =
    this and (1L shl primitiveId.shiftOffset()).inv()

internal inline fun Int.withId(objectId: Int) = this or (1 shl objectId.shiftOffset())

private inline fun Int.withoutId(objectId: Int) = this and (1 shl objectId.shiftOffset()).inv()

private inline fun compareProperty(primitivesSet: Long, id: Byte, a: Float, b: Float): Boolean =
    primitivesSet.hasId(id) && a.toRawBits() != b.toRawBits()

private inline fun compareProperty(primitivesSet: Long, id: Byte, a: Color, b: Color): Boolean =
    primitivesSet.hasId(id) && a != b

private inline fun compareProperty(primitivesSet: Long, id: Byte, a: Boolean, b: Boolean): Boolean =
    primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: TextUnit,
    b: TextUnit,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: BaselineShift,
    b: BaselineShift,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: LineBreak,
    b: LineBreak,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: FontStyle,
    b: FontStyle,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: FontWeight,
    b: FontWeight,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: TextAlign,
    b: TextAlign,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: TextDirection,
    b: TextDirection,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun compareProperty(primitivesSet: Long, id: Byte, a: Hyphens, b: Hyphens): Boolean =
    primitivesSet.hasId(id) && a != b

private inline fun compareProperty(
    primitivesSet: Long,
    id: Byte,
    a: FontSynthesis,
    b: FontSynthesis,
): Boolean = primitivesSet.hasId(id) && a != b

private inline fun <T : Any> compareProperty(objectsSet: Int, id: Int, a: T?, b: T?): Boolean =
    objectsSet.hasId(id) && a != b
