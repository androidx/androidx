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
import android.os.Build;
import android.os.CancellationSignal;
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

/**
 * Helper for accessing features in {@link Typeface}.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class TypefaceCompat {
    private static final String TAG = "TypefaceCompat";

    private static final TypefaceCompatImpl sTypefaceCompatImpl;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sTypefaceCompatImpl = new TypefaceCompatApi26Impl();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && TypefaceCompatApi24Impl.isUsable()) {
            sTypefaceCompatImpl = new TypefaceCompatApi24Impl();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sTypefaceCompatImpl = new TypefaceCompatApi21Impl();
        } else {
            sTypefaceCompatImpl = new TypefaceCompatBaseImpl();
        }
    }

    /**
     * Cache for Typeface objects dynamically loaded from assets.
     */
    private static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);

    interface TypefaceCompatImpl {
        // Create Typeface from XML which root node is "font-family"
        Typeface createFromFontFamilyFilesResourceEntry(
                Context context, FontFamilyFilesResourceEntry entry, Resources resources,
                int style);

        Typeface createFromFontInfo(Context context,
                @Nullable CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts,
                int style);

        Typeface createFromResourcesFontFile(
                Context context, Resources resources, int id, String path, int style);
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
     * @param style style to be used for this resource, -1 if not available.
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
                    context, (FontFamilyFilesResourceEntry) entry, resources, style);
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
            Context context, Resources resources, int id, String path, int style) {
        Typeface typeface = sTypefaceCompatImpl.createFromResourcesFontFile(
                context, resources, id, path, style);
        if (typeface != null) {
            sTypefaceCache.put(createResourceUid(resources, id, style), typeface);
        }
        return typeface;
    }

    /**
     * Create a Typeface from a given FontInfo list and a map that matches them to ByteBuffers.
     */
    public static Typeface createFromFontInfo(Context context,
            @Nullable CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts, int style) {
        return sTypefaceCompatImpl.createFromFontInfo(context, cancellationSignal, fonts, style);
    }
}
