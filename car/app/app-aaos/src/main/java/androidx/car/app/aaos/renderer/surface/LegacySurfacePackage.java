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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

/**
 * A serializable class containing all the data required to render and interact with a surface from
 * an off-process renderer.
 *
 * This class exists for compatibility with Q devices. In Android R and later,
 * {@link android.view.SurfaceControlViewHost.SurfacePackage} will be used instead.
 */
//TODO(179714355): Investigate using Bundleable instead of Parcelable
@SuppressLint({"BanParcelableUsage"})
public final class LegacySurfacePackage implements Parcelable {
    private final ISurfaceControl mSurfaceControl;

    /**
     * Creates a {@link LegacySurfacePackage}.
     *
     * @param callback a {@link SurfaceControlCallback} to be registered to receive off-process
     *                 renderer events affecting the {@link android.view.SurfaceView} that
     *                 content is rendered on.
     */
    @SuppressLint("ExecutorRegistration")
    public LegacySurfacePackage(@NonNull SurfaceControlCallback callback) {
        mSurfaceControl = new ISurfaceControl.Stub() {
            final SurfaceControlCallback mCallback = callback;

            @Override
            public void setSurfaceWrapper(@NonNull SurfaceWrapper surfaceWrapper) {
                requireNonNull(surfaceWrapper);
                if (mCallback != null) {
                    mCallback.setSurfaceWrapper(surfaceWrapper);
                }
            }

            @Override
            public void onWindowFocusChanged(boolean hasFocus, boolean isInTouchMode) {
                if (mCallback != null) {
                    mCallback.onWindowFocusChanged(hasFocus, isInTouchMode);
                }
            }

            @Override
            public void onTouchEvent(@NonNull MotionEvent event) {
                requireNonNull(event);
                if (mCallback != null) {
                    mCallback.onTouchEvent(event);
                }
            }
        };
    }

    LegacySurfacePackage(@NonNull Parcel parcel) {
        mSurfaceControl = ISurfaceControl.Stub.asInterface(parcel.readStrongBinder());
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeStrongInterface(mSurfaceControl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    ISurfaceControl getSurfaceControl() {
        return mSurfaceControl;
    }

    @NonNull
    public static final Creator<LegacySurfacePackage> CREATOR =
            new Creator<LegacySurfacePackage>() {
                @NonNull
                @Override
                public LegacySurfacePackage createFromParcel(@NonNull Parcel parcel) {
                    return new LegacySurfacePackage(parcel);
                }

                @NonNull
                @Override
                public LegacySurfacePackage[] newArray(int size) {
                    return new LegacySurfacePackage[size];
                }
            };
}

