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
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link AppInitializer} can be used to initialize all discovered [ComponentInitializer]s.
 * <br/>
 * The discovery mechanism is via `<meta-data>` entries in the merged `AndroidManifest.xml`.
 */
@SuppressWarnings("WeakerAccess")
public final class AppInitializer {

    /**
     * Keeps track of whether components were initialized.
     */
    @VisibleForTesting
    @NonNull
    public static AtomicBoolean sInitialized = new AtomicBoolean(false);

    private AppInitializer() {
        // Does nothing.
    }

    /**
     * Discovers an initializes all available {@link ComponentInitializer} classes based on the
     * merged manifest `<meta-data>` entries in the `AndroidManifest.xml`.
     *
     * @param context The Application context
     */
    public static void initialize(@NonNull Context context) {
        List<Class<?>> components = discoverComponents(context);
        initialize(context, components);
    }

    /**
     * Initializes a {@link List} of {@link ComponentInitializer} class types.
     *
     * @param context    The Application context
     * @param components The {@link List} of {@link Class}es that represent all discovered
     *                   {@link ComponentInitializer}s
     */
    public static void initialize(@NonNull Context context, @NonNull List<Class<?>> components) {
        if (sInitialized.compareAndSet(false, true)) {
            doInitialize(context, components, new HashSet<>(), new HashSet<>());
        } else {
            if (StartupLogger.DEBUG) {
                StartupLogger.i("Already initialized");
            }
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void doInitialize(
            @NonNull Context context,
            @NonNull List<Class<?>> components,
            @NonNull Set<Class<?>> initializing,
            @NonNull Set<Class<?>> initialized) {

        for (Class<?> component : components) {
            if (initializing.contains(component)) {
                String message = String.format(
                        "Cannot initialize %s. Cycle detected.", component.getName()
                );
                throw new IllegalStateException(message);
            }
            if (!initialized.contains(component)) {
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
                    List<Class<?>> filtered = new ArrayList<>(dependencies.size());
                    for (Class<? extends ComponentInitializer<?>> clazz : dependencies) {
                        if (!initialized.contains(clazz)) {
                            filtered.add(clazz);
                        }
                    }
                    if (!filtered.isEmpty()) {
                        doInitialize(context, filtered, initializing, initialized);
                    }
                    if (StartupLogger.DEBUG) {
                        StartupLogger.i(String.format("Initializing %s", component.getName()));
                    }
                    initializer.create(context);
                    if (StartupLogger.DEBUG) {
                        StartupLogger.i(String.format("Initialized %s", component.getName()));
                    }
                    initializing.remove(component);
                    initialized.add(component);
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
    public static List<Class<?>> discoverComponents(@NonNull Context context) {
        try {
            ApplicationInfo applicationInfo =
                    context.getPackageManager()
                            .getApplicationInfo(context.getPackageName(), GET_META_DATA);

            Bundle metadata = applicationInfo.metaData;
            String startup = context.getString(R.string.androidx_startup);
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
