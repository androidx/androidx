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
@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.wear.protolayout.modifiers

import android.annotation.SuppressLint
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.annotation.FloatRange
import androidx.wear.protolayout.DimensionBuilders.DegreesProp
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.PivotDimension
import androidx.wear.protolayout.ModifiersBuilders.Transformation
import androidx.wear.protolayout.TypeBuilders.FloatProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import androidx.wear.protolayout.types.asBoundingBoxRatio
import androidx.wear.protolayout.types.asDegreesProp
import androidx.wear.protolayout.types.asDpProp
import androidx.wear.protolayout.types.asFloatProp
import androidx.wear.protolayout.types.boundingBoxRatio
import androidx.wear.protolayout.types.degrees
import androidx.wear.protolayout.types.dp
import androidx.wear.protolayout.types.prop

/**
 * Applies a translation to the layout element along the X axis.
 *
 * @param staticValueDp The static offset in DP.
 * @param dynamicValueDp The dynamic offset for animations or state binding.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.translateX(
    @Dimension(unit = DP) staticValueDp: Float,
    dynamicValueDp: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            translationX = dynamicValueDp?.asDpProp(staticValueDp) ?: staticValueDp.dp
        )

/**
 * Applies a translation to the layout element along the Y axis.
 *
 * @param staticValueDp The static offset in DP.
 * @param dynamicValueDp The dynamic offset for animations or state binding.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.translateY(
    @Dimension(unit = DP) staticValueDp: Float,
    dynamicValueDp: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            translationY = dynamicValueDp?.asDpProp(staticValueDp) ?: staticValueDp.dp
        )

/**
 * Scales the layout element along the X axis.
 *
 * @param staticValue The static scale factor (1.0 is original size).
 * @param dynamicValue The dynamic scale factor for animations or state binding.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.scaleX(
    @FloatRange(from = 0.0) staticValue: Float,
    dynamicValue: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            scaleX = dynamicValue?.asFloatProp(staticValue) ?: staticValue.prop
        )

/**
 * Scales the layout element along the Y axis.
 *
 * @param staticValue The static scale factor (1.0 is original size).
 * @param dynamicValue The dynamic scale factor for animations or state binding.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.scaleY(
    @FloatRange(from = 0.0) staticValue: Float,
    dynamicValue: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            scaleY = dynamicValue?.asFloatProp(staticValue) ?: staticValue.prop
        )

/**
 * Rotates the layout element. Positive values result in clockwise rotation.
 *
 * @param staticValueDegrees The static rotation angle in degrees.
 * @param dynamicValueDegrees The dynamic rotations angle for animations or state binding.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.rotate(
    staticValueDegrees: Float,
    dynamicValueDegrees: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            rotation =
                dynamicValueDegrees?.asDegreesProp(staticValueDegrees) ?: staticValueDegrees.degrees
        )

/**
 * Sets the horizontal pivot offset for scaling and rotation as an absolute distance from the
 * element center.
 *
 * @param staticValueDp The static horizontal pivot in DP.
 * @param dynamicValueDp The dynamic horizontal pivot offset.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.pivotX(
    @Dimension(unit = DP) staticValueDp: Float,
    dynamicValueDp: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            pivotX = dynamicValueDp?.asDpProp(staticValueDp) ?: staticValueDp.dp
        )

/**
 * Sets the vertical pivot offset for scaling and rotation as an absolute distance from the element
 * center.
 *
 * @param staticValueDp The static vertical pivot in DP.
 * @param dynamicValueDp The dynamic vertical pivot offset.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.pivotY(
    @Dimension(unit = DP) staticValueDp: Float,
    dynamicValueDp: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            pivotY = dynamicValueDp?.asDpProp(staticValueDp) ?: staticValueDp.dp
        )

/**
 * Sets the horizontal pivot point for scaling and rotation as a location proportional to the
 * bounding box width.
 *
 * @param staticRatio The static horizontal pivot ratio (e.g., 0.5 for center). Value outside [0, 1]
 *   are also valid.
 * @param dynamicRatio The dynamic horizontal pivot ratio.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.ratioPivotX(
    staticRatio: Float,
    dynamicRatio: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            pivotX = dynamicRatio?.asBoundingBoxRatio(staticRatio) ?: staticRatio.boundingBoxRatio
        )

/**
 * Sets the vertical pivot point for scaling and rotation as a location proportional to the bounding
 * box height.
 *
 * @param staticRatio The static vertical pivot ratio (e.g., 0.5 for center). Value outside [0, 1]
 *   are also valid.
 * @param dynamicRatio The dynamic vertical pivot ratio.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
public fun LayoutModifier.ratioPivotY(
    staticRatio: Float,
    dynamicRatio: DynamicFloat? = null,
): LayoutModifier =
    this then
        BaseTransformationElement(
            pivotY = dynamicRatio?.asBoundingBoxRatio(staticRatio) ?: staticRatio.boundingBoxRatio
        )

internal class BaseTransformationElement(
    val translationX: DpProp? = null,
    val translationY: DpProp? = null,
    val scaleX: FloatProp? = null,
    val scaleY: FloatProp? = null,
    val rotation: DegreesProp? = null,
    val pivotX: PivotDimension? = null,
    val pivotY: PivotDimension? = null,
) : BaseProtoLayoutModifiersElement<Transformation.Builder> {
    @SuppressLint("ProtoLayoutMinSchema")
    override fun mergeTo(initialBuilder: Transformation.Builder?): Transformation.Builder? =
        (initialBuilder ?: Transformation.Builder()).apply {
            translationX?.let { setTranslationX(translationX) }
            translationY?.let { setTranslationY(translationY) }
            scaleX?.let { setScaleX(scaleX) }
            scaleY?.let { setScaleY(scaleY) }
            rotation?.let { setRotation(rotation) }
            pivotX?.let { setPivotX(pivotX) }
            pivotY?.let { setPivotY(pivotY) }
        }
}
