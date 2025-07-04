/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.integration.demos.common.ActivityDemo
import androidx.wear.compose.integration.demos.common.Demo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material3.demos.RobotoFlexTypography
import java.io.FileNotFoundException

/** Main [Activity] for Wear Compose related demos. */
class DemoActivity : ComponentActivity() {
    lateinit var hostView: View
    lateinit var focusManager: FocusManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recents = RecentsHandler()

        ComposeView(this)
            .also { setContentView(it) }
            .setContent {
                // Load & save recents as needed when apps got to background/foreground
                recents.Persist()
                val allDemos = recents.addRecentsMenu(WearComposeDemos)

                hostView = LocalView.current
                focusManager = LocalFocusManager.current
                val activityStarter =
                    fun(demo: ActivityDemo<*>) {
                        startActivity(Intent(this, demo.activityClass.java))
                    }
                val navigator =
                    rememberSaveable(
                        saver = Navigator.Saver(allDemos, onBackPressedDispatcher, activityStarter)
                    ) {
                        Navigator(allDemos, onBackPressedDispatcher, activityStarter)
                    }
                MaterialTheme {
                    androidx.wear.compose.material3.MaterialTheme(
                        typography = RobotoFlexTypography
                    ) {
                        DemoApp(
                            currentDemo = navigator.currentDemo,
                            parentDemo = navigator.parentDemo,
                            onNavigateTo = { demo ->
                                recents.addDemoToRecents(demo)
                                navigator.navigateTo(demo)
                            },
                            onNavigateBack = {
                                if (!navigator.navigateBack()) {
                                    ActivityCompat.finishAffinity(this)
                                }
                            },
                        )
                    }
                }
            }
    }
}

private class RecentsHandler() {
    private val recents = mutableListOf<Demo>()

    fun addRecentsMenu(demos: DemoCategory) =
        DemoCategory(demos.title, listOf(DemoCategory("Recents", recents)) + demos.demos)

    fun addDemoToRecents(demo: Demo) {
        if (demo.title == "Recents") return // :)
        recents.indexOf(demo).let { if (it >= 0) recents.removeAt(it) }
        recents.add(0, demo)
        while (recents.size > 20) recents.removeAt(20)
    }

    @Composable
    fun Persist() {
        val context = LocalContext.current

        load(context)
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> save(context)
                Lifecycle.Event.ON_RESUME -> load(context)
                else -> {} // Nothing else matters
            }
        }

        DisposableEffect(Unit) {
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }

    private val STATE_FNAME = "recents.txt"

    private fun load(context: Context) {
        try {
            val data = context.openFileInput(STATE_FNAME).use { it.readBytes() }
            recents.clear()
            String(data).split("\n").forEach {
                findDemo(WearComposeDemos, it)?.let { demo -> recents.add(demo) }
            }
        } catch (e: FileNotFoundException) {
            // Can happen on first run.
        }
    }

    private fun save(context: Context) {
        context.openFileOutput(STATE_FNAME, Context.MODE_PRIVATE).use {
            it.write(recents.joinToString(separator = "\n") { it.title }.toByteArray())
        }
    }
}

private class Navigator
private constructor(
    private val backDispatcher: OnBackPressedDispatcher,
    private val launchActivityDemo: (ActivityDemo<*>) -> Unit,
    initialDemo: Demo,
    private val backStack: MutableList<Demo>,
) {
    constructor(
        rootDemo: Demo,
        backDispatcher: OnBackPressedDispatcher,
        launchActivityDemo: (ActivityDemo<*>) -> Unit,
    ) : this(backDispatcher, launchActivityDemo, rootDemo, mutableListOf<Demo>())

    private val onBackPressed =
        object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            }
            .apply {
                isEnabled = !isRoot
                backDispatcher.addCallback(this)
            }

    private var _currentDemo by mutableStateOf(initialDemo)
    var currentDemo: Demo
        get() = _currentDemo
        private set(value) {
            _currentDemo = value
            onBackPressed.isEnabled = !isRoot
        }

    val parentDemo: Demo?
        get() = backStack.lastOrNull()

    val isRoot: Boolean
        get() = backStack.isEmpty()

    fun navigateTo(demo: Demo) {
        if (demo is ActivityDemo<*>) {
            launchActivityDemo(demo)
        } else {
            backStack.add(currentDemo)
            currentDemo = demo
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            currentDemo = backStack.removeAt(backStack.lastIndex)
            return true
        } else {
            return false
        }
    }

    companion object {
        fun Saver(
            rootDemo: DemoCategory,
            backDispatcher: OnBackPressedDispatcher,
            launchActivityDemo: (ActivityDemo<*>) -> Unit,
        ): Saver<Navigator, *> =
            listSaver<Navigator, String>(
                save = { navigator ->
                    (navigator.backStack + navigator.currentDemo).map { it.title }
                },
                restore = { restored ->
                    require(restored.isNotEmpty()) { "restored demo is empty" }
                    val backStack =
                        restored.mapTo(mutableListOf()) {
                            requireNotNull(findDemo(rootDemo, it)) { "No root demo" }
                        }
                    val initial = backStack.removeAt(backStack.lastIndex)
                    Navigator(backDispatcher, launchActivityDemo, initial, backStack)
                },
            )
    }
}

private fun findDemo(demo: Demo, title: String): Demo? {
    if (demo.title == title) {
        return demo
    }
    if (demo is DemoCategory) {
        demo.demos.forEach { child ->
            findDemo(child, title)?.let {
                return it
            }
        }
    }
    return null
}
