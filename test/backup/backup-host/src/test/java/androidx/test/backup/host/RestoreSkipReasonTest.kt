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

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class RestoreSkipReasonTest {

    @Test
    fun reportsAGoogleAppAndHowToTestIt() {
        val logcat = logcat(notEligible(PACKAGE, "INELIGIBLE_FIRST_PARTY"))

        assertEquals(
            "Google Play services skipped the app as not eligible for restore " +
                "(INELIGIBLE_FIRST_PARTY), since it doesn't restore apps whose application ID " +
                "starts with \"com.google.\" or \"com.android.\". Test this app with " +
                "BackupTransportMode.LOCAL, or give it another application ID",
            restoreSkipReason(logcat, PACKAGE),
        )
    }

    @Test
    fun reportsAnyOtherEligibilityByItsName() {
        val logcat = logcat(notEligible(PACKAGE, "INELIGIBLE_DENYLISTED"))

        assertEquals(
            "Google Play services skipped the app as not eligible for restore " +
                "(INELIGIBLE_DENYLISTED)",
            restoreSkipReason(logcat, PACKAGE),
        )
    }

    @Test
    fun reportsAnEmptyBackup() {
        val logcat = logcat(emptyBackup(PACKAGE))

        assertEquals(
            "Google Play services skipped the app, because its backup is empty",
            restoreSkipReason(logcat, PACKAGE),
        )
    }

    @Test
    fun reportsAnAppThatDoesNotAllowBackups() {
        val logcat =
            logcat(
                "[CacheRestoreHelper] Skipping restore of package: $PACKAGE as allow_backup is " +
                    "set to false."
            )

        assertEquals(
            "Google Play services skipped the app, because it doesn't allow backups " +
                "(android:allowBackup=\"false\")",
            restoreSkipReason(logcat, PACKAGE),
        )
    }

    /**
     * A package whose name starts with the one of the app, or matches it as a regex, is another.
     */
    @Test
    fun ignoresTheSkipOfAnotherApp() {
        val logcat =
            logcat(
                notEligible("$PACKAGE.other", "INELIGIBLE_FIRST_PARTY"),
                notEligible("com.examplexapp", "INELIGIBLE_FIRST_PARTY"),
                emptyBackup("$PACKAGE.other"),
            )

        assertNull(restoreSkipReason(logcat, PACKAGE))
    }

    @Test
    fun returnsNullForTheLogOfARestoredApp() {
        val logcat =
            logcat(
                "[EligibilityFilter] Package eligibility filter is enabled for D2D restore.",
                "restoreFinished packageName=$PACKAGE",
            )

        assertNull(restoreSkipReason(logcat, PACKAGE))
    }

    private companion object {
        const val PACKAGE = "com.example.app"

        /** Returns [messages] as logcat entries of Google Play services, one per line. */
        fun logcat(vararg messages: String): String =
            messages.joinToString("") { "10-14 07:21:50.666 18335 18694 I Backup  : $it\n" }

        fun notEligible(packageName: String, eligibility: String): String =
            "[CacheRestoreHelper] Package $packageName is not eligible for D2D restore: " +
                eligibility

        fun emptyBackup(packageName: String): String =
            "[CacheRestoreHelper] Skipping restore of package: $packageName, as the " +
                "corresponding backup file is empty."
    }
}
