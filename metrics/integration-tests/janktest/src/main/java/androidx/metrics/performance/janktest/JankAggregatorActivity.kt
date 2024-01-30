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

package androidx.metrics.performance.janktest

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.PerformanceMetricsState
import androidx.metrics.performance.janktest.databinding.ActivityMainBinding
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController

/**
 * This activity shows how to use JankStatsAggregator, a class in this test directory layered
 * on top of JankStats which aggregates the per-frame data. Instead of receiving jank data
 * per frame (which would happen by using JankStats directly), the report listener only
 * receives data when a report is issued, either when the activity goes into the background
 * or if JankStatsAggregator issues the report itself.
 */
class JankAggregatorActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var jankStatsAggregator: JankStatsAggregator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(binding.root)
        jankStatsAggregator = JankStatsAggregator(window, jankReportListener)
        metricsStateHolder.state?.putState("Activity", javaClass.simpleName)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    object jankReportListener : JankStatsAggregator.OnJankReportListener {

        override fun onJankReport(
            reason: String,
            totalFrames: Int,
            jankFrameData: List<FrameData>
        ) {
            println("Jank Report ($reason), totalFrames = $totalFrames, " +
                "jankFrames = ${jankFrameData.size}")
            for (frameData in jankFrameData) {
                println("$frameData")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        jankStatsAggregator.jankStats.isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        jankStatsAggregator.issueJankReport("Activity paused")
        jankStatsAggregator.jankStats.isTrackingEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
