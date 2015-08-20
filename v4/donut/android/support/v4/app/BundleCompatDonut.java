/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.app;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @hide
 */
class BundleCompatDonut {
    private static final String TAG = "BundleCompatDonut";

    private static Method sGetIBinderMethod;
    private static boolean sGetIBinderMethodFetched;

    private static Method sPutIBinderMethod;
    private static boolean sPutIBinderMethodFetched;

    public static IBinder getBinder(Bundle bundle, String key) {
        if (!sGetIBinderMethodFetched) {
            try {
                sGetIBinderMethod = Bundle.class.getMethod("getIBinder", String.class);
                sGetIBinderMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve getIBinder method", e);
            }
            sGetIBinderMethodFetched = true;
        }

        if (sGetIBinderMethod != null) {
            try {
                return (IBinder) sGetIBinderMethod.invoke(bundle, key);
            } catch (InvocationTargetException | IllegalAccessException
                    | IllegalArgumentException e) {
                Log.i(TAG, "Failed to invoke getIBinder via reflection", e);
                sGetIBinderMethod = null;
            }
        }
        return null;
    }

    public static void putBinder(Bundle bundle, String key, IBinder binder) {
        if (!sPutIBinderMethodFetched) {
            try {
                sPutIBinderMethod =
                        Bundle.class.getMethod("putIBinder", String.class, IBinder.class);
                sPutIBinderMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve putIBinder method", e);
            }
            sPutIBinderMethodFetched = true;
        }

        if (sPutIBinderMethod != null) {
            try {
                sPutIBinderMethod.invoke(bundle, key, binder);
            } catch (InvocationTargetException | IllegalAccessException
                    | IllegalArgumentException e) {
                Log.i(TAG, "Failed to invoke putIBinder via reflection", e);
                sPutIBinderMethod = null;
            }
        }
    }
}
