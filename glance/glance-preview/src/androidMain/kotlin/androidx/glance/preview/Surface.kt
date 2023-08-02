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

package androidx.glance.preview

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/**
 * The list of glance surfaces that have preview available. The list will grow as more glance
 * surfaces are added and allow the preview functionality. Items should be used as values for
 * surface parameter in [GlancePreview] annotation.
 */
@ExperimentalGlancePreviewApi
object Surfaces {
    const val APP_WIDGET = "AppWidget"
    const val TILE = "Tile"
}

/**
 * The annotation that ensures that the variable value is strictly a recognized glance surface.
 *
 * @see GlancePreview annotation
 */
@Retention(AnnotationRetention.SOURCE)
@OptIn(ExperimentalGlancePreviewApi::class)
@StringDef(value = [Surfaces.APP_WIDGET, Surfaces.TILE])
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class Surface
