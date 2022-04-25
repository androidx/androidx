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

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Utility class for {@link Context} related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ContextUtil {
    /**
     * Gets the application context and preserves the attribution tag.
     */
    @NonNull
    public static Context getApplicationContext(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String attributeTag = Api30Impl.getAttributionTag(context);

            if (attributeTag != null) {
                return Api30Impl.createAttributionContext(applicationContext, attributeTag);
            }
        }
        return applicationContext;
    }

    /**
     * Gets the base context and preserves the attribution tag.
     */
    @NonNull
    public static Context getBaseContext(@NonNull ContextWrapper context) {
        Context baseContext = context.getBaseContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String attributeTag = Api30Impl.getAttributionTag(context);

            if (attributeTag != null) {
                return Api30Impl.createAttributionContext(baseContext, attributeTag);
            }
        }

        return baseContext;
    }

    /**
     * Attempts to retrieve an {@link Application} object from the provided {@link Context}.
     *
     * <p>Because the contract does not specify that {@code Context.getApplicationContext()} must
     * return an {@code Application} object, this method will attempt to retrieve the
     * {@code Application} by unwrapping the context via {@link ContextWrapper#getBaseContext()} if
     * {@code Context.getApplicationContext()}} does not succeed.
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
                appContext = getBaseContext((ContextWrapper) appContext);
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

        @DoNotInline
        @NonNull
        static Context createAttributionContext(@NonNull Context context,
                @Nullable String attributeTag) {
            return context.createAttributionContext(attributeTag);
        }

        @DoNotInline
        @Nullable
        static String getAttributionTag(@NonNull Context context) {
            return context.getAttributionTag();
        }
    }
}
