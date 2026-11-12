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

import android.app.UiAutomation
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Tests how [BackupRestoreTestRunner] runs an action and reports its outcome to the host. */
@RunWith(AndroidJUnit4::class)
public class BackupRestoreTestRunnerTest {

    /** The statuses that the runner sent, in order. */
    private val statuses = mutableListOf<Pair<Int, Bundle>>()

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

    /** The action gets the arguments in the file, and none of the instrumentation arguments. */
    @Test
    public fun passesTheActionTheArgumentsInTheArgsFile() {
        val filesRead = mutableListOf<String>()

        val result =
            resultOf(
                runAction(
                    bundleOf(
                        "actionClass" to EchoArgsAction::class.java.name,
                        "argsFile" to ARGS_FILE,
                        "extra" to "not for the action",
                    ),
                    readFile = { path ->
                        filesRead += path
                        """{"key":"value","quote":"a\"b"}"""
                    },
                )
            )

        assertEquals(listOf(ARGS_FILE), filesRead)
        assertEquals(
            mapOf("key" to "value", "quote" to "a\"b", "status" to "success"),
            payloadOf(result),
        )
    }

    @Test
    public fun reportsAnArgsFileThatCannotBeRead() {
        val result =
            resultOf(
                runAction(
                    bundleOf(
                        "actionClass" to EchoArgsAction::class.java.name,
                        "argsFile" to ARGS_FILE,
                    ),
                    readFile = { throw IOException("No such file") },
                )
            )

        assertFalse(result.getBoolean("isSuccess"))
        assertEquals(
            "Failed to read the action arguments from $ARGS_FILE: No such file",
            result.getString("errorMessage"),
        )
    }

    @Test
    public fun reportsAnArgsFileWithAValueThatIsNotAString() {
        val result =
            resultOf(
                runAction(
                    bundleOf(
                        "actionClass" to EchoArgsAction::class.java.name,
                        "argsFile" to ARGS_FILE,
                    ),
                    readFile = { """{"count":1}""" },
                )
            )

        assertEquals(
            "Failed to read the action arguments from $ARGS_FILE: The value of 'count' is not a " +
                "string",
            result.getString("errorMessage"),
        )
    }

    /** A result too large for one Binder transaction goes to the host in parts, as statuses. */
    @Test
    public fun sendsAResultTooLargeForOneTransactionInParts() {
        val data = "x".repeat(3 * MAX_RESULT_CHARS_PER_TRANSACTION)

        val bundle =
            runAction(bundleOf("actionClass" to EchoArgsAction::class.java.name, "data" to data))

        assertNull(bundle.getString("resultJson"))
        val parts = statuses.map { (code, status) ->
            status.getString("resultPart")!!.also {
                assertEquals(1, code)
            }
        }
        assertEquals(parts.size.toString(), bundle.getString("resultParts"))
        assertTrue(parts.all { it.length <= MAX_RESULT_CHARS_PER_TRANSACTION })
        val outcomeJson = parts.joinToString("")
        assertEquals(outcomeJson.length.toString(), bundle.getString("resultLength"))
        assertEquals(
            mapOf("data" to data, "status" to "success"),
            payloadOf(JSONObject(outcomeJson)),
        )
    }

    /** A lone surrogate does not survive the trip to the host, so parts keep pairs whole. */
    @Test
    public fun neverSplitsASurrogatePairBetweenParts() {
        val emoji = "\uD83D\uDE00"
        // Each of the two lengths of padding puts a part boundary inside a pair for one of them.
        for (padding in listOf("", "a")) {
            statuses.clear()
            val data = padding + emoji.repeat(MAX_RESULT_CHARS_PER_TRANSACTION)

            runAction(bundleOf("actionClass" to EchoArgsAction::class.java.name, "data" to data))

            val parts = statuses.map { (_, status) -> status.getString("resultPart")!! }
            assertTrue(parts.size > 1)
            for (part in parts) {
                assertFalse(Character.isHighSurrogate(part.last()))
                assertFalse(Character.isLowSurrogate(part.first()))
            }
            assertEquals(data, payloadOf(JSONObject(parts.joinToString("")))["data"])
        }
    }

    @Test
    public fun truncatesALongStackTrace() {
        val result = runWith("actionClass" to LongMessageThrowingAction::class.java.name)

        val stackTrace = result.getString("stackTrace")
        assertTrue(stackTrace.startsWith("java.lang.IllegalStateException: xxx"))
        assertTrue(stackTrace.endsWith(TRUNCATION_MARKER))
        assertEquals(MAX_TEXT_CHARS + TRUNCATION_MARKER.length, stackTrace.length)
    }

    @Test
    public fun truncatesALongErrorMessage() {
        val result = runWith("actionClass" to LongMessageThrowingAction::class.java.name)

        val errorMessage = result.getString("errorMessage")
        assertTrue(errorMessage.startsWith("Exception executing action: xxx"))
        assertTrue(errorMessage.endsWith(TRUNCATION_MARKER))
        assertEquals(MAX_TEXT_CHARS + TRUNCATION_MARKER.length, errorMessage.length)
    }

    /** The host pushes the arguments file as the shell user, which the app itself can't read. */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public fun runsTheActionWithTheArgumentsInAFileThatTheShellWrote() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val path = "/data/local/tmp/backup_runner_test_${UUID.randomUUID()}.json"
        writeFileAsShell(path, """{"key":"value from the shell"}""")
        try {
            val result =
                resultOf(
                    runAction(
                        bundleOf(
                            "actionClass" to EchoArgsAction::class.java.name,
                            "argsFile" to path,
                        ),
                        readFile = { BackupRestoreTestRunner.readFileAsShell(instrumentation, it) },
                    )
                )

            assertEquals(
                mapOf("key" to "value from the shell", "status" to "success"),
                payloadOf(result),
            )
        } finally {
            runAsShell("rm -f $path")
        }
    }

    @Test
    public fun readFileAsShellReportsAFileThatDoesNotExist() {
        val path = "/data/local/tmp/backup_runner_test_${UUID.randomUUID()}.json"

        val e =
            assertThrows(IOException::class.java) {
                BackupRestoreTestRunner.readFileAsShell(
                    InstrumentationRegistry.getInstrumentation(),
                    path,
                )
            }

        assertEquals("$path is empty or can't be read", e.message)
    }

    /** The shell command is split on whitespace and runs without a shell. */
    @Test
    public fun readFileAsShellRejectsAPathThatTheCommandWouldMisread() {
        for (path in listOf("args.json", "-n", "/data/local/tmp/a b.json")) {
            val e =
                assertThrows(IOException::class.java) {
                    BackupRestoreTestRunner.readFileAsShell(
                        InstrumentationRegistry.getInstrumentation(),
                        path,
                    )
                }
            assertEquals("Not an absolute path without whitespace: '$path'", e.message)
        }
    }

    /** Runs the runner with the instrumentation [arguments], and returns the result it reports. */
    private fun runWith(vararg arguments: Pair<String, String>): JSONObject =
        resultOf(runAction(bundleOf(*arguments)))

    private fun runAction(
        arguments: Bundle?,
        readFile: (String) -> String = { throw AssertionError("Unexpected read of $it") },
    ): Bundle {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        return BackupRestoreTestRunner.runAction(
            arguments,
            instrumentation.context,
            instrumentation.targetContext,
            readFile,
            sendStatus = { code, status -> statuses += code to status },
        )
    }

    private fun bundleOf(vararg arguments: Pair<String, String>): Bundle =
        Bundle().apply { arguments.forEach { (key, value) -> putString(key, value) } }

    /**
     * Returns the result that the host reads from the result bundle of the runner, and from the
     * parts that the runner sent before it when the result is too large for one transaction.
     */
    private fun resultOf(bundle: Bundle): JSONObject =
        JSONObject(
            bundle.getString("resultJson")
                ?: statuses.joinToString("") { (_, status) -> status.getString("resultPart")!! }
        )

    private fun payloadOf(result: JSONObject): Map<String, String> =
        toMap(result.getString("payloadJson"))

    /** Returns the members of the JSON object [json], whose values are all strings. */
    private fun toMap(json: String): Map<String, String> {
        val jsonObject = JSONObject(json)
        return jsonObject.keys().asSequence().associateWith { jsonObject.getString(it) }
    }

    /** Writes [content] to [path] as the shell user, the way the host pushes a file. */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun writeFileAsShell(path: String, content: String) {
        val (stdout, stdin) = uiAutomation().executeShellCommandRw("tee $path")
        ParcelFileDescriptor.AutoCloseOutputStream(stdin).use { it.write(content.toByteArray()) }
        // tee echoes what it wrote, and ends once its input does.
        ParcelFileDescriptor.AutoCloseInputStream(stdout).use { it.readBytes() }
    }

    /** Runs [command] as the shell user and waits for it to end. */
    private fun runAsShell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(uiAutomation().executeShellCommand(command)).use {
            it.readBytes()
        }
    }

    private fun uiAutomation(): UiAutomation =
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation(UiAutomation.FLAG_DONT_USE_ACCESSIBILITY)

    /** Returns its arguments as its result. */
    public class EchoArgsAction : BackupDeviceAction {
        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = BackupDeviceActionResult.success(args.payload)
    }

    /** Reports a failed check, as an assertion does. */
    public class FailingAction : BackupDeviceAction {
        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = BackupDeviceActionResult.failure("Expected 'a' but found 'b'")
    }

    /** Crashes. */
    public class ThrowingAction : BackupDeviceAction {
        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = throw IllegalStateException("Action crashed")
    }

    /** Crashes with a message, and so a stack trace, longer than the runner reports. */
    public class LongMessageThrowingAction : BackupDeviceAction {
        override fun execute(
            context: Context,
            args: BackupDeviceActionArgs,
        ): BackupDeviceActionResult = throw IllegalStateException("x".repeat(2 * MAX_TEXT_CHARS))
    }

    private companion object {
        /** Length of the longest result that the runner sends in one Binder transaction. */
        const val MAX_RESULT_CHARS_PER_TRANSACTION = 64 * 1024

        /** Length to which the runner cuts an error message or a stack trace. */
        const val MAX_TEXT_CHARS = 50 * 1024

        /** A device path for an arguments file that is never read from the device. */
        const val ARGS_FILE = "/data/local/tmp/backup_args_test.json"
    }
}
