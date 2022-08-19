/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface

import android.graphics.Color
import androidx.annotation.RestrictTo
import androidx.wear.watchface.data.WatchFaceColorsWireFormat

/**
 * Provides information about the colors of a watch face, exposing the three most
 * representative colors. This may be used by the system to influence the colors used for the
 * system ui.
 */
public class WatchFaceColors(
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WatchFaceColors

        if (primaryColor != other.primaryColor) return false
        if (secondaryColor != other.secondaryColor) return false
        if (tertiaryColor != other.tertiaryColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryColor.hashCode()
        result = 31 * result + secondaryColor.hashCode()
        result = 31 * result + tertiaryColor.hashCode()
        return result
    }

    override fun toString(): String {
        return "WatchfaceColors(primaryColor=$primaryColor, secondaryColor=$secondaryColor, " +
            "tertiaryColor=$tertiaryColor)"
    }

    internal fun toWireFormat() = WatchFaceColorsWireFormat(
        primaryColor.toArgb(),
        secondaryColor.toArgb(),
        tertiaryColor.toArgb()
    )
}

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun WatchFaceColorsWireFormat.toApiFormat() = WatchFaceColors(
    Color.valueOf(primaryColor),
    Color.valueOf(secondaryColor),
    Color.valueOf(tertiaryColor)
)