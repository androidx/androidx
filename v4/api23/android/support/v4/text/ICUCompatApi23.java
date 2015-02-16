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
 * limitations under the License
 */

package android.support.v4.text;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class ICUCompatApi23 {

    private static final String TAG = "ICUCompatIcs";

    private static Method sAddLikelySubtagsMethod;

    static {
        try {
            // This class should always exist on API-23 since it's CTS tested.
            final Class<?> clazz = Class.forName("libcore.icu.ICU");
            sAddLikelySubtagsMethod = clazz.getMethod("addLikelySubtags",
                    new Class[]{ Locale.class });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public static String maximizeAndGetScript(Locale locale) {
        try {
            final Object[] args = new Object[] { locale };
            return ((Locale) sAddLikelySubtagsMethod.invoke(null, args)).getScript();
        } catch (InvocationTargetException e) {
            Log.w(TAG, e);
        } catch (IllegalAccessException e) {
            Log.w(TAG, e);
        }

        return locale.getScript();
    }
}
