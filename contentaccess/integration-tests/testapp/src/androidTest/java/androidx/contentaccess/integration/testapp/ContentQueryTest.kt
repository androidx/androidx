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

import com.google.common.truth.Truth.assertThat

import android.Manifest
import android.content.ContentValues
import android.provider.CalendarContract.Events
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.ContentQuery
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@MediumTest
@RunWith(AndroidJUnit4::class)
class ContentQueryTest {

    private val contentResolver =
        InstrumentationRegistry.getInstrumentation().context.contentResolver
    private val eventsAccessor = ContentAccess.getAccessor(EventsAccessor::class, contentResolver)

    @Rule
    val readCalPermission =
        GrantPermissionRule.grant(Manifest.permission.READ_CALENDAR)

    @Before
    fun setup(){
        val values = ContentValues()
        values.put(Events.DTSTART, 1000L)
        values.put(Events.DTEND, 1500L)
        values.put(Events.TITLE, "FirstEvent")
        values.put(Events.DESCRIPTION, "An event that happens to be first")
        values.put(Events.CALENDAR_ID, 1L)
        values.put(Events.EVENT_TIMEZONE, "America/Los_Angeles")
        contentResolver.insert(Events.CONTENT_URI, values)

        values.put(Events.DTSTART, 2000L)
        values.put(Events.DTEND, 2500L)
        values.put(Events.TITLE, "SecondEvent")
        values.put(Events.DESCRIPTION, "An event that happens to be second")
        values.put(Events.CALENDAR_ID, 2L)
        values.put(Events.EVENT_TIMEZONE, "America/Los_Angeles")
        contentResolver.insert(Events.CONTENT_URI, values)
    }

    @ContentAccessObject(Event::class)
    interface EventsAccessor {
        @ContentQuery
        fun getSingleElement(): Event?
    }

    @Test
    fun testGetsSingleElementProperly(){
        assertThat(listOf("FirstsEvent", "SecosndEvent")).contains(eventsAccessor
           .getSingleElement()!!.title)

    }
}