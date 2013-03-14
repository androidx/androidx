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

public class ICUCompatIcs {

    private static final String TAG = "ICUCompatIcs";

    private static Method getScriptMethod;
    private static Method addLikelySubtagsMethod;

    static {
        try {
            final Class<?> clazz = Class.forName("libcore.icu.ICU");
            if (clazz != null) {
                getScriptMethod = clazz.getMethod("getScript",
                        new Class[]{ String.class });
                addLikelySubtagsMethod = clazz.getMethod("addLikelySubtags",
                        new Class[]{ String.class });
            }
        } catch (Exception e) {
            // Nothing we can do here, we just log the exception
            Log.w(TAG, e);
        }
    }

    public static String getScript(String locale) {
        try {
            if (getScriptMethod != null) {
                final Object[] args = new Object[] { locale };
                return (String) getScriptMethod.invoke(null, args);
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
            if (addLikelySubtagsMethod != null) {
                final Object[] args = new Object[] { locale };
                return (String) addLikelySubtagsMethod.invoke(null, args);
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
