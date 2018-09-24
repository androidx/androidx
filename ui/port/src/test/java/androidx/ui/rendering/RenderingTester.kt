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

package androidx.ui.rendering

import androidx.ui.bindings.EnginePhase
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.window.Window
import androidx.ui.painting.alignment.Alignment
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.proxybox.RenderConstrainedBox
import androidx.ui.rendering.shiftedbox.RenderPositionedBox
import androidx.ui.widgets.binding.WidgetsBinding
import androidx.ui.widgets.binding.WidgetsFlutterBinding

internal class TestRenderingFlutterBinding(
    val binding: WidgetsBinding
) : WidgetsBinding by binding {

    internal var phase: EnginePhase = EnginePhase.composite

    override fun drawFrame() {
        assert(phase != EnginePhase.build) {
            "rendering_tester does not support testing the build phase; use flutter_test instead" }
        pipelineOwner!!.flushLayout()
        if (phase == EnginePhase.layout)
            return
        pipelineOwner.flushCompositingBits()
        if (phase == EnginePhase.compositingBits)
            return
        pipelineOwner.flushPaint()
        if (phase == EnginePhase.paint)
            return
        renderView!!.compositeFrame()
        if (phase == EnginePhase.composite)
            return
        pipelineOwner.flushSemantics()
        if (phase == EnginePhase.flushSemantics)
            return
        assert(phase == EnginePhase.flushSemantics ||
                phase == EnginePhase.sendSemanticsUpdate)
    }
}

private var _renderer: TestRenderingFlutterBinding? = null
internal val renderer: TestRenderingFlutterBinding get() {
    if (_renderer == null) {
        val window = Window().apply {
            physicalSize = Size(800.0, 600.0)
        }
        _renderer = TestRenderingFlutterBinding(WidgetsFlutterBinding.create(window))
    }
    return _renderer!!
}

/**
 * Place the box in the render tree, at the given size and with the given
 * alignment on the screen.
 *
 * If you've updated `box` and want to lay it out again, use [pumpFrame].
 *
 * Once a particular [RenderBox] has been passed to [layout], it cannot easily
 * be put in a different place in the tree or passed to [layout] again, because
 * [layout] places the given object into another [RenderBox] which you would
 * need to unparent it from (but that box isn't itself made available).
 *
 * The EnginePhase must not be [EnginePhase.build], since the rendering layer
 * has no build phase.
 */
internal fun layout(
    box: RenderBox,
    constraints: BoxConstraints? = null,
    alignment: Alignment = Alignment.center,
    phase: EnginePhase = EnginePhase.layout
) {
    assert(box != null); // If you want to just repump the last box, call pumpFrame().
    assert(box.parent == null); // We stick the box in another, so you can't reuse it easily, sorry.

    var resultBox = box
    renderer.renderView!!.child = null
    if (constraints != null) {
        resultBox = RenderPositionedBox(
                alignment = alignment,
                child = RenderConstrainedBox(
                        _additionalConstraints = constraints,
        child = box
        )
        )
    }
    renderer.renderView!!.child = resultBox

    pumpFrame(phase = phase)
}

internal fun pumpFrame(phase: EnginePhase = EnginePhase.layout) {
    assert(renderer != null)
    assert(renderer.renderView != null)
    assert(renderer.renderView!!.child != null); // call layout() first!
    renderer.phase = phase
    renderer.drawFrame()
}