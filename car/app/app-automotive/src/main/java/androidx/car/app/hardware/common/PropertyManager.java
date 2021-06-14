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

import android.car.VehicleAreaType;
import android.car.hardware.CarPropertyValue;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.utils.LogTags;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the communication between the apps and backend Android platform car service.
 *
 * <p>It takes requests from {@link androidx.car.app.hardware.info.AutomotiveCarInfo}, handles the
 * conversion between vehicle zone and areaId, checks preconditions. After that, it uses
 * {@link PropertyRequestProcessor} to complete the request.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class PropertyManager {
    private final Context mContext;
    final PropertyRequestProcessor mPropertyRequestProcessor;
    final Object mLock = new Object();

    // The callback is used by the CarPropertyManager to update property values in the
    // PropertyResponseCache
    final PropertyProcessorCallback mPropertyProcessorCallback = new PropertyProcessorCallback();

    /*
     * The cache contains listeners and properties that registered by listeners. It shares the same
     * lock with the map for active listeners. Needs to update the cache and the actively
     * listener map together.
     */
    @GuardedBy("mLock")
    final PropertyResponseCache mListenerAndResponseCache = new PropertyResponseCache();

    /*
     * Contains registered listeners and the interval time for sampling data to listeners. It
     * shares the same lock with the property value cache. Needs to update the cache and the
     * actively listener map together.
     */
    @GuardedBy("mLock")
    final Map<OnCarPropertyResponseListener, Long> mListenerToSamplingIntervalMap = new HashMap<>();

    // Executor has two threads for dispatching response and unregister properties.
    final ScheduledExecutorService mScheduledExecutorService =
            Executors.newScheduledThreadPool(/* corePoolSize= */2);

    public PropertyManager(@NonNull Context context) {
        mContext = context;
        mPropertyRequestProcessor = new PropertyRequestProcessor(context,
                mPropertyProcessorCallback);
    }

    /**
     * Submits a request for registering the listener to get property updates.
     *
     * @param propertyIds           a list of property id in {@link android.car.VehiclePropertyIds}
     * @param sampleRate            sample rate in Hz
     * @param listener              {@link OnCarPropertyResponseListener}
     * @param executor              execute the task for registering properties
     * @throws SecurityException    if the application did not grant permissions for
     *                              registering properties
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public void submitRegisterListenerRequest(@NonNull List<Integer> propertyIds, float sampleRate,
            @NonNull OnCarPropertyResponseListener listener, @NonNull Executor executor) {
        checkPermissions(propertyIds);
        long samplingIntervalMs;
        synchronized (mLock) {
            mListenerAndResponseCache.putListenerAndPropertyIds(listener, propertyIds);
            if (sampleRate == 0) {
                throw new IllegalArgumentException("Sample rate cannot be zero.");
            }
            samplingIntervalMs = (long) (1000 / sampleRate);
            mListenerToSamplingIntervalMap.put(listener, samplingIntervalMs);
        }

        // register properties
        executor.execute(() -> {
            for (int propertyId : propertyIds) {
                try {
                    mPropertyRequestProcessor.registerProperty(propertyId, sampleRate);
                } catch (IllegalArgumentException e) {
                    // the property is not implemented
                    Log.e(LogTags.TAG_CAR_HARDWARE,
                            "Failed to register for property: " + propertyId, e);
                    mPropertyProcessorCallback.onErrorEvent(CarInternalError.create(propertyId,
                            VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL,
                            CarValue.STATUS_UNIMPLEMENTED));
                } catch (Exception e) {
                    Log.e(LogTags.TAG_CAR_HARDWARE,
                            "Failed to register for property: " + propertyId, e);
                    mPropertyProcessorCallback.onErrorEvent(CarInternalError.create(propertyId,
                            VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL, CarValue.STATUS_UNAVAILABLE));
                }
            }
        });
        mScheduledExecutorService.schedule(()-> dispatchResponseWithDelay(listener),
                samplingIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Submits a request for unregistering the listener to get property updates.
     *
     * @param listener                  {@link OnCarPropertyResponseListener} to be unregistered.
     * @throws IllegalStateException    if the listener was not registered yet
     * @throws SecurityException        if the application did not grant permissions for
     *                                  unregistering properties
     */
    public void submitUnregisterListenerRequest(@NonNull OnCarPropertyResponseListener listener) {
        List<Integer> propertyIds;
        List<Integer> propertyIdsToBeUnregistered;
        synchronized (mLock) {
            propertyIds = mListenerAndResponseCache.getPropertyIdsByListener(listener);
            if (propertyIds == null) {
                throw new IllegalStateException("Listener was not registered yet.");
            }
            propertyIdsToBeUnregistered = mListenerAndResponseCache.removeListener(listener);
            mListenerToSamplingIntervalMap.remove(listener);
        }
        if (propertyIdsToBeUnregistered.size() != 0) {
            mScheduledExecutorService.execute(() -> {
                for (int propertyId : propertyIdsToBeUnregistered) {
                    mPropertyRequestProcessor.unregisterProperty(propertyId);
                }
            });
        }
    }

    /**
     * Submits {@link CarPropertyResponse} for getting property values.
     *
     * @param rawRequests           a list of {@link GetPropertyRequest}
     * @param executor              executes the expensive operation such as fetching property
     *                              values from cars
     * @throws SecurityException    if the application did not grant permissions for getting
     *                              property
     *
     * @return {@link ListenableFuture} contains a list of {@link CarPropertyResponse}
     */
    @NonNull
    public ListenableFuture<List<CarPropertyResponse<?>>> submitGetPropertyRequest(
            @NonNull List<GetPropertyRequest> rawRequests, @NonNull Executor executor) {
        List<Integer> propertyIds = new ArrayList<>();
        for (GetPropertyRequest request : rawRequests) {
            propertyIds.add(request.getPropertyId());
        }
        checkPermissions(propertyIds);
        List<Pair<Integer, Integer>> requests = parseRawRequest(rawRequests);
        return CallbackToFutureAdapter.getFuture(completer -> {
            // Getting properties' value is expensive operation.
            executor.execute(() ->
                    mPropertyRequestProcessor.fetchCarPropertyValues(requests, (values, errors) ->
                                    completer.set(createResponses(values, errors))));
            return "Get property values done";
        });
    }

    /**
     * Dispatches a list of {@link CarPropertyResponse} without delay.
     *
     * <p>For on_change properties and error events, dispatches them to listeners without delay.
     *
     * @param propertyId property id in {@link android.car.VehiclePropertyIds}
     */
    void dispatchResponsesWithoutDelay(int propertyId) {
        synchronized (mLock) {
            Set<OnCarPropertyResponseListener> listeners =
                    mListenerAndResponseCache.getListenersByPropertyId(propertyId);
            if (listeners == null) {
                return;
            }
            for (OnCarPropertyResponseListener listener : listeners) {
                // build the group of property
                List<CarPropertyResponse<?>> propertyResponses =
                        mListenerAndResponseCache.getResponsesByListener(listener);
                if (propertyResponses != null) {
                    listener.onCarPropertyResponses(propertyResponses);
                }
            }
        }
    }

    /**
     * Dispatches {@link CarPropertyResponse} to the listener in {@link DelayQueue}.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    void dispatchResponseWithDelay(OnCarPropertyResponseListener listener) {
        List<CarPropertyResponse<?>> propertyResponses = null;
        Long delayTime;
        synchronized (mLock) {
            delayTime = mListenerToSamplingIntervalMap.get(listener);
            if (delayTime != null) {
                propertyResponses = mListenerAndResponseCache.getResponsesByListener(listener);

                //Schedules for next dispatch
                mScheduledExecutorService.schedule(()-> dispatchResponseWithDelay(listener),
                        delayTime, TimeUnit.MILLISECONDS);
            }
        }
        if (propertyResponses != null) {
            listener.onCarPropertyResponses(propertyResponses);
        }
    }

    /**
     * Registers all properties in the car service.
     *
     * <p>The callback updates the value in cache. For on_change and error events, the callback
     * trigger dispatching task without delay.
     */
    class PropertyProcessorCallback extends PropertyRequestProcessor.PropertyEventCallback {
        @Override
        public void onChangeEvent(CarPropertyValue carPropertyValue) {
            synchronized (mLock) {
                // check timestamp
                if (mListenerAndResponseCache.updateResponseIfNeeded(carPropertyValue)) {
                    int propertyId = carPropertyValue.getPropertyId();
                    if (PropertyUtils.isOnChangeProperty(propertyId)) {
                        mScheduledExecutorService.execute(() ->
                                dispatchResponsesWithoutDelay(propertyId));
                    }
                }
            }
        }

        @Override
        public void onErrorEvent(CarInternalError carInternalError) {
            synchronized (mLock) {
                mListenerAndResponseCache.updateInternalError(carInternalError);
                mScheduledExecutorService.execute(() ->
                        dispatchResponsesWithoutDelay(carInternalError.getPropertyId()));
            }
        }
    }

    private static List<CarPropertyResponse<?>> createResponses(
            List<CarPropertyValue<?>> propertyValues, List<CarInternalError> propertyErrors) {
        // TODO(b/190869722): handle AreaId to VehicleZone map in V1.2
        List<CarPropertyResponse<?>> carResponses = new ArrayList<>();
        for (CarPropertyValue<?> value : propertyValues) {
            int statusCode = PropertyUtils.mapToStatusCodeInCarValue(value.getStatus());
            long timeInMillis = TimeUnit.MILLISECONDS.convert(value.getTimestamp(),
                    TimeUnit.NANOSECONDS);
            carResponses.add(CarPropertyResponse.create(
                    value.getPropertyId(), statusCode, timeInMillis, value.getValue()));
        }
        for (CarInternalError error: propertyErrors) {
            carResponses.add(CarPropertyResponse.createErrorResponse(error.getPropertyId(),
                    error.getErrorCode()));
        }
        return carResponses;
    }

    // Maps VehicleZones to AreaIds.
    private List<Pair<Integer, Integer>> parseRawRequest(List<GetPropertyRequest> requestList) {
        List<Pair<Integer, Integer>> requestsWithAreaId = new ArrayList<>(requestList.size());
        for (GetPropertyRequest request : requestList) {
            if (PropertyUtils.isGlobalProperty(request.getPropertyId())) {
                // ignore the VehicleZone, set areaId to 0.
                requestsWithAreaId.add(new Pair<>(request.getPropertyId(), 0));
            }
        }
        return requestsWithAreaId;
    }

    private void checkPermissions(List<Integer> propertyIds) {
        Set<String> requiredPermission = PropertyUtils.getReadPermissionsByPropertyIds(propertyIds);
        for (String permission : requiredPermission) {
            if (mContext.checkCallingOrSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Missed permission: " + permission);
            }
        }
    }
}
