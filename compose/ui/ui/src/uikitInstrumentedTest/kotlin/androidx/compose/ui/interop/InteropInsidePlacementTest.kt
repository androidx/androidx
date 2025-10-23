/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import platform.UIKit.UIView

class InteropInsidePlacementTest {
    @Test
    fun testInteropViewsOrder() = runUIKitInstrumentedTestWithInterop { overlay ->
        val interopView1 = UIView()
        val interopView2 = UIView()
        val interopView3 = UIView()
        val middleViewVisible = mutableStateOf(false)
        setContent {
            Box {
                UIKitView(
                    factory = { interopView1 },
                    modifier = Modifier.size(100.dp),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                if (middleViewVisible.value) {
                    UIKitView(
                        factory = { interopView2 },
                        modifier = Modifier.size(100.dp),
                        properties = UIKitInteropProperties(placedAsOverlay = overlay)
                    )
                }
                UIKitView(
                    factory = { interopView3 },
                    modifier = Modifier.size(100.dp),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertNotNull(interopView1.window)
        assertNull(interopView2.window)
        assertNotNull(interopView3.window)

        assertTrue(interopView1.viewDepth < interopView3.viewDepth)

        middleViewVisible.value = true
        waitForIdle()

        assertNotNull(interopView1.window)
        assertNotNull(interopView2.window)
        assertNotNull(interopView3.window)

        assertTrue(interopView1.viewDepth < interopView2.viewDepth)
        assertTrue(interopView1.viewDepth < interopView3.viewDepth)
    }

    @Test
    fun testInteropViewsOverlayOrder() = runUIKitInstrumentedTest {
        val overlayInteropView = UIView()
        val dynamicInteropView = UIView()
        val backgroundInteropView = UIView()
        val isOverlay = mutableStateOf(false)
        setContent {
            Box {
                UIKitView(
                    factory = { overlayInteropView },
                    modifier = Modifier.size(100.dp),
                    properties = UIKitInteropProperties(placedAsOverlay = true)
                )
                UIKitView(
                    factory = { dynamicInteropView },
                    modifier = Modifier.size(100.dp),
                    properties = UIKitInteropProperties(placedAsOverlay = isOverlay.value)
                )
                UIKitView(
                    factory = { backgroundInteropView },
                    modifier = Modifier.size(100.dp),
                    properties = UIKitInteropProperties(placedAsOverlay = false)
                )
            }
        }

        assertTrue(dynamicInteropView.viewDepth < backgroundInteropView.viewDepth)
        assertTrue(backgroundInteropView.viewDepth < overlayInteropView.viewDepth)

        isOverlay.value = true
        waitForIdle()

        assertTrue(backgroundInteropView.viewDepth < overlayInteropView.viewDepth)
        assertTrue(overlayInteropView.viewDepth < dynamicInteropView.viewDepth)
    }
}

private val UIView.childrenCount: Int get() =
    subviews.fold(0) { acc, view -> acc + (view as UIView).childrenCount + 1 }

private val UIView.viewDepth: Int get() {
    var levelInParent = 1
    for (subview in superview?.subviews ?: emptyList<UIView>()) {
        subview as UIView
        if (subview == this) {
            break
        } else {
            levelInParent = 1 + subview.viewDepth
        }
    }
    return levelInParent + (superview?.viewDepth ?: 0)
}
