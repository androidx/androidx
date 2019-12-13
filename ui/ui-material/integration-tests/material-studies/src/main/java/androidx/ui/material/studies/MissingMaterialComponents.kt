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

package androidx.ui.material.studies

import androidx.ui.material.surface.Surface
import androidx.compose.Composable
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme

/**
 * This file contains Material components that are needed to build the Rally app and not
 * yet available in the Material module. These are mostly API stubs to show how the
 * component may look like API wise.
 */

@Composable
fun Scaffold(appBar: @Composable() () -> Unit, children: @Composable() () -> Unit) {
    Column {
        appBar()
        Surface(modifier = LayoutFlexible(1f), color = MaterialTheme.colors().surface) {
            children()
        }
    }
}