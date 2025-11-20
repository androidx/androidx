/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.wear.protolayout.renderer.inflater

import androidx.annotation.ColorInt
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat
import androidx.wear.protolayout.expression.proto.DynamicProto.StateColorSource
import androidx.wear.protolayout.expression.proto.DynamicProto.StateFloatSource
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat
import androidx.wear.protolayout.proto.ColorProto.Brush
import androidx.wear.protolayout.proto.ColorProto.ColorProp
import androidx.wear.protolayout.proto.ColorProto.ColorStop
import androidx.wear.protolayout.proto.ColorProto.LinearGradient
import androidx.wear.protolayout.proto.ColorProto.SweepGradient
import androidx.wear.protolayout.proto.DimensionProto.BoundingBoxRatio
import androidx.wear.protolayout.proto.DimensionProto.ContainerDimension
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp
import androidx.wear.protolayout.proto.DimensionProto.DpProp
import androidx.wear.protolayout.proto.DimensionProto.ExpandedDimensionProp
import androidx.wear.protolayout.proto.DimensionProto.OffsetDimension
import androidx.wear.protolayout.proto.LayoutElementProto.Box
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement
import androidx.wear.protolayout.proto.ModifiersProto.Background
import androidx.wear.protolayout.proto.ModifiersProto.Modifiers
import androidx.wear.protolayout.proto.TypesProto.FloatProp
import androidx.wear.protolayout.renderer.helper.TestFingerprinter

/**
 * Kotlin helpers for creating ProtoLayout elements in proto format. To be used to simplify kt
 * tests.
 */
object ProtoLayoutHelpers {
    fun fingerprintedLayout(root: LayoutElement) =
        TestFingerprinter.getDefault().buildLayoutWithFingerprints(root)

    fun expandedBox(block: Box.Builder.() -> Unit) =
        LayoutElement.newBuilder()
            .setBox(Box.newBuilder().setWidth(expand()).setHeight(expand()).apply(block))
            .build()

    fun expand() =
        ContainerDimension.newBuilder()
            .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance())

    fun modifiers(block: Modifiers.Builder.() -> Unit) = Modifiers.newBuilder().apply(block).build()

    fun background(block: Background.Builder.() -> Unit) =
        Background.newBuilder().apply(block).build()

    fun linearGradient(
        vararg colorStops: ColorStop,
        block: LinearGradient.Builder.() -> Unit = {},
    ) = LinearGradient.newBuilder().addAllColorStops(colorStops.toList()).apply(block).build()

    fun sweepGradient(vararg colorStops: ColorStop, block: SweepGradient.Builder.() -> Unit = {}) =
        SweepGradient.newBuilder().addAllColorStops(colorStops.toList()).apply(block).build()

    val LinearGradient.toBrush: Brush
        get() = Brush.newBuilder().setLinearGradient(this).build()

    fun boundingBoxRatio(ratio: Float) = BoundingBoxRatio.newBuilder().setRatio(ratio.prop).build()

    val BoundingBoxRatio.toOffset: OffsetDimension
        get() = OffsetDimension.newBuilder().setLocationRatio(this).build()

    val DpProp.toOffset: OffsetDimension
        get() = OffsetDimension.newBuilder().setOffsetDp(this).build()

    val Float.dpProp: DpProp
        get() = DpProp.newBuilder().setValue(this).build()

    val Float.prop: FloatProp
        get() = floatProp(this)

    val Float.degreesProp: DegreesProp
        get() = DegreesProp.newBuilder().setValue(this).build()

    /** Builds a [FloatProp]. */
    fun floatProp(v: Float, dynamicValue: DynamicFloat? = null): FloatProp =
        FloatProp.newBuilder()
            .setValue(v)
            .apply { dynamicValue?.let { setDynamicValue(it) } }
            .build()

    /** Builds a [DegreesProp]. */
    fun degreesProp(v: Float, dynamicValue: DynamicFloat? = null): DegreesProp =
        DegreesProp.newBuilder()
            .setValue(v)
            .apply { dynamicValue?.let { setDynamicValue(it) } }
            .build()

    /** Builds a [ColorStop]. */
    fun colorStop(@ColorInt color: Int, offset: Float? = null): ColorStop =
        colorStop(colorProp(color), offset?.prop)

    /** Builds a [ColorProp]. */
    fun colorProp(@ColorInt color: Int, dynamicColor: DynamicColor? = null): ColorProp =
        ColorProp.newBuilder()
            .setArgb(color)
            .apply { dynamicColor?.let { setDynamicValue(it) } }
            .build()

    /** Builds a [ColorStop]. */
    fun colorStop(colorProp: ColorProp, offset: FloatProp? = null): ColorStop =
        ColorStop.newBuilder().setColor(colorProp).apply { offset?.let { setOffset(it) } }.build()

    /** Builds a [DynamicColor] with a fixed value. */
    fun fixedDynamicColor(@ColorInt v: Int): DynamicColor =
        DynamicColor.newBuilder().setFixed(FixedColor.newBuilder().setArgb(v)).build()

    /** Builds a [DynamicColor] with a state source. */
    fun stateDynamicColor(key: String): DynamicColor =
        DynamicColor.newBuilder()
            .setStateSource(StateColorSource.newBuilder().setSourceKey(key))
            .build()

    /** Builds a [DynamicFloat]. */
    fun fixedDynamicFloat(v: Float): DynamicFloat =
        DynamicFloat.newBuilder().setFixed(FixedFloat.newBuilder().setValue(v)).build()

    /** Builds a [DynamicFloat] with a state source. */
    fun stateDynamicFloat(key: String): DynamicFloat =
        DynamicFloat.newBuilder()
            .setStateSource(StateFloatSource.newBuilder().setSourceKey(key))
            .build()
}
