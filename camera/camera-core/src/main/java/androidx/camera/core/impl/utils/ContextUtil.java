/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for {@link Context} related operations.
 */
public final class ContextUtil {
    private static final int DEVICE_ID_DEFAULT = 0;
    private static final Object CACHE_LOCK = new Object();
    @GuardedBy("CACHE_LOCK")
    private static final Map<String, WeakReference<Context>> CACHED_CONTEXTS = new HashMap<>();

    /**
     * Gets the persistent application context.
     *
     * <p>The persistent application context preserves the attribution tag and the device ID of the
     * provided {@link Context}.
     *
     * <p>The device ID of the returned {@link Context} will not be changed by the system.
     *
     * <p>The returned {@link Context} is guaranteed to be the same object if the application,
     * device ID and attribution tag are the same.
     */
    public static @NonNull Context getPersistentApplicationContext(@NonNull Context context) {
        // Obtains the application context outside the synchronization block.
        Context resultContext = context.getApplicationContext();
        String hashKey = getApplicationContextHashKey(context);
        synchronized (CACHE_LOCK) {
            // Directly returns the cached context if it can be found. Caching is needed because
            // consumers of this method, such as CameraExtensionCharacteristics, may rely on object
            // identity to function correctly.
            Context cachedContext = getCachedContext(hashKey);
            if (cachedContext != null) {
                return cachedContext;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                int deviceId = Api34Impl.getDeviceId(context);
                // Call createDeviceContext even if the device IDs are the same. The device ID of a
                // Context created by createDeviceContext with an explicit device ID will not be
                // changed by the system when the foreground activity is switched to different
                // display.
                resultContext = Api34Impl.createDeviceContext(resultContext, deviceId);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                String attributionTagContext = Api30Impl.getAttributionTag(context);
                String attributionTagResultContext = Api30Impl.getAttributionTag(resultContext);
                if (!Objects.equals(attributionTagContext, attributionTagResultContext)) {
                    resultContext = Api30Impl.createAttributionContext(
                            resultContext, attributionTagContext);
                }
            }
            // Caches the resultContext for the application context + device id + attribution tag.
            CACHED_CONTEXTS.put(hashKey, new WeakReference<>(resultContext));
            return resultContext;
        }
    }

    @GuardedBy("CACHE_LOCK")
    private static @Nullable Context getCachedContext(@NonNull String hashKey) {
        WeakReference<Context> cachedContextReference = CACHED_CONTEXTS.get(hashKey);

        if (cachedContextReference != null) {
            Context cachedContext = cachedContextReference.get();
            if (cachedContext != null) {
                return cachedContext;
            } else {
                CACHED_CONTEXTS.remove(hashKey);
            }
        }
        return null;
    }

    private static @NonNull String getApplicationContextHashKey(@NonNull Context context) {
        int applicationHashCode = context.getApplicationContext().hashCode();
        int deviceId = getDeviceId(context);
        String attributionTag =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ? Api30Impl.getAttributionTag(
                        context) : null;
        return String.format("%d-%d-%s", applicationHashCode, deviceId, attributionTag);
    }

    /**
     * Attempts to retrieve an {@link Application} object from the provided {@link Context}.
     *
     * <p>Because the contract does not specify that {@link Context#getApplicationContext()} must
     * return an {@link Application} object, this method will attempt to retrieve the
     * {@link Application} by unwrapping the context via {@link ContextWrapper#getBaseContext()} if
     * {@link Context#getApplicationContext()}} does not succeed.
     *
     * <p>Since the purpose of this method is to retrieve the {@link Application} instance, it is
     * not necessary to keep the attribution and device id info and also invoking
     * {@link Context#createAttributionContext(String)} or {@link Context#createDeviceContext(int)}
     * will create a non-ContextWrapper instance which could fail to invoke
     * {@link ContextWrapper#getBaseContext()}.
     */
    public static @Nullable Application getApplication(@NonNull Context context) {
        Application application = null;
        Context appContext = context.getApplicationContext();
        while (appContext instanceof ContextWrapper) {
            if (appContext instanceof Application) {
                application = (Application) appContext;
                break;
            } else {
                appContext = ((ContextWrapper) appContext).getBaseContext();
            }
        }
        return application;
    }

    /**
     * Returns the default device ID.
     */
    public static int getDefaultDeviceId() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ? Api34Impl.DEVICE_ID_DEFAULT : DEVICE_ID_DEFAULT;
    }

    /**
     * Returns the device ID associated with the given {@link Context}.
     */
    public static int getDeviceId(@NonNull Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ? Api34Impl.getDeviceId(context) : getDefaultDeviceId();
    }

    private ContextUtil() {
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
     */
    @RequiresApi(30)
    private static class Api30Impl {

        private Api30Impl() {
        }

        static @NonNull Context createAttributionContext(@NonNull Context context,
                @Nullable String attributeTag) {
            return context.createAttributionContext(attributeTag);
        }

        static @Nullable String getAttributionTag(@NonNull Context context) {
            return context.getAttributionTag();
        }
    }

    @RequiresApi(34)
    private static class Api34Impl {
        private Api34Impl() {
        }

        static final int DEVICE_ID_DEFAULT = Context.DEVICE_ID_DEFAULT;

        static @NonNull Context createDeviceContext(@NonNull Context context, int deviceId) {
            return context.createDeviceContext(deviceId);
        }

        static int getDeviceId(@NonNull Context context) {
            return context.getDeviceId();
        }
    }
}
