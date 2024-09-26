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
package androidx.window.embedding

import android.content.res.Configuration
import androidx.window.core.Bounds
import androidx.window.layout.WindowLayoutInfo

/**
 * The parent container information directly passed from WM Extensions, which is created to make
 * test implementation easier.
 *
 * @property windowBounds The parent container's [Bounds].
 * @property windowLayoutInfo The parent container's [WindowLayoutInfo].
 * @property configuration The parent container's [Configuration].
 * @property density The parent container's density in DP, which has the same unit as
 *   [android.util.DisplayMetrics.density].
 */
internal data class ParentContainerInfo(
    /** The parent container's [Bounds]. */
    val windowBounds: Bounds,
    /** The parent container's [WindowLayoutInfo]. */
    val windowLayoutInfo: WindowLayoutInfo,
    /** The parent container's [Configuration]. */
    val configuration: Configuration,
    /**
     * The parent container's density in DP, which has the same unit as
     * [android.util.DisplayMetrics.density].
     */
    val density: Float,
)
