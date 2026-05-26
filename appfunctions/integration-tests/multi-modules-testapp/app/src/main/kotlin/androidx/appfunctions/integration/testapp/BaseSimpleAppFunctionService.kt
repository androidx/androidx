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

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunction.integration.test.sharedschema.MultiServiceCreateNoteParams
import androidx.appfunction.integration.test.sharedschema.MultiServiceFilesData
import androidx.appfunction.integration.test.sharedschema.MultiServiceNote
import androidx.appfunction.integration.test.sharedschema.MultiServiceProxyTypesWrapper
import androidx.appfunctions.AppFunctionEntryPoint
import androidx.appfunctions.AppFunctionIntValueConstraint
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionStringValueConstraint
import androidx.appfunctions.AppFunctionUriGrant
import androidx.appfunctions.service.AppFunction

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionEntryPoint(
    serviceName = "SimpleAppFunctionService",
    appFunctionXmlFileName = "simple_app_function_service",
)
abstract class BaseSimpleAppFunctionService : AppFunctionService() {
    @AppFunction
    fun echoProxyTypes(value: MultiServiceProxyTypesWrapper): MultiServiceProxyTypesWrapper = value

    @AppFunction
    fun doThrow() {
        throw AppFunctionInvalidArgumentException("invalid")
    }

    @AppFunction
    fun enumValueFunction(
        @AppFunctionIntValueConstraint(enumValues = [0, 1]) intEnum: Int,
        @AppFunctionStringValueConstraint(enumValues = ["A", "B"]) stringEnum: String,
    ) {}

    @AppFunction
    fun getFilesData(): MultiServiceFilesData {
        return MultiServiceFilesData(
            readOnlyUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/read_only_test_file.txt"
                        ),
                    modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
                ),
            writeOnlyUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/write_only_test_file.txt"
                        ),
                    modeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ),
            readWriteUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/read_write_test_file.txt"
                        ),
                    modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ),
            persistReadWriteUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/persist_read_write_test_file.txt"
                        ),
                    modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                ),
        )
    }

    /**
     * Multiservice to create note.
     *
     * @param createNoteParams Multi-service's createNoteParams.
     * @return The multiservice node.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun createNote(createNoteParams: MultiServiceCreateNoteParams): MultiServiceNote {
        return MultiServiceNote(title = createNoteParams.title, content = createNoteParams.content)
    }
}
