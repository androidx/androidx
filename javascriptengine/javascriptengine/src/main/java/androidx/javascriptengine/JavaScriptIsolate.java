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

import android.content.res.AssetFileDescriptor;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolate;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateClient;

import java.util.Objects;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.GuardedBy;

/**
 * Environment within a {@link JavaScriptSandbox} where JavaScript is executed.
 * <p>
 * A single {@link JavaScriptSandbox} process can contain any number of {@link JavaScriptIsolate}
 * instances where JS can be evaluated independently and in parallel.
 * <p>
 * Each isolate has its own state and JS global object,
 * and cannot interact with any other isolate through JS APIs. There is only a <em>moderate</em>
 * security boundary between isolates in a single {@link JavaScriptSandbox}. If the code in one
 * {@link JavaScriptIsolate} is able to compromise the security of the JS engine then it may be
 * able to observe or manipulate other isolates, since they run in the same process. For strong
 * isolation multiple {@link JavaScriptSandbox} processes should be used, but it is not supported
 * at the moment. Please find the feature request <a href="https://crbug.com/1349860">here</a>.
 * <p>
 * This class is thread-safe.
 */
public final class JavaScriptIsolate implements AutoCloseable {
    private static final String TAG = "JavaScriptIsolate";
    private final Object mLock = new Object();
    private final CloseGuardHelper mGuard = CloseGuardHelper.create();

    @NonNull
    final JavaScriptSandbox mJsSandbox;

    @GuardedBy("mLock")
    @NonNull
    private IsolateState mIsolateState;

    private final class JsSandboxIsolateClient extends IJsSandboxIsolateClient.Stub {
        JsSandboxIsolateClient() {}

        @Override
        public void onTerminated(int status, String message) {
            final long identity = Binder.clearCallingIdentity();
            try {
                // If we're already closed, this will do nothing
                maybeSetIsolateDead(new TerminationInfo(status, message));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    @NonNull
    static JavaScriptIsolate create(@NonNull JavaScriptSandbox sandbox,
            IsolateStartupParameters settings) throws RemoteException {
        final JavaScriptIsolate isolate = new JavaScriptIsolate(sandbox);
        isolate.initialize(settings);
        isolate.mGuard.open("close");
        return isolate;
    }

    @NonNull
    static JavaScriptIsolate createDead(@NonNull JavaScriptSandbox sandbox,
            @NonNull String message) {
        final JavaScriptIsolate isolate = new JavaScriptIsolate(sandbox);
        final TerminationInfo terminationInfo =
                new TerminationInfo(TerminationInfo.STATUS_SANDBOX_DEAD, message);
        synchronized (isolate.mLock) {
            isolate.mIsolateState = new EnvironmentDeadState(terminationInfo);
        }
        isolate.mGuard.open("close");
        return isolate;
    }

    private JavaScriptIsolate(@NonNull JavaScriptSandbox sandbox) {
        mJsSandbox = sandbox;
        synchronized (mLock) {
            mIsolateState = new IsolateClosedState("isolate not initialized");
        }
    }

    // Create an isolate on the service side and complete initialization.
    // This is done outside of the constructor to avoid leaking a partially constructed
    // JavaScriptIsolate to the service (which would complicate thread-safety).
    private void initialize(@NonNull IsolateStartupParameters settings) throws RemoteException {
        synchronized (mLock) {
            final IJsSandboxIsolateClient instanceCallback;
            if (mJsSandbox.isFeatureSupported(
                    JavaScriptSandbox.JS_FEATURE_ISOLATE_CLIENT)) {
                instanceCallback = new JsSandboxIsolateClient();
            } else {
                instanceCallback = null;
            }
            IJsSandboxIsolate jsIsolateStub = mJsSandbox.createIsolateOnService(settings,
                    instanceCallback);
            mIsolateState = new IsolateUsableState(this, jsIsolateStub,
                    settings.getMaxEvaluationReturnSizeBytes());
        }
    }

    /**
     * Changes the state to denote that the isolate is dead.
     * <p>
     * {@link IsolateClosedState} takes precedence so it will not change state if the current state
     * is {@link IsolateClosedState}.
     * <p>
     * If the isolate is already dead, the existing dead state is preserved.
     *
     * @return true iff the state was changed to a new EnvironmentDeadState
     */
    boolean maybeSetIsolateDead(@NonNull TerminationInfo terminationInfo) {
        synchronized (mLock) {
            if (terminationInfo.getStatus() == TerminationInfo.STATUS_MEMORY_LIMIT_EXCEEDED) {
                Log.e(TAG, "isolate exceeded its heap memory limit - killing sandbox");
                mJsSandbox.kill();
            }
            final IsolateState oldState = mIsolateState;
            if (oldState.canDie()) {
                mIsolateState = new EnvironmentDeadState(terminationInfo);
                oldState.onDied(terminationInfo);
                return true;
            }
        }
        return false;
    }

    /**
     * Changes the state to denote that the sandbox is dead.
     * <p>
     * See {@link #maybeSetIsolateDead(TerminationInfo)} for additional information.
     *
     * @return the generated termination info if it was set, or null if the state did not change.
     */
    @Nullable
    TerminationInfo maybeSetSandboxDead() {
        synchronized (mLock) {
            final TerminationInfo terminationInfo =
                    new TerminationInfo(TerminationInfo.STATUS_SANDBOX_DEAD, "sandbox dead");
            if (maybeSetIsolateDead(terminationInfo)) {
                return terminationInfo;
            } else {
                return null;
            }
        }
    }

    /**
     * Evaluates the given JavaScript code and returns the result.
     * <p>
     * There are 3 possible behaviors based on the output of the expression:
     * <ul>
     *   <li><strong>If the JS expression returns a JS String</strong>, then the Java Future
     * resolves to Java String.</li>
     *   <li><strong>If the JS expression returns a JS Promise</strong>,
     * and if {@link JavaScriptSandbox#isFeatureSupported(String)} for
     * {@link JavaScriptSandbox#JS_FEATURE_PROMISE_RETURN} returns {@code true}, Java Future
     * resolves to Java String once the promise resolves. If it returns {@code false}, then the
     * Future resolves to an empty string.</li>
     *   <li><strong>If the JS expression returns another data type</strong>, then Java Future
     * resolves to empty Java String.</li>
     * </ul>
     * The environment uses a single JS global object for all the calls to
     * evaluateJavaScriptAsync(String) and {@link #provideNamedData(String, byte[])} methods.
     * These calls are queued up and are run one at a time in sequence, using the single JS
     * environment for the isolate. The global variables set by one evaluation are visible for
     * later evaluations. This is similar to adding multiple {@code <script>} tags in HTML. The
     * behavior is also similar to
     * {@link android.webkit.WebView#evaluateJavascript(String, android.webkit.ValueCallback)}.
     * <p>
     * If {@link JavaScriptSandbox#isFeatureSupported(String)} for
     * {@link JavaScriptSandbox#JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT} returns {@code
     * false},
     * the size of the expression to be evaluated and the return/error value is limited by the
     * binder transaction limit ({@link android.os.TransactionTooLargeException}). If it returns
     * {@code true}, they are not limited by the binder
     * transaction limit but are bound by
     * {@link IsolateStartupParameters#setMaxEvaluationReturnSizeBytes(int)} with a default size
     * of {@link IsolateStartupParameters#DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES}.
     * <p>
     * Do not use this method to transfer raw binary data. Scripts or results containing unpaired
     * surrogate code units are not supported.
     *
     * @param code JavaScript code to evaluate. The script should return a JavaScript String or,
     *             alternatively, a Promise that will resolve to a String if
     *             {@link JavaScriptSandbox#JS_FEATURE_PROMISE_RETURN} is supported.
     * @return Future that evaluates to the result String of the evaluation or exceptions (see
     * {@link JavaScriptException} and subclasses) if there is an error.
     */
    @NonNull
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull String code) {
        Objects.requireNonNull(code);
        synchronized (mLock) {
            return mIsolateState.evaluateJavaScriptAsync(code);
        }
    }

    /**
     * Reads and evaluates the JavaScript code in the file described by the given
     * AssetFileDescriptor.
     * <p>
     * Please refer to the documentation of {@link #evaluateJavaScriptAsync(String)} as the
     * behavior of this method is similar other than for the input type.
     * <p>
     * This API exposes the underlying file to the service. In case the service process is
     * compromised for unforeseen reasons, it might be able to read from the {@code
     * AssetFileDescriptor} beyond the given length and offset.  This API does <strong> not
     * </strong> close the given {@code AssetFileDescriptor}.
     * <p>
     * <strong>Note: The underlying file must be UTF-8 encoded.</strong>
     * <p>
     * This overload is useful when the source of the data is easily readable as a
     * {@code AssetFileDescriptor}, e.g. an asset or raw resource.
     *
     * @param afd An {@code AssetFileDescriptor} for a file containing UTF-8 encoded JavaScript
     *            code that is evaluated. Returns a String or a Promise of a String in case
     *            {@link JavaScriptSandbox#JS_FEATURE_PROMISE_RETURN} is supported
     * @return Future that evaluates to the result String of the evaluation or exceptions (see
     * {@link JavaScriptException} and subclasses) if there is an error
     */
    @SuppressWarnings("NullAway")
    @NonNull
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_EVALUATE_FROM_FD,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull AssetFileDescriptor afd) {
        Objects.requireNonNull(afd);
        synchronized (mLock) {
            return mIsolateState.evaluateJavaScriptAsync(afd);
        }
    }

    /**
     * Reads and evaluates the JavaScript code in the file described by the given
     * {@code ParcelFileDescriptor}.
     * <p>
     * Please refer to the documentation of {@link #evaluateJavaScriptAsync(String)} as the
     * behavior of this method is similar other than for the input type.
     * <p>
     * This API exposes the underlying file to the service. In case the service process is
     * compromised for unforeseen reasons, it might be able to read from the {@code
     * ParcelFileDescriptor} beyond the given length and offset. This API does <strong> not
     * </strong> close the given {@code ParcelFileDescriptor}.
     * <p>
     * <strong>Note: The underlying file must be UTF-8 encoded.</strong>
     * <p>
     * This overload is useful when the source of the data is easily readable as a
     * {@code ParcelFileDescriptor}, e.g. a file from shared memory or the app's data directory.
     *
     * @param pfd A {@code ParcelFileDescriptor} for a file containing UTF-8 encoded JavaScript
     *            code that is evaluated. Returns a String or a Promise of a String in case
     *             {@link JavaScriptSandbox#JS_FEATURE_PROMISE_RETURN} is supported
     * @return Future that evaluates to the result String of the evaluation or exceptions (see
     * {@link JavaScriptException} and subclasses) if there is an error
     */
    @SuppressWarnings("NullAway")
    @NonNull
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_EVALUATE_FROM_FD,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull ParcelFileDescriptor pfd) {
        Objects.requireNonNull(pfd);
        synchronized (mLock) {
            return mIsolateState.evaluateJavaScriptAsync(pfd);
        }
    }

    /**
     * Closes the {@link JavaScriptIsolate} object and renders it unusable.
     * <p>
     * Once closed, no more method calls should be made. Pending evaluations will reject with
     * an {@link IsolateTerminatedException} immediately.
     * <p>
     * If {@link JavaScriptSandbox#isFeatureSupported(String)} is {@code true} for {@link
     * JavaScriptSandbox#JS_FEATURE_ISOLATE_TERMINATION}, then any pending evaluations are
     * terminated. If it is {@code false}, the isolate will not get cleaned
     * up until the pending evaluations have run to completion and will consume resources until
     * then.
     * <p>
     * Closing an isolate via this method does not wait on the isolate to clean up. Resources
     * held by the isolate may remain in use for a duration after this method returns.
     */
    @Override
    public void close() {
        closeWithDescription("isolate closed");
    }

    void closeWithDescription(@NonNull String description) {
        synchronized (mLock) {
            mIsolateState.close();
            mIsolateState = new IsolateClosedState(description);
        }
        // Do not hold mLock whilst calling into JavaScriptSandbox, as JavaScriptSandbox also has
        // its own lock and may want to call into JavaScriptIsolate from another thread.
        mJsSandbox.removeFromIsolateSet(this);
        mGuard.close();
    }

    /**
     * Provides a byte array for consumption from the JavaScript environment.
     * <p>
     * This method provides an efficient way to pass in data from Java into the JavaScript
     * environment which can be referred to from JavaScript. This is more efficient than including
     * data in the JS expression, and allows large data to be sent.
     * <p>
     * This data can be consumed in the JS environment using {@code
     * android.consumeNamedDataAsArrayBuffer(String)} by referring to the data with the name that
     * was used when calling this method. This is a one-time transfer and the calls should be
     * paired.
     * <p>
     * A single name can only be used once in a particular {@link JavaScriptIsolate}.
     * Clients can generate unique names for each call if they
     * need to use this method multiple times. The same name should be included into the JS code.
     * <p>
     * This API can be used to pass a WASM module into the JS
     * environment for compilation if {@link JavaScriptSandbox#isFeatureSupported(String)} returns
     * {@code true} for {@link JavaScriptSandbox#JS_FEATURE_WASM_COMPILATION}.
     * <br>
     * In Java,
     * <pre>
     *     jsIsolate.provideNamedData("id-1", byteArray);
     * </pre>
     * In JS,
     * <pre>
     *     android.consumeNamedDataAsArrayBuffer("id-1").then((value) => {
     *       return WebAssembly.compile(value).then((module) => {
     *          ...
     *       });
     *     });
     * </pre>
     * <p>
     * The environment uses a single JS global object for all the calls to {@link
     * #evaluateJavaScriptAsync(String)} and provideNamedData(String, byte[]) methods.
     * <p>
     * This method should only be called if
     * {@link JavaScriptSandbox#isFeatureSupported(String)}
     * returns true for {@link JavaScriptSandbox#JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER}.
     *
     * @param name       Identifier for the data that is passed, the same identifier should be used
     *                   in the JavaScript environment to refer to the data
     * @param inputBytes Bytes to be passed into the JavaScript environment. This array must not be
     *                   modified until the JavaScript promise returned by
     *                   consumeNamedDataAsArrayBuffer has resolved (or rejected).
     * @return {@code true} on success, {@code false} if the name has already been used before,
     * in which case the client should use an unused name
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public boolean provideNamedData(@NonNull String name, @NonNull byte[] inputBytes) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(inputBytes);
        synchronized (mLock) {
            return mIsolateState.provideNamedData(name, inputBytes);
        }
    }

    @Override
    @SuppressWarnings("GenericException") // super.finalize() throws Throwable
    protected void finalize() throws Throwable {
        try {
            mGuard.warnIfOpen();
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Set a JavaScriptConsoleCallback to process console messages from the isolate.
     * <p>
     * Scripts always have access to console APIs, regardless of whether a console callback is
     * set. By default, no console callback is set and calling a console API from JavaScript will do
     * nothing, and will be relatively cheap. Setting a console callback allows console messages to
     * be forwarded to the embedding application, but may negatively impact performance.
     * <p>
     * Note that console APIs may expose messages generated by both JavaScript code and
     * V8/JavaScriptEngine internals.
     * <p>
     * Use caution if using this in production code as it may result in the exposure of debugging
     * information or secrets through logs.
     * <p>
     * When setting a console callback, this method should be called before requesting any
     * evaluations. Calling setConsoleCallback after requesting evaluations may result in those
     * pending evaluations' console messages being dropped or logged to a previous console callback.
     * <p>
     * Note that delayed console messages may continue to be delivered after the isolate has been
     * closed (or has crashed).
     *
     * @param executor Executor for running callback methods.
     * @param callback Callback implementing console logging behaviour.
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void setConsoleCallback(@NonNull Executor executor,
            @NonNull JavaScriptConsoleCallback callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        synchronized (mLock) {
            mIsolateState.setConsoleCallback(executor, callback);
        }
    }

    /**
     * Set a JavaScriptConsoleCallback to process console messages from the isolate.
     * <p>
     * This is the same as calling {@link #setConsoleCallback(Executor, JavaScriptConsoleCallback)}
     * using the main executor of the context used to create the {@link JavaScriptSandbox} object.
     *
     * @param callback Callback implementing console logging behaviour.
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void setConsoleCallback(@NonNull JavaScriptConsoleCallback callback) {
        Objects.requireNonNull(callback);
        synchronized (mLock) {
            mIsolateState.setConsoleCallback(mJsSandbox.getMainExecutor(),
                    callback);
        }
    }

    /**
     * Clear any JavaScriptConsoleCallback set via {@link #setConsoleCallback}.
     * <p>
     * Clearing a callback is not guaranteed to take effect for any already pending evaluations.
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void clearConsoleCallback() {
        synchronized (mLock) {
            mIsolateState.clearConsoleCallback();
        }
    }

    /**
     * Add a callback to listen for isolate crashes.
     * <p>
     * There is no guaranteed order to when these callbacks are triggered and unfinished
     * evaluations' futures are rejected.
     * <p>
     * Registering a callback after the isolate has crashed will result in it being executed
     * immediately on the supplied executor with the isolate's {@link TerminationInfo} as an
     * argument.
     * <p>
     * Closing an isolate via {@link #close()} is not considered a crash, even if there are
     * unresolved evaluations, and will not trigger termination callbacks.
     *
     * @param executor Executor with which to run callback.
     * @param callback Consumer to be called with TerminationInfo when a crash occurs.
     * @throws IllegalStateException if the callback is already registered (using any executor).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void addOnTerminatedCallback(@NonNull Executor executor,
            @NonNull Consumer<TerminationInfo> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        synchronized (mLock) {
            mIsolateState.addOnTerminatedCallback(executor, callback);
        }
    }

    /**
     * Add a callback to listen for isolate crashes.
     * <p>
     * This is the same as calling {@link #addOnTerminatedCallback(Executor, Consumer)} using the
     * main executor of the context used to create the {@link JavaScriptSandbox} object.
     *
     * @param callback Consumer to be called with TerminationInfo when a crash occurs.
     * @throws IllegalStateException if the callback is already registered (using any executor).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void addOnTerminatedCallback(@NonNull Consumer<TerminationInfo> callback) {
        addOnTerminatedCallback(mJsSandbox.getMainExecutor(), callback);
    }

    /**
     * Remove a callback previously registered with addOnTerminatedCallback.
     *
     * @param callback The callback to unregister, if currently registered.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void removeOnTerminatedCallback(@NonNull Consumer<TerminationInfo> callback) {
        Objects.requireNonNull(callback);
        synchronized (mLock) {
            mIsolateState.removeOnTerminatedCallback(callback);
        }
    }
}
