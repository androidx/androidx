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

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.rules.TemporaryFolder

fun SQLiteDatabase.addTable(table: Table) = execSQL(table.toCreateString())

fun SQLiteDatabase.insertValues(table: Table, vararg values: String) {
    assertThat(values).isNotEmpty()
    assertThat(values).hasLength(table.columns.size)
    execSQL(values.joinToString(
        prefix = "INSERT INTO ${table.name} VALUES(",
        postfix = ");"
    ) { it })
}

fun Database.createInstance(temporaryFolder: TemporaryFolder): SQLiteDatabase {
    val path = temporaryFolder.newFile(this.name).absolutePath
    val context = ApplicationProvider.getApplicationContext() as android.content.Context
    val openHelper = object : SQLiteOpenHelper(context, path, null, 1) {
        override fun onCreate(db: SQLiteDatabase?) = Unit
        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
    }
    val db = openHelper.readableDatabase
    tables.forEach { t -> db.addTable(t) }
    return db
}

fun Table.toCreateString(): String {
    val primaryKeyColumns = columns.filter { it.isPrimaryKey }
    val primaryKeyPart =
        if (primaryKeyColumns.isEmpty()) ""
        else primaryKeyColumns
            .sortedBy { it.primaryKey }
            .joinToString(prefix = ",PRIMARY KEY(", postfix = ")") { it.name }

    return columns.joinToString(
        prefix = "CREATE TABLE $name (",
        postfix = "$primaryKeyPart );"
    ) {
        "${it.name} " +
                "${it.type} " +
                (if (it.isNotNull) "NOT NULL " else "") +
                (if (it.isUnique) "UNIQUE " else "")
    }
}
