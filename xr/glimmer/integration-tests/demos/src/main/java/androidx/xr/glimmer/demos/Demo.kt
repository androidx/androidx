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

import androidx.compose.runtime.Composable

/** Generic demo with a [title] that will be displayed in the list of demos. */
sealed class Demo(val title: String) {
    override fun toString() = title
}

/** Demo that displays [Composable] [content] when selected. */
class ComposableDemo(title: String, val content: @Composable () -> Unit) : Demo(title)

/** A category of [Demo]s, that will display a list of [demos] when selected. */
class DemoCategory(title: String, val demos: List<Demo>) : Demo(title)
