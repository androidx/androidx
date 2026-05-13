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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun RcMacroForEachDemoPreview() {
    RemoteDocumentPreview(RcMacroForEachDemo())
}

@Suppress("RestrictedApiAndroidX")
fun RcMacroForEachDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        8, // Using v8 for Macros
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        // 1. Define a template for a list item
        val itemTextParam = definePatternParameter("itemText")
        val itemColorParam = definePatternParameter("itemColor")
        val itemTemplateId =
            definePattern("ListItem", itemTextParam, itemColorParam) {
                box(Modifier.fillMaxWidth().height(150).padding(8).backgroundId(itemColorParam)) {
                    text(
                        itemTextParam,
                        color = Color.WHITE,
                        fontSize = 40f,
                        modifier = Modifier.padding(16),
                    )
                }
            }

        // 2. Define a template for a gallery that uses MacroForEach
        val collectionParam = definePatternParameter("items")
        val galleryTemplateId =
            definePattern("VerticalGallery", collectionParam) {
                column(Modifier.fillMaxWidth().padding(16)) {
                    val itemId = localId() // Unique ID for remapping inside forEach
                    patternForEach(collectionParam, itemId) {
                        // Call the item template for each item in the collection
                        // Here we assume the collection contains text IDs
                        inflatePattern(itemTemplateId, itemId, addColor(Color.GRAY)) {}
                    }
                }
            }

        root {
            column(Modifier.fillMaxSize().background(Color.WHITE)) {
                text("MacroForEach Demo", fontSize = 60f, modifier = Modifier.padding(24))

                // 3. Create a collection of strings at the call site
                val myData =
                    addDataListIds(
                        intArrayOf(
                            textId("Item One"),
                            textId("Item Two"),
                            textId("Item Three"),
                            textId("Item Four"),
                        )
                    )

                // 4. Instantiate the gallery macro with our data
                inflatePattern(galleryTemplateId, myData) {}
            }
        }
    }
}
