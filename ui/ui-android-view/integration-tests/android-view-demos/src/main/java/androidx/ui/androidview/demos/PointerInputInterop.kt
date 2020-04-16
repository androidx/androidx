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

package androidx.ui.androidview.demos

import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.DemoCategory

val PointerInteropDemos = DemoCategory(
    "Pointer Interop Demos", listOf(
        ActivityDemo(
            "Compose with no gestures in an Android Clickable",
            ComposeNothingInAndroidTap::class
        ),
        ActivityDemo(
            "Compose with tap in an Android Clickable",
            ComposeTapInAndroidTap::class
        ),
        ActivityDemo(
            "Compose with tap in an Android Scrollable",
            ComposeTapInAndroidScroll::class
        ),
        ActivityDemo(
            "Compose with scroll in an Android Scrollable (same orientation)",
            ComposeScrollInAndroidScrollSameOrientation::class
        ),
        ActivityDemo(
            "Compose with scroll in an Android Scrollable (different orientation)",
            ComposeScrollInAndroidScrollDifferentOrientation::class
        )
    )
)