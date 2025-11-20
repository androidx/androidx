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

package androidx.metrics.performance.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController

open class DelayedActivity : AppCompatActivity() {

    var delayMs: Long
        get() {
            val delayedView = findViewById<DelayedView>(R.id.delayedView)
            return delayedView?.delayMs ?: 0
        }
        set(value) {
            val delayedView = findViewById<DelayedView>(R.id.delayedView)
            delayedView?.delayMs = value
        }

    var maxReps: Int
        get() {
            val delayedView = findViewById<DelayedView>(R.id.delayedView)
            return delayedView?.maxReps ?: 0
        }
        set(value) {
            val delayedView = findViewById<DelayedView>(R.id.delayedView)
            delayedView?.maxReps = value
        }

    var repetitions: Int
        get() {
            val delayedView = findViewById<DelayedView>(R.id.delayedView)
            return delayedView?.repetitions ?: 0
        }
        set(value) {
            val delayedView = findViewById<DelayedView>(R.id.delayedView)
            delayedView?.repetitions = value
        }

    fun invalidate() {
        val delayedView = findViewById<DelayedView>(R.id.delayedView)
        delayedView?.invalidate()
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.delayed_activity_main)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
