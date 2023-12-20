/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.glance.ColorFilterParams
import androidx.glance.unit.ColorProvider

/**
 * An internal, AppWidget specific colorFilter that applies alpha as well as tint from the provided
 * color. Helps with changing color of entire drawable and using it as shaped color background.
 */
internal class TintAndAlphaColorFilterParams(val colorProvider: ColorProvider) : ColorFilterParams {
    override fun toString() =
        "TintAndAlphaColorFilterParams(colorProvider=$colorProvider))"
}
