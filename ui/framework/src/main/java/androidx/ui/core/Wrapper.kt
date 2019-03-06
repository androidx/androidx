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
import androidx.annotation.CheckResult
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.R4a
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.effectOf

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
            var width = IntPx.Zero
            var height = IntPx.Zero
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

/**
 * [ambient] to get a [Density] object from an internal [DensityAmbient].
 *
 * Note: this is an experiment with the ways to achieve a read-only public [Ambient]s.
 */
@CheckResult(suggest = "+")
fun ambientDensity() = effectOf<Density> { +ambient(DensityAmbient) }

/**
 * An effect to be able to convert dimensions between each other.
 * A [Density] object will be taken from an ambient.
 *
 * Usage examples:
 *
 *     +withDensity() {
 *        val pxHeight = DpHeight.toPx()
 *     }
 *
 * or
 *
 *     val pxHeight = +withDensity(density) { DpHeight.toPx() }
 *
 */
@CheckResult(suggest = "+")
// can't make this inline as tests are failing with "DensityKt.$jacocoInit()' is inaccessible"
/*inline*/ fun <R> withDensity(/*crossinline*/ block: DensityReceiver.() -> R) = effectOf<R> {
    withDensity(+ambientDensity(), block)
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
