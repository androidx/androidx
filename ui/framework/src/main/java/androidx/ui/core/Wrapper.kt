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
import com.google.r4a.Composable
import com.google.r4a.R4a
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.effectOf
import com.google.r4a.memo
import com.google.r4a.unaryPlus

@Composable
fun CraneWrapper(@Children children: () -> Unit) {
    val rootRef = +memo { Ref<AndroidCraneView>() }
    var ambientsRef: Ambient.Reference? = null

    val measure = { constraints: Constraints ->
        val rootLayoutNode = rootRef.value?.root ?: error("Failed to create root platform view")
        val context = rootRef.value?.context ?: composer.composer.context
        R4a.composeInto(container = rootLayoutNode, context = context, parent = ambientsRef!!) {
            <ContextAmbient.Provider value=context>
                <DensityAmbient.Provider value=Density(context)>
                    <children />
                </DensityAmbient.Provider>
            </ContextAmbient.Provider>
        }
        var width = IntPx.Zero
        var height = IntPx.Zero
        rootLayoutNode.childrenMeasureBoxes().forEach { measureBox ->
            val layoutNode: LayoutNode
            when (measureBox) {
                is ComplexMeasureBox -> {
                    measureBox.runBlock()
                    measureBox.measure(constraints)
                    measureBox.placeChildren()
                    layoutNode = measureBox.layoutNode
                }
                is ComplexLayoutState -> {
                    measureBox.measure(constraints)
                    measureBox.placeChildren()
                    layoutNode = measureBox.layoutNode
                }
                else -> error("Invalid CraneWrapper child found.")
            }
            width = max(width, layoutNode.width)
            height = max(height, layoutNode.height)
        }
        rootLayoutNode.resize(width, height)
    }
    // TODO(popam): make requestLayoutOnNodesLayoutChange=false when old measure boxes disappear
    <AndroidCraneView ref=rootRef requestLayoutOnNodesLayoutChange=true onMeasureRecompose=measure>
        <Ambient.Portal> reference ->
            ambientsRef = reference
        </Ambient.Portal>
    </AndroidCraneView>
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
