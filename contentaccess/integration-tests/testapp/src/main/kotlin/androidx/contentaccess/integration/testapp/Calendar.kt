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

import androidx.contentaccess.ContentEntity
import androidx.contentaccess.ContentPrimaryKey
import androidx.contentaccess.ContentColumn

@ContentEntity("content://com.android.calendar/calendars")
data class Calendar(
    @ContentPrimaryKey("_id")
    var iD: Long?,
    @ContentColumn("calendar_displayName")
    var displayName: String?,
    @ContentColumn("account_name")
    var accountName: String?,
    @ContentColumn("account_type")
    var accountType: String?,
    @ContentColumn("calendar_timezone")
    var timezone: String?,
    @ContentColumn("calendar_location")
    var location: String?,
    @ContentColumn("isPrimary")
    var isPrimary: Boolean?,
    @ContentColumn("calendar_color")
    var color: String?,
    @ContentColumn("owner_account")
    var ownerAccount: String?
)

@ContentEntity("content://com.android.calendar/events")
data class Event(
    @ContentPrimaryKey("_id")
    var iD: Long?,
    @ContentColumn("calendar_id")
    var calendarId: Long?,
    @ContentColumn("title")
    var title: String?,
    @ContentColumn("description")
    var description: String?,
    @ContentColumn("dtstart")
    var startTime: Long?,
    @ContentColumn("dtend")
    var endTime: Long?,
    @ContentColumn("duration")
    var duration: Long?,
    @ContentColumn("allDay")
    var allDay: Int?
)