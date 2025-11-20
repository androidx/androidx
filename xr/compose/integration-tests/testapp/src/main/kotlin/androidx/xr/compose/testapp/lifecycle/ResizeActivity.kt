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

package androidx.xr.compose.testapp.lifecycle

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.testapp.common.composables.BasicLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * This test checks how an activity in Home Space Mode reacts to a reconfiguration event
 * Simulates resizing an app and checks for activity to restart
 */

class ResizeActivity : BaseLifecycleTestActivity() {
    private val activityName = "ResizeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ResizeContent(this) }
    }

    @Composable
    fun ResizeContent(activity: ResizeActivity) {
        var width by rememberSaveable { mutableIntStateOf(0) }
        var height by rememberSaveable { mutableIntStateOf(0) }
        val size = IntSize(width, height)
        var launched by rememberSaveable { mutableStateOf(false) }
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val lifecycleEvents = remember { mutableStateOf<List<String>>(emptyList()) }

        LaunchedEffect(Unit) {
            val params = activity.window.attributes
            scope.launch {
                if (!launched) {
                    delay(3000) // Delay to ensure the activity is fully launched
                    val newWidthPx = (1000 * density.density).toInt()
                    val newHeightPx = (500 * density.density).toInt()
                    params.width = newWidthPx
                    params.height = newHeightPx
                    activity.window.attributes = params
                    width = newWidthPx
                    height = newHeightPx
                    launched = true
                    Log.i(TAG, "[$activityName] resized. Size set to $width x $height")
                    activity.recreate()
                } else {
                    params.width = width
                    params.height = height
                    activity.window.attributes = params
                    lifecycleEvents.value = LifecycleDataStore.readLifecycleEvents(activity)
                    if (runAutomated) {
                        delay(3000)
                        finish()
                    }
                }
            }
        }

        Column(
            modifier =
                Modifier.fillMaxSize()
                    .onSizeChanged { newSize ->
                        width = newSize.width
                        height = newSize.height
                    }
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
        ) {
            BasicLayout("Resize Activity") {
                Text(
                    modifier = Modifier.testTag("size_text"),
                    text = "Size: ${size.width}px x ${size.height}px",
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Density: ${density.density}",
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (!launched) {
                    Text(
                        modifier = Modifier.padding(vertical = 12.dp),
                        text = "This window will automatically resized...",
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (lifecycleEvents.value.isNotEmpty()) {
                    LifecycleEventComparator(
                        activityName = activity.localClassName,
                        expectedEvents = ExpectedLifecycleEvents.RESIZE,
                        actualEvents = lifecycleEvents.value,
                    )
                }
            }
        }
    }
}
