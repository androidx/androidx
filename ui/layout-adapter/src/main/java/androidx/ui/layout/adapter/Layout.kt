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

package androidx.ui.core.adapter

import androidx.ui.core.Constraints
import androidx.ui.core.Dimension
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FlexChildren
import androidx.ui.layout.StackChildren
import com.google.r4a.Children
import com.google.r4a.Composable

// Ignore that the IDEA cannot resolve these.
import androidx.ui.painting.Color

import androidx.ui.layout.AlignKt
import androidx.ui.layout.ConstrainedBoxKt
import androidx.ui.layout.ContainerKt
import androidx.ui.layout.FlexKt
import androidx.ui.layout.PaddingKt
import androidx.ui.layout.StackKt

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun FlexRow(@Children() block: (FlexChildren) -> Unit) {
    FlexKt.FlexRow(block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun FlexColumn(@Children() block: (FlexChildren) -> Unit) {
    FlexKt.FlexColumn(block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Row(@Children() block: () -> Unit) {
    FlexKt.Row(block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Column(@Children() block: () -> Unit) {
    FlexKt.Column(block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Center
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Center(@Children() block: () -> Unit) {
    AlignKt.Center(block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Alignment
 */
@Suppress("PLUGIN_ERROR")
class Alignment {
    companion object {
        val TopLeft = androidx.ui.layout.Alignment(-1f, -1f)
        val TopCenter = androidx.ui.layout.Alignment(-1f, 0f)
        val TopRight = androidx.ui.layout.Alignment(-1f, 1f)
        val CenterLeft = androidx.ui.layout.Alignment(0f, -1f)
        val Center = androidx.ui.layout.Alignment(0f, 0f)
        val CenterRight = androidx.ui.layout.Alignment(0f, 1f)
        val BottomLeft = androidx.ui.layout.Alignment(1f, -1f)
        val BottomCenter = androidx.ui.layout.Alignment(1f, 0f)
        val BottomRight = androidx.ui.layout.Alignment(1f, 1f)
    }
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Align
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Align(alignment: androidx.ui.layout.Alignment, @Children() block: () -> Unit) {
    AlignKt.Align(alignment, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Stack
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Stack(
    defaultAlignment: androidx.ui.layout.Alignment = Alignment.Center,
    @Children() block: (StackChildren) -> Unit
) {
    StackKt.Stack(defaultAlignment, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.ConstrainedBox
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun ConstrainedBox(additionalConstraints: Constraints, @Children() block: () -> Unit) {
    ConstrainedBoxKt.ConstrainedBox(additionalConstraints, block)
}

/**
 * For the original logic
 * @see androidx.ui.layout.Padding
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Padding(
    padding: EdgeInsets,
    @Children() block: () -> Unit
) {
    PaddingKt.Padding(padding, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Padding
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Container(
    padding: EdgeInsets? = null,
    color: Color? = null,
    alignment: androidx.ui.layout.Alignment? = null,
    margin: EdgeInsets? = null,
    constraints: Constraints? = null,
    width: Dimension? = null,
    height: Dimension? = null,
    @Children() block: () -> Unit
) {
    ContainerKt.Container(padding, color, alignment, margin, constraints, width, height, block)
}
