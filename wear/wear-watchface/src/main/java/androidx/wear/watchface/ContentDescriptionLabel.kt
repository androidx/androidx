/*
 * Copyright 2021 The Android Open Source Project
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

import android.app.PendingIntent
import android.content.res.Resources
import android.graphics.Rect
import androidx.wear.complications.data.ComplicationText
import java.util.Objects

/**
 * Describes regions of the watch face for use by a screen reader.
 *
 * @param text [ComplicationText] associated with the region, to be read by the screen reader.
 * @param bounds [Rect] describing the area of the feature on screen.
 * @param tapAction [PendingIntent] to be used if the screen reader's user triggers a tap
 * action.
 */
public class ContentDescriptionLabel(
    public val text: ComplicationText,
    public val bounds: Rect,
    public val tapAction: PendingIntent?
) {
    /**
     * Returns the text that should be displayed for the given timestamp.
     *
     * @param resources [Resources] from the current [android.content.Context]
     * @param dateTimeMillis milliseconds since epoch, e.g. from [System.currentTimeMillis]
     */
    public fun getTextAt(resources: Resources, dateTimeMillis: Long): CharSequence =
        text.getTextAt(resources, dateTimeMillis)

    override fun equals(other: Any?): Boolean =
        other is ContentDescriptionLabel &&
            text == other.text &&
            bounds == other.bounds &&
            tapAction == other.tapAction

    override fun hashCode(): Int {
        return Objects.hash(
            text,
            bounds,
            tapAction
        )
    }
}