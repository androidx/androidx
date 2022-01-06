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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * A TrustedWebActivityServiceConnectionPool will be used by a Trusted Web Activity provider and
 * takes care of connecting to and communicating with {@link TrustedWebActivityService}s.
 * This is done through the {@link #connect} method.
 * <p>
 * Multiple Trusted Web Activity client apps may be suitable for a given scope.
 * These are passed in to {@link #connect} and {@link #serviceExistsForScope} and the most
 * appropriate one for the scope is chosen.
 */
public final class TrustedWebActivityServiceConnectionPool {
    private static final String TAG = "TWAConnectionPool";

    /** Application context, used to connect to the services. */
    private final Context mContext;

    /** Map from ServiceWorker scope to Connection. */
    private final Map<Uri, ConnectionHolder> mConnections = new HashMap<>();

    private TrustedWebActivityServiceConnectionPool(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Creates a TrustedWebActivityServiceConnectionPool.
     * @param context A Context used for accessing SharedPreferences.
     */
    @NonNull
    public static TrustedWebActivityServiceConnectionPool create(@NonNull Context context) {
        return new TrustedWebActivityServiceConnectionPool(context);
    }

    /**
     * Connects to the appropriate {@link TrustedWebActivityService} or uses an existing connection
     * if available and runs code once connected.
     * <p>
     * To find a Service to connect to, this method attempts to resolve an
     * {@link Intent#ACTION_VIEW} Intent with the {@code scope} as data.
     * The first of the resolved packages to be contained in the {@code possiblePackages} set will
     * be chosen.
     * Finally, an Intent with the action
     * {@link TrustedWebActivityService#ACTION_TRUSTED_WEB_ACTIVITY_SERVICE} will be used to find
     * the Service.
     * <p>
     * This method should be called on the UI thread.
     *
     * @param scope The scope used in an Intent to find packages that may have a
     *              {@link TrustedWebActivityService}.
     * @param possiblePackages A collection of packages to consider.
     *                         These would be the packages that have previously launched a
     *                         Trusted Web Activity for the origin.
     * @param executor The {@link Executor} to connect to the Service on if a new connection is
     *                 required.
     * @return A {@link ListenableFuture} for the resulting
     *         {@link TrustedWebActivityServiceConnection}.
     *         This may be set to an {@link IllegalArgumentException} if no service exists for
     *         the scope (you can check for this beforehand by calling
     *         {@link #serviceExistsForScope}).
     *         It may be set to a {@link SecurityException} if the Service does not accept
     *         connections from this app.
     *         It may be set to an {@link IllegalStateException} if connecting to the Service fails.
     */
    @MainThread
    @NonNull
    @SuppressWarnings("deprecation") /* AsyncTask */
    public ListenableFuture<TrustedWebActivityServiceConnection> connect(
            @NonNull final Uri scope,
            @NonNull Set<Token> possiblePackages,
            @NonNull Executor executor) {
        // If we have an existing connection, use it.
        ConnectionHolder connection = mConnections.get(scope);
        if (connection != null) {
            return connection.getServiceWrapper();
        }

        // Check that this is a notification we want to handle.
        final Intent bindServiceIntent =
                createServiceIntent(mContext, scope, possiblePackages, true);
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

    @SuppressWarnings("deprecation") /* AsyncTask */
    static class BindToServiceAsyncTask extends android.os.AsyncTask<Void, Void, Exception> {
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
                if (mAppContext.bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE
                        | Context.BIND_INCLUDE_CAPABILITIES)) {
                    return null;
                }

                mAppContext.unbindService(mConnection);
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
     * origin.
     * This method uses the same logic as {@link #connect}.
     * If this method returns {@code false}, {@link #connect} will return a Future containing an
     * {@link IllegalStateException}.
     * <p>
     * This method should be called on the UI thread.
     *
     * @param scope The scope used in an Intent to find packages that may have a
     *              {@link TrustedWebActivityService}.
     * @param possiblePackages A collection of packages to consider.
     *                         These would be the packages that have previously launched a
     *                         Trusted Web Activity for the origin.
     * @return Whether a {@link TrustedWebActivityService} was found.
     */
    @MainThread
    public boolean serviceExistsForScope(@NonNull Uri scope,
            @NonNull Set<Token> possiblePackages) {
        // If we have an existing connection, we can deal with the scope.
        if (mConnections.get(scope) != null) return true;

        return createServiceIntent(mContext, scope, possiblePackages, false) != null;
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
     * Creates an Intent to launch the Service for the given scope and to an app contained in
     * {@code possiblePackages}.
     * Will return {@code null} if there is no applicable Service.
     */
    @SuppressWarnings("deprecation")
    private @Nullable Intent createServiceIntent(Context appContext, Uri scope,
            Set<Token> possiblePackages, boolean shouldLog) {
        if (possiblePackages == null || possiblePackages.size() == 0) {
            return null;
        }

        // Get a list of installed packages that would match the scope.
        Intent scopeResolutionIntent = new Intent();
        scopeResolutionIntent.setData(scope);
        scopeResolutionIntent.setAction(Intent.ACTION_VIEW);
        List<ResolveInfo> candidateActivities = appContext.getPackageManager()
                .queryIntentActivities(scopeResolutionIntent, PackageManager.MATCH_DEFAULT_ONLY);

        // Choose the first of the installed packages that is verified.
        String resolvedPackage = null;
        for (ResolveInfo info : candidateActivities) {
            String packageName = info.activityInfo.packageName;

            for (Token possiblePackage : possiblePackages) {
                if (possiblePackage.matches(packageName, appContext.getPackageManager())) {
                    resolvedPackage = packageName;
                    break;
                }
            }
        }

        if (resolvedPackage == null) {
            if (shouldLog) Log.w(TAG, "No TWA candidates for " + scope + " have been registered.");
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
            Log.i(TAG, "Found " + info.serviceInfo.name + " to handle request for " + scope);
        }
        Intent finalIntent = new Intent();
        finalIntent.setComponent(new ComponentName(resolvedPackage, info.serviceInfo.name));
        return finalIntent;
    }
}
