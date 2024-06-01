/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Test case representing a layout hierarchy of nested rows and columns. The purpose of this
 * benchmark is to measure compose, measure, and layout performance of Row and Column, which are
 * extremely common layouts. The parameters can be used to change the nature of the hierarchy (wide
 * vs deep, etc.) and stress different situations. The benchmark attempts to use different common
 * arrangements/alignments as well as child modifiers such as weight and alignBy.
 *
 * @param useWeight If true, weight modifiers will be added to some children in the hierarchy
 * @param useAlign If true, align modifiers will be added to some children in the hierarchy
 * @param depth This is the depth of the resulting hierarchy. Be careful making this number too big
 *   as it will quickly increase the runtime of the benchmark.
 * @param breadth This is the number of direct children each row/column has at each level of the
 *   hierarchy. Be careful making this number too big as it will quickly increase the runtime of the
 *   benchmark.
 */
class DeepRowColumnTestCase(
    private val useWeight: Boolean,
    private val useAlign: Boolean,
    private val depth: Int,
    private val breadth: Int,
) : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        Row { DeepTree(useWeight, useAlign, depth, breadth) }
    }
}

val blueBackground = Modifier.background(color = Color.Blue)
val magentaBackground = Modifier.background(color = Color.Magenta)
val blackBackground = Modifier.background(color = Color.Black)

@Composable
@NonRestartableComposable
private fun Terminal(style: Int, modifier: Modifier = Modifier) {
    val background =
        when (style) {
            0 -> blueBackground
            1 -> blackBackground
            else -> magentaBackground
        }
    Box(modifier = modifier.fillMaxSize().then(background))
}

private fun horizArrangementFor(id: Int) =
    when (id % 2) {
        0 -> Arrangement.Start
        1 -> Arrangement.End
        else -> Arrangement.Center
    }

private fun vertArrangementFor(id: Int) =
    when (id % 2) {
        0 -> Arrangement.Top
        1 -> Arrangement.Bottom
        else -> Arrangement.Center
    }

private fun vertAlignmentFor(id: Int) =
    when (id % 2) {
        0 -> Alignment.Top
        1 -> Alignment.CenterVertically
        else -> Alignment.Bottom
    }

private fun horizAlignmentFor(id: Int) =
    when (id % 2) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }

private fun ColumnScope.modifierFor(id: Int, useWeight: Boolean, useAlign: Boolean): Modifier {
    return if (useWeight && id == 0) Modifier.weight(0.5f, true)
    else if (useAlign && id == 0) Modifier.align(Alignment.CenterHorizontally)
    else Modifier.fillMaxWidth()
}

private fun ColumnScope.terminalModifierFor(id: Int, useWeight: Boolean): Modifier {
    return if (useWeight && id == 0) Modifier.weight(0.5f, true) else Modifier
}

private fun RowScope.modifierFor(id: Int, useWeight: Boolean, useAlign: Boolean): Modifier {
    return if (useWeight && id == 0) Modifier.weight(0.5f, true)
    else if (useAlign && id == 0) Modifier.align(Alignment.CenterVertically)
    else Modifier.fillMaxHeight()
}

private fun RowScope.terminalModifierFor(id: Int, useWeight: Boolean): Modifier {
    return if (useWeight && id == 0) Modifier.weight(0.5f, true) else Modifier
}

@Composable
@NonRestartableComposable
private fun ColumnScope.DeepTree(
    useWeight: Boolean,
    useAlign: Boolean,
    depth: Int,
    breadth: Int,
    id: Int = 0
) {
    Row(
        modifier = modifierFor(id, useWeight, useAlign),
        horizontalArrangement = horizArrangementFor(id),
        verticalAlignment = vertAlignmentFor(id),
    ) {
        if (depth == 0) {
            Terminal(
                style = id % 3,
                modifier = terminalModifierFor(id, useWeight),
            )
        } else {
            repeat(breadth) { DeepTree(useWeight, useAlign, depth - 1, breadth, it) }
        }
    }
}

@Composable
@NonRestartableComposable
private fun RowScope.DeepTree(
    useWeight: Boolean,
    useAlign: Boolean,
    depth: Int,
    breadth: Int,
    id: Int = 0
) {
    Column(
        modifier = modifierFor(id, useWeight, useAlign),
        verticalArrangement = vertArrangementFor(id),
        horizontalAlignment = horizAlignmentFor(id),
    ) {
        if (depth == 0) {
            Terminal(
                style = id % 3,
                modifier = terminalModifierFor(id, useWeight),
            )
        } else {
            repeat(breadth) { DeepTree(useWeight, useAlign, depth - 1, breadth, it) }
        }
    }
}
