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

package android.support.v4.graphics;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import android.support.v4.provider.FontsContractCompat;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Implementation of the Typeface compat methods for API 26 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(26)
public class TypefaceCompatApi26Impl extends TypefaceCompatApi21Impl {
    private static final String TAG = "TypefaceCompatApi26Impl";

    private static final String FONT_FAMILY_CLASS = "android.graphics.FontFamily";
    private static final String ADD_FONT_FROM_ASSET_MANAGER_METHOD = "addFontFromAssetManager";
    private static final String ADD_FONT_FROM_BUFFER_METHOD = "addFontFromBuffer";
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD =
            "createFromFamiliesWithDefault";
    private static final String FREEZE_METHOD = "freeze";
    private static final String ABORT_CREATION_METHOD = "abortCreation";
    private static final Class sFontFamily;
    private static final Constructor sFontFamilyCtor;
    private static final Method sAddFontFromAssetManager;
    private static final Method sAddFontFromBuffer;
    private static final Method sFreeze;
    private static final Method sAbortCreation;
    private static final Method sCreateFromFamiliesWithDefault;
    private static final int RESOLVE_BY_FONT_TABLE = -1;

    static {
        Class fontFamilyClass;
        Constructor fontFamilyCtor;
        Method addFontMethod;
        Method addFromBufferMethod;
        Method freezeMethod;
        Method abortCreationMethod;
        Method createFromFamiliesWithDefaultMethod;
        try {
            fontFamilyClass = Class.forName(FONT_FAMILY_CLASS);
            fontFamilyCtor = fontFamilyClass.getConstructor();
            addFontMethod = fontFamilyClass.getMethod(ADD_FONT_FROM_ASSET_MANAGER_METHOD,
                    AssetManager.class, String.class, Integer.TYPE, Boolean.TYPE, Integer.TYPE,
                    Integer.TYPE, Integer.TYPE, FontVariationAxis[].class);
            addFromBufferMethod = fontFamilyClass.getMethod(ADD_FONT_FROM_BUFFER_METHOD,
                    ByteBuffer.class, Integer.TYPE, FontVariationAxis[].class, Integer.TYPE,
                    Integer.TYPE);
            freezeMethod = fontFamilyClass.getMethod(FREEZE_METHOD);
            abortCreationMethod = fontFamilyClass.getMethod(ABORT_CREATION_METHOD);
            Object familyArray = Array.newInstance(fontFamilyClass, 1);
            createFromFamiliesWithDefaultMethod =
                    Typeface.class.getDeclaredMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD,
                            familyArray.getClass(), Integer.TYPE, Integer.TYPE);
            createFromFamiliesWithDefaultMethod.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.e(TAG, "Unable to collect necessary methods for class " + e.getClass().getName(),
                    e);
            fontFamilyClass = null;
            fontFamilyCtor = null;
            addFontMethod = null;
            addFromBufferMethod = null;
            freezeMethod = null;
            abortCreationMethod = null;
            createFromFamiliesWithDefaultMethod = null;
        }
        sFontFamilyCtor = fontFamilyCtor;
        sFontFamily = fontFamilyClass;
        sAddFontFromAssetManager = addFontMethod;
        sAddFontFromBuffer = addFromBufferMethod;
        sFreeze = freezeMethod;
        sAbortCreation = abortCreationMethod;
        sCreateFromFamiliesWithDefault = createFromFamiliesWithDefaultMethod;
    }

    /**
     * Returns true if API26 implementation is usable.
     */
    private static boolean isFontFamilyPrivateAPIAvailable() {
        if (sAddFontFromAssetManager == null) {
            Log.w(TAG, "Unable to collect necessary private methods."
                    + "Fallback to legacy implementation.");
        }
        return sAddFontFromAssetManager != null;
    }

    /**
     * Create a new FontFamily instance
     */
    private static Object newFamily() {
        try {
            return sFontFamilyCtor.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#addFontFromAssetManager(AssetManager mgr, String path, int cookie,
     *      boolean isAsset, int ttcIndex, int weight, int isItalic, FontVariationAxis[] axes)
     */
    private static boolean addFontFromAssetManager(Context context, Object family, String fileName,
            int ttcIndex, int weight, int style) {
        try {
            final Boolean result = (Boolean) sAddFontFromAssetManager.invoke(family,
                    context.getAssets(), fileName, 0 /* cookie */, false /* isAsset */, ttcIndex,
                    weight, style, null /* axes */);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#addFontFromBuffer(ByteBuffer font, int ttcIndex, FontVariationAxis[] axes,
     *      int weight, int italic)
     */
    private static boolean addFontFromBuffer(Object family, ByteBuffer buffer,
            int ttcIndex, int weight, int style) {
        try {
            final Boolean result = (Boolean) sAddFontFromBuffer.invoke(family,
                    buffer, ttcIndex, null /* axes */, weight, style);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call static method Typeface#createFromFamiliesWithDefault(
     *      FontFamily[] families, int weight, int italic)
     */
    private static Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Object familyArray = Array.newInstance(sFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) sCreateFromFamiliesWithDefault.invoke(null /* static method */,
                    familyArray, RESOLVE_BY_FONT_TABLE, RESOLVE_BY_FONT_TABLE);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#freeze()
     */
    private static boolean freeze(Object family) {
        try {
            Boolean result = (Boolean) sFreeze.invoke(family);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#abortCreation()
     */
    private static boolean abortCreation(Object family) {
        try {
            Boolean result = (Boolean) sAbortCreation.invoke(family);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Typeface createFromFontFamilyFilesResourceEntry(Context context,
            FontResourcesParserCompat.FontFamilyFilesResourceEntry entry, Resources resources,
            int style) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromFontFamilyFilesResourceEntry(context, entry, resources, style);
        }
        Object fontFamily = newFamily();
        for (final FontFileResourceEntry fontFile : entry.getEntries()) {
            // TODO: Add ttc and variation font support. (b/37853920)
            if (!addFontFromAssetManager(context, fontFamily, fontFile.getFileName(),
                    0 /* ttcIndex */, fontFile.getWeight(), fontFile.isItalic() ? 1 : 0)) {
                abortCreation(fontFamily);
                return null;
            }
        }
        if (!freeze(fontFamily)) {
            return null;
        }
        return createFromFamiliesWithDefault(fontFamily);
    }

    @Override
    public Typeface createFromFontInfo(Context context,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull FontsContractCompat.FontInfo[] fonts, int style) {
        if (fonts.length < 1) {
            return null;
        }
        if (!isFontFamilyPrivateAPIAvailable()) {
            // Even if the private API is not avaiable, don't use API 21 implemenation and use
            // public API to create Typeface from file descriptor.
            final FontsContractCompat.FontInfo bestFont = findBestInfo(fonts, style);
            final ContentResolver resolver = context.getContentResolver();
            try (ParcelFileDescriptor pfd =
                    resolver.openFileDescriptor(bestFont.getUri(), "r", cancellationSignal)) {
                return new Typeface.Builder(pfd.getFileDescriptor())
                        .setWeight(bestFont.getWeight())
                        .setItalic(bestFont.isItalic())
                        .build();
            } catch (IOException e) {
                return null;
            }
        }
        Map<Uri, ByteBuffer> uriBuffer = FontsContractCompat.prepareFontData(
                context, fonts, cancellationSignal);
        final Object fontFamily = newFamily();
        boolean atLeastOneFont = false;
        for (FontsContractCompat.FontInfo font : fonts) {
            final ByteBuffer fontBuffer = uriBuffer.get(font.getUri());
            if (fontBuffer == null) {
                continue;  // skip
            }
            final boolean success = addFontFromBuffer(fontFamily, fontBuffer,
                    font.getTtcIndex(), font.getWeight(), font.isItalic() ? 1 : 0);
            if (!success) {
                abortCreation(fontFamily);
                return null;
            }
            atLeastOneFont = true;
        }
        if (!atLeastOneFont) {
            abortCreation(fontFamily);
            return null;
        }
        if (!freeze(fontFamily)) {
            return null;
        }
        return createFromFamiliesWithDefault(fontFamily);
    }

    /**
     * Used by Resources to load a font resource of type font file.
     */
    @Nullable
    @Override
    public Typeface createFromResourcesFontFile(
            Context context, Resources resources, int id, String path, int style) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromResourcesFontFile(context, resources, id, path, style);
        }
        Object fontFamily = newFamily();
        if (!addFontFromAssetManager(context, fontFamily, path,
                0 /* ttcIndex */, RESOLVE_BY_FONT_TABLE /* weight */,
                RESOLVE_BY_FONT_TABLE /* italic */)) {
            abortCreation(fontFamily);
            return null;
        }
        if (!freeze(fontFamily)) {
            return null;
        }
        return createFromFamiliesWithDefault(fontFamily);
    }
}
