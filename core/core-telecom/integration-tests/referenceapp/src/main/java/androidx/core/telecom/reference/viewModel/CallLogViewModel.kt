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

package androidx.core.telecom.reference.viewModel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Data class to hold a single call log entry
data class CallLogItem(
    val displayName: String,
    val number: String,
    val date: String,
    val callbackUri: Uri,
)

class CallLogViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _callLogEntries = MutableStateFlow<List<CallLogItem>>(emptyList())
    val callLogEntries: StateFlow<List<CallLogItem>> = _callLogEntries

    @RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
    fun loadCallLog() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = mutableListOf<CallLogItem>()
            val projection =
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.UUID,
                )

            // Query only VoIP calls made by this app
            val selection = "${CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME} LIKE ?"
            val selectionArgs = arrayOf("${app.packageName}%")

            app.contentResolver
                .query(
                    CallLog.Calls.CONTENT_URI.buildUpon()
                        .appendQueryParameter("include_voip_calls", "true")
                        .build(),
                    projection,
                    selection,
                    selectionArgs,
                    "${CallLog.Calls.DATE} DESC", // Sort by date descending
                )
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val numberColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val dateColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val number = cursor.getString(numberColumn)
                        val date = cursor.getLong(dateColumn)

                        val displayName = name.takeIf { !it.isNullOrEmpty() } ?: "Unknown"
                        val formattedDate =
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                .format(Date(date))
                        val callbackUri = ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id)

                        entries.add(CallLogItem(displayName, number, formattedDate, callbackUri))
                    }
                } ?: Log.i("CallLogColumn", "Query returned null cursor.")
            _callLogEntries.value = entries
        }
    }
}
