/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import androidx.core.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import androidx.core.provider.FontsContractCompat.FontInfo;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;


/**
 * Implementation of the Typeface compat methods for API 24 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(24)
class TypefaceCompatApi24Impl extends TypefaceCompatBaseImpl {
    private static final String TAG = "TypefaceCompatApi24Impl";

    private static final String FONT_FAMILY_CLASS = "android.graphics.FontFamily";
    private static final String ADD_FONT_WEIGHT_STYLE_METHOD = "addFontWeightStyle";
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD =
            "createFromFamiliesWithDefault";
    private static final Class sFontFamily;
    private static final Constructor sFontFamilyCtor;
    private static final Method sAddFontWeightStyle;
    private static final Method sCreateFromFamiliesWithDefault;

    static {
        Class fontFamilyClass;
        Constructor fontFamilyCtor;
        Method addFontMethod;
        Method createFromFamiliesWithDefaultMethod;
        try {
            fontFamilyClass = Class.forName(FONT_FAMILY_CLASS);
            fontFamilyCtor = fontFamilyClass.getConstructor();
            addFontMethod = fontFamilyClass.getMethod(ADD_FONT_WEIGHT_STYLE_METHOD,
                    ByteBuffer.class, Integer.TYPE, List.class, Integer.TYPE, Boolean.TYPE);
            Object familyArray = Array.newInstance(fontFamilyClass, 1);
            createFromFamiliesWithDefaultMethod =
                    Typeface.class.getMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD,
                          familyArray.getClass());
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.e(TAG, e.getClass().getName(), e);
            fontFamilyClass = null;
            fontFamilyCtor = null;
            addFontMethod = null;
            createFromFamiliesWithDefaultMethod = null;
        }
        sFontFamilyCtor = fontFamilyCtor;
        sFontFamily = fontFamilyClass;
        sAddFontWeightStyle = addFontMethod;
        sCreateFromFamiliesWithDefault = createFromFamiliesWithDefaultMethod;
    }

    /**
     * Returns true if API24 implementation is usable.
     */
    public static boolean isUsable() {
        if (sAddFontWeightStyle == null) {
            Log.w(TAG, "Unable to collect necessary private methods."
                    + "Fallback to legacy implementation.");
        }
        return sAddFontWeightStyle != null;
    }

    private static Object newFamily() {
        try {
            return sFontFamilyCtor.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean addFontWeightStyle(Object family, ByteBuffer buffer, int ttcIndex,
            int weight, boolean style) {
        try {
            final Boolean result = (Boolean) sAddFontWeightStyle.invoke(
                    family, buffer, ttcIndex, null /* variation axis */, weight, style);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Object familyArray = Array.newInstance(sFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) sCreateFromFamiliesWithDefault.invoke(
                    null /* static method */, familyArray);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Typeface createFromFontInfo(Context context,
            @Nullable CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts, int style) {
        Object family = newFamily();
        SimpleArrayMap<Uri, ByteBuffer> bufferCache = new SimpleArrayMap<>();

        for (final FontInfo font : fonts) {
            final Uri uri = font.getUri();
            ByteBuffer buffer = bufferCache.get(uri);
            if (buffer == null) {
                buffer = TypefaceCompatUtil.mmap(context, cancellationSignal, uri);
                bufferCache.put(uri, buffer);
            }
            if (!addFontWeightStyle(family, buffer, font.getTtcIndex(), font.getWeight(),
                    font.isItalic())) {
                return null;
            }
        }
        final Typeface typeface = createFromFamiliesWithDefault(family);
        return Typeface.create(typeface, style);
    }

    @Override
    public Typeface createFromFontFamilyFilesResourceEntry(Context context,
            FontFamilyFilesResourceEntry entry, Resources resources, int style) {
        Object family = newFamily();
        for (final FontFileResourceEntry e : entry.getEntries()) {
            final ByteBuffer buffer =
                    TypefaceCompatUtil.copyToDirectBuffer(context, resources, e.getResourceId());
            if (buffer == null) {
                return null;
            }
            if (!addFontWeightStyle(family, buffer, e.getTtcIndex(), e.getWeight(), e.isItalic())) {
                return null;
            }
        }
        return createFromFamiliesWithDefault(family);
    }
}
