/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test

/**
 * Pins the host half of the action wire protocol to its literal strings.
 *
 * This host library is a Kotlin/JVM artifact and cannot link against the device-side
 * `androidx.test.backup.BackupActionInputKeys`, so the two copies are kept honest by this test and
 * its device-side twin, `BackupActionKeysTest`. If one of them is edited alone, the build fails
 * rather than a device run silently receiving arguments it does not recognize.
 */
class BackupActionWireProtocolTest {

    @Test
    fun inputKeysMatchTheDeviceLibrary() {
        assertEquals("storage_type", BackupActionInputKeys.STORAGE_TYPE)
        assertEquals("is_device_protected", BackupActionInputKeys.IS_DEVICE_PROTECTED)
        assertEquals("pref_name", BackupActionInputKeys.PREF_NAME)
        assertEquals("pref_key", BackupActionInputKeys.PREF_KEY)
        assertEquals("value", BackupActionInputKeys.VALUE)
        assertEquals("value_type", BackupActionInputKeys.VALUE_TYPE)
        assertEquals("db_name", BackupActionInputKeys.DB_NAME)
        assertEquals("table", BackupActionInputKeys.TABLE)
        assertEquals("values", BackupActionInputKeys.VALUES)
        assertEquals("key_col", BackupActionInputKeys.KEY_COL)
        assertEquals("key_val", BackupActionInputKeys.KEY_VAL)
        assertEquals("path", BackupActionInputKeys.PATH)
        assertEquals("is_binary", BackupActionInputKeys.IS_BINARY)
        assertEquals("expected", BackupActionInputKeys.EXPECTED)
        assertEquals("expected_col", BackupActionInputKeys.EXPECTED_COL)
        assertEquals("expected_val", BackupActionInputKeys.EXPECTED_VAL)
        assertEquals("expect_null", BackupActionInputKeys.EXPECT_NULL)
    }

    @Test
    fun outputKeysMatchTheDeviceLibrary() {
        assertEquals("status", BackupActionOutputKeys.STATUS)
        assertEquals("error", BackupActionOutputKeys.ERROR)
    }

    @Test
    fun valuesMatchTheDeviceLibrary() {
        assertEquals("PREFS", BackupActionValues.STORAGE_TYPE_PREFS)
        assertEquals("DATABASE", BackupActionValues.STORAGE_TYPE_DATABASE)
        assertEquals("FILES", BackupActionValues.STORAGE_TYPE_FILES)
        assertEquals("INT", BackupActionValues.VALUE_TYPE_INT)
        assertEquals("LONG", BackupActionValues.VALUE_TYPE_LONG)
        assertEquals("FLOAT", BackupActionValues.VALUE_TYPE_FLOAT)
        assertEquals("BOOLEAN", BackupActionValues.VALUE_TYPE_BOOLEAN)
        assertEquals("STRING", BackupActionValues.VALUE_TYPE_STRING)
        assertEquals("success", BackupActionValues.STATUS_SUCCESS)
        assertEquals("failure", BackupActionValues.STATUS_FAILURE)
    }

    /** The action status is lower case; the telemetry report status is upper case. */
    @Test
    fun actionStatusIsDistinctFromReportStatus() {
        assertEquals("success", BackupActionValues.STATUS_SUCCESS)
        assertEquals("SUCCESS", BackupReportKeys.REPORT_STATUS_SUCCESS)
        assertEquals("failure", BackupActionValues.STATUS_FAILURE)
        assertEquals("FAILURE", BackupReportKeys.REPORT_STATUS_FAILURE)
    }

    @Test
    fun encodesPlainPairs() {
        assertEquals(
            "col1=val1&col2=val2",
            BackupActionWireProtocol.encodeColumnValues(listOf("col1" to "val1", "col2" to "val2")),
        )
    }

    /** Unencoded separators used to corrupt the column list on arrival. */
    @Test
    fun encodesSeparatorsInsideValues() {
        assertEquals(
            "q=a%26b%3Dc",
            BackupActionWireProtocol.encodeColumnValues(listOf("q" to "a&b=c")),
        )
    }

    @Test
    fun encodesAnEmptyValue() {
        assertEquals("col=", BackupActionWireProtocol.encodeColumnValues(listOf("col" to "")))
    }

    /** What the host writes has to survive the decoder the device applies to it. */
    @Test
    fun roundTripsThroughTheDeviceDecoder() {
        val pairs = listOf("a&b" to "c=d", "e%f" to "g+h", "unicode" to "café", "empty" to "")

        val decoded =
            BackupActionWireProtocol.encodeColumnValues(pairs).split("&").map { segment ->
                val separator = segment.indexOf('=')
                decode(segment.substring(0, separator)) to decode(segment.substring(separator + 1))
            }

        assertEquals(pairs, decoded)
    }

    @Test
    fun runnerComponentPointsAtTheTestApk() {
        assertEquals(
            "com.example.app.test/androidx.test.backup.BackupRestoreTestRunner",
            BackupActionWireProtocol.runnerComponent("com.example.app"),
        )
    }

    @Test
    fun instrumentationArgsNameTheActionAndTheOverflowDirectory() {
        val args =
            BackupActionWireProtocol.instrumentationArgs(
                "com.example.MyAction",
                linkedMapOf("k1" to "v1", "k2" to "v2"),
                waitForDebugger = false,
            )

        assertEquals(
            listOf("action", "actionClass", "payload_id", "redirect_dir", "k1", "k2"),
            args.keys.toList(),
        )
        assertEquals("com.example.MyAction", args["action"])
        assertEquals("com.example.MyAction", args["actionClass"])
        assertEquals("/data/local/tmp", args["redirect_dir"])
        assertEquals("v1", args["k1"])
        assertEquals("v2", args["k2"])
    }

    @Test
    fun instrumentationArgsUseAFreshPayloadIdPerCall() {
        fun payloadId() =
            BackupActionWireProtocol.instrumentationArgs("A", emptyMap(), false)["payload_id"]!!

        val first = payloadId()
        assertEquals(first, UUID.fromString(first).toString())
        assertNotEquals(first, payloadId())
    }

    @Test
    fun instrumentationArgsAskTheRunnerToWaitForADebugger() {
        val args = BackupActionWireProtocol.instrumentationArgs("A", emptyMap(), true)

        assertEquals("debug", args.keys.first())
        assertEquals("true", args["debug"])
    }

    @Test
    fun populateArgsTypesPreferenceValues() {
        mapOf(
                7 to "INT",
                7L to "LONG",
                1.5f to "FLOAT",
                true to "BOOLEAN",
                "text" to "STRING",
            )
            .forEach { (value, valueType) ->
                assertEquals(
                    mapOf(
                        "storage_type" to "PREFS",
                        "pref_name" to "prefs",
                        "pref_key" to "key",
                        "value" to value.toString(),
                        "value_type" to valueType,
                    ),
                    BackupActionWireProtocol.populateArgs(
                        StorageDomain.Preference("prefs", "key", value)
                    ),
                )
            }
    }

    @Test
    fun populateArgsMarksANullPreferenceWithExpectNull() {
        assertEquals(
            mapOf(
                "storage_type" to "PREFS",
                "pref_name" to "prefs",
                "pref_key" to "key",
                "expect_null" to "true",
            ),
            BackupActionWireProtocol.populateArgs(StorageDomain.Preference("prefs", "key", null)),
        )
    }

    @Test
    fun populateArgsAddTheMissingPrimaryKeyToTheDatabaseRow() {
        val args =
            BackupActionWireProtocol.populateArgs(
                StorageDomain.Database("app.db", "users", "id", 42, mapOf("name" to "Ann"))
            )

        assertEquals(
            mapOf(
                "storage_type" to "DATABASE",
                "db_name" to "app.db",
                "table" to "users",
                "values" to "name=Ann&id=42",
                "key_col" to "id",
                "key_val" to "42",
            ),
            args,
        )
    }

    @Test
    fun populateArgsKeepAPrimaryKeyTheRowAlreadyHas() {
        val args =
            BackupActionWireProtocol.populateArgs(
                StorageDomain.Database(
                    "app.db",
                    "users",
                    "id",
                    42,
                    linkedMapOf("ID" to 42, "note" to null),
                )
            )

        assertEquals("ID=42&note=", args["values"])
        assertEquals("id", args["key_col"])
        assertEquals("42", args["key_val"])
    }

    @Test
    fun populateArgsWriteTextFiles() {
        assertEquals(
            mapOf("storage_type" to "FILES", "path" to "a/b.txt", "value" to "hello"),
            BackupActionWireProtocol.populateArgs(StorageDomain.TextFile("a/b.txt", "hello")),
        )
    }

    @Test
    fun populateArgsEncodeBinaryFilesInBase64() {
        assertEquals(
            mapOf(
                "storage_type" to "FILES",
                "path" to "blob.bin",
                "value" to "AQL/",
                "is_binary" to "true",
            ),
            BackupActionWireProtocol.populateArgs(
                StorageDomain.BinaryFile("blob.bin", byteArrayOf(1, 2, -1))
            ),
        )
    }

    @Test
    fun assertArgsExpectThePreferenceValue() {
        val domain = StorageDomain.Preference("prefs", "key", 7)

        assertEquals(
            BackupActionWireProtocol.populateArgs(domain) + ("expected" to "7"),
            BackupActionWireProtocol.assertArgs(domain),
        )
    }

    @Test
    fun assertArgsExpectANullPreferenceToBeAbsent() {
        val domain = StorageDomain.Preference("prefs", "key", null)

        assertEquals(
            BackupActionWireProtocol.populateArgs(domain),
            BackupActionWireProtocol.assertArgs(domain),
        )
    }

    @Test
    fun assertArgsCheckTheFirstColumnOfTheKeyedRow() {
        val domain =
            StorageDomain.Database(
                "app.db",
                "users",
                "id",
                42,
                linkedMapOf("name" to null, "age" to 30),
            )

        assertEquals(
            BackupActionWireProtocol.populateArgs(domain) +
                mapOf("expected_col" to "name", "expected_val" to ""),
            BackupActionWireProtocol.assertArgs(domain),
        )
    }

    @Test
    fun assertArgsExpectTheFileContent() {
        val text = StorageDomain.TextFile("a.txt", "hello")
        val binary = StorageDomain.BinaryFile("b.bin", byteArrayOf(1, 2, -1))

        assertEquals(
            BackupActionWireProtocol.populateArgs(text) + ("expected" to "hello"),
            BackupActionWireProtocol.assertArgs(text),
        )
        assertEquals(
            BackupActionWireProtocol.populateArgs(binary) + ("expected" to "AQL/"),
            BackupActionWireProtocol.assertArgs(binary),
        )
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
