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

package androidx.test.backup

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/**
 * Keys accepted in [BackupDeviceActionArgs.payload] by the actions bundled with this library.
 *
 * Those actions are [androidx.test.backup.actions.PopulateStorageAction] and
 * [androidx.test.backup.actions.AssertStorageAction].
 *
 * Every value in the payload is a [String]; non-string data is encoded as described on the
 * individual key. Keys that an action does not recognize are ignored, so a single payload may be
 * reused for both the populate and the verify phase of a flow.
 *
 * Which keys apply depends on [STORAGE_TYPE]:
 * - [BackupActionValues.STORAGE_TYPE_PREFS] uses [PREF_NAME], [PREF_KEY], [VALUE], [VALUE_TYPE],
 *   [EXPECTED] and [EXPECT_NULL].
 * - [BackupActionValues.STORAGE_TYPE_DATABASE] uses [DB_NAME], [TABLE], [VALUES], [KEY_COL],
 *   [KEY_VAL], [EXPECTED_COL] and [EXPECTED_VAL].
 * - [BackupActionValues.STORAGE_TYPE_FILES] uses [PATH], [VALUE], [IS_BINARY] and [EXPECTED].
 *
 * [IS_DEVICE_PROTECTED] applies to every storage type.
 *
 * Custom [BackupDeviceAction] implementations are free to define their own keys; this object only
 * describes the vocabulary understood by the bundled actions. Result keys live in a separate
 * namespace, [BackupActionOutputKeys].
 */
public object BackupActionInputKeys {
    /**
     * Storage medium the action operates on.
     *
     * Must be one of [BackupActionValues.STORAGE_TYPE_PREFS],
     * [BackupActionValues.STORAGE_TYPE_DATABASE] or [BackupActionValues.STORAGE_TYPE_FILES],
     * matched case-insensitively. Defaults to [BackupActionValues.STORAGE_TYPE_PREFS] when absent.
     * Any other value fails the action.
     */
    public const val STORAGE_TYPE: String = "storage_type"

    /**
     * Whether to operate on device-protected (direct boot aware) storage.
     *
     * Parsed with [String.toBoolean], so `"true"` in any casing enables it and every other value,
     * including an absent key, leaves it disabled. When enabled, the action runs against
     * [android.content.Context.createDeviceProtectedStorageContext] instead of the default
     * credential-encrypted context.
     */
    public const val IS_DEVICE_PROTECTED: String = "is_device_protected"

    /**
     * Name of the [android.content.SharedPreferences] file.
     *
     * Defaults to [BackupActionValues.DEFAULT_PREF_NAME] when absent.
     */
    public const val PREF_NAME: String = "pref_name"

    /** Key within the [android.content.SharedPreferences] file. Required for preference storage. */
    public const val PREF_KEY: String = "pref_key"

    /**
     * Value to write, and the fallback expected value during verification when [EXPECTED] is
     * absent.
     *
     * For preference storage the string is converted according to [VALUE_TYPE]. For file storage it
     * is the file body, Base64-encoded when [IS_BINARY] is set.
     */
    public const val VALUE: String = "value"

    /**
     * Primitive type used to store and read back a preference [VALUE].
     *
     * Must be one of [BackupActionValues.VALUE_TYPE_INT], [BackupActionValues.VALUE_TYPE_LONG],
     * [BackupActionValues.VALUE_TYPE_FLOAT], [BackupActionValues.VALUE_TYPE_BOOLEAN] or
     * [BackupActionValues.VALUE_TYPE_STRING], matched case-insensitively. Defaults to
     * [BackupActionValues.VALUE_TYPE_STRING] when absent. Any other value fails the action.
     */
    public const val VALUE_TYPE: String = "value_type"

    /** File name of the SQLite database. Required for database storage. */
    public const val DB_NAME: String = "db_name"

    /** Table name inside the SQLite database. Required for database storage. */
    public const val TABLE: String = "table"

    /**
     * Column values to insert, as an `application/x-www-form-urlencoded` string.
     *
     * Pairs are separated by `&` and each pair is `name=value`, with both the name and the value
     * percent-encoded in UTF-8 exactly as [java.net.URLEncoder] produces. Encoding is what makes
     * column values containing `&`, `=`, `%` or `+` round-trip correctly; a literal `+` decodes
     * back to a space, so it must be encoded as `%2B`.
     *
     * For example, the columns `name = "a&b"` and `city = "Rio"` are expressed as
     * `"name=a%26b&city=Rio"`. Malformed pairs fail the action rather than being skipped.
     *
     * During verification this key replaces the [EXPECTED_COL] / [EXPECTED_VAL] pair. Every pair it
     * carries is checked against the row, so a restore that corrupts any column of the row fails,
     * and all mismatches are reported together. A malformed value fails the action rather than
     * falling back to [EXPECTED_COL] and [EXPECTED_VAL].
     */
    public const val VALUES: String = "values"

    /** Name of the column used to locate the row to verify. Required for database verification. */
    public const val KEY_COL: String = "key_col"

    /** Value of [KEY_COL] identifying the row to verify. Required for database verification. */
    public const val KEY_VAL: String = "key_val"

    /**
     * Location of the file to write or read.
     *
     * Absolute paths are used verbatim. Relative paths are resolved against
     * [android.content.Context.getFilesDir] of the context selected by [IS_DEVICE_PROTECTED].
     */
    public const val PATH: String = "path"

    /**
     * Whether [VALUE] and [EXPECTED] carry Base64-encoded binary data rather than text.
     *
     * Parsed with [String.toBoolean]. When enabled, the payload is decoded with
     * [android.util.Base64.DEFAULT] and the file is compared byte for byte.
     */
    public const val IS_BINARY: String = "is_binary"

    /**
     * Expected content during verification of preference or file storage.
     *
     * Falls back to [VALUE] when absent, which lets the same payload seed and verify a domain.
     * Database verification uses [EXPECTED_COL] and [EXPECTED_VAL] instead.
     */
    public const val EXPECTED: String = "expected"

    /** Name of the column whose content is verified. Ignored when [VALUES] is present. */
    public const val EXPECTED_COL: String = "expected_col"

    /**
     * Expected content of [EXPECTED_COL], as the string form of the column value.
     *
     * SQLite has no boolean type, so booleans are compared as `"true"` or `"false"`. Ignored when
     * [VALUES] is present.
     */
    public const val EXPECTED_VAL: String = "expected_val"

    /**
     * Whether verification should assert that the preference is absent instead of comparing it.
     *
     * Parsed with [String.toBoolean]. When enabled, [EXPECTED] and [VALUE] are not consulted.
     */
    public const val EXPECT_NULL: String = "expect_null"
}

/**
 * Keys produced in [BackupDeviceActionResult.payload] by the actions bundled with this library.
 *
 * [STATUS] is always present. [ERROR] is present if and only if [STATUS] is
 * [BackupActionValues.STATUS_FAILURE]. Actions may add their own keys alongside these; the host
 * orchestrator forwards the whole payload to the caller.
 *
 * Prefer [BackupDeviceActionResult.success] and [BackupDeviceActionResult.failure] over populating
 * these keys by hand, and [BackupDeviceActionResult.isSuccess] and
 * [BackupDeviceActionResult.errorMessage] over reading them by hand.
 *
 * Input keys live in a separate namespace, [BackupActionInputKeys].
 */
public object BackupActionOutputKeys {
    /**
     * Outcome of the action.
     *
     * Always one of [BackupActionValues.STATUS_SUCCESS] or [BackupActionValues.STATUS_FAILURE]. The
     * outcome is carried as a string rather than a boolean so that further categories, such as a
     * warning or a partial success, can be added without breaking the wire format.
     */
    public const val STATUS: String = "status"

    /**
     * Human-readable description of why the action failed.
     *
     * Present if and only if [STATUS] is [BackupActionValues.STATUS_FAILURE].
     */
    public const val ERROR: String = "error"
}

/**
 * Values that the keys in [BackupActionInputKeys] and [BackupActionOutputKeys] accept or produce.
 *
 * Keys whose value is a boolean are not listed here: they are parsed with [String.toBoolean], so
 * `"true"` in any casing means `true` and everything else, including an absent key, means `false`.
 */
public object BackupActionValues {
    /**
     * [BackupActionInputKeys.STORAGE_TYPE] value selecting [android.content.SharedPreferences]
     * storage. This is the default when the key is absent.
     */
    public const val STORAGE_TYPE_PREFS: String = "PREFS"

    /** [BackupActionInputKeys.STORAGE_TYPE] value selecting SQLite database storage. */
    public const val STORAGE_TYPE_DATABASE: String = "DATABASE"

    /** [BackupActionInputKeys.STORAGE_TYPE] value selecting raw file storage. */
    public const val STORAGE_TYPE_FILES: String = "FILES"

    /** [BackupActionInputKeys.VALUE_TYPE] value storing the value with `putInt`. */
    public const val VALUE_TYPE_INT: String = "INT"

    /** [BackupActionInputKeys.VALUE_TYPE] value storing the value with `putLong`. */
    public const val VALUE_TYPE_LONG: String = "LONG"

    /** [BackupActionInputKeys.VALUE_TYPE] value storing the value with `putFloat`. */
    public const val VALUE_TYPE_FLOAT: String = "FLOAT"

    /** [BackupActionInputKeys.VALUE_TYPE] value storing the value with `putBoolean`. */
    public const val VALUE_TYPE_BOOLEAN: String = "BOOLEAN"

    /**
     * [BackupActionInputKeys.VALUE_TYPE] value storing the value with `putString`. This is the
     * default when the key is absent.
     */
    public const val VALUE_TYPE_STRING: String = "STRING"

    /** [BackupActionOutputKeys.STATUS] value reported when the action completed its work. */
    public const val STATUS_SUCCESS: String = "success"

    /**
     * [BackupActionOutputKeys.STATUS] value reported when the action could not complete its work.
     */
    public const val STATUS_FAILURE: String = "failure"

    /** [BackupActionInputKeys.PREF_NAME] used when the key is absent. */
    public const val DEFAULT_PREF_NAME: String = "default_prefs"
}

/**
 * Denotes that the annotated [String] is an action input key — typically one of the constants in
 * [BackupActionInputKeys], or a key defined by a custom [BackupDeviceAction].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    BackupActionInputKeys.STORAGE_TYPE,
    BackupActionInputKeys.IS_DEVICE_PROTECTED,
    BackupActionInputKeys.PREF_NAME,
    BackupActionInputKeys.PREF_KEY,
    BackupActionInputKeys.VALUE,
    BackupActionInputKeys.VALUE_TYPE,
    BackupActionInputKeys.DB_NAME,
    BackupActionInputKeys.TABLE,
    BackupActionInputKeys.VALUES,
    BackupActionInputKeys.KEY_COL,
    BackupActionInputKeys.KEY_VAL,
    BackupActionInputKeys.PATH,
    BackupActionInputKeys.IS_BINARY,
    BackupActionInputKeys.EXPECTED,
    BackupActionInputKeys.EXPECTED_COL,
    BackupActionInputKeys.EXPECTED_VAL,
    BackupActionInputKeys.EXPECT_NULL,
    open = true,
)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
)
public annotation class BackupActionInputKey

/**
 * Denotes that the annotated [String] is an action output key — typically one of the constants in
 * [BackupActionOutputKeys], or a key produced by a custom [BackupDeviceAction].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Retention(AnnotationRetention.SOURCE)
@StringDef(BackupActionOutputKeys.STATUS, BackupActionOutputKeys.ERROR, open = true)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
)
public annotation class BackupActionOutputKey

/**
 * Denotes that the annotated [String] is an action status — typically one of the
 * [BackupActionOutputKeys.STATUS] values in [BackupActionValues], or a status published by a custom
 * [BackupDeviceAction].
 *
 * The set is open because [BackupDeviceActionResult.isSuccess] deliberately tolerates an
 * unrecognized status, so a custom action reporting, say, `"verified"` is not a lint error.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Retention(AnnotationRetention.SOURCE)
@StringDef(BackupActionValues.STATUS_SUCCESS, BackupActionValues.STATUS_FAILURE, open = true)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
)
public annotation class BackupActionStatus
