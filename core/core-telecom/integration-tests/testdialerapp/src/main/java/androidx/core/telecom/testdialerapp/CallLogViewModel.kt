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
package androidx.core.telecom.testdialerapp

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog.Calls
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallLogEntry(val uri: Uri, val displayName: String, val number: String, val date: Long)

class CallLogViewModel(application: Application) : AndroidViewModel(application) {
    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callLogs: StateFlow<List<CallLogEntry>> = _callLogs.asStateFlow()

    fun loadVoipCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val projection =
                arrayOf(Calls._ID, Calls.CACHED_NAME, Calls.NUMBER, Calls.UUID, Calls.DATE)

            val queryArgs =
                Bundle().apply {
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(Calls.DATE))
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
                    )
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 50)
                }

            val contentResolver = getApplication<Application>().contentResolver
            val callLogItems = mutableListOf<CallLogEntry>()

            // This is the URI that includes VoIP calls
            val queryUri =
                Calls.CONTENT_URI.buildUpon()
                    .appendQueryParameter("include_voip_calls", "true")
                    .build()

            val cursor = contentResolver.query(queryUri, projection, queryArgs, null)

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(Calls._ID)
                val nameColumn = it.getColumnIndexOrThrow(Calls.CACHED_NAME)
                val numberColumn = it.getColumnIndexOrThrow(Calls.NUMBER)
                val uuidColumn = it.getColumnIndexOrThrow(Calls.UUID)
                val dateColumn = it.getColumnIndexOrThrow(Calls.DATE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val number = it.getString(numberColumn)
                    val uuid = it.getString(uuidColumn)
                    val date = it.getLong(dateColumn)

                    val entryUri = ContentUris.withAppendedId(queryUri, id)

                    callLogItems.add(
                        CallLogEntry(
                            uri = entryUri,
                            displayName = name ?: uuid ?: "Unknown",
                            number = number,
                            date = date,
                        )
                    )
                }
            }
            _callLogs.value = callLogItems
        }
    }
}
