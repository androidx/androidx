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

package androidx.xr.scenecore.impl.extensions;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.RestrictTo;

import com.android.extensions.xr.XrExtensions;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Provides the OEM implementation of {@link XrExtensions}. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class XrExtensionsProvider {
    private static final String TAG = "XrExtensionsProvider";

    private XrExtensionsProvider() {}

    /**
     * Returns the OEM implementation of {@link XrExtensions} or returns null if no implementation
     * is found.
     */
    public static @Nullable XrExtensions getXrExtensions() {
        try {
            return XrExtensionsInstance.getInstance();
        } catch (NoClassDefFoundError e) {
            Log.w(TAG, "No XrExtensions implementation found.", e);
            return null;
        }
    }

    private static class XrExtensionsInstance {
        private XrExtensionsInstance() {}

        @SuppressLint("BanUncheckedReflection")
        private static class XrExtensionsHolder {
            public static final XrExtensions INSTANCE = new XrExtensions();

            static {
                // Try to call the @TestApi method XrExtensions.setCurrentExtensions to register
                // the current extensions instance. If this fails for various reasons (e.g. the
                // platform has removed this method or the app is not debuggable), ignore the
                // error.
                try {
                    Method setCurrentExtensionsMethod =
                            XrExtensions.class.getDeclaredMethod(
                                    "setCurrentExtensions", XrExtensions.class);
                    setCurrentExtensionsMethod.setAccessible(true);
                    setCurrentExtensionsMethod.invoke(null, INSTANCE);
                } catch (NoSuchMethodException
                        | InvocationTargetException
                        | IllegalAccessException
                        | SecurityException e) {
                    Log.d(
                            TAG,
                            "XrExtensions.setCurrentExtensions method could not be called: " + e);
                }
            }
        }

        private static XrExtensions getInstance() {
            return XrExtensionsHolder.INSTANCE;
        }
    }
}
