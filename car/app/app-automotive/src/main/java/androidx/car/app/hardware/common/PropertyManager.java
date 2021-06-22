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
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
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
    private final PropertyRequestProcessor mPropertyRequestProcessor;

    public PropertyManager(@NonNull Context context) {
        mContext = context;
        mPropertyRequestProcessor = new PropertyRequestProcessor(context);
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

        Set<String> requiredPermission = PropertyUtils.getReadPermissions(rawRequests);
        for (String permission : requiredPermission) {
            if (mContext.checkCallingOrSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Missed permission: " + permission);
            }
        }
        List<Pair<Integer, Integer>> requests = parseRawRequest(rawRequests);
        return CallbackToFutureAdapter.getFuture(completer -> {
            // Getting properties' value is expensive operation.
            executor.execute(() ->
                    mPropertyRequestProcessor.fetchCarPropertyValues(requests, (values, errors) ->
                                    completer.set(createResponses(values, errors))));
            return "Get property values done";
        });
    }

    private static List<CarPropertyResponse<?>> createResponses(
            List<CarPropertyValue<?>> propertyValues, List<CarInternalError> propertyErrors) {
        // TODO(b/190869722): handle AreaId to VehicleZone map in V1.2
        List<CarPropertyResponse<?>> carResponses = new ArrayList<>();
        for (CarPropertyValue<?> value : propertyValues) {
            int statusCode = getStatusCodeForCarValue(value.getStatus());
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

    private static @CarValue.StatusCode int getStatusCodeForCarValue(int carPropertyStatus) {
        switch (carPropertyStatus) {
            case CarPropertyValue.STATUS_AVAILABLE:
                return CarValue.STATUS_SUCCESS;
            case CarPropertyValue.STATUS_ERROR:
                return CarValue.STATUS_UNKNOWN;
            case CarPropertyValue.STATUS_UNAVAILABLE:
                return CarValue.STATUS_UNAVAILABLE;
            default:
                throw new IllegalArgumentException("Invalid car property status: "
                        + carPropertyStatus);
        }
    }
}
