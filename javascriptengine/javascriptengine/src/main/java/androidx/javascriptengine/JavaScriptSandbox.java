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
import android.os.Build;
import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.javascriptengine.common.Utils;

import com.google.common.util.concurrent.ListenableFuture;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolate;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateClient;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Sandbox that provides APIs for JavaScript evaluation in a restricted environment.
 * <p>
 * JavaScriptSandbox represents a connection to an isolated process. The isolated process is
 * exclusive to the calling app (i.e. it doesn't share anything with, and can't be compromised by
 * another app's isolated process).
 * <p>
 * Code that is run in a sandbox does not have any access to data
 * belonging to the original app unless explicitly passed into it by using the methods of this
 * class. This provides a security boundary between the calling app and the JavaScript execution
 * environment.
 * <p>
 * It's safe to share a single {@link JavaScriptSandbox}
 * object with multiple threads and use it from multiple threads at once.
 * For example, {@link JavaScriptSandbox} can be stored at a global location and multiple threads
 * can create their own {@link JavaScriptIsolate} objects from it but the
 * {@link JavaScriptIsolate} object cannot be shared.
 * <p>
 * A JavaScriptSandbox is normally obtained via {@link #createSharedSandboxAsync(Context)} or
 * {@link #createDedicatedSandboxAsync(Context)}.
 */
@ThreadSafe
public final class JavaScriptSandbox implements AutoCloseable {
    private static final String TAG = "JavaScriptSandbox";

    private final Object mLock = new Object();
    private final CloseGuardHelper mGuard = CloseGuardHelper.create();

    @NonNull
    @GuardedBy("mLock")
    private final IJsSandboxService mJsSandboxService;

    // The connection may be closed without holding the lock.
    @NonNull
    private final SandboxServiceConnection mConnection;
    @NonNull
    private final Context mContext;

    @GuardedBy("mLock")
    @NonNull
    private Set<JavaScriptIsolate> mActiveIsolateSet;

    private enum State {
        ALIVE,
        DEAD,
        CLOSED,
    }

    @GuardedBy("mLock")
    @NonNull
    private State mState;

    /**
     * A client-side feature, which may be conditional on one or more service-side features.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(value =
            {
                    JS_FEATURE_ISOLATE_TERMINATION,
                    JS_FEATURE_PROMISE_RETURN,
                    JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER,
                    JS_FEATURE_WASM_COMPILATION,
                    JS_FEATURE_ISOLATE_MAX_HEAP_SIZE,
                    JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT,
                    JS_FEATURE_CONSOLE_MESSAGING,
                    JS_FEATURE_ISOLATE_CLIENT,
                    JS_FEATURE_EVALUATE_FROM_FD,
                    JS_FEATURE_MESSAGE_PORTS,
            })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface JsSandboxFeature {
    }

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * When this
     * feature is present, {@link JavaScriptIsolate#close()} terminates the currently running JS
     * evaluation and closes the isolate. If it is absent, {@link JavaScriptIsolate#close()} cannot
     * terminate any running or queued evaluations in the background, so the isolate continues to
     * consume resources until they complete.
     * <p>
     * Irrespective of this feature, calling {@link JavaScriptSandbox#close()} for a legacy or
     * dedicated sandbox terminates all {@link JavaScriptIsolate} objects (and the isolated process)
     * immediately and all pending {@link JavaScriptIsolate#evaluateJavaScriptAsync(String)} futures
     * resolve with {@link IsolateTerminatedException}. Shared sandboxes may not terminate isolates
     * as reliably if there are other shared sandboxes in use, but will attempt to terminate their
     * isolates if possible.
     */
    public static final String JS_FEATURE_ISOLATE_TERMINATION = "JS_FEATURE_ISOLATE_TERMINATION";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * When this feature is present, JS expressions may return promises. The Future returned by
     * {@link JavaScriptIsolate#evaluateJavaScriptAsync(String)} resolves to the promise's result,
     * once the promise resolves.
     */
    public static final String JS_FEATURE_PROMISE_RETURN = "JS_FEATURE_PROMISE_RETURN";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * When this feature is present, {@link JavaScriptIsolate#provideNamedData(String, byte[])}
     * can be used.
     * <p>
     * This also covers the JS API android.consumeNamedDataAsArrayBuffer(string).
     */
    public static final String JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER =
            "JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * This features provides additional behavior to {@link
     * JavaScriptIsolate#evaluateJavaScriptAsync(String)} ()}. When this feature is present, the JS
     * API WebAssembly.compile(ArrayBuffer) can be used.
     */
    public static final String JS_FEATURE_WASM_COMPILATION = "JS_FEATURE_WASM_COMPILATION";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * When this feature is present,
     * {@link JavaScriptSandbox#createIsolate(IsolateStartupParameters)} can be used.
     */
    public static final String JS_FEATURE_ISOLATE_MAX_HEAP_SIZE =
            "JS_FEATURE_ISOLATE_MAX_HEAP_SIZE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * When this feature is present, the script passed into
     * {@link JavaScriptIsolate#evaluateJavaScriptAsync(String)} as well as the result/error is
     * not limited by the Binder transaction buffer size.
     */
    @SuppressWarnings("IntentName")
    public static final String JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT =
            "JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * When this feature is present, {@link JavaScriptIsolate#setConsoleCallback} can be used to set
     * a {@link JavaScriptConsoleCallback} for processing console messages.
     */
    public static final String JS_FEATURE_CONSOLE_MESSAGING = "JS_FEATURE_CONSOLE_MESSAGING";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * When this feature is present, the service can be provided with a Binder interface for
     * calling into the client, independent of callbacks.
     */
    static final String JS_FEATURE_ISOLATE_CLIENT =
            "JS_FEATURE_ISOLATE_CLIENT";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * <p>
     * When this feature is present,
     * {@link JavaScriptIsolate#evaluateJavaScriptAsync(android.content.res.AssetFileDescriptor)}
     * and {@link JavaScriptIsolate#evaluateJavaScriptAsync(android.os.ParcelFileDescriptor)}
     * can be used to evaluate JavaScript code of known and unknown length from file descriptors.
     */
    public static final String JS_FEATURE_EVALUATE_FROM_FD =
            "JS_FEATURE_EVALUATE_FROM_FD";

    /**
     * Feature for {@link #isFeatureSupported(String)}
     * <p>
     * When this feature is present, message ports can be sent to JavaScript isolates using
     * {@link JavaScriptIsolate#createMessageChannel(String, Executor, MessagePortClient)} and be
     * used to send and receive messages between the embedder and the JavaScript isolate.
     */
    public static final String JS_FEATURE_MESSAGE_PORTS = "JS_FEATURE_MESSAGE_PORTS";

    // This set must not be modified after JavaScriptSandbox construction.
    @NonNull
    private final HashSet<String> mClientSideFeatureSet;

    /**
     * Asynchronously create and connect to a legacy sandbox process.
     * <p>
     * Only one sandbox process created via this method can exist at a time. Attempting to create
     * a new instance before the previous instance has been closed fails with an
     * {@link IllegalStateException}.
     * <p>
     * Using this method is discouraged. Prefer to use {@link #createSharedSandboxAsync(Context)}
     * or {@link #createDedicatedSandboxAsync(Context)} instead, as these allow the creation of
     * multiple concurrent JavaScriptSandbox instances.
     * <p>
     * You should only fall back to using createConnectedInstanceAsync if you are sure that your
     * code is (and will always be) the only possible caller of this method. If you cannot guarantee
     * this, you should either accept not being able to run JavaScript, or fall back to using an
     * alternative such as evaluating scripts via a {@link android.webkit.WebView}, but be aware of
     * the significant differences between JavaScriptEngine and WebView JavaScript environments and
     * their security/isolation properties.
     * <p>
     * Sandbox support should be checked using {@link #isSupported()} before attempting to create
     * a sandbox via this method.
     *
     * @param context the Context for the sandbox. Use an application context if the connection
     *                is expected to outlive a single activity or service.
     * @return a Future that evaluates to a connected {@link JavaScriptSandbox} instance or an
     * exception if binding to the service fails.
     */
    @NonNull
    public static ListenableFuture<JavaScriptSandbox> createConnectedInstanceAsync(
            @NonNull Context context) {
        Objects.requireNonNull(context);
        return SandboxServiceInstance.createLegacySandbox(context);
    }

    /**
     * Asynchronously connect to a shared sandbox process, creating it if needed.
     * <p>
     * Each call to this method resolves to a distinct JavaScriptSandbox instance, but a
     * common underlying sandbox service process will be used. The lifecycle of the shared process
     * is managed automatically.
     * <p>
     * For most use cases, using a shared sandbox is preferred over using a dedicated sandbox, as
     * shared sandboxes use fewer system resources and avoid repeated startup overheads.
     * <p>
     * Note that this does not provide any process-level isolation between the returned shared
     * sandboxes. This may result in decreased stability and security compared to using dedicated
     * sandboxes.
     * <p>
     * Sandbox support should be checked using {@link JavaScriptSandbox#isSupported()} before
     * attempting to create a sandbox via this method.
     *
     * @param context the Context for the sandbox. Use an application context if the connection
     *                is expected to outlive a single activity or service.
     * @return a Future that evaluates to a connected {@link JavaScriptSandbox} instance or an
     * exception if binding to the service fails.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    @RequiresApi(Build.VERSION_CODES.Q)
    public static ListenableFuture<JavaScriptSandbox> createSharedSandboxAsync(
            @NonNull Context context) {
        Objects.requireNonNull(context);
        return SandboxServiceInstance.createSharedSandbox(context);
    }

    /**
     * Asynchronously create and connect to a dedicated sandbox process.
     * <p>
     * Each call to this method resolves to a distinct {@link JavaScriptSandbox} instance with a
     * dedicated sandbox process.
     * <p>
     * This offers a higher level of security and stability compared to shared sandboxes, but may
     * incur additional startup overhead and consume additional system resources. Generally, you
     * should prefer using a shared sandbox unless you require the stronger isolation properties
     * of a dedicated sandbox.
     * <p>
     * Sandbox support should be checked using {@link JavaScriptSandbox#isSupported()} before
     * attempting to create a sandbox via this method.
     *
     * @param context the Context for the sandbox. Use an application context if the connection
     *                is expected to outlive a single activity or service.
     * @return a Future that evaluates to a connected {@link JavaScriptSandbox} instance or an
     * exception if binding to the service fails.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    @RequiresApi(Build.VERSION_CODES.Q)
    public static ListenableFuture<JavaScriptSandbox> createDedicatedSandboxAsync(
            @NonNull Context context) {
        Objects.requireNonNull(context);
        return SandboxServiceInstance.createDedicatedSandbox(context);
    }

    /**
     * Asynchronously create and connect to a legacy sandbox process for testing.
     * <p>
     * Only one sandbox process created via this method can exist at a time. Attempting to create
     * a new instance before the previous instance has been closed fails with an
     * {@link IllegalStateException}.
     *
     * @param context the Context for the sandbox. Use an application context if the connection
     *                is expected to outlive a single activity or service.
     * @return a Future that evaluates to a connected {@link JavaScriptSandbox} instance or an
     * exception if binding to the service fails.
     */
    @NonNull
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static ListenableFuture<JavaScriptSandbox> createConnectedInstanceForTestingAsync(
            @NonNull Context context) {
        Objects.requireNonNull(context);
        return SandboxServiceInstance.createLegacySandboxForTesting(context);
    }

    /**
     * Check if JavaScriptSandbox is supported on the system.
     * <p>
     * This method should be used to check for sandbox support before attempting to create a
     * sandbox.
     *
     * @return true if JavaScriptSandbox is supported and false otherwise.
     */
    public static boolean isSupported() {
        return SandboxServiceInstance.isSandboxSupported();
    }

    // We prevent direct initializations of this class.
    // Use JavaScriptSandbox.createConnectedInstance().
    JavaScriptSandbox(@NonNull Context context,
            @NonNull SandboxServiceConnection sandboxServiceConnection,
            @NonNull IJsSandboxService jsSandboxService) throws RemoteException {
        mContext = context;
        mConnection = sandboxServiceConnection;
        mJsSandboxService = jsSandboxService;
        final List<String> features = mJsSandboxService.getSupportedFeatures();
        mClientSideFeatureSet = buildClientSideFeatureSet(features);
        mActiveIsolateSet = new HashSet<>();
        mState = State.ALIVE;
        mGuard.open("close");
        // This should be at the end of the constructor.
    }

    /**
     * Returns true if this JavaScriptSandbox is (or was) using a shared service process.
     * <p>
     * This may be called even after the sandbox has been closed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isShared() {
        return mConnection.getSandboxServiceInstance().getServiceType()
                == SandboxServiceInstance.SERVICE_TYPE_SHARED;
    }

    /**
     * Creates and returns a {@link JavaScriptIsolate} within which JS can be executed with default
     * settings.
     *
     * @return a new JavaScriptIsolate
     */
    @NonNull
    public JavaScriptIsolate createIsolate() {
        return createIsolate(new IsolateStartupParameters());
    }

    /**
     * Creates and returns a {@link JavaScriptIsolate} within which JS can be executed with the
     * specified settings.
     * <p>
     * If the sandbox is dead, this will still return an isolate, but evaluations will fail with
     * {@link SandboxDeadException}.
     *
     * @param settings the configuration for the isolate
     * @return a new JavaScriptIsolate
     */
    @NonNull
    public JavaScriptIsolate createIsolate(@NonNull IsolateStartupParameters settings) {
        Objects.requireNonNull(settings);
        synchronized (mLock) {
            JavaScriptIsolate isolate;
            switch (mState) {
                case ALIVE:
                    try {
                        isolate = JavaScriptIsolate.create(this, settings);
                    } catch (DeadObjectException e) {
                        killDueToException(e);
                        isolate = JavaScriptIsolate.createDead(this,
                                "sandbox found dead during call to createIsolate");
                    } catch (RemoteException | RuntimeException e) {
                        killDueToException(e);
                        throw Utils.exceptionToRuntimeException(e);
                    }
                    break;
                case DEAD:
                    isolate = JavaScriptIsolate.createDead(this,
                            "sandbox was dead before call to createIsolate");
                    break;
                case CLOSED:
                    throw new IllegalStateException("Cannot create isolate in closed sandbox");
                default:
                    throw new AssertionError("unreachable");
            }
            mActiveIsolateSet.add(isolate);
            return isolate;
        }
    }

    // In practice, this method should only be called whilst already holding mLock, but it is
    // called via JavaScriptIsolate and this constraint cannot be cleanly expressed via GuardedBy.
    IJsSandboxIsolate createIsolateOnService(@NonNull IsolateStartupParameters settings,
            @Nullable IJsSandboxIsolateClient isolateInstanceCallback) throws RemoteException {
        synchronized (mLock) {
            assert mState == State.ALIVE;
            if (isFeatureSupported(JavaScriptSandbox.JS_FEATURE_ISOLATE_CLIENT)) {
                return mJsSandboxService.createIsolate2(settings.getMaxHeapSizeBytes(),
                        isolateInstanceCallback);
            } else if (isFeatureSupported(JavaScriptSandbox.JS_FEATURE_ISOLATE_MAX_HEAP_SIZE)) {
                return mJsSandboxService.createIsolateWithMaxHeapSizeBytes(
                        settings.getMaxHeapSizeBytes());
            } else {
                return mJsSandboxService.createIsolate();
            }
        }
    }

    @NonNull
    private HashSet<String> buildClientSideFeatureSet(@NonNull List<String> features) {
        HashSet<String> featureSet = new HashSet<>();
        if (features.contains(IJsSandboxService.ISOLATE_TERMINATION)) {
            featureSet.add(JS_FEATURE_ISOLATE_TERMINATION);
        }
        if (features.contains(IJsSandboxService.WASM_FROM_ARRAY_BUFFER)) {
            featureSet.add(JS_FEATURE_PROMISE_RETURN);
            featureSet.add(JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER);
            featureSet.add(JS_FEATURE_WASM_COMPILATION);
        }
        if (features.contains(IJsSandboxService.ISOLATE_MAX_HEAP_SIZE_LIMIT)) {
            featureSet.add(JS_FEATURE_ISOLATE_MAX_HEAP_SIZE);
        }
        if (features.contains(IJsSandboxService.EVALUATE_WITHOUT_TRANSACTION_LIMIT)) {
            featureSet.add(JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT);
        }
        if (features.contains(IJsSandboxService.CONSOLE_MESSAGING)) {
            featureSet.add(JS_FEATURE_CONSOLE_MESSAGING);
        }
        if (features.contains(IJsSandboxService.ISOLATE_CLIENT)) {
            featureSet.add(JS_FEATURE_ISOLATE_CLIENT);
        }
        if (features.contains(IJsSandboxService.EVALUATE_FROM_FD)) {
            featureSet.add(JS_FEATURE_EVALUATE_FROM_FD);
        }
        if (features.contains(IJsSandboxService.MESSAGE_PORTS)) {
            featureSet.add(JS_FEATURE_MESSAGE_PORTS);
        }
        return featureSet;
    }

    /**
     * Checks whether a given feature is supported by the JS Sandbox implementation.
     * <p>
     * The sandbox implementation is provided by the version of WebView installed on the device.
     * The app must use this method to check which library features are supported by the device's
     * implementation before using them.
     * <p>
     * A feature check should be made prior to depending on certain features.
     *
     * @param feature the feature to be checked
     * @return {@code true} if supported, {@code false} otherwise
     */
    public boolean isFeatureSupported(@JsSandboxFeature @NonNull String feature) {
        Objects.requireNonNull(feature);
        return mClientSideFeatureSet.contains(feature);
    }

    void removeFromIsolateSet(@NonNull JavaScriptIsolate isolate) {
        synchronized (mLock) {
            mActiveIsolateSet.remove(isolate);
        }
    }

    /**
     * Closes the {@link JavaScriptSandbox} object and renders it unusable.
     *
     * <p>The client is expected to call this method explicitly to terminate the isolated process.
     *
     * <p>Once closed, no more {@link JavaScriptSandbox} and {@link JavaScriptIsolate} method calls
     * can be made unless stated by the method documentation.
     *
     * <p>For legacy and dedicated sandboxes, closing terminates the isolated process immediately
     * and all pending evaluations are immediately terminated.
     *
     * <p>For shared sandboxes, the process is only terminated once all shared
     * {@link JavaScriptSandbox} instances are closed. Note that isolates associated with this
     * JavaScriptSandbox will still become unusable once the sandbox is closed.
     *
     * <p>For legacy sandboxes, once closed, the client may call
     * {@link JavaScriptSandbox#createConnectedInstanceAsync(Context)} to create another
     * {@link JavaScriptSandbox}. You should still call close even if the sandbox has died,
     * otherwise you will not be able to create a new legacy sandbox.
     */
    @Override
    public void close() {
        mGuard.close();
        synchronized (mLock) {
            if (mState == State.CLOSED) {
                return;
            }
            mState = State.CLOSED;
            mConnection.close();
        }
        notifyIsolatesAboutClosure();
    }

    SandboxServiceInstance getSandboxServiceInstance() {
        return mConnection.getSandboxServiceInstance();
    }

    /**
     * Unbind from the service if it hasn't been unbound already.
     *
     * <p>By itself, this will not put the sandbox into an official dead state, but any subsequent
     * interaction with the sandbox will result in a DeadObjectException. As this method does NOT
     * trigger SandboxServiceConnection.onServiceDisconnected or .onBindingDied, it is also
     * useful for testing how methods handle DeadObjectException without a race against these
     * callbacks.
     *
     * <p>This method interferes with normal sandbox death handling. The sandbox may not
     * automatically enter into a dead state anymore.
     *
     * <p>This may not behave as expected for a shared sandbox. If the original service process is
     * still alive (and thus the binder is still alive), this method may be ineffective. Only use
     * this method for testing legacy or dedicated sandboxes; sparingly.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    public void unbindServiceForTesting() {
        mConnection.getSandboxServiceInstance().unbindAndUnregisterForTesting(mConnection);
    }

    /**
     * Unbind from the service and bring the sandbox/isolates into dead states on the calling
     * thread.
     *
     * <p>This can somewhat emulate a service death and subsequent handling.
     *
     * <p>Note that this bypasses the normal executor used for performing this logic.
     *
     * <p>This may not behave as expected for a shared sandbox. Other shared sandboxes may continue
     * working, though the unusual state of this JavaScriptSandbox might plausibly result in a later
     * service kill. Only use this method for testing legacy or dedicated sandboxes.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    public void unbindServiceAndEnterDeadStateForTesting() {
        unbindServiceForTesting();
        enterDeadState();
    }

    /**
     * Update state to reflect death and trigger callbacks/futures immediately on the calling
     * thread.
     *
     * <p>Safe to call multiple times and/or when closed.
     *
     * <p>This should only be called via onServiceKilled to avoid running callbacks under unexpected
     * locks, or directly for testing purposes only.
     */
    private void enterDeadState() {
        synchronized (mLock) {
            if (mState != State.ALIVE) {
                return;
            }
            mState = State.DEAD;
        }
        notifyIsolatesAboutDeath();
    }

    /**
     * Kill the sandbox and its service.
     *
     * <p>This will unbind the sandbox service so that any future IPC will fail immediately.
     * However, isolates will be notified asynchronously, from mContext's main executor.
     *
     * <p>Safe to call multiple times and/or when closed.
     */
    void killDueToException(@NonNull Exception e) {
        if (e instanceof DeadObjectException) {
            Log.e(TAG, "Sandbox died before or during remote call", e);
        } else {
            Log.e(TAG, "Killing sandbox due to exception", e);
        }
        // Schedules onServiceKilled if we were still bound.
        mConnection.killService(e);
    }

    /**
     * Handle an unplanned service death.
     *
     * <p>Safe to call multiple times and/or when closed.
     */
    void onServiceKilled() {
        getMainExecutor().execute(this::enterDeadState);
    }

    private void notifyIsolatesAboutClosure() {
        // Do not hold mLock whilst calling into JavaScriptIsolate, as JavaScriptIsolate also has
        // its own lock and may want to call into JavaScriptSandbox from another thread.
        final Set<JavaScriptIsolate> activeIsolateSet;
        synchronized (mLock) {
            activeIsolateSet = mActiveIsolateSet;
            mActiveIsolateSet = Collections.emptySet();
        }
        for (JavaScriptIsolate isolate : activeIsolateSet) {
            final TerminationInfo terminationInfo =
                    new TerminationInfo(TerminationInfo.STATUS_SANDBOX_DEAD, "sandbox closed");
            isolate.maybeSetIsolateDead(terminationInfo);
        }
    }

    private void notifyIsolatesAboutDeath() {
        // Do not hold mLock whilst calling into JavaScriptIsolate, as JavaScriptIsolate also has
        // its own lock and may want to call into JavaScriptSandbox from another thread.
        final JavaScriptIsolate[] activeIsolateSet;
        synchronized (mLock) {
            activeIsolateSet = mActiveIsolateSet.toArray(new JavaScriptIsolate[0]);
        }
        for (JavaScriptIsolate isolate : activeIsolateSet) {
            isolate.maybeSetSandboxDead();
        }
    }

    @Override
    @SuppressWarnings({"GenericException", "removal"}) // super.finalize() throws Throwable
    protected void finalize() throws Throwable {
        try {
            mGuard.warnIfOpen();
            close();
        } finally {
            super.finalize();
        }
    }

    @NonNull
    Executor getMainExecutor() {
        return ContextCompat.getMainExecutor(mContext);
    }
}
