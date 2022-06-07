/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build

import net.saff.checkmark.Checkmark.Companion.check
import org.junit.Assert
import org.junit.Test

class AndroidXRootPluginTest {
    @Test
    fun rootProjectConfigurationHasAndroidXTasks() = pluginTest {
        writeRootSettingsFile()
        writeRootBuildFile()
        Assert.assertTrue(privateJar.path, privateJar.exists())

        // --stacktrace gives more details on failure.
        runGradle("tasks", "--stacktrace").output.check {
            it.contains("listAndroidXProperties - Lists AndroidX-specific properties")
        }
    }
}