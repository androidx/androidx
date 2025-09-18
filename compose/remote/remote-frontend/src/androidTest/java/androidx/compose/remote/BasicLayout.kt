/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.frontend.action.HostAction
import androidx.compose.remote.frontend.action.ValueChange
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.rememberAsyncRemoteDocument
import androidx.compose.remote.frontend.capture.rememberRemoteDocument
import androidx.compose.remote.frontend.capture.rotate
import androidx.compose.remote.frontend.capture.scale
import androidx.compose.remote.frontend.capture.shaders.RemoteBrush
import androidx.compose.remote.frontend.capture.shaders.radialGradient
import androidx.compose.remote.frontend.capture.translate
import androidx.compose.remote.frontend.layout.Alignment
import androidx.compose.remote.frontend.layout.Arrangement
import androidx.compose.remote.frontend.layout.Box
import androidx.compose.remote.frontend.layout.CaptureAsBitmap
import androidx.compose.remote.frontend.layout.RemoteBox
import androidx.compose.remote.frontend.layout.RemoteCanvas
import androidx.compose.remote.frontend.layout.RemoteColumn
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.frontend.layout.RemoteContext
import androidx.compose.remote.frontend.layout.RemoteRow
import androidx.compose.remote.frontend.layout.RemoteText
import androidx.compose.remote.frontend.layout.Row
import androidx.compose.remote.frontend.layout.StateLayout
import androidx.compose.remote.frontend.layout.createIds
import androidx.compose.remote.frontend.layout.rememberStateMachine
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.background
import androidx.compose.remote.frontend.modifier.drawWithContent
import androidx.compose.remote.frontend.modifier.fillMaxHeight
import androidx.compose.remote.frontend.modifier.fillMaxSize
import androidx.compose.remote.frontend.modifier.fillMaxWidth
import androidx.compose.remote.frontend.modifier.height
import androidx.compose.remote.frontend.modifier.onClick
import androidx.compose.remote.frontend.modifier.onTouchCancel
import androidx.compose.remote.frontend.modifier.onTouchDown
import androidx.compose.remote.frontend.modifier.onTouchUp
import androidx.compose.remote.frontend.modifier.padding
import androidx.compose.remote.frontend.modifier.size
import androidx.compose.remote.frontend.modifier.toComposeUi
import androidx.compose.remote.frontend.modifier.width
import androidx.compose.remote.frontend.state.rememberRemoteIntValue
import androidx.compose.remote.frontend.state.rememberRemoteString
import androidx.compose.remote.player.view.RemoteComposeDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.remote.serialization.yaml.YAMLSerializer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(AndroidJUnit4::class)
class LayoutTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Composable
    fun rememberRemoteDocumentFixedDensity(
        content: @Composable () -> Unit
    ): MutableState<CoreDocument?> {
        val customDensity = Density(density = 2.75f, fontScale = 1.0f)
        return rememberRemoteDocument {
            CompositionLocalProvider(LocalDensity provides customDensity) { content() }
        }
    }

    @Composable
    fun rememberAsyncRemoteDocumentFixedDensity(
        content: @Composable (MutableState<Boolean>) -> Unit
    ): MutableState<CoreDocument?> {
        val customDensity = Density(density = 2.75f, fontScale = 1.0f)
        return rememberAsyncRemoteDocument {
            CompositionLocalProvider(LocalDensity provides customDensity) { content(it) }
        }
    }

    fun testLayout(result: String, content: @Composable @RemoteComposable () -> Unit) {
        composeTestRule.setContent {
            val customDensity = Density(density = 2.75f, fontScale = 1.0f)
            CompositionLocalProvider(LocalDensity provides customDensity) {
                val doc = rememberRemoteDocumentFixedDensity { content() }
                Column {
                    var documentWidth by remember { mutableStateOf(300) }
                    var documentHeight by remember { mutableStateOf(300) }
                    var documentContent = remember { mutableStateOf("") }
                    var docu = remember(doc.value) { mutableStateOf<RemoteComposeDocument?>(null) }

                    if (doc.value != null) {
                        docu.value = RemoteComposeDocument(doc.value!!)
                    }
                    AndroidView(
                        modifier =
                            androidx.compose.ui.Modifier.size(documentWidth.dp, documentHeight.dp),
                        factory = {
                            val player = RemoteComposePlayer(it)
                            player
                        },
                        update = {
                            if (docu.value != null && it.document != docu.value) {
                                it.setDocument(docu.value!!)
                                it.setUseChoreographer(false)
                            }
                        },
                    )

                    Text(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Document"
                            },
                        text = documentContent.value,
                    )

                    Button(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Button"
                            },
                        onClick = {
                            documentWidth = 400
                            documentHeight = 350
                        },
                    ) {}

                    Button(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Update"
                            },
                        onClick = {
                            documentContent.value = "\n${docu.value?.document?.displayHierarchy()}"
                        },
                    ) {}
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Update").performClick()
        composeTestRule.waitForIdle()
        val content = composeTestRule.onNodeWithContentDescription("Document")
        content.assertTextMatches(result)
    }

    fun testLayoutAndPaint(
        layoutResult: String,
        drawResult: String,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        composeTestRule.setContent {
            val customDensity = Density(density = 2.75f, fontScale = 1.0f)
            CompositionLocalProvider(LocalDensity provides customDensity) {
                val doc = rememberRemoteDocumentFixedDensity { content() }
                Column {
                    var documentWidth by remember { mutableStateOf(300) }
                    var documentHeight by remember { mutableStateOf(300) }
                    var documentContent = remember { mutableStateOf("") }
                    var docu = remember(doc.value) { mutableStateOf<RemoteComposeDocument?>(null) }

                    if (doc.value != null) {
                        docu.value = RemoteComposeDocument(doc.value!!)
                    }
                    AndroidView(
                        modifier =
                            androidx.compose.ui.Modifier.size(documentWidth.dp, documentHeight.dp),
                        factory = {
                            val player = RemoteComposePlayer(it)
                            player
                        },
                        update = {
                            if (docu.value != null && it.document != docu.value) {
                                it.setDocument(docu.value!!)
                                it.setUseChoreographer(false)
                            }
                        },
                    )

                    Text(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Document"
                            },
                        text = documentContent.value,
                    )

                    Button(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Button"
                            },
                        onClick = {
                            documentWidth = 400
                            documentHeight = 350
                        },
                    ) {}

                    Button(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Update"
                            },
                        onClick = {
                            documentContent.value = "\n${docu.value?.document?.displayHierarchy()}"
                        },
                    ) {}

                    Button(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "GetDoc"
                            },
                        onClick = {
                            val serializer = YAMLSerializer()
                            val root = docu.value?.document?.rootLayoutComponent
                            if (root != null) {
                                root.serialize(serializer.serializeMap())
                                val result = serializer.toString()
                                documentContent.value = "\n$result"
                            }
                        },
                    ) {}
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Update").performClick()
        composeTestRule.waitForIdle()
        val content = composeTestRule.onNodeWithContentDescription("Document")
        content.assertTextMatches(layoutResult)

        composeTestRule.onNodeWithContentDescription("GetDoc").performClick()
        composeTestRule.waitForIdle()
        val contentPaint = composeTestRule.onNodeWithContentDescription("Document")
        contentPaint.assertTextMatches(drawResult)
    }

    fun testAsyncLayout(
        result: String,
        content: @Composable @RemoteComposable (MutableState<Boolean>) -> Unit,
    ) {
        composeTestRule.setContent {
            val customDensity = Density(density = 2.75f, fontScale = 1.0f)
            CompositionLocalProvider(LocalDensity provides customDensity) {
                val doc = rememberAsyncRemoteDocumentFixedDensity { content(it) }
                Column {
                    var documentWidth by remember { mutableStateOf(300) }
                    var documentHeight by remember { mutableStateOf(300) }
                    var documentContent = remember { mutableStateOf("") }
                    var docu = remember(doc.value) { mutableStateOf<RemoteComposeDocument?>(null) }

                    if (doc.value != null) {
                        docu.value = RemoteComposeDocument(doc.value!!)
                    }
                    AndroidView(
                        modifier =
                            androidx.compose.ui.Modifier.size(documentWidth.dp, documentHeight.dp),
                        factory = {
                            val player = RemoteComposePlayer(it)
                            player
                        },
                        update = {
                            if (docu.value != null && it.document != docu.value) {
                                it.setDocument(docu.value!!)
                            }
                        },
                    )

                    Text(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Document"
                            },
                        text = documentContent.value,
                    )

                    Button(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Button"
                            },
                        onClick = {
                            documentWidth = 400
                            documentHeight = 350
                        },
                    ) {}

                    Button(
                        modifier =
                            androidx.compose.ui.Modifier.semantics {
                                contentDescription = "Update"
                            },
                        onClick = {
                            documentContent.value =
                                "\n${docu.value?.document?.rootLayoutComponent?.displayHierarchy()}"
                        },
                    ) {}
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Update").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Document").assertTextMatches(result)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testLayoutAndValues() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    CANVAS [-5:-1] = [0.0, 275.0, 825.0, 275.0] VISIBLE
      MODIFIERS
        HEIGHT = 275.0
        BACKGROUND = [0.0, 0.0, 825.0, 275.0] color [1.0, 1.0, 1.0, 1.0] shape [0]
        PADDING = [22.0, 22.0, 22.0, 22.0]
        BACKGROUND = [0.0, 0.0, 781.0, 231.0] color [0.8, 0.8, 0.8, 1.0] shape [0]
      CANVAS_CONTENT [-7:-1] = [0.0, 0.0, 781.0, 231.0] VISIBLE
        ComponentValue value 43 set to WIDTH of Component -7
        ComponentValue value 44 set to HEIGHT of Component -7
        DrawLine(0.0, 0.0, [43 = 781.0], [44 = 231.0])
        DrawLine(0.0, [44 = 231.0], [43 = 781.0], 0.0)
"""
        testLayout(result) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.Yellow),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RemoteCanvas(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(100.dp)
                            .background(Color.White)
                            .padding(8.dp)
                            .background(Color.LightGray)
                ) {
                    val color = Color.Blue
                    val w = remote.component.width
                    val h = remote.component.height
                    val topLeft = Offset(0f, 0f)
                    val topRight = Offset(w.internalAsFloat(), 0f)
                    val bottomLeft = Offset(0f, h.internalAsFloat())
                    val bottomRight = Offset(w.internalAsFloat(), h.internalAsFloat())

                    drawLine(color, start = topLeft, end = bottomRight, strokeWidth = 4f)
                    drawLine(color, start = bottomLeft, end = topRight, strokeWidth = 4f)
                }
            }
        }
    }

    @Test
    fun testSimple() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    CANVAS [-5:-1] = [0.0, 275.0, 825.0, 275.0] VISIBLE
      MODIFIERS
        HEIGHT = 275.0
        BACKGROUND = [0.0, 0.0, 825.0, 275.0] color [1.0, 1.0, 1.0, 1.0] shape [0]
        PADDING = [22.0, 22.0, 22.0, 22.0]
        BACKGROUND = [0.0, 0.0, 781.0, 231.0] color [0.8, 0.8, 0.8, 1.0] shape [0]
      CANVAS_CONTENT [-7:-1] = [0.0, 0.0, 781.0, 231.0] VISIBLE
"""
        testLayout(result) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.Yellow),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RemoteCanvas(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(100.dp)
                            .background(Color.White)
                            .padding(8.dp)
                            .background(Color.LightGray)
                ) {}
            }
        }
    }

    @Ignore("Flaky")
    @Test
    fun testAsyncLayout() {
        val result =
            """
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 451.0] VISIBLE
    MODIFIERS
    BOX [-5:-1] = [0.0, 0.0, 825.0, 88.0] VISIBLE
      MODIFIERS
        HEIGHT = 88.0
        BACKGROUND = [0.0, 0.0, 825.0, 88.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
      ROW [-7:-1] = [0.0, 0.0, 825.0, 82.5] VISIBLE
        MODIFIERS
          BACKGROUND = [0.0, 0.0, 825.0, 82.5] color [0.0, 0.0, 0.0, 1.0] shape [0]
          HEIGHT = 82.5
        CANVAS_CONTENT [-19:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
          DATA_TEXT<47> = "Hello, World"
    ROW [-9:-1] = [0.0, 88.0, 555.0, 275.0] VISIBLE
      MODIFIERS
      BOX [-11:-1] = [0.0, 0.0, 88.0, 27.5] VISIBLE
        MODIFIERS
          WIDTH = 88.0
          HEIGHT = 27.5
          BACKGROUND = [0.0, 0.0, 88.0, 27.5] color [0.0, 0.0, 1.0, 1.0] shape [0]
      BOX [-13:-1] = [88.0, 0.0, 379.0, 259.0] VISIBLE
        MODIFIERS
          WIDTH = 379.0
          HEIGHT = 259.0
        CANVAS_CONTENT [-20:-1] = [0.0, 0.0, 379.0, 259.0] VISIBLE
          BitmapData id 48 (379x259)
          DATA_TEXT<49> = ""
      BOX [-15:-1] = [467.0, 0.0, 88.0, 275.0] VISIBLE
        MODIFIERS
          WIDTH = 88.0
          HEIGHT = 275.0
          BACKGROUND = [0.0, 0.0, 88.0, 275.0] color [0.0, 0.0, 1.0, 1.0] shape [0]
    BOX [-17:-1] = [0.0, 363.0, 825.0, 88.0] VISIBLE
      MODIFIERS
        HEIGHT = 88.0
        BACKGROUND = [0.0, 0.0, 825.0, 88.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
"""

        testAsyncLayout(result) {
            RemoteContext {
                Column {
                    Box(
                        modifier = RemoteModifier.height(32.dp).fillMaxWidth().background(Color.Red)
                    ) {
                        Row(
                            modifier =
                                RemoteModifier.background(Color.Black).fillMaxWidth().height(30.dp)
                        ) {
                            Text("Hello, World", color = Color.White)
                        }
                    }
                    Row {
                        Box(
                            modifier =
                                RemoteModifier.width(32.dp).height(10.dp).background(Color.Blue)
                        )
                        CaptureAsBitmap(onCapture = { it.value = true }) {
                            Column(modifier = RemoteModifier.background(Color.Yellow)) {
                                Text("🏄 🐶 élo! 🥳")
                                Text("أليس هذا رائعا؟")
                                Text("किं न शीतलम् ?")
                                Text("是不是很酷？")
                                Text("かっこいいでしょう？")
                            }
                        }
                        Box(
                            modifier =
                                RemoteModifier.width(32.dp).height(100.dp).background(Color.Blue)
                        )
                    }
                    Box(
                        modifier = RemoteModifier.height(32.dp).fillMaxWidth().background(Color.Red)
                    )
                }
            }
        }
    }

    @Ignore("Flaky")
    @Test
    fun testSimpleText() {
        val result =
            """
DATA_TEXT<42> = "default"
DATA_TEXT<43> = "Bonjour le monde!"
DATA_TEXT<44> = "Hello World"
DATA_TEXT<45> = "Hola Mundo"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
      PADDING = [55.0, 55.0, 55.0, 55.0]
      BACKGROUND = [0.0, 0.0, 715.0, 715.0] color [0.0, 1.0, 1.0, 1.0] shape [0]
    ROW [-5:-1] = [0.0, 181.5, 715.0, 250.0] VISIBLE
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 715.0, 250.0] color [0.8, 0.8, 0.8, 1.0] shape [0]
      TEXT_LAYOUT [-7:-1] = [0.0, 105.5, 171.0, 39.0] VISIBLE (44:"Hello World")
        MODIFIERS
      TEXT_LAYOUT [-9:-1] = [171.0, 0.0, 544.0, 250.0] VISIBLE (43:"Bonjour le monde!")
        MODIFIERS
          BACKGROUND = [0.0, 0.0, 544.0, 250.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
          PADDING = [11.0, 11.0, 11.0, 11.0]
          BACKGROUND = [0.0, 0.0, 522.0, 228.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
          PADDING = [11.0, 11.0, 11.0, 11.0]
    TEXT_LAYOUT [-11:-1] = [202.0, 431.5, 311.0, 102.0] VISIBLE (45:"Hola Mundo")
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 311.0, 102.0] color [0.0, 0.0, 1.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
        BACKGROUND = [0.0, 0.0, 289.0, 80.0] color [0.0, 0.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
"""
        testLayout(result) {
            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .background(Color.Red)
                        .padding(20.dp)
                        .background(Color.Cyan),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val text = rememberRemoteString("test") { "Bonjour le monde!" }

                RemoteRow(
                    modifier = RemoteModifier.background(Color.LightGray),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RemoteText(text = "Hello World")
                    RemoteText(
                        text = text,
                        modifier =
                            RemoteModifier.background(Color.Yellow)
                                .padding(4.dp)
                                .background(Color.Red)
                                .padding(4.dp),
                        fontSize = 32.sp,
                        color = Color.White,
                    )
                }
                RemoteText(
                    text = "Hola Mundo",
                    modifier =
                        RemoteModifier.background(Color.Blue)
                            .padding(4.dp)
                            .background(Color.Black)
                            .padding(4.dp),
                    fontSize = 18.sp,
                    color = Color.White,
                )
            }
        }
    }

    @Ignore("Flaky (off by one pixel error)")
    @Test
    fun testSimpleTextVariants() {
        val result =
            """
DATA_TEXT<42> = "default"
DATA_TEXT<43> = "Bonjour Le Monde!"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    DATA_TEXT<44> = "serif"
    DATA_TEXT<45> = "sans-serif"
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
      PADDING = [55.0, 55.0, 55.0, 55.0]
      BACKGROUND = [0.0, 0.0, 715.0, 715.0] color [0.0, 1.0, 1.0, 1.0] shape [0]
    TEXT_LAYOUT [-5:-1] = [130.0, 51.5, 455.0, 102.0] VISIBLE (43:"Bonjour Le Monde!")
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 455.0, 102.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
        BACKGROUND = [0.0, 0.0, 433.0, 80.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
    TEXT_LAYOUT [-7:-1] = [137.5, 153.5, 440.0, 102.0] VISIBLE (43:"Bonjour Le Monde!")
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 440.0, 102.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
        BACKGROUND = [0.0, 0.0, 418.0, 80.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
    TEXT_LAYOUT [-9:-1] = [136.5, 255.5, 442.0, 102.0] VISIBLE (43:"Bonjour Le Monde!")
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 442.0, 102.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
        BACKGROUND = [0.0, 0.0, 420.0, 80.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
    TEXT_LAYOUT [-11:-1] = [124.5, 357.5, 466.0, 102.0] VISIBLE (43:"Bonjour Le Monde!")
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 466.0, 102.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
        BACKGROUND = [0.0, 0.0, 444.0, 80.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
    TEXT_LAYOUT [-13:-1] = [110.5, 459.5, 494.0, 102.0] VISIBLE (43:"Bonjour Le Monde!")
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 494.0, 102.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
        BACKGROUND = [0.0, 0.0, 472.0, 80.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
    TEXT_LAYOUT [-15:-1] = [130.0, 561.5, 455.0, 102.0] VISIBLE (43:"Bonjour Le Monde!")
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 455.0, 102.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
        BACKGROUND = [0.0, 0.0, 433.0, 80.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
        PADDING = [11.0, 11.0, 11.0, 11.0]
"""
        testLayout(result) {
            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .background(Color.Red)
                        .padding(20.dp)
                        .background(Color.Cyan),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val text = rememberRemoteString("plop") { "Bonjour Le Monde!" }

                RemoteText(
                    text,
                    RemoteModifier.background(Color.Yellow)
                        .padding(4.dp)
                        .background(Color.Red)
                        .padding(4.dp),
                    Color.White,
                    18.sp,
                )
                RemoteText(
                    text,
                    RemoteModifier.background(Color.Yellow)
                        .padding(4.dp)
                        .background(Color.Red)
                        .padding(4.dp),
                    Color.White,
                    18.sp,
                    FontStyle.Italic,
                )
                RemoteText(
                    text,
                    RemoteModifier.background(Color.Yellow)
                        .padding(4.dp)
                        .background(Color.Red)
                        .padding(4.dp),
                    Color.White,
                    18.sp,
                    fontWeight = FontWeight.ExtraLight,
                )
                RemoteText(
                    text,
                    RemoteModifier.background(Color.Yellow)
                        .padding(4.dp)
                        .background(Color.Red)
                        .padding(4.dp),
                    Color.White,
                    18.sp,
                    fontWeight = FontWeight.Black,
                )
                RemoteText(
                    text,
                    RemoteModifier.background(Color.Yellow)
                        .padding(4.dp)
                        .background(Color.Red)
                        .padding(4.dp),
                    Color.White,
                    18.sp,
                    fontFamily = FontFamily.Serif,
                )
                RemoteText(
                    text,
                    RemoteModifier.background(Color.Yellow)
                        .padding(4.dp)
                        .background(Color.Red)
                        .padding(4.dp),
                    Color.White,
                    18.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }

    @Test
    fun testBasicClickAction() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    BOX [-5:-1] = [275.0, 275.0, 275.0, 275.0] VISIBLE
      MODIFIERS
        WIDTH = 275.0
        HEIGHT = 275.0
        CLICK_MODIFIER
          HOST_NAMED_ACTION = 43
        SEMANTICS = SEMANTICS BUTTON
"""
        testLayout(result) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.Yellow),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RemoteBox(
                    modifier = RemoteModifier.size(100.dp).onClick(HostAction("my_host_action"))
                )
            }
        }
    }

    @Test
    fun testBasicClickActionParam() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    BOX [-5:-1] = [275.0, 275.0, 275.0, 275.0] VISIBLE
      MODIFIERS
        WIDTH = 275.0
        HEIGHT = 275.0
        CLICK_MODIFIER
          HOST_NAMED_ACTION = 44 : 43
        SEMANTICS = SEMANTICS BUTTON
"""
        testLayout(result) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.Yellow),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val param = rememberRemoteIntValue { 128 }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.dp).onClick(HostAction("my_host_action", param))
                )
            }
        }
    }

    @Ignore("Broken")
    @Test
    fun testStateLayoutOn() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    STATE_LAYOUT [-5:-1] = [330.0, 330.0, 165.0, 165.0] VISIBLE
      MODIFIERS
      BOX [-7:-1] = [330.0, 330.0, 165.0, 165.0] VISIBLE
        MODIFIERS
        BOX [-9:-1] = [0.0, 0.0, 165.0, 165.0] VISIBLE
          MODIFIERS
            WIDTH = 165.0
            HEIGHT = 165.0
            BACKGROUND = [0.0, 0.0, 165.0, 165.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
      BOX [-11:-1] = [0.0, 0.0, 0.0, 0.0] GONE
        MODIFIERS
        BOX [-13:-1] = [0.0, 0.0, 220.0, 220.0] VISIBLE
          MODIFIERS
            WIDTH = 220.0
            HEIGHT = 220.0
            BACKGROUND = [0.0, 0.0, 0.0, 0.0] color [0.0, 1.0, 0.0, 1.0] shape [0]
"""
        testLayout(result) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.Yellow),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val checked = rememberRemoteIntValue { 0 }
                val (off, on) = createIds
                val fsm = rememberStateMachine(checked, off, on)

                StateLayout(stateMachine = fsm, modifier = RemoteModifier.fillMaxSize()) { state ->
                    Box {
                        when (state) {
                            off -> {
                                Box(modifier = RemoteModifier.size(60.dp).background(Color.Red))
                            }
                            on -> {
                                Box(modifier = RemoteModifier.size(80.dp).background(Color.Green))
                            }
                        }
                    }
                }
            }
        }
    }

    @Ignore("OOM")
    @Test
    fun testStateLayoutOff() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    STATE_LAYOUT [-5:-1] = [302.5, 302.5, 220.0, 220.0] VISIBLE
      MODIFIERS
      BOX [-7:-1] = [0.0, 0.0, 165.0, 165.0] GONE
        MODIFIERS
        BOX [-9:-1] = [0.0, 0.0, 165.0, 165.0] GONE
          MODIFIERS
            WIDTH = 165.0
            HEIGHT = 165.0
            BACKGROUND = [0.0, 0.0, 165.0, 165.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
      BOX [-11:-1] = [302.5, 302.5, 220.0, 220.0] VISIBLE
        MODIFIERS
        BOX [-13:-1] = [0.0, 0.0, 220.0, 220.0] VISIBLE
          MODIFIERS
            WIDTH = 220.0
            HEIGHT = 220.0
            BACKGROUND = [0.0, 0.0, 220.0, 220.0] color [0.0, 1.0, 0.0, 1.0] shape [0]
"""
        testLayout(result) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.Yellow),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val checked = rememberRemoteIntValue { 1 }
                val (off, on) = createIds
                val fsm = rememberStateMachine(checked, off, on)

                StateLayout(stateMachine = fsm, modifier = RemoteModifier.fillMaxSize()) { state ->
                    Box {
                        when (state) {
                            off -> {
                                Box(modifier = RemoteModifier.size(60.dp).background(Color.Red))
                            }
                            on -> {
                                Box(modifier = RemoteModifier.size(80.dp).background(Color.Green))
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testTouch() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 825.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    STATE_LAYOUT [-5:-1] = [302.5, 302.5, 220.0, 220.0] VISIBLE
      MODIFIERS
        TOUCH_DOWN_MODIFIER
          VALUE_INTEGER_CHANGE = 43 -> 0
        TOUCH_UP_MODIFIER
          VALUE_INTEGER_CHANGE = 43 -> 1
        TOUCH_CANCEL_MODIFIER
          VALUE_INTEGER_CHANGE = 43 -> 1
      BOX [-7:-1] = [0.0, 0.0, 165.0, 165.0] GONE
        MODIFIERS
        BOX [-9:-1] = [0.0, 0.0, 165.0, 165.0] GONE
          MODIFIERS
            WIDTH = 165.0
            HEIGHT = 165.0
            BACKGROUND = [0.0, 0.0, 165.0, 165.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
      BOX [-11:-1] = [302.5, 302.5, 220.0, 220.0] VISIBLE
        MODIFIERS
        BOX [-13:-1] = [0.0, 0.0, 220.0, 220.0] VISIBLE
          MODIFIERS
            WIDTH = 220.0
            HEIGHT = 220.0
            BACKGROUND = [0.0, 0.0, 220.0, 220.0] color [0.0, 1.0, 0.0, 1.0] shape [0]
"""
        testLayout(result) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.Yellow),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val checked = rememberRemoteIntValue { 1 }
                val (off, on) = createIds
                val fsm = rememberStateMachine(checked, off, on)

                StateLayout(
                    stateMachine = fsm,
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .onTouchDown(ValueChange(checked, 0))
                            .onTouchUp(ValueChange(checked, 1))
                            .onTouchCancel(ValueChange(checked, 1)),
                ) { state ->
                    Box {
                        when (state) {
                            off -> {
                                Box(modifier = RemoteModifier.size(60.dp).background(Color.Red))
                            }
                            on -> {
                                Box(modifier = RemoteModifier.size(80.dp).background(Color.Green))
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testIntrinsics1() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  ROW [-3:-1] = [0.0, 0.0, 825.0, 165.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 165.0] color [0.0, 1.0, 1.0, 1.0] shape [0]
    BOX [-5:-1] = [0.0, 0.0, 411.125, 82.5] VISIBLE
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 411.125, 82.5] color [1.0, 0.0, 0.0, 1.0] shape [0]
        HEIGHT = 82.5
        PADDING = [11.0, 0.0, 0.0, 0.0]
    BOX [-7:-1] = [411.125, 0.0, 2.75, 165.0] VISIBLE
      MODIFIERS
        WIDTH = 2.75
        BACKGROUND = [0.0, 0.0, 2.75, 165.0] color [0.0, 1.0, 0.0, 1.0] shape [0]
    BOX [-9:-1] = [413.875, 0.0, 411.125, 165.0] VISIBLE
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 411.125, 165.0] color [0.0, 0.0, 1.0, 1.0] shape [0]
        HEIGHT = 165.0
        PADDING = [0.0, 0.0, 11.0, 0.0]
"""
        testLayout(result) {
            Row(modifier = RemoteModifier.background(Color.Cyan).height(IntrinsicSize.Min)) {
                Box(
                    modifier =
                        RemoteModifier.background(Color.Red)
                            .weight(1f)
                            .height(30.dp)
                            .padding(left = 4.dp)
                )
                Box(modifier = RemoteModifier.fillMaxHeight().width(1.dp).background(Color.Green))
                Box(
                    modifier =
                        RemoteModifier.background(Color.Blue)
                            .weight(1f)
                            .height(60.dp)
                            .padding(right = 4.dp)
                )
            }
        }
    }

    @Test
    fun testIntrinsics2() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  ROW [-3:-1] = [0.0, 0.0, 825.0, 165.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 825.0, 165.0] color [0.0, 1.0, 1.0, 1.0] shape [0]
    BOX [-5:-1] = [0.0, 0.0, 411.125, 165.0] VISIBLE
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 411.125, 165.0] color [1.0, 0.0, 0.0, 1.0] shape [0]
        HEIGHT = 165.0
        PADDING = [11.0, 0.0, 0.0, 0.0]
    BOX [-7:-1] = [411.125, 0.0, 2.75, 165.0] VISIBLE
      MODIFIERS
        WIDTH = 2.75
        BACKGROUND = [0.0, 0.0, 2.75, 165.0] color [0.0, 1.0, 0.0, 1.0] shape [0]
    BOX [-9:-1] = [413.875, 0.0, 411.125, 82.5] VISIBLE
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 411.125, 82.5] color [0.0, 0.0, 1.0, 1.0] shape [0]
        HEIGHT = 82.5
        PADDING = [0.0, 0.0, 11.0, 0.0]
  BOX [-11:-1] = [0.0, 0.0, 825.0, 55.0] VISIBLE
    MODIFIERS
      HEIGHT = 55.0
      BACKGROUND = [0.0, 0.0, 825.0, 55.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
"""
        testLayout(result) {
            Column {
                Row(modifier = RemoteModifier.background(Color.Cyan).height(IntrinsicSize.Min)) {
                    Box(
                        modifier =
                            RemoteModifier.background(Color.Red)
                                .weight(1f)
                                .height(60.dp)
                                .padding(left = 4.dp)
                    )
                    Box(
                        modifier =
                            RemoteModifier.fillMaxHeight().width(1.dp).background(Color.Green)
                    )
                    Box(
                        modifier =
                            RemoteModifier.background(Color.Blue)
                                .weight(1f)
                                .height(30.dp)
                                .padding(right = 4.dp)
                    )
                }
                Box(modifier = RemoteModifier.fillMaxWidth().height(20.dp).background(Color.Yellow))
            }
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testColorFilter1() {
        val result =
            """
DATA_TEXT<42> = "default"
DATA_TEXT<43> = "Green"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  BOX [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
    CANVAS [-5:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
      MODIFIERS
      CANVAS_CONTENT [-7:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
        ComponentValue value 44 set to WIDTH of Component -7
        ComponentValue value 45 set to HEIGHT of Component -7
    TEXT_LAYOUT [-8:-1] = [305.0, 364.0, 215.0, 97.0] VISIBLE (43:"Green")
      MODIFIERS
"""
        testLayout(result) {
            val colors =
                listOf((Color.Red).copy(alpha = 0.5f).compositeOver(Color.Green), Color.Black)
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .background(RemoteBrush.radialGradient(colors = colors))
            ) {
                RemoteText("Green", color = Color.Green, fontSize = 30.sp)
            }
        }
    }

    @RemoteComposable
    @Composable
    fun RemoteIconVector(
        icon: ImageVector,
        size: Dp = 32.dp,
        modifier: RemoteModifier = RemoteModifier,
        tint: Color = Color.White,
    ) {
        val captureMode = LocalRemoteComposeCreationState.current
        if (true || captureMode is NoRemoteCompose) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = modifier.toComposeUi().size(size),
                tint = tint,
            )
        } else {
            // note -- tint isn't applied in that codepath
            val painter = rememberVectorPainter(icon)
            val iconSizePx = with(LocalDensity.current) { size.toPx() }
            val scale = iconSizePx / 24f
            RemoteCanvas(modifier = RemoteModifier.size(size)) {
                scale(scale, pivot = Offset.Zero) {
                    // Suppressed because of https://buganizer.corp.google.com/issues/375131944
                    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
                    with(painter.vector.root) { this@RemoteCanvas.drawScope.draw() }
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testColorFilter2() {
        val result =
            """
DATA_TEXT<42> = "default"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  BOX [-3:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
    MODIFIERS
    CANVAS [-5:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
      MODIFIERS
      CANVAS_CONTENT [-7:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
        ComponentValue value 43 set to WIDTH of Component -7
        ComponentValue value 44 set to HEIGHT of Component -7
    ROW [-8:-1] = [368.5, 368.5, 88.0, 88.0] VISIBLE
      MODIFIERS
        BACKGROUND = [0.0, 0.0, 88.0, 88.0] color [0.0, 0.0, 1.0, 1.0] shape [0]
      CANVAS_CONTENT [-10:-1] = [0.0, 0.0, 88.0, 88.0] VISIBLE
        BitmapData id 47 (88x88)
        DATA_TEXT<48> = ""
"""
        testLayout(result) {
            val colors =
                listOf((Color.Red).copy(alpha = 0.5f).compositeOver(Color.Green), Color.Black)
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .background(RemoteBrush.radialGradient(colors = colors))
            ) {
                RemoteRow(modifier = RemoteModifier.background(Color.Blue)) {
                    RemoteIconVector(
                        ImageVector.vectorResource(
                            androidx.compose.remote.frontend.test.R.drawable.android
                        ),
                        tint = Color.Red,
                    )
                }
            }
        }
    }

    @Ignore("Flaky off by one / different save/restore in API 35")
    @Test
    fun testContentDraw() {
        val layoutResult =
            """
DATA_TEXT<42> = "default"
DATA_TEXT<43> = "XYZ"
ROOT [-2:-1] = [0.0, 0.0, 825.0, 825.0] VISIBLE
  ROW [-3:-1] = [0.0, 0.0, 176.0, 176.0] VISIBLE
    MODIFIERS
      BACKGROUND = [0.0, 0.0, 176.0, 176.0] color [1.0, 1.0, 0.0, 1.0] shape [0]
    BOX [-5:-1] = [0.0, 0.0, 176.0, 176.0] VISIBLE
      MODIFIERS
        DRAW_CONTENT
        WIDTH = 176.0
        HEIGHT = 176.0
        BACKGROUND = [0.0, 0.0, 176.0, 176.0] color [0.0, 0.0, 1.0, 1.0] shape [0]
        PADDING = [22.0, 22.0, 22.0, 22.0]
        BACKGROUND = [0.0, 0.0, 132.0, 132.0] color [1.0, 0.0, 1.0, 1.0] shape [0]
      TEXT_LAYOUT [-7:-1] = [35.5, 46.5, 61.0, 39.0] VISIBLE (43:"XYZ")
        MODIFIERS
"""
        val paintResult =
            """
type: RootLayoutComponent
id: -2
x: 0.0
y: 0.0
width: 825.0
height: 825.0
visibility: VISIBLE
list:
- {type: MatrixSave}
- {type: MatrixSave}
- {type: MatrixSave}
- {type: MatrixSave}
- type: ROW
  id: -3
  x: 0.0
  y: 0.0
  width: 176.0
  height: 176.0
  visibility: VISIBLE
  list:
  - type: ComponentModifiers
    modifiers:
    - {type: BackgroundModifierOperation, x: 0.0, y: 0.0, width: 176.0, height: 176.0,
      color: '0xffffff00', shapeType: RECTANGLE}
  - type: BOX
    id: -5
    x: 0.0
    y: 0.0
    width: 176.0
    height: 176.0
    visibility: VISIBLE
    list:
    - type: ComponentModifiers
      modifiers:
      - {type: DrawContentOperation}
      - type: WidthModifierOperation
        width: {type: Value, value: 176.0}
        dimensionModifierType: EXACT
      - type: HeightModifierOperation
        height: {type: Value, value: 176.0}
        dimensionModifierType: EXACT
      - {type: BackgroundModifierOperation, x: 0.0, y: 0.0, width: 176.0, height: 176.0,
        color: '0xff0000ff', shapeType: RECTANGLE}
      - {type: PaddingModifierOperation, left: 22.0, top: 22.0, right: 22.0, bottom: 22.0}
      - {type: BackgroundModifierOperation, x: 0.0, y: 0.0, width: 132.0, height: 132.0,
        color: '0xffff00ff', shapeType: RECTANGLE}
    - type: TEXT_LAYOUT
      id: -7
      x: 35.5
      y: 46.5
      width: 61.0
      height: 39.0
      visibility: VISIBLE
      list:
      - type: ComponentModifiers
        modifiers: []
      paddingLeft: 0.0
      paddingRight: 0.0
      paddingTop: 0.0
      paddingBottom: 0.0
      textId: 43
      color: '0xff000000'
      fontSize: 33.0
      fontStyle: -1
      fontWeight: 400.0
      fontFamilyId: -1
      textAlign: -1
    paddingLeft: 22.0
    paddingRight: 22.0
    paddingTop: 22.0
    paddingBottom: 22.0
    verticalPositioning: CENTER
    horizontalPositioning: CENTER
  paddingLeft: 0.0
  paddingRight: 0.0
  paddingTop: 0.0
  paddingBottom: 0.0
  verticalPositioning: CENTER
  horizontalPositioning: CENTER
  spacedBy: 0.0
- {type: MatrixRestore}
- {type: MatrixRestore}
- {type: MatrixRestore}
- {type: MatrixRestore}
"""
        testLayoutAndPaint(layoutResult, paintResult) {
            RemoteRow(
                modifier = RemoteModifier.background(Color.Yellow),
                horizontalArrangement = Arrangement.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        RemoteModifier.drawWithContent {
                                rotate(37f) { this@drawWithContent.drawContent() }
                                translate(40f, 40f) {
                                    rotate(45f) {
                                        scale(1.2f) { this@drawWithContent.drawContent() }
                                    }
                                }
                                drawContent()
                                translate(-40f, -40f) {
                                    rotate(30f) { this@drawWithContent.drawContent() }
                                }
                            }
                            .size(64.dp)
                            .background(Color.Blue)
                            .padding(8.dp)
                            .background(Color.Magenta),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    RemoteText("XYZ")
                }
            }
        }
    }
}

private fun SemanticsNodeInteraction.assertTextMatches(expected: String) {
    val text =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.toString()
    assertThat(text).isEqualTo(expected)
}
