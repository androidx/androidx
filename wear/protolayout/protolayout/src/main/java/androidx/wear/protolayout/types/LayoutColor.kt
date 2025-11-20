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
@file:JvmName("LayoutColorUtil")

package androidx.wear.protolayout.types

import android.annotation.SuppressLint
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import java.util.Objects

/**
 * Static or dynamic color value for layout fields.
 *
 * This can be used on layout color fields with data binding support.
 *
 * @property staticArgb is the static color value in ARGB. If [dynamicArgb] is not null this will be
 *   used as the default value for when [dynamicArgb] can't be resolved.
 * @property dynamicArgb the dynamic value. If this value can be resolved, the `staticValue` won't
 *   be used.
 */
@SuppressLint("ProtoLayoutMinSchema") // 1.2 Schema is used only when dynamicValue is not null
class LayoutColor(
    @ColorInt val staticArgb: Int,
    @RequiresSchemaVersion(major = 1, minor = 200) val dynamicArgb: DynamicColor? = null,
) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val prop: ColorProp by lazy {
        ColorProp.Builder(staticArgb).apply { dynamicArgb?.let { setDynamicValue(it) } }.build()
    }

    override fun equals(other: Any?) =
        this === other || (other is LayoutColor && prop == other.prop)

    override fun hashCode() = Objects.hash(prop)

    override fun toString() = "LayoutColor(prop=$prop)"
}

/** Extension for creating a [LayoutColor] from an ARGB Color Int. */
val Int.argb: LayoutColor
    @JvmName("createLayoutColor") get() = LayoutColor(this)

/** Extension for creating a [LayoutColor] from an ARGB Color Long. */
val Long.argb: LayoutColor
    @JvmName("createLayoutColor") get() = LayoutColor(this.toInt())

/**
 * Extension for creating a [LayoutColor] from a [DynamicColor]
 *
 * @param staticArgb the static value that can be used when the `dynamicValue` can't be resolved.
 */
@JvmName("createLayoutColor")
@RequiresSchemaVersion(major = 1, minor = 200)
fun DynamicColor.asLayoutColor(@ColorInt staticArgb: Int) = LayoutColor(staticArgb, this)

/**
 * Extension for creating a [LayoutColor] from a [DynamicColor]
 *
 * @param staticArgb the static value that can be used when the `dynamicValue` can't be resolved.
 */
@JvmName("createLayoutColor")
@RequiresSchemaVersion(major = 1, minor = 200)
fun DynamicColor.asLayoutColor(@ColorInt staticArgb: Long) = LayoutColor(staticArgb.toInt(), this)
