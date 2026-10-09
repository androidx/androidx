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
import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID
import org.json.JSONObject

/**
 * Executes on-device backup actions dispatched by host orchestrators.
 *
 * Stays dormant until the host orchestrator asks for an action, runs the requested
 * [BackupDeviceAction], and returns its result.
 */
/*
 * NOTE: The host wakes this runner up via `am instrument`, and the action class is loaded and
 * instantiated dynamically by name. Results larger than MAX_BINDER_PAYLOAD_SIZE_BYTES are written
 * to a file and referenced by path, because the Binder transaction cannot carry them.
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
        finish(Activity.RESULT_OK, runAction(arguments, context, targetContext))
    }

    internal companion object {
        /** The argument naming the fully-qualified class of the action to run. */
        private const val KEY_ACTION_CLASS = "actionClass"

        /** The argument that, when "true", makes the runner wait for a debugger to attach. */
        private const val KEY_DEBUG = "debug"

        /** The argument naming the directory that receives a payload too large for Binder. */
        private const val KEY_REDIRECT_DIR = "redirect_dir"

        /** The arguments that the runner reads itself, which are not passed to the action. */
        private val RUNNER_KEYS = setOf(KEY_ACTION_CLASS, KEY_DEBUG, KEY_REDIRECT_DIR)

        /** The result bundle key enclosing the JSON-serialized execution outcome. */
        private const val KEY_RESULT_JSON = "resultJson"

        /**
         * The maximum size of the JSON payload (in bytes) before switching to disk storage to
         * prevent Binder overflow transactions.
         */
        private const val MAX_BINDER_PAYLOAD_SIZE_BYTES = 500 * 1024

        /** The maximum length of an unhandled exception stack trace passed over Binder (50 KB). */
        private const val MAX_STACK_TRACE_LENGTH_CHARS = 50 * 1024

        /**
         * Runs the action that the instrumentation [arguments] name, and returns the result bundle
         * that reports its outcome.
         *
         * @param testContext context of the test APK, whose class loader loads the action
         * @param targetContext context of the app under test, which the action runs in
         */
        internal fun runAction(
            arguments: Bundle?,
            testContext: Context,
            targetContext: Context,
        ): Bundle {
            if (arguments == null) {
                return result(
                    isSuccess = false,
                    errorMessage = "No instrumentation arguments received.",
                )
            }
            val actionClassName =
                arguments.getString(KEY_ACTION_CLASS)
                    ?: return result(
                        isSuccess = false,
                        errorMessage = "No actionClass parameter specified.",
                    )

            if (arguments.getString(KEY_DEBUG).toBoolean()) {
                Debug.waitForDebugger()
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

                val actionArgs = buildMap {
                    for (key in arguments.keySet()) {
                        if (key !in RUNNER_KEYS) arguments.getString(key)?.let { put(key, it) }
                    }
                }
                val actionResult =
                    actionInstance.execute(targetContext, BackupDeviceActionArgs(actionArgs))

                result(
                    isSuccess = actionResult.isSuccess,
                    errorMessage = actionResult.errorMessage,
                    payloadJson = JSONObject(actionResult.payload).toString(),
                    overflowDir = { overflowDir(arguments, targetContext) },
                )
            } catch (e: Throwable) {
                val stackTrace = StringWriter()
                e.printStackTrace(PrintWriter(stackTrace))
                result(
                    isSuccess = false,
                    errorMessage = "Exception executing action: ${e.message ?: e.toString()}",
                    stackTrace = stackTrace.toString(),
                )
            }
        }

        /**
         * Returns the result bundle reporting an outcome. A [payloadJson] too large for Binder is
         * written to a file in [overflowDir], and referenced by its path.
         */
        private fun result(
            isSuccess: Boolean,
            errorMessage: String?,
            payloadJson: String? = null,
            stackTrace: String? = null,
            overflowDir: () -> File = { error("No overflow directory for a payload") },
        ): Bundle {
            val result = mutableMapOf<String, Any?>()
            result["isSuccess"] = isSuccess
            result["errorMessage"] = errorMessage

            // Bound the stack trace so the result stays within a Binder transaction.
            val safeStackTrace =
                stackTrace?.let {
                    if (it.length > MAX_STACK_TRACE_LENGTH_CHARS) {
                        it.substring(0, MAX_STACK_TRACE_LENGTH_CHARS) + "\n... [truncated]"
                    } else {
                        it
                    }
                } ?: ""
            result["stackTrace"] = safeStackTrace

            if (payloadJson != null) {
                val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
                if (payloadBytes.size > MAX_BINDER_PAYLOAD_SIZE_BYTES) {
                    val overflowFile = File(overflowDir(), "overflow_${UUID.randomUUID()}.json")
                    try {
                        overflowFile.parentFile?.mkdirs()
                        val fileMap = mapOf<String, Any?>("payloadJson" to payloadJson)
                        overflowFile.writeText(JSONObject(fileMap).toString())
                        result["payload_path"] = overflowFile.absolutePath
                    } catch (e: Throwable) {
                        result["errorMessage"] =
                            "Failed to write overflow payload file: " + e.message
                        result["isSuccess"] = false
                    }
                } else {
                    result["payloadJson"] = payloadJson
                }
            }

            return Bundle().apply { putString(KEY_RESULT_JSON, JSONObject(result).toString()) }
        }

        private fun overflowDir(arguments: Bundle, targetContext: Context): File {
            val redirectDir = arguments.getString(KEY_REDIRECT_DIR)
            if (!redirectDir.isNullOrEmpty()) {
                val file = File(redirectDir)
                if (file.exists() || file.mkdirs()) {
                    return file
                }
            }
            return targetContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: targetContext.cacheDir
        }
    }
}
