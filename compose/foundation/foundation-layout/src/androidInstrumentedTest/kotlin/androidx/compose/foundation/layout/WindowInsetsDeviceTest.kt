/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.compose.foundation.layout

import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowInsetsDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<WindowInsetsActivity>()

    @Before
    fun setup() {
        rule.activity.createdLatch.await(1, TimeUnit.SECONDS)
    }

    @After
    fun teardown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            show(WindowInsetsCompat.Type.navigationBars())
            show(WindowInsetsCompat.Type.statusBars())
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = rule.activity
        while (!activity.isDestroyed) {
            instrumentation.runOnMainSync {
                if (!activity.isDestroyed) {
                    activity.finish()
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun disableConsumeDisablesAnimationConsumption() {
        rule.runOnUiThread {
            Assume.assumeTrue(
                rule.activity.resources.configuration.keyboardHidden !=
                    Configuration.KEYBOARDHIDDEN_YES
            )
        }

        var imeInset1 = 0
        var imeInset2 = 0

        val focusRequester = FocusRequester()

        rule.setContent {
            Column(Modifier.fillMaxSize()) {
                BasicTextField(
                    "Hello",
                    onValueChange = {},
                    Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { outerContext ->
                        ComposeView(outerContext).apply {
                            consumeWindowInsets = false
                            setContent {
                                imeInset1 = WindowInsets.ime.getBottom(LocalDensity.current)
                                Box(Modifier.fillMaxSize()) {
                                    AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { context ->
                                            ComposeView(context).apply {
                                                consumeWindowInsets = false
                                                setContent {
                                                    imeInset2 =
                                                        WindowInsets.ime.getBottom(
                                                            LocalDensity.current
                                                        )
                                                    Box(
                                                        Modifier.fillMaxSize()
                                                            .background(Color.Cyan)
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }

        rule.waitForIdle()

        rule.waitUntil(timeoutMillis = 3000) {
            rule.runOnIdle {
                focusRequester.requestFocus()
                val controller = rule.activity.window.insetsController
                controller?.show(android.view.WindowInsets.Type.ime())
            }
            rule.runOnIdle { imeInset1 > 0 }
        }
        rule.runOnIdle { assertThat(imeInset2).isEqualTo(imeInset1) }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun enableConsumeEnablesAnimationConsumption() {
        rule.runOnUiThread {
            Assume.assumeTrue(
                rule.activity.resources.configuration.keyboardHidden !=
                    Configuration.KEYBOARDHIDDEN_YES
            )
        }
        var imeInset1 = 0
        var imeInset2 = 0

        val focusRequester = FocusRequester()

        rule.setContent {
            Column(Modifier.fillMaxSize()) {
                BasicTextField(
                    "Hello",
                    onValueChange = {},
                    Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { outerContext ->
                        ComposeView(outerContext).apply {
                            consumeWindowInsets = true
                            setContent {
                                imeInset1 = WindowInsets.ime.getBottom(LocalDensity.current)
                                Box(Modifier.fillMaxSize()) {
                                    AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { context ->
                                            ComposeView(context).apply {
                                                setContent {
                                                    imeInset2 =
                                                        WindowInsets.ime.getBottom(
                                                            LocalDensity.current
                                                        )
                                                    Box(
                                                        Modifier.fillMaxSize()
                                                            .background(Color.Cyan)
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }

        rule.waitForIdle()

        rule.waitUntil(timeoutMillis = 3000) {
            rule.runOnIdle {
                focusRequester.requestFocus()
                val controller = rule.activity.window.insetsController
                controller?.show(android.view.WindowInsets.Type.ime())
            }
            rule.runOnIdle { imeInset1 > 0 }
        }
        rule.runOnIdle { assertThat(imeInset2).isEqualTo(0) }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun defaultConsumption() {
        rule.runOnUiThread {
            Assume.assumeTrue(
                rule.activity.resources.configuration.keyboardHidden !=
                    Configuration.KEYBOARDHIDDEN_YES
            )
        }
        var imeInset1 = 0
        var imeInset2 = 0

        val focusRequester = FocusRequester()
        var defaultConsume: Boolean? = null

        rule.setContent {
            defaultConsume = (LocalView.current.parent as ComposeView).consumeWindowInsets
            imeInset1 = WindowInsets.ime.getBottom(LocalDensity.current)
            Column(Modifier.fillMaxSize()) {
                BasicTextField("Hello", onValueChange = {}, Modifier.focusRequester(focusRequester))
                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { context ->
                        ComposeView(context).apply {
                            setContent {
                                imeInset2 = WindowInsets.ime.getBottom(LocalDensity.current)
                                Box(Modifier.fillMaxSize().background(Color.Cyan))
                            }
                        }
                    },
                )
            }
        }

        rule.waitForIdle()

        assertThat(defaultConsume).isFalse()

        // Loop until the value changes.
        rule.waitUntil(timeoutMillis = 3000) {
            rule.runOnIdle {
                focusRequester.requestFocus()
                val controller = rule.activity.window.insetsController
                controller?.show(android.view.WindowInsets.Type.ime())
                Snapshot.sendApplyNotifications()
            }
            rule.runOnIdle { imeInset1 > 0 }
        }
        rule.runOnIdle { assertThat(imeInset2).isEqualTo(imeInset1) }
    }

    @Test
    fun insetsUsedAfterInitialComposition() {
        var useInsets by mutableStateOf(false)
        var systemBarsInsets by mutableStateOf(Insets.NONE)

        rule.setContent {
            val view = LocalView.current
            DisposableEffect(Unit) {
                // Ensure that the system bars are shown
                val window = rule.activity.window

                @Suppress("RedundantNullableReturnType") // nullable on some versions
                val controller: WindowInsetsControllerCompat? =
                    WindowCompat.getInsetsController(window, view)
                controller?.show(WindowInsetsCompat.Type.systemBars())
                onDispose {}
            }
            Box(Modifier.fillMaxSize()) {
                if (useInsets) {
                    val systemBars = WindowInsets.systemBars
                    val density = LocalDensity.current
                    val left = systemBars.getLeft(density, LayoutDirection.Ltr)
                    val top = systemBars.getTop(density)
                    val right = systemBars.getRight(density, LayoutDirection.Ltr)
                    val bottom = systemBars.getBottom(density)
                    systemBarsInsets = Insets.of(left, top, right, bottom)
                }
            }
        }

        rule.runOnIdle { useInsets = true }

        rule.runOnIdle { assertThat(systemBarsInsets).isNotEqualTo(Insets.NONE) }
    }

    @Test
    fun insetsAfterStopWatching() {
        var useInsets by mutableStateOf(true)
        var hasStatusBarInsets = false

        rule.setContent {
            val view = LocalView.current
            DisposableEffect(Unit) {
                // Ensure that the status bars are shown
                val window = rule.activity.window

                @Suppress("RedundantNullableReturnType") // nullable on some versions
                val controller: WindowInsetsControllerCompat? =
                    WindowCompat.getInsetsController(window, view)
                controller?.hide(WindowInsetsCompat.Type.statusBars())
                onDispose {}
            }
            Box(Modifier.fillMaxSize()) {
                if (useInsets) {
                    val statusBars = WindowInsets.statusBars
                    val density = LocalDensity.current
                    val left = statusBars.getLeft(density, LayoutDirection.Ltr)
                    val top = statusBars.getTop(density)
                    val right = statusBars.getRight(density, LayoutDirection.Ltr)
                    val bottom = statusBars.getBottom(density)
                    hasStatusBarInsets = left != 0 || top != 0 || right != 0 || bottom != 0
                }
            }
        }

        rule.waitForIdle()

        rule.waitUntil(1000) { !hasStatusBarInsets }

        // disable watching the insets
        rule.runOnIdle { useInsets = false }

        val statusBarsWatcher = StatusBarsShowListener()

        // show the insets while we're not watching
        rule.runOnIdle {
            ViewCompat.setOnApplyWindowInsetsListener(
                rule.activity.window.decorView,
                statusBarsWatcher,
            )
            @Suppress("RedundantNullableReturnType")
            val controller: WindowInsetsControllerCompat? =
                WindowCompat.getInsetsController(
                    rule.activity.window,
                    rule.activity.window.decorView,
                )
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }

        assertThat(statusBarsWatcher.latch.await(1, TimeUnit.SECONDS)).isTrue()

        // Now look at the insets
        rule.runOnIdle { useInsets = true }

        rule.runOnIdle { assertThat(hasStatusBarInsets).isTrue() }
    }

    @Test
    fun insetsAfterReattachingView() {
        var hasStatusBarInsets = false

        // hide the insets
        rule.runOnUiThread {
            @Suppress("RedundantNullableReturnType")
            val controller: WindowInsetsControllerCompat? =
                WindowCompat.getInsetsController(
                    rule.activity.window,
                    rule.activity.window.decorView,
                )
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        }

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                val statusBars = WindowInsets.statusBars
                val density = LocalDensity.current
                val left = statusBars.getLeft(density, LayoutDirection.Ltr)
                val top = statusBars.getTop(density)
                val right = statusBars.getRight(density, LayoutDirection.Ltr)
                val bottom = statusBars.getBottom(density)
                hasStatusBarInsets = left != 0 || top != 0 || right != 0 || bottom != 0
            }
        }

        rule.waitForIdle()

        rule.waitUntil(1000) { !hasStatusBarInsets }

        val contentView = rule.activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = contentView.children.first()

        // remove the view
        rule.runOnUiThread { contentView.removeView(composeView) }

        val statusBarsWatcher = StatusBarsShowListener()

        // show the insets while we're not watching
        rule.runOnUiThread {
            ViewCompat.setOnApplyWindowInsetsListener(
                rule.activity.window.decorView,
                statusBarsWatcher,
            )
            @Suppress("RedundantNullableReturnType")
            val controller: WindowInsetsControllerCompat? =
                WindowCompat.getInsetsController(
                    rule.activity.window,
                    rule.activity.window.decorView,
                )
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }

        assertThat(statusBarsWatcher.latch.await(1, TimeUnit.SECONDS)).isTrue()

        // Now add the view back again
        rule.runOnUiThread { contentView.addView(composeView) }

        rule.waitUntil(1000) { hasStatusBarInsets }
    }

    /** If we have setDecorFitsSystemWindows(false), there should be insets. */
    @Test
    fun insetsSetAtStart() {
        rule.runOnUiThread {
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            WindowCompat.getInsetsController(rule.activity.window, rule.activity.window.decorView)!!
                .show(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.systemBars())
        }
        var leftInset = 0
        var topInset = 0
        var rightInset = 0
        var bottomInset = 0

        rule.setContent {
            val insets = WindowInsets.safeContent
            leftInset = insets.getLeft(LocalDensity.current, LocalLayoutDirection.current)
            topInset = insets.getTop(LocalDensity.current)
            rightInset = insets.getRight(LocalDensity.current, LocalLayoutDirection.current)
            bottomInset = insets.getBottom(LocalDensity.current)
        }

        rule.waitForIdle()
        assertTrue(leftInset != 0 || topInset != 0 || rightInset != 0 || bottomInset != 0)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun startVisibilityNavigationBarsNotVisible() {
        val insetsType = WindowInsetsCompat.Type.navigationBars()
        hide(insetsType)
        val isVisible = mutableListOf<Boolean>()
        rule.setContent { isVisible += WindowInsets.areNavigationBarsVisible }
        rule.runOnIdle { assertThat(isVisible.first()).isFalse() }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun startVisibilityNavigationBarsVisible() {
        val insetsType = WindowInsetsCompat.Type.navigationBars()
        show(insetsType)
        val isVisible = mutableListOf<Boolean>()
        rule.setContent { isVisible += WindowInsets.areNavigationBarsVisible }
        rule.runOnIdle { assertThat(isVisible.first()).isTrue() }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun startVisibilityStatusBarsNotVisible() {
        val insetsType = WindowInsetsCompat.Type.statusBars()
        hide(insetsType)
        val isVisible = mutableListOf<Boolean>()
        rule.setContent { isVisible += WindowInsets.areStatusBarsVisible }
        rule.runOnIdle { assertThat(isVisible.first()).isFalse() }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun startVisibilityStatusBarsVisible() {
        val insetsType = WindowInsetsCompat.Type.statusBars()
        show(insetsType)
        val isVisible = mutableListOf<Boolean>()
        rule.setContent { isVisible += WindowInsets.areStatusBarsVisible }
        rule.runOnIdle { assertThat(isVisible.first()).isTrue() }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun startVisibilityImeNotVisible() {
        val insetsType = WindowInsetsCompat.Type.ime()
        hide(insetsType)
        val isVisible = mutableListOf<Boolean>()
        rule.setContent { isVisible += WindowInsets.isImeVisible }
        rule.runOnIdle { assertThat(isVisible.first()).isFalse() }
    }

    private fun hide(insetsType: Int) {
        rule.runOnUiThread {
            val window = rule.activity.window
            WindowCompat.getInsetsController(window, window.decorView).hide(insetsType)
        }
        // There could be an animation, so wait for the insets to disappear
        rule.waitUntil {
            rule.runOnIdle {
                val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
                insets?.isVisible(insetsType) == false
            }
        }
    }

    private fun show(insetsType: Int) {
        rule.runOnUiThread {
            val window = rule.activity.window
            WindowCompat.getInsetsController(window, window.decorView).show(insetsType)
        }
        // There could be an animation, so wait for the insets to appear
        rule.waitUntil {
            rule.runOnIdle {
                val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
                insets?.isVisible(insetsType) == true
            }
        }
    }

    class StatusBarsShowListener : OnApplyWindowInsetsListener {
        val latch = CountDownLatch(1)

        override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            if (statusBars != Insets.NONE) {
                latch.countDown()
                ViewCompat.setOnApplyWindowInsetsListener(v, null)
            }
            return insets
        }
    }
}
