/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.net.Uri;
import android.os.Bundle;
import android.support.test.filters.SmallTest;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

@SmallTest
public class NavDeepLinkTest {
    private static final String DEEP_LINK_EXACT_NO_SCHEME = "www.example.com";
    private static final String DEEP_LINK_EXACT_HTTP = "http://" + DEEP_LINK_EXACT_NO_SCHEME;
    private static final String DEEP_LINK_EXACT_HTTPS = "https://" + DEEP_LINK_EXACT_NO_SCHEME;

    @Test
    public void deepLinkExactMatch() {
        NavDeepLink deepLink = new NavDeepLink(DEEP_LINK_EXACT_HTTP);

        assertThat(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTP)), is(true));
        assertThat(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTPS)), is(false));
    }

    @Test
    public void deepLinkExactMatchWithHyphens() {
        String deepLinkString = "android-app://com.example";
        NavDeepLink deepLink = new NavDeepLink(deepLinkString);

        assertThat(deepLink.matches(Uri.parse(deepLinkString)), is(true));
    }

    @Test
    public void deepLinkExactMatchNoScheme() {
        NavDeepLink deepLink = new NavDeepLink(DEEP_LINK_EXACT_NO_SCHEME);

        assertThat(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTP)), is(true));
        assertThat(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTPS)), is(true));
    }

    @Test
    public void deepLinkArgumentMatch() {
        String deepLinkArgument = DEEP_LINK_EXACT_HTTPS + "/users/{id}/posts";
        NavDeepLink deepLink = new NavDeepLink(deepLinkArgument);

        String id = "2";
        Bundle matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id)));
        assertThat(matchArgs, not(nullValue()));
        assertThat(matchArgs.getString("id"), is(id));
    }

    @Test
    public void deepLinkArgumentMatchEncoded() throws UnsupportedEncodingException {
        String deepLinkArgument = DEEP_LINK_EXACT_HTTPS + "/users/{name}/posts";
        NavDeepLink deepLink = new NavDeepLink(deepLinkArgument);

        String name = "John Doe";
        Bundle matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{name}", Uri.encode(name))));
        assertThat(matchArgs, not(nullValue()));
        assertThat(matchArgs.getString("name"), is(name));
    }

    @Test
    public void deepLinkMultipleArgumentMatch() {
        String deepLinkArgument = DEEP_LINK_EXACT_HTTPS + "/users/{id}/posts/{postId}";
        NavDeepLink deepLink = new NavDeepLink(deepLinkArgument);

        String id = "2";
        String postId = "42";
        Bundle matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id).replace("{postId}", postId)));
        assertThat(matchArgs, not(nullValue()));
        assertThat(matchArgs.getString("id"), is(id));
        assertThat(matchArgs.getString("postId"), is(postId));
    }

    @Test
    public void deepLinkEmptyArgumentNoMatch() {
        String deepLinkArgument = DEEP_LINK_EXACT_HTTPS + "/users/{id}/posts";
        NavDeepLink deepLink = new NavDeepLink(deepLinkArgument);

        assertThat(deepLink.matches(
                Uri.parse(deepLinkArgument.replace("{id}", ""))),
                is(false));
    }

    @Test
    public void deepLinkPrefixMatch() {
        String deepLinkPrefix = DEEP_LINK_EXACT_HTTPS + "/posts/.*";
        NavDeepLink deepLink = new NavDeepLink(deepLinkPrefix);

        assertThat(deepLink.matches(
                Uri.parse(deepLinkPrefix.replace(".*", "test"))),
                is(true));
    }

    @Test
    public void deepLinkWildcardMatch() {
        String deepLinkWildcard = DEEP_LINK_EXACT_HTTPS + "/posts/.*/new";
        NavDeepLink deepLink = new NavDeepLink(deepLinkWildcard);

        assertThat(deepLink.matches(
                Uri.parse(deepLinkWildcard.replace(".*", "test"))),
                is(true));
    }

    @Test
    public void deepLinkMultipleMatch() {
        String deepLinkMultiple = DEEP_LINK_EXACT_HTTPS + "/users/{id}/posts/.*";
        NavDeepLink deepLink = new NavDeepLink(deepLinkMultiple);

        String id = "2";
        Bundle matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkMultiple.replace("{id}", id)));
        assertThat(matchArgs, not(nullValue()));
        assertThat(matchArgs.getString("id"), is(id));
    }
}
