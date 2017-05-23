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

    @Override
    public Typeface createTypeface(Context context, @NonNull FontInfo[] fonts,
            Map<Uri, ByteBuffer> uriBuffer) {
        // When we load from file, we can only load one font so just take the first one.
        if (fonts.length < 1) {
            return null;
        }
        Typeface typeface = null;
        FontInfo font = fonts[0];
        ByteBuffer buffer = uriBuffer.get(font.getUri());
        File tmpFile = copyToCacheFile(context, buffer);
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

    private static File copyToCacheFile(Context context, final InputStream is) {
        FileOutputStream fos = null;
        File cacheFile;
        try {
            cacheFile = new File(context.getCacheDir(),
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

    private static File copyToCacheFile(Context context, final ByteBuffer is) {
        FileOutputStream fos = null;
        File cacheFile;
        try {
            cacheFile = new File(context.getCacheDir(),
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

    private static void closeQuietly(InputStream is) {
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
    public Typeface createFromResourcesFontFile(Context context, Resources resources, int id,
            int style) {
        InputStream is = null;
        try {
            is = resources.openRawResource(id);
            return createTypeface(context, resources, is);
        } catch (IOException e) {
            return null;
        } finally {
            closeQuietly(is);
        }
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

    @Nullable
    @Override
    public Typeface createFromFontFamilyFilesResourceEntry(Context context,
            FontFamilyFilesResourceEntry entry, Resources resources, int id, int style) {
        FontFileResourceEntry best = findBestEntry(
                entry, ((style & Typeface.BOLD) == 0) ? 400 : 700, (style & Typeface.ITALIC) != 0);
        if (best == null) {
            return null;
        }

        InputStream is = null;
        try {
            is = resources.openRawResource(best.getResourceId());
            return createTypeface(context, resources, is);
        } catch (IOException e) {
            // This is fine. The resource can be string type which indicates a name of Typeface.
        } finally {
            closeQuietly(is);
        }
        return null;
    }

    // Caller must close "is"
    Typeface createTypeface(Context context, Resources resources, InputStream is)
            throws IOException {
        File tmpFile = copyToCacheFile(context, is);
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
