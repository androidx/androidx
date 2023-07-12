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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolate;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxService;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;

/**
 * Sandbox that provides APIs for JavaScript evaluation in a restricted environment.
 * <p>
 * JavaScriptSandbox represents a connection to an isolated process. The isolated process is
 * exclusive to the calling app (i.e. it doesn't share anything with, and can't be compromised by
 * another app's isolated process).
 * <p>
 * Code that is run in a sandbox does not have any access to data
 * belonging to the original app unless explicitly passed into it by using the methods of this
 * class. This provides a security boundary between the calling app and the Javascript execution
 * environment.
 * <p>
 * The calling app can only have only one isolated process at a time, so only one
 * instance of this object can exist at a time.
 * <p>
 * It's safe to share a single {@link JavaScriptSandbox}
 * object with multiple threads and use it from multiple threads at once.
 * For example, {@link JavaScriptSandbox} can be stored at a global location and multiple threads
 * can create their own {@link JavaScriptIsolate} objects from it but the
 * {@link JavaScriptIsolate} object cannot be shared.
 */
public final class JavaScriptSandbox implements AutoCloseable {
    // TODO(crbug.com/1297672): Add capability to this class to support spawning
    // different processes as needed. This might require that we have a static
    // variable in here that tracks the existing services we are connected to and
    // connect to a different one when creating a new object.
    private static final String TAG = "JavaScriptSandbox";
    private static final String JS_SANDBOX_SERVICE_NAME =
            "org.chromium.android_webview.js_sandbox.service.JsSandboxService0";

    static AtomicBoolean sIsReadyToConnect = new AtomicBoolean(true);
    private final Object mLock = new Object();
    private CloseGuardHelper mGuard = CloseGuardHelper.create();

    @Nullable
    @GuardedBy("mLock")
    private IJsSandboxService mJsSandboxService;

    private final ConnectionSetup mConnection;

    @Nullable
    @GuardedBy("mLock")
    private HashSet<JavaScriptIsolate> mActiveIsolateSet = new HashSet<JavaScriptIsolate>();

    final ExecutorService mThreadPoolTaskExecutor =
            Executors.newCachedThreadPool(new ThreadFactory() {
                private final AtomicInteger mCount = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "JavaScriptSandbox Thread #" + mCount.getAndIncrement());
                }
            });

    /**
     *
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
     * evaluation and close the isolate. If it is absent, {@link JavaScriptIsolate#close()} cannot
     * terminate any running or queued evaluations in the background, so the isolate continues to
     * consume resources until they complete.
     * <p>
     * Irrespective of this feature, calling {@link JavaScriptSandbox#close()} terminates all
     * {@link JavaScriptIsolate} objects (and the isolated process) immediately and all pending
     * {@link JavaScriptIsolate#evaluateJavaScriptAsync(String)} futures resolve with {@link
     * IsolateTerminatedException}.
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

    @GuardedBy("mLock")
    @Nullable
    private HashSet<String> mClientSideFeatureSet;

    static class ConnectionSetup implements ServiceConnection {
        @Nullable
        private CallbackToFutureAdapter.Completer<JavaScriptSandbox> mCompleter;
        @Nullable
        private JavaScriptSandbox mJsSandbox;
        Context mContext;

        @Override
        @SuppressWarnings("NullAway")
        public void onServiceConnected(ComponentName name, IBinder service) {
            // It's possible for the service to die and already have been restarted before
            // we've actually observed the original death (b/267864650). If that happens,
            // onServiceConnected will be called a second time immediately after
            // onServiceDisconnected even though we already unbound. Just do nothing.
            if (mCompleter == null) {
                return;
            }
            IJsSandboxService jsSandboxService =
                    IJsSandboxService.Stub.asInterface(service);
            mJsSandbox = new JavaScriptSandbox(this, jsSandboxService);
            mCompleter.set(mJsSandbox);
            mCompleter = null;
        }

        // TODO(crbug.com/1297672): We may want an explicit way to signal to the client that the
        // process crashed (like onRenderProcessGone in WebView), without them having to first call
        // one of the methods and have it fail.
        @Override
        public void onServiceDisconnected(ComponentName name) {
            runShutdownTasks(new RuntimeException(
                    "JavaScriptSandbox internal error: onServiceDisconnected()"));
        }

        @Override
        public void onBindingDied(ComponentName name) {
            runShutdownTasks(
                    new RuntimeException("JavaScriptSandbox internal error: onBindingDead()"));
        }

        @Override
        public void onNullBinding(ComponentName name) {
            runShutdownTasks(
                    new RuntimeException("JavaScriptSandbox internal error: onNullBinding()"));
        }

        private void runShutdownTasks(@NonNull Exception e) {
            if (mJsSandbox != null) {
                mJsSandbox.close();
            } else {
                mContext.unbindService(this);
                sIsReadyToConnect.set(true);
            }
            if (mCompleter != null) {
                mCompleter.setException(e);
            }
            mCompleter = null;
        }

        ConnectionSetup(@NonNull Context context,
                @NonNull CallbackToFutureAdapter.Completer<JavaScriptSandbox> completer) {
            mContext = context;
            mCompleter = completer;
        }
    }

    /**
     * Asynchronously create and connect to the sandbox process.
     * <p>
     * Only one sandbox process can exist at a time. Attempting to create a new instance before
     * the previous instance has been closed fails with an {@link IllegalStateException}.
     * <p>
     * Sandbox support should be checked using {@link JavaScriptSandbox#isSupported()} before
     * attempting to create a sandbox via this method.
     *
     * @param context When the context is destroyed, the connection is closed. Use an
     *                application
     *                context if the connection is expected to outlive a single activity or service.
     * @return Future that evaluates to a connected {@link JavaScriptSandbox} instance or an
     * exception if binding to service fails.
     */
    @NonNull
    public static ListenableFuture<JavaScriptSandbox> createConnectedInstanceAsync(
            @NonNull Context context) {
        Objects.requireNonNull(context);
        if (!isSupported()) {
            throw new SandboxUnsupportedException("The system does not support JavaScriptSandbox");
        }
        PackageInfo systemWebViewPackage = WebView.getCurrentWebViewPackage();
        ComponentName compName =
                new ComponentName(systemWebViewPackage.packageName, JS_SANDBOX_SERVICE_NAME);
        int flag = Context.BIND_AUTO_CREATE | Context.BIND_EXTERNAL_SERVICE;
        return bindToServiceWithCallback(context, compName, flag);
    }

    /**
     * Asynchronously create and connect to the sandbox process for testing.
     * <p>
     * Only one sandbox process can exist at a time. Attempting to create a new instance before
     * the previous instance has been closed will fail with an {@link IllegalStateException}.
     *
     * @param context When the context is destroyed, the connection will be closed. Use an
     *                application
     *                context if the connection is expected to outlive a single activity/service.
     * @return Future that evaluates to a connected {@link JavaScriptSandbox} instance or an
     * exception if binding to service fails.
     */
    @NonNull
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static ListenableFuture<JavaScriptSandbox> createConnectedInstanceForTestingAsync(
            @NonNull Context context) {
        Objects.requireNonNull(context);
        ComponentName compName = new ComponentName(context, JS_SANDBOX_SERVICE_NAME);
        int flag = Context.BIND_AUTO_CREATE;
        return bindToServiceWithCallback(context, compName, flag);
    }

    /**
     * Check if JavaScriptSandbox is supported on the system.
     * <p>
     * This method should be used to check for sandbox support before calling
     * {@link JavaScriptSandbox#createConnectedInstanceAsync(Context)}.
     *
     * @return true if JavaScriptSandbox is supported and false otherwise.
     */
    public static boolean isSupported() {
        PackageInfo systemWebViewPackage = WebView.getCurrentWebViewPackage();
        if (systemWebViewPackage == null) {
            return false;
        }
        long versionCode = PackageInfoCompat.getLongVersionCode(systemWebViewPackage);
        // The current IPC interface was introduced in 102.0.4976.0 (crrev.com/3560402), so all
        // versions above that are supported. Additionally, the relevant IPC changes were
        // cherry-picked into M101 at 101.0.4951.24 (crrev.com/3568575), so versions between
        // 101.0.4951.24 inclusive and 102.0.4952.0 exclusive are also supported.
        return versionCode >= 4976_000_00L
                || (4951_024_00L <= versionCode && versionCode < 4952_000_00L);
    }

    @NonNull
    private static ListenableFuture<JavaScriptSandbox> bindToServiceWithCallback(
            @NonNull Context context, @NonNull ComponentName compName, int flag) {
        Intent intent = new Intent();
        intent.setComponent(compName);
        return CallbackToFutureAdapter.getFuture(completer -> {
            ConnectionSetup connectionSetup = new ConnectionSetup(context, completer);
            if (sIsReadyToConnect.compareAndSet(true, false)) {
                try {
                    boolean isBinding = context.bindService(intent, connectionSetup, flag);
                    if (isBinding) {
                        Executor mainExecutor;
                        mainExecutor = ContextCompat.getMainExecutor(context);
                        completer.addCancellationListener(
                                () -> {
                                    context.unbindService(connectionSetup);
                                }, mainExecutor);
                    } else {
                        context.unbindService(connectionSetup);
                        sIsReadyToConnect.set(true);
                        completer.setException(
                                new RuntimeException("bindService() returned false " + intent));
                    }
                } catch (SecurityException e) {
                    context.unbindService(connectionSetup);
                    sIsReadyToConnect.set(true);
                    completer.setException(e);
                }
            } else {
                completer.setException(
                        new IllegalStateException("Binding to already bound service"));
            }

            // Debug string.
            return "JavaScriptSandbox Future";
        });
    }

    // We prevent direct initializations of this class.
    // Use JavaScriptSandbox.createConnectedInstance().
    JavaScriptSandbox(@NonNull ConnectionSetup connectionSetup,
            @NonNull IJsSandboxService jsSandboxService) {
        mConnection = connectionSetup;
        synchronized (mLock) {
            mJsSandboxService = jsSandboxService;
        }
        mGuard.open("close");
        // This should be at the end of the constructor.
    }

    /**
     * Creates and returns an {@link JavaScriptIsolate} within which JS can be executed with default
     * settings.
     */
    @NonNull
    public JavaScriptIsolate createIsolate() {
        return createIsolate(new IsolateStartupParameters());
    }

    /**
     * Creates and returns an {@link JavaScriptIsolate} within which JS can be executed with the
     * specified settings.
     *
     * @param settings configuration used to set up the isolate
     */
    @NonNull
    public JavaScriptIsolate createIsolate(@NonNull IsolateStartupParameters settings) {
        Objects.requireNonNull(settings);
        synchronized (mLock) {
            if (mJsSandboxService == null) {
                throw new IllegalStateException(
                        "Attempting to createIsolate on a service that isn't connected");
            }
            IJsSandboxIsolate isolateStub;
            try {
                if (settings.getMaxHeapSizeBytes()
                        == IsolateStartupParameters.DEFAULT_ISOLATE_HEAP_SIZE) {
                    isolateStub = mJsSandboxService.createIsolate();
                } else {
                    isolateStub = mJsSandboxService.createIsolateWithMaxHeapSizeBytes(
                            settings.getMaxHeapSizeBytes());
                    if (isolateStub == null) {
                        throw new RuntimeException(
                                "Service implementation doesn't support setting maximum heap size");
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            return createJsIsolateLocked(isolateStub, settings);
        }
    }

    @GuardedBy("mLock")
    @SuppressWarnings("NullAway")
    private void populateClientFeatureSet() {
        List<String> features;
        try {
            features = mJsSandboxService.getSupportedFeatures();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        mClientSideFeatureSet = new HashSet<String>();
        if (features.contains(IJsSandboxService.ISOLATE_TERMINATION)) {
            mClientSideFeatureSet.add(JS_FEATURE_ISOLATE_TERMINATION);
        }
        if (features.contains(IJsSandboxService.WASM_FROM_ARRAY_BUFFER)) {
            mClientSideFeatureSet.add(JS_FEATURE_PROMISE_RETURN);
            mClientSideFeatureSet.add(JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER);
            mClientSideFeatureSet.add(JS_FEATURE_WASM_COMPILATION);
        }
        if (features.contains(IJsSandboxService.ISOLATE_MAX_HEAP_SIZE_LIMIT)) {
            mClientSideFeatureSet.add(JS_FEATURE_ISOLATE_MAX_HEAP_SIZE);
        }
        if (features.contains(IJsSandboxService.EVALUATE_WITHOUT_TRANSACTION_LIMIT)) {
            mClientSideFeatureSet.add(JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT);
        }
        if (features.contains(IJsSandboxService.CONSOLE_MESSAGING)) {
            mClientSideFeatureSet.add(JS_FEATURE_CONSOLE_MESSAGING);
        }
    }

    @GuardedBy("mLock")
    @NonNull
    @SuppressWarnings("NullAway")
    private JavaScriptIsolate createJsIsolateLocked(@NonNull IJsSandboxIsolate isolateStub,
            @NonNull IsolateStartupParameters settings) {
        JavaScriptIsolate isolate = new JavaScriptIsolate(isolateStub, this, settings);
        mActiveIsolateSet.add(isolate);
        return isolate;
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
     * @param feature feature to be checked
     * @return {@code true} if supported, {@code false} otherwise
     */
    @SuppressWarnings("NullAway")
    public boolean isFeatureSupported(@JsSandboxFeature @NonNull String feature) {
        Objects.requireNonNull(feature);
        synchronized (mLock) {
            if (mJsSandboxService == null) {
                throw new IllegalStateException(
                        "Attempting to check features on a service that isn't connected");
            }
            if (mClientSideFeatureSet == null) {
                populateClientFeatureSet();
            }
            return mClientSideFeatureSet.contains(feature);
        }
    }

    void removeFromIsolateSet(@NonNull JavaScriptIsolate isolate) {
        synchronized (mLock) {
            if (mActiveIsolateSet != null) {
                mActiveIsolateSet.remove(isolate);
            }
        }
    }

    /**
     * Closes the {@link JavaScriptSandbox} object and renders it unusable.
     * <p>
     * The client is expected to call this method explicitly to terminate the isolated process.
     * <p>
     * Once closed, no more {@link JavaScriptSandbox} and {@link JavaScriptIsolate} method calls
     * can be made. Closing terminates the isolated process immediately. All pending evaluations are
     * immediately terminated. Once closed, the client may call
     * {@link JavaScriptSandbox#createConnectedInstanceAsync(Context)} to create another
     * {@link JavaScriptSandbox}.
     */
    @Override
    public void close() {
        synchronized (mLock) {
            if (mJsSandboxService == null) {
                return;
            }
            // This is the closest thing to a .close() method for ExecutorServices. This doesn't
            // force the threads or their Runnables to immediately terminate, but will ensure
            // that once the
            // worker threads finish their current runnable (if any) that the thread pool terminates
            // them, preventing a leak of threads.
            mThreadPoolTaskExecutor.shutdownNow();
            notifyIsolatesAboutClosureLocked();
            mConnection.mContext.unbindService(mConnection);
            // Currently we consider that we are ready for a new connection once we unbind. This
            // might not be true if the process is not immediately killed by ActivityManager once it
            // is unbound.
            sIsReadyToConnect.set(true);
            mJsSandboxService = null;
        }
    }

    @GuardedBy("mLock")
    private void notifyIsolatesAboutClosureLocked() {
        for (JavaScriptIsolate ele : mActiveIsolateSet) {
            ele.notifySandboxClosed();
        }
        mActiveIsolateSet = null;
    }

    @Override
    @SuppressWarnings("GenericException") // super.finalize() throws Throwable
    protected void finalize() throws Throwable {
        try {
            if (mGuard != null) {
                mGuard.warnIfOpen();
            }
            synchronized (mLock) {
                if (mJsSandboxService != null) {
                    close();
                }
            }
        } finally {
            super.finalize();
        }
    }

    @NonNull
    Executor getMainExecutor() {
        return ContextCompat.getMainExecutor(mConnection.mContext);
    }
}
