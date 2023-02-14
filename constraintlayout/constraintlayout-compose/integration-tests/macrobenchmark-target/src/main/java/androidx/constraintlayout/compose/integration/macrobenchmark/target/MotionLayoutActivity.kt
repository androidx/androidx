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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.integration.macrobenchmark.target.motionlayout.newmessage.NewMotionMessagePreview
import androidx.compose.integration.macrobenchmark.target.motionlayout.newmessage.NewMotionMessagePreviewWithDsl
import androidx.compose.integration.macrobenchmark.target.motionlayout.toolbar.MotionCollapseToolbarPreview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

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
                        "CollapsibleToolbar" -> {
                            MotionCollapseToolbarPreview()
                        }
                        else -> {
                            throw IllegalArgumentException("No Composable with name: $name")
                        }
                    }
                }
            }
        }
    }
}