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

package androidx.xr.glimmer.demos

import androidx.xr.glimmer.samples.ButtonSampleUsage
import androidx.xr.glimmer.samples.ColorsSample
import androidx.xr.glimmer.samples.IconSampleUsage
import androidx.xr.glimmer.samples.ListItemSampleUsage
import androidx.xr.glimmer.samples.ShapesSample
import androidx.xr.glimmer.samples.SurfaceSampleUsage
import androidx.xr.glimmer.samples.TypographySample

val Demos =
    DemoCategory(
        "Glimmer Demos",
        listOf(
            ComposableDemo("Colors") { ColorsSample() },
            ComposableDemo("Typography") { TypographySample() },
            ComposableDemo("Shapes") { ShapesSample() },
            ComposableDemo("Surface") { SurfaceSampleUsage() },
            ComposableDemo("Icons") { IconSampleUsage() },
            ComposableDemo("Buttons") { ButtonSampleUsage() },
            ComposableDemo("ListItems") { ListItemSampleUsage() },
            DemoCategory("Focus", FocusDemos),
            DemoCategory("List", ListDemos),
            ComposableDemo("Settings") { DemoSettings() },
        ),
    )
