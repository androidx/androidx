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
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import android.support.v4.provider.FontsContractCompat.FontInfo;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Implementation of the Typeface compat methods for API 14 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(14)
class TypefaceCompatBaseImpl implements TypefaceCompat.TypefaceCompatImpl {
    private static final String TAG = "TypefaceCompatBaseImpl";
    private static final String CACHE_FILE_PREFIX = "cached_font_";

    /**
     * Cache for Typeface objects dynamically loaded from assets. Currently max size is 16.
     */
    private static final LruCache<String, Typeface> sDynamicTypefaceCache =
            new LruCache<>(16);

    private final Context mApplicationContext;

    TypefaceCompatBaseImpl(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    @Override
    public Typeface createTypeface(
            @NonNull FontInfo[] fonts, Map<Uri, ByteBuffer> uriBuffer) {
        // When we load from file, we can only load one font so just take the first one.
        if (fonts.length < 1) {
            return null;
        }
        Typeface typeface = null;
        FontInfo font = fonts[0];
        ByteBuffer buffer = uriBuffer.get(font.getUri());
        File tmpFile = copyToCacheFile(buffer);
        if (tmpFile != null) {
            try {
                typeface = Typeface.createFromFile(tmpFile.getPath());
            } catch (RuntimeException e) {
                // This was thrown from Typeface.createFromFile when a Typeface could not be loaded,
                // such as due to an invalid ttf or unreadable file. We don't want to throw that
                // exception anymore.
                return null;
            } finally {
                tmpFile.delete();
            }
        }
        return typeface;
    }

    private File copyToCacheFile(final InputStream is) {
        FileOutputStream fos = null;
        File cacheFile;
        try {
            cacheFile = new File(mApplicationContext.getCacheDir(),
                    CACHE_FILE_PREFIX + Thread.currentThread().getId());
            fos = new FileOutputStream(cacheFile, false);

            byte[] buffer = new byte[1024];
            int readLen;
            while ((readLen = is.read(buffer)) != -1) {
                fos.write(buffer, 0, readLen);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying font file descriptor to temp local file.", e);
            return null;
        } finally {
            closeQuietly(is);
            closeQuietly(fos);
        }
        return cacheFile;
    }

    private File copyToCacheFile(final ByteBuffer is) {
        FileOutputStream fos = null;
        File cacheFile;
        try {
            cacheFile = new File(mApplicationContext.getCacheDir(),
                    CACHE_FILE_PREFIX + Thread.currentThread().getId());
            fos = new FileOutputStream(cacheFile, false);

            byte[] buffer = new byte[1024];
            while (is.hasRemaining()) {
                int len = Math.min(1024, is.remaining());
                is.get(buffer, 0, len);
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying font file descriptor to temp local file.", e);
            return null;
        } finally {
            closeQuietly(fos);
        }
        return cacheFile;
    }

    static void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException io) {
                Log.e(TAG, "Error closing input stream", io);
            }
        }
    }

    @Nullable
    @Override
    public Typeface createFromResourcesFontFile(Resources resources, int id, int style) {
        InputStream is = null;
        try {
            is = resources.openRawResource(id);
            Typeface typeface = createTypeface(resources, is);
            if (typeface == null) {
                return null;
            }
            final String key = createAssetUid(resources, id, style);
            sDynamicTypefaceCache.put(key, typeface);
            return typeface;
        } catch (IOException e) {
            return null;
        } finally {
            closeQuietly(is);
        }
    }

    @Nullable
    @Override
    public Typeface createFromFontFamilyFilesResourceEntry(
            FontFamilyFilesResourceEntry filesEntry, Resources resources, int id, int style) {
        Typeface typeface = createFromResources(filesEntry, resources, id, style);
        if (typeface != null) {
            final String key = createAssetUid(resources, id, style);
            sDynamicTypefaceCache.put(key, typeface);
        }
        return typeface;
    }

    private FontFileResourceEntry findBestEntry(FontFamilyFilesResourceEntry entry,
            int targetWeight, boolean isTargetItalic) {
        FontFileResourceEntry bestEntry = null;
        int bestScore = Integer.MAX_VALUE;  // smaller is better

        for (final FontFileResourceEntry e : entry.getEntries()) {
            final int score = (Math.abs(e.getWeight() - targetWeight) * 2)
                    + (isTargetItalic == e.isItalic() ? 0 : 1);

            if (bestEntry == null || bestScore > score) {
                bestEntry = e;
                bestScore = score;
            }
        }
        return bestEntry;
    }

    /**
     * Implementation of resources font retrieval for a file type xml resource. This should be
     * overriden by other implementations.
     */
    @Nullable
    Typeface createFromResources(FontFamilyFilesResourceEntry entry, Resources resources,
            int id, int style) {
        FontFileResourceEntry best = findBestEntry(
                entry, ((style & Typeface.BOLD) == 0) ? 400 : 700, (style & Typeface.ITALIC) != 0);
        if (best == null) {
            return null;
        }

        InputStream is = null;
        try {
            is = resources.openRawResource(best.getResourceId());
            return createTypeface(resources, is);
        } catch (IOException e) {
            // This is fine. The resource can be string type which indicates a name of Typeface.
        } finally {
            closeQuietly(is);
        }
        return null;
    }

    @Override
    public Typeface findFromCache(Resources resources, int id, int style) {
        final String key = createAssetUid(resources, id, style);
        synchronized (sDynamicTypefaceCache) {
            return sDynamicTypefaceCache.get(key);
        }
    }

    /**
     * Creates a unique id for a given AssetManager and asset id
     *
     * @param resources Resources instance
     * @param id a resource id
     * @param style a style to be used for this resource, -1 if not avaialble.
     * @return Unique id for a given AssetManager and id
     */
    private static String createAssetUid(final Resources resources, int id, int style) {
        return resources.getResourcePackageName(id) + "-" + id + "-" + style;
    }

    // Caller must close "is"
    Typeface createTypeface(Resources resources, InputStream is) throws IOException {
        File tmpFile = copyToCacheFile(is);
        if (tmpFile != null) {
            try {
                return Typeface.createFromFile(tmpFile.getPath());
            } catch (RuntimeException e) {
                // This was thrown from Typeface.createFromFile when a Typeface could not be loaded,
                // such as due to an invalid ttf or unreadable file. We don't want to throw that
                // exception anymore.
                android.util.Log.e(TAG, "Failed to create font", e);
                return null;
            } finally {
                tmpFile.delete();
            }
        }
        return null;
    }

    static void closeQuietly(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException io) {
                Log.e(TAG, "Error closing stream", io);
            }
        }
    }
}
