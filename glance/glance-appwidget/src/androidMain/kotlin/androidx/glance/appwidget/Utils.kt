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

package androidx.glance.appwidget

import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.glance.unit.Dp
import androidx.glance.unit.dp

internal fun Dp.toPixels(displayMetrics: DisplayMetrics) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()

internal fun Int.pixelsToDp(displayMetrics: DisplayMetrics) =
    (this / displayMetrics.density).dp

/**
 * KTX for calling `setEnabled` on a View. Note that this is **not safe on TextViews (and
 * descendants) before API 24**, but it is safe for any other view type.
 */
internal fun RemoteViews.setViewEnabled(viewId: Int, enabled: Boolean) {
    setBoolean(viewId, "setEnabled", enabled)
}