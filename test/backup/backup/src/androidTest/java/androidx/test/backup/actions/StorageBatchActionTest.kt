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

package androidx.test.backup.actions

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Bundle
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult
import androidx.test.backup.BackupRestoreTestRunner
import androidx.test.backup.TRUNCATION_MARKER
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests how [StorageBatchAction] runs the storage actions of its calls and reports a failure. */
@RunWith(AndroidJUnit4::class)
public class StorageBatchActionTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .createDeviceProtectedStorageContext()

    private val textFile = File(context.filesDir, TEXT_FILE)
    private val binaryFile = File(context.filesDir, BINARY_FILE)

    @Before
    @After
    public fun deleteTestData() {
        context.deleteSharedPreferences(PREFS)
        context.deleteDatabase(DATABASE)
        File(context.filesDir, "datastore/$DATASTORE.preferences_pb").delete()
        textFile.delete()
        binaryFile.delete()
    }

    @Test
    public fun runsTheCallsOfEveryStorageType() {
        val domains =
            listOf(
                preference("theme", "dark"),
                mapOf(
                    "storage_type" to "DATASTORE_PREFS",
                    "pref_name" to DATASTORE,
                    "pref_key" to "ratio",
                    "value" to "1.5",
                    "value_type" to "DOUBLE",
                ),
                mapOf(
                    "storage_type" to "DATABASE",
                    "db_name" to DATABASE,
                    "table" to "notes",
                    "values" to "text=a%26b&id=7",
                    "key_col" to "id",
                    "key_val" to "7",
                ),
                textFile("line 1\nline \"2\""),
                mapOf(
                    "storage_type" to "FILES",
                    "path" to BINARY_FILE,
                    "value" to "AQL/",
                    "is_binary" to "true",
                ),
            )

        val populated = runBatch(domains.map { POPULATE to it })

        assertEquals(mapOf("status" to "success"), populated.payload)
        // The process ends right after a batch, so what it wrote must be in the files by then.
        val preferences = File(context.dataDir, "shared_prefs/$PREFS.xml").readText()
        assertTrue(preferences, preferences.contains("""<string name="theme">dark</string>"""))
        assertEquals("line 1\nline \"2\"", textFile.readText())
        assertEquals(listOf<Byte>(1, 2, -1), binaryFile.readBytes().toList())
        assertEquals(
            mapOf("status" to "success"),
            runBatch(domains.map { ASSERT to it }).payload,
        )
    }

    /** Each assertion passes only after the write before it, and before the write after it. */
    @Test
    public fun runsTheCallsInOrder() {
        val result =
            runBatch(
                listOf(
                    POPULATE to preference("count", "1"),
                    ASSERT to preference("count", "1"),
                    POPULATE to preference("count", "2"),
                    ASSERT to preference("count", "2"),
                )
            )

        assertEquals(mapOf("status" to "success"), result.payload)
    }

    @Test
    public fun stopsAtTheFirstCallThatFailsAndReportsItsIndexAndItsMessage() {
        val missing = preference("missing", "x")
        val onItsOwn = AssertStorageAction().execute(context, BackupDeviceActionArgs(missing))

        val result =
            runBatch(
                listOf(
                    POPULATE to preference("theme", "dark"),
                    ASSERT to missing,
                    POPULATE to textFile("never written"),
                )
            )

        assertEquals(
            mapOf(
                "status" to "failure",
                "error" to "Preference 'missing' not found in '$PREFS'; expected 'x'.",
                "failed_call" to "1",
            ),
            result.payload,
        )
        assertEquals(onItsOwn.errorMessage, result.errorMessage)
        assertTrue(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("theme"))
        assertFalse(textFile.exists())
    }

    /**
     * The host reports a call that failed in a batch as it reports the call on its own, so a call
     * that throws must read as the runner reports it, cut to the same length.
     */
    @Test
    public fun reportsACallThatThrowsAsTheRunnerReportsItOnItsOwn() {
        for (message in listOf("No preferences", "x".repeat(2 * MAX_TEXT_CHARS))) {
            // The storage actions report an Exception as a failure, and let an Error through.
            val throwingContext =
                object : ContextWrapper(context) {
                    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                        throw NotImplementedError(message)
                }
            val onItsOwn =
                runWithRunner(
                    throwingContext,
                    mapOf("actionClass" to POPULATE) + preference("theme", "dark"),
                )

            val batch =
                runWithRunner(
                    throwingContext,
                    mapOf(
                        "actionClass" to BATCH,
                        "calls" to
                            callsJson(
                                listOf(
                                    POPULATE to textFile("written"),
                                    POPULATE to preference("theme", "dark"),
                                    ASSERT to textFile("never verified"),
                                )
                            ),
                    ),
                )

            val error = onItsOwn.getString("errorMessage")
            assertTrue(error, error.startsWith("Exception executing action: ${message.take(64)}"))
            assertEquals(message.length > MAX_TEXT_CHARS, error.endsWith(TRUNCATION_MARKER))
            assertFalse(batch.getBoolean("isSuccess"))
            val payload = JSONObject(batch.getString("payloadJson"))
            assertEquals(error, payload.getString("error"))
            assertEquals("1", payload.getString("failed_call"))
            assertEquals("failure", payload.getString("status"))
        }
    }

    /** A batch calls only the two storage actions, even when the runner could load the class. */
    @Test
    public fun runsNoCallWhenOneNamesAnotherAction() {
        val otherAction = OtherAction::class.java.name

        val result =
            runBatch(listOf(POPULATE to textFile("never written"), otherAction to emptyMap()))

        assertEquals(
            mapOf(
                "status" to "failure",
                "error" to "Invalid 'calls' argument: Unsupported action class $otherAction",
            ),
            result.payload,
        )
        assertFalse(textFile.exists())
    }

    @Test
    public fun failsWithoutAnIndexWhenTheCallsAreMissing() {
        val result = StorageBatchAction().execute(context, BackupDeviceActionArgs())

        assertEquals(
            mapOf("status" to "failure", "error" to "Missing 'calls' argument."),
            result.payload,
        )
    }

    @Test
    public fun runsNoCallWhenTheCallsAreInvalid() {
        val validCall =
            callsJson(listOf(POPULATE to textFile("never written"))).removeSurrounding("[", "]")
        val invalidCalls =
            listOf(
                "",
                "not JSON",
                "{}",
                "[$validCall,1]",
                """[$validCall,{"args":{}}]""",
                """[$validCall,{"action":"$POPULATE"}]""",
                """[$validCall,{"action":"$POPULATE","args":[]}]""",
            )

        for (calls in invalidCalls) {
            val result =
                StorageBatchAction()
                    .execute(context, BackupDeviceActionArgs(mapOf("calls" to calls)))

            assertEquals(calls, "failure", result.status)
            assertEquals(calls, setOf("status", "error"), result.payload.keys)
            val error = result.errorMessage.orEmpty()
            assertTrue("$calls: $error", error.startsWith("Invalid 'calls' argument: "))
            assertFalse(calls, textFile.exists())
        }
    }

    /** Arguments are strings, as in an arguments file. */
    @Test
    public fun runsNoCallWhenAnArgumentIsNotAString() {
        val args = JSONObject(textFile("never written")).put("value", 1)
        val calls = JSONArray().put(JSONObject().put("action", POPULATE).put("args", args))

        val result =
            StorageBatchAction()
                .execute(context, BackupDeviceActionArgs(mapOf("calls" to calls.toString())))

        assertEquals(
            mapOf(
                "status" to "failure",
                "error" to "Invalid 'calls' argument: The value of 'value' is not a string",
            ),
            result.payload,
        )
        assertFalse(textFile.exists())
    }

    /** The runner loads the batch by its class name, as it loads the storage actions. */
    @Test
    public fun runsTheCallsThatTheHostSendsToTheRunner() {
        val outcome = runWithRunner(context, mapOf("actionClass" to BATCH, "calls" to HOST_CALLS))

        assertTrue(outcome.toString(), outcome.getBoolean("isSuccess"))
        assertEquals("""{"status":"success"}""", outcome.getString("payloadJson"))
        assertEquals("it's \"here\"\n", textFile.readText())
    }

    /** A batch too long for the command line arrives in an arguments file, as any arguments do. */
    @Test
    public fun runsTheCallsThatArriveInAnArgumentsFile() {
        val argsFile = JSONObject().put("calls", HOST_CALLS).toString()

        val outcome =
            runWithRunner(
                context,
                mapOf("actionClass" to BATCH, "argsFile" to "/data/local/tmp/batch_args.json"),
                readFile = { argsFile },
            )

        assertTrue(outcome.toString(), outcome.getBoolean("isSuccess"))
        assertEquals("it's \"here\"\n", textFile.readText())
    }

    private fun runBatch(calls: List<Pair<String, Map<String, String>>>): BackupDeviceActionResult =
        StorageBatchAction()
            .execute(context, BackupDeviceActionArgs(mapOf("calls" to callsJson(calls))))

    /** Returns [calls], the class names and arguments of actions, as the host encodes them. */
    private fun callsJson(calls: List<Pair<String, Map<String, String>>>): String =
        JSONArray(
                calls.map { (action, args) ->
                    JSONObject().put("action", action).put("args", JSONObject(args))
                }
            )
            .toString()

    /**
     * Returns the outcome that the runner reports to the host for a run with the instrumentation
     * [arguments], in which the action gets [targetContext].
     */
    private fun runWithRunner(
        targetContext: Context,
        arguments: Map<String, String>,
        readFile: (String) -> String = { throw AssertionError("Unexpected read of $it") },
    ): JSONObject {
        val parts = mutableListOf<String>()
        val result =
            BackupRestoreTestRunner.runAction(
                Bundle().apply { arguments.forEach { (key, value) -> putString(key, value) } },
                InstrumentationRegistry.getInstrumentation().context,
                targetContext,
                readFile,
                sendStatus = { _, status -> parts += status.getString("resultPart")!! },
            )
        return JSONObject(result.getString("resultJson") ?: parts.joinToString(""))
    }

    private fun preference(key: String, value: String): Map<String, String> =
        mapOf("storage_type" to "PREFS", "pref_name" to PREFS, "pref_key" to key, "value" to value)

    private fun textFile(content: String): Map<String, String> =
        mapOf("storage_type" to "FILES", "path" to TEXT_FILE, "value" to content)

    /** An action that the runner can load, and that a batch must not call. */
    public class OtherAction : BackupDeviceAction {
        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = BackupDeviceActionResult.success()
    }

    private companion object {
        const val POPULATE = "androidx.test.backup.actions.PopulateStorageAction"
        const val ASSERT = "androidx.test.backup.actions.AssertStorageAction"
        const val BATCH = "androidx.test.backup.actions.StorageBatchAction"

        const val PREFS = "storage_batch_prefs"
        const val DATASTORE = "storage_batch_datastore"
        const val DATABASE = "storage_batch.db"
        const val TEXT_FILE = "storage_batch.txt"
        const val BINARY_FILE = "storage_batch.bin"

        /** Length to which the runner cuts an error message. */
        const val MAX_TEXT_CHARS = 50 * 1024

        /**
         * The calls that write and then verify [TEXT_FILE], as the host library encodes them, which
         * its `BackupActionWireProtocolTest` pins to the same string.
         */
        const val HOST_CALLS =
            """[{"action":"androidx.test.backup.actions.PopulateStorageAction",""" +
                """"args":{"storage_type":"FILES","path":"storage_batch.txt",""" +
                """"value":"it's \"here\"\n"}},""" +
                """{"action":"androidx.test.backup.actions.AssertStorageAction",""" +
                """"args":{"storage_type":"FILES","path":"storage_batch.txt",""" +
                """"value":"it's \"here\"\n"}}]"""
    }
}
