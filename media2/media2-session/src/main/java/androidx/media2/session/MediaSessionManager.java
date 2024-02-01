/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides support for interacting with media sessions that applications have published in order to
 * express their ongoing media playback state.
 *
 * @see MediaSessionCompat
 * @see MediaSession
 * @see MediaSessionService
 * @see MediaLibraryService
 * @see MediaControllerCompat
 * @see MediaController
 * @see MediaBrowser
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
public final class MediaSessionManager {
    static final String TAG = "MediaSessionManager";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static MediaSessionManager sInstance;

    private final Context mContext;

    /**
     * Gets an instance of MediaSessionManager associated with the context.
     *
     * @return the MediaSessionManager instance for this context
     */
    @NonNull
    public static MediaSessionManager getInstance(@NonNull Context context) {
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new MediaSessionManager(context.getApplicationContext());
            }
            return sInstance;
        }
    }

    private MediaSessionManager(Context context) {
        mContext = context;
    }

    /**
     * Gets {@link Set} of {@link SessionToken} for {@link MediaSessionService} regardless of
     * their activeness. This list represents media apps that support background playback.
     *
     * <p>
     * The app targeting API level 30 or higher must include a {@code <queries>} element in their
     * manifest to get service tokens of other apps. See the following example and
     * <a href="{@docRoot}training/package-visibility">this guide</a> for more information.
     * <pre>{@code
     * <intent>
     *   <action android:name="androidx.media2.session.MediaSessionService" />
     * </intent>
     * <intent>
     *   <action android:name="androidx.media2.session.MediaLibraryService" />
     * </intent>
     * <intent>
     *   <action android:name="android.media.browse.MediaBrowserService" />
     * </intent>
     * }</pre>
     *
     * @return set of tokens
     */
    @NonNull
    @SuppressWarnings("deprecation")
    public Set<SessionToken> getSessionServiceTokens() {
        ArraySet<SessionToken> sessionServiceTokens = new ArraySet<>();
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> services = new ArrayList<>();
        // If multiple actions are declared for a service, browser gets higher priority.
        List<ResolveInfo> libraryServices = pm.queryIntentServices(
                new Intent(MediaLibraryService.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
        if (libraryServices != null) {
            services.addAll(libraryServices);
        }
        List<ResolveInfo> sessionServices = pm.queryIntentServices(
                new Intent(MediaSessionService.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
        if (sessionServices != null) {
            services.addAll(sessionServices);
        }
        List<ResolveInfo> browserServices = pm.queryIntentServices(
                new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        if (browserServices != null) {
            services.addAll(browserServices);
        }

        for (ResolveInfo service : services) {
            if (service == null || service.serviceInfo == null) {
                continue;
            }
            ServiceInfo serviceInfo = service.serviceInfo;
            SessionToken token = new SessionToken(mContext,
                    new ComponentName(serviceInfo.packageName, serviceInfo.name));
            sessionServiceTokens.add(token);
        }
        if (DEBUG) {
            Log.d(TAG, "Found " + sessionServiceTokens.size() + " session services");
            for (SessionToken token : sessionServiceTokens) {
                Log.d(TAG, "   " + token);
            }
        }
        return sessionServiceTokens;
    }
}
