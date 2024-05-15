/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RestrictTo;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handles extracting Exif data for content Uri thumbnails, which don't have exif data embedded.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ExifThumbnailUtils {
    private static final String TAG = ExifThumbnailUtils.class.getSimpleName();

    private ExifThumbnailUtils() {
    }

    /**
     * Get the {@link ExifInterface#TAG_ORIENTATION} value for the file represented by the
     * contentUri.
     *
     * <p>Normally, we read the Exif orientation data from the file itself, but before Android Q,
     * Exif
     * data is not transferred to lo-res thumbnails requested from the ContentResolver, so this
     * gives
     * us a way to read the original file's Exif orientation and apply it manually to the Thumbnail.
     */
    public static int getExifOrientation(Uri contentUri, ContentResolver contentResolver) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            // On Q and above, the system takes care of applying the exif orientation to the
            // thumbnail.
            return 0;
        }
        try {
            InputStream is = contentResolver.openInputStream(contentUri);
            if (is == null) {
                return 0;
            }
            ExifInterface exifInterface = new ExifInterface(is);
            return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
        } catch (IOException e) {
            ErrorLog.log(TAG, "Unable to getExifOrientation.", e);
        }
        return 0;
    }
}
