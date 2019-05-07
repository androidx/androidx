/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

val DefaultTestTag = "DEFAULT_TEST_TAG"
val TestTagAmbient = Ambient.of { DefaultTestTag }

// Implementation with ambients now for only one semantics inside.
// replace with mergeable semantics later
@Composable
fun TestTag(tag: String, @Children children: @Composable() () -> Unit) {
    TestTagAmbient.Provider(value = tag, children = children)
}