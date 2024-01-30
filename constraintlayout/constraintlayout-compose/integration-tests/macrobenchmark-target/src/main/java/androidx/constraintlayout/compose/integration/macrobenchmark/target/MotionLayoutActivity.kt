/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.integration.macrobenchmark.target

import android.os.Bundle
import android.view.Choreographer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.constraintlayout.compose.integration.macrobenchmark.target.graphs.DynamicGraphsPreview
import androidx.constraintlayout.compose.integration.macrobenchmark.target.newmessage.NewMotionMessagePreview
import androidx.constraintlayout.compose.integration.macrobenchmark.target.newmessage.NewMotionMessagePreviewWithDsl
import androidx.constraintlayout.compose.integration.macrobenchmark.target.newmessage.NewMotionMessagePreviewWithDslOptimized
import androidx.constraintlayout.compose.integration.macrobenchmark.target.toolbar.MotionCollapseToolbarPreview

class MotionLayoutActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("ComposableName")
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        // Required to reference UI elements by Macrobenchmark
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colors.background
                ) {
                    // Here we resolve the Composable requested by Macrobenchark
                    when (name) {
                        "NewMessageJson" -> {
                            NewMotionMessagePreview()
                        }

                        "NewMessageDsl" -> {
                            NewMotionMessagePreviewWithDsl()
                        }

                        "OptimizedNewMessageDsl" -> {
                            NewMotionMessagePreviewWithDslOptimized()
                        }

                        "CollapsibleToolbar" -> {
                            MotionCollapseToolbarPreview()
                        }

                        "DynamicGraphs" -> {
                            DynamicGraphsPreview()
                        }

                        else -> {
                            throw IllegalArgumentException("No Composable with name: $name")
                        }
                    }
                }
            }
        }
        launchIdlenessTracking()
    }

    // Copied from LazyColumnActivity.kt
    private fun ComponentActivity.launchIdlenessTracking() {
        val contentView: View = findViewById(android.R.id.content)
        val callback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (Recomposer.runningRecomposers.value.any { it.hasPendingWork }) {
                    contentView.contentDescription = "COMPOSE-BUSY"
                } else {
                    contentView.contentDescription = "COMPOSE-IDLE"
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
    }
}
