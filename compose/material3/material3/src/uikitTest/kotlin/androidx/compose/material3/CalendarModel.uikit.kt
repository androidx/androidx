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

package androidx.compose.material3

import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.defaultTimeZone
import platform.Foundation.setDefaultTimeZone
import platform.Foundation.timeZoneWithName

actual fun calendarLocale(language : String, country : String) : CalendarLocale =
    NSLocale("$language-${country}")

actual val supportsDateSkeleton: Boolean
    get() = true

actual fun setTimeZone(id: String) {
    NSTimeZone.setDefaultTimeZone(NSTimeZone.timeZoneWithName(id)!!)
}

actual fun getTimeZone(): String {
    return NSTimeZone.defaultTimeZone().name
}