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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.PlainTooltip(
    modifier: Modifier,
    caretSize: DpSize,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
): Unit = implementedInJetBrainsFork()

@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.PlainTooltip(
    modifier: Modifier,
    caretSize: DpSize,
    maxWidth: Dp,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
): Unit = implementedInJetBrainsFork()

@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.RichTooltip(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    action: (@Composable () -> Unit)?,
    caretSize: DpSize,
    shape: Shape,
    colors: RichTooltipColors,
    tonalElevation: Dp,
    shadowElevation: Dp,
    text: @Composable () -> Unit
): Unit = implementedInJetBrainsFork()

@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.RichTooltip(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    action: (@Composable () -> Unit)?,
    caretSize: DpSize,
    maxWidth: Dp,
    shape: Shape,
    colors: RichTooltipColors,
    tonalElevation: Dp,
    shadowElevation: Dp,
    text: @Composable () -> Unit
): Unit = implementedInJetBrainsFork()
