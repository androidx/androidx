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

package androidx.test.backup

import android.content.Context
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Tests how [BackupRestoreTestRunner] runs an action and reports its outcome to the host. */
@RunWith(AndroidJUnit4::class)
public class BackupRestoreTestRunnerTest {

    /** The arguments that the runner reads itself are not the action's. */
    @Test
    public fun passesTheActionOnlyItsOwnArguments() {
        val result =
            runWith(
                "actionClass" to EchoArgsAction::class.java.name,
                "debug" to "false",
                "redirect_dir" to "/data/local/tmp",
                "key" to "value",
            )

        assertTrue(result.getBoolean("isSuccess"))
        assertEquals(mapOf("key" to "value", "status" to "success"), payloadOf(result))
    }

    @Test
    public fun reportsTheFailureThatTheActionReturns() {
        val result = runWith("actionClass" to FailingAction::class.java.name)

        assertFalse(result.getBoolean("isSuccess"))
        assertEquals("Expected 'a' but found 'b'", result.getString("errorMessage"))
        assertEquals("failure", payloadOf(result)["status"])
    }

    @Test
    public fun reportsAnExceptionOfTheActionWithItsStackTrace() {
        val result = runWith("actionClass" to ThrowingAction::class.java.name)

        assertFalse(result.getBoolean("isSuccess"))
        assertEquals("Exception executing action: Action crashed", result.getString("errorMessage"))
        assertTrue(result.getString("stackTrace").contains("ThrowingAction.execute"))
        assertFalse(result.has("payloadJson"))
    }

    @Test
    public fun reportsAnActionClassThatCannotBeLoaded() {
        val result = runWith("actionClass" to "com.example.MissingAction")

        assertFalse(result.getBoolean("isSuccess"))
        assertEquals(
            "Exception executing action: Could not load class com.example.MissingAction",
            result.getString("errorMessage"),
        )
    }

    @Test
    public fun reportsAMissingActionClass() {
        val result = runWith("key" to "value")

        assertFalse(result.getBoolean("isSuccess"))
        assertEquals("No actionClass parameter specified.", result.getString("errorMessage"))
    }

    @Test
    public fun reportsMissingArguments() {
        val result = resultOf(runAction(arguments = null))

        assertFalse(result.getBoolean("isSuccess"))
        assertEquals("No instrumentation arguments received.", result.getString("errorMessage"))
    }

    /** A payload too large for a Binder transaction is written to a file in `redirect_dir`. */
    @Test
    public fun writesAPayloadTooLargeForBinderToAFile() {
        val redirectDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val data = "x".repeat(MAX_BINDER_PAYLOAD_BYTES)

        val result =
            runWith(
                "actionClass" to EchoArgsAction::class.java.name,
                "redirect_dir" to redirectDir.absolutePath,
                "data" to data,
            )

        val overflowFile = File(result.getString("payload_path"))
        try {
            assertTrue(result.getBoolean("isSuccess"))
            assertFalse(result.has("payloadJson"))
            assertEquals(redirectDir.absolutePath, overflowFile.parent)
            val payloadJson = JSONObject(overflowFile.readText()).getString("payloadJson")
            assertEquals(mapOf("data" to data, "status" to "success"), toMap(payloadJson))
        } finally {
            overflowFile.delete()
        }
    }

    @Test
    public fun truncatesALongStackTrace() {
        val result = runWith("actionClass" to LongMessageThrowingAction::class.java.name)

        val stackTrace = result.getString("stackTrace")
        assertTrue(stackTrace.startsWith("java.lang.IllegalStateException: xxx"))
        assertTrue(stackTrace.endsWith(TRUNCATION_MARKER))
        assertEquals(MAX_STACK_TRACE_CHARS + TRUNCATION_MARKER.length, stackTrace.length)
    }

    /** Runs the runner with the instrumentation [arguments], and returns the result it reports. */
    private fun runWith(vararg arguments: Pair<String, String>): JSONObject =
        resultOf(
            runAction(
                Bundle().apply { arguments.forEach { (key, value) -> putString(key, value) } }
            )
        )

    private fun runAction(arguments: Bundle?): Bundle {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        return BackupRestoreTestRunner.runAction(
            arguments,
            instrumentation.context,
            instrumentation.targetContext,
        )
    }

    /** Returns the result that the host reads from the result bundle of the runner. */
    private fun resultOf(bundle: Bundle): JSONObject = JSONObject(bundle.getString("resultJson")!!)

    private fun payloadOf(result: JSONObject): Map<String, String> =
        toMap(result.getString("payloadJson"))

    /** Returns the members of the JSON object [json], whose values are all strings. */
    private fun toMap(json: String): Map<String, String> {
        val jsonObject = JSONObject(json)
        return jsonObject.keys().asSequence().associateWith { jsonObject.getString(it) }
    }

    /** Returns its arguments as its result. */
    public class EchoArgsAction : BackupDeviceAction {
        override val phase: Int = BackupDeviceAction.PHASE_VERIFY

        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = BackupDeviceActionResult.success(args.payload)
    }

    /** Reports a failed check, as an assertion does. */
    public class FailingAction : BackupDeviceAction {
        override val phase: Int = BackupDeviceAction.PHASE_VERIFY

        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = BackupDeviceActionResult.failure("Expected 'a' but found 'b'")
    }

    /** Crashes. */
    public class ThrowingAction : BackupDeviceAction {
        override val phase: Int = BackupDeviceAction.PHASE_VERIFY

        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = throw IllegalStateException("Action crashed")
    }

    /** Crashes with a message, and so a stack trace, longer than the runner reports. */
    public class LongMessageThrowingAction : BackupDeviceAction {
        override val phase: Int = BackupDeviceAction.PHASE_VERIFY

        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult =
            throw IllegalStateException("x".repeat(2 * MAX_STACK_TRACE_CHARS))
    }

    private companion object {
        /** Size of a payload, in UTF-8 bytes, above which the runner writes it to a file. */
        const val MAX_BINDER_PAYLOAD_BYTES = 500 * 1024

        /** Length to which the runner cuts a stack trace. */
        const val MAX_STACK_TRACE_CHARS = 50 * 1024

        /** What the runner appends to a stack trace that it cut. */
        const val TRUNCATION_MARKER = "\n... [truncated]"
    }
}
