/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.ui.window

import android.animation.ValueAnimator
import android.content.res.Configuration.HARDKEYBOARDHIDDEN_NO
import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogWithInsetsTest {
    @get:Rule val rule = createAndroidComposeRule<ActivityWithInsets>()

    private val durationSetter =
        ValueAnimator::class.java.getDeclaredMethod("setDurationScale", Float::class.java)

    @Before
    fun setDurationScale() {
        durationSetter.invoke(null, 1f)
    }

    @After
    fun resetDurationScale() {
        durationSetter.invoke(null, 0f)
    }

    /** Make sure that insets are available in the Dialog. */
    @Test
    fun dialogSupportsWindowInsets() {
        var dialogSize = IntSize.Zero
        val focusRequester = FocusRequester()
        var imeInsets = Insets.NONE
        lateinit var controller: WindowInsetsControllerCompat
        rule.setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(decorFitsSystemWindows = false)
            ) {
                val view = LocalView.current
                SideEffect {
                    val dialogWindowProvider = findDialogWindowProviderInParent(view)
                    if (dialogWindowProvider != null) {
                        controller = WindowInsetsControllerCompat(dialogWindowProvider.window, view)
                    }
                }
                val density = LocalDensity.current
                imeInsets =
                    Insets.of(
                        WindowInsets.ime.getLeft(density, LayoutDirection.Ltr),
                        WindowInsets.ime.getTop(density),
                        WindowInsets.ime.getRight(density, LayoutDirection.Ltr),
                        WindowInsets.ime.getBottom(density),
                    )
                Box(Modifier.fillMaxSize().background(Color.White).imePadding()) {
                    Box(Modifier.fillMaxSize().onSizeChanged { dialogSize = it }) {
                        TextField(
                            value = "Hello World",
                            onValueChange = {},
                            modifier =
                                Modifier.focusRequester(focusRequester).align(Alignment.Center)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        val originalSize = dialogSize

        // Add listener for the IME insets
        val insetsAppliedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            val decorView = rule.activity.window.decorView
            ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                if (ime.bottom > 0) {
                    insetsAppliedLatch.countDown()
                }
                insets
            }
        }

        // show the IME
        rule.runOnUiThread { focusRequester.requestFocus() }

        rule.waitForIdle()
        rule.runOnUiThread { controller.show(WindowInsetsCompat.Type.ime()) }

        val applied = insetsAppliedLatch.await(1, TimeUnit.SECONDS)

        if (!applied) {
            // The IME couldn't be opened, so we'll just exit the test.
            // This is pretty rare, but some devices are flaky when it comes
            // to opening the IME.
            return
        }

        rule.waitUntil { dialogSize != originalSize }
        rule.waitForIdle()
        assertNotEquals(Insets.NONE, imeInsets)
    }

    @Test
    fun dialogCanTakeEntireScreen() {
        var size = IntSize.Zero
        var displayWidth = 0
        var displayHeight = 0
        var insetsLeft = 0
        var insetsTop = 0
        var insetsRight = 0
        var insetsBottom = 0
        var textTop = 0
        var controller: SoftwareKeyboardController? = null
        rule.setContent {
            val displayMetrics = LocalView.current.resources.displayMetrics
            controller = LocalSoftwareKeyboardController.current
            displayWidth = displayMetrics.widthPixels
            displayHeight = displayMetrics.heightPixels
            Box(Modifier.fillMaxSize()) {
                Dialog(
                    {},
                    properties =
                        DialogProperties(
                            decorFitsSystemWindows = false,
                            usePlatformDefaultWidth = false
                        )
                ) {
                    val insets = WindowInsets.safeDrawing

                    Box(
                        Modifier.fillMaxSize()
                            .layout { m, c ->
                                val p = m.measure(c)
                                size = IntSize(p.width, p.height)
                                insetsTop = insets.getTop(this)
                                insetsLeft = insets.getLeft(this, layoutDirection)
                                insetsBottom = insets.getBottom(this)
                                insetsRight = insets.getRight(this, layoutDirection)
                                layout(p.width, p.height) { p.place(0, 0) }
                            }
                            .safeDrawingPadding()
                    ) {
                        TextField(
                            value = "Hello",
                            onValueChange = {},
                            Modifier.align(Alignment.BottomStart).testTag("textField").onPlaced {
                                layoutCoordinates ->
                                textTop = layoutCoordinates.positionInRoot().y.roundToInt()
                            }
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        if (
            Build.VERSION.SDK_INT >= 35 &&
                rule.activity.applicationContext.applicationInfo.targetSdkVersion >= 35
        ) {
            // On SDK >= 35, the metrics is the size of the entire screen
            assertThat(size.width).isEqualTo(displayWidth)
            assertThat(size.height).isEqualTo(displayHeight)
        } else {
            // On SDK < 35, the metrics is the size of the screen with some insets removed
            assertThat(size.width).isAtLeast(displayWidth)
            assertThat(size.height).isAtLeast(displayHeight)
        }
        // There is going to be some insets
        assertThat(maxOf(insetsLeft, insetsTop, insetsRight, insetsBottom)).isNotEqualTo(0)

        val hardKeyboardHidden =
            rule.runOnUiThread { rule.activity.resources.configuration.hardKeyboardHidden }
        if (hardKeyboardHidden == HARDKEYBOARDHIDDEN_NO) {
            return // can't launch the IME when the hardware keyboard is up.
        }
        val bottomInsetsBeforeIme = insetsBottom
        val textTopBeforeIme = textTop
        rule.onNodeWithTag("textField").requestFocus()
        rule.waitUntil {
            controller?.show()
            insetsBottom != bottomInsetsBeforeIme
        }
        rule.runOnIdle { assertThat(textTop).isLessThan(textTopBeforeIme) }
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun animatedWindowInsets() {
        val hardKeyboardHidden = rule.activity.resources.configuration.hardKeyboardHidden
        if (hardKeyboardHidden == HARDKEYBOARDHIDDEN_NO) {
            return // can't test when IME doesn't launch
        }

        var fullHeight by mutableIntStateOf(0)
        val outsideImeInsets = mutableListOf<Insets>()
        lateinit var outsideImeBounds: BoundsCompat
        val insideImeInsets = mutableListOf<Insets>()
        lateinit var insideImeBounds: BoundsCompat
        lateinit var dialogView: View
        val focusRequester = FocusRequester()
        var softwareKeyboardController: SoftwareKeyboardController? = null
        var animationRunning = false

        rule.setContent {
            Box(Modifier.fillMaxSize().onPlaced { fullHeight = it.size.height }) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false
                        )
                ) {
                    dialogView = LocalView.current
                    var view = dialogView
                    while (view !is AbstractComposeView) {
                        view = view.parent as View
                    }
                    view.consumeWindowInsets = false
                    softwareKeyboardController = LocalSoftwareKeyboardController.current
                    // center the content vertically by 34 pixels
                    val height = with(LocalDensity.current) { maxOf(0, fullHeight - 34).toDp() }
                    Box(Modifier.fillMaxWidth().height(height)) {
                        TextField(
                            "Hello World",
                            onValueChange = {},
                            Modifier.focusRequester(focusRequester).safeDrawingPadding()
                        )
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = {
                                View(it).apply {
                                    ViewCompat.setWindowInsetsAnimationCallback(
                                        this,
                                        object :
                                            WindowInsetsAnimationCompat.Callback(
                                                DISPATCH_MODE_CONTINUE_ON_SUBTREE
                                            ) {
                                            override fun onProgress(
                                                insets: WindowInsetsCompat,
                                                runningAnimations:
                                                    MutableList<WindowInsetsAnimationCompat>
                                            ): WindowInsetsCompat {
                                                insideImeInsets +=
                                                    insets.getInsets(WindowInsetsCompat.Type.ime())
                                                return insets
                                            }

                                            override fun onStart(
                                                animation: WindowInsetsAnimationCompat,
                                                bounds: BoundsCompat
                                            ): BoundsCompat {
                                                insideImeBounds = bounds
                                                return bounds
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            var rootView = dialogView
            while (rootView.parent is View) {
                rootView = rootView.parent as View
            }
            ViewCompat.setWindowInsetsAnimationCallback(
                rootView,
                object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    override fun onProgress(
                        insets: WindowInsetsCompat,
                        runningAnimations: MutableList<WindowInsetsAnimationCompat>
                    ): WindowInsetsCompat {
                        outsideImeInsets += insets.getInsets(WindowInsetsCompat.Type.ime())
                        return insets
                    }

                    override fun onStart(
                        animation: WindowInsetsAnimationCompat,
                        bounds: BoundsCompat
                    ): BoundsCompat {
                        outsideImeBounds = bounds
                        animationRunning = true
                        return bounds
                    }

                    override fun onEnd(animation: WindowInsetsAnimationCompat) {
                        animationRunning = false
                    }
                }
            )
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
            softwareKeyboardController?.show()
        }

        rule.waitForIdle()

        rule.waitUntil { !animationRunning }

        rule.runOnIdle {
            assertThat(insideImeBounds.upperBound.bottom)
                .isEqualTo(outsideImeBounds.upperBound.bottom - 17)
            for (i in insideImeInsets.size - 1 downTo 0) {
                val inside = insideImeInsets[i]
                val outside = outsideImeInsets[i + outsideImeInsets.size - insideImeInsets.size]
                assertThat(inside.bottom).isEqualTo(maxOf(0, outside.bottom - 17))
            }
        }
    }

    private fun findDialogWindowProviderInParent(view: View): DialogWindowProvider? {
        if (view is DialogWindowProvider) {
            return view
        }
        val parent = view.parent ?: return null
        if (parent is View) {
            return findDialogWindowProviderInParent(parent)
        }
        return null
    }
}
