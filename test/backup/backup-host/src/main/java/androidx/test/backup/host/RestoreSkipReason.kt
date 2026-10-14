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
 * Returns why Google Play services skipped the restore of [packageName], as the entries of [logcat]
 * report it, or null if they report no skip of the app.
 *
 * Google Play services restores the backups of every mode but [BackupTransportMode.LOCAL]. It skips
 * an app whose backup is empty, that does not allow backups, or that it finds not eligible for
 * restore, such as a Google app, and logs why. The restore still succeeds.
 */
internal fun restoreSkipReason(logcat: String, packageName: String): String? {
    val eligibility =
        Regex("""Package ${Regex.escape(packageName)} is not eligible for D2D restore: (\w+)""")
            .find(logcat)
            ?.groupValues
            ?.get(1)
    return when {
        eligibility == INELIGIBLE_FIRST_PARTY ->
            "Google Play services skipped the app as not eligible for restore ($eligibility), " +
                "since it doesn't restore apps whose application ID starts with \"com.google.\" " +
                "or \"com.android.\". Test this app with BackupTransportMode.LOCAL, or give it " +
                "another application ID"
        eligibility != null ->
            "Google Play services skipped the app as not eligible for restore ($eligibility)"
        "Skipping restore of package: $packageName, as the corresponding backup file is empty" in
            logcat -> "Google Play services skipped the app, because its backup is empty"
        "Skipping restore of package: $packageName as allow_backup is set to false" in logcat ->
            "Google Play services skipped the app, because it doesn't allow backups " +
                "(android:allowBackup=\"false\")"
        else -> null
    }
}

/** The eligibility with which Google Play services skips the restore of a Google app. */
private const val INELIGIBLE_FIRST_PARTY = "INELIGIBLE_FIRST_PARTY"
