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

/** Commands about the current focused display. */
public class ScreenCommands internal constructor(private val shell: Shell) {

    /**
     * Returns the full name of the resumed activity on the screen in the format
     * <package_name>/<activity_name> that the user last interacted with.
     */
    public fun resumedActivityName(): String =
        // Example output string for the command:
        // ResumedActivity: ActivityRecord{7952128 u0
        // com.google.android.apps.messaging/.ui.ConversationListActivity t363}
        with(shell.command("dumpsys activity activities | grep ResumedActivity")) {
            stdErr.assertEmpty()
            return regex.find(stdOut)?.groupValues?.get(1)
                ?: throw IllegalStateException(
                    "Could not parse resumed activity from output:\n$stdOut"
                )
        }

    /** Returns whether the keyboard is currently visible. */
    public fun isKeyboardVisible(): Boolean {
        with(shell.command("dumpsys input_method | grep InputShown")) {
            stdErr.assertEmpty()
            return stdOut
                .lines()
                .first { it.contains("InputShown=") }
                .substringAfter("InputShown=")
                .trim()
                .toBooleanStrict()
        }
    }

    // Regex breakdown:
    // ActivityRecord\{         # literal “ActivityRecord{”
    //   [^}]*?                 # non-greedy skip up to the first space before the component
    //   \s                     # that space
    // (?<activity>            # start named group “activity”
    //   [^\s}]+/[^\s}]+        #   one-or-more non-space/non-} chars, slash, same again
    // )
    private val regex = Regex("""ActivityRecord\{[^}]*?\s(?<activity>[^\s}]+/[^\s}]+)""")
}
