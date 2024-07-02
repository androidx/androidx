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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * Utility class for {@link Context} related operations.
 */
public final class ContextUtil {
    /**
     * Gets the application context and preserves the attribution tag and device id.
     */
    @NonNull
    public static Context getApplicationContext(@NonNull Context context) {
        Context resultContext  = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            int deviceIdContext = Api34Impl.getDeviceId(context);
            int deviceIdResultContext = Api34Impl.getDeviceId(resultContext);
            if (deviceIdContext != deviceIdResultContext) {
                resultContext = Api34Impl.createDeviceContext(resultContext, deviceIdContext);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String attributeTagContext = Api30Impl.getAttributionTag(context);
            String attributeTagResultContext = Api30Impl.getAttributionTag(resultContext);
            if (!Objects.equals(attributeTagContext, attributeTagResultContext)) {
                resultContext = Api30Impl.createAttributionContext(
                        resultContext, attributeTagContext);
            }
        }
        return resultContext;
    }


    /**
     * Attempts to retrieve an {@link Application} object from the provided {@link Context}.
     *
     * <p>Because the contract does not specify that {@code Context.getApplicationContext()} must
     * return an {@code Application} object, this method will attempt to retrieve the
     * {@code Application} by unwrapping the context via {@link ContextWrapper#getBaseContext()} if
     * {@code Context.getApplicationContext()}} does not succeed.
     *
     * <p>Since the purpose of this method is to retrieve the {@link Application} instance, it is
     * not necessary to keep the attribution and device id info and also invoking
     * {@link Context#createAttributionContext(String)} or {@link Context#createDeviceContext(int)}
     * will create a non-ContextWrapper instance which could fail to invoke
     * {@link ContextWrapper#getBaseContext()}.
     */
    @Nullable
    public static Application getApplicationFromContext(@NonNull Context context) {
        Application application = null;
        Context appContext = getApplicationContext(context);
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

    private ContextUtil() {
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
     */
    @RequiresApi(30)
    private static class Api30Impl {

        private Api30Impl() {
        }

        @NonNull
        static Context createAttributionContext(@NonNull Context context,
                @Nullable String attributeTag) {
            return context.createAttributionContext(attributeTag);
        }

        @Nullable
        static String getAttributionTag(@NonNull Context context) {
            return context.getAttributionTag();
        }
    }

    @RequiresApi(34)
    private static class Api34Impl {
        private Api34Impl() {
        }

        @NonNull
        static Context createDeviceContext(@NonNull Context context, int deviceId) {
            return context.createDeviceContext(deviceId);
        }

        static int getDeviceId(@NonNull Context context) {
            return context.getDeviceId();
        }
    }
}
