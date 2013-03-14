/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.text;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ICUCompatIcs {

    private static final String TAG = "ICUCompatIcs";

    private static Method sGetScriptMethod;
    private static Method sAddLikelySubtagsMethod;

    static {
        try {
            final Class<?> clazz = Class.forName("libcore.icu.ICU");
            if (clazz != null) {
                sGetScriptMethod = clazz.getMethod("getScript",
                        new Class[]{ String.class });
                sAddLikelySubtagsMethod = clazz.getMethod("addLikelySubtags",
                        new Class[]{ String.class });
            }
        } catch (Exception e) {
            // Nothing we can do here, we just log the exception
            Log.w(TAG, e);
        }
    }

    public static String getScript(String locale) {
        try {
            if (sGetScriptMethod != null) {
                final Object[] args = new Object[] { locale };
                return (String) sGetScriptMethod.invoke(null, args);
            }
        } catch (IllegalAccessException e) {
            // Nothing we can do here, we just log the exception
            Log.w(TAG, e);
        }
        catch (InvocationTargetException e) {
            // Nothing we can do here, we just log the exception
            Log.w(TAG, e);
        }
        return null;
    }

    public static String addLikelySubtags(String locale) {
        try {
            if (sAddLikelySubtagsMethod != null) {
                final Object[] args = new Object[] { locale };
                return (String) sAddLikelySubtagsMethod.invoke(null, args);
            }
        } catch (IllegalAccessException e) {
            // Nothing we can do here, we just log the exception
            Log.w(TAG, e);
        }
        catch (InvocationTargetException e) {
            // Nothing we can do here, we just log the exception
            Log.w(TAG, e);
        }
        return locale;
    }
}
