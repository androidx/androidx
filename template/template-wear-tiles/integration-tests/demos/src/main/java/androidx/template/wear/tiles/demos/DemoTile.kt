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

package androidx.template.wear.tiles.demos

import androidx.compose.runtime.Composable
import androidx.glance.ImageProvider
import androidx.glance.wear.tiles.GlanceTileService
import androidx.template.template.SingleEntityTemplateData
import androidx.template.template.TemplateImageWithDescription
import androidx.template.template.TemplateText
import androidx.template.wear.tiles.SingleEntityTemplate

/** Simple demo tile, displays [SingleEntityTemplate]  */
class DemoTile : GlanceTileService() {

    @Composable
    override fun Content() {
        SingleEntityTemplate(
            SingleEntityTemplateData(
                header = TemplateText("Single Entity Demo", TemplateText.Type.Title),
                headerIcon = TemplateImageWithDescription(
                    ImageProvider(R.drawable.compose),
                    "image"
                ),
                text1 = TemplateText("Title", TemplateText.Type.Title),
                text2 = TemplateText("Subtitle", TemplateText.Type.Label),
                image = TemplateImageWithDescription(ImageProvider(R.drawable.compose), "image"),
            )
        )
    }
}
