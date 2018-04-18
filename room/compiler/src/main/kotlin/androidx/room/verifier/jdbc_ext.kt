/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.room.parser.SQLTypeAffinity
import androidx.room.verifier.ColumnInfo
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException

internal fun <T> ResultSet.collect(f: (ResultSet) -> T): List<T> {
    val result = arrayListOf<T>()
    try {
        while (next()) {
            result.add(f.invoke(this))
        }
    } finally {
        close()
    }
    return result
}

private fun <T> PreparedStatement.map(f: (Int, ResultSetMetaData) -> T): List<T> {
    val columnCount = try {
        metaData.columnCount
    } catch (ex: SQLException) {
        // ignore, no-result query
        0
    }
    // return is separate than data creation because we want to know who throws the exception
    return (1.rangeTo(columnCount)).map { f(it, metaData) }
}

internal fun PreparedStatement.columnNames(): List<String> {
    return map { index, data -> data.getColumnName(index) }
}

private fun PreparedStatement.tryGetAffinity(columnIndex: Int): SQLTypeAffinity {
    return try {
        SQLTypeAffinity.valueOf(metaData.getColumnTypeName(columnIndex).capitalize())
    } catch (ex: IllegalArgumentException) {
        SQLTypeAffinity.NULL
    }
}

internal fun PreparedStatement.columnInfo(): List<ColumnInfo> {
    //see: http://sqlite.1065341.n5.nabble.com/Column-order-in-resultset-td23127.html
    return map { index, data -> ColumnInfo(data.getColumnName(index), tryGetAffinity(index)) }
}
