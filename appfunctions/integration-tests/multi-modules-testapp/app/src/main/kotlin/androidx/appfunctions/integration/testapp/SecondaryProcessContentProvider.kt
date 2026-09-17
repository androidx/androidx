/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.integration.testapp

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success.Companion.PROPERTY_RETURN_VALUE
import kotlinx.coroutines.runBlocking

/**
 * A [ContentProvider] running in a secondary process (:secondary_test_process) to test execution of
 * app functions from a caller in the same application but a different process.
 */
@RequiresApi(36)
class SecondaryProcessContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == METHOD_EXECUTE_APP_FUNCTION) {
            val functionId = checkNotNull(arg) { "Missing functionId" }
            val currentContext = checkNotNull(context)
            val manager = checkNotNull(AppFunctionManager.getInstance(currentContext))

            val response = runBlocking {
                manager.executeAppFunction(
                    ExecuteAppFunctionRequest(
                        targetPackageName = currentContext.packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )
                )
            }

            val resultBundle = Bundle()
            resultBundle.putInt(EXTRA_PID, Process.myPid())
            when (response) {
                is ExecuteAppFunctionResponse.Success -> {
                    resultBundle.putBoolean(EXTRA_IS_SUCCESS, true)
                    resultBundle.putString(
                        EXTRA_RETURN_VALUE,
                        response.returnValue.getString(PROPERTY_RETURN_VALUE),
                    )
                }
                is ExecuteAppFunctionResponse.Error -> {
                    resultBundle.putBoolean(EXTRA_IS_SUCCESS, false)
                    resultBundle.putString(
                        EXTRA_ERROR_CLASS,
                        response.error::class.java.name,
                    )
                }
            }
            return resultBundle
        }
        return super.call(method, arg, extras)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "androidx.appfunctions.integration.testapp.secondary_process_provider"
        const val METHOD_EXECUTE_APP_FUNCTION = "executeAppFunction"
        const val EXTRA_IS_SUCCESS = "is_success"
        const val EXTRA_RETURN_VALUE = "return_value"
        const val EXTRA_ERROR_CLASS = "error_class"
        const val EXTRA_PID = "pid"
    }
}
