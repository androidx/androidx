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

import android.car.hardware.CarPropertyValue;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SparseArray<Set<OnCarPropertyResponseListener>> mPropertyIdToListeners =
            new SparseArray<>();

    // key: listener, value: properties
    @GuardedBy("mLock")
    private final Map<OnCarPropertyResponseListener, List<Integer>> mListenerToPropertyIds =
            new HashMap<>();

    // cache for car property values.
    @GuardedBy("mLock")
    private final SparseArray<CarPropertyResponse<?>> mPropertyIdToResponse = new SparseArray<>();

    /**
     * Puts the listener and a list of properties that are registered by the listener into cache.
     */
    void putListenerAndPropertyIds(OnCarPropertyResponseListener listener,
            List<Integer> propertyIds) {
        synchronized (mLock) {
            mListenerToPropertyIds.put(listener, propertyIds);
            for (int propertyId : propertyIds) {
                Set<OnCarPropertyResponseListener> listenerSet =
                        mPropertyIdToListeners.get(propertyId, new HashSet<>());
                listenerSet.add(listener);
                mPropertyIdToListeners.put(propertyId, listenerSet);

                // add an init value if needed
                if (mPropertyIdToResponse.get(propertyId) == null) {
                    mPropertyIdToResponse.put(propertyId,
                            CarPropertyResponse.createErrorResponse(propertyId,
                                    CarValue.STATUS_UNAVAILABLE));
                }
            }
        }
    }

    /** Returns a list of properties that are registered by the listener. */
    List<Integer> getPropertyIdsByListener(OnCarPropertyResponseListener listener) {
        synchronized (mLock) {
            return mListenerToPropertyIds.getOrDefault(listener, Collections.emptyList());
        }
    }

    /**
     * Returns a {@link Set} containing all {@link OnCarPropertyResponseListener} registered the
     * property.
     */
    Set<OnCarPropertyResponseListener> getListenersByPropertyId(int propertyId) {
        synchronized (mLock) {
            return mPropertyIdToListeners.get(propertyId);
        }
    }

    /** Gets a list of {@link CarPropertyResponse} that need to be dispatched to the listener. */
    List<CarPropertyResponse<?>> getResponsesByListener(
            OnCarPropertyResponseListener listener) {
        List<CarPropertyResponse<?>> values = new ArrayList<>();
        synchronized (mLock) {
            List<Integer> propertyIds = mListenerToPropertyIds.get(listener);
            if (propertyIds == null) {
                return values;
            }
            for (int propertyId : propertyIds) {
                // return a response with unavailable status if can not find in cache
                CarPropertyResponse<?> propertyResponse = mPropertyIdToResponse.get(propertyId,
                        CarPropertyResponse.createErrorResponse(propertyId,
                                CarValue.STATUS_UNAVAILABLE));
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
    List<Integer> removeListener(OnCarPropertyResponseListener listener) {
        List<Integer> propertyWithOutListener = new ArrayList<>();
        synchronized (mLock) {
            List<Integer> propertyIds = mListenerToPropertyIds.get(listener);
            mListenerToPropertyIds.remove(listener);
            if (propertyIds == null) {
                throw new IllegalStateException("Listener is not registered yet");
            }
            for (int propertyId : propertyIds) {
                Set<OnCarPropertyResponseListener> listenerSet =
                        mPropertyIdToListeners.get(propertyId);
                listenerSet.remove(listener);
                if (listenerSet.isEmpty()) {
                    propertyWithOutListener.add(propertyId);
                    mPropertyIdToListeners.remove(propertyId);
                    mPropertyIdToResponse.remove(propertyId);
                }
            }
        }
        return propertyWithOutListener;
    }

    /** Returns {@code true} if the value in cache is updated. */
    boolean updateResponseIfNeeded(CarPropertyValue<?> propertyValue) {
        synchronized (mLock) {
            int propertyId = propertyValue.getPropertyId();
            CarPropertyResponse<?> responseInCache = mPropertyIdToResponse.get(propertyId);
            if (responseInCache == null) {
                // the property is unregistered
                return false;
            }
            long timestampMs = TimeUnit.MILLISECONDS.convert(propertyValue.getTimestamp(),
                    TimeUnit.NANOSECONDS);
            // CarService can not guarantee the order of events.
            if (responseInCache.getTimestampMillis() <= timestampMs) {
                // In V1.1, all properties are global properties.
                CarPropertyResponse<?> response =
                        CarPropertyResponse.createFromPropertyValue(propertyValue);
                mPropertyIdToResponse.put(propertyId, response);
                return true;
            }
            return false;
        }
    }

    /** Updates the error event in cache */
    void updateInternalError(CarInternalError internalError) {
        CarPropertyResponse<?> response = CarPropertyResponse.createErrorResponse(
                internalError.getPropertyId(), internalError.getErrorCode());
        synchronized (mLock) {
            mPropertyIdToResponse.put(internalError.getPropertyId(), response);
        }
    }
}
