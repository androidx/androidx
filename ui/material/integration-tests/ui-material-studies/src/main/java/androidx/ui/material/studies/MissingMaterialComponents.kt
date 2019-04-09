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

import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.layout.Container
import androidx.ui.layout.FlexColumn
import androidx.ui.material.Colors
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.unaryPlus

/**
 * This file contains Material components that are needed to build the Rally app and not
 * yet available in the Material module. These are mostly API stubs to show how the
 * component may look like API wise.
 */

@Composable
fun Scaffold(appBar: @Composable() () -> Unit, @Children children: () -> Unit) {
    <FlexColumn>
        inflexible {
            <appBar />
        }
        expanded(flex = 1.0f) {
            val colors = +ambient(Colors)
            <Surface color={ surface }>
                <children />
            </Surface>
        }
    </FlexColumn>
}

@Composable
fun Spacer(size: Dp, orientation: Orientation) {
    if (orientation == Orientation.Horizontal) <Container expanded=true width=size />
    else <Container expanded=true height=size />
}

@Composable
fun Divider(color: Color, size: Dp = 2.dp) {
    <Container expanded=true height=size color />
}