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

package androidx.ui.rendering.proxybox

import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.rendering.box.RenderBox

// /// A render object that absorbs pointers during hit testing.
// ///
// /// When [absorbing] is true, this render object prevents its subtree from
// /// receiving pointer events by terminating hit testing at itself. It still
// /// consumes space during layout and paints its child as usual. It just prevents
// /// its children from being the target of located events, because its render
// /// object returns true from [hitTest].
// ///
// /// See also:
// ///
// ///  * [RenderIgnorePointer], which has the opposite effect: removing the
// ///    subtree from considering entirely for the purposes of hit testing.
class RenderAbsorbPointer(
    child: RenderBox? = null,
    // /// Whether this render object absorbs pointers during hit testing.
    // ///
    // /// Regardless of whether this render object absorbs pointers during hit
    //  /// testing, it will still consume space during layout and be visible during
    //  /// painting.
    var absorbing: Boolean = true
) : RenderProxyBox(child) {

    override fun hitTest(result: HitTestResult, position: Offset): Boolean {
        return if (absorbing) size.contains(position) else super.hitTest(result, position)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("absorbing", absorbing))
    }
}