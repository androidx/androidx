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

package androidx.appfunctions.testing

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionServiceEntryPoint(
    serviceName = "TestAppFunctionService",
    appFunctionXmlFileName = "test_app_function_service",
)
abstract class BaseTestAppFunctionService :
    AppFunctionService(), CreateNoteAppFunction<CreateNoteParameters, CreateNoteResponse> {
    @AppFunctionDeclaration(isDescribedByKDoc = false) fun add(num1: Long, num2: Long) = num1 + num2

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun logLocalDateTime(dateTime: DateTime) {
        Log.d("TestFunctions", "LocalDateTime: ${dateTime.localDateTime}")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun getLocalDate(): DateTime {
        return DateTime(localDateTime = LocalDateTime.now())
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun doThrow() {
        throw AppFunctionInvalidArgumentException("invalid")
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false) fun voidFunction() {}

    @AppFunctionDeclaration(isDescribedByKDoc = false) fun enabledByDefault() {}

    @AppFunctionDeclaration(isDescribedByKDoc = false, isEnabled = false) fun disabledByDefault() {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override suspend fun createNote(parameters: CreateNoteParameters): CreateNoteResponse {
        return CreateNoteResponse(MyNote(id = "testId", title = parameters.title))
    }
}

@AppFunctionSerializable(isDescribedByKDoc = false)
data class DateTime(val localDateTime: LocalDateTime)

@AppFunctionSerializable(isDescribedByKDoc = false)
class MyNote(override val id: String, override val title: String) : AppFunctionNote

@AppFunctionSerializable(isDescribedByKDoc = false)
class CreateNoteParameters(override val title: String) : CreateNoteAppFunction.Parameters

@AppFunctionSerializable(isDescribedByKDoc = false)
class CreateNoteResponse(override val createdNote: MyNote) : CreateNoteAppFunction.Response
