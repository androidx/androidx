/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.benchmark.Outputs
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Note - These tests are in benchmark-macro so we can access UiAutomator for shell access.
 * UiAutomator is minApi 18, lower than minApi of benchmark-common where Outputs resides
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
public class OutputsTest {
    @Test
    public fun dirUsableByAppAndShell_writeAppReadApp() {
        val dir = Outputs.dirUsableByAppAndShell
        val file = File.createTempFile("testFile", null, dir)
        try {
            file.writeText(file.name) // use name, as it's fairly unique
            assertEquals(file.name, file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    public fun dirUsableByAppAndShell_writeAppReadShell() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val dir = Outputs.dirUsableByAppAndShell
        val file = File.createTempFile("testFile", null, dir)
        try {
            file.writeText(file.name) // use name, as it's fairly unique
            assertEquals(
                file.name,
                device.executeShellCommand("cat ${file.absolutePath}")
            )
        } finally {
            file.delete()
        }
    }
}