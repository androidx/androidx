/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.samples.activitypanel

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.xr.scenecore.samples.commontestview.DebugTextLinearView
import java.util.Timer
import java.util.TimerTask

class CounterActivity : AppCompatActivity() {
    var counter = 0
    private val TAG = "CounterActivity"
    private val timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = DebugTextLinearView(this)
        view.setName("Secondary Counter Activity")
        setContentView(view)

        // Schedule a timer to update the counter every second
        Log.i(TAG, "Counter Activity timer scheduled")
        timer.schedule(createTimerTask(view), 0, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Counter Activity onDestroy called")
        timer.cancel()
        timer.purge()
    }

    // Timer task that increments a counter and updates the view.
    private fun createTimerTask(view: DebugTextLinearView) =
        object : TimerTask() {
            override fun run() {
                counter++
                view.setLine("counter", counter.toString())
            }
        }
}
