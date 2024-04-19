/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.inspection.ArtTooling;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

class SqlDelightInvalidation implements Invalidation {
    public static final String TAG = "StudioInspectors";
    public static final String HIDDEN_TAG = "studio.inspectors";

    private static final String SQLDELIGHT_QUERY_CLASS_NAME = "com.squareup.sqldelight.Query";
    private static final String SQLDELIGHT_NOTIFY_METHOD_NAME = "notifyDataChanged";

    private final @NonNull ArtTooling mArtTooling;
    private final @NonNull Class<?> mQueryClass;
    private final @NonNull Method mNotifyDataChangeMethod;

    @NonNull
    static Invalidation create(@NonNull ArtTooling artTooling) {
        ClassLoader classLoader = SqlDelightInvalidation.class.getClassLoader();
        Objects.requireNonNull(classLoader);
        try {
            Class<?> queryClass = classLoader.loadClass(SQLDELIGHT_QUERY_CLASS_NAME);
            Method notifyMethod = queryClass.getMethod(SQLDELIGHT_NOTIFY_METHOD_NAME);
            return new SqlDelightInvalidation(artTooling, queryClass, notifyMethod);
        } catch (ClassNotFoundException e) {
            Log.v(HIDDEN_TAG, "SqlDelight not found", e);
            return () -> {
            };
        } catch (Exception e) {
            Log.w(TAG, "Error setting up SqlDelight invalidation", e);
            return () -> {
            };
        }
    }

    private SqlDelightInvalidation(@NonNull ArtTooling artTooling, @NonNull Class<?> queryClass,
            @NonNull Method notifyDataChangeMethod) {
        mArtTooling = artTooling;
        mQueryClass = queryClass;
        mNotifyDataChangeMethod = notifyDataChangeMethod;
    }

    @SuppressLint("BanUncheckedReflection")
    @Override
    public void triggerInvalidations() {
        // invalidating all queries because we can't say which ones were actually affected.
        for (Object query: mArtTooling.findInstances(mQueryClass)) {
            try {
                mNotifyDataChangeMethod.invoke(query);
            } catch (IllegalAccessException | InvocationTargetException e) {
                Log.w(TAG, "Error calling notifyDataChanged", e);
            }
        }
    }
}
