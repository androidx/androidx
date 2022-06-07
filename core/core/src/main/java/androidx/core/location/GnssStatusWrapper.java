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

package androidx.core.location;

import static android.os.Build.VERSION;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.location.GnssStatus;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/** @hide */
@RestrictTo(LIBRARY)
@RequiresApi(24)
class GnssStatusWrapper extends GnssStatusCompat {

    private final GnssStatus mWrapped;

    GnssStatusWrapper(Object gnssStatus) {
        mWrapped = Preconditions.checkNotNull((GnssStatus) gnssStatus);
    }

    @Override
    public int getSatelliteCount() {
        return mWrapped.getSatelliteCount();
    }

    @Override
    public int getConstellationType(int satelliteIndex) {
        return mWrapped.getConstellationType(satelliteIndex);
    }

    @Override
    public int getSvid(int satelliteIndex) {
        return mWrapped.getSvid(satelliteIndex);
    }

    @Override
    public float getCn0DbHz(int satelliteIndex) {
        return mWrapped.getCn0DbHz(satelliteIndex);
    }

    @Override
    public float getElevationDegrees(int satelliteIndex) {
        return mWrapped.getElevationDegrees(satelliteIndex);
    }

    @Override
    public float getAzimuthDegrees(int satelliteIndex) {
        return mWrapped.getAzimuthDegrees(satelliteIndex);
    }

    @Override
    public boolean hasEphemerisData(int satelliteIndex) {
        return mWrapped.hasEphemerisData(satelliteIndex);
    }

    @Override
    public boolean hasAlmanacData(int satelliteIndex) {
        return mWrapped.hasAlmanacData(satelliteIndex);
    }

    @Override
    public boolean usedInFix(int satelliteIndex) {
        return mWrapped.usedInFix(satelliteIndex);
    }

    @Override
    public boolean hasCarrierFrequencyHz(int satelliteIndex) {
        if (VERSION.SDK_INT >= 26) {
            return Api26Impl.hasCarrierFrequencyHz(mWrapped, satelliteIndex);
        } else {
            return false;
        }
    }

    @Override
    public float getCarrierFrequencyHz(int satelliteIndex) {
        if (VERSION.SDK_INT >= 26) {
            return Api26Impl.getCarrierFrequencyHz(mWrapped, satelliteIndex);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean hasBasebandCn0DbHz(int satelliteIndex) {
        if (VERSION.SDK_INT >= 30) {
            return Api30Impl.hasBasebandCn0DbHz(mWrapped, satelliteIndex);
        } else {
            return false;
        }
    }

    @Override
    public float getBasebandCn0DbHz(int satelliteIndex) {
        if (VERSION.SDK_INT >= 30) {
            return Api30Impl.getBasebandCn0DbHz(mWrapped, satelliteIndex);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GnssStatusWrapper)) {
            return false;
        }
        GnssStatusWrapper that = (GnssStatusWrapper) o;
        return mWrapped.equals(that.mWrapped);
    }

    @Override
    public int hashCode() {
        return mWrapped.hashCode();
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static float getCarrierFrequencyHz(GnssStatus gnssStatus, int satelliteIndex) {
            return gnssStatus.getCarrierFrequencyHz(satelliteIndex);
        }

        @DoNotInline
        static boolean hasCarrierFrequencyHz(GnssStatus gnssStatus, int satelliteIndex) {
            return gnssStatus.hasCarrierFrequencyHz(satelliteIndex);
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean hasBasebandCn0DbHz(GnssStatus gnssStatus, int satelliteIndex) {
            return gnssStatus.hasBasebandCn0DbHz(satelliteIndex);
        }

        @DoNotInline
        static float getBasebandCn0DbHz(GnssStatus gnssStatus, int satelliteIndex) {
            return gnssStatus.getBasebandCn0DbHz(satelliteIndex);
        }
    }
}
