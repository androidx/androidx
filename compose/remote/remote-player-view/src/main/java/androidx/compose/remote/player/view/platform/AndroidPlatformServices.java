/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.player.view.platform;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PathIterator;
import android.os.Build;
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.operations.PathData;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/** Services that are needed to be provided by the platform during encoding. */
public class AndroidPlatformServices implements Platform {
    private static final String LOG_TAG = "RemoteCompose";

    @Override
    public byte[] imageToByteArray(@NonNull Object image) {
        if (image instanceof Bitmap) {
            // let's create a bitmap
            ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
            ((Bitmap) image).compress(Bitmap.CompressFormat.PNG, 90, byteArrayBitmapStream);
            return byteArrayBitmapStream.toByteArray();
        }
        return null;
    }

    @Override
    public int getImageWidth(@NonNull Object image) {
        if (image instanceof Bitmap) {
            return ((Bitmap) image).getWidth();
        }
        return 0;
    }

    @Override
    public int getImageHeight(@NonNull Object image) {
        if (image instanceof Bitmap) {
            return ((Bitmap) image).getHeight();
        }
        return 0;
    }

    @Override
    public boolean isAlpha8Image(@NonNull Object image) {
        if (image instanceof Bitmap) {
            return ((Bitmap) image).getConfig().equals(Bitmap.Config.ALPHA_8);
        }
        return false;
    }

    @Override
    public float @Nullable [] pathToFloatArray(@NonNull Object path) {
        //        if (path is RemotePath) {
        //            return path.createFloatArray()
        //        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // REMOVE IN PLATFORM
            if (path instanceof Path) {
                return androidPathToFloatArray((Path) path);
            }
        } // REMOVE IN PLATFORM
        return null;
    }

    @Override
    public void log(@NonNull LogCategory category, @NonNull String message) {
        switch (category) {
            case DEBUG:
                Log.d(LOG_TAG, message);
                break;
            case INFO:
                Log.i(LOG_TAG, message);
                break;
            case WARN:
                Log.w(LOG_TAG, message);
                break;
            default:
                Log.e(LOG_TAG, message);
                break;
        }
    }

    @androidx.annotation.RequiresApi(// REMOVE IN PLATFORM
            api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE) // REMOVE IN PLATFORM
    private float @NonNull [] androidPathToFloatArray(@NonNull Path path) {
        PathIterator i = path.getPathIterator();
        int estimatedSize = 0;

        while (i.hasNext()) {
            i.next();
            estimatedSize++;
        }

        PathIterator iter = path.getPathIterator();
        float[] pathFloat = new float[estimatedSize * 10];

        int count = 0;
        while (i.hasNext()) {
            PathIterator.Segment seg = i.next();

            switch (seg.getVerb()) {
                case PathIterator.VERB_MOVE:
                    pathFloat[count++] = PathData.MOVE_NAN;
                    break;
                case PathIterator.VERB_LINE:
                    pathFloat[count++] = PathData.LINE_NAN;
                    break;
                case PathIterator.VERB_QUAD:
                    pathFloat[count++] = PathData.QUADRATIC_NAN;
                    break;
                case PathIterator.VERB_CONIC:
                    pathFloat[count++] = PathData.CONIC_NAN;
                    break;
                case PathIterator.VERB_CUBIC:
                    pathFloat[count++] = PathData.CUBIC_NAN;
                    break;
                case PathIterator.VERB_CLOSE:
                    pathFloat[count++] = PathData.CLOSE_NAN;
                    break;
                case PathIterator.VERB_DONE:
                    pathFloat[count++] = PathData.DONE_NAN;
                    break;
            }
            for (float p : seg.getPoints()) {
                pathFloat[count++] = p;
            }
            if (seg.getVerb() == PathIterator.VERB_CONIC) {
                pathFloat[count++] = seg.getConicWeight();
            }
        }

        return Arrays.copyOf(pathFloat, count);
    }
}
