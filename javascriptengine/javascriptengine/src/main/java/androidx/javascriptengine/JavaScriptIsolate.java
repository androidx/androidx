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
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.javascriptengine.common.Utils;

import com.google.common.util.concurrent.ListenableFuture;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxConsoleCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolate;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateSyncCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.GuardedBy;

/**
 * Environment within a {@link JavaScriptSandbox} where Javascript is executed.
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
 * Each isolate object must only be used from one thread.
 */
public final class JavaScriptIsolate implements AutoCloseable {
    private static final String TAG = "JavaScriptIsolate";
    private final Object mSetLock = new Object();
    /**
     * Interface to underlying service-backed implementation.
     * <p>
     * mJsIsolateStub should only be null when the Isolate has been explicitly closed - not when the
     * isolate has crashed or simply had its pending and future evaluations cancelled.
     */
    @Nullable
    private IJsSandboxIsolate mJsIsolateStub;
    private CloseGuardHelper mGuard = CloseGuardHelper.create();
    final JavaScriptSandbox mJsSandbox;

    @Nullable
    @GuardedBy("mSetLock")
    private HashSet<CallbackToFutureAdapter.Completer<String>> mPendingCompleterSet =
            new HashSet<CallbackToFutureAdapter.Completer<String>>();
    /**
     * If mSandboxClosed is true, new evaluations will throw this exception asynchronously.
     * <p>
     * Note that if the isolate is closed, IllegalStateException is thrown synchronously instead.
     */
    @Nullable
    private Exception mExceptionForNewEvaluations;
    private AtomicBoolean mSandboxClosed = new AtomicBoolean(false);
    IsolateStartupParameters mStartupParameters;

    private class IJsSandboxIsolateSyncCallbackStubWrapper extends
            IJsSandboxIsolateSyncCallback.Stub {
        @NonNull
        private CallbackToFutureAdapter.Completer<String> mCompleter;

        IJsSandboxIsolateSyncCallbackStubWrapper(
                @NonNull CallbackToFutureAdapter.Completer<String> completer) {
            mCompleter = completer;
        }

        @Override
        public void reportResultWithFd(AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            mJsSandbox.mThreadPoolTaskExecutor.execute(
                    () -> {
                        String result;
                        try {
                            result = Utils.readToString(afd,
                                    mStartupParameters.getMaxEvaluationReturnSizeBytes(), false);
                        } catch (IOException | UnsupportedOperationException ex) {
                            mCompleter.setException(
                                    new JavaScriptException(
                                            "Retrieving result failed: " + ex.getMessage()));
                            removePending(mCompleter);
                            return;
                        } catch (IllegalArgumentException ex) {
                            if (ex.getMessage() != null) {
                                mCompleter.setException(
                                        new EvaluationResultSizeLimitExceededException(
                                                ex.getMessage()));
                            } else {
                                mCompleter.setException(
                                        new EvaluationResultSizeLimitExceededException());
                            }
                            removePending(mCompleter);
                            return;
                        }
                        handleEvaluationResult(mCompleter, result);
                    });
        }

        @Override
        public void reportErrorWithFd(@ExecutionErrorTypes int type, AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            mJsSandbox.mThreadPoolTaskExecutor.execute(
                    () -> {
                        String error;
                        try {
                            error = Utils.readToString(afd,
                                    mStartupParameters.getMaxEvaluationReturnSizeBytes(), true);
                        } catch (IOException | UnsupportedOperationException ex) {
                            mCompleter.setException(
                                    new JavaScriptException(
                                            "Retrieving error failed: " + ex.getMessage()));
                            removePending(mCompleter);
                            return;
                        }
                        handleEvaluationError(mCompleter, type, error);
                    });
        }
    }

    private class IJsSandboxIsolateCallbackStubWrapper extends IJsSandboxIsolateCallback.Stub {
        @NonNull
        private CallbackToFutureAdapter.Completer<String> mCompleter;

        IJsSandboxIsolateCallbackStubWrapper(
                @NonNull CallbackToFutureAdapter.Completer<String> completer) {
            mCompleter = completer;
        }

        @Override
        public void reportResult(String result) {
            Objects.requireNonNull(result);
            handleEvaluationResult(mCompleter, result);
        }

        @Override
        public void reportError(@ExecutionErrorTypes int type, String error) {
            Objects.requireNonNull(error);
            handleEvaluationError(mCompleter, type, error);
        }
    }

    private static final class JsSandboxConsoleCallbackRelay
            extends IJsSandboxConsoleCallback.Stub {
        @NonNull
        private final Executor mExecutor;
        @NonNull
        private final JavaScriptConsoleCallback mCallback;

        JsSandboxConsoleCallbackRelay(@NonNull Executor executor,
                @NonNull JavaScriptConsoleCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void consoleMessage(final int contextGroupId, final int level, final String message,
                final String source, final int line, final int column, final String trace) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> {
                    if ((level & JavaScriptConsoleCallback.ConsoleMessage.LEVEL_ALL) == 0
                            || ((level - 1) & level) != 0) {
                        throw new IllegalArgumentException(
                                "invalid console level " + level + " provided by isolate");
                    }
                    Objects.requireNonNull(message);
                    Objects.requireNonNull(source);
                    mCallback.onConsoleMessage(
                            new JavaScriptConsoleCallback.ConsoleMessage(
                                level, message, source, line, column, trace));
                });
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "Console message dropped", e);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void consoleClear(int contextGroupId) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mCallback.onConsoleClear());
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "Console clear dropped", e);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    JavaScriptIsolate(@NonNull IJsSandboxIsolate jsIsolateStub, @NonNull JavaScriptSandbox sandbox,
            @NonNull IsolateStartupParameters settings) {
        mJsSandbox = sandbox;
        mJsIsolateStub = jsIsolateStub;
        mStartupParameters = settings;
        mGuard.open("close");
        // This should be at the end of the constructor.
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
     * The environment uses a single JS global object for all the calls to {@link
     * #evaluateJavaScriptAsync(String)} and {@link #provideNamedData(String, byte[])} methods.
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
     *
     * @param code JavaScript code that is evaluated, it should return a JavaScript String or a
     *             Promise of a String in case {@link JavaScriptSandbox#JS_FEATURE_PROMISE_RETURN}
     *             is supported
     * @return Future that evaluates to the result String of the evaluation or exceptions (see
     * {@link JavaScriptException} and subclasses) if there is an error
     */
    @SuppressWarnings("NullAway")
    @NonNull
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull String code) {
        Objects.requireNonNull(code);
        if (!mSandboxClosed.get() && mJsSandbox.isFeatureSupported(
                JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT)) {
            // This process can be made more memory efficient by converting the String to
            // UTF-8 encoded bytes and writing to the pipe in chunks.
            byte[] inputBytes = code.getBytes(StandardCharsets.UTF_8);
            return evaluateJavaScriptAsync(inputBytes);
        }
        if (mJsIsolateStub == null) {
            throw new IllegalStateException(
                    "Calling evaluateJavaScriptAsync() after closing the Isolate");
        }
        return CallbackToFutureAdapter.getFuture(completer -> {
            final String futureDebugMessage = "evaluateJavascript Future";
            IJsSandboxIsolateCallbackStubWrapper callbackStub;
            synchronized (mSetLock) {
                if (mPendingCompleterSet == null) {
                    completer.setException(mExceptionForNewEvaluations);
                    return futureDebugMessage;
                }
                mPendingCompleterSet.add(completer);
            }
            callbackStub = new IJsSandboxIsolateCallbackStubWrapper(completer);
            try {
                mJsIsolateStub.evaluateJavascript(code, callbackStub);
            } catch (RemoteException e) {
                completer.setException(new RuntimeException(e));
                synchronized (mSetLock) {
                    mPendingCompleterSet.remove(completer);
                }
            }
            // Debug string.
            return futureDebugMessage;
        });
    }

    /**
     * Evaluates the given JavaScript code which is encoded in UTF-8 and returns the result.
     * <p>
     * Please refer to the documentation of {@link #evaluateJavaScriptAsync(String)} as the
     * behavior of this method is similar other than for the input type.
     * <p>
     * <strong>Note: The {@code byte[]} must be UTF-8 encoded.</strong>
     * <p>
     * This overload is provided for clients to pass in a UTF-8 encoded {@code byte[]} directly
     * instead of having to convert it into a {@code String} to use
     * {@link #evaluateJavaScriptAsync(String)}.
     *
     * @param code UTF-8 encoded JavaScript code that is evaluated, it should return a JavaScript
     *             String or a Promise of a String in case
     *             {@link JavaScriptSandbox#JS_FEATURE_PROMISE_RETURN} is supported
     * @return Future that evaluates to the result String of the evaluation or exceptions (see
     * {@link JavaScriptException} and subclasses) if there is an error
     */
    @SuppressWarnings("NullAway")
    @NonNull
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull byte[] code) {
        Objects.requireNonNull(code);
        if (mJsIsolateStub == null) {
            throw new IllegalStateException(
                    "Calling evaluateJavaScriptAsync() after closing the Isolate");
        }
        return CallbackToFutureAdapter.getFuture(completer -> {
            final String futureDebugMessage = "evaluateJavascript Future";
            IJsSandboxIsolateSyncCallbackStubWrapper callbackStub;
            synchronized (mSetLock) {
                if (mPendingCompleterSet == null) {
                    completer.setException(mExceptionForNewEvaluations);
                    return futureDebugMessage;
                }
                mPendingCompleterSet.add(completer);
            }
            callbackStub = new IJsSandboxIsolateSyncCallbackStubWrapper(completer);
            try {
                AssetFileDescriptor codeAfd = Utils.writeBytesIntoPipeAsync(code,
                        mJsSandbox.mThreadPoolTaskExecutor);
                try {
                    mJsIsolateStub.evaluateJavascriptWithFd(codeAfd, callbackStub);
                } finally {
                    // We pass the codeAfd to the separate sandbox process but we still need to
                    // close it on our end to avoid file descriptor leaks.
                    codeAfd.close();
                }
            } catch (RemoteException | IOException e) {
                completer.setException(new RuntimeException(e));
                synchronized (mSetLock) {
                    mPendingCompleterSet.remove(completer);
                }
            }
            // Debug string.
            return futureDebugMessage;
        });
    }

    /**
     * Closes the {@link JavaScriptIsolate} object and renders it unusable.
     * <p>
     * Once closed, no more method calls should be made. Pending evaluations resolve with
     * {@link IsolateTerminatedException} immediately.
     * <p>
     * If {@link JavaScriptSandbox#isFeatureSupported(String)} is {@code true} for {@link
     * JavaScriptSandbox#JS_FEATURE_ISOLATE_TERMINATION}, then any pending evaluation is immediately
     * terminated and memory is freed. If it is {@code false}, the isolate will not get cleaned
     * up until the pending evaluations have run to completion and will consume resources until
     * then.
     */
    @Override
    public void close() {
        // IllegalStateException will be thrown synchronously instead for new evaluations.
        mExceptionForNewEvaluations = null;
        if (mJsIsolateStub == null) {
            return;
        }
        try {
            cancelAllPendingEvaluations(new IsolateTerminatedException());
            mJsIsolateStub.close();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException was thrown during close()", e);
        }
        mJsIsolateStub = null;
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
     * #evaluateJavaScriptAsync(String)} and {@link #provideNamedData(String, byte[])} methods.
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
        if (mJsIsolateStub == null) {
            throw new IllegalStateException("Calling provideNamedData() after closing the Isolate");
        }
        try {
            AssetFileDescriptor codeAfd = Utils.writeBytesIntoPipeAsync(inputBytes,
                    mJsSandbox.mThreadPoolTaskExecutor);
            try {
                return mJsIsolateStub.provideNamedData(name, codeAfd);
            } finally {
                // We pass the codeAfd to the separate sandbox process but we still need to close
                // it on our end to avoid file descriptor leaks.
                codeAfd.close();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException was thrown during provideNamedData()", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException was thrown during provideNamedData", e);
        }
        return false;
    }

    void handleEvaluationError(@NonNull CallbackToFutureAdapter.Completer<String> completer,
            int type, @NonNull String error) {
        boolean crashing = false;
        switch (type) {
            case IJsSandboxIsolateSyncCallback.JS_EVALUATION_ERROR:
                completer.setException(new EvaluationFailedException(error));
                break;
            case IJsSandboxIsolateSyncCallback.MEMORY_LIMIT_EXCEEDED:
                completer.setException(new MemoryLimitExceededException(error));
                crashing = true;
                break;
            default:
                completer.setException(new JavaScriptException(
                        "Crashing due to unknown JavaScriptException: " + error));
                // Assume the worst
                crashing = true;
        }
        removePending(completer);
        if (crashing) {
            handleCrash();
        }
    }

    void handleEvaluationResult(@NonNull CallbackToFutureAdapter.Completer<String> completer,
            @NonNull String result) {
        completer.set(result);
        removePending(completer);
    }

    void notifySandboxClosed() {
        mSandboxClosed.set(true);
        cancelAllPendingEvaluations(new SandboxDeadException());
    }

    // Cancel all pending and future evaluations with the given exception.
    // Only the first call to this method has any effect.
    void cancelAllPendingEvaluations(@NonNull Exception e) {
        final HashSet<CallbackToFutureAdapter.Completer<String>> pendingSet;
        synchronized (mSetLock) {
            if (mPendingCompleterSet == null) return;
            pendingSet = mPendingCompleterSet;
            mPendingCompleterSet = null;
            mExceptionForNewEvaluations = e;
        }
        for (CallbackToFutureAdapter.Completer<String> ele : pendingSet) {
            ele.setException(e);
        }
    }

    void removePending(@NonNull CallbackToFutureAdapter.Completer<String> completer) {
        synchronized (mSetLock) {
            if (mPendingCompleterSet != null) {
                mPendingCompleterSet.remove(completer);
            }
        }
    }

    void handleCrash() {
        cancelAllPendingEvaluations(new IsolateTerminatedException());
    }

    @Override
    @SuppressWarnings("GenericException") // super.finalize() throws Throwable
    protected void finalize() throws Throwable {
        try {
            if (mGuard != null) {
                mGuard.warnIfOpen();
            }
            if (mJsIsolateStub != null) {
                close();
            }
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
     * @param executor Executor for running callback methods.
     * @param callback Callback implementing console logging behaviour.
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void setConsoleCallback(@NonNull Executor executor,
            @NonNull JavaScriptConsoleCallback callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        if (mJsIsolateStub == null) {
            throw new IllegalStateException(
                    "Calling setConsoleCallback() after closing the Isolate");
        }
        try {
            mJsIsolateStub.setConsoleCallback(
                    new JsSandboxConsoleCallbackRelay(executor, callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set a JavaScriptConsoleCallback to process console messages from the isolate.
     * <p>
     * This is the same as calling {@link #setConsoleCallback(Executor, JavaScriptConsoleCallback}
     * using the main executor ({@link Context.getMainExecutor()}) of the context used to create the
     * {@link JavaScriptSandbox} object.
     * @param callback Callback implementing console logging behaviour.
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void setConsoleCallback(@NonNull JavaScriptConsoleCallback callback) {
        Objects.requireNonNull(callback);
        setConsoleCallback(mJsSandbox.getMainExecutor(), callback);
    }

    /**
     * Clear any JavaScriptConsoleCallback set via {@link #setConsoleCallback}.
     * <p>
     * Clearing a callback is not guaranteed to take effect for any already pending evaluations.
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void clearConsoleCallback() {
        if (mJsIsolateStub == null) {
            throw new IllegalStateException(
                    "Calling clearConsoleCallback() after closing the Isolate");
        }
        try {
            mJsIsolateStub.setConsoleCallback(null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
