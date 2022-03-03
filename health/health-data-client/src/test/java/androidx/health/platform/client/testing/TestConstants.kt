/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.testing

import androidx.annotation.RestrictTo
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Constants shared in tests. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("NewApi") // Fix false positive once we can enable granular java8 build
object TestConstants {
    @SuppressWarnings("GoodTime") // ZoneOffset.of() is used for testing purposes only
    @JvmField
    val TEST_ZONE_OFFSET: ZoneOffset = ZoneOffset.of("+08:00")
    @JvmField val START_TIME: Instant = Instant.parse("2020-09-08T07:06:05.432Z")
    @JvmField val END_TIME: Instant = Instant.parse("2020-10-02T00:00:00.000Z")
    @JvmField
    val START_LOCAL_DATE_TIME: LocalDateTime = START_TIME.atZone(TEST_ZONE_OFFSET).toLocalDateTime()
    @JvmField
    val END_LOCAL_DATE_TIME: LocalDateTime = END_TIME.atZone(TEST_ZONE_OFFSET).toLocalDateTime()
    @JvmField val TEST_TIME: Instant = Instant.ofEpochMilli(1234L)
    @JvmField val UPDATE_TIME = Instant.ofEpochMilli(12345L)
    const val TEST_UID = "test_uid"
    const val TEST_CLIENT_ID = "test_client_id"
    const val TEST_APPLICATION_ID = "test_application_id"
}
