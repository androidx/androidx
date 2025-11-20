/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.preferences.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString.Companion.decodeBase64

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreferencesCompatibilityTest {

    @Test
    fun testWireCompatibility() =
        runTest(timeout = 10000.milliseconds) {

            // base64 output of serializing "expectedProto"
            val protoBase64 =
                "ChAKB215RmxvYXQSBRXNzIw/ChUKCG15RG91YmxlEgk5mpmZmZmZ8T8KCwoFbXlJbnQSAh" +
                    "gBCgwKBm15TG9uZxICIAEKGQoIbXlTdHJpbmcSDSoLc3RyaW5nVmFsdWUKDwoJbXlCb29sZWFuEgIIAQo" +
                    "bCgtteVN0cmluZ1NldBIMMgoKA29uZQoDdHdvChMKC215Qnl0ZUFycmF5EgRCAgEC"
            val byteString = protoBase64.decodeBase64() ?: throw Exception("Unable to decode")
            val expectedProto =
                preferencesOf(
                    Preferences.Pair(floatPreferencesKey("myFloat"), 1.1f),
                    Preferences.Pair(doublePreferencesKey("myDouble"), 1.1),
                    Preferences.Pair(intPreferencesKey("myInt"), 1),
                    Preferences.Pair(longPreferencesKey("myLong"), 1L),
                    Preferences.Pair(stringPreferencesKey("myString"), "stringValue"),
                    Preferences.Pair(booleanPreferencesKey("myBoolean"), true),
                    Preferences.Pair(stringSetPreferencesKey("myStringSet"), setOf("one", "two")),
                    Preferences.Pair(byteArrayPreferencesKey("myByteArray"), byteArrayOf(1, 2)),
                )

            val protoBuffer = Buffer()
            protoBuffer.write(byteString)
            val protoPrefsFromBytes = PreferencesSerializer.readFrom(protoBuffer)
            assertEquals(expectedProto, protoPrefsFromBytes)
        }
}
