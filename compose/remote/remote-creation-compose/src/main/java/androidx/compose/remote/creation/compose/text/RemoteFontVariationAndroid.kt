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

@file:JvmName("RemoteFontVariationKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.text

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Density

/**
 * Converts these [FontVariation.Settings] to [RemoteFontVariation.Settings], resolving each value
 * with [FontVariation.Setting.toVariationValue].
 *
 * @param density The [Density] used to resolve settings that need it, such as
 *   [FontVariation.opticalSizing]. Without a [density], such a setting is resolved only when its
 *   axis value is written to the document, and then throws an [IllegalArgumentException].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun FontVariation.Settings.toRemoteFontVariationSettings(
    density: Density? = null
): RemoteFontVariation.Settings =
    RemoteFontVariation.Settings(
        List(settings.size) {
            val setting = settings[it]
            RemoteFontVariation.Setting(setting.axisName, setting.toRemoteVariationValue(density))
        }
    )

/**
 * Returns [FontVariation.Setting.toVariationValue] as a [RemoteFloat]. A setting that needs a
 * missing [density] becomes a dynamic value that resolves when it is written, so it does not throw
 * if `fontFeatureSettings` override its axis, and it equals another such value exactly when the
 * [FontVariation.Setting]s are equal.
 */
private fun FontVariation.Setting.toRemoteVariationValue(density: Density?): RemoteFloat =
    if (density == null && needsDensity) {
        RemoteFloatExpression(constantValueOrNull = null, cacheKey = RemoteConstantCacheKey(this)) {
            floatArrayOf(toVariationValue(null))
        }
    } else {
        toVariationValue(density).rf
    }
