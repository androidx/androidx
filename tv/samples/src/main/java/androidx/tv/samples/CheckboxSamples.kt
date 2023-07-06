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

@file:OptIn(ExperimentalTvMaterial3Api::class)

package androidx.tv.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api

@Sampled
@Composable
fun CheckboxSample() {
    Checkbox(checked = true, onCheckedChange = { })
}
