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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.ProviderResourceEntry;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.FontsContractCompat.FontInfo;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Helper for accessing features in {@link Typeface} in a backwards compatible fashion.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class TypefaceCompat {
    @GuardedBy("sLock")
    private static TypefaceCompatImpl sTypefaceCompatImpl;
    private static final Object sLock = new Object();

    interface TypefaceCompatImpl {
        // Create Typeface from font file in res/font directory.
        Typeface createFromResourcesFontFile(Resources resources, int id, int style);

        // Create Typeface from XML which root node is "font-family"
        Typeface createFromFontFamilyFilesResourceEntry(
                FontFamilyFilesResourceEntry entry, Resources resources, int id, int style);

        // For finiding cache before parsing xml data.
        Typeface findFromCache(Resources resources, int id, int style);

        Typeface createTypeface(@NonNull FontInfo[] fonts, Map<Uri, ByteBuffer> uriBuffer);
    }

    /**
     * If the current implementation is not set, set it according to the current build version. This
     * is safe to call several times, even if the implementation has already been set.
     */
    @TargetApi(26)
    private static void maybeInitImpl(Context context) {
        if (sTypefaceCompatImpl == null) {
            synchronized (sLock) {
                if (sTypefaceCompatImpl == null) {
                    // TODO: Maybe we can do better thing on Android N or later.
                    sTypefaceCompatImpl = new TypefaceCompatBaseImpl(context);
                }
            }
        }
    }

    private TypefaceCompat() {}

    /**
     * Find from internal cache.
     *
     * @return null if not found.
     */
    public static Typeface findFromCache(Resources resources, int id, int style) {
        synchronized (sLock) {
            // There is no cache if there is no impl.
            if (sTypefaceCompatImpl == null) {
                return null;
            }
        }
        return sTypefaceCompatImpl.findFromCache(resources, id, style);
    }

    /**
     * Create Typeface from XML resource which root node is font-family.
     *
     * @return null if failed to create.
     */
    public static Typeface createFromResourcesFamilyXml(
            Context context, FamilyResourceEntry entry, Resources resources, int id, int style) {
        maybeInitImpl(context);
        if (entry instanceof ProviderResourceEntry) {
            return FontsContractCompat.getFontSync(context,
                    ((ProviderResourceEntry) entry).getRequest());
        } else {
            return sTypefaceCompatImpl.createFromFontFamilyFilesResourceEntry(
                    (FontFamilyFilesResourceEntry) entry, resources, id, style);
        }
    }

    /**
     * Used by Resources to load a font resource of type font file.
     */
    @Nullable
    public static Typeface createFromResourcesFontFile(
            Context context, Resources resources, int id, int style) {
        maybeInitImpl(context);
        return sTypefaceCompatImpl.createFromResourcesFontFile(resources, id, style);
    }

    /**
     * Create a Typeface from a given FontInfo list and a map that matches them to ByteBuffers.
     */
    public static Typeface createTypeface(Context context, @NonNull FontInfo[] fonts,
            Map<Uri, ByteBuffer> uriBuffer) {
        maybeInitImpl(context);
        return sTypefaceCompatImpl.createTypeface(fonts, uriBuffer);
    }
}
