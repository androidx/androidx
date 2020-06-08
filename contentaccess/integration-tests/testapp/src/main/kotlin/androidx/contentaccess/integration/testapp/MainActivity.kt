/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.contentaccess.integration.testapp

import android.app.Activity
import android.os.Bundle
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.ContentQuery
import androidx.contentaccess.ContentAccessObject
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val accessor = ContentAccess.getAccessor(CalendarAccessor::class, contentResolver)

        // May 18th 2020: 1589777445000L
        val eventsBefore = accessor.getLastEventBefore()
        val eventsAfter = accessor.getAllEventsAfter(1589777445000L, "content://com.android" +
                ".calendar/events")

        eventsbeforeid.text = eventsBefore.joinToString("\n")
        eventsafterid.text = eventsAfter.joinToString("\n")
    }

    data class TitleDescription(val title: String, val description: String)

    @ContentAccessObject(Event::class)
    interface CalendarAccessor {

        @ContentQuery(selection = "startTime > :t", query = "iD", uri = ":uri")
        fun getAllEventsAfter(t: Long, uri: String): List<Long>

        @ContentQuery(selection = "startTime < 1589777445000")
        fun getLastEventBefore(): List<TitleDescription>

        @ContentQuery(selection = "startTime > :t and endTime > :l and endTime < :k", query = "iD",
            uri =
        ":uri")
        fun getAllEventsAfters(t: Long, l: Long, k: Long, uri: String): List<Long>
    }
}
