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

import android.webkit.WebResourceResponse;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.WebkitUtils;

import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Tests for {@link WebResourceResponseCompat}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class WebResourceResponseCompatTest {

    private static final String MULTI_COOKIE_KEY =
            WebViewProviderFactoryBoundaryInterface.MULTI_COOKIE_HEADER_NAME;
    private static final String MULTI_COOKIE_SEPARATOR =
            WebViewProviderFactoryBoundaryInterface.MULTI_COOKIE_VALUE_SEPARATOR;
    private static final String MIMETYPE = "text/text";

    @Test
    public void shortConstructorSetsValues() {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        WebResourceResponseCompat response = new WebResourceResponseCompat("text/html", "utf-16",
                is);
        Assert.assertEquals("text/html", response.getMimeType());
        Assert.assertEquals("utf-16", response.getEncoding());
        Assert.assertEquals(is, response.getData());

        WebResourceResponse baseResponse = response.toWebResourceResponse();
        Assert.assertEquals("text/html", baseResponse.getMimeType());
        Assert.assertEquals("utf-16", baseResponse.getEncoding());
        Assert.assertEquals(is, baseResponse.getData());
    }

    @Test
    public void longConstructorSetsValues() {
        Map<String, String> headers = Map.of("X-MoreData", "Value");
        InputStream is = new ByteArrayInputStream(new byte[0]);
        WebResourceResponseCompat response = new WebResourceResponseCompat("text/html", "utf-16",
                418, "I'm a Teapot", headers, is);
        Assert.assertEquals("text/html", response.getMimeType());
        Assert.assertEquals("utf-16", response.getEncoding());
        Assert.assertEquals(418, response.getStatusCode());
        Assert.assertEquals("I'm a Teapot", response.getReasonPhrase());
        Assert.assertEquals(headers, response.getResponseHeaders());
        Assert.assertEquals(is, response.getData());

        WebResourceResponse baseResponse = response.toWebResourceResponse();
        Assert.assertEquals("text/html", baseResponse.getMimeType());
        Assert.assertEquals("utf-16", baseResponse.getEncoding());
        Assert.assertEquals(418, baseResponse.getStatusCode());
        Assert.assertEquals("I'm a Teapot", baseResponse.getReasonPhrase());
        Assert.assertEquals(headers, baseResponse.getResponseHeaders());
        Assert.assertEquals(is, baseResponse.getData());
    }

    @Test
    public void valuesPreservedThroughRoundTripShort() {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        WebResourceResponse startResponse = new WebResourceResponse("text/html", "utf-16", is);
        WebResourceResponseCompat response = WebResourceResponseCompat.toWebResourceResponseCompat(
                startResponse);
        Assert.assertEquals("text/html", response.getMimeType());
        Assert.assertEquals("utf-16", response.getEncoding());
        Assert.assertEquals(is, response.getData());

        WebResourceResponse baseResponse = response.toWebResourceResponse();
        Assert.assertEquals("text/html", baseResponse.getMimeType());
        Assert.assertEquals("utf-16", baseResponse.getEncoding());
        Assert.assertEquals(is, baseResponse.getData());
    }

    @Test
    public void valuesPreservedThroughRoundTripLongConstructor() {
        Map<String, String> headers = Map.of("X-MoreData", "Value");
        InputStream is = new ByteArrayInputStream(new byte[0]);
        WebResourceResponse startResponse = new WebResourceResponse("text/html", "utf-16", 418,
                "I'm a Teapot", headers, is);
        WebResourceResponseCompat response = WebResourceResponseCompat.toWebResourceResponseCompat(
                startResponse);
        Assert.assertEquals("text/html", response.getMimeType());
        Assert.assertEquals("utf-16", response.getEncoding());
        Assert.assertEquals(418, response.getStatusCode());
        Assert.assertEquals("I'm a Teapot", response.getReasonPhrase());
        Assert.assertEquals(headers, response.getResponseHeaders());
        Assert.assertEquals(is, response.getData());

        WebResourceResponse baseResponse = response.toWebResourceResponse();
        Assert.assertEquals("text/html", baseResponse.getMimeType());
        Assert.assertEquals("utf-16", baseResponse.getEncoding());
        Assert.assertEquals(418, baseResponse.getStatusCode());
        Assert.assertEquals("I'm a Teapot", baseResponse.getReasonPhrase());
        Assert.assertEquals(headers, baseResponse.getResponseHeaders());
        Assert.assertEquals(is, baseResponse.getData());
    }

    @Test
    public void canGetAndSetWebResourceResponseValues() {
        WebResourceResponseCompat response = new WebResourceResponseCompat(MIMETYPE, null, null);

        response.setMimeType("text/html");
        Assert.assertEquals("text/html", response.getMimeType());

        response.setEncoding("utf-16");
        Assert.assertEquals("utf-16", response.getEncoding());

        response.setStatusCodeAndReasonPhrase(418, "I'm a Teapot");
        Assert.assertEquals(418, response.getStatusCode());
        Assert.assertEquals("I'm a Teapot", response.getReasonPhrase());

        Map<String, String> headers = Map.of("X-MoreData", "Value");
        response.setResponseHeaders(headers);
        Assert.assertEquals(headers, response.getResponseHeaders());

        InputStream is = new ByteArrayInputStream(new byte[0]);
        response.setData(is);
        Assert.assertEquals(is, response.getData());

        WebResourceResponse baseResponse = response.toWebResourceResponse();
        Assert.assertEquals("text/html", baseResponse.getMimeType());
        Assert.assertEquals("utf-16", baseResponse.getEncoding());
        Assert.assertEquals(418, baseResponse.getStatusCode());
        Assert.assertEquals("I'm a Teapot", baseResponse.getReasonPhrase());
        Assert.assertEquals(headers, baseResponse.getResponseHeaders());
        Assert.assertEquals(is, baseResponse.getData());
    }

    @Test
    public void canGetUnmodifiedResponseHeadersSetByConstructor() {
        WebkitUtils.checkFeature(WebViewFeature.COOKIE_INTERCEPT);

        Map<String, String> testHeaders = Map.of("Content-Disposition", "attachment", "Encoding",
                "gzip", "X-Response", "Value");
        WebResourceResponseCompat response = new WebResourceResponseCompat(MIMETYPE, "utf-8", 200,
                "OK", testHeaders, null);

        Assert.assertEquals(testHeaders, response.toWebResourceResponse().getResponseHeaders());
    }

    @Test
    public void responseHeadersContainsMergedSetCookieValues() {
        WebkitUtils.checkFeature(WebViewFeature.COOKIE_INTERCEPT);
        WebResourceResponseCompat response = new WebResourceResponseCompat(MIMETYPE, "utf-8", null);

        List<String> cookieValues = List.of("foo=bar", "bar=baz");
        response.setCookies(cookieValues);

        Map<String, String> responseHeaders = response.toWebResourceResponse().getResponseHeaders();
        Assert.assertTrue(responseHeaders.containsKey(MULTI_COOKIE_KEY));

        String serialized = String.join(MULTI_COOKIE_SEPARATOR, cookieValues);
        Assert.assertEquals(serialized, responseHeaders.get(MULTI_COOKIE_KEY));
    }

    @Test
    public void responseHeadersContainsMergedSetCookieValuesAndOtherHeaders() {
        WebkitUtils.checkFeature(WebViewFeature.COOKIE_INTERCEPT);
        WebResourceResponseCompat response = new WebResourceResponseCompat(MIMETYPE, "utf-8", null);

        Map<String, String> testHeaders = Map.of("Content-Disposition", "attachment", "Encoding",
                "gzip", "X-Response", "Value");
        response.setResponseHeaders(testHeaders);

        List<String> cookieValues = List.of("foo=bar", "bar=baz");
        response.setCookies(cookieValues);


        String serialized = String.join(MULTI_COOKIE_SEPARATOR, cookieValues);
        Map<String, String> expected = new HashMap<>(testHeaders);
        expected.put(MULTI_COOKIE_KEY, serialized);

        Assert.assertEquals(expected, response.toWebResourceResponse().getResponseHeaders());
    }

    @Test
    public void mergedCookieValuesAreTrimmed() {
        WebkitUtils.checkFeature(WebViewFeature.COOKIE_INTERCEPT);
        WebResourceResponseCompat response = new WebResourceResponseCompat(MIMETYPE, "utf-8", null);

        // Values deliberately contains leading and trailing spaces.
        List<String> cookieValues = List.of("  foo=bar; domain=example.com; path=/   ",
                "  bar=baz; domain=example.com; path=/index.html   ");
        response.setCookies(cookieValues);

        Map<String, String> responseHeaders = response.toWebResourceResponse().getResponseHeaders();
        Assert.assertTrue(responseHeaders.containsKey(MULTI_COOKIE_KEY));

        String expected = "foo=bar; domain=example.com; path=/" + MULTI_COOKIE_SEPARATOR
                + "bar=baz; domain=example.com; path=/index.html";
        Assert.assertEquals(expected, responseHeaders.get(MULTI_COOKIE_KEY));
    }

    @Test
    public void mergedCookieValuesExcludeBlankValues() {
        WebkitUtils.checkFeature(WebViewFeature.COOKIE_INTERCEPT);
        WebResourceResponseCompat response = new WebResourceResponseCompat(MIMETYPE, "utf-8", null);

        // Values deliberately contains a blank string.
        List<String> cookieValues = List.of("foo=bar; domain=example.com; path=/   ", "  ",
                "  bar=baz; domain=example.com; path=/index.html   ");
        response.setCookies(cookieValues);

        Map<String, String> responseHeaders = response.toWebResourceResponse().getResponseHeaders();
        Assert.assertTrue(responseHeaders.containsKey(MULTI_COOKIE_KEY));

        String expected = "foo=bar; domain=example.com; path=/" + MULTI_COOKIE_SEPARATOR
                + "bar=baz; domain=example.com; path=/index.html";
        Assert.assertEquals(expected, responseHeaders.get(MULTI_COOKIE_KEY));
    }

}
