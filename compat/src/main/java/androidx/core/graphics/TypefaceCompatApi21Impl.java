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
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.provider.FontsContractCompat.FontInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * Implementation of the Typeface compat methods for API 21 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(21)
class TypefaceCompatApi21Impl extends TypefaceCompatBaseImpl {
    private static final String TAG = "TypefaceCompatApi21Impl";

    private File getFile(ParcelFileDescriptor fd) {
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

    @Override
    public Typeface createFromFontInfo(Context context, CancellationSignal cancellationSignal,
            @NonNull FontInfo[] fonts, int style) {
        if (fonts.length < 1) {
            return null;
        }
        final FontInfo bestFont = findBestInfo(fonts, style);
        final ContentResolver resolver = context.getContentResolver();
        try (ParcelFileDescriptor pfd =
                     resolver.openFileDescriptor(bestFont.getUri(), "r", cancellationSignal)) {
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
}
