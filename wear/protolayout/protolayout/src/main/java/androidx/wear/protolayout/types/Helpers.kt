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

package androidx.wear.protolayout.types

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.EmProp
import androidx.wear.protolayout.DimensionBuilders.SpProp
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TypeBuilders.BoolProp
import androidx.wear.protolayout.expression.RequiresSchemaVersion

@get:RestrictTo(Scope.LIBRARY_GROUP)
val Float.sp: SpProp
    get() = SpProp.Builder().setValue(this).build()

internal val Float.em: EmProp
    get() = EmProp.Builder().setValue(this).build()

internal val Boolean.prop: BoolProp
    get() = BoolProp.Builder(this).build()

@get:RestrictTo(Scope.LIBRARY_GROUP)
val Float.dp: DpProp
    get() = DpProp.Builder(this).build()

@RequiresSchemaVersion(major = 1, minor = 400)
internal fun cornerRadius(x: Float, y: Float) =
    ModifiersBuilders.CornerRadius.Builder(x.dp, y.dp).build()
