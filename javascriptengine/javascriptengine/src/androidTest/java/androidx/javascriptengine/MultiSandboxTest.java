/*
 * Copyright 2026 The Android Open Source Project
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
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Tests for multi-sandbox functionality.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
public class MultiSandboxTest {
    private static final int TIMEOUT_SECONDS = 5;

    private final Map<String, ListenableFuture<JavaScriptSandbox>> mSandboxes = new HashMap<>();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        Assume.assumeTrue(JavaScriptSandbox.isSupported());
    }

    @After
    public void tearDown() throws Throwable {
        // Tests should assert that they've closed everything before teardown.
        // This is just a fallback in case a test fails.
        for (ListenableFuture<JavaScriptSandbox> jsSandboxFuture : mSandboxes.values()) {
            try {
                jsSandboxFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).close();
            } catch (ExecutionException e) {
                // Ignore. The sandbox didn't start.
            }
        }
        mSandboxes.clear();
    }

    private void storeSandbox(@NonNull String name,
            @NonNull ListenableFuture<JavaScriptSandbox> jsSandboxFuture) throws Throwable {
        if (mSandboxes.containsKey(name)) {
            // Best effort pre-crash cleanup
            jsSandboxFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).close();
            Assert.fail("A sandbox with name '" + name + "' is already being tracked.");
        }
        mSandboxes.put(name, jsSandboxFuture);
    }

    private void spinUpLegacy(@NonNull String name) throws Throwable {
        storeSandbox(name, JavaScriptSandbox.createConnectedInstanceAsync(mContext));
    }

    private void spinUpShared(@NonNull String name) throws Throwable {
        storeSandbox(name, JavaScriptSandbox.createSharedSandboxAsync(mContext));
    }

    private void spinUpDedicated(@NonNull String name) throws Throwable {
        storeSandbox(name, JavaScriptSandbox.createDedicatedSandboxAsync(mContext));
    }

    private JavaScriptSandbox awaitSandbox(@NonNull String name) throws Throwable {
        return mSandboxes.get(name).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void assertSandboxAlive(@NonNull String name) throws Throwable {
        JavaScriptSandbox jsSandbox = awaitSandbox(name);
        try (JavaScriptIsolate isolate = jsSandbox.createIsolate()) {
            String result = isolate.evaluateJavaScriptAsync("'ping'").get(TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
            Assert.assertEquals(result, "ping");
        }
    }

    private void killSandbox(@NonNull String name) throws Throwable {
        awaitSandbox(name).killDueToException(new RuntimeException("test kill"));
    }

    private void assertSandboxDead(@NonNull String name) throws Throwable {
        try {
            JavaScriptSandbox jsSandbox = awaitSandbox(name);
            try (JavaScriptIsolate isolate = jsSandbox.createIsolate()) {
                isolate.evaluateJavaScriptAsync("'ping'").get(TIMEOUT_SECONDS,
                        TimeUnit.SECONDS);
            }
            Assert.fail("Should have thrown.");
        } catch (ExecutionException e) {
            // Expected. There may not be a specific cause.
        }
    }

    private void spinDownSandbox(@NonNull String name) throws Throwable {
        mSandboxes.remove(name).get(TIMEOUT_SECONDS, TimeUnit.SECONDS).close();
    }

    private void spinDownMaybeDeadSandbox(@NonNull String name) throws Throwable {
        try {
            mSandboxes.remove(name).get(TIMEOUT_SECONDS, TimeUnit.SECONDS).close();
        } catch (ExecutionException e) {
            // The sandbox service died before the future completed. Ignore it.
        }
    }

    private void assertAllSandboxesClosed() {
        Assert.assertTrue(mSandboxes.isEmpty());
    }

    @Test
    public void testLegacySandbox() throws Throwable {
        spinUpLegacy("legacy");
        assertSandboxAlive("legacy");
        spinDownSandbox("legacy");

        assertAllSandboxesClosed();
    }

    @Test
    public void testSingleSharedSandbox() throws Throwable {
        spinUpShared("shared");
        assertSandboxAlive("shared");
        spinDownSandbox("shared");

        assertAllSandboxesClosed();
    }

    @Test
    public void testTwoSequentialSharedSandboxes() throws Throwable {
        spinUpShared("shared/1");
        assertSandboxAlive("shared/1");
        spinDownSandbox("shared/1");

        spinUpShared("shared/2");
        assertSandboxAlive("shared/2");
        spinDownSandbox("shared/2");

        assertAllSandboxesClosed();
    }

    @Test
    public void testTwoConcurrentSharedSandboxes() throws Throwable {
        spinUpShared("shared/1");
        assertSandboxAlive("shared/1");
        spinUpShared("shared/2");
        assertSandboxAlive("shared/1");
        assertSandboxAlive("shared/2");
        spinDownSandbox("shared/1");
        assertSandboxAlive("shared/2");
        spinDownSandbox("shared/2");

        assertAllSandboxesClosed();
    }

    @Test
    public void testManyConcurrentSharedSandboxes() throws Throwable {
        // So long as the service is already up, it's cheap to create a new shared
        // sandbox.
        int numSandboxes = 100;
        for (int i = 0; i < numSandboxes; i++) {
            spinUpShared("shared/" + i);
        }

        for (int i = 0; i < numSandboxes; i++) {
            assertSandboxAlive("shared/" + i);
        }

        for (int i = 0; i < numSandboxes; i++) {
            spinDownSandbox("shared/" + i);
        }

        assertAllSandboxesClosed();
    }

    @Test
    public void testManySequentialSharedSandboxes() throws Throwable {
        // This test tears down and reconstructs the service each time, which can be quite expensive
        // (CPU time). Don't do it too many times.
        int numSandboxes = 32;
        for (int i = 0; i < numSandboxes; i++) {
            spinUpShared("shared/" + i);
            assertSandboxAlive("shared/" + i);
            spinDownSandbox("shared/" + i);
        }

        assertAllSandboxesClosed();
    }

    @Test
    public void testSharedSandboxCloseCleansUpIsolates() throws Throwable {
        // Is this test flaky?
        // There technically isn't anything that waits for an isolate to be cleaned up when
        // an isolate is closed. This could mean that we end up creating numSandboxes before
        // anything is actually fully cleaned up. But, perhaps this isn't a problem in practice?
        int numSandboxes = 100;
        int bytesPerIsolate = 100_000_000;
        spinUpShared("shared/keep-alive");
        assertSandboxAlive("shared/keep-alive");

        for (int i = 0; i < numSandboxes; i++) {
            spinUpShared("shared/" + i);
            // We deliberately do not close this isolate. We want to check that closing a sandbox
            // does the cleanup.
            JavaScriptIsolate isolate = awaitSandbox("shared/" + i).createIsolate();
            String result = isolate.evaluateJavaScriptAsync(
                            "let x = new Uint8Array(" + bytesPerIsolate + ").fill(1); 'ping'")
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertEquals(result, "ping");
            spinDownSandbox("shared/" + i);
        }
        assertSandboxAlive("shared/keep-alive");
        spinDownSandbox("shared/keep-alive");

        assertAllSandboxesClosed();
    }

    @Test
    public void testDedicatedSandbox() throws Throwable {
        spinUpDedicated("dedicated");
        assertSandboxAlive("dedicated");
        spinDownSandbox("dedicated");

        assertAllSandboxesClosed();
    }

    @Test
    public void testConcurrentDedicatedSandboxes() throws Throwable {
        spinUpDedicated("dedicated/1");
        assertSandboxAlive("dedicated/1");
        spinUpDedicated("dedicated/2");
        assertSandboxAlive("dedicated/1");
        assertSandboxAlive("dedicated/2");
        spinDownSandbox("dedicated/1");
        assertSandboxAlive("dedicated/2");
        spinDownSandbox("dedicated/2");

        assertAllSandboxesClosed();
    }

    @Test
    public void testSharedSandboxDeath_allSharedSandboxesAffected() throws Throwable {
        spinUpDedicated("dedicated/1");
        spinUpShared("shared/1");
        spinUpShared("shared/2");

        killSandbox("shared/1");

        assertSandboxDead("shared/1");
        assertSandboxDead("shared/2");
        assertSandboxAlive("dedicated/1");

        spinDownSandbox("dedicated/1");
        spinDownMaybeDeadSandbox("shared/1");
        spinDownMaybeDeadSandbox("shared/2");

        assertAllSandboxesClosed();
    }

    @Test
    public void testSharedSandboxDead_doesNotAffectSandboxesOnNewerService() throws Throwable {
        spinUpShared("shared/1");
        JavaScriptSandbox shared1 = awaitSandbox("shared/1");
        // Should deactivate the current shared instance, so the next sandbox gets a fresh one.
        spinDownSandbox("shared/1");

        // New sandboxes should use a new process and be unaffected by any kills on "shared/1".
        spinUpShared("shared/2");
        awaitSandbox("shared/2");
        shared1.killDueToException(new RuntimeException("test kill"));
        spinUpShared("shared/3");
        assertSandboxAlive("shared/2");
        assertSandboxAlive("shared/3");

        spinDownSandbox("shared/2");
        spinDownSandbox("shared/3");
    }

    @Test
    public void testDedicatedSandboxDeath_otherSandboxesUnaffected() throws Throwable {
        spinUpDedicated("dedicated/1");
        spinUpDedicated("dedicated/2");
        spinUpShared("shared/1");
        spinUpShared("shared/2");

        killSandbox("dedicated/1");

        assertSandboxDead("dedicated/1");
        assertSandboxAlive("dedicated/2");
        assertSandboxAlive("shared/1");
        assertSandboxAlive("shared/2");

        spinDownMaybeDeadSandbox("dedicated/1");
        spinDownSandbox("dedicated/2");
        spinDownSandbox("shared/1");
        spinDownSandbox("shared/2");

        assertAllSandboxesClosed();
    }

    @Test
    public void testLegacySandboxDeath_canCreateNewServiceAfterClose() throws Throwable {
        spinUpLegacy("legacy/1");

        JavaScriptSandbox legacySandbox1 = awaitSandbox("legacy/1");
        legacySandbox1.killDueToException(new RuntimeException("test kill 1"));

        // Legacy sandbox must be explicitly closed before creating another one.
        ListenableFuture<JavaScriptSandbox> concurrentLegacySandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(mContext);
        try {
            JavaScriptSandbox unexpectedSandbox =
                    concurrentLegacySandboxFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            unexpectedSandbox.close();
            Assert.fail("Should have thrown ExecutionException");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof RuntimeException)) {
                throw e;
            }
            Assert.assertTrue(e.getCause().getMessage().contains(
                    "Cannot create two concurrent legacy sandboxes. Close the existing "
                            + "JavaScriptSandbox first."));
        }

        spinDownMaybeDeadSandbox("legacy/1");

        // Should work immediately once first legacy sandbox is closed.
        spinUpLegacy("legacy/2");

        // killDueToException is permitted even if the sandbox is closed, but this should not do
        // anything to the new sandbox.
        legacySandbox1.killDueToException(new RuntimeException("test kill 2"));
        assertSandboxAlive("legacy/2");

        spinDownSandbox("legacy/2");

        assertAllSandboxesClosed();
    }

    @Test
    public void testSharedServiceDeath_canCreateNewServiceWithoutClosingOldOne() throws Throwable {
        spinUpShared("shared/1");
        spinUpShared("shared/2");

        killSandbox("shared/1");

        // Should work immediately, even if first sandbox hasn't been closed yet.
        spinUpShared("shared/3");
        assertSandboxDead("shared/1");
        assertSandboxDead("shared/2");
        assertSandboxAlive("shared/3");

        // A delayed kill on an old sandbox service shouldn't affect the new service.
        killSandbox("shared/1");
        assertSandboxAlive("shared/3");

        spinDownMaybeDeadSandbox("shared/1");
        spinDownMaybeDeadSandbox("shared/2");
        spinDownSandbox("shared/3");

        assertAllSandboxesClosed();
    }

    @Test
    public void testSharedSandboxClose_invalidatesOnlyItsIsolates() throws Throwable {
        spinUpShared("keep-alive");
        try (JavaScriptIsolate keepAliveIsolate = awaitSandbox("keep-alive").createIsolate()) {
            spinUpShared("ephemeral");
            try (JavaScriptIsolate ephemeralIsolate = awaitSandbox("ephemeral").createIsolate()) {
                spinDownSandbox("ephemeral");
                ListenableFuture<String> resultFuture = ephemeralIsolate.evaluateJavaScriptAsync(
                        "'ping'");
                try {
                    resultFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    Assert.fail("Should have thrown ExecutionException");
                } catch (ExecutionException e) {
                    Assert.assertTrue(e.getCause() instanceof IsolateTerminatedException);
                }
            }
            String result = keepAliveIsolate.evaluateJavaScriptAsync("'ping'").get(TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
            Assert.assertEquals(result, "ping");
        }
        spinDownSandbox("keep-alive");

        assertAllSandboxesClosed();
    }

    @Test
    public void testAllConcurrentSandboxTypes() throws Throwable {
        spinUpLegacy("legacy");
        assertSandboxAlive("legacy");

        spinUpDedicated("dedicated");
        assertSandboxAlive("dedicated");
        assertSandboxAlive("legacy");

        spinUpShared("shared/1");
        assertSandboxAlive("shared/1");
        spinUpShared("shared/2");
        assertSandboxAlive("shared/1");
        assertSandboxAlive("shared/2");
        spinDownSandbox("shared/2");
        assertSandboxAlive("shared/1");
        spinUpShared("shared/3");
        assertSandboxAlive("shared/1");
        assertSandboxAlive("shared/3");
        spinDownSandbox("shared/1");
        spinDownSandbox("shared/3");

        spinUpShared("shared/4");
        assertSandboxAlive("shared/4");
        spinDownSandbox("shared/4");

        assertSandboxAlive("legacy");
        assertSandboxAlive("dedicated");
        spinDownSandbox("dedicated");

        assertSandboxAlive("legacy");
        spinDownSandbox("legacy");

        assertAllSandboxesClosed();
    }

    @Test
    public void testIsShared_returnsCorrectValue() throws Throwable {
        spinUpShared("shared");
        spinUpLegacy("legacy");
        spinUpDedicated("dedicated");
        Assert.assertTrue(awaitSandbox("shared").isShared());
        Assert.assertFalse(awaitSandbox("legacy").isShared());
        Assert.assertFalse(awaitSandbox("dedicated").isShared());

        killSandbox("shared");
        killSandbox("legacy");
        killSandbox("dedicated");
        assertSandboxDead("shared");
        assertSandboxDead("legacy");
        assertSandboxDead("dedicated");
        Assert.assertTrue(awaitSandbox("shared").isShared());
        Assert.assertFalse(awaitSandbox("legacy").isShared());
        Assert.assertFalse(awaitSandbox("dedicated").isShared());

        awaitSandbox("shared").close();
        awaitSandbox("legacy").close();
        awaitSandbox("dedicated").close();
        Assert.assertTrue(awaitSandbox("shared").isShared());
        Assert.assertFalse(awaitSandbox("legacy").isShared());
        Assert.assertFalse(awaitSandbox("dedicated").isShared());

        spinDownSandbox("shared");
        spinDownSandbox("legacy");
        spinDownSandbox("dedicated");

        assertAllSandboxesClosed();
    }
}
