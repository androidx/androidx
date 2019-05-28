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

package androidx.media2.test.service;

import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.Callback;

import androidx.annotation.GuardedBy;
import androidx.media.MediaBrowserServiceCompat;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Mock implementation of the media browser service.
 */
public class MockMediaBrowserServiceCompat extends MediaBrowserServiceCompat {
    private static final String TAG = "MockMediaBrowserServiceCompat";
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static volatile MockMediaBrowserServiceCompat sInstance;
    @GuardedBy("sLock")
    private static volatile Proxy sServiceProxy;

    private MediaSessionCompat mSessionCompat;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sLock) {
            sInstance = this;
        }
        mSessionCompat = new MediaSessionCompat(this, TAG);
        mSessionCompat.setCallback(new Callback() { });
        mSessionCompat.setActive(true);
        setSessionToken(mSessionCompat.getSessionToken());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSessionCompat.release();
        synchronized (sLock) {
            sInstance = null;
            // Note: Don't reset sServiceProxy.
            //       When a test is finished and its next test is running, this service will be
            //       destroyed and re-created for the next test. When it happens, onDestroy() may be
            //       called after the next test's proxy has set because onDestroy() and tests run on
            //       the different threads.
            //       So keep sServiceProxy for the next test.
        }
    }

    public static MockMediaBrowserServiceCompat getInstance() {
        synchronized (sLock) {
            return sInstance;
        }
    }

    public static void setMediaBrowserServiceProxy(Proxy proxy) {
        synchronized (sLock) {
            sServiceProxy = proxy;
        }
    }

    private static boolean isProxyOverridesMethod(String methodName) {
        return isProxyOverridesMethod(methodName, -1);
    }

    private static boolean isProxyOverridesMethod(String methodName, int paramCount) {
        synchronized (sLock) {
            if (sServiceProxy == null) {
                return false;
            }
            Method[] methods = sServiceProxy.getClass().getMethods();
            if (methods == null) {
                return false;
            }
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(methodName)) {
                    if (paramCount < 0
                            || (methods[i].getParameterTypes() != null
                            && methods[i].getParameterTypes().length == paramCount)) {
                        // Found method. Check if it overrides
                        return methods[i].getDeclaringClass() != Proxy.class;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        if (!CLIENT_PACKAGE_NAME.equals(clientPackageName)) {
            // Test only -- reject any other request.
            return null;
        }
        synchronized (sLock) {
            if (isProxyOverridesMethod("onGetRoot")) {
                return sServiceProxy.onGetRoot(clientPackageName, clientUid, rootHints);
            }
        }
        return new BrowserRoot("stub", null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        synchronized (sLock) {
            if (isProxyOverridesMethod("onLoadChildren", 2)) {
                sServiceProxy.onLoadChildren(parentId, result);
                return;
            }
        }
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result, Bundle options) {
        synchronized (sLock) {
            if (isProxyOverridesMethod("onLoadChildren", 3)) {
                sServiceProxy.onLoadChildren(parentId, result, options);
                return;
            }
        }
        super.onLoadChildren(parentId, result, options);
    }

    @Override
    public void onLoadItem(String itemId, Result<MediaItem> result) {
        synchronized (sLock) {
            if (isProxyOverridesMethod("onLoadItem")) {
                sServiceProxy.onLoadItem(itemId, result);
                return;
            }
        }
        super.onLoadItem(itemId, result);
    }

    @Override
    public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result) {
        synchronized (sLock) {
            if (isProxyOverridesMethod("onSearch")) {
                sServiceProxy.onSearch(query, extras, result);
                return;
            }
        }
        super.onSearch(query, extras, result);
    }

    @Override
    public void onCustomAction(String action, Bundle extras, Result<Bundle> result) {
        synchronized (sLock) {
            if (isProxyOverridesMethod("onCustomAction")) {
                sServiceProxy.onCustomAction(action, extras, result);
                return;
            }
        }
        super.onCustomAction(action, extras, result);
    }

    public static class Proxy {
        public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
            return new BrowserRoot("stub", null);
        }

        public void onLoadChildren(String parentId, Result<List<MediaItem>> result) { }

        public void onLoadChildren(String parentId, Result<List<MediaItem>> result,
                Bundle options) { }

        public void onLoadItem(String itemId, Result<MediaItem> result) { }

        public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result) { }

        public void onCustomAction(String action, Bundle extras, Result<Bundle> result) { }
    }
}
