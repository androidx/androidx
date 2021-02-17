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
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.SurfaceControlViewHost.SurfacePackage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A parcelable that stores either a {@link LegacySurfacePackage} or a
 * {@link android.view.SurfaceControlViewHost.SurfacePackage}.
 *
 * Please note that {@link android.view.SurfaceControlViewHost.SurfacePackage} requires API level R.
 */
//TODO(179714355): Investigate using Bundleable instead of Parcelable
@SuppressLint({"BanParcelableUsage"})
public final class SurfacePackageWrapper implements Parcelable {
    enum SurfacePackageType {
        LEGACY,
        SURFACE_CONTROL;
    }

    @Nullable
    private final Parcelable mSurfacePackage;

    /**
     * Creates a {@link SurfacePackageWrapper} that stores a
     * {@link android.view.SurfaceControlViewHost.SurfacePackage}.
     *
     * @param legacySurfacePackage the {@link android.view.SurfaceControlViewHost.SurfacePackage}
     *                             to be stored in this wrapper.
     */
    public SurfacePackageWrapper(@NonNull LegacySurfacePackage legacySurfacePackage) {
        mSurfacePackage = legacySurfacePackage;
    }

    /**
     * Creates a {@link SurfacePackageWrapper} that stores a {@link LegacySurfacePackage}.
     *
     * @param surfacePackage the {@link LegacySurfacePackage} to be stored in this wrapper.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public SurfacePackageWrapper(@NonNull SurfacePackage surfacePackage) {
        mSurfacePackage = surfacePackage;
    }

    SurfacePackageWrapper(Parcel parcel) {
        SurfacePackageType type = SurfacePackageType.values()[parcel.readInt()];
        if (type == SurfacePackageType.LEGACY) {
            mSurfacePackage = parcel.readParcelable(LegacySurfacePackage.class.getClassLoader());
        } else if (type == SurfacePackageWrapper.SurfacePackageType.SURFACE_CONTROL
                && VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mSurfacePackage = parcel.readParcelable(SurfacePackage.class.getClassLoader());
        } else {
            mSurfacePackage = null;
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        SurfacePackageWrapper.SurfacePackageType type =
                mSurfacePackage instanceof LegacySurfacePackage
                        ? SurfacePackageWrapper.SurfacePackageType.LEGACY
                        : SurfacePackageWrapper.SurfacePackageType.SURFACE_CONTROL;
        parcel.writeInt(type.ordinal());
        parcel.writeParcelable(mSurfacePackage, flags);
    }

    @Nullable
    public Parcelable getSurfacePackage() {
        return mSurfacePackage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<SurfacePackageWrapper> CREATOR =
            new Creator<SurfacePackageWrapper>() {
                @NonNull
                @Override
                public SurfacePackageWrapper createFromParcel(@NonNull Parcel parcel) {
                    return new SurfacePackageWrapper(parcel);
                }

                @NonNull
                @Override
                public SurfacePackageWrapper[] newArray(int size) {
                    return new SurfacePackageWrapper[size];
                }
            };
}

