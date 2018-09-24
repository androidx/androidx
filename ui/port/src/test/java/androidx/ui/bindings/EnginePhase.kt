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

package androidx.ui.bindings

/**
 * Phases that can be reached by [WidgetTester.pumpWidget] and
 * [TestWidgetsFlutterBinding.pump].
 *
 * See [WidgetsBinding.drawFrame] for a more detailed description of some of
 * these phases.
 */
internal enum class EnginePhase {
    /** The build phase in the widgets library. See [BuildOwner.buildScope]. */
    build,

    /** The layout phase in the rendering library. See [PipelineOwner.flushLayout]. */
    layout,

    /**
     * The compositing bits update phase in the rendering library. See
     * [PipelineOwner.flushCompositingBits].
     */
    compositingBits,

    /** The paint phase in the rendering library. See [PipelineOwner.flushPaint]. */
    paint,

    /**
     * The compositing phase in the rendering library. See
     * [RenderView.compositeFrame]. This is the phase in which data is sent to
     * the GPU. If semantics are not enabled, then this is the last phase.
     */
    composite,

    /**
     * The semantics building phase in the rendering library. See
     * [PipelineOwner.flushSemantics].
     */
    flushSemantics,

    /**
     * The final phase in the rendering library, wherein semantics information is
     * sent to the embedder. See [SemanticsOwner.sendSemanticsUpdate].
     */
    sendSemanticsUpdate
}
