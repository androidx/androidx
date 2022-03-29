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

package androidx.glance.wear.tiles.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.tiles.GlanceTileService
import androidx.glance.wear.tiles.curved.AnchorType
import androidx.glance.wear.tiles.curved.CurvedRow
import androidx.glance.wear.tiles.curved.CurvedTextStyle
import androidx.glance.wear.tiles.curved.GlanceCurvedModifier
import androidx.glance.wear.tiles.curved.sweepAngleDegrees
import androidx.glance.wear.tiles.curved.thickness

class CurvedLayoutTileService : GlanceTileService() {

    @Composable
    override fun Content() {
        CurvedRow(
            anchorType = AnchorType.Start
        ) {
            curvedText(
                text = "hello",
                style = CurvedTextStyle(
                    color = ColorProvider(Color.Green)
                )
            )
            curvedComposable {
                Text(
                    text = "glance",
                    style = TextStyle(color = ColorProvider(Color.Yellow))
                )
            }
            curvedLine(
                color = ColorProvider(Color.Cyan),
                curvedModifier =
                GlanceCurvedModifier.sweepAngleDegrees(30f).thickness(10.dp)
            )
            curvedSpacer(
                curvedModifier = GlanceCurvedModifier.sweepAngleDegrees(10f)
            )
            curvedText(
                text = "wear",
                style = CurvedTextStyle(
                    color = ColorProvider(Color.Green)
                )
            )
            curvedComposable(false) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    modifier = GlanceModifier.size(30.dp, 30.dp),
                    contentScale = ContentScale.FillBounds,
                    contentDescription = "Hello tile icon"
                )
                Text(text = "tiles")
            }
        }
    }
}