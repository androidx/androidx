/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.material.demos

import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.Constraints
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.div
import androidx.ui.core.times
import androidx.ui.core.toRoundedPixels
import androidx.ui.material.Checkbox
import androidx.ui.material.RadioButton
import androidx.ui.material.Switch
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.composer

@Composable
fun FillGrid(horizontalGridCount: Int, @Children children: () -> Unit) {
    <MeasureBox> constraints ->
        val measurables = collect(children)
        val verticalGrid = (measurables.size + horizontalGridCount - 1) / horizontalGridCount
        val cellW = constraints.maxWidth / horizontalGridCount
        val cellH = constraints.maxHeight / verticalGrid
        val c = Constraints.tightConstraints(cellW, cellH)
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables
                    .map { it.measure(c) }
                    .forEachIndexed { index, placeable ->
                        val x = index % horizontalGridCount * cellW
                        val y = cellH * (index / horizontalGridCount)
                        placeable.place(x, y)
                    }
        }
    </MeasureBox>
}

@Composable
class SelectionsControlsDemo : Component() {
    override fun compose() {
        val customColor = Color(0xffff0000.toInt())
        <FillGrid horizontalGridCount=4>
            <Checkbox value=ToggleableState.CHECKED />
            <Checkbox value=ToggleableState.UNCHECKED />
            <Checkbox color=customColor />
            <Checkbox value=ToggleableState.INDETERMINATE />
            <Switch checked=true />
            <Switch checked=false />
            <Switch checked=true color=customColor />
            <Switch checked=false color=customColor />
            <RadioButton checked=true />
            <RadioButton checked=false />
            <RadioButton checked=true color=customColor />
            <RadioButton checked=false color=customColor />
        </FillGrid>
    }
}
