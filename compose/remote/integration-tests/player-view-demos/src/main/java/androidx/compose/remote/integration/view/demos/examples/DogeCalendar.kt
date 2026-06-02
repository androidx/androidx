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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.BitmapFactory
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.layout.FitBox
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alpha
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.CUBIC_DECELERATE
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.interpolateRemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.integration.view.demos.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.util.Calendar

/** Data class representing a schedule entry. */
data class DogeEntry(val time: String, val task: String)

/** Full Doge schedule array. */
val DOGE_SCHEDULE =
    arrayOf(
        DogeEntry("7:00 AM", "Wake up. Very yawn. Much stretch. Wow."),
        DogeEntry("8:00 AM", "Breakfast. Avocado Toast. Me hipster doge."),
        DogeEntry("9:00 AM", "Bark at mailman. Very protect. Much loud."),
        DogeEntry("10:30 AM", "Morning Nap. So cozy. Much dream."),
        DogeEntry("12:30 PM", "Lunch. Much eat.  8-seat omakase. Very exclusive."),
        DogeEntry("1:30 PM", "Backyard Patrol. Such squirrel. Many sniff."),
        DogeEntry("3:00 PM", "Afternoon Siesta. Just lounging. Much rest."),
        DogeEntry("5:00 PM", "Park time. Many grass. Such run. Wow."),
        DogeEntry("7:00 PM", "Dinner Time. 3 Michelin stars. Many good chef."),
        DogeEntry("8:30 PM", "Toy Squeaking. Such noise. Much play."),
        DogeEntry("10:00 PM", "Bedtime. Much sleep. So quiet."),
    )

/** A Doge Calendar demo. */
@Composable
@RemoteComposable
fun DogeCalendar(currentTimeSeconds: Float = 1000f, animate: Boolean = true) {
    val res = LocalResources.current

    val icons = remember {
        arrayOf(
                R.drawable.doge_yawning_on_bed,
                R.drawable.doge_eating_avocado_toast,
                R.drawable.doge_barking_at_mailman,
                R.drawable.doge_sleeping_on_pillow,
                R.drawable.doge_eating_sushi,
                R.drawable.doge_hunting_squirrel,
                R.drawable.doge_sleeping_on_rug,
                R.drawable.doge_walking_in_park,
                R.drawable.doge_eating_fancy_dinner,
                R.drawable.doge_chewing_bone,
                R.drawable.doge_sleeping_under_stars,
            )
            .map { resId ->
                BitmapFactory.decodeResource(res, resId, BitmapFactory.Options()).asImageBitmap()
            }
            .toTypedArray()
    }

    val interval = 3f
    val totalDuration = DOGE_SCHEDULE.size * interval

    // Zero the timer by subtracting the local seconds-past-hour at composition time.
    // TODO: Fix up and use animation clock after new RC player release
    val cal = Calendar.getInstance()
    val localOffset = (cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)).toFloat()

    val time =
        if (animate) {
            RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) - localOffset.rf
        } else {
            currentTimeSeconds.rf
        }

    // Ensure loopTime is positive by adding a large multiple of totalDuration before modulo
    val loopTime = (time + (totalDuration * 1000f).rf) % totalDuration.rf

    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(Color.Black),
        contentAlignment = RemoteAlignment.Center,
    ) {
        DOGE_SCHEDULE.forEachIndexed { index, entry ->
            val start = index * interval
            val progress = clamp((loopTime - start.rf) / 0.8f.rf, 0f.rf, 1f.rf)
            val easedProgress = interpolateRemoteFloat(progress, 0f.rf, 1f.rf, CUBIC_DECELERATE)

            // Check if we should be visible: current time is within [start, start + interval]
            @Suppress("DEPRECATION")
            val isVisible = (loopTime ge start.rf) and (loopTime lt (start + interval).rf)
            val icon = icons[index % icons.size]

            DogeSlot(
                entry = entry,
                fontSize = 18.rsp,
                icon = icon,
                alpha = easedProgress,
                isVisible = isVisible,
            )
        }
    }
}

@Composable
@RemoteComposable
private fun DogeSlot(
    entry: DogeEntry,
    fontSize: RemoteTextUnit,
    icon: androidx.compose.ui.graphics.ImageBitmap,
    alpha: RemoteFloat,
    isVisible: RemoteBoolean,
) {
    val containerAlpha = isVisible.select(alpha, 0f.rf)

    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().alpha(containerAlpha),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        // Large Doge Image
        RemoteImage(
            bitmap = icon,
            contentDescription = RemoteString("Doge Icon"),
            modifier = RemoteModifier.size(300.rdp).padding(bottom = 24.rdp),
        )

        // Text bubble below
        RemoteBox(
            modifier = RemoteModifier.fillMaxWidth(0.9f).height(100.rdp),
            contentAlignment = RemoteAlignment.Center,
        ) {
            // Background Pill
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .clip(RemoteRoundedCornerShape(50.rdp))
                        .background(Color(0x1AFFFFFF).rc)
            )

            // Content Pill
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .padding(4.rdp)
                        .clip(RemoteRoundedCornerShape(46.rdp))
                        .background(Color(0xB31A1A1A).rc)
            )

            // Text
            FitBox(modifier = RemoteModifier.padding(horizontal = 24.rdp)) {
                arrayOf(fontSize, 16.rsp, 14.rsp, 12.rsp, 10.rsp).forEach { size ->
                    RemoteText(
                        text = "${entry.time}: ${entry.task}",
                        fontSize = size,
                        color = Color.White.rc,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
