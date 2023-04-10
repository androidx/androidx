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
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.tiles.GlanceTileService
import androidx.glance.wear.tiles.LocalTimeInterval
import androidx.glance.wear.tiles.TimeInterval
import androidx.glance.wear.tiles.TimelineMode
import java.time.Instant

class CalendarTileService : GlanceTileService() {
    private val timeInstant = Instant.now()
    override val timelineMode = TimelineMode.TimeBoundEntries(
       setOf(
           TimeInterval(),
           TimeInterval(
               timeInstant,
               timeInstant.plusSeconds(60)
           ),
           TimeInterval(
               timeInstant.plusSeconds(60),
               timeInstant.plusSeconds(120)
           )
       )
    )

    @Composable
    override fun Content() {
        val eventTextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        val locationTextStyle = TextStyle(
            color = ColorProvider(Color.Gray),
            fontSize = 15.sp
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (LocalTimeInterval.current) {
                timelineMode.timeIntervals.elementAt(0) -> {
                    Text(text = "No event", style = eventTextStyle)
                }
                timelineMode.timeIntervals.elementAt(1) -> {
                    Text(text = "Coffee", style = eventTextStyle)
                    Spacer(GlanceModifier.height(5.dp))
                    Text(text = "Micro Kitchen", style = locationTextStyle)
                }
                timelineMode.timeIntervals.elementAt(2) -> {
                    Text(text = "Work", style = eventTextStyle)
                    Spacer(GlanceModifier.height(5.dp))
                    Text(text = "Remote from home", style = locationTextStyle)
                }
            }

            Spacer(GlanceModifier.height(15.dp))

            Image(
                provider = ImageProvider(R.drawable.ic_calendar),
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionStartActivity(CalendarActivity::class.java)),
                contentScale = ContentScale.Fit,
                contentDescription = "launch calendar activity"

            )
        }
    }
}