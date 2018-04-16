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

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Instance of AppComponentFactory for support libraries.
 * @see CompatWrapped
 * @hide
 */
@RequiresApi(api = 28)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CoreComponentFactory extends AppComponentFactory {
    private static final String TAG = "CoreComponentFactory";

    @Override
    public Activity instantiateActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(super.instantiateActivity(cl, className, intent));
    }

    @Override
    public Application instantiateApplication(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(super.instantiateApplication(cl, className));
    }

    @Override
    public BroadcastReceiver instantiateReceiver(ClassLoader cl, String className,
            Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(super.instantiateReceiver(cl, className, intent));
    }

    @Override
    public ContentProvider instantiateProvider(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(super.instantiateProvider(cl, className));
    }

    @Override
    public Service instantiateService(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return checkCompatWrapper(super.instantiateService(cl, className, intent));
    }

    static <T> T checkCompatWrapper(T obj) {
        if (obj instanceof CompatWrapped) {
            T wrapper = (T) ((CompatWrapped) obj).getWrapper();
            if (wrapper != null) {
                return wrapper;
            }
        }
        return obj;
    }

    /**
     * Implement this interface to allow a different class to be returned when instantiating
     * on certain API levels.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface CompatWrapped {
        /**
         * Called while this class is being instantiated by the OS.
         *
         * If an object is returned then it will be used in place of the class.
         * Note: this will not be called on API <= 27.
         *
         * Example:
         * <pre class="prettyprint">
         * {@literal
         * public AndroidXContentProvider extends ContentProvider implements CompatWrapped {
         *     ...
         *
         *     public Object getWrapper() {
         *         if (SDK_INT >= 29) {
         *             return new AndroidXContentProviderV29(this);
         *         }
         *         return null;
         *     }
         * }
         * }
         * </pre>
         */
        Object getWrapper();
    }

}
