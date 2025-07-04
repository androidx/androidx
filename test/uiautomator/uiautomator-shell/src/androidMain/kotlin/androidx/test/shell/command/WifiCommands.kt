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

/** Commands to turn on and off the wifi, and to check whether is enabled. */
public class WifiCommands internal constructor(private val shell: Shell) {

    /** Turns on the wifi. */
    public fun turnOn() {
        with(shell.command("svc wifi enable")) {
            stdOut.assertEmpty()
            stdErr.assertEmpty()
        }
    }

    /** Turns off the wifi. */
    public fun turnOff() {
        with(shell.command("svc wifi disable")) {
            stdOut.assertEmpty()
            stdErr.assertEmpty()
        }
    }

    /** Returns whether the wifi is enabled. */
    public fun isEnabled(): Boolean {
        with(shell.command("settings get global wifi_on")) {
            stdErr.assertEmpty()
            return when (stdOut.trim()) {
                "1",
                "2" -> true
                "0",
                "3" -> false
                else -> throw IllegalStateException("Unexpected output: `$stdOut`")
            }
        }
    }
}
