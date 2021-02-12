/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.aaos.renderer.surface;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class holding the information needed to render the content on a surface.
 */
//TODO(179714355): Investigate using Bundleable instead of Parcelable
@SuppressLint("BanParcelableUsage")
public final class SurfaceWrapper implements Parcelable {
    @Nullable
    private final IBinder mHostToken;
    private final int mWidth;
    private final int mHeight;
    private final int mDisplayId;
    private final int mDensityDpi;
    private final Surface mSurface;

    /**
     * Creates a {@link SurfaceWrapper}.
     *
     * @param hostToken  a token used for constructing SurfaceControlViewHost. see
     *                   {@link SurfaceView} for more details
     * @param width      the width of the surface view
     * @param height     the height of the surface view
     * @param displayId  the ID of the display showing the surface
     * @param densityDpi the density of the display showing the surface
     * @param surface    the surface for which the wrapper is created
     */
    public SurfaceWrapper(@Nullable IBinder hostToken, int width, int height, int displayId,
            int densityDpi, @NonNull Surface surface) {
        mHostToken = hostToken;
        mWidth = width;
        mHeight = height;
        mDisplayId = displayId;
        mDensityDpi = densityDpi;
        mSurface = surface;
    }

    SurfaceWrapper(@NonNull Parcel parcel) {
        mHostToken = parcel.readStrongBinder();
        mWidth = parcel.readInt();
        mHeight = parcel.readInt();
        mDisplayId = parcel.readInt();
        mDensityDpi = parcel.readInt();
        mSurface = parcel.readParcelable(Surface.class.getClassLoader());
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeStrongBinder(mHostToken);
        parcel.writeInt(mWidth);
        parcel.writeInt(mHeight);
        parcel.writeInt(mDisplayId);
        parcel.writeInt(mDensityDpi);
        parcel.writeParcelable((Parcelable) mSurface, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Nullable
    public IBinder getHostToken() {
        return mHostToken;
    }

    int getWidth() {
        return mWidth;
    }

    int getHeight() {
        return mHeight;
    }

    int getDisplayId() {
        return mDisplayId;
    }

    int getDensityDpi() {
        return mDensityDpi;
    }

    @NonNull
    Surface getSurface() {
        return mSurface;
    }

    @NonNull
    public static final Creator<SurfaceWrapper> CREATOR = new Creator<SurfaceWrapper>() {
        @NonNull
        @Override
        public SurfaceWrapper createFromParcel(@NonNull Parcel parcel) {
            return new SurfaceWrapper(parcel);
        }

        @NonNull
        @Override
        public SurfaceWrapper[] newArray(int size) {
            return new SurfaceWrapper[size];
        }
    };
}
