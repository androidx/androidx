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

package androidx.xr.glimmer.demos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.launch

/**
 * The main activity containing all Jetpack Compose Glimmer related demos.
 *
 * If there is a connected device, when this activity is created the first time, it will attempt to
 * automatically launch [ProjectedDemoActivity] on the connected device.
 */
class DemoActivity : BaseDemoActivity() {

    private var hasLaunchedProjectedActivity = false

    @OptIn(ExperimentalProjectedApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isFirstOnCreate = savedInstanceState == null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    ProjectedContext.isProjectedDeviceConnected(this@DemoActivity, coroutineContext)
                        .collect { isConnected ->
                            // Launch the projected activity if there is a connected device.
                            if (isConnected && isFirstOnCreate && !hasLaunchedProjectedActivity) {
                                launchProjectedActivity()
                                hasLaunchedProjectedActivity = true
                            }
                        }
                }
            }
        }
    }

    @OptIn(ExperimentalProjectedApi::class)
    private fun launchProjectedActivity() {
        val options = ProjectedContext.createProjectedActivityOptions(this)
        val intent =
            Intent(this, ProjectedDemoActivity::class.java).apply {
                // Prevent stacking multiple instances
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        startActivity(intent, options.toBundle())
    }
}
