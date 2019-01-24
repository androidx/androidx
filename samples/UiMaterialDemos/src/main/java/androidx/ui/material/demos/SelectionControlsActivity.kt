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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.CraneWrapper
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.core.min
import androidx.ui.core.tightConstraints
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.painting.Color
import com.google.r4a.Composable
import com.google.r4a.setContent

open class SelectionControlsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <TmpLayout />
                </MaterialTheme>
            </CraneWrapper> }
    }
}

@Composable
fun TmpLayout() {
    <MeasureBox> constraints, measureOperations ->
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            constraints.minWidth
        }

        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            constraints.minHeight
        }

        val measurables = measureOperations.collect {
            <Checkbox value=ToggleableState.CHECKED/>
            <Checkbox value=ToggleableState.UNCHECKED/>
            <Checkbox value=ToggleableState.INDETERMINATE/>
            <Checkbox color=Color(0xffff0000.toInt()) />
        }

        val size = min(width, height)
        val rectSize = size / 2
        measureOperations.layout(size, size) {
            val placeables = measurables.map {
                measureOperations.measure(it, tightConstraints(rectSize, rectSize))
            }
            placeables[0].place(0.dp, 0.dp)
            placeables[1].place(rectSize, 0.dp)
            placeables[2].place(0.dp, rectSize)
            placeables[3].place(rectSize, rectSize)
        }
    </MeasureBox>
}