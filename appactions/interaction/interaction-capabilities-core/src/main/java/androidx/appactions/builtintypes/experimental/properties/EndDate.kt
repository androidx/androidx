/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.builtintypes.experimental.properties

import java.time.LocalDate
import java.time.ZonedDateTime

class EndDate internal constructor(
    @get:JvmName("asDate")
    val asDate: LocalDate? = null,
    @get:JvmName("asZonedDateTime")
    val asZonedDateTime: ZonedDateTime? = null
) {
    constructor(date: LocalDate) : this(asDate = date, asZonedDateTime = null)
    constructor(zonedDateTime: ZonedDateTime) : this(asDate = null, asZonedDateTime = zonedDateTime)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EndDate) return false
        if (asDate != other.asDate) return false
        if (asZonedDateTime != other.asZonedDateTime) return false
        return true
    }
}