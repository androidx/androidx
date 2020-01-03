/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection.test

import androidx.sqlite.inspection.SqliteInspectorProtocol
import androidx.sqlite.inspection.SqliteInspectorProtocol.CellValue
import androidx.sqlite.inspection.SqliteInspectorProtocol.CellValue.ValueCase

val CellValue.value: Any? get() = valueType.first
val CellValue.type: String get() = valueType.second
val CellValue.valueType: Pair<Any?, String>
    get() = when (valueCase) {
        ValueCase.STRING_VALUE -> stringValue to "text"
        ValueCase.INT_VALUE -> intValue to "integer"
        ValueCase.FLOAT_VALUE -> floatValue to "float"
        ValueCase.BLOB_VALUE -> blobValue.toByteArray().toTypedArray() to "blob"
        ValueCase.VALUE_NOT_SET -> null to "null"
        else -> throw IllegalArgumentException()
    }

object MessageFactory {
    fun createTrackDatabasesCommand(): SqliteInspectorProtocol.Command =
        SqliteInspectorProtocol.Command.newBuilder().setTrackDatabases(
            SqliteInspectorProtocol.TrackDatabasesCommand.getDefaultInstance()
        ).build()

    fun createTrackDatabasesResponse(): SqliteInspectorProtocol.Response =
        SqliteInspectorProtocol.Response.newBuilder().setTrackDatabases(
            SqliteInspectorProtocol.TrackDatabasesResponse.getDefaultInstance()
        ).build()

    fun createGetSchemaCommand(databaseId: Int): SqliteInspectorProtocol.Command =
        SqliteInspectorProtocol.Command.newBuilder().setGetSchema(
            SqliteInspectorProtocol.GetSchemaCommand.newBuilder().setDatabaseId(databaseId).build()
        ).build()

    fun createQueryTableCommand(
        databaseId: Int,
        query: String
    ): SqliteInspectorProtocol.Command =
        SqliteInspectorProtocol.Command.newBuilder().setQuery(
            SqliteInspectorProtocol.QueryCommand.newBuilder()
                .setDatabaseId(databaseId)
                .setQuery(query)
                .build()
        ).build()
}
