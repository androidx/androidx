/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.webkit

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.webkit.WebViewFeature
import java.io.File
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration test for {@link ProcessGlobalConfigActivity}. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProcessGlobalConfigActivityTestAppTest {

    @get:Rule val rule = ActivityScenarioRule(ProcessGlobalConfigActivity::class.java)

    @Test
    fun testSetDataDirectorySuffix() {
        WebkitTestHelpers.assumeStartupFeature(
            WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
            ApplicationProvider.getApplicationContext(),
        )

        val file =
            File(
                ContextCompat.getDataDir(ApplicationProvider.getApplicationContext()),
                "app_webview_data_directory_suffix_activity_suffix",
            )

        // delete existing files from previous tests
        file.deleteRecursively()

        // This should ideally be an assumption, but we want a stronger signal to ensure the test
        // does not silently stop working.
        Assert.assertFalse(
            "WebView directory exists before test despite attempt to delete it",
            file.exists(),
        )

        WebkitTestHelpers.clickMenuListItemWithString(R.string.data_directory_suffix_activity_title)

        // We need to wait for the WebView to finish loading on a different process.
        @SuppressLint("BanThreadSleep") Thread.sleep(5000)

        Assert.assertTrue(file.exists())
    }

    @Test
    fun testSetDirectoryBasePaths() {
        WebkitTestHelpers.assumeStartupFeature(
            WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS,
            ApplicationProvider.getApplicationContext(),
        )

        val dataBasePath =
            File(ContextCompat.getDataDir(ApplicationProvider.getApplicationContext()), "data_dir")
        val cacheBasePath =
            File(ContextCompat.getDataDir(ApplicationProvider.getApplicationContext()), "cache_dir")
        val dataSuffixedPath = File(dataBasePath, "webview_directory_base_path_activity_suffix")

        // delete existing files from previous tests
        dataBasePath.deleteRecursively()
        cacheBasePath.deleteRecursively()

        // This should ideally be an assumption, but we want a stronger signal to ensure the test
        // does not silently stop working.
        Assert.assertFalse(
            "WebView Directory exists before test despite attempt to delete it",
            dataBasePath.exists() || cacheBasePath.exists() || dataSuffixedPath.exists(),
        )

        WebkitTestHelpers.clickMenuListItemWithString(R.string.directory_base_path_activity_title)

        // We need to wait for the WebView to finish loading on a different process.
        @SuppressLint("BanThreadSleep") Thread.sleep(5000)

        Assert.assertTrue(dataBasePath.exists())
        Assert.assertTrue(cacheBasePath.exists())
        Assert.assertTrue(dataSuffixedPath.exists())
    }
}
