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

package androidx.core.app;

import static androidx.core.app.CoreComponentFactory.checkCompatWrapper;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;

/**
 * Version of {@link android.app.AppComponentFactory} that works with androidx libraries.
 *
 * Note: This will only work on API 28+ and does not backport AppComponentFactory functionality.
 */
@RequiresApi(28)
public class AppComponentFactory extends android.app.AppComponentFactory {

    /**
     * @see #instantiateActivityCompat
     */
    @Override
    public final Activity instantiateActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(instantiateActivityCompat(cl, className, intent));
    }

    /**
     * @see #instantiateApplicationCompat
     */
    @Override
    public final Application instantiateApplication(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(instantiateApplicationCompat(cl, className));
    }

    /**
     * @see #instantiateReceiverCompat
     */
    @Override
    public final BroadcastReceiver instantiateReceiver(ClassLoader cl, String className,
            Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(instantiateReceiverCompat(cl, className, intent));
    }

    /**
     * @see #instantiateProviderCompat
     */
    @Override
    public final ContentProvider instantiateProvider(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(instantiateProviderCompat(cl, className));
    }

    /**
     * @see #instantiateServiceCompat
     */
    @Override
    public final Service instantiateService(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(instantiateServiceCompat(cl, className, intent));
    }

    /**
     * Allows application to override the creation of the application object. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the Application object. The returned object will not be initialized
     * as a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     */
    public @NonNull Application instantiateApplicationCompat(@NonNull ClassLoader cl,
            @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (Application) cl.loadClass(className).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Couldn't call constructor", e);
        }
    }

    /**
     * Allows application to override the creation of activities. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the Activity object. The returned object will not be initialized
     * as a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     * @param intent    Intent creating the class.
     */
    public @NonNull Activity instantiateActivityCompat(@NonNull ClassLoader cl,
            @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (Activity) cl.loadClass(className).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Couldn't call constructor", e);
        }
    }

    /**
     * Allows application to override the creation of receivers. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     * @param intent    Intent creating the class.
     */
    public @NonNull BroadcastReceiver instantiateReceiverCompat(@NonNull ClassLoader cl,
            @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (BroadcastReceiver) cl.loadClass(className).getDeclaredConstructor()
                    .newInstance();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Couldn't call constructor", e);
        }
    }

    /**
     * Allows application to override the creation of services. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the Service object. The returned object will not be initialized
     * as a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     * @param intent    Intent creating the class.
     */
    public @NonNull Service instantiateServiceCompat(@NonNull ClassLoader cl,
            @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (Service) cl.loadClass(className).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Couldn't call constructor", e);
        }
    }

    /**
     * Allows application to override the creation of providers. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the ContentProvider object. The returned object will not be initialized
     * with a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     */
    public @NonNull ContentProvider instantiateProviderCompat(@NonNull ClassLoader cl,
            @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (ContentProvider) cl.loadClass(className).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Couldn't call constructor", e);
        }
    }
}
