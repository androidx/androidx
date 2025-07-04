/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import java.time.LocalDateTime

@AppFunctionSerializable data class DateTime(val localDateTime: LocalDateTime)

class TestFunctions {
    @AppFunction
    fun add(appFunctionContext: AppFunctionContext, num1: Long, num2: Long) = num1 + num2

    @AppFunction
    fun logLocalDateTime(appFunctionContext: AppFunctionContext, dateTime: DateTime) {
        Log.d("TestFunctions", "LocalDateTime: ${dateTime.localDateTime}")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @AppFunction
    fun getLocalDate(appFunctionContext: AppFunctionContext): DateTime {
        return DateTime(localDateTime = LocalDateTime.now())
    }

    @AppFunction
    fun doThrow(appFunctionContext: AppFunctionContext) {
        throw AppFunctionInvalidArgumentException("invalid")
    }

    @AppFunction fun voidFunction(appFunctionContext: AppFunctionContext) {}

    @AppFunction fun enabledByDefault(appFunctionContext: AppFunctionContext) {}

    @AppFunction(isEnabled = false) fun disabledByDefault(appFunctionContext: AppFunctionContext) {}
}

class NotesFunctions : CreateNoteAppFunction<NotesFunctions.Parameters, NotesFunctions.Response> {

    @AppFunction
    override suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        parameters: Parameters,
    ): Response {
        return Response(MyNote(id = "testId", title = parameters.title))
    }

    @AppFunctionSerializable
    class MyNote(override val id: String, override val title: String) : AppFunctionNote

    @AppFunctionSerializable
    class Parameters(override val title: String) : CreateNoteAppFunction.Parameters

    @AppFunctionSerializable
    class Response(override val createdNote: MyNote) : CreateNoteAppFunction.Response
}
