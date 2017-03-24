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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import android.support.v4.graphics.fonts.FontResult;
import android.util.Log;
import android.util.TypedValue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Implementation of the Typeface compat methods for API 24 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(24)
class TypefaceCompatApi24Impl extends TypefaceCompatBaseImpl {
    private static final String TAG = "TypefaceCompatApi24Impl";
    private static final int NORMAL_WEIGHT = 400;

    private static final Class<?> sFamilyClass;
    private static final Constructor<?> sFamilyCtor;
    private static final Method sAddFontWeightStyleMethod;
    private static final Method sCreateFromFamiliesWithDefaultMethod;

    static {
        Class<?> typefaceClass = Typeface.class;
        Class<?> familyClass = null;
        Constructor<?> familyCtor = null;
        Method addFontWeightStyleMethod = null;
        Method createFromFamiliesWithDefaultMethod = null;
        boolean success = true;
        try {
            familyClass = Class.forName("android.graphics.FontFamily");
            familyCtor = familyClass.getDeclaredConstructor();

            // boolean nAddFont(long nativeFamily, ByteBuffer font, int ttcIndex);
            addFontWeightStyleMethod = familyClass
                    .getDeclaredMethod("addFontWeightStyle", ByteBuffer.class, int.class,
                            List.class, int.class, boolean.class);
            addFontWeightStyleMethod.setAccessible(true);

            // Typeface createFromFamiliesWithDefault(FontFamily[] families)
            Object familyArray = Array.newInstance(familyClass, 1);
            createFromFamiliesWithDefaultMethod = typefaceClass
                    .getDeclaredMethod("createFromFamiliesWithDefault", familyArray.getClass());
            createFromFamiliesWithDefaultMethod.setAccessible(true);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            Log.e(TAG, "Could not locate Typeface reflection classes for API 24, falling back to"
                    + "creating font via ICS APIs.", e);
            success = false;
        }
        if (success) {
            sFamilyClass = familyClass;
            sFamilyCtor = familyCtor;
            sAddFontWeightStyleMethod = addFontWeightStyleMethod;
            sCreateFromFamiliesWithDefaultMethod = createFromFamiliesWithDefaultMethod;
        } else {
            sFamilyClass = null;
            sFamilyCtor = null;
            sAddFontWeightStyleMethod = null;
            sCreateFromFamiliesWithDefaultMethod = null;
        }
    }

    TypefaceCompatApi24Impl(Context context) {
        super(context);
    }

    @Override
    public Typeface createTypeface(List<FontResult> resultList) {
        if (sFamilyClass == null) {
            // If the reflection methods were not available, fall back to loading from file path.
            return super.createTypeface(resultList);
        }
        FileInputStream fis = null;
        try {
            Object family = sFamilyCtor.newInstance();

            for (int i = 0; i < resultList.size(); i++) {
                FontResult result = resultList.get(i);

                // create and memory map the file
                fis = new FileInputStream(result.getFileDescriptor().getFileDescriptor());
                FileChannel fileChannel = fis.getChannel();
                long fontSize = fileChannel.size();
                ByteBuffer fontBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fontSize);

                // load font into FontFamily
                sAddFontWeightStyleMethod.invoke(family, fontBuffer, result.getTtcIndex(), null,
                        result.getWeight(), result.getItalic());
                closeQuietly(fis);
            }

            Object familyArray = Array.newInstance(sFamilyClass, 1);
            Array.set(familyArray, 0, family);

            @SuppressWarnings("unchecked")
            Typeface typeface = (Typeface) sCreateFromFamiliesWithDefaultMethod.invoke(
                    null, familyArray);
            return typeface;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | IOException e) {
            Log.e(TAG, "Error generating typeface by reflection", e);
            return null;
        } finally {
            closeQuietly(fis);
        }
    }

    @Nullable
    @Override
    Typeface createFromResources(FontFamilyFilesResourceEntry entry,
            Resources resources, int id, String path) {
        if (sFamilyClass == null) {
            // If the reflection methods were not available, fall back to base behavior.
            return super.createFromResources(entry, resources, id, path);
        }

        try {
            Object family = sFamilyCtor.newInstance();

            FontFileResourceEntry[] entries = entry.getEntries();
            TypedValue value = new TypedValue();
            for (FontFileResourceEntry fontFileEntry : entries) {
                resources.getValue(fontFileEntry.getResourceId(), value, false);
                addFontToFamily(family, resources, (String) value.string, fontFileEntry.getWeight(),
                        fontFileEntry.isItalic());
            }
            Object familyArray = Array.newInstance(sFamilyClass, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) sCreateFromFamiliesWithDefaultMethod.invoke(
                    null, familyArray);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | IOException e) {
            Log.e(TAG, "Error generating typeface by reflection", e);
            return null;
        }
    }

    @Override
    Typeface createTypeface(Resources resources, String path) throws IOException {
        if (sFamilyClass == null) {
            // If the reflection methods were not available, fall back to base behavior.
            return super.createTypeface(resources, path);
        }
        try {
            Object family = sFamilyCtor.newInstance();
            addFontToFamily(family, resources, path, NORMAL_WEIGHT, false /* italic */);
            Object familyArray = Array.newInstance(sFamilyClass, 1);
            Array.set(familyArray, 0, family);
            @SuppressWarnings("unchecked")
            Typeface typeface = (Typeface) sCreateFromFamiliesWithDefaultMethod.invoke(
                    null, familyArray);
            return typeface;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | IOException e) {
            Log.e(TAG, "Error generating typeface by reflection", e);
            return null;
        }
    }

    private void addFontToFamily(Object family, Resources resources, String path, int weight,
            boolean italic)
            throws IOException, InvocationTargetException, IllegalAccessException {
        AssetFileDescriptor fd = resources.getAssets().openNonAssetFd(path);
        FileInputStream fis = fd.createInputStream();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int readLen;
            int totalRead = 0;
            while ((readLen = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, readLen);
                totalRead += readLen;
            }
            ByteBuffer fontBuffer = ByteBuffer.allocateDirect(totalRead);
            fontBuffer.put(baos.toByteArray());

            // load font into FontFamily
            // TODO: axes
            sAddFontWeightStyleMethod.invoke(family, fontBuffer, 0, null, weight, italic);
        } finally {
            closeQuietly(fis);
        }
    }
}
