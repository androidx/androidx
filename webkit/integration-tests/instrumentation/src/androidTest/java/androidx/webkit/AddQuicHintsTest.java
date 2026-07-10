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

package androidx.webkit;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

/**
 * Test for {@link Profile#addQuicHints(Set)}.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class AddQuicHintsTest {
    private static final String URL1 = "https://www.example.com";
    private static final String URL2 = "https://www.example.org";

    private Profile mDefaultProfile;

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        WebkitUtils.checkFeature(WebViewFeature.ADD_QUIC_HINTS_V1);

        mDefaultProfile = WebkitUtils.onMainThreadSync(
                () -> ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME));
    }

    @Test
    public void doesNotCrash() {
        // Tests that the call doesn't crash WebView.
        WebkitUtils.onMainThreadSync(() -> {
            mDefaultProfile.addQuicHints(Set.of(URL1, URL2));
        });
    }

    @Test
    public void doesNotCrash_url() {
        // Tests that we allow calling with a URL, not an origin.
        WebkitUtils.onMainThreadSync(() -> {
            mDefaultProfile.addQuicHints(Set.of(URL1, "https://www.example.org/1.html"));
        });
    }

    @Test
    public void throws_onEmptyUrl() {
        WebkitUtils.onMainThreadSync(() -> {
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mDefaultProfile.addQuicHints(Set.of(URL1, "")));
        });
    }

    @Test
    public void throws_onMalformedUrl() {
        WebkitUtils.onMainThreadSync(() -> {
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mDefaultProfile.addQuicHints(Set.of(URL1, "https:example.com:foo")));
        });
    }
}
