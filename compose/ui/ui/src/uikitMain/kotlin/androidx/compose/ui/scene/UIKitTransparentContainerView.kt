/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.scene

import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import androidx.compose.ui.uikit.utils.CMPScrollView

internal class UIKitTransparentContainerView(
    var onLayoutSubviews: () -> Unit = {}
) : CMPScrollView(CGRectZero.readValue()) {
    private var onAppeared: (() -> Unit)? = null

    init {
        showsHorizontalScrollIndicator = false
        showsVerticalScrollIndicator = false
        panGestureRecognizer.setEnabled(false)
        bounces = false
    }

    fun runOnceOnAppeared(block: () -> Unit) {
        onAppeared = {
            block()
            onAppeared = null
        }

        runOnAppearedIfEligible()
    }

    fun dispose() {
        onLayoutSubviews = {}
        onAppeared = {}
    }

    private fun runOnAppearedIfEligible() {
        if (window != null && !CGRectIsEmpty(frame)) {
            onAppeared?.invoke()
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        onLayoutSubviews()
        runOnAppearedIfEligible()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        setNeedsLayout()
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        // forwards touches forward to the children, is never a target for a touch
        val result = super.hitTest(point, withEvent)

        return if (result == this) {
            null
        } else {
            result
        }
    }
}