/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.shell.command

import android.content.Intent
import androidx.test.shell.Shell

/** Commands to be used on applications. */
public class ApplicationCommands internal constructor(private val shell: Shell) {

    /** Clears the application data. */
    public fun clearAppData(packageName: String): Unit =
        with(shell.command("pm clear $packageName")) {
            stdOut.assertNoFailure()
            stdErr.assertNoFailure()
        }

    /**
     * Starts the application. The first activity with category main and action launch is selected.
     */
    public fun startApp(packageName: String): Unit =
        with(shell.command("monkey -p $packageName 1")) {
            stdOut.assertNoFailure()
            stdErr.assertNoFailure()
        }

    /** Stops the application. */
    public fun stopApp(packageName: String): Unit =
        with(shell.command("am force-stop $packageName")) {
            stdOut.assertNoFailure()
            stdErr.assertNoFailure()
        }

    /**
     * Starts an activity intent via `am start` shell command, using the intent uri as target.
     *
     * Note that parcelables are not serialized in the URI and so will be ignored as part of the
     * intent when calling this method.
     *
     * @param intent activity intent to start.
     * @param additionalArguments additional arguments to pass to `am start` command, space
     *   separated, as these are passed to adb shell directly.
     */
    @JvmOverloads
    public fun amStartActivityIntent(intent: Intent, additionalArguments: String = "") {
        val newIntent = Intent(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        val uri = newIntent.toUri(Intent.URI_INTENT_SCHEME)
        with(shell.command("am start $additionalArguments -W \"$uri\"")) {
            stdOut.assertNoFailure()
            stdErr.assertNoFailure()
        }
    }
}
