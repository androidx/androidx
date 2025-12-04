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

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview @Composable fun DemoAnchorTextPreview() = RemoteDocPreview(DemoAnchorText.anchoredText())

@Preview
@Composable
fun DemoAttributedStringPreview() = RemoteDocPreview(DemoAttributedString.demo())

// Failing to display due to b/450104887
@Preview @Composable fun DemoPathsPreview() = RemoteDocPreview(DemoPaths.pathTest())
