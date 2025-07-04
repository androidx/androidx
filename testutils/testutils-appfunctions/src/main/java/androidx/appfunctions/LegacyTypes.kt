/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appsearch.annotation.Document
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Document(name = "com.google.android.appfunctions.schema.common.v1.types.Attachment")
data class LegacyAttachment(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    /** The display name of the attachment. */
    @Document.StringProperty(required = true) val displayName: String,
    /** The MIME type of the attachment. Format defined in RFC 6838. */
    @Document.StringProperty val mimeType: String? = null,
    /** The URI of the attachment. */
    @Document.DocumentProperty(required = true) val uri: LegacyUri,
)

@Document(name = "com.google.android.appfunctions.schema.common.v1.types.Uri")
data class LegacyUri(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    @Document.StringProperty(required = true) val uri: String,
)

/** A date and time. */
@Document(name = "com.google.android.appfunctions.schema.common.v1.types.DateTime")
data class LegacyDateTime(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    /** The time zone. Formatted as an IANA Time Zone Database name (e.g. "Europe/Zurich"). */
    @Document.StringProperty(required = true) val timeZone: String,
    /** The date. */
    @Document.DocumentProperty(required = true) val date: LegacyDate,
    /** The time. */
    @Document.DocumentProperty(required = true) val timeOfDay: LegacyTimeOfDay,
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toZonedDateTime(): ZonedDateTime =
        ZonedDateTime.of(
            LocalDate.of(date.year, date.month, date.day),
            LocalTime.of(timeOfDay.hours, timeOfDay.minutes, timeOfDay.seconds, timeOfDay.nanos),
            ZoneId.of(timeZone),
        )
}

/** A date. */
@Document(name = "com.google.android.appfunctions.schema.common.v1.types.Date")
data class LegacyDate(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    /** The year. */
    @Document.LongProperty(required = true) val year: Int,
    /** The month. */
    @Document.LongProperty(required = true) val month: Int,
    /** The day. */
    @Document.LongProperty(required = true) val day: Int,
)

/** A time of day. */
@Document(name = "com.google.android.appfunctions.schema.common.v1.types.TimeOfDay")
data class LegacyTimeOfDay(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    /** The hours. */
    @Document.LongProperty(required = true) val hours: Int,
    /** The minutes. */
    @Document.LongProperty(required = true) val minutes: Int,
    /** The seconds. */
    @Document.LongProperty(required = true) val seconds: Int,
    /** The nanoseconds. */
    @Document.LongProperty(required = true) val nanos: Int,
)

/**
 * An interface for [@Document] classes that carry a decision to set a field of the given type in an
 * entity. These `Set*Field` classes are used as properties of `Upgrade*Params` classes, always
 * nullable. The convention is that when the property is set to `null`, the field of the entity is
 * not modified. Otherwise, it is set to the value carried by the `Set*Field` object. In particular,
 * for a nullable field, that value could be `null` which allows setting a field of the entity to
 * `null`.
 */
interface LegacySetField<T> {
    val value: T
}

@Document(name = "com.google.android.appfunctions.schema.common.v1.types.SetStringField")
data class LegacySetStringField(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    @Document.StringProperty(required = true) override val value: String,
) : LegacySetField<String>

@Document(name = "com.google.android.appfunctions.schema.common.v1.types.SetStringNullableField")
data class LegacySetStringNullableField(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    @Document.StringProperty override val value: String?,
) : LegacySetField<String?>

@Document(name = "com.google.android.appfunctions.schema.common.v1.types.SetAttachmentListField")
data class LegacySetAttachmentListField(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    @Document.DocumentProperty override val value: List<LegacyAttachment>,
) : LegacySetField<List<LegacyAttachment>>

@RequiresApi(26)
fun ZonedDateTime.toLegacyDateTime(): LegacyDateTime {
    return LegacyDateTime(
        timeZone = this.zone.id,
        date = LegacyDate(year = this.year, month = this.monthValue, day = this.dayOfMonth),
        timeOfDay =
            LegacyTimeOfDay(
                hours = this.hour,
                minutes = this.minute,
                seconds = this.second,
                nanos = this.nano,
            ),
    )
}
