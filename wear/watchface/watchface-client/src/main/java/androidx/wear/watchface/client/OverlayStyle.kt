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

package androidx.wear.watchface.client

import android.graphics.Color

/**
 * This class reflects a snapshot of [androidx.wear.watchface.WatchFace.OverlayStyle], which is used
 * to configure the status overlay rendered by the system on top of the watch face. These settings
 * are applicable from Wear 3.0 and will be ignored on earlier devices.
 */
public class OverlayStyle(
    /**
     * The background color of the status indicator tray. This can be any color, including
     * [Color.TRANSPARENT]. If this is `null` then the system default will be used.
     */
    val backgroundColor: Color?,

    /**
     * The background color of items rendered in the status indicator tray. If not `null` then
     * this must be either [Color.BLACK] or [Color.WHITE]. If this is `null` then the system
     * default will be used.
     */
    val foregroundColor: Color?
) {
    /**
     * Default constructor when when there's no data available. E.g. when dealing with an old watch
     * face.
     */
    public constructor() : this(null, null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OverlayStyle

        if (backgroundColor != other.backgroundColor) return false
        if (foregroundColor != other.foregroundColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor?.hashCode() ?: 0
        result = 31 * result + (foregroundColor?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "OverlayStyle(backgroundColor=$backgroundColor, " +
            "foregroundColor=$foregroundColor)"
    }
}