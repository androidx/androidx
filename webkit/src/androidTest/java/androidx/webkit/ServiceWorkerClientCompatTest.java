/*
 * Copyright 2018 The Android Open Source Project
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
import static org.junit.Assume.assumeTrue;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ServiceWorkerClientCompatTest {

    // The BASE_URL does not matter since the tests will intercept the load, but it should be https
    // for the Service Worker registration to succeed.
    private static final String BASE_URL = "https://www.example.com/";
    private static final String INDEX_URL = BASE_URL + "index.html";
    private static final String SW_URL = BASE_URL + "sw.js";
    private static final String FETCH_URL = BASE_URL + "fetch.html";

    private static final String JS_INTERFACE_NAME = "Android";
    private static final int POLLING_TIMEOUT = 10 * 1000;

    // static HTML page always injected instead of the url loaded.
    private static final String INDEX_RAW_HTML =
            "<!DOCTYPE html>\n"
                    + "<html>\n"
                    + "  <body>\n"
                    + "    <script>\n"
                    + "      navigator.serviceWorker.register('sw.js').then(function(reg) {\n"
                    + "         " + JS_INTERFACE_NAME + ".registrationSuccess();\n"
                    + "      }).catch(function(err) {\n"
                    + "         console.error(err);\n"
                    + "      });\n"
                    + "    </script>\n"
                    + "  </body>\n"
                    + "</html>\n";
    private static final String SW_RAW_HTML = "fetch('fetch.html');";
    private static final String SW_UNREGISTER_RAW_JS =
            "navigator.serviceWorker.getRegistration().then(function(r) {"
                    + "  r.unregister().then(function(success) {"
                    + "    if (success) " + JS_INTERFACE_NAME + ".unregisterSuccess();"
                    + "    else console.error('unregister() was not successful');"
                    + "  });"
                    + "}).catch(function(err) {"
                    + "   console.error(err);"
                    + "});";

    private JavascriptStatusReceiver mJavascriptStatusReceiver;
    private WebViewOnUiThread mOnUiThread;

    // Both this test and WebViewOnUiThread need to override some of the methods on WebViewClient,
    // so this test subclasses the WebViewClient from WebViewOnUiThread.
    private static class InterceptClient extends WebViewOnUiThread.WaitForLoadedClient {

        InterceptClient(WebViewOnUiThread webViewOnUiThread) throws Exception {
            super(webViewOnUiThread);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                WebResourceRequest request) {
            // Only return content for INDEX_URL, deny all other requests.
            try {
                if (request.getUrl().toString().equals(INDEX_URL)) {
                    return new WebResourceResponse("text/html", "utf-8",
                            new ByteArrayInputStream(INDEX_RAW_HTML.getBytes("UTF-8")));
                }
            } catch (java.io.UnsupportedEncodingException e) { }
            return new WebResourceResponse("text/html", "UTF-8", null);
        }
    }

    public static class InterceptServiceWorkerClient extends ServiceWorkerClientCompat {
        private List<WebResourceRequest> mInterceptedRequests = new ArrayList<WebResourceRequest>();

        @Override
        public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
            // Records intercepted requests and only return content for SW_URL.
            mInterceptedRequests.add(request);
            try {
                if (request.getUrl().toString().equals(SW_URL)) {
                    return new WebResourceResponse("application/javascript", "utf-8",
                            new ByteArrayInputStream(SW_RAW_HTML.getBytes("UTF-8")));
                }
            } catch (java.io.UnsupportedEncodingException e) { }
            return new WebResourceResponse("text/html", "UTF-8", null);
        }

        List<WebResourceRequest> getInterceptedRequests() {
            return mInterceptedRequests;
        }
    }

    @Before
    public void setUp() throws Exception {
        mOnUiThread = new WebViewOnUiThread();
        mOnUiThread.getSettings().setJavaScriptEnabled(true);

        mJavascriptStatusReceiver = new JavascriptStatusReceiver();
        mOnUiThread.addJavascriptInterface(mJavascriptStatusReceiver, JS_INTERFACE_NAME);
        mOnUiThread.setWebViewClient(new InterceptClient(mOnUiThread));
    }

    @After
    public void tearDown() throws Exception {
        if (mOnUiThread != null) {
            mOnUiThread.cleanUp();
        }
    }

    // Test correct invocation of shouldInterceptRequest for Service Workers.
    @Test
    public void testServiceWorkerClientInterceptCallback() throws Exception {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(
                WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST));

        final InterceptServiceWorkerClient mInterceptServiceWorkerClient =
                new InterceptServiceWorkerClient();
        ServiceWorkerControllerCompat swController = ServiceWorkerControllerCompat.getInstance();
        swController.setServiceWorkerClient(mInterceptServiceWorkerClient);

        mOnUiThread.loadUrlAndWaitForCompletion(INDEX_URL);

        Callable<Boolean> registrationSuccess = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mJavascriptStatusReceiver.mRegistrationSuccess;
            }
        };
        PollingCheck.check("JS could not register Service Worker", POLLING_TIMEOUT,
                registrationSuccess);

        Callable<Boolean> receivedRequest = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mInterceptServiceWorkerClient.getInterceptedRequests().size() >= 2;
            }
        };
        PollingCheck.check("Service Worker intercept callbacks not invoked", POLLING_TIMEOUT,
                receivedRequest);

        List<WebResourceRequest> requests = mInterceptServiceWorkerClient.getInterceptedRequests();
        assertEquals(2, requests.size());
        assertEquals(SW_URL, requests.get(0).getUrl().toString());
        assertEquals(FETCH_URL, requests.get(1).getUrl().toString());

        // Clean-up, make sure to unregister the Service Worker.
        mOnUiThread.evaluateJavascript(SW_UNREGISTER_RAW_JS, null);
        Callable<Boolean> unregisterSuccess = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mJavascriptStatusReceiver.mUnregisterSuccess;
            }
        };
        PollingCheck.check("JS could not unregister Service Worker", POLLING_TIMEOUT,
                unregisterSuccess);
    }

    // Object added to the page via AddJavascriptInterface() that is used by the test Javascript to
    // notify back to Java if the Service Worker registration was successful.
    public static final class JavascriptStatusReceiver {
        public volatile boolean mRegistrationSuccess = false;
        public volatile boolean mUnregisterSuccess = false;

        @JavascriptInterface
        public void registrationSuccess() {
            mRegistrationSuccess = true;
        }

        @JavascriptInterface
        public void unregisterSuccess() {
            mUnregisterSuccess = true;
        }
    }
}
