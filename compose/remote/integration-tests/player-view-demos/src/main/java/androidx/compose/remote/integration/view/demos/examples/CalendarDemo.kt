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

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val displayColor: Int,
    val isAllDay: Boolean,
    val location: String?,
)

@Suppress("RestrictedApiAndroidX")
fun readTodayEvents(context: Context): List<CalendarEvent> {
    val events = mutableListOf<CalendarEvent>()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val endOfDay = cal.timeInMillis

    val projection =
        arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
        )

    val uri =
        CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startOfDay.toString())
            .appendPath(endOfDay.toString())
            .build()

    var cursor: Cursor? = null
    try {
        cursor =
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC",
            )
        cursor?.let {
            while (it.moveToNext()) {
                events.add(
                    CalendarEvent(
                        title = it.getString(0) ?: "(No title)",
                        startTime = it.getLong(1),
                        endTime = it.getLong(2),
                        displayColor = it.getInt(3),
                        isAllDay = it.getInt(4) != 0,
                        location = it.getString(5),
                    )
                )
            }
        }
    } catch (_: SecurityException) {
        // Permission not granted
    } finally {
        cursor?.close()
    }
    return events
}

@Suppress("LocalVariableName", "RestrictedApiAndroidX")
private class CalendarColorPack(val rc: RemoteComposeContextAndroid) {
    val backgroundId: Int
    val headerTextId: Int
    val titleTextId: Int
    val subtitleTextId: Int
    val dividerId: Int
    val panelId: Int

    init {
        rc.beginGlobal()
        val surfaceLight = rc.addNamedColor("color.system_accent2_50", 0xFFF5F5F5.toInt())
        val surfaceDark = rc.addNamedColor("color.system_accent2_800", 0xFF1C1C1E.toInt())
        backgroundId = rc.mColor(surfaceLight, surfaceDark)

        val onSurfaceLight = rc.addNamedColor("color.system_on_surface_light", 0xFF1C1B1F.toInt())
        val onSurfaceDark = rc.addNamedColor("color.system_on_surface_dark", 0xFFE6E1E5.toInt())
        headerTextId = rc.mColor(onSurfaceLight, onSurfaceDark)

        val neutral700 = rc.addNamedColor("color.system_neutral2_800", 0xFF1C1B1F.toInt())
        val neutral300 = rc.addNamedColor("color.system_neutral2_400", 0xFFCAC4D0.toInt())
        titleTextId = rc.mColor(neutral700, neutral300)

        val accent600 = rc.addNamedColor("color.system_accent3_600", 0xFF625B71.toInt())
        val accent200 = rc.addNamedColor("color.system_accent3_100", 0xFF958DA5.toInt())
        subtitleTextId = rc.mColor(accent600, accent200)

        val dividerLight = rc.addNamedColor("color.system_accent2_700", 0xFFE0E0E0.toInt())
        val dividerDark = rc.addNamedColor("color.system_accent2_400", 0xFF3C3C3E.toInt())
        dividerId = rc.mColor(dividerLight, dividerDark)

        val panelLight = rc.addNamedColor("color.system_accent2_10", 0xFFFFFFFF.toInt())
        val panelDark = rc.addNamedColor("color.system_accent2_900", 0xFF2C2C2E.toInt())
        panelId = rc.mColor(panelLight, panelDark)
        rc.endGlobal()
    }
}

@Suppress("RestrictedApiAndroidX")
fun calendarDayAgenda(events: List<CalendarEvent>): RemoteComposeContextAndroid {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
        RemoteComposeWriter.HTag(Header.DEBUG, 0),
    ) {
        val colors = CalendarColorPack(this)

        root {
            column(Modifier.fillMaxWidth().backgroundId(colors.backgroundId)) {
                dateHeader(colors)
                // Divider
                box(Modifier.fillMaxWidth().height(2).backgroundId(colors.dividerId))
                eventList(events, colors)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.dateHeader(colors: CalendarColorPack) {
    val cal = Calendar.getInstance()
    val dayOfWeekFmt = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateFmt = SimpleDateFormat("MMMM d", Locale.getDefault())
    val dayOfWeek = dayOfWeekFmt.format(cal.time)
    val dateStr = dateFmt.format(cal.time)
    val fullDate = "$dayOfWeek, $dateStr"

    column(Modifier.fillMaxWidth().padding(24, 20, 24, 16)) {
        text("Today", fontSize = 28f, colorId = colors.subtitleTextId)
        text(
            fullDate,
            Modifier.padding(0, 4, 0, 0),
            fontSize = 48f,
            fontWeight = 700f,
            colorId = colors.headerTextId,
        )
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.eventList(
    events: List<CalendarEvent>,
    colors: CalendarColorPack,
) {
    column(Modifier.fillMaxWidth().verticalScroll()) {
        if (events.isEmpty()) {
            box(Modifier.fillMaxWidth().padding(24, 40, 24, 40)) {
                text("No events today", fontSize = 36f, colorId = colors.subtitleTextId)
            }
        } else {
            for (event in events) {
                eventRow(event, colors)
                // Divider between events
                box(
                    Modifier.fillMaxWidth()
                        .padding(42, 0, 0, 0)
                        .height(1)
                        .backgroundId(colors.dividerId)
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.eventRow(event: CalendarEvent, colors: CalendarColorPack) {
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr =
        if (event.isAllDay) {
            "All day"
        } else {
            "${timeFmt.format(event.startTime)} - ${timeFmt.format(event.endTime)}"
        }

    row(Modifier.fillMaxWidth().padding(16, 12, 16, 12), vertical = RowLayout.CENTER) {
        // Color bar
        val r = 8f
        box(
            Modifier.width(6)
                .height(60)
                .clip(RoundedRectShape(r, r, r, r))
                .background(event.displayColor or 0xFF000000.toInt())
        )
        // Spacer
        box(Modifier.width(12))
        // Event details
        column(Modifier.horizontalWeight(1f)) {
            text(event.title, fontSize = 34f, fontWeight = 700f, colorId = colors.titleTextId)
            text(
                timeStr,
                Modifier.padding(0, 4, 0, 0),
                fontSize = 28f,
                colorId = colors.subtitleTextId,
            )
            if (!event.location.isNullOrBlank()) {
                text(
                    event.location,
                    Modifier.padding(0, 2, 0, 0),
                    fontSize = 26f,
                    colorId = colors.subtitleTextId,
                )
            }
        }
    }
}

// Sample events for preview
private fun sampleEvents(): List<CalendarEvent> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 9)
    cal.set(Calendar.MINUTE, 0)
    val start1 = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 10)
    val end1 = cal.timeInMillis

    cal.set(Calendar.HOUR_OF_DAY, 12)
    val start2 = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 13)
    val end2 = cal.timeInMillis

    cal.set(Calendar.HOUR_OF_DAY, 15)
    val start3 = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 16)
    cal.set(Calendar.MINUTE, 30)
    val end3 = cal.timeInMillis

    return listOf(
        CalendarEvent("Team Standup", start1, end1, 0xFF4CAF50.toInt(), false, "Room 42"),
        CalendarEvent("Lunch with Alex", start2, end2, 0xFF2196F3.toInt(), false, "Cafe"),
        CalendarEvent("Design Review", start3, end3, 0xFFE91E63.toInt(), false, null),
    )
}
