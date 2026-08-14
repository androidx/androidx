/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.integration.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.MaterialTheme
import kotlinx.coroutines.delay

data class StaticSampleItem(
    val name: String,
    val isBox: Boolean,
    val content: @Composable () -> Unit,
)

class ScreenshotActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val allSamples =
            tlcScreenshotRegistry.map { (name, content) ->
                StaticSampleItem(name, isBox = false, content)
            } +
                boxScreenshotRegistry.map { (name, content) ->
                    StaticSampleItem(name, isBox = true, content)
                }

        setContent {
            MaterialTheme {
                if (allSamples.isNotEmpty()) {
                    ScreenshotRunner(allSamples, this@ScreenshotActivity)
                } else {
                    Log.i("ScreenshotSystem", "FINISHED")
                }
            }
        }
    }
}

@Composable
fun ScreenshotRunner(samples: List<StaticSampleItem>, context: Context) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentSample = samples[currentIndex]

    // Advance to the next sample when we receive the broadcast from Python
    DisposableEffect(Unit) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (currentIndex < samples.size - 1) {
                        currentIndex++
                    } else {
                        Log.i("ScreenshotSystem", "FINISHED")
                    }
                }
            }
        val filter = IntentFilter("androidx.wear.compose.integration.media.NEXT_SAMPLE")
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // When the sample changes, wait for it to stabilize then announce to Logcat (with buffer flush)
    LaunchedEffect(currentSample) {
        delay(3000)
        Log.i("ScreenshotSystem", "SAMPLE_READY:${currentSample.name}" + " ".repeat(4096))
    }

    // Render the actual sample in its designated container with the base black watch background
    val backgroundModifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    if (currentSample.isBox) {
        Box(modifier = backgroundModifier, contentAlignment = Alignment.Center) {
            currentSample.content.invoke()
        }
    } else {
        TransformingLazyColumn(
            modifier = backgroundModifier,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            item { currentSample.content.invoke() }
        }
    }
}
