/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import android.app.Activity
import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.CompositionContext
import androidx.ui.core.composeIntoActivity
import androidx.test.rule.ActivityTestRule
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface

fun ComposeMaterialIntoActivity(
    activity: Activity,
    composable: @Composable() () -> Unit
): CompositionContext? {
    return composeIntoActivity(activity) {
        MaterialTheme {
            Surface {
                composable()
            }
        }
    }
}

fun <T : Activity> ActivityTestRule<T>.runOnUiThreadSync(action: () -> Unit) {
    // Workaround for lambda bug in IR
    runOnUiThread(object : Runnable {
        override fun run() {
            action.invoke()
        }
    })
}

fun Activity.runOnUiThreadSync(action: () -> Unit) {
    // Workaround for lambda bug in IR
    runOnUiThread(object : Runnable {
        override fun run() {
            action.invoke()
        }
    })
}