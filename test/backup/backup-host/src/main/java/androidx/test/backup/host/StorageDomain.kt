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

import java.util.Collections
import java.util.Objects

/**
 * Defines target data coordinates in the device sandbox that are seeded and verified.
 *
 * Provides strongly-typed, compile-time safe data structures for zero-boilerplate testing. The
 * framework uses these to automatically drive the bundled storage actions, or the actions of a
 * [CustomData].
 *
 * Note: This sealed class is open to future additions. Consumers should use a non-exhaustive 'when'
 * pattern-matching approach (incorporating an explicit 'else' branch) to ensure binary
 * compatibility when new subclasses are introduced.
 */
public sealed class StorageDomain {

    internal companion object {
        /** Number of characters of a value or of file content that a string form shows. */
        private const val MAX_SHOWN_LENGTH = 64

        fun isValidPreferenceType(value: Any): Boolean {
            return value is String ||
                value is Int ||
                value is Long ||
                value is Float ||
                value is Boolean ||
                (value is Set<*> && value.all { it is String })
        }

        fun isValidDatabaseType(value: Any): Boolean {
            return value is String ||
                value is Int ||
                value is Long ||
                value is Float ||
                value is Double ||
                value is Boolean
        }

        /** Returns what a string form adds for a domain in device-protected storage. */
        private fun deviceProtectedSuffix(isDeviceProtectedStorage: Boolean): String =
            if (isDeviceProtectedStorage) ", isDeviceProtectedStorage=true" else ""

        /**
         * Returns the string form of [value], or, if it is longer than [MAX_SHOWN_LENGTH]
         * characters, its start followed by `...` and its length.
         */
        private fun shownValue(value: Any?): String {
            val text = value.toString()
            return if (text.length <= MAX_SHOWN_LENGTH) text
            else "${shownStart(text)}... (${text.length} chars)"
        }

        /**
         * Returns the first [MAX_SHOWN_LENGTH] characters of [text], without the first half of a
         * surrogate pair at the end, or [text] itself if it isn't longer than that.
         */
        private fun shownStart(text: String): String {
            if (text.length <= MAX_SHOWN_LENGTH) return text
            val end =
                if (text[MAX_SHOWN_LENGTH - 1].isHighSurrogate()) MAX_SHOWN_LENGTH - 1
                else MAX_SHOWN_LENGTH
            return text.substring(0, end)
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
     *   state. Supported types are: String, Int, Long, Float, Boolean, a Set of Strings, or null. A
     *   set is stored with `putStringSet` and compared regardless of the order of its elements; the
     *   preference keeps a copy of it. A `null` value is seeded by removing the key and verified by
     *   checking that the key is absent. Clearing app data also removes the key, so a `null` value
     *   cannot detect a restore that loses data.
     * @property isDeviceProtectedStorage Whether the SharedPreferences file is in device-protected
     *   storage, which is available before the user first unlocks the device, rather than in the
     *   default storage of the app. That is credential-encrypted storage, unless the app sets
     *   `android:defaultToDeviceProtectedStorage`.
     */
    public class Preference
    @JvmOverloads
    constructor(
        public val prefName: String,
        public val key: String,
        value: Any? = null,
        public val isDeviceProtectedStorage: Boolean = false,
    ) : StorageDomain() {

        public val value: Any? =
            if (value is Set<*>) Collections.unmodifiableSet(LinkedHashSet(value)) else value

        init {
            val value = this.value
            if (value != null) {
                require(isValidPreferenceType(value)) {
                    "Unsupported SharedPreferences value type: ${value.javaClass.name}. " +
                        "Supported types are: String, Int, Long, Float, Boolean, Set<String>."
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Preference) return false
            return prefName == other.prefName &&
                key == other.key &&
                value == other.value &&
                isDeviceProtectedStorage == other.isDeviceProtectedStorage
        }

        override fun hashCode(): Int {
            var result = prefName.hashCode()
            result = 31 * result + key.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            result = 31 * result + isDeviceProtectedStorage.hashCode()
            return result
        }

        override fun toString(): String {
            return "Preference(prefName='$prefName', key='$key', value=${shownValue(value)}" +
                "${deviceProtectedSuffix(isDeviceProtectedStorage)})"
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
     * @property columnValues A non-empty Map of column names to their values to populate or verify.
     *   Supported types are: String, Int, Long, Float, Double, Boolean, or null, which stands for
     *   SQL `NULL`. It may include [primaryKeyCol], matched case-insensitively like SQLite column
     *   names, but only with a value whose string form equals that of [primaryKeyVal]; otherwise
     *   construction fails with an [IllegalArgumentException]. When it is omitted, [primaryKeyVal]
     *   is written to that column. The domain keeps a copy of it.
     * @property isDeviceProtectedStorage Whether the database is in device-protected storage, which
     *   is available before the user first unlocks the device, rather than in the default storage
     *   of the app. That is credential-encrypted storage, unless the app sets
     *   `android:defaultToDeviceProtectedStorage`.
     */
    public class Database
    @JvmOverloads
    constructor(
        public val dbName: String,
        public val table: String,
        public val primaryKeyCol: String,
        public val primaryKeyVal: Any,
        columnValues: Map<String, Any?>,
        public val isDeviceProtectedStorage: Boolean = false,
    ) : StorageDomain() {

        public val columnValues: Map<String, Any?> =
            Collections.unmodifiableMap(LinkedHashMap(columnValues))

        init {
            require(columnValues.isNotEmpty()) {
                "Database columnValues must contain at least one column to populate and verify."
            }
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
                // Both values travel to the device as strings, so they are compared as strings.
                if (col.equals(primaryKeyCol, ignoreCase = true)) {
                    require(valItem?.toString() == primaryKeyVal.toString()) {
                        "Column '$col' is the primary key column, so its value '$valItem' must " +
                            "match primaryKeyVal '$primaryKeyVal'."
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
                columnValues == other.columnValues &&
                isDeviceProtectedStorage == other.isDeviceProtectedStorage
        }

        override fun hashCode(): Int {
            var result = dbName.hashCode()
            result = 31 * result + table.hashCode()
            result = 31 * result + primaryKeyCol.hashCode()
            result = 31 * result + primaryKeyVal.hashCode()
            result = 31 * result + columnValues.hashCode()
            result = 31 * result + isDeviceProtectedStorage.hashCode()
            return result
        }

        override fun toString(): String {
            return "Database(dbName='$dbName', table='$table', primaryKeyCol='$primaryKeyCol', " +
                "primaryKeyVal=${shownValue(primaryKeyVal)}, " +
                "columnValues=${columnValues.mapValues { shownValue(it.value) }}" +
                "${deviceProtectedSuffix(isDeviceProtectedStorage)})"
        }
    }

    /**
     * Targets local raw text files stored within internal app storage.
     *
     * @property path The path of the file, relative to the app's files directory
     *   (`Context.getFilesDir()`), such as `"my_data.txt"`. An absolute path is used as is.
     * @property content The string content of the file.
     * @property isDeviceProtectedStorage Whether the file is in device-protected storage, which is
     *   available before the user first unlocks the device, rather than in the default storage of
     *   the app. That is credential-encrypted storage, unless the app sets
     *   `android:defaultToDeviceProtectedStorage`. A relative [path] then starts at the files
     *   directory of device-protected storage.
     */
    public class TextFile
    @JvmOverloads
    constructor(
        public val path: String,
        public val content: String,
        public val isDeviceProtectedStorage: Boolean = false,
    ) : StorageDomain() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TextFile) return false
            return path == other.path &&
                content == other.content &&
                isDeviceProtectedStorage == other.isDeviceProtectedStorage
        }

        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + content.hashCode()
            result = 31 * result + isDeviceProtectedStorage.hashCode()
            return result
        }

        override fun toString(): String {
            val shownContent =
                if (content.length <= MAX_SHOWN_LENGTH) {
                    "'$content'"
                } else {
                    "'${shownStart(content)}...' (${content.length} chars)"
                }
            return "TextFile(path='$path', content=$shownContent" +
                "${deviceProtectedSuffix(isDeviceProtectedStorage)})"
        }
    }

    /**
     * Targets local raw binary files stored within internal app storage.
     *
     * @property path The path of the file, relative to the app's files directory
     *   (`Context.getFilesDir()`), such as `"my_image.png"`. An absolute path is used as is.
     * @property content The raw byte array content of the file.
     * @property isDeviceProtectedStorage Whether the file is in device-protected storage, which is
     *   available before the user first unlocks the device, rather than in the default storage of
     *   the app. That is credential-encrypted storage, unless the app sets
     *   `android:defaultToDeviceProtectedStorage`. A relative [path] then starts at the files
     *   directory of device-protected storage.
     */
    public class BinaryFile
    @JvmOverloads
    constructor(
        public val path: String,
        content: ByteArray,
        public val isDeviceProtectedStorage: Boolean = false,
    ) : StorageDomain() {
        public val content: ByteArray = content.clone()
            get() = field.clone()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BinaryFile) return false
            return path == other.path &&
                content.contentEquals(other.content) &&
                isDeviceProtectedStorage == other.isDeviceProtectedStorage
        }

        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + content.contentHashCode()
            result = 31 * result + isDeviceProtectedStorage.hashCode()
            return result
        }

        override fun toString(): String {
            return "BinaryFile(path='$path', size=${content.size}" +
                "${deviceProtectedSuffix(isDeviceProtectedStorage)})"
        }
    }

    /**
     * Targets data that device actions of your own seed and verify, such as a restore credential
     * that only the app's code can create and read.
     *
     * The flow runs [populateAction] with [args] before the backup. After the restore, it runs
     * [verifyAction] with [args] and the data that [populateAction] returned, except its `status`
     * and `error`. That data replaces the values of [args] with the same keys. The flow fails if
     * either action fails, or if [populateAction] returns a key that the runner reserves.
     *
     * @property populateAction class name of the [androidx.test.backup.BackupDeviceAction] that
     *   seeds the data
     * @property verifyAction class name of the [androidx.test.backup.BackupDeviceAction] that
     *   verifies the data after the restore
     * @property args arguments for both actions, as [BackupRestoreController.runOnDevice] takes
     *   them. The keys `actionClass`, `argsFile`, `debug` and `redirect_dir` are reserved for the
     *   runner; with one of them, construction fails with an [IllegalArgumentException]. The domain
     *   keeps a copy of it.
     */
    public class CustomData
    @JvmOverloads
    constructor(
        public val populateAction: String,
        public val verifyAction: String,
        args: Map<String, String> = emptyMap(),
    ) : StorageDomain() {

        public val args: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(args))

        init {
            val runnerKeys = args.keys.filter { it in BackupActionWireProtocol.RUNNER_KEYS }
            require(runnerKeys.isEmpty()) {
                "CustomData args must not use the keys $runnerKeys, which the runner reads itself."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CustomData) return false
            return populateAction == other.populateAction &&
                verifyAction == other.verifyAction &&
                args == other.args
        }

        override fun hashCode(): Int {
            var result = populateAction.hashCode()
            result = 31 * result + verifyAction.hashCode()
            result = 31 * result + args.hashCode()
            return result
        }

        override fun toString(): String {
            return "CustomData(populateAction='$populateAction', verifyAction='$verifyAction', " +
                "args=${args.mapValues { shownValue(it.value) }})"
        }
    }

    /**
     * Targets data that the restore doesn't bring back, such as data that the app's backup rules
     * exclude.
     *
     * The flow seeds [domain] before the backup, and verifies after the restore that the preference
     * key, the database row with the primary key value, or the file is absent. A flow needs a
     * restored domain as well, to verify that the restore happened: a flow with only excluded data
     * fails with an [IllegalArgumentException].
     *
     * @property domain data to seed and then expect absent: a [Preference] with a value, a
     *   [Database], a [TextFile] or a [BinaryFile]. Any other domain fails construction with an
     *   [IllegalArgumentException].
     */
    public class ExcludedData(public val domain: StorageDomain) : StorageDomain() {

        init {
            require(
                (domain is Preference && domain.value != null) ||
                    domain is Database ||
                    domain is TextFile ||
                    domain is BinaryFile
            ) {
                "ExcludedData needs a Preference with a value, a Database, a TextFile or a " +
                    "BinaryFile, not $domain."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExcludedData) return false
            return domain == other.domain
        }

        override fun hashCode(): Int = Objects.hash(domain)

        override fun toString(): String = "ExcludedData($domain)"
    }
}
