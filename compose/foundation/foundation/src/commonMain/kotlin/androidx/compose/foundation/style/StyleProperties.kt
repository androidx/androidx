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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.foundation.style

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.lerp as fontLerp
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp as uiLerp
import kotlin.jvm.JvmInline

/** A useful type alias which for which map to use to store property values. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal typealias StylePropertyMap<T> = MutableScatterMap<StyleProperty<T>, T>

/**
 * A class that is used to store the property values collected by t he [StylePropertyCollector]
 * indirectly used by the [StyleResolver].
 */
@ExperimentalFoundationStyleApi
@Suppress("UNCHECKED_CAST")
@JvmInline
internal value class StyleProperties(
    val properties: StylePropertyMap<Any?> = mutableScatterMapOf()
) {
    operator fun <T> get(property: StyleProperty<T>): T =
        (properties as StylePropertyMap<T>).getOrElse(property) { property.defaultValue() }

    operator fun <T> set(property: StyleProperty<T>, value: T) {
        (properties as StylePropertyMap<T>)[property] = value
    }

    fun <T> getOrNull(property: StyleProperty<T>): T? =
        (properties as StylePropertyMap<T>)[property]

    operator fun contains(property: StyleProperty<*>) =
        (property as StyleProperty<Any?>) in properties

    @Suppress("AsCollectionCall") fun keys() = properties.asMap().keys

    fun copy() = StyleProperties(properties.copy())

    fun clear() {
        properties.clear()
    }

    fun diffInto(
        other: StyleProperties,
        changes: MutableScatterSet<StyleProperty<*>>?,
    ): MutableScatterSet<StyleProperty<*>>? {
        // Defensively clearing the set. This should always come in as `null` or a clear set but
        // this
        // checking makes the code more resilient.
        changes.clearIfNotNull()

        if (other.properties === this.properties) return changes

        var newChanges = changes
        forEach { property, value ->
            val otherValue = other.getOrNull(property)
            if (otherValue == null || otherValue != value) {
                (newChanges ?: mutableScatterSetOf<StyleProperty<*>>().also { newChanges = it })
                    .add(property)
            }
        }
        other.forEach { property, _ ->
            if (property !in this) {
                (newChanges ?: mutableScatterSetOf<StyleProperty<*>>().also { newChanges = it })
                    .add(property)
            }
        }

        return newChanges
    }

    inline fun forEach(block: (StyleProperty<*>, value: Any?) -> Unit) {
        properties.forEach { property, value -> block(property, value) }
    }

    inline fun removeIf(block: (StyleProperty<*>) -> Boolean) {
        properties.removeIf { property, _ -> block(property) }
    }
}

// TODO(chuckj@google.com): Consider copy-on-write
@ExperimentalFoundationStyleApi
private fun StylePropertyMap<Any?>.copy(): StylePropertyMap<Any?> {
    val newMap = StylePropertyMap<Any?>()
    forEach { key, value -> newMap[key] = value }
    return newMap
}

/*
 * A set of property definitions that are used to implement the Style properties.
 *
 * These will be will move to tests as an example set of properties.
 */

private val oneFloat: () -> Float = { 1.0f }
private val zeroFloat: () -> Float = { 0.0f }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val contentPaddingStartProperty = stylePropertyOf("contentPaddingStart", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val contentPaddingEndProperty = stylePropertyOf("contentPaddingEnd", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val contentPaddingTopProperty = stylePropertyOf("contentPaddingTop", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val contentPaddingBottomProperty = stylePropertyOf("contentPaddingBottom", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val externalPaddingStartProperty = stylePropertyOf("externalPaddingStart", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val externalPaddingEndProperty = stylePropertyOf("externalPaddingEnd", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val externalPaddingTopProperty = stylePropertyOf("externalPaddingTop", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val externalPaddingBottomProperty =
    stylePropertyOf("externalPaddingBottom", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val widthProperty = stylePropertyOf("width") { Breadth.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val heightProperty = stylePropertyOf("height") { Breadth.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val leftProperty = stylePropertyOf("left", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val rightProperty = stylePropertyOf("right", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val topProperty = stylePropertyOf("top", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val bottomProperty = stylePropertyOf("bottom", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val minWidthProperty = stylePropertyOf("minWidth", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val maxWidthProperty = stylePropertyOf("maxWidth", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val minHeightProperty = stylePropertyOf("minHeight", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val maxHeightProperty = stylePropertyOf("maxHeight", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val borderFillProperty = stylePropertyOf("borderFill") { Fill.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val borderWidthProperty = stylePropertyOf("borderWidth", ::lerp) { 0.dp }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val backgroundProperty = stylePropertyOf("background") { Fill.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val foregroundProperty = stylePropertyOf("foreground") { Fill.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val clipProperty = stylePropertyOf("clip") { false }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val shapeProperty = stylePropertyOf("shape") { RectangleShape }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val dropShadowProperty = stylePropertyOf("dropShadow") { Shadows.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val innerShadowProperty = stylePropertyOf("innerShadow") { Shadows.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val alphaProperty = stylePropertyOf("alpha", ::uiLerp, oneFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val scaleXProperty = stylePropertyOf("scaleX", ::uiLerp, oneFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val scaleYProperty = stylePropertyOf("scaleY", ::uiLerp, oneFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val translationXProperty = stylePropertyOf("translationX", ::uiLerp, zeroFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val translationYProperty = stylePropertyOf("translationY", ::uiLerp, zeroFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val rotationXProperty = stylePropertyOf("rotationX", ::uiLerp, zeroFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val rotationYProperty = stylePropertyOf("rotationY", ::uiLerp, zeroFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val rotationZProperty = stylePropertyOf("rotationZ", ::uiLerp, zeroFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val transformOriginXProperty =
    stylePropertyOf("transformOriginX", ::uiLerp) { TransformOrigin.Center.pivotFractionX }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val transformOriginYProperty =
    stylePropertyOf("transformOriginY", ::uiLerp) { TransformOrigin.Center.pivotFractionY }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val cameraDistanceProperty = stylePropertyOf("cameraDistance", ::uiLerp, oneFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val zIndexProperty = stylePropertyOf("zIndex", ::uiLerp, zeroFloat)

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val colorFilterProperty = stylePropertyOf<ColorFilter?>("colorFilter") { null }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val contentFillLocal = styleLocalOf("contentFill") { Fill.None }

/**
 * Transitionary value. Do not use. This will be removed before the Styles API is stable.
 *
 * This gives access to the fill value set by [ContentColorScope].
 */
@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
public val StylePropertyAccessorScope.contentFill: Fill
    get() = contentFillLocal.value

/**
 * Transitionary value. Do not use. This will be removed before the Styles API is stable.
 *
 * This gives access to the fill value set by [ContentColorScope].
 */
@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
public val StylePropertyAccessorScope.contentColor: Color
    get() = contentFillLocal.value.asColor()

/**
 * Transitionary value. Do not use. This will be removed before the Styles API is stable.
 *
 * This gives access to the fill value set by [ContentColorScope].
 */
@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
public val StylePropertyAccessorScope.contentBrush: Brush?
    get() = contentFillLocal.value.asBrush()

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val fontFamilyLocal = styleLocalOf("fontFamily") { FontFamily.Default as FontFamily }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val textIndentFirstLineLocal =
    styleLocalOf("textIndentFirstLine", ::lerp) { TextUnit.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val textIndentRestLineLocal =
    styleLocalOf("textIndentRestLine", ::lerp) { TextUnit.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val fontSizeLocal = styleLocalOf("fontSize", ::lerp) { TextUnit.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val lineHeightLocal = styleLocalOf("lineHeight", ::lerp) { TextUnit.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val baselineShiftLocal = styleLocalOf("baselineShift") { BaselineShift.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val lineBreakLocal = styleLocalOf("lineBreak") { LineBreak.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val textDecorationLocal = styleLocalOf("textDecoration") { TextDecoration.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val letterSpacingLocal = styleLocalOf("letterSpacing", ::lerp) { TextUnit.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val fontStyleLocal = styleLocalOf("fontStyle") { FontStyle.Normal }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val textAlignLocal = styleLocalOf("textAlign") { TextAlign.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val textDirectionLocal = styleLocalOf("textDirection") { TextDirection.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val hyphensLocal = styleLocalOf("hyphens") { Hyphens.Unspecified }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val fontSynthesisLocal = styleLocalOf("fontSynthesis") { FontSynthesis.None }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val fontWeightLocal = styleLocalOf("fontWeight", ::fontLerp) { FontWeight.Normal }

@Suppress("ExperimentalPropertyAnnotation")
@ExperimentalFoundationStyleApi
internal val textMotionLocal =
    styleLocalOf(
        "textMotion",
        { a, b, _ ->
            // If either of the motions are animated then the value styles animated until both are
            // static.
            if (a == b) a else TextMotion.Animated
        },
    ) {
        TextMotion.Static
    }
