/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Typeface;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implementation of the Typeface compat methods for API 28 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(28)
public class TypefaceCompatApi28Impl extends TypefaceCompatApi26Impl {
    private static final String TAG = "TypefaceCompatApi28Impl";

    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD =
            "createFromFamiliesWithDefault";
    private static final int RESOLVE_BY_FONT_TABLE = -1;
    private static final String DEFAULT_FAMILY = "sans-serif";

    /**
     * Call method Typeface#createFromFamiliesWithDefault(
     *      FontFamily[] families, String fallbackName, int weight, int italic)
     */
    @Override
    protected Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Object familyArray = Array.newInstance(mFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) mCreateFromFamiliesWithDefault.invoke(null /* static method */,
                    familyArray, DEFAULT_FAMILY, RESOLVE_BY_FONT_TABLE, RESOLVE_BY_FONT_TABLE);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Method obtainCreateFromFamiliesWithDefaultMethod(Class fontFamily)
            throws NoSuchMethodException {
        Object familyArray = Array.newInstance(fontFamily, 1);
        Method m =  Typeface.class.getDeclaredMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD,
                familyArray.getClass(), String.class, Integer.TYPE, Integer.TYPE);
        m.setAccessible(true);
        return m;
    }
}
