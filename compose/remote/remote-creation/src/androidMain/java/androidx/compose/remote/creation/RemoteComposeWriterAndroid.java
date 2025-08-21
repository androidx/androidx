/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation;

import android.graphics.Bitmap;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.profile.Profile;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class RemoteComposeWriterAndroid extends RemoteComposeWriter {
    private final @NonNull Painter mPainter = new Painter(this);

    public RemoteComposeWriterAndroid(int width, int height,
            @NonNull String contentDescription,
            @NonNull Platform platform) {
        super(width, height, contentDescription, platform);
    }

    public RemoteComposeWriterAndroid(int width, int height, @NonNull String contentDescription,
            int apilLevel, int profiles, @NonNull Platform platform) {
        super(width, height, contentDescription, apilLevel, profiles, platform);
    }

    public RemoteComposeWriterAndroid(@NonNull Platform platform, int apiLevel,
            HTag @NonNull ... tags) {
        super(platform, apiLevel, tags);
    }

    public RemoteComposeWriterAndroid(@NonNull Platform platform, HTag @NonNull ... tags) {
        super(platform, tags);
    }

    protected RemoteComposeWriterAndroid(
            @NonNull Profile profile,
            @NonNull RemoteComposeBuffer buffer, HTag @NonNull ... tags) {
        super(profile, buffer, tags);
    }

    /**
     * Add a bitmap to the document
     *
     * @param image a Bitmap object
     * @param contentDescription a description for the image
     */
    public void drawBitmap(@NonNull Bitmap image, @Nullable String contentDescription) {
        super.drawBitmap(image, image.getWidth(), image.getHeight(), contentDescription);
    }

    /**
     * Add a matrix constant
     *
     * @param m matrix
     * @return float id of the property
     */
    public float addMatrixConst(android.graphics.@NonNull Matrix m) {
        float[] values = new float[9];
        m.getValues(values);
        int id = mState.createNextAvailableId();
        mBuffer.addMatrixConst(id, values);
        return Utils.asNan(id);
    }


    /**
     * Reuse the painter associated with this connection
     *
     * @return
     */
    public @NonNull Painter getPainter() {
        return mPainter;
    }
}
