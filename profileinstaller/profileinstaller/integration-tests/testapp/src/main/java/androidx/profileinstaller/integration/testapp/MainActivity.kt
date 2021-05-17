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

package androidx.profileinstaller.integration.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

val LotsOfItems = Array<String>(5_000) { position ->
    "A very long list item, #$position"
}

class ProfileWatcher {
    var profilePresent by mutableStateOf(false)
        private set

    suspend fun pollForProfiles() {
        withContext(Dispatchers.IO) {
            while (!profilePresent) {
                profilePresent = checkProfile()
                delay(100)
            }
        }
    }

    private fun checkProfile(): Boolean {
        val file = File(
            "/data/misc/profiles/cur/0/" +
                "androidx.profileinstaller.integration.testapp/primary.prof"
        )
        return file.exists()
    }
}

class MainActivity : ComponentActivity() {
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val profileState = remember { ProfileWatcher() }
            val lazyState = rememberLazyListState()
            LaunchedEffect(Unit) {
                launch { profileState.pollForProfiles() }
                while (true) {
                    lazyState.animateScrollToItem(LotsOfItems.size - 1)
                    lazyState.animateScrollToItem(0)
                    delay(100)
                }
            }
            LazyColumn(Modifier.fillMaxSize(1f), state = lazyState) {
                stickyHeader {
                    if (!profileState.profilePresent) {
                        Text(
                            "⏳ waiting for profile",
                            Modifier.background(Color.Yellow)
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    } else {
                        Text(
                            "\uD83C\uDF89 profile installed",
                            Modifier.background(Color.Gray)
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    }
                }
                items(LotsOfItems) { item ->
                    Text(item, Modifier.fillMaxWidth().height(100.dp))
                }
            }
        }
    }
}