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

package androidx.webkit;

import static org.junit.Assert.assertEquals;

import android.webkit.CookieManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CookieManagerCompatTest {
    @Test
    public void testGetCookieInfo() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.GET_COOKIE_INFO);
        String url = "http://www.example.com";
        String cookie = "foo=bar; domain=.example.com; path=/";
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie(url, cookie);

        List<String> cookies = CookieManagerCompat.getCookieInfo(
                cookieManager, url);
        assertEquals(1, cookies.size());
        assertEquals(cookie, cookies.get(0));
    }
}
