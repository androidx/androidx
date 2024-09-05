/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.browser.customtabs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link CustomTabsClient}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CustomTabsClientTest {
    private static final String TEST_CUSTOM_TABS_PROVIDER = "androidx.browser.test";
    private static final String TEST_NONEXISTENT_CUSTOM_TABS_PROVIDER =
            "androidx.browser.nonexistent";

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestCustomTabsServiceSetNetwork.class
    );

    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testCustomTabsServiceCategorySetNetwork() {
        // Specify the package name of androidTest suite that can handle the CustomTabsService
        // action, CustomTabsClient.getPackageName will return the package name of test suite
        // instead of the default browser package name if any installed in the target device, it
        // may or may not have the SET_NETWORK category. So we always specify with the package name
        // of androidTest to avoid the flakiness.
        List<String> packages = Collections.singletonList(TEST_CUSTOM_TABS_PROVIDER);
        String provider =
                CustomTabsClient.getPackageName(mContext, packages, true /* ignoreDefault */);
        assertNotNull(provider);
        assertTrue(CustomTabsClient.isSetNetworkSupported(mContext, provider));
    }

    @Test
    public void testCustomTabsServiceCategorySetNetwork_intentFilterCategoryDoesNotMatch() {
        // Disable the TestCustomTabsServiceSetNetwork service and enable
        // TestCustomTabsServiceSupportsTwas service intentionally, which doesn't offer
        // the CATEGORY_SET_NETWORK, check for the support should fail.
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSetNetwork.class);
        mEnableComponents.manuallyEnable(TestCustomTabsServiceSupportsTwas.class);

        List<String> packages = Collections.singletonList(TEST_CUSTOM_TABS_PROVIDER);
        String provider =
                CustomTabsClient.getPackageName(mContext, packages, true /* ignoreDefault */);
        assertNotNull(provider);
        assertFalse(CustomTabsClient.isSetNetworkSupported(mContext, provider));
    }

    @Test
    public void testCustomTabsServiceCategorySetNetwork_packageNameDoesNotMatch() {
        assertFalse(CustomTabsClient.isSetNetworkSupported(mContext,
                TEST_NONEXISTENT_CUSTOM_TABS_PROVIDER));
    }
}
