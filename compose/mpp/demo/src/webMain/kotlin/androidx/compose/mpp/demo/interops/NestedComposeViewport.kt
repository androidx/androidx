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

package androidx.compose.mpp.demo.interops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.createElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLIFrameElement
import androidx.compose.ui.window.ComposeViewport

private val ttGoogleMaps =
    "https://www.google.com/maps/embed?pb=!1m14!1m12!1m3!1d1361.8421810316931!2d4.894047523068853!3d52.338403908686736!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!5e0!3m2!1sen!2snl!4v1749117010713!5m2!1sen!2snl"
private val ttOSM =
    "https://www.openstreetmap.org/export/embed.html?bbox=4.890965223312379%2C52.33722052818563%2C4.893990755081177%2C52.33860862450587&amp;layer=mapnik"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NestedComposeViewportDemo() {

    val mapProvider = remember { mutableStateOf("OSM") }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        HtmlElementView(
            factory = {
                (document.createElement("iframe") as HTMLIFrameElement)
            },
            modifier = Modifier.fillMaxSize(),
            update = { iframe ->
                iframe.src = if (mapProvider.value == "OSM") ttOSM else ttGoogleMaps
            }
        )

        NestedComposeViewPort(
            modifier = Modifier.size(150.dp, 60.dp).padding(top = 8.dp).align(Alignment.TopCenter)
        ) {
            Button(
                modifier = Modifier.align(Alignment.Center),
                onClick = {
                    mapProvider.value =
                        if (mapProvider.value == "OSM") "GM" else "OSM"
                }) {
                Text("Switch Map")
            }
        }

    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NestedComposeViewPort(
    id: String = "nested-compose-viewport",
    modifier: Modifier = Modifier.size(300.dp),
    content: @Composable BoxScope.() -> Unit
) {
    HtmlElementView(
        modifier = modifier,
        factory = {
            (document.createElement("div") as HTMLDivElement).apply {
                this.id = id
                style.apply { background = "transparent" }

                window.requestAnimationFrame {
                    ComposeViewport(this) {
                        Box(modifier = Modifier.fillMaxSize().drawBehind {
                            drawRect(
                                color = Color.Transparent,
                                blendMode = BlendMode.Clear
                            )
                        }) {
                            content()
                        }
                    }
                }
            }
        }
    )
}