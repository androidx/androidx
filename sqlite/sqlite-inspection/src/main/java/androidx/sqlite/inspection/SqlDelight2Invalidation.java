/*
 * Copyright 2024 The Android Open Source Project
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/**
 * An [Invalidation] for the SqlDelight 2 library.
 * <p>
 * SqlDelight 2 invalidation API uses an internal "queryKey" to associate queries with listeners.
 * The key is created by the generated code and is typically just the affected table name but can
 * in theory be anything. In fact, a user can register a listener directly using
 * SqlDriver#addListener() and provide their own queryKeys. This will work as long as the user
 * also manages notification using SqlDriver#notifyListeners().
 * <p>
 * The public API that notifies listeners requires this queryKey:
 * <pre>
 *   override fun notifyListeners(vararg queryKeys: String)
 * </pre>
 * There is no public API that works without it and there is no public API that lists the current
 * listeners or queryKey's.
 * <p>
 * Because of this, we need to access the private field AndroidSqliteDriver#listeners and extract
 * the registered queryKeys.
 */
class SqlDelight2Invalidation implements Invalidation {
    public static final String TAG = "StudioInspectors";
    public static final String HIDDEN_TAG = "studio.inspectors";

    public static final String DRIVER_CLASSNAME =
            "app.cash.sqldelight.driver.android.AndroidSqliteDriver";
    public static final String NOTIFY_METHOD = "notifyListeners";
    public static final String LISTENERS_FIELD = "listeners";

    private final @NonNull ArtTooling mArtTooling;
    private final @NonNull Class<?> mDriverClass;
    private final @NonNull Method mNotifyListenersMethod;
    private final @NonNull Field mListenersField;

    static Invalidation create(@NonNull ArtTooling artTooling) {
        try {
            ClassLoader classLoader = Objects.requireNonNull(
                    SqlDelight2Invalidation.class.getClassLoader());
            Class<?> driverClass = classLoader.loadClass(DRIVER_CLASSNAME);
            Method notifyListenersMethod =
                    driverClass.getDeclaredMethod(NOTIFY_METHOD, String[].class);
            Field listenersField = driverClass.getDeclaredField(LISTENERS_FIELD);
            listenersField.setAccessible(true);
            return new SqlDelight2Invalidation(
                    artTooling,
                    driverClass,
                    notifyListenersMethod,
                    listenersField);
        } catch (ClassNotFoundException e) {
            Log.v(HIDDEN_TAG, "SqlDelight 2 not found", e);
            return () -> {
            };
        } catch (Exception e) {
            Log.w(TAG, "Error setting up SqlDelight 2 invalidation", e);
            return () -> {
            };
        }
    }

    private SqlDelight2Invalidation(
            @NonNull ArtTooling artTooling,
            @NonNull Class<?> driverClass,
            @NonNull Method notifyListenersMethod,
            @NonNull Field listenersField) {
        mArtTooling = artTooling;
        mDriverClass = driverClass;
        mNotifyListenersMethod = notifyListenersMethod;
        mListenersField = listenersField;
    }

    @SuppressLint("BanUncheckedReflection")
    @Override
    public void triggerInvalidations() {
        for (Object driver : mArtTooling.findInstances(mDriverClass)) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> listeners =
                        Objects.requireNonNull((Map<String, Object>) mListenersField.get(driver));
                synchronized (listeners) {
                    mNotifyListenersMethod.invoke(
                            driver,
                            (Object) listeners.keySet().toArray(new String[0]));
                }
            } catch (Exception e) {
                Log.w(TAG, "Error invalidating SqlDriver", e);
            }
        }
    }
}
