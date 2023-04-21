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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Instrumentation test for JavaScriptSandbox. */
@RunWith(AndroidJUnit4.class)
public class WebViewJavaScriptSandboxTest {
    // This value is somewhat arbitrary. It might need bumping if V8 snapshots become significantly
    // larger in future. However, we don't want it too large as that will make the tests slower and
    // require more memory. Although this is a long, it must not be greater than Integer.MAX_VALUE
    // and should be much smaller (for the purposes of testing).
    private static final long REASONABLE_HEAP_SIZE = 100 * 1024 * 1024;

    @Before
    public void setUp() throws Throwable {
        Assume.assumeTrue(JavaScriptSandbox.isSupported());
    }

    @Test
    @MediumTest
    public void testSimpleJsEvaluation() throws Throwable {
        final String code = "\"PASS\"";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            String result = resultFuture.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected, result);
        }
    }

    @Test
    @MediumTest
    public void testClosingOneIsolate() throws Throwable {
        final String code = "'PASS'";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate1 = jsSandbox.createIsolate()) {
            JavaScriptIsolate jsIsolate2 = jsSandbox.createIsolate();
            jsIsolate2.close();

            ListenableFuture<String> resultFuture = jsIsolate1.evaluateJavaScriptAsync(code);
            String result = resultFuture.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected, result);
        }
    }

    @Test
    @MediumTest
    public void testEvaluationInTwoIsolates() throws Throwable {
        final String code1 = "this.x = 'PASS';\n";
        final String expected1 = "PASS";
        final String code2 = "this.x = 'SUPER_PASS';\n";
        final String expected2 = "SUPER_PASS";

        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate1 = jsSandbox.createIsolate();
             JavaScriptIsolate jsIsolate2 = jsSandbox.createIsolate()) {
            ListenableFuture<String> resultFuture1 = jsIsolate1.evaluateJavaScriptAsync(code1);
            String result1 = resultFuture1.get(5, TimeUnit.SECONDS);
            ListenableFuture<String> resultFuture2 = jsIsolate2.evaluateJavaScriptAsync(code2);
            String result2 = resultFuture2.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected1, result1);
            Assert.assertEquals(expected2, result2);
        }
    }

    @Test
    @MediumTest
    public void testTwoIsolatesDoNotShareEnvironment() throws Throwable {
        final String code1 = "this.y = 'PASS';\n";
        final String expected1 = "PASS";
        final String code2 = "this.y = this.y + ' PASS';\n";
        final String expected2 = "undefined PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate1 = jsSandbox.createIsolate();
             JavaScriptIsolate jsIsolate2 = jsSandbox.createIsolate()) {
            ListenableFuture<String> resultFuture1 = jsIsolate1.evaluateJavaScriptAsync(code1);
            String result1 = resultFuture1.get(5, TimeUnit.SECONDS);
            ListenableFuture<String> resultFuture2 = jsIsolate2.evaluateJavaScriptAsync(code2);
            String result2 = resultFuture2.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected1, result1);
            Assert.assertEquals(expected2, result2);
        }
    }

    @Test
    @MediumTest
    public void testTwoExecutionsShareEnvironment() throws Throwable {
        final String code1 = "this.z = 'PASS';\n";
        final String expected1 = "PASS";
        final String code2 = "this.z = this.z + ' PASS';\n";
        final String expected2 = "PASS PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate1 = jsSandbox.createIsolate()) {
            ListenableFuture<String> resultFuture1 = jsIsolate1.evaluateJavaScriptAsync(code1);
            String result1 = resultFuture1.get(5, TimeUnit.SECONDS);
            ListenableFuture<String> resultFuture2 = jsIsolate1.evaluateJavaScriptAsync(code2);
            String result2 = resultFuture2.get(5, TimeUnit.SECONDS);

            Assert.assertEquals(expected1, result1);
            Assert.assertEquals(expected2, result2);
        }
    }

    @Test
    @MediumTest
    public void testJsEvaluationError() throws Throwable {
        final String code = "throw new WebAssembly.LinkError('RandomLinkError');";
        final String contains = "RandomLinkError";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            boolean isOfCorrectType = false;
            String error = "";
            try {
                resultFuture.get(5, TimeUnit.SECONDS);
                Assert.fail("Should have thrown.");
            } catch (ExecutionException e) {
                isOfCorrectType = e.getCause().getClass().equals(EvaluationFailedException.class);
                error = e.getCause().getMessage();
            }

            Assert.assertTrue(isOfCorrectType);
            Assert.assertTrue(error.contains(contains));
        }
    }

    @Test
    @MediumTest
    public void testInfiniteLoop() throws Throwable {
        final String code = "while(true){}";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_ISOLATE_TERMINATION));

            ListenableFuture<String> resultFuture;
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            }

            try {
                resultFuture.get(5, TimeUnit.SECONDS);
                Assert.fail("Should have thrown.");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof IsolateTerminatedException)) {
                    throw e;
                }
            }
        }
    }

    @Test
    @MediumTest
    public void testMultipleInfiniteLoops() throws Throwable {
        final String code = "while(true){}";
        final int num_of_evaluations = 10;
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_ISOLATE_TERMINATION));

            Vector<ListenableFuture<String>> resultFutures = new Vector<ListenableFuture<String>>();
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                for (int i = 0; i < num_of_evaluations; i++) {
                    ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
                    resultFutures.add(resultFuture);
                }
            }

            for (int i = 0; i < num_of_evaluations; i++) {
                try {
                    resultFutures.elementAt(i).get(5, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown.");
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof IsolateTerminatedException)) {
                        throw e;
                    }
                }
            }
        }
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
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));

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
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_WASM_COMPILATION));

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
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));

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

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));

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
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));

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

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));

            ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            try {
                resultFuture.get(5, TimeUnit.SECONDS);
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
    public void testEvaluationThrowsWhenSandboxClosed() throws Throwable {
        final String code = "while(true){}";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            ListenableFuture<String> resultFuture1 = jsIsolate.evaluateJavaScriptAsync(code);
            jsSandbox.close();
            // Check already running evaluation gets SandboxDeadException
            try {
                resultFuture1.get(5, TimeUnit.SECONDS);
                Assert.fail("Should have thrown.");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof SandboxDeadException)) {
                    throw e;
                }
            }
            // Check post-close evaluation gets SandboxDeadException
            ListenableFuture<String> resultFuture2 = jsIsolate.evaluateJavaScriptAsync(code);
            try {
                resultFuture2.get(5, TimeUnit.SECONDS);
                Assert.fail("Should have thrown.");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof SandboxDeadException)) {
                    throw e;
                }
            }
            // Check that closing an isolate then causes the IllegalStateException to be
            // thrown instead.
            jsIsolate.close();
            try {
                ListenableFuture<String> postCloseResultFuture =
                        jsIsolate.evaluateJavaScriptAsync(code);
                Assert.fail("Should have thrown.");
            } catch (IllegalStateException e) {
                // Expected
            }
        }
    }

    @Test
    @MediumTest
    public void testMultipleSandboxesCannotCoexist() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        final String contains = "already bound";
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture1 =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox1 = jsSandboxFuture1.get(5, TimeUnit.SECONDS)) {
            ListenableFuture<JavaScriptSandbox> jsSandboxFuture2 =
                    JavaScriptSandbox.createConnectedInstanceAsync(context);
            try {
                try (JavaScriptSandbox jsSandbox2 = jsSandboxFuture2.get(5, TimeUnit.SECONDS)) {
                    Assert.fail("Should have thrown.");
                }
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
        final int num_of_startups = 2;
        Context context = ApplicationProvider.getApplicationContext();

        for (int i = 0; i < num_of_startups; i++) {
            ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                    JavaScriptSandbox.createConnectedInstanceAsync(context);
            try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
                 JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
                String result = resultFuture.get(5, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
            }
        }
    }

    @Test
    @MediumTest
    public void testHeapSizeAdjustment() throws Throwable {
        final String code = "\"PASS\"";
        final String expected = "PASS";
        final long[] heapSizes = {
                0,
                REASONABLE_HEAP_SIZE,
                REASONABLE_HEAP_SIZE - 1,
                REASONABLE_HEAP_SIZE + 1,
                REASONABLE_HEAP_SIZE + 4095,
                REASONABLE_HEAP_SIZE + 4096,
                REASONABLE_HEAP_SIZE + 65535,
                REASONABLE_HEAP_SIZE + 65536,
                1L << 50,
        };
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_ISOLATE_MAX_HEAP_SIZE));
            for (long heapSize : heapSizes) {
                IsolateStartupParameters isolateStartupParameters = new IsolateStartupParameters();
                isolateStartupParameters.setMaxHeapSizeBytes(heapSize);
                try (JavaScriptIsolate jsIsolate =
                             jsSandbox.createIsolate(isolateStartupParameters)) {
                    ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
                    String result = resultFuture.get(5, TimeUnit.SECONDS);
                    Assert.assertEquals(expected, result);
                } catch (Throwable e) {
                    throw new AssertionError(
                            "Failed to evaluate JavaScript using max heap size setting " + heapSize,
                            e);
                }
            }
        }
    }

    @Test
    @LargeTest
    public void testHeapSizeEnforced() throws Throwable {
        final long maxHeapSize = REASONABLE_HEAP_SIZE;
        // We need to beat the v8 optimizer to ensure it really allocates the required memory. Note
        // that we're allocating an array of elements - not bytes. Filling will ensure that the
        // array is not sparsely allocated.
        final String oomingCode = ""
                + "const array = Array(" + maxHeapSize + ").fill(Math.random(), 0);";
        final String stableCode = "'PASS'";
        final String stableExpected = "PASS";
        final String unresolvedCode = "new Promise((resolve, reject) => {/* never resolve */})";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture1 =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture1.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_ISOLATE_MAX_HEAP_SIZE));
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
            IsolateStartupParameters isolateStartupParameters = new IsolateStartupParameters();
            isolateStartupParameters.setMaxHeapSizeBytes(maxHeapSize);
            try (JavaScriptIsolate jsIsolate1 = jsSandbox.createIsolate(isolateStartupParameters);
                 JavaScriptIsolate jsIsolate2 = jsSandbox.createIsolate()) {
                ListenableFuture<String> earlyUnresolvedResultFuture =
                        jsIsolate1.evaluateJavaScriptAsync(unresolvedCode);
                ListenableFuture<String> earlyResultFuture =
                        jsIsolate1.evaluateJavaScriptAsync(stableCode);
                ListenableFuture<String> oomResultFuture =
                        jsIsolate1.evaluateJavaScriptAsync(oomingCode);

                // Wait for jsIsolate2 to fully initialize before using jsIsolate1.
                jsIsolate2.evaluateJavaScriptAsync(stableCode).get(5, TimeUnit.SECONDS);

                // Check that the heap limit is enforced and that it reports this was the evaluation
                // that exceeded the limit.
                try {
                    // Use a generous timeout for OOM, as it may involve multiple rounds of garbage
                    // collection.
                    oomResultFuture.get(60, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown.");
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof MemoryLimitExceededException)) {
                        throw e;
                    }
                }

                // Check that the previously submitted (but unresolved) promise evaluation reports a
                // crash
                try {
                    earlyUnresolvedResultFuture.get(5, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown.");
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof IsolateTerminatedException)) {
                        throw e;
                    }
                }

                // Check that the previously submitted evaluation which completed before the memory
                // limit was exceeded, but for which we haven't yet gotten the result, returns its
                // result just fine.
                String result = earlyResultFuture.get(5, TimeUnit.SECONDS);
                Assert.assertEquals(stableExpected, result);

                // Check that a totally new evaluation reports a crash
                ListenableFuture<String> lateResultFuture =
                        jsIsolate1.evaluateJavaScriptAsync(stableCode);
                try {
                    lateResultFuture.get(5, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown.");
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof IsolateTerminatedException)) {
                        throw e;
                    }
                }

                // Check that other pre-existing isolates can still be used.
                ListenableFuture<String> otherIsolateResultFuture =
                        jsIsolate2.evaluateJavaScriptAsync(stableCode);
                String otherIsolateResult = otherIsolateResultFuture.get(5, TimeUnit.SECONDS);
                Assert.assertEquals(stableExpected, otherIsolateResult);
            }
        }
    }

    @Test
    @LargeTest
    public void testIsolateCreationAfterCrash() throws Throwable {
        final long maxHeapSize = REASONABLE_HEAP_SIZE;
        // We need to beat the v8 optimizer to ensure it really allocates the required memory. Note
        // that we're allocating an array of elements - not bytes. Filling will ensure that the
        // array is not sparsely allocated.
        final String oomingCode = ""
                + "const array = Array(" + maxHeapSize + ").fill(Math.random(), 0);";
        final String stableCode = "'PASS'";
        final String stableExpected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture1 =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture1.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_ISOLATE_MAX_HEAP_SIZE));
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
            IsolateStartupParameters isolateStartupParameters = new IsolateStartupParameters();
            isolateStartupParameters.setMaxHeapSizeBytes(maxHeapSize);
            try (JavaScriptIsolate jsIsolate1 = jsSandbox.createIsolate(isolateStartupParameters)) {
                ListenableFuture<String> oomResultFuture =
                        jsIsolate1.evaluateJavaScriptAsync(oomingCode);

                // Check that the heap limit is enforced and that it reports this was the evaluation
                // that exceeded the limit.
                try {
                    // Use a generous timeout for OOM, as it may involve multiple rounds of garbage
                    // collection.
                    oomResultFuture.get(60, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown.");
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof MemoryLimitExceededException)) {
                        throw e;
                    }
                }

                // Check that other isolates can still be created and used (without closing
                // jsIsolate1).
                try (JavaScriptIsolate jsIsolate2 =
                             jsSandbox.createIsolate(isolateStartupParameters)) {
                    ListenableFuture<String> resultFuture =
                            jsIsolate2.evaluateJavaScriptAsync(stableCode);
                    String result = resultFuture.get(5, TimeUnit.SECONDS);
                    Assert.assertEquals(stableExpected, result);
                }
            }

            // Check that other isolates can still be created and used (after closing jsIsolate1).
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate(isolateStartupParameters)) {
                ListenableFuture<String> resultFuture =
                        jsIsolate.evaluateJavaScriptAsync(stableCode);
                String result = resultFuture.get(5, TimeUnit.SECONDS);
                Assert.assertEquals(stableExpected, result);
            }
        }

        // Check that the old sandbox with the "crashed" isolate can be torn down and that a new
        // sandbox and isolate can be spun up.
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture2 =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture2.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(stableCode);
            String result = resultFuture.get(5, TimeUnit.SECONDS);
            Assert.assertEquals(stableExpected, result);
        }
    }

    @Test
    @MediumTest
    public void testAsyncPromiseCallbacks() throws Throwable {
        // Unlike testPromiseReturn and testPromiseEvaluationThrow, this test is guaranteed to
        // exercise promises in an asynchronous way, rather than in ways which cause a promise to
        // resolve or reject immediately within the v8::Script::Run call.
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER));
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                // Set up a promise that we can resolve
                final String goodPromiseCode = ""
                        + "let ext_resolve;"
                        + "new Promise((resolve, reject) => {"
                        + " ext_resolve = resolve;"
                        + "})";
                ListenableFuture<String> goodPromiseFuture =
                        jsIsolate.evaluateJavaScriptAsync(goodPromiseCode);

                // Set up a promise that we can reject
                final String badPromiseCode = ""
                        + "let ext_reject;"
                        + "new Promise((resolve, reject) => {"
                        + " ext_reject = reject;"
                        + "})";
                ListenableFuture<String> badPromiseFuture =
                        jsIsolate.evaluateJavaScriptAsync(badPromiseCode);

                // This acts as a barrier to ensure promise code finishes (to the extent of
                // returning the promises) before we ask to evaluate the trigger code - else the
                // potentially async `ext_resolve = resolve` (or `ext_reject = reject`) code might
                // not have been run or queued yet.
                jsIsolate.evaluateJavaScriptAsync("''").get(5, TimeUnit.SECONDS);

                // Trigger the resolve and rejection from another evaluation to ensure the promises
                // are truly asynchronous.
                final String triggerCode = ""
                        + "ext_resolve('I should succeed!');"
                        + "ext_reject(new Error('I should fail!'));"
                        + "'DONE'";
                ListenableFuture<String> triggerFuture =
                        jsIsolate.evaluateJavaScriptAsync(triggerCode);
                String triggerResult = triggerFuture.get(5, TimeUnit.SECONDS);
                Assert.assertEquals("DONE", triggerResult);

                // Check resolve
                String goodPromiseResult = goodPromiseFuture.get(5, TimeUnit.SECONDS);
                Assert.assertEquals("I should succeed!", goodPromiseResult);

                // Check reject
                try {
                    String badPromiseResult = badPromiseFuture.get(5, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown");
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof EvaluationFailedException)) {
                        throw e;
                    }
                    Assert.assertTrue(e.getCause().getMessage().contains("I should fail!"));
                }
            }
        }
    }

    @Test
    @LargeTest
    public void testLargeScriptJsEvaluation() throws Throwable {
        String longString = "a".repeat(2000000);
        final String code = ""
                + "let " + longString + " = 0;"
                + "\"PASS\"";
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT));
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
                String result = resultFuture.get(10, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
            }
        }
    }

    @Test
    @LargeTest
    public void testLargeScriptByteArrayJsEvaluation() throws Throwable {
        final String longString = "a".repeat(2000000);
        final String codeString = ""
                + "let " + longString + " = 0;"
                + "\"PASS\"";
        final byte[] code = codeString.getBytes(StandardCharsets.UTF_8);
        final String expected = "PASS";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT));
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
                String result = resultFuture.get(10, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
            }
        }
    }

    @Test
    @LargeTest
    public void testLargeReturn() throws Throwable {
        final String longString = "a".repeat(2000000);
        final String code = "'a'.repeat(2000000);";
        final String expected = longString;
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT));
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
                String result = resultFuture.get(60, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
            }
        }
    }

    @Test
    @LargeTest
    public void testLargeError() throws Throwable {
        final String longString = "a".repeat(2000000);
        final String code = "throw \"" + longString + "\");";
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT));
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
                ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
                try {
                    resultFuture.get(5, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown.");
                } catch (ExecutionException e) {
                    Assert.assertTrue(e.getCause().getClass().equals(
                            EvaluationFailedException.class));
                    Assert.assertTrue(e.getCause().getMessage().contains(longString));
                }
            }
        }
    }

    @Test
    @MediumTest
    public void testResultSizeEnforced() throws Throwable {
        final int maxSize = 100;
        Context context = ApplicationProvider.getApplicationContext();

        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS)) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT));
            IsolateStartupParameters settings = new IsolateStartupParameters();
            settings.setMaxEvaluationReturnSizeBytes(maxSize);
            try (JavaScriptIsolate jsIsolate = jsSandbox.createIsolate(settings)) {
                // Running code that returns greater than `maxSize` number of bytes should throw.
                final String greaterThanMaxSizeCode = ""
                        + "'a'.repeat(" + (maxSize + 1) + ");";
                ListenableFuture<String> greaterThanMaxSizeResultFuture =
                        jsIsolate.evaluateJavaScriptAsync(greaterThanMaxSizeCode);
                try {
                    greaterThanMaxSizeResultFuture.get(5, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown.");
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof EvaluationResultSizeLimitExceededException)) {
                        throw e;
                    }
                }

                // Running code that returns `maxSize` number of bytes should not throw.
                final String maxSizeCode = ""
                        + "'a'.repeat(" + maxSize + ");";
                final String maxSizeExpected = "a".repeat(maxSize);
                ListenableFuture<String> maxSizeResultFuture =
                        jsIsolate.evaluateJavaScriptAsync(maxSizeCode);
                String maxSizeResult = maxSizeResultFuture.get(5, TimeUnit.SECONDS);
                Assert.assertEquals(maxSizeExpected, maxSizeResult);

                // Running code that returns less than `maxSize` number of bytes should not throw.
                final String lessThanMaxSizeCode = ""
                        + "'a'.repeat(" + (maxSize - 1) + ");";
                final String lessThanMaxSizeExpected = "a".repeat(maxSize - 1);
                ListenableFuture<String> lessThanMaxSizeResultFuture =
                        jsIsolate.evaluateJavaScriptAsync(lessThanMaxSizeCode);
                String lessThanMaxSizeResult = lessThanMaxSizeResultFuture.get(5,
                        TimeUnit.SECONDS);
                Assert.assertEquals(lessThanMaxSizeExpected, lessThanMaxSizeResult);
            }
        }
    }

    @Test
    @LargeTest
    public void testConsoleLogging() throws Throwable {
        final class LoggingJavaScriptConsoleCallback implements JavaScriptConsoleCallback {
            private final Object mLock = new Object();
            @GuardedBy("mLock")
            private final StringBuilder mMessages = new StringBuilder();

            public static final String CLEAR_MARKER = "(console.clear() called)\n";
            // This is required for synchronization between the instrumentation thread and the UI
            // thread.
            public CountDownLatch latch;

            LoggingJavaScriptConsoleCallback(int numberOfCalls) {
                latch = new CountDownLatch(numberOfCalls);
            }

            @Override
            public void onConsoleMessage(
                    @NonNull JavaScriptConsoleCallback.ConsoleMessage message) {
                synchronized (mLock) {
                    mMessages.append(message.toString()).append("\n");
                }
                latch.countDown();
            }

            @Override
            public void onConsoleClear() {
                synchronized (mLock) {
                    mMessages.append(CLEAR_MARKER);
                }
                latch.countDown();
            }

            public String messages() {
                synchronized (mLock) {
                    return mMessages.toString();
                }
            }

            public void resetLatch(int count) {
                latch = new CountDownLatch(count);
            }
        }

        final String code = ""
                + "function a() {b();}\n"
                + "function b() {c();}\n"
                + "function c() {console.trace('I am a trace message!');}\n"
                + "console.log('I am a log message!');\n"
                + "console.debug('I am a debug message!');\n"
                + "console.info('I am an info message!');\n"
                + "console.error('I am an error message!');\n"
                + "console.warn('I am a warn message!');\n"
                + "console.assert(false, 'I am an assert message!');\n"
                + "console.log({'I am': [1, 'complex', {}]});\n"
                + "a();\n"
                + "console.count('I am counting');\n"
                + "console.count('I am counting');\n"
                + "console.clear();\n"
                + "\"PASS\"";
        final String expected = "PASS";
        final String expectedLog = ""
                + "L <expression>:4:9: I am a log message!\n"
                + "D <expression>:5:9: I am a debug message!\n"
                + "I <expression>:6:9: I am an info message!\n"
                + "E <expression>:7:9: I am an error message!\n"
                + "W <expression>:8:9: I am a warn message!\n"
                + "E <expression>:9:9: I am an assert message!\n"
                + "L <expression>:10:9: [object Object]\n"
                + "I <expression>:3:23: I am a trace message!\n"
                + "D <expression>:12:9: I am counting: 1\n"
                + "D <expression>:13:9: I am counting: 2\n"
                + LoggingJavaScriptConsoleCallback.CLEAR_MARKER;
        final int numOfLogs = 11;
        final Context context = ApplicationProvider.getApplicationContext();

        final ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING));
            final LoggingJavaScriptConsoleCallback client1 =
                    new LoggingJavaScriptConsoleCallback(numOfLogs);
            final LoggingJavaScriptConsoleCallback client2 =
                    new LoggingJavaScriptConsoleCallback(numOfLogs);
            // Test logging does not crash when no client attached
            // (There may be no inspector)
            {
                final ListenableFuture<String> resultFuture =
                        jsIsolate.evaluateJavaScriptAsync(code);
                final String result = resultFuture.get(5, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
            }
            // Test logging works when client attached
            // (This may spin up an inspector)
            {
                jsIsolate.setConsoleCallback(client1);
                final ListenableFuture<String> resultFuture =
                        jsIsolate.evaluateJavaScriptAsync(code);
                final String result = resultFuture.get(5, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
                Assert.assertTrue(client1.latch.await(2, TimeUnit.SECONDS));
                Assert.assertEquals(expectedLog, client1.messages());
            }
            // Test client can be replaced
            // (This may retain the same inspector)
            {
                jsIsolate.setConsoleCallback(client2);
                final ListenableFuture<String> resultFuture =
                        jsIsolate.evaluateJavaScriptAsync(code);
                final String result = resultFuture.get(5, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
                Assert.assertTrue(client2.latch.await(2, TimeUnit.SECONDS));
                Assert.assertEquals(expectedLog, client2.messages());
            }
            // Test client can be nullified/disabled
            // (This may tear down the inspector)
            {
                jsIsolate.clearConsoleCallback();
                final ListenableFuture<String> resultFuture =
                        jsIsolate.evaluateJavaScriptAsync(code);
                final String result = resultFuture.get(5, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
            }
            // Ensure console messaging can be re-enabled (on an existing client)
            // (This may spin up a new inspector)
            {
                client1.resetLatch(numOfLogs);
                jsIsolate.setConsoleCallback(client1);
                final ListenableFuture<String> resultFuture =
                        jsIsolate.evaluateJavaScriptAsync(code);
                final String result = resultFuture.get(5, TimeUnit.SECONDS);

                Assert.assertEquals(expected, result);
                Assert.assertTrue(client1.latch.await(2, TimeUnit.SECONDS));
                Assert.assertEquals(expectedLog + expectedLog, client1.messages());
            }
            // Ensure client1 and client2 hasn't received anything additional
            {
                Thread.sleep(1000);
                Assert.assertEquals(expectedLog + expectedLog, client1.messages());
                Assert.assertEquals(expectedLog, client2.messages());
            }
        }
    }

    @Test
    @MediumTest
    public void testConsoleCallbackCanCallService() throws Throwable {
        // This checks that there is nothing intrinsically wrong with calling service APIs from a
        // console client. Note that, in theory, Binder will reuse the same threads if code recurses
        // back between processes, which may make subtle (dead)locking bugs harder to detect.
        //
        // The console client handler for console.clear will trigger the main evaluation to resolve
        // via a secondary evaluation.
        final String code = ""
                + "var checkIn = null;\n"
                + "console.clear();\n"
                + "new Promise((resolve) => {\n"
                + "  checkIn = resolve;\n"
                + "})\n";
        final String callbackCode = ""
                + "checkIn('PASS');\n"
                + "'(this callback result is ignored)'\n";
        final String expected = "PASS";
        final Context context = ApplicationProvider.getApplicationContext();

        final ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(context);
        try (JavaScriptSandbox jsSandbox = jsSandboxFuture.get(5, TimeUnit.SECONDS);
             JavaScriptIsolate jsIsolate = jsSandbox.createIsolate()) {
            Assume.assumeTrue(jsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING));
            Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));

            CountDownLatch latch = new CountDownLatch(1);
            jsIsolate.setConsoleCallback(new JavaScriptConsoleCallback() {
                @Override
                public void onConsoleMessage(@NonNull ConsoleMessage message) {}

                @Override
                public void onConsoleClear() {
                    jsIsolate.evaluateJavaScriptAsync(callbackCode);
                    latch.countDown();
                }
            });
            final ListenableFuture<String> resultFuture = jsIsolate.evaluateJavaScriptAsync(code);
            // Note: the main executor is on a different thread to the instrumentation thread, so
            // blocking here will not block the console callback.
            final String result = resultFuture.get(5, TimeUnit.SECONDS);
            Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
            Assert.assertEquals(expected, result);
        }
    }
}
