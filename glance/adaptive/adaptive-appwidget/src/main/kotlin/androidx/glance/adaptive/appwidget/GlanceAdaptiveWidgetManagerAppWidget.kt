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

package androidx.glance.adaptive.appwidget

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager

/**
 * Creates a [GlanceAdaptiveWidgetManager] configured with the platform AppWidget delegate for phone
 * widget operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun GlanceAdaptiveWidgetManager(context: Context): GlanceAdaptiveWidgetManager =
    GlanceAdaptiveWidgetManager(BaseWidgetDelegate(context))
