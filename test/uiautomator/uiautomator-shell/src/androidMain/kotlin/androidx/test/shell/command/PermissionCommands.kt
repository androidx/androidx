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

/** Commands to grant and revoke permission on a given app (defined via package name). */
public class PermissionCommands
internal constructor(private val shell: Shell, private val packageName: String) {

    /**
     * Grants the given permissions from the app.
     *
     * @param permissions one of more permissions from [android.Manifest.permission].
     */
    public fun grant(vararg permissions: String): Unit =
        permissions.forEach {
            with(shell.command("pm grant $packageName $it")) {
                stdOut.assertNoFailure()
                stdErr.assertNoFailure()
            }
        }

    /**
     * Revokes the given permissions from the app.
     *
     * @param permissions one of more permissions from [android.Manifest.permission].
     */
    public fun revoke(vararg permissions: String): Unit =
        permissions.forEach {
            with(shell.command("pm revoke $packageName $it")) {
                stdOut.assertNoFailure()
                stdErr.assertNoFailure()
            }
        }
}
