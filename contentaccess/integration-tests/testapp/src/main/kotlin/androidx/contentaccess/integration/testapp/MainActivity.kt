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
import android.provider.CalendarContract
import android.provider.CalendarContract.Events.TITLE
import android.provider.CalendarContract.Events._ID
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.ContentQuery
import androidx.contentaccess.ContentColumn
import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.ContentUpdate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Optional
import java.util.concurrent.Executor
import android.provider.CalendarContract.Events.DTSTART as DTSTART1

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val defaultAccessor = ContentAccess.getAccessor(CalendarAccessor::class, contentResolver)

        val accessorWithExecutor = ContentAccess.getAccessor(
            CalendarAccessor::class,
            contentResolver,
            object : Executor {
                override fun execute(p0: Runnable?) {
                    p0?.run()
                }
            }
        )
        // Check suspend function working properly.
        GlobalScope.launch {
            val eventsBefore = accessorWithExecutor.getAllEventsTitlesAndDescriptions()
            eventsbeforeid.text = eventsBefore.joinToString("\n")
        }
        // May 18th 2020: 1589777445000L
        val eventsAfter = defaultAccessor.getAllEventsAfter(1589777445000L, CalendarContract.Events
            .CONTENT_URI.toString())
        eventsafterid.text = eventsAfter.joinToString("\n")
    }

    data class TitleDescription(
        @ContentColumn(_ID) val eventId: Long,
        @ContentColumn(TITLE) val title: String?,
        @ContentColumn(DTSTART1) val startTime: Long?,
        val description: String?
    )

    @ContentAccessObject(Event::class)
    interface CalendarAccessor {

        @ContentQuery
        suspend fun getAll(): List<Event>

        @ContentQuery
        suspend fun getOneEvent(): Event?

        @ContentQuery(projection = arrayOf(_ID, DTSTART1))
        suspend fun getAllEventsTitlesAndDescriptions(): List<TitleDescription>

        @ContentQuery(selection = "$DTSTART1 > :t", projection = arrayOf(_ID), uri = ":uri")
        fun getAllEventsAfter(t: Long, uri: String): List<Long>

        @ContentQuery(selection = "$DTSTART1 > :t and dtend > :l and dtend < :k", uri = ":uri")
        fun getAllEventsAfters(t: Long, l: Long, k: Long, uri: String): List<TitleDescription>

        @ContentQuery(projection = arrayOf(_ID, DTSTART1))
        fun getAllEventIdsAndStartTimes(): List<EventIdStartTime>

        @ContentQuery(projection = arrayOf("description"))
        fun getSingleDescription(): String?

        @ContentQuery(projection = arrayOf("description"))
        fun getSetOfDescriptions(): Set<String?>

        @ContentQuery(projection = arrayOf("description"))
        fun getOptionalSingleDescription(): Optional<String>

        @ContentUpdate(where = "dtstart = :startTime")
        fun updateDescription(@ContentColumn("description") desc: String, startTime: Long): Int

        @ContentUpdate
        fun updateDescription(@ContentColumn("dtend") newEndTime: Long?): Int
    }
}
