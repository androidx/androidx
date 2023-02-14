/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.Rect;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * An {@link ImageProxy} which overwrites the {@link ImageInfo}.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SettableImageProxy extends ForwardingImageProxy {
    private final Object mLock = new Object();
    private final ImageInfo mImageInfo;

    @GuardedBy("mLock")
    @Nullable
    private Rect mCropRect;

    private final int mWidth;
    private final int mHeight;

    /**
     * Constructor for a {@link SettableImageProxy}.
     *
     * @param imageProxy The {@link ImageProxy} to forward.
     * @param imageInfo The {@link ImageInfo} to overwrite with.
     */
    SettableImageProxy(ImageProxy imageProxy, ImageInfo imageInfo) {
        this(imageProxy, null, imageInfo);
    }

    /**
     * Constructor for a {@link SettableImageProxy} which overrides the resolution.
     *
     * @param imageProxy The {@link ImageProxy} to forward.
     * @param resolution The resolution to overwrite with.
     * @param imageInfo The {@link ImageInfo} to overwrite with.
     */
    public SettableImageProxy(@NonNull ImageProxy imageProxy, @Nullable Size resolution,
            @NonNull ImageInfo imageInfo) {
        super(imageProxy);
        if (resolution == null) {
            mWidth = super.getWidth();
            mHeight = super.getHeight();
        } else {
            mWidth = resolution.getWidth();
            mHeight = resolution.getHeight();
        }
        mImageInfo = imageInfo;
    }

    @NonNull
    @Override
    public Rect getCropRect() {
        synchronized (mLock) {
            if (mCropRect == null) {
                return new Rect(0, 0, getWidth(), getHeight());
            } else {
                return new Rect(mCropRect); // return a copy
            }
        }
    }

    @Override
    public void setCropRect(@Nullable Rect cropRect) {
        if (cropRect != null) {
            cropRect = new Rect(cropRect);  // make a copy
            if (!cropRect.intersect(0, 0, getWidth(), getHeight())) {
                cropRect.setEmpty();
            }
        }
        synchronized (mLock) {
            mCropRect = cropRect;
        }
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    @NonNull
    public ImageInfo getImageInfo() {
        return mImageInfo;
    }
}
