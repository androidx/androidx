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

package androidx.test.uiautomator.testapp

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.shell.Shell
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellTest {

    companion object {
        private const val APP_PACKAGE_NAME = "androidx.test.uiautomator.testapp"
    }

    @Test
    @SmallTest
    fun permissions() {
        Shell.permission(APP_PACKAGE_NAME).apply {
            grant(Manifest.permission.READ_EXTERNAL_STORAGE)
            revoke(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
