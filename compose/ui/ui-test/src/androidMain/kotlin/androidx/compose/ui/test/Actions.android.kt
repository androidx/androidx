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
@file:JvmName("AndroidActions")

package androidx.compose.ui.test

import androidx.compose.ui.platform.ViewRootForTest

internal actual fun SemanticsNodeInteraction.performClickImpl(): SemanticsNodeInteraction {
    return performTouchInput { click() }
}

@Suppress("DocumentExceptions") // Documented in expect fun
actual fun SemanticsNodeInteraction.tryPerformAccessibilityChecks(): SemanticsNodeInteraction {
    testContext.platform.accessibilityValidator?.let { av ->
        testContext.testOwner
            .getRoots(true)
            .map {
                // We're on Android, so we're guaranteed a ViewRootForTest
                (it as ViewRootForTest).view.rootView
            }
            .distinct()
            .run {
                // Synchronization needs to happen off the UI thread, so only switch to the UI
                // thread after the call to getRoots, but before the call to forEach so we don't
                // have to switch thread for each root view.
                testContext.testOwner.runOnUiThread { forEach { av.check(it) } }
            }
    }
    return this
}
