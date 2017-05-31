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
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.ProviderResourceEntry;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.FontsContractCompat.FontInfo;
import android.support.v4.util.LruCache;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Helper for accessing features in {@link Typeface} in a backwards compatible fashion.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class TypefaceCompat {
    // TODO(nona): Introduce API 24 implementation.
    private static final TypefaceCompatImpl sTypefaceCompatImpl = new TypefaceCompatBaseImpl();

    /**
     * Cache for Typeface objects dynamically loaded from assets.
     */
    private static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);

    interface TypefaceCompatImpl {
        // Create Typeface from font file in res/font directory.
        Typeface createFromResourcesFontFile(Context context, Resources resources, int id,
                int style);

        // Create Typeface from XML which root node is "font-family"
        Typeface createFromFontFamilyFilesResourceEntry(
                Context context, FontFamilyFilesResourceEntry entry, Resources resources, int id,
                int style);

        Typeface createTypeface(Context context, @NonNull FontInfo[] fonts,
                Map<Uri, ByteBuffer> uriBuffer);
    }

    private TypefaceCompat() {}

    /**
     * Find from internal cache.
     *
     * @return null if not found.
     */
    public static Typeface findFromCache(Resources resources, int id, int style) {
        return sTypefaceCache.get(createResourceUid(resources, id, style));
    }

    /**
     * Create a unique id for a given Resource and id.
     *
     * @param resources Resources instance
     * @param id a resource id
     * @param a style to be used for this resource, -1 if not availbale.
     * @return Unique id for a given resource and id.
     */
    private static String createResourceUid(final Resources resources, int id, int style) {
        return resources.getResourcePackageName(id) + "-" + id + "-" + style;
    }

    /**
     * Create Typeface from XML resource which root node is font-family.
     *
     * @return null if failed to create.
     */
    public static Typeface createFromResourcesFamilyXml(
            Context context, FamilyResourceEntry entry, Resources resources, int id, int style,
            @Nullable TextView targetView) {
        Typeface typeface;
        if (entry instanceof ProviderResourceEntry) {
            ProviderResourceEntry providerEntry = (ProviderResourceEntry) entry;
            typeface = FontsContractCompat.getFontSync(context,
                    providerEntry.getRequest(), targetView, providerEntry.getFetchStrategy(),
                    providerEntry.getTimeout(), style);
        } else {
            typeface = sTypefaceCompatImpl.createFromFontFamilyFilesResourceEntry(
                    context, (FontFamilyFilesResourceEntry) entry, resources, id, style);
        }
        if (typeface != null) {
            sTypefaceCache.put(createResourceUid(resources, id, style), typeface);
        }
        return typeface;
    }

    /**
     * Used by Resources to load a font resource of type font file.
     */
    @Nullable
    public static Typeface createFromResourcesFontFile(
            Context context, Resources resources, int id, int style) {
        Typeface typeface = sTypefaceCompatImpl.createFromResourcesFontFile(
                context, resources, id, style);
        if (typeface != null) {
            sTypefaceCache.put(createResourceUid(resources, id, style), typeface);
        }
        return typeface;
    }

    /**
     * Create a Typeface from a given FontInfo list and a map that matches them to ByteBuffers.
     */
    public static Typeface createTypeface(Context context, @NonNull FontInfo[] fonts,
            Map<Uri, ByteBuffer> uriBuffer) {
        return sTypefaceCompatImpl.createTypeface(context, fonts, uriBuffer);
    }
}
