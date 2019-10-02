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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
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
 * To interact with a {@link TrustedWebActivityService}, call {@link #connect}.
 */
public final class TrustedWebActivityServiceConnectionManager {
    private static final String TAG = "TWAConnectionManager";
    private static final String PREFS_FILE = "TrustedWebActivityVerifiedPackages";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;

    /** Map from ServiceWorker scope to Connection. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<Uri, ConnectionHolder> mConnections = new HashMap<>();

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
        // was created, but ultimately if that load hadn't completed by the time {@link #connect} or
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
     * @param executor The {@link Executor} to connect to the Service on if a new connection is
     *                 required.
     * @return A {@link ListenableFuture} for the resulting
     *         {@link TrustedWebActivityServiceWrapper}. This may be set to an
     *         {@link IllegalArgumentException} if no service exists for the scope (you can check
     *         for this beforehand by calling {@link #serviceExistsForScope(Uri, String)}. It may
     *         be set to a {@link SecurityException} if the Service does not accept connections from
     *         this app. It may be set to an {@link IllegalStateException} if connecting to the
     *         Service fails.
     */
    @MainThread
    @NonNull
    public ListenableFuture<TrustedWebActivityServiceWrapper> connect(
            @NonNull final Uri scope, @NonNull String origin, @NonNull Executor executor) {
        // If we have an existing connection, use it.
        ConnectionHolder connection = mConnections.get(scope);
        if (connection != null) {
            return connection.getServiceWrapper();
        }

        // Check that this is a notification we want to handle.
        final Intent bindServiceIntent = createServiceIntent(mContext, scope, origin, true);
        if (bindServiceIntent == null) {
            return FutureUtils.immediateFailedFuture(
                    new IllegalArgumentException("No service exists for scope"));
        }

        ConnectionHolder newConnection = new ConnectionHolder(() -> mConnections.remove(scope));
        mConnections.put(scope, newConnection);

        // Create a new connection.
        new BindToServiceAsyncTask(mContext, bindServiceIntent, newConnection)
                .executeOnExecutor(executor);

        return newConnection.getServiceWrapper();
    }

    static class BindToServiceAsyncTask extends AsyncTask<Void, Void, Exception> {
        private final Context mAppContext;
        private final Intent mIntent;
        private final ConnectionHolder mConnection;

        BindToServiceAsyncTask(Context context, Intent intent, ConnectionHolder connection) {
            mAppContext = context.getApplicationContext();
            mIntent = intent;
            mConnection = connection;
        }

        @Nullable
        @Override
        protected Exception doInBackground(Void... voids) {
            try {
                // We can pass newConnection to bindService here on a background thread because
                // bindService assures us it will use newConnection on the UI thread.
                if (mAppContext.bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE)) {
                    return null;
                }

                mAppContext.unbindService(mConnection);
                // TODO: Find a better exception to use here.
                return new IllegalStateException("Could not bind to the service");
            } catch (SecurityException e) {
                Log.w(TAG, "SecurityException while binding.", e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception bindingException) {
            if (bindingException != null) mConnection.cancel(bindingException);
        }
    }

    /**
     * Checks if a TrustedWebActivityService exists to handle requests for the given scope and
     * origin. The value will be the same as that returned from {@link #connect} so calling that
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
        for (ConnectionHolder connection : mConnections.values()) {
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
