/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.trusted;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.TransactionTooLargeException;
import android.support.customtabs.trusted.ITrustedWebActivityService;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A TrustedWebActivityServiceConnectionManager will be used by a Trusted Web Activity provider and
 * takes care of connecting to and communicating with {@link TrustedWebActivityService}s.
 * <p>
 * Trusted Web Activity client apps are registered with {@link #registerClient}, associating a
 * package with an origin. There may be multiple packages associated with a single origin.
 * Note, the origins are essentially keys to a map of origin to package name - while they
 * semantically are web origins, they aren't used that way.
 * <p>
 * To interact with a {@link TrustedWebActivityService}, call {@link #execute}.
 */
public class TrustedWebActivityServiceConnectionManager {
    private static final String TAG = "TWAConnectionManager";
    private static final String PREFS_FILE = "TrustedWebActivityVerifiedPackages";

    /**
     * A callback to be executed once a connection to a {@link TrustedWebActivityService} is open.
     */
    public interface ExecutionCallback {
        /**
         * Is run when a connection is open. See {@link #execute} for more information.
         * @param service A {@link TrustedWebActivityServiceWrapper} wrapping the connected
         *                {@link TrustedWebActivityService}.
         *                It may be null if the connection failed.
         * @throws RemoteException May be thrown by {@link TrustedWebActivityServiceWrapper}'s
         *                         methods. If the developer does not want to catch them, they will
         *                         be caught gracefully by {@link #execute}.
         */
        @SuppressLint("RethrowRemoteException")  // We're accepting RemoteExceptions not throwing.
        void onConnected(@Nullable TrustedWebActivityServiceWrapper service) throws RemoteException;
    }

    /** The callback used internally that will wrap an ExecutionCallback. */
    private interface WrappedCallback {
        void onConnected(@Nullable TrustedWebActivityServiceWrapper service);
    }

    /**
     * Holds a connection to a TrustedWebActivityService.
     * It should only be used on the UI Thread.
     */
    private class Connection implements ServiceConnection {
        private TrustedWebActivityServiceWrapper mService;
        private List<WrappedCallback> mCallbacks = new LinkedList<>();
        private final Uri mScope;

        Connection(Uri scope) {
            mScope = scope;
        }

        public TrustedWebActivityServiceWrapper getService() {
            return mService;
        }

        /** This method will be called on the UI Thread by the Android Framework. */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = new TrustedWebActivityServiceWrapper(
                    ITrustedWebActivityService.Stub.asInterface(iBinder), componentName);
            for (WrappedCallback callback : mCallbacks) {
                callback.onConnected(mService);
            }
            mCallbacks.clear();
        }

        /** This method will be called on the UI Thread by the Android Framework. */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mConnections.remove(mScope);
        }

        public void addCallback(WrappedCallback callback) {
            if (mService == null) {
                mCallbacks.add(callback);
            } else {
                callback.onConnected(mService);
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;

    /** Map from ServiceWorker scope to Connection. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<Uri, Connection> mConnections = new HashMap<>();

    private static AtomicReference<SharedPreferences> sSharedPreferences = new AtomicReference<>();

    /**
     * Gets the verified packages for the given origin. This is safe to be called on any thread,
     * however it may hit disk the first time it is called.
     *
     * @param context A Context to be used for accessing SharedPreferences.
     * @param origin The origin that was previously used with {@link #registerClient}.
     * @return A set of package names. This set is safe to be modified.
     */
    public static @NonNull Set<String> getVerifiedPackages(@NonNull Context context,
            @NonNull String origin) {
        // Loading preferences is on the critical path for this class - we need to synchronously
        // inform the client whether or not an notification can be handled by a TWA.
        // I considered loading the preferences into a cache on a background thread when this class
        // was created, but ultimately if that load hadn't completed by the time {@link #execute} or
        // {@link #registerClient} were called, we'd still need to block for it to complete.
        // Therefore we attempt to asynchronously load the preferences in the constructor, but if
        // they aren't loaded by the time they are needed, we disable StrictMode and read them on
        // the main thread.
        StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();

        try {
            ensurePreferencesOpened(context);

            return new HashSet<>(
                    sSharedPreferences.get().getStringSet(origin, Collections.<String>emptySet()));
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void ensurePreferencesOpened(@NonNull Context context) {
        if (sSharedPreferences.get() == null) {
            sSharedPreferences.compareAndSet(null,
                    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE));
        }
    }

    /**
     * Creates a TrustedWebActivityServiceConnectionManager.
     * @param context A Context used for accessing SharedPreferences.
     */
    public TrustedWebActivityServiceConnectionManager(@NonNull Context context) {
        mContext = context.getApplicationContext();

        // Asynchronously try to load (and therefore cache) the preferences.
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ensurePreferencesOpened(context);
            }
        });
    }

    private static WrappedCallback wrapCallback(final ExecutionCallback callback) {
        return new WrappedCallback() {
            @Override
            public void onConnected(@Nullable final TrustedWebActivityServiceWrapper service) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onConnected(service);
                        } catch (TransactionTooLargeException e) {
                            Log.w(TAG,
                                    "TransactionTooLargeException from TrustedWebActivityService, "
                                            + "possibly due to large size of small icon.", e);
                        } catch (RemoteException | RuntimeException e) {
                            Log.w(TAG,
                                    "Exception while trying to use TrustedWebActivityService.", e);
                        }
                    }
                });
            }
        };
    }

    /**
     * Connects to the appropriate {@link TrustedWebActivityService} or uses an existing connection
     * if available and runs code once connected.
     * <p>
     * To find a Service to connect to, this method attempts to resolve an
     * {@link Intent#ACTION_VIEW} Intent with the {@code scope} as data. The first of the resolved
     * packages that is registered (through {@link #registerClient}) to {@code origin} will be
     * chosen. Finally, an Intent with the action
     * {@link TrustedWebActivityService#ACTION_TRUSTED_WEB_ACTIVITY_SERVICE} will be used to find
     * the Service.
     * <p>
     * This method should be called on the UI thread.
     *
     * @param scope The scope used in an Intent to find packages that may have a
     *              {@link TrustedWebActivityService}.
     * @param origin An origin that the {@link TrustedWebActivityService} package must be registered
     *               to.
     * @param callback A {@link ExecutionCallback} that will be run with a connection.
     *                 It will be run on a background thread from the ThreadPool as most methods
     *                 from {@link TrustedWebActivityServiceWrapper} require this.
     *                 Any {@link RemoteException} or {@link RuntimeException} exceptions thrown by
     *                 the callback will be swallowed.
     *                 This is to allow users to deal with exceptions thrown by
     *                 {@link TrustedWebActivityServiceWrapper} if they wish, but to fail
     *                 gracefully if they don't.
     * @return Whether a {@link TrustedWebActivityService} was found.
     */
    @SuppressLint("StaticFieldLeak")
    @MainThread
    public boolean execute(@NonNull final Uri scope, @NonNull String origin,
            @NonNull final ExecutionCallback callback) {
        final WrappedCallback wrappedCallback = wrapCallback(callback);

        // If we have an existing connection, use it.
        Connection connection = mConnections.get(scope);
        if (connection != null) {
            connection.addCallback(wrappedCallback);
            return true;
        }

        // Check that this is a notification we want to handle.
        final Intent bindServiceIntent = createServiceIntent(mContext, scope, origin, true);
        if (bindServiceIntent == null) return false;

        final Connection newConnection = new Connection(scope);
        newConnection.addCallback(wrappedCallback);

        // Create a new connection.
        new AsyncTask<Void, Void, Connection>() {
            @Override
            protected Connection doInBackground(Void... voids) {
                try {
                    // We can pass newConnection to bindService here on a background thread because
                    // bindService assures us it will use newConnection on the UI thread.
                    if (mContext.bindService(bindServiceIntent, newConnection,
                            Context.BIND_AUTO_CREATE)) {
                        return newConnection;
                    }

                    mContext.unbindService(newConnection);
                    return null;
                } catch (SecurityException e) {
                    Log.w(TAG, "SecurityException while binding.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Connection newConnection) {
                if (newConnection == null) {
                    wrappedCallback.onConnected(null);
                } else {
                    mConnections.put(scope, newConnection);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return true;
    }

    /**
     * Checks if a TrustedWebActivityService exists to handle requests for the given scope and
     * origin. The value will be the same as that returned from {@link #execute} so calling that
     * and checking the return may be more convenient.
     *
     * This method should be called on the UI thread.
     *
     * @param scope The scope used in an Intent to find packages that may have a
     *              {@link TrustedWebActivityService}.
     * @param origin An origin that the {@link TrustedWebActivityService} package must be registered
     *               to.
     * @return Whether a {@link TrustedWebActivityService} was found.
     */
    @MainThread
    public boolean serviceExistsForScope(@NonNull Uri scope, @NonNull String origin) {
        // If we have an existing connection, we can deal with the scope.
        if (mConnections.get(scope) != null) return true;

        return createServiceIntent(mContext, scope, origin, false) != null;
    }

    /**
     * Unbinds all open connections to Trusted Web Activity clients.
     */
    void unbindAllConnections() {
        for (Connection connection : mConnections.values()) {
            mContext.unbindService(connection);
        }
        mConnections.clear();
    }

    /**

     * Creates an Intent to launch the Service for the given scope and verified origin. Will
     * return null if there is no applicable Service.
     */
    private @Nullable Intent createServiceIntent(Context appContext, Uri scope, String origin,
            boolean shouldLog) {
        Set<String> possiblePackages = getVerifiedPackages(appContext, origin);

        if (possiblePackages == null || possiblePackages.size() == 0) {
            return null;
        }

        // Get a list of installed packages that would match the scope.
        Intent scopeResolutionIntent = new Intent();
        scopeResolutionIntent.setData(scope);
        scopeResolutionIntent.setAction(Intent.ACTION_VIEW);
        // TODO(peconn): Do we want MATCH_ALL here.
        // TODO(peconn): Do we need a category here?
        List<ResolveInfo> candidateActivities = appContext.getPackageManager()
                .queryIntentActivities(scopeResolutionIntent, PackageManager.MATCH_DEFAULT_ONLY);

        // Choose the first of the installed packages that is verified.
        String resolvedPackage = null;
        for (ResolveInfo info : candidateActivities) {
            String packageName = info.activityInfo.packageName;

            if (possiblePackages.contains(packageName)) {
                resolvedPackage = packageName;
                break;
            }
        }

        if (resolvedPackage == null) {
            if (shouldLog) Log.w(TAG, "No TWA candidates for " + origin + " have been registered.");
            return null;
        }

        // Find the TrustedWebActivityService within that package.
        Intent serviceResolutionIntent = new Intent();
        serviceResolutionIntent.setPackage(resolvedPackage);
        serviceResolutionIntent.setAction(
                TrustedWebActivityService.ACTION_TRUSTED_WEB_ACTIVITY_SERVICE);
        ResolveInfo info = appContext.getPackageManager().resolveService(serviceResolutionIntent,
                PackageManager.MATCH_ALL);

        if (info == null) {
            if (shouldLog) Log.w(TAG, "Could not find TWAService for " + resolvedPackage);
            return null;
        }

        if (shouldLog) {
            Log.i(TAG, "Found " + info.serviceInfo.name + " to handle request for " + origin);
        }
        Intent finalIntent = new Intent();
        finalIntent.setComponent(new ComponentName(resolvedPackage, info.serviceInfo.name));
        return finalIntent;
    }

    /**
     * Registers (and persists) a package to be used for an origin. This information is persisted
     * in SharedPreferences. Although this method can be called on any thread, it may read
     * SharedPreferences and hit the disk, so call it on a background thread if possible.
     * @param context A Context to access SharedPreferences.
     * @param origin The origin for which the package is relevant.
     * @param clientPackage The packages to register.
     */
    public static void registerClient(@NonNull Context context, @NonNull String origin,
            @NonNull String clientPackage) {
        Set<String> possiblePackages = getVerifiedPackages(context, origin);
        possiblePackages.add(clientPackage);

        // sSharedPreferences won't be null after a call to getVerifiedPackages.
        SharedPreferences.Editor editor = sSharedPreferences.get().edit();
        editor.putStringSet(origin, possiblePackages);
        editor.apply();
    }
}
