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

import android.content.Context
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.R4a
import com.google.r4a.composer
import kotlin.math.max

class CraneWrapper(@Children var children: () -> Unit) : Component() {
    private val androidCraneView = arrayOfNulls<AndroidCraneView>(1)
    private var ambients: Ambient.Reference? = null

    override fun compose() {
        <AndroidCraneView
            ref=androidCraneView
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
            val layoutNode = craneView.root

            R4a.composeInto(container = layoutNode, context = context, parent = ambients!!) {
                <ContextAmbient.Provider value=context>
                    <DensityAmbient.Provider value=Density(context)>
                        <children />
                    </DensityAmbient.Provider>
                </ContextAmbient.Provider>
            }
            var width = 0
            var height = 0
            layoutNode.childrenMeasureBoxes().forEach { measureBox ->
                measureBox as ComplexMeasureBox
                measureBox.runBlock()
                measureBox.measure(craneView.constraints)
                measureBox.placeChildren()
                width = max(width, measureBox.layoutNode.width)
                height = max(height, measureBox.layoutNode.height)
            }
            layoutNode.resize(width, height)
        }
    }
}

val ContextAmbient = Ambient.of<Context>()

internal val DensityAmbient = Ambient.of<Density>()

@Composable
fun DensityConsumer(@Children children: (density: Density) -> Unit) {
    <DensityAmbient.Consumer> density ->
        <children density />
    </DensityAmbient.Consumer>
}

/**
 * Temporary needed to be able to use the component from the adapter module. b/120971484
 */
@Composable
fun CraneWrapperComposable(@Children children: () -> Unit) {
    <CraneWrapper>
        <children />
    </CraneWrapper>
}
