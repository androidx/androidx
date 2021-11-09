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

package androidx.glance

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.DpSize

/**
 * Size of the glance view being generated.
 *
 * The glance view will have at least that much space to be displayed. The exact meaning may
 * changed depending on the surface and how it is configured.
 */
public val LocalSize = staticCompositionLocalOf<DpSize> { error("No default size") }

/**
 * Context of application when generating the glance view.
 */
public val LocalContext = staticCompositionLocalOf<Context> { error("No default context") }
