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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.hardware.common.PropertyUtils.CAR_ZONE_TO_AREA_ID;

import android.car.hardware.CarPropertyValue;

import androidx.annotation.GuardedBy;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A caching class for {@link CarPropertyResponse} and the {@link OnCarPropertyResponseListener}'s
 * that need those responses.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
final class PropertyResponseCache {
    private final Object mLock = new Object();

    // key: property Id, value: listener which registered for the key value
    @GuardedBy("mLock")
    private final Map<PropertyIdAreaId, List<OnCarPropertyResponseListener>> mUIdToListeners =
            new HashMap<>();

    // key: listener, value: properties
    @GuardedBy("mLock")
    private final Map<OnCarPropertyResponseListener, List<PropertyIdAreaId>> mListenerToUIds =
            new HashMap<>();

    // cache for car property values.
    @GuardedBy("mLock")
    private final Map<PropertyIdAreaId, CarPropertyResponse<?>> mUIdToResponse = new HashMap<>();

    /**
     * Puts the listener and a list of properties that are registered by the listener into cache.
     */
    void putListenerAndUIds(OnCarPropertyResponseListener listener, List<PropertyIdAreaId> uIds) {
        synchronized (mLock) {
            mListenerToUIds.put(listener, uIds);
            for (PropertyIdAreaId uId : uIds) {
                List<OnCarPropertyResponseListener> listenerList =
                        mUIdToListeners.getOrDefault(uId, new ArrayList<>());
                listenerList.add(listener);
                mUIdToListeners.put(uId, listenerList);

                // add an init value if needed
                if (mUIdToResponse.get(uId) == null) {
                    mUIdToResponse.put(uId, CarPropertyResponse.builder()
                            .setPropertyId(uId.getPropertyId())
                            .setStatus(CarValue.STATUS_UNKNOWN).build());
                }
            }
        }
    }

    /** Returns a list of properties that are registered by the listener. */
    List<PropertyIdAreaId> getUIdsByListener(OnCarPropertyResponseListener listener) {
        synchronized (mLock) {
            return mListenerToUIds.getOrDefault(listener, Collections.emptyList());
        }
    }

    /**
     * Returns a {@link List} containing all {@link OnCarPropertyResponseListener} registered the
     * property.
     */
    List<OnCarPropertyResponseListener> getListenersByUId(PropertyIdAreaId uId) {
        synchronized (mLock) {
            return mUIdToListeners.getOrDefault(uId, new ArrayList<>());
        }
    }

    /** Gets a list of {@link CarPropertyResponse} that need to be dispatched to the listener. */
    List<CarPropertyResponse<?>> getResponsesByListener(OnCarPropertyResponseListener listener) {
        List<CarPropertyResponse<?>> values = new ArrayList<>();
        synchronized (mLock) {
            List<PropertyIdAreaId> uIds = mListenerToUIds.get(listener);
            if (uIds == null) {
                return values;
            }

            for (PropertyIdAreaId uId : uIds) {
                // Return a response with unknown status if cannot find in cache.
                CarPropertyResponse<?> propertyResponse = mUIdToResponse.getOrDefault(uId,
                        CarPropertyResponse.builder().setPropertyId(uId.getPropertyId())
                                .setStatus(CarValue.STATUS_UNKNOWN).build());
                values.add(propertyResponse);
            }
        }
        return values;
    }

    /**
     * Removes the listener and related {@link CarPropertyResponse} from cache.
     *
     * @return a list of property ids that are not registered by any other listener
     */
    List<PropertyIdAreaId> removeListener(OnCarPropertyResponseListener listener) {
        List<PropertyIdAreaId> propertyWithOutListener = new ArrayList<>();
        synchronized (mLock) {
            List<PropertyIdAreaId> uIds = mListenerToUIds.get(listener);
            mListenerToUIds.remove(listener);
            if (uIds == null) {
                throw new IllegalStateException("Listener is not registered yet");
            }
            for (PropertyIdAreaId uId : uIds) {
                List<OnCarPropertyResponseListener> listenerList = mUIdToListeners.getOrDefault(uId,
                                new ArrayList<>());
                listenerList.remove(listener);
                if (listenerList.isEmpty()) {
                    propertyWithOutListener.add(uId);
                    mUIdToListeners.remove(uId);
                    mUIdToResponse.remove(uId);
                }
            }
        }
        return propertyWithOutListener;
    }

    /** Returns {@code true} if the value in cache is updated. */
    boolean updateResponseIfNeeded(CarPropertyValue<?> propertyValue) {
        synchronized (mLock) {
            PropertyIdAreaId uId = PropertyIdAreaId.builder()
                    .setPropertyId(propertyValue.getPropertyId())
                    .setAreaId(propertyValue.getAreaId()).build();

            CarPropertyResponse<?> responseInCache = mUIdToResponse.get(uId);

            if (responseInCache == null) {
                // the property is unregistered
                return false;
            }

            long timestampMs = TimeUnit.MILLISECONDS.convert(propertyValue.getTimestamp(),
                    TimeUnit.NANOSECONDS);
            // CarService can not guarantee the order of events.
            if (responseInCache.getTimestampMillis() <= timestampMs) {
                CarPropertyResponse<?> response =
                        PropertyUtils.convertPropertyValueToPropertyResponse(propertyValue);
                mUIdToResponse.put(uId, response);
                return true;
            }
            return false;
        }
    }

    /** Updates the error event in cache */
    @OptIn(markerClass = ExperimentalCarApi.class)
    void updateInternalError(CarInternalError internalError) {
        List<CarZone> carZones = new ArrayList<CarZone>();
        carZones.add(CAR_ZONE_TO_AREA_ID.inverse().get(internalError.getAreaId()));
        CarPropertyResponse<?> response = CarPropertyResponse.builder().setPropertyId(
                internalError.getPropertyId()).setCarZones(carZones).setStatus(
                internalError.getErrorCode()).build();
        PropertyIdAreaId uId = PropertyIdAreaId.builder()
                .setPropertyId(internalError.getPropertyId())
                .setAreaId(internalError.getAreaId()).build();
        synchronized (mLock) {
            mUIdToResponse.put(uId, response);
        }
    }
}
