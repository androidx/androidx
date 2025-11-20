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

import androidx.test.shell.Shell

/** Commands to be used on applications. */
public class ApplicationCommands
internal constructor(private val shell: Shell, private val packageName: String) {

    /** Clears the application data. */
    public fun clearAppData(): Unit =
        with(shell.command("pm clear $packageName")) {
            stdOut.assertNoFailure()
            stdErr.assertNoFailure()
        }

    /**
     * Starts the application. The first activity with category main and action launch is selected.
     */
    public fun startApp(): Unit =
        with(shell.command("monkey -p $packageName 1")) {
            stdOut.assertNoFailure()
            stdErr.assertNoFailure()
        }

    /** Stops the application. */
    public fun stopApp(): Unit =
        with(shell.command("am force-stop $packageName")) {
            stdOut.assertNoFailure()
            stdErr.assertNoFailure()
        }
}
