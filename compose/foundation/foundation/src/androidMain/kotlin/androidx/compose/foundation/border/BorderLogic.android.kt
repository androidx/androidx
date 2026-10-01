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

package androidx.compose.foundation.border

import android.os.Build

/**
 * On API 28+, we use a GraphicsLayer as it is more performant than allocating an offscreen bitmap.
 * On API < 28, GraphicsLayer uses the legacy OpenGL ES pipeline which rasterizes generic paths and
 * BlendMode.Clear differently from Skia, so we fall back to an offscreen ImageBitmap for consistent
 * rendering.
 */
internal actual fun shouldUseGraphicsLayerForGenericBorder(): Boolean = Build.VERSION.SDK_INT >= 28
