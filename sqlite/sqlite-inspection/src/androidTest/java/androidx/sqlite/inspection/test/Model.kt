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

data class Database(val name: String, val tables: List<Table>) {
    constructor(name: String, vararg tables: Table) : this(name, tables.toList())
}

data class Table(val name: String, val columns: List<Column>) {
    constructor(name: String, vararg columns: Column) : this(name, columns.toList())
}

data class Column(val name: String, val type: String)
