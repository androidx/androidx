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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import java.util.Iterator;

/** @hide */
@RestrictTo(LIBRARY)
class GpsStatusWrapper extends GnssStatusCompat {

    private static final int GPS_PRN_OFFSET = 0;
    private static final int GPS_PRN_COUNT = 32;
    private static final int SBAS_PRN_MIN = 33;
    private static final int SBAS_PRN_MAX = 64;
    private static final int SBAS_PRN_OFFSET = -87;
    private static final int GLONASS_PRN_OFFSET = 64;
    private static final int GLONASS_PRN_COUNT = 24;
    private static final int QZSS_SVID_MIN = 193;
    private static final int QZSS_SVID_MAX = 200;
    private static final int BEIDOU_PRN_OFFSET = 200;
    private static final int BEIDOU_PRN_COUNT = 35;

    private final GpsStatus mWrapped;

    @GuardedBy("mWrapped")
    private int mCachedSatelliteCount;

    @GuardedBy("mWrapped")
    private Iterator<GpsSatellite> mCachedIterator;
    @GuardedBy("mWrapped")
    private int mCachedIteratorPosition;
    @GuardedBy("mWrapped")
    private GpsSatellite mCachedSatellite;

    GpsStatusWrapper(GpsStatus gpsStatus) {
        mWrapped = Preconditions.checkNotNull(gpsStatus);
        mCachedSatelliteCount = -1;
        mCachedIterator = mWrapped.getSatellites().iterator();
        mCachedIteratorPosition = -1;
        mCachedSatellite = null;
    }

    @Override
    public int getSatelliteCount() {
        synchronized (mWrapped) {
            if (mCachedSatelliteCount == -1) {
                for (@SuppressWarnings("unused") GpsSatellite ignored : mWrapped.getSatellites()) {
                    mCachedSatelliteCount++;
                }
                mCachedSatelliteCount++;
            }

            return mCachedSatelliteCount;
        }
    }

    @Override
    public int getConstellationType(int satelliteIndex) {
        if (VERSION.SDK_INT < VERSION_CODES.N) {
            return CONSTELLATION_GPS;
        } else {
            return getConstellationFromPrn(getSatellite(satelliteIndex).getPrn());
        }
    }

    @Override
    public int getSvid(int satelliteIndex) {
        if (VERSION.SDK_INT < VERSION_CODES.N) {
            return getSatellite(satelliteIndex).getPrn();
        } else {
            return getSvidFromPrn(getSatellite(satelliteIndex).getPrn());
        }
    }

    @Override
    public float getCn0DbHz(int satelliteIndex) {
        return getSatellite(satelliteIndex).getSnr();
    }

    @Override
    public float getElevationDegrees(int satelliteIndex) {
        return getSatellite(satelliteIndex).getElevation();
    }

    @Override
    public float getAzimuthDegrees(int satelliteIndex) {
        return getSatellite(satelliteIndex).getAzimuth();
    }

    @Override
    public boolean hasEphemerisData(int satelliteIndex) {
        return getSatellite(satelliteIndex).hasEphemeris();
    }

    @Override
    public boolean hasAlmanacData(int satelliteIndex) {
        return getSatellite(satelliteIndex).hasAlmanac();
    }

    @Override
    public boolean usedInFix(int satelliteIndex) {
        return getSatellite(satelliteIndex).usedInFix();
    }

    @Override
    public boolean hasCarrierFrequencyHz(int satelliteIndex) {
        return false;
    }

    @Override
    public float getCarrierFrequencyHz(int satelliteIndex) {
        throw new UnsupportedOperationException();
    }

    private GpsSatellite getSatellite(int satelliteIndex) {
        GpsSatellite satellite;
        synchronized (mWrapped) {
            if (satelliteIndex < mCachedIteratorPosition) {
                mCachedIterator = mWrapped.getSatellites().iterator();
                mCachedIteratorPosition = -1;
            }
            while (mCachedIteratorPosition < satelliteIndex) {
                mCachedIteratorPosition++;
                if (!mCachedIterator.hasNext()) {
                    mCachedSatellite = null;
                    break;
                } else {
                    mCachedSatellite = mCachedIterator.next();
                }
            }
            satellite = mCachedSatellite;
        }
        return Preconditions.checkNotNull(satellite);
    }

    private static int getConstellationFromPrn(int prn) {
        if (prn > GPS_PRN_OFFSET && prn <= GPS_PRN_OFFSET + GPS_PRN_COUNT) {
            return CONSTELLATION_GPS;
        } else if (prn >= SBAS_PRN_MIN && prn <= SBAS_PRN_MAX) {
            return CONSTELLATION_SBAS;
        } else if (prn > GLONASS_PRN_OFFSET && prn <= GLONASS_PRN_OFFSET + GLONASS_PRN_COUNT) {
            return CONSTELLATION_GLONASS;
        } else if (prn > BEIDOU_PRN_OFFSET && prn <= BEIDOU_PRN_OFFSET + BEIDOU_PRN_COUNT) {
            return CONSTELLATION_BEIDOU;
        } else if (prn >= QZSS_SVID_MIN && prn <= QZSS_SVID_MAX) {
            return CONSTELLATION_QZSS;
        } else {
            return CONSTELLATION_UNKNOWN;
        }
    }

    private static int getSvidFromPrn(int prn) {
        switch (getConstellationFromPrn(prn)) {
            case CONSTELLATION_SBAS:
                prn -= SBAS_PRN_OFFSET;
                break;
            case CONSTELLATION_GLONASS:
                prn -= GLONASS_PRN_OFFSET;
                break;
            case CONSTELLATION_BEIDOU:
                prn -= BEIDOU_PRN_OFFSET;
                break;
        }
        return prn;
    }
}
