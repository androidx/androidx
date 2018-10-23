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
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.rendering.box.BoxHitTestEntry
import androidx.ui.rendering.box.RenderBox

/**
 * A RenderProxyBox subclass that allows you to customize the
 * hit-testing behavior.
 */
open class RenderProxyBoxWithHitTestBehavior(
    /** How to behave during hit testing. */
    var behavior: HitTestBehavior = HitTestBehavior.DEFER_TO_CHILD,
    child: RenderBox? = null
) : RenderProxyBox(
    child
) {
    override fun hitTest(result: HitTestResult, position: Offset): Boolean {
        var hitTarget = false
        if (size.contains(position)) {
            hitTarget = this.hitTestChildren(result, position = position) ||
                    this.hitTestSelf(position)
            if (hitTarget || behavior == HitTestBehavior.TRANSLUCENT) {
                result.add(BoxHitTestEntry(this, position))
            }
        }
        return hitTarget
    }

    override fun hitTestSelf(position: Offset) = behavior == HitTestBehavior.OPAQUE

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(EnumProperty("behavior", behavior, defaultValue = null))
    }
}