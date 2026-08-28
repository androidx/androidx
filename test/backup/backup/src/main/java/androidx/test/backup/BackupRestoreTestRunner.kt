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

import android.app.Instrumentation
import android.os.Bundle
import java.io.File
import java.util.UUID

/**
 * Executes on-device backup actions dispatched by host orchestrators.
 *
 * Stays dormant until woken up by the host orchestrator (via `am instrument`), dynamically loads
 * and instantiates requested [BackupDeviceAction]s, and returns structured execution results.
 * Handles Binder payload overflow redirection for large payloads.
 */
/*
 * NOTE: This class MUST remain public. Custom `Instrumentation` subclasses must be public because
 * the Android operating system platform's system server (`system_server` via
 * `ActivityManagerService`) dynamically loads and instantiates the instrumentation class via
 * reflection from outside the package/module boundary. Making it internal or private will result in
 * a runtime security/instantiation exception on startup.
 */
public class BackupRestoreTestRunner : Instrumentation() {

    internal companion object {
        internal lateinit var instance: Instrumentation

        /** The intent extra key representing the fully-qualified action class name to run. */
        private const val KEY_ACTION_CLASS = "actionClass"

        /** The result bundle key enclosing the JSON-serialized execution outcome. */
        private const val KEY_RESULT_JSON = "resultJson"

        /**
         * The maximum size of the JSON payload (in bytes) before switching to disk storage to
         * prevent Binder overflow transactions.
         */
        private const val MAX_BINDER_PAYLOAD_SIZE_BYTES = 500 * 1024

        /** The maximum length of an unhandled exception stack trace passed over Binder (50 KB). */
        private const val MAX_STACK_TRACE_LENGTH_CHARS = 50 * 1024
    }

    private var arguments: Bundle? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        instance = this
        this.arguments = arguments
        start()
    }

    override fun onStart() {
        super.onStart()

        val args = arguments
        if (args == null) {
            reportResult(false, "No instrumentation arguments received.", null, null, null)
            return
        }

        val actionClassName = args.getString(KEY_ACTION_CLASS)
        if (actionClassName == null) {
            reportResult(false, "No actionClass parameter specified.", null, null, null)
            return
        }

        try {
            val actionClass =
                try {
                    context.classLoader.loadClass(actionClassName)
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

            // Convert the raw arguments bundle to a platform-agnostic Map
            val argsMap = mutableMapOf<String, String>()
            for (key in args.keySet()) {
                args.getString(key)?.let { argsMap[key] = it }
            }

            // Execute the action and get the structured results
            val result = actionInstance.execute(targetContext, BackupDeviceActionArgs(argsMap))

            // Convert the returned payload into a flat map for JSON serialization (Application
            // Data)
            val resultMap = mutableMapOf<String, Any?>()
            for ((key, value) in result.payload) {
                resultMap[key] = value
            }
            val payloadJson = mapToJson(resultMap)

            reportResult(true, null, payloadJson, warnings = null, infos = null)
        } catch (e: Throwable) {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            val exceptionMessage = e.message ?: e.toString()
            reportResult(
                isSuccess = false,
                errorMessage = "Exception executing action: $exceptionMessage",
                payloadJson = null,
                warnings = null,
                infos = null,
                stackTrace = sw.toString(),
            )
        }
    }

    private fun reportResult(
        isSuccess: Boolean,
        errorMessage: String?,
        payloadJson: String?,
        warnings: String?,
        infos: String?,
        stackTrace: String? = null,
    ) {
        val result = mutableMapOf<String, Any?>()
        result["isSuccess"] = isSuccess
        result["errorMessage"] = errorMessage
        result["warnings"] = warnings ?: ""
        result["infos"] = infos ?: ""

        // Truncate stackTrace to prevent Binder payload exhaustion on huge exceptions (50 KB max)
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
                val overflowFile = File(getOverflowDir(), "overflow_${UUID.randomUUID()}.json")
                try {
                    overflowFile.parentFile?.mkdirs()
                    val fileMap = mapOf<String, Any?>("payloadJson" to payloadJson)
                    overflowFile.writeText(mapToJson(fileMap))
                    result["payload_path"] = overflowFile.absolutePath
                } catch (e: Throwable) {
                    result["errorMessage"] = "Failed to write overflow payload file: " + e.message
                    result["isSuccess"] = false
                }
            } else {
                result["payloadJson"] = payloadJson
            }
        }

        val jsonString = mapToJson(result)
        val out = java.io.PrintStream(System.out)
        out.println("BACKUP_RESTORE_RESULT: $jsonString")
        out.flush()

        val bundle = Bundle()
        bundle.putString(KEY_RESULT_JSON, jsonString)
        finish(android.app.Activity.RESULT_OK, bundle)
    }

    private fun getOverflowDir(): File {
        val redirectDir = arguments?.getString("redirect_dir")
        if (!redirectDir.isNullOrEmpty()) {
            val file = File(redirectDir)
            if (file.exists() || file.mkdirs()) {
                return file
            }
        }
        val targetCtx = targetContext
        return targetCtx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: targetCtx.cacheDir
    }

    private fun mapToJson(map: Map<String, Any?>): String {
        return org.json.JSONObject(map).toString()
    }
}
