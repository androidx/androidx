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

package androidx.compose.remote.integration.view.demos

// Busting Gradle Build Cache for Native Custom Component Delegates!
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.remote.integration.view.demos.customCompose.SupportSlider
import androidx.compose.remote.integration.view.demos.customCompose.SupportText
import androidx.compose.remote.integration.view.demos.utils.RCDoc
import androidx.compose.remote.player.compose.custom.ComposeCustomSupport
import androidx.compose.remote.player.compose.impl.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApiAndroidX")
public class ExperimentComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val docList =
            DemosCreation.getDemos(this, 1).apply {

                // addAll(getRemoteComposable(this@ExperimentComposeActivity))

                sortBy { if (it.toString().contains("ComposeDemo")) "0" else it.toString() }
            }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF222222)) {
                    ExperimentComposeScreen(docList)
                }
            }
        }
    }
}

@Composable
@SuppressLint("RestrictedApiAndroidX")
public fun ExperimentComposeScreen(docList: List<RCDoc>) {
    val compositionContext = rememberCompositionContext()
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    @SuppressLint("FrequentlyChangingValue")
    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentIndex = listState.firstVisibleItemIndex
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Control Bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = {
                    if (currentIndex > 0) {
                        val nextIdx = currentIndex - 1
                        coroutineScope.launch { listState.animateScrollToItem(nextIdx) }
                    }
                }
            ) {
                Text("<")
            }

            Text(
                text = "${currentIndex + 1} / ${docList.size}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Button(
                onClick = { isLocked = !isLocked },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = if (isLocked) Color(0xFFFFAAAA) else Color(0xFFAAFFAA),
                        contentColor = Color.Black,
                    ),
            ) {
                Text(if (isLocked) "Lock ON" else "Lock OFF", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (currentIndex < docList.size - 1) {
                        val nextIdx = currentIndex + 1
                        coroutineScope.launch { listState.animateScrollToItem(nextIdx) }
                    }
                }
            ) {
                Text(">")
            }
        }

        // Active Document Title
        val currentDoc = docList.getOrNull(currentIndex)
        Text(
            text = currentDoc?.toString() ?: "No Document",
            color = Color.Cyan,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(12.dp).clickable { isLocked = !isLocked },
        )

        // Document Player Row
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            userScrollEnabled = !isLocked,
        ) {
            itemsIndexed(docList) { _, docItem ->
                Box(
                    modifier =
                        Modifier.fillParentMaxSize().padding(16.dp).background(Color(0xFF444444))
                ) {
                    val remoteDocument = remember(docItem) { docItem.doc }
                    val customSupport =
                        remember(docItem) {
                            ComposeCustomSupport().apply {
                                addDelegate("TextView") { state, remoteContext ->
                                    SupportText.Content(state, remoteContext)
                                }
                                addDelegate("Text") { state, remoteContext ->
                                    SupportText.Content(state, remoteContext)
                                }
                                addDelegate("ProgressBar") { state, remoteContext ->
                                    SupportSlider.Content(state, remoteContext)
                                }
                                addDelegate("Slider") { state, remoteContext ->
                                    SupportSlider.Content(state, remoteContext)
                                }
                            }
                        }
                    if (remoteDocument != null) {
                        RemoteComposePlayer(
                            document = remoteDocument,
                            modifier = Modifier.fillMaxSize(),
                            customSupport = customSupport,
                        )
                    }
                }
            }
        }
    }
}
