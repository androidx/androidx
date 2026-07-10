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

package androidx.wear.compose.navigation3.demos

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme

class Navigation3DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasBackstack = intent.extras?.getBoolean(EXTRA_HAS_BACKSTACK, true) ?: true

        setContent {
            MaterialTheme {
                SwipeDismissableSceneStrategyDemo(hasBackstack) {
                    if (hasBackstack) startActivityWithPopBackstack() else finish()
                }
            }
        }
    }

    fun startActivityWithPopBackstack() {
        // Start this activity again as new task, but don't maintain backstack when navigating to
        // the target screen. When swiping back from the target screen, the new activity will be
        // closed, and we'll always return to the main menu ("Screen 1") on an initial activity.
        // The predictive back gesture we see when swiping back is handled by the system for
        // activity transition.
        startActivity(
            Intent(this@Navigation3DemoActivity, Navigation3DemoActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_HAS_BACKSTACK, false)
            }
        )
    }
}

internal const val EXTRA_HAS_BACKSTACK = "extra_has_backstack"
