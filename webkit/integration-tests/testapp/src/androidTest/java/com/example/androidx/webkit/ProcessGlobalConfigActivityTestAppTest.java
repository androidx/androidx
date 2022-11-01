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

import org.junit.Before;
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
    public ActivityScenarioRule<MainActivity> mRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        WebkitTestHelpers.assumeStartupFeature(
                WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testSetDataDirectorySuffix() throws Throwable {
        final String dataDirPrefix = "app_webview_";
        final String dataDirSuffix = "per_process_webview_data_0";

        WebkitTestHelpers.clickMenuListItemWithString(
                R.string.process_global_config_activity_title);
        Thread.sleep(1000);

        File file = new File(ContextCompat.getDataDir(ApplicationProvider.getApplicationContext()),
                dataDirPrefix + dataDirSuffix);

        assertTrue(file.exists());
    }
}
