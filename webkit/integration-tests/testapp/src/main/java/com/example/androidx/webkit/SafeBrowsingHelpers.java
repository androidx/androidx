/*
 * Copyright 2019 The Android Open Source Project
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

import android.net.Uri;

/**
 * Static utility methods for Safe Browsing functionality.
 */
public final class SafeBrowsingHelpers {
    public static final String TEST_SAFE_BROWSING_DOMAIN = "testsafebrowsing.appspot.com";
    public static final String MALWARE_URL = new Uri.Builder()
            .scheme("http")
            .authority(TEST_SAFE_BROWSING_DOMAIN)
            .path("/s/malware.html")
            .build()
            .toString();
    public static final String PHISHING_URL = new Uri.Builder()
            .scheme("http")
            .authority(TEST_SAFE_BROWSING_DOMAIN)
            .path("/s/phishing.html")
            .build()
            .toString();
    public static final String UNWANTED_SOFTWARE_URL = new Uri.Builder()
            .scheme("http")
            .authority(TEST_SAFE_BROWSING_DOMAIN)
            .path("/s/unwanted.html")
            .build()
            .toString();
    public static final String BILLING_URL = new Uri.Builder()
            .scheme("http")
            .authority(TEST_SAFE_BROWSING_DOMAIN)
            .path("/s/trick_to_bill.html")
            .build()
            .toString();
    public static final String TEST_SAFE_BROWSING_SITE = new Uri.Builder()
            .scheme("http")
            .authority(TEST_SAFE_BROWSING_DOMAIN)
            .path("/")
            .build()
            .toString();

    // Do not instantiate this class.
    private SafeBrowsingHelpers() {}
}
