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

package androidx.car.app;

import static androidx.car.app.utils.LogTags.TAG;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines all constants for permissions that a car app can use.
 */
public final class CarAppPermission {
    /**
     * Defines which permissions are ones defined by the car app library.
     *
     * @hide
     */
    @StringDef(value = {ACCESS_SURFACE, NAVIGATION_TEMPLATES})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LibraryPermission {
    }

    /**
     * Permission that apps can use to get access to a canvas surface.
     *
     * <p>This surface can be used for drawing custom content like navigation apps can use it to
     * draw a map.
     */
    public static final String ACCESS_SURFACE = "androidx.car.app.ACCESS_SURFACE";

    /**
     * Permission that apps can use to get access to the navigation templates of the car app
     * library.
     *
     * <p>This permission should only be declared by apps that belong to one of the categories that
     * allow using the navigation templates. See
     * <a href="https://developer.android.com/training/cars/apps/navigation#access-navigation-templates">the
     * documentation</a> for the list of such categories. An app not in one of those categories
     * requesting this permission may be rejected upon submission to the Play Store. See
     * {@link CarAppService} for how to declare your app's category.
     */
    public static final String NAVIGATION_TEMPLATES = "androidx.car.app.NAVIGATION_TEMPLATES";

    /**
     * Permission that apps can use to get access to templates that show a map such as
     * {@link androidx.car.app.model.PlaceListMapTemplate}. Templates used by navigation apps that
     * draw their own maps
     * (e.g. {@link androidx.car.app.navigation.model.PlaceListNavigationTemplate}) don't require
     * this permission.
     *
     * <p>This permission should only be declared by apps that belong to one of the categories that
     * allow using the map templates. See
     * <a href="https://developer.android.com/training/cars/apps/poi#access-map-template">the
     * documentation</a> for the list of such categories. An app not in one of those categories
     * requesting this permission may be rejected upon submission to the Play Store. See
     * {@link CarAppService} for how to declare your app's category.
     */
    public static final String MAP_TEMPLATES = "androidx.car.app.MAP_TEMPLATES";

    /**
     * Checks that the car app has the given {@code permission} granted.
     *
     * @throws SecurityException if the app does not have a required permission granted
     */
    public static void checkHasPermission(@NonNull Context context, @NonNull String permission) {
        if (context.getPackageManager().checkPermission(permission, context.getPackageName())
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        throw new SecurityException(
                "The car app does not have the required permission: " + permission);
    }

    /**
     * Checks that the car app has declared the required library {@code permission}.
     *
     * <p>In contrast to {@link #checkHasPermission}, this method will validate that the app has at
     * least declared the permission requested.
     *
     * @throws SecurityException if the app does not have the required permission declared
     */
    public static void checkHasLibraryPermission(
            @NonNull Context context, @NonNull @LibraryPermission String permission) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG,
                    "Checking to see if the car app requested the required library permission: "
                            + permission);
        }

        try {
            checkHasPermission(context, permission);
            return;
        } catch (SecurityException e) {
            // Do nothing, we use a fallback for library permissions.
        }

        // Fallback for devices where permissions may not be granted by the system if the app
        // was installed before the host is installed.
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null) {
                for (String requestedPermission : packageInfo.requestedPermissions) {
                    if (requestedPermission.equals(permission)) {
                        return;
                    }
                }
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package name not found on the system: " + context.getPackageName(), e);
        }
        throw new SecurityException(
                "The car app does not have a required permission: " + permission);
    }

    private CarAppPermission() {
    }
}
