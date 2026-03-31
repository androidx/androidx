/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.demos.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.layout.RemoteAbsoluteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.integration.demos.common.propertyName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteBoxAlignmentsDemo() {
    val alignmentId = "alignmentId"
    val alignments =
        listOf(
            0 to RemoteAlignment.TopStart,
            1 to RemoteAlignment.TopCenter,
            2 to RemoteAlignment.TopEnd,
            3 to RemoteAlignment.CenterStart,
            4 to RemoteAlignment.Center,
            5 to RemoteAlignment.CenterEnd,
            6 to RemoteAlignment.BottomStart,
            7 to RemoteAlignment.BottomCenter,
            8 to RemoteAlignment.BottomEnd,
            9 to RemoteAbsoluteAlignment.TopLeft,
            10 to RemoteAbsoluteAlignment.TopRight,
            11 to RemoteAbsoluteAlignment.CenterLeft,
            12 to RemoteAbsoluteAlignment.CenterRight,
            13 to RemoteAbsoluteAlignment.BottomLeft,
            14 to RemoteAbsoluteAlignment.BottomRight,
        )
    var selectedAlignment by remember { mutableIntStateOf(alignments[0].first) }
    var expanded by remember { mutableStateOf(false) }

    Column {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color.Gray)
                    .clickable { expanded = true }
                    .padding(16.dp)
        ) {
            Text(
                "Alignment: ${alignments.find { it.first == selectedAlignment }?.second?.propertyName()}"
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                alignments.forEach { (id, alignment) ->
                    DropdownMenuItem(
                        text = { Text(alignment.propertyName()) },
                        onClick = {
                            selectedAlignment = id
                            expanded = false
                        },
                    )
                }
            }
        }

        RemoteDemo(update = { player -> player.setUserLocalInt(alignmentId, selectedAlignment) }) {
            val alignmentId = rememberNamedRemoteInt(alignmentId, alignments[0].first)

            RemoteStateLayout(
                modifier = RemoteModifier.wrapContentSize(),
                state = alignmentId,
                states = alignments.map { it.first }.toIntArray(),
            ) { state ->
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize().background(RemoteColor(Color.LightGray)),
                    contentAlignment = alignments[state].second,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(RemoteDp(50.dp)).background(RemoteColor(Color.Red))
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RemoteBoxAlignmentsDemoPreview() {
    RemoteBoxAlignmentsDemo()
}
