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

package androidx.javascriptengine;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Instrumentation test for JsSandboxService. */
@RunWith(AndroidJUnit4.class)
public class WebViewJsSandboxTest {
    private boolean canCreateJsSandbox() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox;
        try {
            jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return false;
        }
        jsSandbox.close();
        return true;
    }

    @Before
    public void setUp() throws Throwable {
        // Ensure WebView version supports creation of sandbox. Remove this once we have a client
        // side check.
        Assume.assumeTrue(canCreateJsSandbox());
    }

    @Test
    @MediumTest
    public void testSimpleJsEvaluation() throws Throwable {
        final String code = "\"PASS\"";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
        String result = resultFuture.get(5, TimeUnit.SECONDS);
        jsIsolate.close();
        jsSandbox.close();

        Assert.assertEquals(expected, result);
    }

    @Test
    @MediumTest
    public void testClosingOneIsolate() throws Throwable {
        final String code = "'PASS'";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate1 = jsSandbox.createIsolate();
        JsIsolate jsIsolate2 = jsSandbox.createIsolate();
        jsIsolate1.close();
        ListenableFuture<String> resultFuture = jsIsolate2.evaluateJavaScriptAsync(code);
        String result = resultFuture.get(5, TimeUnit.SECONDS);
        jsIsolate2.close();
        jsSandbox.close();

        Assert.assertEquals(expected, result);
    }

    @Test
    @MediumTest
    public void testEvaluationInTwoIsolates() throws Throwable {
        final String code1 = "this.x = 'PASS';\n";
        final String expected1 = "PASS";
        final String code2 = "this.x = 'SUPER_PASS';\n";
        final String expected2 = "SUPER_PASS";

        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate1 = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture1 = jsIsolate1.evaluateJavaScriptAsync(code1);
        String result1 = resultFuture1.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate2 = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture2 = jsIsolate2.evaluateJavaScriptAsync(code2);
        String result2 = resultFuture2.get(5, TimeUnit.SECONDS);
        jsIsolate1.close();
        jsIsolate2.close();
        jsSandbox.close();

        Assert.assertEquals(expected1, result1);
        Assert.assertEquals(expected2, result2);
    }

    @Test
    @MediumTest
    public void testTwoIsolatesDoNotShareEnvironment() throws Throwable {
        final String code1 = "this.y = 'PASS';\n";
        final String expected1 = "PASS";
        final String code2 = "this.y = this.y + ' PASS';\n";
        final String expected2 = "undefined PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate1 = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture1 = jsIsolate1.evaluateJavaScriptAsync(code1);
        String result1 = resultFuture1.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate2 = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture2 = jsIsolate2.evaluateJavaScriptAsync(code2);
        String result2 = resultFuture2.get(5, TimeUnit.SECONDS);
        jsIsolate1.close();
        jsIsolate2.close();
        jsSandbox.close();

        Assert.assertEquals(expected1, result1);
        Assert.assertEquals(expected2, result2);
    }

    @Test
    @MediumTest
    public void testTwoExecutionsShareEnvironment() throws Throwable {
        final String code1 = "this.z = 'PASS';\n";
        final String expected1 = "PASS";
        final String code2 = "this.z = this.z + ' PASS';\n";
        final String expected2 = "PASS PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate1 = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture1 = jsIsolate1.evaluateJavaScriptAsync(code1);
        String result1 = resultFuture1.get(5, TimeUnit.SECONDS);
        ListenableFuture<String> resultFuture2 = jsIsolate1.evaluateJavaScriptAsync(code2);
        String result2 = resultFuture2.get(5, TimeUnit.SECONDS);
        jsIsolate1.close();
        jsSandbox.close();

        Assert.assertEquals(expected1, result1);
        Assert.assertEquals(expected2, result2);
    }

    @Test
    @MediumTest
    public void testJsEvaluationError() throws Throwable {
        final String code = "throw new WebAssembly.LinkError('RandomLinkError');";
        final String contains = "RandomLinkError";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
        boolean isOfCorrectType = false;
        String error = "";
        try {
            String result = resultFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            isOfCorrectType = e.getCause().getClass().equals(EvaluationFailedException.class);
            error = e.getCause().getMessage();
        }
        jsIsolate.close();
        jsSandbox.close();

        Assert.assertTrue(isOfCorrectType);
        Assert.assertTrue(error.contains(contains));
    }

    @Test
    @MediumTest
    public void testInfiniteLoop() throws Throwable {
        final String code = "while(true){}";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_ISOLATE_TERMINATION));

        JsIsolate jsIsolate = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
        boolean isOfCorrectType = false;
        try {
            jsIsolate.close();
            String result = resultFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            isOfCorrectType = e.getCause().getClass().equals(IsolateTerminatedException.class);
        }
        jsSandbox.close();

        Assert.assertTrue(isOfCorrectType);
    }

    @Test
    @MediumTest
    public void testMultipleInfiniteLoops() throws Throwable {
        final String code = "while(true){}";
        final int num_of_evaluations = 10;
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_ISOLATE_TERMINATION));

        JsIsolate jsIsolate = jsSandbox.createIsolate();
        Vector<ListenableFuture<String>> resultFutures = new Vector<ListenableFuture<String>>();
        for (int i = 0; i < num_of_evaluations; i++) {
            ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            resultFutures.add(resultFuture);
        }
        jsIsolate.close();

        for (int i = 0; i < num_of_evaluations; i++) {
            boolean isOfCorrectType = false;
            try {
                String result = resultFutures.elementAt(i).get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                isOfCorrectType = e.getCause().getClass().equals(IsolateTerminatedException.class);
            }
            Assert.assertTrue(isOfCorrectType);
        }
        jsSandbox.close();
    }

    @Test
    @MediumTest
    public void testSimpleArrayBuffer() throws Throwable {
        final String provideString = "Hello World";
        final byte[] bytes = provideString.getBytes(StandardCharsets.US_ASCII);
        final String code = ""
                + "function ab2str(buf) {"
                + " return String.fromCharCode.apply(null, new Uint8Array(buf));"
                + "}"
                + "android.consumeNamedDataAsArrayBuffer(\"id-1\").then((value) => {"
                + " return ab2str(value);"
                + "});";
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
             JsIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(
                            JsSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));

            boolean provideNamedDataReturn = jsIsolate.provideNamedData("id-1", bytes);
            Assert.assertTrue(provideNamedDataReturn);
            ListenableFuture<String> resultFuture1 = jsIsolate.evaluateJavaScriptAsync(code);
            String result = resultFuture1.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(provideString, result);
        }
    }

    @Test
    @MediumTest
    public void testArrayBufferWasmCompilation() throws Throwable {
        final String success = "success";
        // The bytes of a minimal WebAssembly module, courtesy of v8/test/cctest/test-api-wasm.cc
        final byte[] bytes = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};
        final String code = ""
                + "android.consumeNamedDataAsArrayBuffer(\"id-1\").then((value) => {"
                + " return WebAssembly.compile(value).then((module) => {"
                + "  return \"success\";"
                + "  });"
                + "});";
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
             JsIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(
                            JsSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_WASM_COMPILATION));

            boolean provideNamedDataReturn = jsIsolate.provideNamedData("id-1", bytes);
            Assert.assertTrue(provideNamedDataReturn);
            ListenableFuture<String> resultFuture1 = jsIsolate.evaluateJavaScriptAsync(code);
            String result = resultFuture1.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(success, result);
        }
    }

    @Test
    @MediumTest
    public void testPromiseReturn() throws Throwable {
        final String code = "Promise.resolve(\"PASS\")";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
             JsIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_PROMISE_RETURN));

            ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            String result = resultFuture.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected, result);
        }
    }

    @Test
    @MediumTest
    public void testPromiseReturnLaterResolve() throws Throwable {
        final String code1 = "var promiseResolve, promiseReject;"
                + "new Promise(function(resolve, reject){"
                + "  promiseResolve = resolve;"
                + "  promiseReject = reject;"
                + "});";
        final String code2 = "promiseResolve(\"PASS\");";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
             JsIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_PROMISE_RETURN));

            ListenableFuture<String> resultFuture1 = jsIsolate.evaluateJavaScriptAsync(code1);
            ListenableFuture<String> resultFuture2 = jsIsolate.evaluateJavaScriptAsync(code2);
            String result = resultFuture1.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected, result);
        }
    }

    @Test
    @MediumTest
    public void testNestedConsumeNamedDataAsArrayBuffer() throws Throwable {
        final String success = "success";
        // The bytes of a minimal WebAssembly module, courtesy of v8/test/cctest/test-api-wasm.cc
        final byte[] bytes = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};
        final String code = ""
                + "android.consumeNamedDataAsArrayBuffer(\"id-1\").then((value) => {"
                + " return android.consumeNamedDataAsArrayBuffer(\"id-2\").then((value) => {"
                + "  return android.consumeNamedDataAsArrayBuffer(\"id-3\").then((value) => {"
                + "   return android.consumeNamedDataAsArrayBuffer(\"id-4\").then((value) => {"
                + "    return android.consumeNamedDataAsArrayBuffer(\"id-5\").then((value) => {"
                + "     return \"success\";"
                + "     }, (error) => {"
                + "     return error.message;"
                + "    });"
                + "   });"
                + "  });"
                + " });"
                + "});";
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
             JsIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JsSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));

            jsIsolate.provideNamedData("id-1", bytes);
            jsIsolate.provideNamedData("id-2", bytes);
            jsIsolate.provideNamedData("id-3", bytes);
            jsIsolate.provideNamedData("id-4", bytes);
            jsIsolate.provideNamedData("id-5", bytes);
            Thread.sleep(1000);
            ListenableFuture<String> resultFuture1 = jsIsolate.evaluateJavaScriptAsync(code);
            String result = resultFuture1.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(success, result);
        }
    }

    @Test
    @MediumTest
    public void testPromiseEvaluationThrow() throws Throwable {
        final String provideString = "Hello World";
        final byte[] bytes = provideString.getBytes(StandardCharsets.US_ASCII);
        final String code = ""
                + "android.consumeNamedDataAsArrayBuffer(\"id-1\").catch((error) => {"
                + " throw new WebAssembly.LinkError('RandomLinkError');"
                + "});";
        final String contains = "RandomLinkError";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
             JsIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(JsSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JsSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));

            ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            try {
                String result = resultFuture.get(5, TimeUnit.SECONDS);
                Assert.fail("Should have thrown.");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof EvaluationFailedException)) {
                    throw e;
                }
                Assert.assertTrue(e.getCause().getMessage().contains(contains));
            }
        }
    }

    @Test
    @MediumTest
    public void testEvaluationThrowsWhenSandboxDead() throws Throwable {
        final String code = "while(true){}";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox = JsSandboxFuture.get(5, TimeUnit.SECONDS);
        JsIsolate jsIsolate = jsSandbox.createIsolate();
        ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
        try {
            jsSandbox.close();
            resultFuture.get(5, TimeUnit.SECONDS);
            Assert.fail("Should have thrown.");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IsolateTerminatedException)) {
                throw e;
            }
        }
    }

    @Test
    @MediumTest
    public void testMultipleSandboxesCannotCoexist() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        final String contains = "already bound";
        ListenableFuture<JsSandbox> JsSandboxFuture1 =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox1 = JsSandboxFuture1.get(5, TimeUnit.SECONDS)) {
            ListenableFuture<JsSandbox> JsSandboxFuture2 =
                    JsSandbox.createConnectedInstanceAsync(context);
            try {
                JsSandbox jsSandbox2 = JsSandboxFuture2.get(5, TimeUnit.SECONDS);
                Assert.fail("Should have thrown.");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof RuntimeException)) {
                    throw e;
                }
                Assert.assertTrue(e.getCause().getMessage().contains(contains));
            }
        }
    }

    @Test
    @MediumTest
    public void testSandboxCanBeCreatedAfterClosed() throws Throwable {
        final String code = "\"PASS\"";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JsSandbox> JsSandboxFuture1 =
                JsSandbox.createConnectedInstanceAsync(context);
        JsSandbox jsSandbox1 = JsSandboxFuture1.get(5, TimeUnit.SECONDS);
        jsSandbox1.close();
        ListenableFuture<JsSandbox> JsSandboxFuture2 =
                JsSandbox.createConnectedInstanceAsync(context);
        try (JsSandbox jsSandbox2 = JsSandboxFuture2.get(5, TimeUnit.SECONDS);
             JsIsolate jsIsolate = jsSandbox2.createIsolate()) {
            ListenableFuture<String> resultFuture1 = jsIsolate.evaluateJavaScriptAsync(code);
            String result = resultFuture1.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected, result);
        }
    }
}
