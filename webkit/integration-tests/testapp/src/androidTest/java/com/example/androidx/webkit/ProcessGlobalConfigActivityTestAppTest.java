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

package com.example.androidx.webkit;

import static org.junit.Assert.assertTrue;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.webkit.WebViewFeature;
import org.junit.Ignore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Integration test for {@link ProcessGlobalConfigActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProcessGlobalConfigActivityTestAppTest {
    @Rule
    public ActivityScenarioRule<ProcessGlobalConfigActivity> mRule =
            new ActivityScenarioRule<>(ProcessGlobalConfigActivity.class);

    @Test
    @Ignore("b/280671406")
    public void testSetDataDirectorySuffix() throws Throwable {
        WebkitTestHelpers.assumeStartupFeature(
                WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
                ApplicationProvider.getApplicationContext());
        final String dataDirPrefix = "app_webview_";
        final String dataDirSuffix = "data_directory_suffix_activity_suffix";
        File file = new File(ContextCompat.getDataDir(ApplicationProvider.getApplicationContext()),
                dataDirPrefix + dataDirSuffix);

        // Ensures WebView directory created during earlier tests runs are purged.
        deleteDirectory(file);
        // This should ideally be an assumption, but we want a stronger signal to ensure the test
        // does not silently stop working.
        if (file.exists()) {
            throw new RuntimeException("WebView Directory exists before test despite attempt to "
                    + "delete it");
        }
        WebkitTestHelpers.clickMenuListItemWithString(
                R.string.data_directory_suffix_activity_title);
        // We need to wait for the WebView to finish loading on a different process.
        Thread.sleep(5000);

        assertTrue(file.exists());
    }

    @Test
    public void testSetDirectoryBasePaths() throws Throwable {
        WebkitTestHelpers.assumeStartupFeature(
                WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS,
                ApplicationProvider.getApplicationContext());
        final String dataDirPrefix = "webview_";
        final String dataDirSuffix = "directory_base_path_activity_suffix";
        File dataBasePath =
                new File(ContextCompat.getDataDir(ApplicationProvider.getApplicationContext()),
                        "data_dir");
        File cacheBasePath =
                new File(ContextCompat.getDataDir(ApplicationProvider.getApplicationContext()),
                "cache_dir");
        File dataSuffixedPath = new File(dataBasePath, dataDirPrefix + dataDirSuffix);

        // Ensures WebView directory created during earlier tests runs are purged.
        deleteDirectory(dataBasePath);
        deleteDirectory(cacheBasePath);
        // This should ideally be an assumption, but we want a stronger signal to ensure the test
        // does not silently stop working.
        if (dataBasePath.exists() || cacheBasePath.exists() || dataSuffixedPath.exists()) {
            throw new RuntimeException("WebView Directory exists before test despite attempt to "
                    + "delete it");
        }
        WebkitTestHelpers.clickMenuListItemWithString(
                R.string.directory_base_path_activity_title);
        // We need to wait for the WebView to finish loading on a different process.
        Thread.sleep(5000);

        assertTrue(dataBasePath.exists());
        assertTrue(cacheBasePath.exists());
        assertTrue(dataSuffixedPath.exists());
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
