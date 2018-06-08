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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import androidx.core.provider.FontsContractCompat;

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
    private static final int RESOLVE_BY_FONT_TABLE = -1;
    private static final String DEFAULT_FAMILY = "sans-serif";

    protected final Class mFontFamily;
    protected final Constructor mFontFamilyCtor;
    protected final Method mAddFontFromAssetManager;
    protected final Method mAddFontFromBuffer;
    protected final Method mFreeze;
    protected final Method mAbortCreation;
    protected final Method mCreateFromFamiliesWithDefault;

    public TypefaceCompatApi26Impl() {
        Class fontFamily;
        Constructor fontFamilyCtor;
        Method addFontFromAssetManager;
        Method addFontFromBuffer;
        Method freeze;
        Method abortCreation;
        Method createFromFamiliesWithDefault;
        try {
            fontFamily = obtainFontFamily();
            fontFamilyCtor = obtainFontFamilyCtor(fontFamily);
            addFontFromAssetManager = obtainAddFontFromAssetManagerMethod(fontFamily);
            addFontFromBuffer = obtainAddFontFromBufferMethod(fontFamily);
            freeze = obtainFreezeMethod(fontFamily);
            abortCreation = obtainAbortCreationMethod(fontFamily);
            createFromFamiliesWithDefault = obtainCreateFromFamiliesWithDefaultMethod(fontFamily);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.e(TAG, "Unable to collect necessary methods for class " + e.getClass().getName(),
                    e);
            fontFamily = null;
            fontFamilyCtor = null;
            addFontFromAssetManager = null;
            addFontFromBuffer = null;
            freeze = null;
            abortCreation = null;
            createFromFamiliesWithDefault = null;
        }
        mFontFamily = fontFamily;
        mFontFamilyCtor = fontFamilyCtor;
        mAddFontFromAssetManager = addFontFromAssetManager;
        mAddFontFromBuffer = addFontFromBuffer;
        mFreeze = freeze;
        mAbortCreation = abortCreation;
        mCreateFromFamiliesWithDefault = createFromFamiliesWithDefault;
    }

    /**
     * Returns true if all the necessary methods were found.
     */
    private boolean isFontFamilyPrivateAPIAvailable() {
        if (mAddFontFromAssetManager == null) {
            Log.w(TAG, "Unable to collect necessary private methods. "
                    + "Fallback to legacy implementation.");
        }
        return mAddFontFromAssetManager != null;
    }

    /**
     * Create a new FontFamily instance
     */
    private Object newFamily() {
        try {
            return mFontFamilyCtor.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#addFontFromAssetManager(AssetManager mgr, String path, int cookie,
     *      boolean isAsset, int ttcIndex, int weight, int isItalic, FontVariationAxis[] axes)
     */
    private boolean addFontFromAssetManager(Context context, Object family, String fileName,
            int ttcIndex, int weight, int style, @Nullable FontVariationAxis[] axes) {
        try {
            final Boolean result = (Boolean) mAddFontFromAssetManager.invoke(family,
                    context.getAssets(), fileName, 0 /* cookie */, false /* isAsset */, ttcIndex,
                    weight, style, axes);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#addFontFromBuffer(ByteBuffer font, int ttcIndex, FontVariationAxis[] axes,
     *      int weight, int italic)
     */
    private boolean addFontFromBuffer(Object family, ByteBuffer buffer,
            int ttcIndex, int weight, int style) {
        try {
            final Boolean result = (Boolean) mAddFontFromBuffer.invoke(family,
                    buffer, ttcIndex, null /* axes */, weight, style);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call method Typeface#createFromFamiliesWithDefault(
     *      FontFamily[] families, int weight, int italic)
     */
    protected Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Object familyArray = Array.newInstance(mFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) mCreateFromFamiliesWithDefault.invoke(null /* static method */,
                    familyArray, RESOLVE_BY_FONT_TABLE, RESOLVE_BY_FONT_TABLE);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#freeze()
     */
    private boolean freeze(Object family) {
        try {
            Boolean result = (Boolean) mFreeze.invoke(family);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call FontFamily#abortCreation()
     */
    private void abortCreation(Object family) {
        try {
            mAbortCreation.invoke(family);
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
            if (!addFontFromAssetManager(context, fontFamily, fontFile.getFileName(),
                    fontFile.getTtcIndex(), fontFile.getWeight(), fontFile.isItalic() ? 1 : 0,
                    FontVariationAxis.fromFontVariationSettings(fontFile.getVariationSettings()))) {
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
                if (pfd == null) {
                    return null;
                }
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
        final Typeface typeface = createFromFamiliesWithDefault(fontFamily);
        return Typeface.create(typeface, style);
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
                RESOLVE_BY_FONT_TABLE /* italic */, null /* axes */)) {
            abortCreation(fontFamily);
            return null;
        }
        if (!freeze(fontFamily)) {
            return null;
        }
        return createFromFamiliesWithDefault(fontFamily);
    }

    // The following getters retrieve by reflection the Typeface methods, belonging to the
    // framework code, which will be invoked. Since the definitions of these methods can change
    // across different API versions, inheriting classes should override these getters in order to
    // reflect the method definitions in the API versions they represent.
    //===========================================================================================
    protected Class obtainFontFamily() throws ClassNotFoundException {
        return Class.forName(FONT_FAMILY_CLASS);
    }

    protected Constructor obtainFontFamilyCtor(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getConstructor();
    }

    protected Method obtainAddFontFromAssetManagerMethod(Class fontFamily)
            throws NoSuchMethodException {
        return fontFamily.getMethod(ADD_FONT_FROM_ASSET_MANAGER_METHOD,
                AssetManager.class, String.class, Integer.TYPE, Boolean.TYPE, Integer.TYPE,
                Integer.TYPE, Integer.TYPE, FontVariationAxis[].class);
    }

    protected Method obtainAddFontFromBufferMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ADD_FONT_FROM_BUFFER_METHOD,
                ByteBuffer.class, Integer.TYPE, FontVariationAxis[].class, Integer.TYPE,
                Integer.TYPE);
    }

    protected Method obtainFreezeMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(FREEZE_METHOD);
    }

    protected Method obtainAbortCreationMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ABORT_CREATION_METHOD);
    }

    protected Method obtainCreateFromFamiliesWithDefaultMethod(Class fontFamily)
            throws NoSuchMethodException {
        Object familyArray = Array.newInstance(fontFamily, 1);
        Method m =  Typeface.class.getDeclaredMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD,
                familyArray.getClass(), Integer.TYPE, Integer.TYPE);
        m.setAccessible(true);
        return m;
    }
}
