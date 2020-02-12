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

package androidx.startup;

import static android.content.pm.PackageManager.GET_META_DATA;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@link AppInitializer} can be used to initialize all discovered [ComponentInitializer]s.
 * <br/>
 * The discovery mechanism is via `<meta-data>` entries in the merged `AndroidManifest.xml`.
 */
@SuppressWarnings("WeakerAccess")
public final class AppInitializer {

    /**
     * The {@link AppInitializer} instance.
     */
    private static AppInitializer sInstance;

    /**
     * Guards app initialization.
     */
    private static final Object sLock = new Object();

    @NonNull
    final List<Class<?>> mDiscovered;

    @NonNull
    final Map<Class<?>, Object> mInitialized;

    @NonNull
    final Context mContext;

    /**
     * Creates an instance of {@link AppInitializer}
     *
     * @param context The application context
     */
    AppInitializer(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mInitialized = new HashMap<>();
        mDiscovered = discoverComponents();
    }

    /**
     * @param context The Application {@link Context}
     * @return The instance of {@link AppInitializer} after initialization.
     */
    @NonNull
    @SuppressWarnings("UnusedReturnValue")
    public static AppInitializer getInstance(@NonNull Context context) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new AppInitializer(context);
            }
            return sInstance;
        }
    }

    /**
     * Discovers an initializes all available {@link ComponentInitializer} classes based on the
     * merged manifest `<meta-data>` entries in the `AndroidManifest.xml`.
     */
    public void initializeAllComponents() {
        initializeComponents(mDiscovered);
    }

    /**
     * Initializes a {@link List} of {@link ComponentInitializer} class types.
     *
     * @param components The {@link List} of {@link Class}es that represent all discovered
     *                   {@link ComponentInitializer}s
     */
    public void initializeComponents(@NonNull List<Class<?>> components) {
        synchronized (sLock) {
            doInitialize(components, new HashSet<>());
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void doInitialize(
            @NonNull List<Class<?>> components,
            @NonNull Set<Class<?>> initializing) {

        for (Class<?> component : components) {
            if (initializing.contains(component)) {
                String message = String.format(
                        "Cannot initialize %s. Cycle detected.", component.getName()
                );
                throw new IllegalStateException(message);
            }
            if (!mInitialized.containsKey(component)) {
                initializing.add(component);
                try {
                    Object instance = component.getDeclaredConstructor().newInstance();
                    if (!(instance instanceof ComponentInitializer<?>)) {
                        String message = String.format(
                                "%s is not a subtype of ComponentInitializer", component.getName()
                        );
                        throw new IllegalStateException(message);
                    }
                    ComponentInitializer<?> initializer = (ComponentInitializer<?>) instance;
                    List<Class<? extends ComponentInitializer<?>>> dependencies =
                            initializer.dependencies();
                    List<Class<?>> filtered = null;
                    if (!dependencies.isEmpty()) {
                        filtered = new ArrayList<>(dependencies.size());
                        for (Class<? extends ComponentInitializer<?>> clazz : dependencies) {
                            if (!mInitialized.containsKey(clazz)) {
                                filtered.add(clazz);
                            }
                        }
                    }
                    if (filtered != null && !filtered.isEmpty()) {
                        doInitialize(filtered, initializing);
                    }
                    if (StartupLogger.DEBUG) {
                        StartupLogger.i(String.format("Initializing %s", component.getName()));
                    }
                    Object result = initializer.create(mContext);
                    if (StartupLogger.DEBUG) {
                        StartupLogger.i(String.format("Initialized %s", component.getName()));
                    }
                    initializing.remove(component);
                    mInitialized.put(component, result);
                } catch (Throwable throwable) {
                    throw new StartupException(throwable);
                }
            }
        }
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public List<Class<?>> discoverComponents() {
        try {
            ApplicationInfo applicationInfo =
                    mContext.getPackageManager()
                            .getApplicationInfo(mContext.getPackageName(), GET_META_DATA);

            Bundle metadata = applicationInfo.metaData;
            String startup = mContext.getString(R.string.androidx_startup);
            if (metadata != null) {
                List<Class<?>> components = new ArrayList<>(metadata.size());
                Set<String> keys = metadata.keySet();
                for (String key : keys) {
                    String value = metadata.getString(key, null);
                    if (startup.equals(value)) {
                        Class<?> clazz = Class.forName(key);
                        if (StartupLogger.DEBUG) {
                            StartupLogger.i(String.format("Discovered %s", key));
                        }
                        components.add(clazz);
                    }
                }
                return components;
            } else {
                return Collections.emptyList();
            }
        } catch (PackageManager.NameNotFoundException | ClassNotFoundException exception) {
            throw new StartupException(exception);
        }
    }
}
