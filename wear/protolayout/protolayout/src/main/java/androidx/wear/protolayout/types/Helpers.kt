/*
 * Copyright 2024 The Android Open Source Project
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
@file:Suppress("FacadeClassJvmName")

package androidx.wear.protolayout.types

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.wear.protolayout.DimensionBuilders.BoundingBoxRatio
import androidx.wear.protolayout.DimensionBuilders.DegreesProp
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.EmProp
import androidx.wear.protolayout.DimensionBuilders.SpProp
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TypeBuilders.BoolProp
import androidx.wear.protolayout.TypeBuilders.FloatProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.RequiresSchemaVersion

@get:RestrictTo(Scope.LIBRARY_GROUP)
public val Float.sp: SpProp
    get() = SpProp.Builder().setValue(this).build()

internal val Float.em: EmProp
    get() = EmProp.Builder().setValue(this).build()

internal val Boolean.prop: BoolProp
    get() = BoolProp.Builder(this).build()

@get:RestrictTo(Scope.LIBRARY_GROUP)
public val Float.dp: DpProp
    get() = DpProp.Builder(this).build()

@get:RestrictTo(Scope.LIBRARY_GROUP)
public val Float.prop: FloatProp
    get() = FloatProp.Builder(this).build()

@get:RestrictTo(Scope.LIBRARY_GROUP)
public val Float.degrees: DegreesProp
    get() = DegreesProp.Builder(this).build()

@get:RestrictTo(Scope.LIBRARY_GROUP)
@get:SuppressLint("ProtoLayoutMinSchema")
public val Float.boundingBoxRatio: BoundingBoxRatio
    get() = BoundingBoxRatio.Builder(this.prop).build()

@SuppressLint("ProtoLayoutMinSchema")
@RestrictTo(Scope.LIBRARY_GROUP)
public fun DynamicFloat.asDpProp(staticValue: Float): DpProp =
    DpProp.Builder(staticValue).setDynamicValue(this).build()

@SuppressLint("ProtoLayoutMinSchema")
@RestrictTo(Scope.LIBRARY_GROUP)
public fun DynamicFloat.asFloatProp(staticValue: Float): FloatProp =
    FloatProp.Builder(staticValue).setDynamicValue(this).build()

@SuppressLint("ProtoLayoutMinSchema")
@RestrictTo(Scope.LIBRARY_GROUP)
public fun DynamicFloat.asDegreesProp(staticValue: Float): DegreesProp =
    DegreesProp.Builder(staticValue).setDynamicValue(this).build()

@SuppressLint("ProtoLayoutMinSchema")
@RestrictTo(Scope.LIBRARY_GROUP)
public fun DynamicFloat.asBoundingBoxRatio(staticValue: Float): BoundingBoxRatio =
    BoundingBoxRatio.Builder(this.asFloatProp(staticValue)).build()

@RequiresSchemaVersion(major = 1, minor = 400)
internal fun cornerRadius(x: Float, y: Float): ModifiersBuilders.CornerRadius =
    ModifiersBuilders.CornerRadius.Builder(x.dp, y.dp).build()
