/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

/**
 * Defines target data coordinates in the device sandbox that are seeded and verified.
 *
 * Provides strongly-typed, compile-time safe data structures for zero-boilerplate testing. The
 * framework uses these to automatically drive the PopulateStorageAction and AssertStorageAction
 * helpers.
 *
 * Note: This sealed class is open to future additions. Consumers should use a non-exhaustive 'when'
 * pattern-matching approach (incorporating an explicit 'else' branch) to ensure binary
 * compatibility when new subclasses are introduced.
 */
public sealed class StorageDomain {

    internal companion object {
        fun isValidPreferenceType(value: Any): Boolean {
            return value is String ||
                value is Int ||
                value is Long ||
                value is Float ||
                value is Boolean
        }

        fun isValidDatabaseType(value: Any): Boolean {
            return value is String ||
                value is Int ||
                value is Long ||
                value is Float ||
                value is Double ||
                value is Boolean
        }
    }

    /**
     * A private subtype to prevent exhaustive when expressions on external clients, forcing
     * consumers to define an 'else' branch.
     */
    @Suppress("unused") private object Unknown : StorageDomain()

    /**
     * Targets on-device SharedPreferences XML files.
     *
     * @property prefName The name of the SharedPreferences XML file (excluding the .xml extension).
     * @property key The specific preference key to seed or verify.
     * @property value The value associated with the preference key, or `null` if asserting missing
     *   state. Supported types are: String, Int, Long, Float, Boolean, or null.
     */
    public class Preference
    @JvmOverloads
    constructor(
        public val prefName: String,
        public val key: String,
        public val value: Any? = null,
    ) : StorageDomain() {

        init {
            if (value != null) {
                require(isValidPreferenceType(value)) {
                    "Unsupported SharedPreferences value type: ${value.javaClass.name}. " +
                        "Supported types are: String, Int, Long, Float, Boolean."
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Preference) return false
            return (prefName == other.prefName) && (key == other.key) && (value == other.value)
        }

        override fun hashCode(): Int {
            var result = prefName.hashCode()
            result = 31 * result + key.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Preference(prefName='$prefName', key='$key', value=$value)"
        }
    }

    /**
     * Targets structured SQLite/Room database records.
     *
     * @property dbName The name of the SQLite database file on the device.
     * @property table The target table name to insert into or query from.
     * @property primaryKeyCol The column name of the primary key used to identify the target
     *   record.
     * @property primaryKeyVal The value of the primary key used to identify the target record.
     *   Supported types are: String, Int, Long, Float, Double, Boolean.
     * @property columnValues A Map of column names to their values to populate or verify. Supported
     *   types are: String, Int, Long, Float, Double, Boolean, or null.
     */
    public class Database(
        public val dbName: String,
        public val table: String,
        public val primaryKeyCol: String,
        public val primaryKeyVal: Any,
        public val columnValues: Map<String, Any?>,
    ) : StorageDomain() {

        init {
            require(isValidDatabaseType(primaryKeyVal)) {
                "Unsupported Database primaryKeyVal type: ${primaryKeyVal.javaClass.name}. " +
                    "Supported types are: String, Int, Long, Float, Double, Boolean."
            }
            columnValues.forEach { (col, valItem) ->
                if (valItem != null) {
                    require(isValidDatabaseType(valItem)) {
                        "Unsupported Database column value type for column '$col': " +
                            "${valItem.javaClass.name}. Supported types are: String, Int, " +
                            "Long, Float, Double, Boolean."
                    }
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Database) return false
            return dbName == other.dbName &&
                table == other.table &&
                primaryKeyCol == other.primaryKeyCol &&
                primaryKeyVal == other.primaryKeyVal &&
                columnValues == other.columnValues
        }

        override fun hashCode(): Int {
            var result = dbName.hashCode()
            result = 31 * result + table.hashCode()
            result = 31 * result + primaryKeyCol.hashCode()
            result = 31 * result + primaryKeyVal.hashCode()
            result = 31 * result + columnValues.hashCode()
            return result
        }

        override fun toString(): String {
            return "Database(dbName='$dbName', table='$table', primaryKeyCol='$primaryKeyCol', " +
                "primaryKeyVal=$primaryKeyVal, columnValues=$columnValues)"
        }
    }

    /**
     * Targets local raw text files stored within internal app storage.
     *
     * @property path The relative path of the file inside the app sandbox (e.g.
     *   "files/my_data.txt").
     * @property content The string content of the file.
     */
    public class TextFile(public val path: String, public val content: String) : StorageDomain() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TextFile) return false
            return path == other.path && content == other.content
        }

        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + content.hashCode()
            return result
        }

        override fun toString(): String {
            return "TextFile(path='$path', content='$content')"
        }
    }

    /**
     * Targets local raw binary files stored within internal app storage.
     *
     * @property path The relative path of the file inside the app sandbox (e.g.
     *   "files/my_image.png").
     * @property content The raw byte array content of the file.
     */
    public class BinaryFile(public val path: String, content: ByteArray) : StorageDomain() {
        public val content: ByteArray = content.clone()
            get() = field.clone()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BinaryFile) return false
            return path == other.path && content.contentEquals(other.content)
        }

        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "BinaryFile(path='$path', size=${content.size})"
        }
    }
}
