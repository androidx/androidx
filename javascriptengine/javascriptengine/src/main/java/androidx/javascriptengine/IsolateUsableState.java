/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.DeadObjectException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.javascriptengine.common.LengthLimitExceededException;
import androidx.javascriptengine.common.Utils;

import com.google.common.util.concurrent.ListenableFuture;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxConsoleCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolate;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateSyncCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Covers the case where the isolate is functional.
 */
@NotThreadSafe
final class IsolateUsableState implements IsolateState {
    private static final String TAG = "IsolateUsableState";
    final JavaScriptIsolate mJsIsolate;
    private final Object mLock = new Object();
    final int mMaxEvaluationReturnSizeBytes;

    /**
     * Interface to underlying service-backed implementation.
     */
    @NonNull
    final IJsSandboxIsolate mJsIsolateStub;
    @NonNull
    @GuardedBy("mLock")
    private Set<CallbackToFutureAdapter.Completer<String>> mPendingCompleterSet =
            new HashSet<>();
    // mOnTerminatedCallbacks does not require this.mLock, as all accesses should be performed
    // whilst holding the mLock of the JavaScriptIsolate that owns this state object.
    @NonNull
    private final HashMap<Consumer<TerminationInfo>, Executor> mOnTerminatedCallbacks =
            new HashMap<>();

    private class IJsSandboxIsolateSyncCallbackStubWrapper extends
            IJsSandboxIsolateSyncCallback.Stub {
        @NonNull
        private final CallbackToFutureAdapter.Completer<String> mCompleter;

        IJsSandboxIsolateSyncCallbackStubWrapper(
                @NonNull CallbackToFutureAdapter.Completer<String> completer) {
            mCompleter = completer;
        }

        @Override
        public void reportResultWithFd(AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            // The completer needs to be removed before offloading to the executor, otherwise there
            // is a race to complete it if all evaluations are cancelled.
            removePending(mCompleter);
            mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor.execute(
                    () -> {
                        String result;
                        try {
                            result = Utils.readToString(afd,
                                    mMaxEvaluationReturnSizeBytes,
                                    /*truncate=*/false);
                        } catch (IOException | UnsupportedOperationException ex) {
                            mCompleter.setException(
                                    new JavaScriptException(
                                            "Retrieving result failed: " + ex.getMessage()));
                            return;
                        } catch (LengthLimitExceededException ex) {
                            if (ex.getMessage() != null) {
                                mCompleter.setException(
                                        new EvaluationResultSizeLimitExceededException(
                                                ex.getMessage()));
                            } else {
                                mCompleter.setException(
                                        new EvaluationResultSizeLimitExceededException());
                            }
                            return;
                        }
                        handleEvaluationResult(mCompleter, result);
                    });
        }

        @Override
        public void reportErrorWithFd(@ExecutionErrorTypes int type, AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            // The completer needs to be removed before offloading to the executor, otherwise there
            // is a race to complete it if all evaluations are cancelled.
            removePending(mCompleter);
            mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor.execute(
                    () -> {
                        String error;
                        try {
                            error = Utils.readToString(afd,
                                    mMaxEvaluationReturnSizeBytes,
                                    /*truncate=*/true);
                        } catch (IOException | UnsupportedOperationException ex) {
                            mCompleter.setException(
                                    new JavaScriptException(
                                            "Retrieving error failed: " + ex.getMessage()));
                            return;
                        } catch (LengthLimitExceededException ex) {
                            throw new AssertionError("unreachable");
                        }
                        handleEvaluationError(mCompleter, type, error);
                    });
        }
    }

    private class IJsSandboxIsolateCallbackStubWrapper extends IJsSandboxIsolateCallback.Stub {
        @NonNull
        private final CallbackToFutureAdapter.Completer<String> mCompleter;

        IJsSandboxIsolateCallbackStubWrapper(
                @NonNull CallbackToFutureAdapter.Completer<String> completer) {
            mCompleter = completer;
        }

        @Override
        public void reportResult(String result) {
            Objects.requireNonNull(result);
            removePending(mCompleter);
            final long identityToken = Binder.clearCallingIdentity();
            try {
                handleEvaluationResult(mCompleter, result);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }

        @Override
        public void reportError(@ExecutionErrorTypes int type, String error) {
            Objects.requireNonNull(error);
            removePending(mCompleter);
            final long identityToken = Binder.clearCallingIdentity();
            try {
                handleEvaluationError(mCompleter, type, error);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }
    }

    static final class JsSandboxConsoleCallbackRelay
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
                mExecutor.execute(mCallback::onConsoleClear);
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "Console clear dropped", e);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

    }

    IsolateUsableState(JavaScriptIsolate isolate, @NonNull IJsSandboxIsolate jsIsolateStub,
            int maxEvaluationResultSizeBytes) {
        mJsIsolate = isolate;
        mJsIsolateStub = jsIsolateStub;
        mMaxEvaluationReturnSizeBytes = maxEvaluationResultSizeBytes;
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull String code) {
        if (mJsIsolate.mJsSandbox.isFeatureSupported(
                JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT)) {
            // This process can be made more memory efficient by converting the
            // String to UTF-8 encoded bytes and writing to the pipe in chunks.
            byte[] inputBytes = code.getBytes(StandardCharsets.UTF_8);
            return evaluateJavaScriptAsync(inputBytes);
        }

        return CallbackToFutureAdapter.getFuture(completer -> {
            final String futureDebugMessage = "evaluateJavascript Future";
            IJsSandboxIsolateCallbackStubWrapper callbackStub =
                    new IJsSandboxIsolateCallbackStubWrapper(completer);
            try {
                mJsIsolateStub.evaluateJavascript(code, callbackStub);
                addPending(completer);
            } catch (DeadObjectException e) {
                // The sandbox process has died.
                final TerminationInfo terminationInfo = mJsIsolate.maybeSetSandboxDead();
                Objects.requireNonNull(terminationInfo);
                completer.setException(terminationInfo.toJavaScriptException());
            } catch (RemoteException e) {
                completer.setException(new RuntimeException(e));
            }
            // Debug string.
            return futureDebugMessage;
        });
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull AssetFileDescriptor afd) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            final String futureDebugMessage = "evaluateJavascript Future";
            IJsSandboxIsolateSyncCallbackStubWrapper callbackStub =
                    new IJsSandboxIsolateSyncCallbackStubWrapper(completer);
            try {
                mJsIsolateStub.evaluateJavascriptWithFd(afd, callbackStub);
                addPending(completer);
            } catch (DeadObjectException e) {
                // The sandbox process has died.
                mJsIsolate.maybeSetSandboxDead();
                completer.setException(new SandboxDeadException());
            } catch (RemoteException e) {
                completer.setException(new RuntimeException(e));
            }
            // Debug string.
            return futureDebugMessage;
        });
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull ParcelFileDescriptor pfd) {
        long length = pfd.getStatSize() >= 0 ? pfd.getStatSize() :
                AssetFileDescriptor.UNKNOWN_LENGTH;
        AssetFileDescriptor wrapperAfd = new AssetFileDescriptor(pfd, 0, length);
        return evaluateJavaScriptAsync(wrapperAfd);
    }

    @Override
    public void setConsoleCallback(@NonNull Executor executor,
            @NonNull JavaScriptConsoleCallback callback) {
        try {
            mJsIsolateStub.setConsoleCallback(
                    new JsSandboxConsoleCallbackRelay(executor, callback));
        } catch (DeadObjectException e) {
            mJsIsolate.maybeSetSandboxDead();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setConsoleCallback(@NonNull JavaScriptConsoleCallback callback) {
        setConsoleCallback(mJsIsolate.mJsSandbox.getMainExecutor(), callback);
    }

    @Override
    public void clearConsoleCallback() {
        try {
            mJsIsolateStub.setConsoleCallback(null);
        } catch (DeadObjectException e) {
            mJsIsolate.maybeSetSandboxDead();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean provideNamedData(@NonNull String name, @NonNull byte[] inputBytes) {
        try {
            // We pass the codeAfd to the separate sandbox process but we still need to close
            // it on our end to avoid file descriptor leaks.
            try (AssetFileDescriptor codeAfd = Utils.writeBytesIntoPipeAsync(inputBytes,
                    mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor)) {
                return mJsIsolateStub.provideNamedData(name, codeAfd);
            }
        } catch (DeadObjectException e) {
            mJsIsolate.maybeSetSandboxDead();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException was thrown during provideNamedData()", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException was thrown during provideNamedData", e);
        }
        return false;
    }

    @Override
    public void close() {
        try {
            mJsIsolateStub.close();
        } catch (DeadObjectException e) {
            Log.e(TAG, "DeadObjectException was thrown during close()", e);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException was thrown during close()", e);
        }
        cancelAllPendingEvaluations(new IsolateTerminatedException("isolate closed"));
    }

    @Override
    public boolean canDie() {
        return true;
    }

    @Override
    public void onDied(@NonNull TerminationInfo terminationInfo) {
        cancelAllPendingEvaluations(terminationInfo.toJavaScriptException());
        mOnTerminatedCallbacks.forEach(
                (callback, executor) -> executor.execute(() -> callback.accept(terminationInfo)));
    }

    // Caller should call mJsIsolate.removePending(mCompleter) first
    void handleEvaluationError(@NonNull CallbackToFutureAdapter.Completer<String> completer,
            int type, @NonNull String error) {
        switch (type) {
            case IJsSandboxIsolateSyncCallback.JS_EVALUATION_ERROR:
                completer.setException(new EvaluationFailedException(error));
                break;
            case IJsSandboxIsolateSyncCallback.MEMORY_LIMIT_EXCEEDED:
                // Note that we won't ever receive a MEMORY_LIMIT_EXCEEDED evaluation error if
                // the service side supports termination notifications, so this only handles the
                // case where it doesn't.
                final TerminationInfo terminationInfo =
                        new TerminationInfo(TerminationInfo.STATUS_MEMORY_LIMIT_EXCEEDED, error);
                mJsIsolate.maybeSetIsolateDead(terminationInfo);
                // The completer was already removed from the set, so we're responsible for it.
                // Use our exception even if the isolate was already dead or closed. This might
                // result in an exception which is inconsistent with everything else if there was
                // a death or close before we called maybeSetIsolateDead above, but that requires
                // the app to have already set up a race condition.
                completer.setException(terminationInfo.toJavaScriptException());
                break;
            case IJsSandboxIsolateSyncCallback.FILE_DESCRIPTOR_IO_ERROR:
                completer.setException(new FileDescriptorIOException(error));
                break;
            default:
                completer.setException(new JavaScriptException(
                        "Unknown error: code " + type + ": " + error));
                break;
        }
    }

    // Caller should call mJsIsolate.removePending(mCompleter) first
    void handleEvaluationResult(@NonNull CallbackToFutureAdapter.Completer<String> completer,
            @NonNull String result) {
        completer.set(result);
    }

    boolean removePending(@NonNull CallbackToFutureAdapter.Completer<String> completer) {
        synchronized (mLock) {
            return mPendingCompleterSet.remove(completer);
        }
    }

    void addPending(@NonNull CallbackToFutureAdapter.Completer<String> completer) {
        synchronized (mLock) {
            mPendingCompleterSet.add(completer);
        }
    }

    // Cancel all pending and future evaluations with the given exception.
    // Only the first call to this method has any effect.
    void cancelAllPendingEvaluations(@NonNull Exception e) {
        Set<CallbackToFutureAdapter.Completer<String>> completers;
        synchronized (mLock) {
            completers = mPendingCompleterSet;
            mPendingCompleterSet = Collections.emptySet();
        }
        for (CallbackToFutureAdapter.Completer<String> ele : completers) {
            ele.setException(e);
        }
    }

    @NonNull
    ListenableFuture<String> evaluateJavaScriptAsync(@NonNull byte[] code) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            final String futureDebugMessage = "evaluateJavascript Future";
            IJsSandboxIsolateSyncCallbackStubWrapper callbackStub =
                    new IJsSandboxIsolateSyncCallbackStubWrapper(completer);
            try {
                // We pass the codeAfd to the separate sandbox process but we still need to
                // close it on our end to avoid file descriptor leaks.
                try (AssetFileDescriptor codeAfd = Utils.writeBytesIntoPipeAsync(code,
                        mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor)) {
                    mJsIsolateStub.evaluateJavascriptWithFd(codeAfd,
                            callbackStub);
                }
                addPending(completer);
            } catch (DeadObjectException e) {
                // The sandbox process has died.
                final TerminationInfo terminationInfo = mJsIsolate.maybeSetSandboxDead();
                Objects.requireNonNull(terminationInfo);
                completer.setException(terminationInfo.toJavaScriptException());
            } catch (RemoteException | IOException e) {
                completer.setException(new RuntimeException(e));
            }
            // Debug string.
            return futureDebugMessage;
        });
    }

    @Override
    public void addOnTerminatedCallback(@NonNull Executor executor,
            @NonNull Consumer<TerminationInfo> callback) {
        if (mOnTerminatedCallbacks.putIfAbsent(callback, executor) != null) {
            throw new IllegalStateException("Termination callback already registered");
        }
    }

    @Override
    public void removeOnTerminatedCallback(@NonNull Consumer<TerminationInfo> callback) {
        synchronized (mLock) {
            mOnTerminatedCallbacks.remove(callback);
        }
    }
}
