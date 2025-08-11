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

package androidx.compose.ui.platform

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.unit.IntSize
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiom
import platform.UIKit.UIUserInterfaceIdiomPad

internal class UIKitWindowInsetsManager(
    val interfaceOrientation: State<InterfaceOrientation>,
    userInterfaceIdiom: UIUserInterfaceIdiom = UIDevice.currentDevice.userInterfaceIdiom
) {
    val layoutMargins = mutableStateOf(PlatformInsets.Zero)
    val safeAreaInsets = mutableStateOf(PlatformInsets.Zero)
    val keyboardOverlapHeight = mutableStateOf(0)
    val sceneSize = mutableStateOf(IntSize.Zero)

    val windowInsets: PlatformWindowInsets = UIKitWindowInsets(
        { layoutMargins.value },
        { safeAreaInsets.value },
        { keyboardOverlapHeight.value },
        { interfaceOrientation.value },
        { userInterfaceIdiom },
        { sceneSize.value }
    )

    /**
     * Cache that stores pre-configured PlatformWindowInsets instances for different exclusion combinations.
     *
     * The key is a Pair of Booleans representing:
     * - Whether to exclude safe insets (status/navigation bars)
     * - Whether to exclude IME insets (keyboard)
     */
    private val windowInsetsExclusionsCache = HashMap<Pair<Boolean, Boolean>, PlatformWindowInsets>()

    private inner class UIKitWindowInsets(
        private val layoutMargins: () -> PlatformInsets,
        private val safeAreaInsets: () -> PlatformInsets,
        private val keyboardOverlapHeight: () -> Int,
        private val interfaceOrientation: () -> InterfaceOrientation,
        private val userInterfaceIdiom: () -> UIUserInterfaceIdiom,
        private val sceneSize: () -> IntSize
    ) : PlatformWindowInsets {

        // Force InterfaceOrientation.Portrait for iPad display cutout computation. iPads don’t have left/right hardware cutouts and
        // safeAreaInsets for iPad report the same value independent of interface rotation.
        private fun displayCutoutEffectiveInterfaceOrientation(): InterfaceOrientation = if (userInterfaceIdiom() == UIUserInterfaceIdiomPad) InterfaceOrientation.Portrait else interfaceOrientation()

        override val displayCutouts: List<Rect>
            get() {
                val orientation = displayCutoutEffectiveInterfaceOrientation()
                val safeAreaInsets = safeAreaInsets()
                val sceneSize = sceneSize()

                val hasCutout = when (orientation) {
                    InterfaceOrientation.Portrait -> safeAreaInsets.top > 0
                    InterfaceOrientation.PortraitUpsideDown -> safeAreaInsets.bottom > 0
                    InterfaceOrientation.LandscapeLeft -> safeAreaInsets.right > 0
                    InterfaceOrientation.LandscapeRight -> safeAreaInsets.left > 0
                }

                if (!hasCutout || sceneSize.width <= 0 || sceneSize.height <= 0) {
                    return emptyList()
                } else {
                    return when (orientation) {
                        InterfaceOrientation.Portrait -> listOf(
                            Rect(0f, 0f, sceneSize.width.toFloat(), safeAreaInsets.top.toFloat())
                        )

                        InterfaceOrientation.PortraitUpsideDown -> listOf(
                            Rect(
                                0f,
                                sceneSize.height - safeAreaInsets.bottom.toFloat(),
                                sceneSize.width.toFloat(),
                                sceneSize.height.toFloat()
                            )
                        )

                        InterfaceOrientation.LandscapeLeft -> listOf(
                            Rect(
                                sceneSize.width - safeAreaInsets.right.toFloat(),
                                0f,
                                sceneSize.width.toFloat(),
                                sceneSize.height.toFloat()
                            )
                        )

                        InterfaceOrientation.LandscapeRight -> listOf(
                            Rect(0f, 0f, safeAreaInsets.left.toFloat(), sceneSize.height.toFloat())
                        )
                    }
                }
            }
        override val captionBar: PlatformInsets get() = PlatformInsets.Zero
        override val displayCutout: PlatformInsets get() = when (displayCutoutEffectiveInterfaceOrientation()) {
                InterfaceOrientation.Portrait -> PlatformInsets(getTop = { safeAreaInsets().top })
                InterfaceOrientation.PortraitUpsideDown -> PlatformInsets(getBottom = { safeAreaInsets().bottom })
                InterfaceOrientation.LandscapeLeft -> PlatformInsets(getRight = { safeAreaInsets().right }) // In LandscapeLeft orientation, the device's top edge (where the front camera is located) is positioned on the right
                InterfaceOrientation.LandscapeRight -> PlatformInsets(getLeft = { safeAreaInsets().left }) // In LandscapeRight orientation, the device's top edge (where the front camera is located) is positioned on the left
            }
        override val ime: PlatformInsets get() = PlatformInsets(getBottom = keyboardOverlapHeight)
        override val mandatorySystemGestures: PlatformInsets get() = PlatformInsets(getTop = { safeAreaInsets().top }, getBottom = { safeAreaInsets().bottom })
        override val navigationBars: PlatformInsets get() = PlatformInsets(getBottom = { safeAreaInsets().bottom })
        override val statusBars: PlatformInsets get() = PlatformInsets(getTop = { safeAreaInsets().top })
        override val systemBars: PlatformInsets get() = PlatformInsets(getLeft = { safeAreaInsets().left }, getRight = { safeAreaInsets().right }, getTop = { safeAreaInsets().top }, getBottom = { safeAreaInsets().bottom })
        override val systemGestures: PlatformInsets get() = PlatformInsets(getLeft = { layoutMargins().left }, getRight = { layoutMargins().right }, getTop = { layoutMargins().top }, getBottom = { layoutMargins().bottom })
        override val tappableElement: PlatformInsets get() = PlatformInsets(getTop = { safeAreaInsets().top })
        override val waterfall: PlatformInsets get() = PlatformInsets.Zero

        override fun excluding(safeInsets: Boolean, ime: Boolean): PlatformWindowInsets {
            if (!safeInsets && !ime) {
                return this
            }

            return windowInsetsExclusionsCache.getOrPut(safeInsets to ime) {
                val derivedLayoutMargins: () -> PlatformInsets by lazy { { layoutMargins().exclude(safeAreaInsets()) } }
                val derivedSafeAreaInsets: () -> PlatformInsets by lazy { { PlatformInsets.Zero } }
                val derivedKeyboardOverlapHeight: () -> Int by lazy { { 0 } }

                UIKitWindowInsets(
                    if (safeInsets) derivedLayoutMargins else layoutMargins,
                    if (safeInsets) derivedSafeAreaInsets else safeAreaInsets,
                    if (ime) derivedKeyboardOverlapHeight else keyboardOverlapHeight,
                    interfaceOrientation,
                    userInterfaceIdiom,
                    sceneSize
                )
            }
        }
    }
}