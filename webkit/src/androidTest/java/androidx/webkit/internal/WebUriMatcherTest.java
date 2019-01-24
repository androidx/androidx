/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.webkit.internal;

import android.net.Uri;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WebUriMatcherTest {

    @Test
    @SmallTest
    public void testFullHttpUriMatching() {
        final Object pathMatch = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("http", "androidplatform.net", "/some/path", pathMatch);

        Assert.assertEquals("Failed to match the exact http path", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path")));
        Assert.assertEquals("Failed to match http path with query", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path?asd")));
        Assert.assertEquals("Failed to match http path with fragment", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path#asd")));
        Assert.assertEquals("Failed to match http path starts with a correct prefix", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path/thing")));

        Assert.assertNull("Incorrectly matched https path",
                    matcher.match(Uri.parse("https://androidplatform.net/some/path")));
        Assert.assertNull("Incorrectly matched http path without a separating slash",
                    matcher.match(Uri.parse("http://androidplatform.net/somepath")));
        Assert.assertNull("Incorrectly matched http .../some/path with .../some/paththing",
                    matcher.match(Uri.parse("http://androidplatform.net/some/paththing")));
        Assert.assertNull("Incorrectly matched http with an empty path",
                    matcher.match(Uri.parse("http://androidplatform.net/")));
        Assert.assertNull("Incorrectly matched http with a different prefix",
                    matcher.match(Uri.parse("http://androidplatform.net/another/some/path")));
    }

    @Test
    @SmallTest
    public void testFullHttpsUriMatching() {
        final Object pathMatch = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("https", "androidplatform.net", "/some/path", pathMatch);

        Assert.assertEquals("Failed to match the exact https path", pathMatch,
                    matcher.match(Uri.parse("https://androidplatform.net/some/path")));
        Assert.assertEquals("Failed to match https path with query", pathMatch,
                    matcher.match(Uri.parse("https://androidplatform.net/some/path?asd")));
        Assert.assertEquals("Failed to match https path with fragment", pathMatch,
                    matcher.match(Uri.parse("https://androidplatform.net/some/path#asd")));
        Assert.assertEquals("Failed to match https path starts with a correct prefix", pathMatch,
                    matcher.match(Uri.parse("https://androidplatform.net/some/path/thing")));

        Assert.assertNull("Incorrectly matched http path",
                    matcher.match(Uri.parse("http://androidplatform.net/some/path")));
        Assert.assertNull("Incorrectly matched https path without a separating slash",
                    matcher.match(Uri.parse("https://androidplatform.net/somepath")));
        Assert.assertNull("Incorrectly matched https .../some/path with .../some/paththing",
                    matcher.match(Uri.parse("https://androidplatform.net/some/paththing")));
        Assert.assertNull("Incorrectly matched https with an empty path",
                    matcher.match(Uri.parse("https://androidplatform.net/")));
        Assert.assertNull("Incorrectly matched https with a different prefix",
                    matcher.match(Uri.parse("https://androidplatform.net/another/some/path")));
    }

    @Test
    @SmallTest
    public void testHttpandHttpsUriMatching() {
        final Object pathMatch = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("http", "androidplatform.net", "/some/path", pathMatch);
        matcher.addUri("https", "androidplatform.net", "/some/path", pathMatch);

        Assert.assertEquals("Failed to match the exact http path", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path/")));
        Assert.assertEquals("Failed to match the exact https path", pathMatch,
                    matcher.match(Uri.parse("https://androidplatform.net/some/path/")));
    }

    @Test
    @SmallTest
    public void testEmptyPathMatching() {
        final Object pathMatch = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("http", "androidplatform.net", "/", pathMatch);

        Assert.assertEquals("Failed to match the exact http path", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/")));
        Assert.assertEquals("Failed to match the exact https path", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path")));
        Assert.assertEquals("Failed to match the exact http path", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/file.ext")));
        Assert.assertEquals("Failed to match the exact https path", pathMatch,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path/file.ext")));
    }

    @Test
    @SmallTest
    public void testManyUriMatchers() {
        final Object pathMatch1 = new Object();
        final Object pathMatch2 = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("http", "androidplatform.net", "/some/path_", pathMatch1);
        matcher.addUri("http", "androidplatform.net", "/some/path_2", pathMatch2);

        Assert.assertEquals("Failed to match the exact http .../some/path_", pathMatch1,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path_")));
        Assert.assertEquals("Failed to match the exact http .../some/path_2", pathMatch2,
                    matcher.match(Uri.parse("http://androidplatform.net/some/path_2")));
        Assert.assertNull("Incorrectly matched a non registered path prefix .../some/path_1",
                    matcher.match(Uri.parse("http://androidplatform.net/some/path_1")));
    }
}
