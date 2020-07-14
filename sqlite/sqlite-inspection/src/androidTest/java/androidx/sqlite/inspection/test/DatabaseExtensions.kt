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
import java.io.File

fun SQLiteDatabase.addTable(table: Table) = execSQL(table.toCreateString())

val SQLiteDatabase.displayName: String
    get() = if (path != ":memory:") path else
        ":memory: {hashcode=0x${String.format("%x", this.hashCode())}}"

val SQLiteDatabase.absolutePath: String
    get() = File(path).absolutePath

fun SQLiteDatabase.insertValues(table: Table, vararg values: String) {
    assertThat(values).isNotEmpty()
    assertThat(values).hasLength(table.columns.size)
    execSQL(values.joinToString(
        prefix = "INSERT INTO ${table.name} VALUES(",
        postfix = ");"
    ) { it })
}

fun Database.createInstance(
    temporaryFolder: TemporaryFolder,
    writeAheadLoggingEnabled: Boolean? = null
): SQLiteDatabase {
    val path = if (name == null) null else
        File(temporaryFolder.root, name)
            .also { it.createNewFile() } // can handle an existing file
            .absolutePath

    val context = ApplicationProvider.getApplicationContext() as android.content.Context
    val openHelper = object : SQLiteOpenHelper(context, path, null, 1) {
        override fun onCreate(db: SQLiteDatabase?) = Unit
        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
    }

    writeAheadLoggingEnabled?.let { openHelper.setWriteAheadLoggingEnabled(it) }
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
        prefix = "CREATE ${if (isView) "VIEW" else "TABLE"} $name (",
        postfix = "$primaryKeyPart )${if (isView) " AS $viewQuery" else ""};"
    ) {
        it.name +
                "${if (isView) "" else " ${it.type}"} " +
                (if (it.isNotNull) "NOT NULL " else "") +
                (if (it.isUnique) "UNIQUE " else "")
    }
}
