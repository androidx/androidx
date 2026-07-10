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

import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.javascriptengine.common.LengthLimitExceededException;
import androidx.javascriptengine.common.MessagePortInternal;
import androidx.javascriptengine.common.Utils;

import com.google.common.util.concurrent.ListenableFuture;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxConsoleCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolate;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateSyncCallback;
import org.chromium.android_webview.js_sandbox.common.IMessagePort;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
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
        private final CallbackToFutureAdapter.@NonNull Completer<String> mCompleter;

        IJsSandboxIsolateSyncCallbackStubWrapper(
                CallbackToFutureAdapter.@NonNull Completer<String> completer) {
            mCompleter = completer;
        }

        @Override
        public void reportResultWithFd(AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            try {
                mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor.execute(
                        () -> {
                            // There could have been a race that removes the completer before this
                            // if all evaluations for the isolate were canceled due to termination.
                            // However, it's safe to proceed regardless.
                            removePending(mCompleter);
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
            } catch (RejectedExecutionException e) {
                // The sandbox could have been killed/closed soon after this result started to
                // arrive, in which case the executor service may have shutdown. The completer will
                // be setExceptioned as part of the close/crash handling. Just release the FD.
                Utils.closeQuietly(afd);
            }
        }

        @Override
        public void reportErrorWithFd(@ExecutionErrorTypes int type, AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            try {
                mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor.execute(
                        () -> {
                            // We'd still potentially need to process the result even if this
                            // returns false (the completer isn't pending; for example, if the
                            // isolate was closed). The result might indicate an OOM that requires
                            // app-side intervention to kill the sandbox.
                            removePending(mCompleter);
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
            } catch (RejectedExecutionException e) {
                // The sandbox could have been killed/closed soon after this result started to
                // arrive, in which case the executor service may have shutdown. The completer will
                // be setExceptioned as part of the close/crash handling. Just release the FD.
                Utils.closeQuietly(afd);
            }
        }

        @Override
        public int getInterfaceVersion() {
            return super.VERSION;
        }
    }

    private class IJsSandboxIsolateCallbackStubWrapper extends IJsSandboxIsolateCallback.Stub {
        private final CallbackToFutureAdapter.@NonNull Completer<String> mCompleter;

        IJsSandboxIsolateCallbackStubWrapper(
                CallbackToFutureAdapter.@NonNull Completer<String> completer) {
            mCompleter = completer;
        }

        @Override
        public void reportResult(String result) {
            Objects.requireNonNull(result);
            // There could have been a race that removes the completer before this
            // if all evaluations for the isolate were canceled due to termination.
            // However, it's safe to proceed regardless.
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
            // We'd still potentially need to process the result even if this
            // returns false (the completer isn't pending; for example, if the
            // isolate was closed). The result might indicate an OOM that requires
            // app-side intervention to kill the sandbox.
            removePending(mCompleter);
            final long identityToken = Binder.clearCallingIdentity();
            try {
                handleEvaluationError(mCompleter, type, error);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }

        @Override
        public int getInterfaceVersion() {
            return super.VERSION;
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

        @Override
        public int getInterfaceVersion() {
            return super.VERSION;
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
                final TerminationInfo terminationInfo = killSandbox(e);
                completer.setException(terminationInfo.toJavaScriptException());
            } catch (RemoteException | RuntimeException e) {
                throw killSandboxAndGetRuntimeException(e);
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
                final TerminationInfo terminationInfo = killSandbox(e);
                completer.setException(terminationInfo.toJavaScriptException());
            } catch (RemoteException | RuntimeException e) {
                throw killSandboxAndGetRuntimeException(e);
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
            killSandbox(e);
        } catch (RemoteException | RuntimeException e) {
            throw killSandboxAndGetRuntimeException(e);
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
            killSandbox(e);
        } catch (RemoteException | RuntimeException e) {
            throw killSandboxAndGetRuntimeException(e);
        }
    }

    @Override
    public void provideNamedData(@NonNull String name, byte @NonNull [] inputBytes) {
        // We pass the codeAfd to the separate sandbox process but we still need to close
        // it on our end to avoid file descriptor leaks.
        //
        // Don't catch RejectedExecutionException, as it shouldn't be possible for the
        // ExecutorService to have shut down without the isolate having been notified.
        try (AssetFileDescriptor codeAfd = Utils.writeBytesIntoPipeAsync(inputBytes,
                mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor)) {
            try {
                final boolean success = mJsIsolateStub.provideNamedData(name, codeAfd);
                if (!success) {
                    throw new IllegalStateException(
                            "Data with name '" + name + "' has already been provided");
                }
            } catch (DeadObjectException e) {
                killSandbox(e);
            } catch (RemoteException | RuntimeException e) {
                throw killSandboxAndGetRuntimeException(e);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            mJsIsolateStub.close();
        } catch (DeadObjectException e) {
            killSandbox(e);
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Exception was thrown during close()", e);
            killSandbox(e);
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
    void handleEvaluationError(CallbackToFutureAdapter.@NonNull Completer<String> completer,
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
                completer.setException(new DataInputException(error));
                break;
            default:
                completer.setException(new JavaScriptException(
                        "Unknown error: code " + type + ": " + error));
                break;
        }
    }

    // Caller should call mJsIsolate.removePending(mCompleter) first
    void handleEvaluationResult(CallbackToFutureAdapter.@NonNull Completer<String> completer,
            @NonNull String result) {
        completer.set(result);
    }

    boolean removePending(CallbackToFutureAdapter.@NonNull Completer<String> completer) {
        synchronized (mLock) {
            return mPendingCompleterSet.remove(completer);
        }
    }

    void addPending(CallbackToFutureAdapter.@NonNull Completer<String> completer) {
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
    ListenableFuture<String> evaluateJavaScriptAsync(byte @NonNull [] code) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            final String futureDebugMessage = "evaluateJavascript Future";
            IJsSandboxIsolateSyncCallbackStubWrapper callbackStub =
                    new IJsSandboxIsolateSyncCallbackStubWrapper(completer);
            // Don't catch RejectedExecutionException, as it shouldn't be possible for the
            // ExecutorService to have shut down without the isolate having been notified.
            try (AssetFileDescriptor codeAfd = Utils.writeBytesIntoPipeAsync(code,
                    mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor)) {
                // We pass the codeAfd to the separate sandbox process but we still need to
                // close it on our end to avoid file descriptor leaks.
                try {
                    mJsIsolateStub.evaluateJavascriptWithFd(codeAfd,
                            callbackStub);
                    addPending(completer);
                } catch (DeadObjectException e) {
                    final TerminationInfo terminationInfo = killSandbox(e);
                    completer.setException(terminationInfo.toJavaScriptException());
                } catch (RemoteException | RuntimeException e) {
                    throw killSandboxAndGetRuntimeException(e);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
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

    @NonNull
    @Override
    public MessagePort provideMessagePort(@NonNull String name, @NonNull Executor executor,
            @NonNull MessagePortClient client) {
        if (!mJsIsolate.mJsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_MESSAGE_PORTS)) {
            throw new UnsupportedOperationException("Sandbox does not support MessagePorts");
        }

        if (mJsIsolate.containsMessagePort(name)) {
            throw new IllegalStateException("MessagePort with name '" + name
                    + "' already exists");
        }

        // Create unentangled MessagePort. Contains local IMessagePort interface implementation.
        final MessagePortInternal messagePortInternal = new MessagePortInternal(
                mJsIsolate.mJsSandbox.mThreadPoolTaskExecutor,
                mMaxEvaluationReturnSizeBytes);


        // Set internal client that calls into the public client to handle received messages.
        messagePortInternal.setClient(new MessagePortInternal.MessagePortClient() {
                    @Override
                    public void onString(@NonNull String string) {
                        executor.execute(() -> client.onMessage(
                                Message.createStringMessage(string)));
                    }

                    @Override
                    public void onArrayBuffer(byte @NonNull [] arrayBuffer) {
                        executor.execute(() -> client.onMessage(
                                Message.createArrayBufferMessage(arrayBuffer)));
                    }
                });

        final IMessagePort portOut;
        try {
            portOut = mJsIsolateStub.provideMessagePort(name,
                    messagePortInternal.getLocalIMessagePort());
        } catch (DeadObjectException e) {
            killSandbox(e);
            return new MessagePort();
        } catch (RemoteException | RuntimeException e) {
            throw killSandboxAndGetRuntimeException(e);
        }
        mJsIsolate.addMessagePort(name);

        // Create entangled MessagePort. Contains remote IMessagePort interface implementation.
        messagePortInternal.setRemoteIMessagePort(portOut);
        MessagePort messagePort = new MessagePort(messagePortInternal);
        return messagePort;
    }

    /**
     * Kill the sandbox and update state.
     * @param e the exception causing us to kill the sandbox
     * @return terminationInfo that has been set on the isolate
     */
    @NonNull
    private TerminationInfo killSandbox(@NonNull Exception e) {
        mJsIsolate.mJsSandbox.killDueToException(e);
        final TerminationInfo terminationInfo = mJsIsolate.maybeSetSandboxDead();
        // We're in the Usable state and the call stack should be holding a lock on the isolate,
        // so this should be the first time we find out the sandbox/isolate has died and
        // terminationInfo should never be null here.
        Objects.requireNonNull(terminationInfo);
        return terminationInfo;
    }

    /**
     * Kill the sandbox, update state, and return a RuntimeException.
     * @param e the original exception causing us to kill the sandbox
     * @return a runtime exception which may optionally be thrown
     */
    @NonNull
    private RuntimeException killSandboxAndGetRuntimeException(@NonNull Exception e) {
        killSandbox(e);
        return Utils.exceptionToRuntimeException(e);
    }
}
