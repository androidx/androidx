/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.internal.compat;

import android.media.ImageWriter;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RequiresApi(26)
final class ImageWriterCompatApi26Impl {
    private static final String TAG = "ImageWriterCompatApi26";

    private static Method sNewInstanceMethod;

    static {
        try {
            sNewInstanceMethod = ImageWriter.class.getMethod("newInstance", Surface.class,
                    int.class, int.class);
        } catch (NoSuchMethodException e) {
            Log.i(TAG, "Unable to initialize via reflection.", e);
        }
    }

    @NonNull
    static ImageWriter newInstance(@NonNull Surface surface, @IntRange(from = 1) int maxImages,
            int format) {
        Throwable t = null;
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                return (ImageWriter) Preconditions.checkNotNull(
                        sNewInstanceMethod.invoke(null, surface, maxImages, format));
            } catch (IllegalAccessException | InvocationTargetException e) {
                t = e;
            }
        }

        throw new RuntimeException("Unable to invoke newInstance(Surface, int, int) via "
                + "reflection.", t);
    }

    // Class should not be instantiated.
    private ImageWriterCompatApi26Impl() {
    }
}

