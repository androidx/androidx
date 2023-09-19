/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.webkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


import android.os.Build;
import android.webkit.WebSettings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO(b/294183509): Add user-agent client hints HTTP header verification tests when unhide the
 * override user-agent metadata APIs.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class WebSettingsCompatUserAgentMetadataTest {
    private WebViewOnUiThread mWebViewOnUiThread;

    @Before
    public void setUp() throws Exception {
        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    @Test
    public void testSetUserAgentMetadataDefault() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        UserAgentMetadata defaultSetting = WebSettingsCompat.getUserAgentMetadata(
                settings);
        // Check brand version list.
        List<String> brands = new ArrayList<>();
        Assert.assertNotNull(defaultSetting.getBrandVersionList());
        for (UserAgentMetadata.BrandVersion bv : defaultSetting.getBrandVersionList()) {
            brands.add(bv.getBrand());
        }
        Assert.assertTrue("The default brand should contains Android WebView.",
                brands.contains("Android WebView"));
        // Check platform, bitness and wow64.
        assertEquals("The default platform is Android.", "Android",
                defaultSetting.getPlatform());
        assertEquals("The default bitness is 0.", UserAgentMetadata.BITNESS_DEFAULT,
                defaultSetting.getBitness());
        assertFalse("The default wow64 is false.", defaultSetting.isWow64());
    }

    @Test
    public void testSetUserAgentMetadataFullOverrides() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        // Overrides user-agent metadata.
        UserAgentMetadata overrideSetting = new UserAgentMetadata.Builder()
                .setBrandVersionList(Collections.singletonList(
                        new UserAgentMetadata.BrandVersion(
                                "myBrand", "1", "1.1.1.1")))
                .setFullVersion("1.1.1.1")
                .setPlatform("myPlatform").setPlatformVersion("2.2.2.2").setArchitecture("myArch")
                .setMobile(true).setModel("myModel").setBitness(32)
                .setWow64(false).setFormFactor("myFormFactor").build();

        WebSettingsCompat.setUserAgentMetadata(settings, overrideSetting);
        assertEquals(
                "After override set the user-agent metadata, it should be returned",
                overrideSetting, WebSettingsCompat.getUserAgentMetadata(
                        settings));
    }

    @Test
    public void testSetUserAgentMetadataPartialOverride() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        // Overrides without setting user-agent metadata platform and bitness.
        UserAgentMetadata overrideSetting = new UserAgentMetadata.Builder()
                .setBrandVersionList(Collections.singletonList(
                        new UserAgentMetadata.BrandVersion(
                                "myBrand", "1", "1.1.1.1")))
                .setFullVersion("1.1.1.1")
                .setPlatformVersion("2.2.2.2").setArchitecture("myArch").setMobile(true)
                .setModel("myModel").setWow64(false).setFormFactor("myFormFactor").build();

        WebSettingsCompat.setUserAgentMetadata(settings, overrideSetting);
        UserAgentMetadata actualSetting = WebSettingsCompat.getUserAgentMetadata(
                settings);
        assertEquals("Platform should reset to system default if no overrides.",
                "Android", actualSetting.getPlatform());
        assertEquals("Bitness should reset to system default if no overrides.",
                UserAgentMetadata.BITNESS_DEFAULT, actualSetting.getBitness());
        assertEquals("FormFactor should be overridden value.",
                "myFormFactor", WebSettingsCompat.getUserAgentMetadata(
                        settings).getFormFactor());
    }

    @Test
    public void testSetUserAgentMetadataBlankBrandVersion() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setBrandVersionList(Collections.singletonList(
                            new UserAgentMetadata.BrandVersion(
                                    "", "", ""))).build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Brand name, major version and full version should not "
                    + "be blank.", e.getMessage());
        }
    }

    @Test
    public void testSetUserAgentMetadataEmptyBrandVersionList() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setBrandVersionList(new ArrayList<>()).build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Brand version list should not be empty.", e.getMessage());
        }
    }

    @Test
    public void testSetUserAgentMetadataBlankFullVersion() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setFullVersion("  ").build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Full version should not be blank.", e.getMessage());
        }
    }

    @Test
    public void testSetUserAgentMetadataBlankPlatform() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setPlatform("  ").build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Platform should not be blank.", e.getMessage());
        }
    }
}
