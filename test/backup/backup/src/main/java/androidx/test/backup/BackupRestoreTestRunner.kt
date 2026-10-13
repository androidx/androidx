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

import android.app.Activity
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.os.ParcelFileDescriptor
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import org.json.JSONObject

/**
 * Executes on-device backup actions dispatched by host orchestrators.
 *
 * Stays dormant until the host orchestrator asks for an action, runs the requested
 * [BackupDeviceAction], and returns its result.
 */
/*
 * NOTE: The host wakes this runner up via `am instrument`, and the action class is loaded and
 * instantiated dynamically by name.
 *
 * Arguments too large for the `am instrument` command line arrive in a file that the host pushed to
 * the device, which the runner reads as the shell user. A result too large for one Binder
 * transaction goes back in parts, each sent as an instrumentation status, which `am instrument`
 * prints before the final result.
 *
 * NOTE: This class MUST remain public. Custom `Instrumentation` subclasses must be public because
 * the Android operating system platform's system server (`system_server` via
 * `ActivityManagerService`) dynamically loads and instantiates the instrumentation class via
 * reflection from outside the package/module boundary. Making it internal or private will result in
 * a runtime security/instantiation exception on startup.
 */
public class BackupRestoreTestRunner : Instrumentation() {

    private var arguments: Bundle? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        this.arguments = arguments
        start()
    }

    override fun onStart() {
        super.onStart()
        finish(
            Activity.RESULT_OK,
            runAction(
                arguments,
                context,
                targetContext,
                readFile = { path -> readFileAsShell(this, path) },
                sendStatus = ::sendStatus,
            ),
        )
    }

    internal companion object {
        /** The argument naming the fully-qualified class of the action to run. */
        private const val KEY_ACTION_CLASS = "actionClass"

        /** The argument that, when "true", makes the runner wait for a debugger to attach. */
        private const val KEY_DEBUG = "debug"

        /**
         * The argument naming the device file that holds the arguments of the action, as a JSON
         * object of strings. Without it, the action gets the other instrumentation arguments.
         */
        private const val KEY_ARGS_FILE = "argsFile"

        /** An argument that older hosts send, which the runner ignores. */
        private const val KEY_REDIRECT_DIR = "redirect_dir"

        /** The arguments that the runner reads itself, which are not passed to the action. */
        private val RUNNER_KEYS =
            setOf(KEY_ACTION_CLASS, KEY_DEBUG, KEY_ARGS_FILE, KEY_REDIRECT_DIR)

        /** The result bundle key enclosing the JSON-serialized execution outcome. */
        private const val KEY_RESULT_JSON = "resultJson"

        /** The status key of one part of a result that is sent in parts. */
        private const val KEY_RESULT_PART = "resultPart"

        /** The result bundle key with the number of parts of a result that was sent in parts. */
        private const val KEY_RESULT_PARTS = "resultParts"

        /** The result bundle key with the length of a result that was sent in parts. */
        private const val KEY_RESULT_LENGTH = "resultLength"

        /** The code of the statuses that carry the parts of a result. */
        private const val RESULT_PART_STATUS_CODE = 1

        /**
         * The maximum length, in characters, of a result in one Binder transaction. A Bundle holds
         * each character in two bytes, so this keeps a transaction far below the 1 MB Binder limit.
         */
        private const val MAX_RESULT_CHARS_PER_TRANSACTION = 64 * 1024

        /** The maximum length of an error message or a stack trace in a result (50 KB). */
        private const val MAX_TEXT_LENGTH_CHARS = 50 * 1024

        /**
         * Returns the content of the device file at [path], read as the shell user through the
         * `UiAutomation` of [instrumentation]. The shell user can read the files that the host
         * pushes to the device, which the app can't.
         *
         * @throws IOException if the file can't be read
         */
        internal fun readFileAsShell(instrumentation: Instrumentation, path: String): String {
            // The command is split on whitespace and runs without a shell, so a path must have no
            // whitespace to be one argument, and must be absolute not to be taken for an option.
            if (!path.startsWith("/") || path.any { it.isWhitespace() }) {
                throw IOException("Not an absolute path without whitespace: '$path'")
            }
            val automation =
                instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_USE_ACCESSIBILITY)
                    ?: throw IOException("Can't connect to UiAutomation to read $path")
            val content =
                ParcelFileDescriptor.AutoCloseInputStream(
                        automation.executeShellCommand("cat $path")
                    )
                    .use { it.readBytes().toString(Charsets.UTF_8) }
            // cat prints why it fails on stderr, which this doesn't read, and nothing on stdout.
            if (content.isEmpty()) throw IOException("$path is empty or can't be read")
            return content
        }

        /**
         * Runs the action that the instrumentation [arguments] name, and returns the result bundle
         * that reports its outcome.
         *
         * @param testContext context of the test APK, whose class loader loads the action
         * @param targetContext context of the app under test, which the action runs in
         * @param readFile returns the content of a device file that the host pushed
         * @param sendStatus sends an instrumentation status to the host, which receives it before
         *   the call returns
         */
        internal fun runAction(
            arguments: Bundle?,
            testContext: Context,
            targetContext: Context,
            readFile: (path: String) -> String,
            sendStatus: (resultCode: Int, results: Bundle) -> Unit,
        ): Bundle =
            resultBundle(outcomeOf(arguments, testContext, targetContext, readFile), sendStatus)

        /** Runs the action that [arguments] name, and returns its outcome as a JSON object. */
        private fun outcomeOf(
            arguments: Bundle?,
            testContext: Context,
            targetContext: Context,
            readFile: (path: String) -> String,
        ): String {
            if (arguments == null) {
                return outcome(
                    isSuccess = false,
                    errorMessage = "No instrumentation arguments received.",
                )
            }
            val actionClassName =
                arguments.getString(KEY_ACTION_CLASS)
                    ?: return outcome(
                        isSuccess = false,
                        errorMessage = "No actionClass parameter specified.",
                    )

            if (arguments.getString(KEY_DEBUG).toBoolean()) {
                Debug.waitForDebugger()
            }

            val argsFile = arguments.getString(KEY_ARGS_FILE)
            val actionArgs =
                if (argsFile != null) {
                    try {
                        parseArgsFile(readFile(argsFile))
                    } catch (e: Exception) {
                        return outcome(
                            isSuccess = false,
                            errorMessage =
                                "Failed to read the action arguments from $argsFile: ${e.message}",
                        )
                    }
                } else {
                    buildMap {
                        for (key in arguments.keySet()) {
                            if (key !in RUNNER_KEYS) arguments.getString(key)?.let { put(key, it) }
                        }
                    }
                }

            return try {
                val actionClass =
                    try {
                        testContext.classLoader.loadClass(actionClassName)
                    } catch (e: Throwable) {
                        try {
                            targetContext.classLoader.loadClass(actionClassName)
                        } catch (cnfe: Throwable) {
                            try {
                                Class.forName(actionClassName)
                            } catch (classNotFound: Throwable) {
                                throw ClassNotFoundException(
                                    "Could not load class $actionClassName",
                                    classNotFound,
                                )
                            }
                        }
                    }
                val actionInstance =
                    actionClass.getDeclaredConstructor().newInstance() as BackupDeviceAction

                val actionResult =
                    actionInstance.execute(targetContext, BackupDeviceActionArgs(actionArgs))

                outcome(
                    isSuccess = actionResult.isSuccess,
                    errorMessage = actionResult.errorMessage,
                    payloadJson = JSONObject(actionResult.payload).toString(),
                )
            } catch (e: Throwable) {
                val stackTrace = StringWriter()
                e.printStackTrace(PrintWriter(stackTrace))
                outcome(
                    isSuccess = false,
                    errorMessage = "Exception executing action: ${e.message ?: e.toString()}",
                    stackTrace = stackTrace.toString(),
                )
            }
        }

        /**
         * Returns the arguments in [json], a JSON object whose values are all strings.
         *
         * @throws org.json.JSONException if [json] is not a JSON object
         * @throws IllegalArgumentException if a value is not a string
         */
        private fun parseArgsFile(json: String): Map<String, String> {
            val jsonObject = JSONObject(json)
            return jsonObject.keys().asSequence().associateWith { key ->
                jsonObject.get(key) as? String
                    ?: throw IllegalArgumentException("The value of '$key' is not a string")
            }
        }

        /**
         * Returns an outcome as the JSON object that the host decodes. The error message and the
         * stack trace are bounded, so that a huge one can't hide the rest of the outcome.
         */
        private fun outcome(
            isSuccess: Boolean,
            errorMessage: String?,
            payloadJson: String? = null,
            stackTrace: String? = null,
        ): String {
            val outcome = mutableMapOf<String, Any?>()
            outcome["isSuccess"] = isSuccess
            outcome["errorMessage"] = errorMessage?.let { truncated(it, MAX_TEXT_LENGTH_CHARS) }
            outcome["stackTrace"] = stackTrace?.let { truncated(it, MAX_TEXT_LENGTH_CHARS) } ?: ""
            if (payloadJson != null) outcome["payloadJson"] = payloadJson
            return JSONObject(outcome).toString()
        }

        /**
         * Returns the result bundle that reports [outcomeJson].
         *
         * An outcome too large for one Binder transaction is sent in parts with [sendStatus] first,
         * and the result bundle then names how many parts there are.
         */
        private fun resultBundle(
            outcomeJson: String,
            sendStatus: (resultCode: Int, results: Bundle) -> Unit,
        ): Bundle {
            if (outcomeJson.length <= MAX_RESULT_CHARS_PER_TRANSACTION) {
                return Bundle().apply { putString(KEY_RESULT_JSON, outcomeJson) }
            }
            var parts = 0
            var start = 0
            while (start < outcomeJson.length) {
                val end =
                    surrogateSafeEnd(
                        outcomeJson,
                        minOf(start + MAX_RESULT_CHARS_PER_TRANSACTION, outcomeJson.length),
                    )
                sendStatus(
                    RESULT_PART_STATUS_CODE,
                    Bundle().apply {
                        putString(KEY_RESULT_PART, outcomeJson.substring(start, end))
                    },
                )
                parts++
                start = end
            }
            return Bundle().apply {
                putString(KEY_RESULT_PARTS, parts.toString())
                putString(KEY_RESULT_LENGTH, outcomeJson.length.toString())
            }
        }
    }
}
