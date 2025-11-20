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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.core.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import androidx.core.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import androidx.core.provider.FontsContractCompat.FontInfo;

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implementation of the Typeface compat methods for API 21 and above.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class TypefaceCompatApi21Impl extends TypefaceCompatBaseImpl {
    private static final String TAG = "TypefaceCompatApi21Impl";

    private static final String FONT_FAMILY_CLASS = "android.graphics.FontFamily";
    private static final String ADD_FONT_WEIGHT_STYLE_METHOD = "addFontWeightStyle";
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD =
            "createFromFamiliesWithDefault";
    private static Class<?> sFontFamily;
    private static Constructor<?> sFontFamilyCtor;
    private static Method sAddFontWeightStyle;
    private static Method sCreateFromFamiliesWithDefault;
    private static boolean sHasInitBeenCalled = false;

    private static void init() {
        if (sHasInitBeenCalled) {
            return;
        }
        sHasInitBeenCalled = true;

        Class<?> fontFamilyClass;
        Constructor<?> fontFamilyCtor;
        Method addFontMethod;
        Method createFromFamiliesWithDefaultMethod;
        try {
            fontFamilyClass = Class.forName(FONT_FAMILY_CLASS);
            fontFamilyCtor = fontFamilyClass.getConstructor();
            addFontMethod = fontFamilyClass.getMethod(ADD_FONT_WEIGHT_STYLE_METHOD,
                    String.class, Integer.TYPE, Boolean.TYPE);
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

    private File getFile(@NonNull ParcelFileDescriptor fd) {
        try {
            final String path = Os.readlink("/proc/self/fd/" + fd.getFd());
            // Check if the symbolic link points the regular file.
            if (OsConstants.S_ISREG(Os.stat(path).st_mode)) {
                return new File(path);
            } else {
                return null;
            }
        } catch (ErrnoException e) {
            return null;  // Mostly permission error.
        }
    }

    private static Object newFamily() {
        init();
        try {
            return sFontFamilyCtor.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Typeface createFromFamiliesWithDefault(Object family) {
        init();
        try {
            Object familyArray = Array.newInstance(sFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) sCreateFromFamiliesWithDefault.invoke(
                    null /* static method */, familyArray);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean addFontWeightStyle(Object family, String name,
            int weight, boolean style) {
        init();
        try {
            final Boolean result = (Boolean) sAddFontWeightStyle.invoke(
                    family, name, weight, style);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Typeface createFromFontInfo(Context context, CancellationSignal cancellationSignal,
            FontInfo @NonNull [] fonts, int style) {
        if (fonts.length < 1) {
            return null;
        }
        final FontInfo bestFont = findBestInfo(fonts, style);
        final ContentResolver resolver = context.getContentResolver();
        try (ParcelFileDescriptor pfd =
                     resolver.openFileDescriptor(bestFont.getUri(), "r", cancellationSignal)) {
            if (pfd == null) {
                return null;
            }
            final File file = getFile(pfd);
            if (file == null || !file.canRead()) {
                // Unable to use the real file for creating Typeface. Fallback to copying
                // implementation.
                try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {
                    return super.createFromInputStream(context, fis);
                }
            }
            return Typeface.createFromFile(file);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Typeface createFromFontFamilyFilesResourceEntry(Context context,
            FontFamilyFilesResourceEntry entry, Resources resources, int style) {
        Object family = newFamily();
        for (final FontFileResourceEntry e : entry.getEntries()) {
            final File tmpFile = TypefaceCompatUtil.getTempFile(context);
            if (tmpFile == null) {
                return null;
            }
            try {
                if (!TypefaceCompatUtil.copyToFile(tmpFile, resources, e.getResourceId())) {
                    return null;
                }

                if (!addFontWeightStyle(family, tmpFile.getPath(), e.getWeight(), e.isItalic())) {
                    return null;
                }
            } catch (RuntimeException exception) {
                // This was thrown from Typeface.createFromFile when a Typeface could not be loaded.
                // such as due to an invalid ttf or unreadable file. We don't want to throw that
                // exception anymore.
                return null;
            } finally {
                tmpFile.delete();
            }
        }
        return createFromFamiliesWithDefault(family);
    }

    @Override
    @NonNull Typeface createWeightStyle(@NonNull Context context,
            @NonNull Typeface base, int weight, boolean italic) {
        Typeface out = null;
        try {
            out = WeightTypefaceApi21.createWeightStyle(base, weight, italic);
        } catch (RuntimeException fallbackFailed) {
            // ignore
        }
        if (out == null) {
            out = super.createWeightStyle(context, base, weight, italic);
        }
        return out;
    }
}
