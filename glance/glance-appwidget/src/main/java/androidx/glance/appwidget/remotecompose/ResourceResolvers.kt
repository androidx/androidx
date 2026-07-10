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

package androidx.glance.appwidget.remotecompose

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.glance.unit.ColorProvider

// this function may not be part of the final api, but it exists to keep track of everywhere
// we are eagerly resolving colors
internal fun eagerlyResolveColor(colorProvider: ColorProvider, context: Context): Int {
    Log.w("ResourceResolvers", "Warning, eagerly resolving color provider")
    return colorProvider.getColor(context).toArgb()
}
