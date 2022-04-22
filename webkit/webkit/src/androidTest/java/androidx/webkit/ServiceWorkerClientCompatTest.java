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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;

import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class ServiceWorkerClientCompatTest {

    // This test relies on
    // https://chromiumdash.appspot.com/commit/48da90e0c6296add4c10f241551f0538fd3f968e
    // being available.
    private static final WebViewVersion MINIMUM_WEBVIEW_VERSION =
            new WebViewVersion("102.0.4997.0");

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
            } catch (UnsupportedEncodingException e) { }
            return new WebResourceResponse("text/html", "UTF-8", null);
        }
    }

    public static class InterceptServiceWorkerClient extends ServiceWorkerClientCompat {
        private final List<WebResourceRequest> mInterceptedRequests = new ArrayList<>();

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(@NonNull WebResourceRequest request) {
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
        Assume.assumeThat("Installed webview version does not support setting a null "
                        + "ServiceWorkerClient",
                WebViewVersion.getInstalledWebViewVersionFromPackage(),
                greaterThanOrEqualTo(MINIMUM_WEBVIEW_VERSION));
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
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(null);
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerClientTest#testServiceWorkerClientInterceptCallback.
     * Modifications to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    // Test correct invocation of shouldInterceptRequest for Service Workers.
    @Test
    public void testServiceWorkerClientInterceptCallback() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_BASIC_USAGE);
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST);

        final InterceptServiceWorkerClient mInterceptServiceWorkerClient =
                new InterceptServiceWorkerClient();
        ServiceWorkerControllerCompat swController = ServiceWorkerControllerCompat.getInstance();
        swController.setServiceWorkerClient(mInterceptServiceWorkerClient);

        mOnUiThread.loadUrlAndWaitForCompletion(INDEX_URL);

        Callable<Boolean> registrationSuccess =
                () -> mJavascriptStatusReceiver.mRegistrationSuccess;
        PollingCheck.check("JS could not register Service Worker", POLLING_TIMEOUT,
                registrationSuccess);

        Callable<Boolean> receivedRequest =
                () -> mInterceptServiceWorkerClient.getInterceptedRequests().size() >= 2;
        PollingCheck.check("Service Worker intercept callbacks not invoked", POLLING_TIMEOUT,
                receivedRequest);

        List<WebResourceRequest> requests = mInterceptServiceWorkerClient.getInterceptedRequests();
        assertEquals(2, requests.size());
        assertEquals(SW_URL, requests.get(0).getUrl().toString());
        assertEquals(FETCH_URL, requests.get(1).getUrl().toString());

        // Clean-up, make sure to unregister the Service Worker.
        mOnUiThread.evaluateJavascript(SW_UNREGISTER_RAW_JS, null);
        Callable<Boolean> unregisterSuccess = () -> mJavascriptStatusReceiver.mUnregisterSuccess;
        PollingCheck.check("JS could not unregister Service Worker", POLLING_TIMEOUT,
                unregisterSuccess);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerClientTest#testSetNullServiceWorkerClient.
     * Modifications to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    // Test setting a null ServiceWorkerClient.
    @Test
    public void testSetNullServiceWorkerClient() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_BASIC_USAGE);
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST);

        ServiceWorkerControllerCompat swController = ServiceWorkerControllerCompat.getInstance();
        swController.setServiceWorkerClient(null);
        mOnUiThread.loadUrlAndWaitForCompletion(INDEX_URL);

        Callable<Boolean> registrationFailure =
                () -> !mJavascriptStatusReceiver.mRegistrationSuccess;
        PollingCheck.check("JS unexpectedly registered the Service Worker", POLLING_TIMEOUT,
                registrationFailure);
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
