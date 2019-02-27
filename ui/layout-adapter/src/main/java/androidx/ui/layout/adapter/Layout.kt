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
import androidx.ui.core.Dp
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FlexChildren
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.StackChildren
import com.google.r4a.Children
import com.google.r4a.Composable

// Ignore that the IDEA cannot resolve these.
import androidx.ui.painting.Color

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun FlexRow(
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
    @Children() block: (FlexChildren) -> Unit
) {
    androidx.ui.layout.FlexRow(crossAxisAlignment, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun FlexColumn(
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
    @Children() block: (FlexChildren) -> Unit
) {
    androidx.ui.layout.FlexColumn(crossAxisAlignment, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Row(crossAxisAlignment: Int = CrossAxisAlignment.Center, @Children() block: () -> Unit) {
    androidx.ui.layout.Row(crossAxisAlignment, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Flex
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Column(crossAxisAlignment: Int = CrossAxisAlignment.Center, @Children() block: () -> Unit) {
    androidx.ui.layout.Column(crossAxisAlignment, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.Center
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Center(@Children() block: () -> Unit) {
    androidx.ui.layout.Center(block)
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
    androidx.ui.layout.Align(alignment, block)
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
    androidx.ui.layout.Stack(defaultAlignment, block)
}

/**
 * For the original logic:
 * @see androidx.ui.layout.ConstrainedBox
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun ConstrainedBox(additionalConstraints: Constraints, @Children() block: () -> Unit) {
    androidx.ui.layout.ConstrainedBox(additionalConstraints, block)
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
    androidx.ui.layout.Padding(padding, block)
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
    width: Dp? = null,
    height: Dp? = null,
    @Children() block: (() -> Unit) = {}
) {
    androidx.ui.layout.Container(
        padding, color, alignment, margin, constraints, width, height, block
    )
}
