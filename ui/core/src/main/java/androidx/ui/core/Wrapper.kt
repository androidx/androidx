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
package androidx.ui.core

import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.R4a
import com.google.r4a.composer

class CraneWrapper(@Children private val children: () -> Unit) : Component() {
    private val androidCraneView = arrayOfNulls<AndroidCraneView>(1)
    private var ambients: Ambient.Reference? = null
    override fun compose() {
        <AndroidCraneView ref=androidCraneView
                          constraintsChanged={ composeChildren() }>
            <Ambient.Portal> reference ->
                ambients = reference
            </Ambient.Portal>
        </AndroidCraneView>
        composeChildren()
    }

    private fun composeChildren() {
        val craneView = androidCraneView[0]
        if (craneView != null) {
            val context = craneView.context ?: composer.composer.context

            val measureBoxes = mutableListOf<MeasureBox>()
            R4a.composeInto(container = craneView.root, context = context, parent = ambients!!) {
                <MeasureBoxes.Provider value=measureBoxes>
                    <children />
                    measureBoxes.forEach { measureBox ->
                        measureBox.measure(craneView.constraints)
                        measureBox.layout()
                    }
                </MeasureBoxes.Provider>
            }
            var width = 0.dp
            var height = 0.dp
            measureBoxes.forEach { measureBox ->
                width = max(width, measureBox.layoutNode.width)
                height = max(width, measureBox.layoutNode.height)
            }
            craneView.root.resize(width, height)
        }
    }
}
