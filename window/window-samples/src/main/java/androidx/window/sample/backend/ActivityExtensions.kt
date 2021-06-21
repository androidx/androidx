/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window.sample.backend

import android.app.Activity
import android.graphics.Point
import androidx.window.WindowManager

/**
 * Return a [Point] whose dimensions match the metrics of the window.
 * @return [Point] whose dimensions match the metrics of the window.
 */
internal fun Activity.calculateWindowSizeExt(): Point {
    val bounds = WindowManager(this).getCurrentWindowMetrics().bounds
    return Point(bounds.width(), bounds.height())
}
