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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Coordinates {@link SandboxServiceConnection} instances to a js_sandbox service instance.
 *
 * <p>This class particularly plays a role in handling sandbox crashes or forced kills. After a
 * crash or kill, no more connections can be associated with a service instance.
 *
 * <p>It is used for legacy, shared, and dedicated sandboxes, though legacy and dedicated sandboxes
 * only ever use one SandboxServiceConnection for the lifetime of the SandboxServiceInstance.
 *
 * <p>The OS ordinarily abstracts away the service process's lifecycle. This can be inconvenient
 * when deliberately trying to manage the lifecycle, e.g. to kill a misbehaving service.
 * SandboxServiceInstances track whether all of their connections get closed and, if so, become
 * inactive to prevent reuse (and incidental state carry-over) across different process lifecycles.
 */
// TODO(b/563285741): Investigate rearchitecting to use a single shared underlying ServiceConnection
//   for all JavaScriptSandbox instances associated with a SandboxServiceInstance.
@ThreadSafe
final class SandboxServiceInstance {
    // Using "JavaScriptSandbox" instead of "SandboxServiceInstance" is more meaningful to
    // developers and in line with historical behavior.
    private static final String TAG = "JavaScriptSandbox";
    private static final String JS_SANDBOX_SERVICE_NAME =
            "org.chromium.android_webview.js_sandbox.service.JsSandboxService0";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            SERVICE_TYPE_LEGACY,
            SERVICE_TYPE_SHARED,
            SERVICE_TYPE_DEDICATED,
    })
    public @interface ServiceType {
    }

    public static final int SERVICE_TYPE_LEGACY = 0;
    public static final int SERVICE_TYPE_SHARED = 1;
    public static final int SERVICE_TYPE_DEDICATED = 2;

    private static final Executor sSharedInstanceExecutor =
            ThreadPool.getInstance().createSequentialExecutor();

    // Do not directly execute callbacks under this lock. Use a non-direct executor.
    // (Beware, a lock may be held at an unexpected place in the call stack.)
    private static final Object sLock = new Object();
    // There is at most one active legacy SandboxServiceInstance at any time.
    @GuardedBy("sLock")
    @Nullable
    private static SandboxServiceInstance sLegacySandboxServiceInstance = null;
    // There is at most one active shared SandboxServiceInstance at any time.
    @GuardedBy("sLock")
    @Nullable
    private static SandboxServiceInstance sSharedSandboxServiceInstance = null;
    @GuardedBy("sLock")
    private static long sNextDedicatedSandboxId = 0;

    // The executor used for handling connection and service lifecycle updates.
    // This must be sequential in nature (e.g. the main looper executor or a SequencedExecutor).
    @NonNull
    private final Executor mExecutor;
    // Context used for managing and listening to service lifecycle.
    // This may be a different Context to the one used for JavaScriptSandbox instances,
    // particularly for shared sandboxes where the application context is used here, but the
    // JavaScriptSandbox might use a ContextWrapper or Activity with subtly different behaviors.
    @NonNull
    private final Context mContext;
    @NonNull
    private final Intent mIntent;
    // The instance name used with bindIsolatedService. Null for legacy sandboxes spawned using
    // bindService via JavaScriptSandbox#createConnectedInstanceAsync().
    @Nullable
    private final String mInstanceName;
    // Flags for bindService or bindIsolatedService.
    private final int mBindFlags;
    // Controls special handling of service lifetime and management of global service instances.
    private final @ServiceType int mServiceType;

    // The set of active SandboxServiceConnections for this instance.
    @GuardedBy("sLock")
    private final Set<SandboxServiceConnection> mSandboxServiceConnections = new HashSet<>();
    // Whether it is valid for SandboxServiceConnections to continue using this service.
    // A killed or obsoleted SandboxServiceInstance becomes inactive.
    // This doesn't technically track whether the service or process is actually up, as the service
    // lifecycle is abstracted by the OS.
    @GuardedBy("sLock")
    private boolean mActive = true;

    /**
     * Check if JavaScriptEngine is supported on the system.
     *
     * <p>This method should be used to check for sandbox support before connecting to a service
     * instance.
     *
     * @return true if JavaScriptEngine is supported and false otherwise.
     */
    public static boolean isSandboxSupported() {
        return isSandboxSupported(WebView.getCurrentWebViewPackage());
    }

    private static boolean isSandboxSupported(@Nullable PackageInfo systemWebViewPackage) {
        if (systemWebViewPackage == null) {
            return false;
        }
        long versionCode = PackageInfoCompat.getLongVersionCode(systemWebViewPackage);
        // Version "104.0.5112.0" which is associated with crrev.com/c/3695978.
        // See b/537461418 for more details.
        return versionCode >= 5112_000_00L;
    }

    /**
     * Creates a JavaScriptSandbox attached to a new legacy SandboxServiceInstance.
     */
    @NonNull
    public static ListenableFuture<JavaScriptSandbox> createLegacySandbox(
            @NonNull Context context) {
        PackageInfo systemWebViewPackage = WebView.getCurrentWebViewPackage();
        // Historically a synchronous exception. Check up-front.
        if (!isSandboxSupported(systemWebViewPackage)) {
            throw new SandboxUnsupportedException("The system does not support JavaScriptSandbox");
        }
        return CallbackToFutureAdapter.getFuture(completer -> {
            Executor executor = ContextCompat.getMainExecutor(context);
            synchronized (sLock) {
                if (sLegacySandboxServiceInstance != null) {
                    throw new IllegalStateException(
                            "Cannot create two concurrent legacy sandboxes. Close the existing "
                                    + "JavaScriptSandbox first.");
                }
                sLegacySandboxServiceInstance = createServiceInstance(executor, context,
                        /*instanceName=*/null, SERVICE_TYPE_LEGACY, systemWebViewPackage);
                sLegacySandboxServiceInstance.openConnectionLocked(context, completer);
            }
            // Debug string.
            return "JavaScriptSandbox Future";
        });
    }

    /**
     * Creates a JavaScriptSandbox attached to a new testing legacy SandboxServiceInstance.
     */
    @NonNull
    public static ListenableFuture<JavaScriptSandbox> createLegacySandboxForTesting(
            @NonNull Context context) {
        // Don't do a support check for this ForTesting method. It doesn't use the system's WebView
        // provider.
        return CallbackToFutureAdapter.getFuture(completer -> {
            Executor executor = ContextCompat.getMainExecutor(context);
            synchronized (sLock) {
                if (sLegacySandboxServiceInstance != null) {
                    throw new IllegalStateException(
                            "Cannot create two concurrent legacy sandboxes. Close the existing "
                                    + "JavaScriptSandbox first.");
                }
                ComponentName componentName = new ComponentName(context, JS_SANDBOX_SERVICE_NAME);
                int bindFlags = Context.BIND_AUTO_CREATE;
                sLegacySandboxServiceInstance = new SandboxServiceInstance(executor, context,
                        componentName, /*instanceName=*/null, bindFlags, SERVICE_TYPE_LEGACY);
                sLegacySandboxServiceInstance.openConnectionLocked(context, completer);
            }
            // Debug string.
            return "JavaScriptSandbox Future";
        });
    }

    /**
     * Create a JavaScriptSandbox attached to a shared service.
     *
     * <p>If one exists, uses the existing SandboxServiceInstance for a shared sandbox service, or
     * creates one if there is no currently active shared SandboxServiceInstance.
     */
    @NonNull
    @RequiresApi(Build.VERSION_CODES.Q)
    public static ListenableFuture<JavaScriptSandbox> createSharedSandbox(
            @NonNull Context context) {
        PackageInfo systemWebViewPackage = WebView.getCurrentWebViewPackage();
        // Historically a synchronous exception. Check up-front.
        if (!isSandboxSupported(systemWebViewPackage)) {
            throw new SandboxUnsupportedException("The system does not support JavaScriptSandbox");
        }
        return CallbackToFutureAdapter.getFuture(completer -> {
            synchronized (sLock) {
                if (sSharedSandboxServiceInstance == null) {
                    String instanceName = "s";
                    sSharedSandboxServiceInstance = createServiceInstance(sSharedInstanceExecutor,
                            context.getApplicationContext(), instanceName, SERVICE_TYPE_SHARED,
                            systemWebViewPackage);
                }
                sSharedSandboxServiceInstance.openConnectionLocked(context, completer);
            }
            // Debug string.
            return "JavaScriptSandbox Future";
        });
    }

    /**
     * Create a JavaScriptSandbox attached to a new dedicated service.
     */
    @NonNull
    @RequiresApi(Build.VERSION_CODES.Q)
    public static ListenableFuture<JavaScriptSandbox> createDedicatedSandbox(
            @NonNull Context context) {
        PackageInfo systemWebViewPackage = WebView.getCurrentWebViewPackage();
        // Historically a synchronous exception. Check up-front.
        if (!isSandboxSupported(systemWebViewPackage)) {
            throw new SandboxUnsupportedException("The system does not support JavaScriptSandbox");
        }
        return CallbackToFutureAdapter.getFuture(completer -> {
            Executor executor = ThreadPool.getInstance().createSequentialExecutor();
            synchronized (sLock) {
                String instanceName = "d." + sNextDedicatedSandboxId++;
                SandboxServiceInstance sandboxServiceInstance =
                        createServiceInstance(executor, context, instanceName,
                                SERVICE_TYPE_DEDICATED, systemWebViewPackage);
                sandboxServiceInstance.openConnectionLocked(context, completer);
            }
            // Debug string.
            return "JavaScriptSandbox Future";
        });
    }

    @NonNull
    private static SandboxServiceInstance createServiceInstance(
            @NonNull Executor executor, @NonNull Context context, @Nullable String instanceName,
            @ServiceType int serviceType, @NonNull PackageInfo systemWebViewPackage) {
        // Double check these, in case a ContextWrapper has given us nulls.
        Objects.requireNonNull(executor);
        Objects.requireNonNull(context);
        ComponentName componentName =
                new ComponentName(systemWebViewPackage.packageName, JS_SANDBOX_SERVICE_NAME);
        int bindFlags = Context.BIND_AUTO_CREATE | Context.BIND_EXTERNAL_SERVICE;

        return new SandboxServiceInstance(executor, context, componentName, instanceName, bindFlags,
                serviceType);
    }

    /**
     * Creates a SandboxServiceInstance.
     *
     * @param executor      The executor to use for service connection and lifecycle notifications.
     *                      This must be the main looper executor for legacy sandboxes, otherwise a
     *                      sequenced executor.
     * @param context       The context to use for binding/unbinding to/from the service.
     * @param componentName The component name of the service.
     * @param instanceName  The instance name to use with bindIsolatedService. Must be null for
     *                      legacy sandboxes (using bindService) and non-null for a "shared" or
     *                      "dedicated" sandbox (using bindIsolatedService). Must be unique during
     *                      the lifetime of the SandboxServiceInstance.
     * @param bindFlags     The flags to use with bindService or bindIsolatedService.
     * @param serviceType   Whether this is a legacy, shared, or dedicated sandbox.
     */
    private SandboxServiceInstance(@NonNull Executor executor, @NonNull Context context,
            @NonNull ComponentName componentName, @Nullable String instanceName, int bindFlags,
            @ServiceType int serviceType) {
        if ((instanceName == null) != (serviceType == SERVICE_TYPE_LEGACY)) {
            throw new AssertionError("instanceName nullness and serviceType mismatch.");
        }
        mExecutor = executor;
        mContext = context;
        mInstanceName = instanceName;
        mBindFlags = bindFlags;
        mServiceType = serviceType;
        mIntent = new Intent();
        mIntent.setComponent(componentName);
    }

    /**
     * Attempt to bind to a sandbox service and resolve the {@link JavaScriptSandbox} completer.
     *
     * <p>If binding succeeds and a service connection is established, the completer is set to a
     * JavaScriptSandbox, else an exception is set and the SandboxServiceConnection is closed.
     *
     * @param context                the context to create a JavaScriptSandbox with.
     * @param completer              the completer to set.
     */
    @GuardedBy("sLock")
    private void openConnectionLocked(@NonNull Context context,
            CallbackToFutureAdapter.@NonNull Completer<JavaScriptSandbox> completer) {
        // Synchronous exceptions will fail the completer implicitly before any executor-based
        // completion can run.
        if (!mActive) {
            // This should not be possible if using sLock properly.
            throw new AssertionError("The SandboxServiceInstance is dead.");
        }
        if (mServiceType != SERVICE_TYPE_SHARED && !mSandboxServiceConnections.isEmpty()) {
            throw new AssertionError(
                    "Only shared services can have multiple connections.");
        }

        SandboxServiceConnection sandboxServiceConnection = new SandboxServiceConnection(
                context, this, completer);
        mSandboxServiceConnections.add(sandboxServiceConnection);

        boolean isBinding = false;
        Exception killException = null;
        // TODO(b/571953853):
        //   Consider offloading the actual bind call to the executor or background thread.
        try {
            if (mServiceType == SERVICE_TYPE_LEGACY) {
                isBinding = mContext.bindService(mIntent, sandboxServiceConnection,
                        mBindFlags);
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    mSandboxServiceConnections.remove(sandboxServiceConnection);
                    throw new UnsupportedOperationException(
                            "Shared/dedicated sandboxes require API level >= 29");
                }
                isBinding = Api29Impl.bindIsolatedService(mContext, mIntent, mBindFlags,
                        Objects.requireNonNull(mInstanceName), mExecutor,
                        sandboxServiceConnection);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException whilst binding.", e);
            killException = e;
        } finally {
            if (!isBinding) {
                Log.e(TAG, "bindService/bindIsolatedService failed. intent=" + mIntent);

                // There may be something wrong with the service, so kill it. This will unbind and
                // remove this connection from mSandboxServiceConnections if needed.
                if (killException == null) {
                    killException = new RuntimeException("Failed to bind to service.");
                }
                kill(killException);
                sandboxServiceConnection.close();
            }
        }
    }

    /**
     * Unbinds and unregisters a connection with this service instance.
     *
     * <p>The SandboxServiceConnection will no longer receive updates about the state of the
     * service.
     *
     * <p>It is illegal to unbind/unregister a connection that is not registered with the instance.
     *
     * <p>Unbinding the last connection will deactivate the service to avoid reuse across multiple
     * service process lifetimes.
     */
    @GuardedBy("sLock")
    private void unbindAndUnregisterLocked(
            @NonNull SandboxServiceConnection sandboxServiceConnection) {
        boolean wasRemoved = mSandboxServiceConnections.remove(sandboxServiceConnection);
        if (!wasRemoved) {
            throw new AssertionError("SandboxServiceConnection is not registered.");
        }
        mContext.unbindService(sandboxServiceConnection);

        if (mSandboxServiceConnections.isEmpty()) {
            // There are no connections, so the service process is going to be torn down.
            // Deactivate this service instance for the following reasons:
            // - Legacy and dedicated sandboxes can only be used once anyway.
            // - New shared sandboxes will begin using a new service process. Ensuring they use a
            //   fresh SandboxServiceInstance means stray kills from old sandboxes won't take down
            //   the unrelated new process.
            deactivateLocked();
        }
    }

    /**
     * Close a specific connection without affecting other connections.
     *
     * <p>This may safely be called multiple times for a connection.
     *
     * <p>If called for a legacy service, a new legacy service instance can then be created.
     *
     * <p>The SandboxServiceConnection will no longer receive updates about the state of the
     * service.
     *
     * @param sandboxServiceConnection The connection to close.
     */
    public void closeConnection(@NonNull SandboxServiceConnection sandboxServiceConnection) {
        Objects.requireNonNull(sandboxServiceConnection);
        synchronized (sLock) {
            if (mSandboxServiceConnections.contains(sandboxServiceConnection)) {
                unbindAndUnregisterLocked(sandboxServiceConnection);
            }

            if (mServiceType == SERVICE_TYPE_LEGACY) {
                if (mActive) {
                    throw new AssertionError("Should have been deactivated by now.");
                }
                if (this == sLegacySandboxServiceInstance) {
                    sLegacySandboxServiceInstance = null;
                }
            }
        }

        mExecutor.execute(sandboxServiceConnection::cleanup);
    }

    /**
     * Deactivate the service instance.
     *
     * <p>This will unset the shared sandbox instance global if this is an instance for it.
     *
     * <p>This should be called after unbinding all connections, but before any kill or cleanup
     * callbacks are triggered for them.
     */
    @GuardedBy("sLock")
    private void deactivateLocked() {
        if (!mSandboxServiceConnections.isEmpty()) {
            throw new AssertionError(
                    "SandboxServiceInstance should not have any connections when deactivated.");
        }
        if (!mActive) {
            return;
        }

        mActive = false;

        // Legacy sandboxes require explicit closing. Do not unset the global on mere deactivation.
        // Only shared sandboxes are handled here.
        if (mServiceType == SERVICE_TYPE_SHARED) {
            if (this != sSharedSandboxServiceInstance) {
                throw new AssertionError("Deactivated untracked shared instance.");
            }
            sSharedSandboxServiceInstance = null;
        }
    }

    /**
     * Unbind all connections, deactivate the service, and notify all sandboxes.
     *
     * <p>Any further bind/connection attempts will be refused, and all active
     * {@link SandboxServiceConnection} instances will be unbound from the service immediately.
     *
     * <p>May safely be called multiple times.
     *
     * <p>If the service instance was previously active, any associated connection instances, and in
     * turn their {@link JavaScriptSandbox} instances, will be notified about the kill.
     *
     * @param e The cause of the kill.
     */
    public void kill(@NonNull Exception e) {
        Objects.requireNonNull(e);
        List<SandboxServiceConnection> sandboxServiceConnections;

        synchronized (sLock) {
            if (!mActive) {
                return;
            }

            Log.e(TAG, "Sandbox service killed: ", e);

            sandboxServiceConnections = new ArrayList<>(mSandboxServiceConnections);
            for (SandboxServiceConnection sandboxServiceConnection : sandboxServiceConnections) {
                unbindAndUnregisterLocked(sandboxServiceConnection);
            }

            if (mActive) {
                throw new AssertionError("Should have been deactivated by now.");
            }
        }

        // Safeguard against propagating exception messages across potentially unrelated sandboxes.
        // Current stack trace is OK, but messages could potentially contain sensitive data from an
        // isolate belonging to a JavaScriptSandbox owned by a different JavaScriptEngine consumer.
        final Exception propagatedException = getServiceType() == SERVICE_TYPE_SHARED
                ? new RuntimeException("Shared sandbox service killed.") : e;
        for (SandboxServiceConnection sandboxServiceConnection : sandboxServiceConnections) {
            mExecutor.execute(() -> sandboxServiceConnection.onKilled(propagatedException));
        }
    }

    /**
     * Returns the type of this service.
     */
    public @ServiceType int getServiceType() {
        return mServiceType;
    }

    /**
     * Returns true if this SandboxServiceInstance is active.
     *
     * <p>A killed or obsoleted SandboxServiceInstance becomes inactive.
     */
    public boolean isActive() {
        synchronized (sLock) {
            return mActive;
        }
    }

    /**
     * Unbind and forget about a connection, but don't trigger any callbacks.
     *
     * <p>This vaguely emulates a service death for legacy and dedicated sandboxes.
     *
     * <p>Note that this will inhibit any later kill notification from this service.
     */
    @VisibleForTesting
    public void unbindAndUnregisterForTesting(
            @NonNull SandboxServiceConnection sandboxServiceConnection) {
        Objects.requireNonNull(sandboxServiceConnection);
        synchronized (sLock) {
            unbindAndUnregisterLocked(sandboxServiceConnection);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static final class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean bindIsolatedService(@NonNull Context context, @NonNull Intent service,
                int flags, @NonNull String instanceName, @NonNull Executor executor,
                @NonNull ServiceConnection conn) {
            return context.bindIsolatedService(service, flags, instanceName, executor, conn);
        }
    }
}
