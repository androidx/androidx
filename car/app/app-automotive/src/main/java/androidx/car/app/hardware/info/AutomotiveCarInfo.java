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
package androidx.car.app.hardware.info;

import static android.car.VehiclePropertyIds.DISTANCE_DISPLAY_UNITS;
import static android.car.VehiclePropertyIds.EV_BATTERY_LEVEL;
import static android.car.VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED;
import static android.car.VehiclePropertyIds.EV_CHARGE_PORT_OPEN;
import static android.car.VehiclePropertyIds.FUEL_LEVEL;
import static android.car.VehiclePropertyIds.FUEL_LEVEL_LOW;
import static android.car.VehiclePropertyIds.FUEL_VOLUME_DISPLAY_UNITS;
import static android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_EV_CONNECTOR_TYPE;
import static android.car.VehiclePropertyIds.INFO_FUEL_CAPACITY;
import static android.car.VehiclePropertyIds.INFO_FUEL_TYPE;
import static android.car.VehiclePropertyIds.INFO_MAKE;
import static android.car.VehiclePropertyIds.INFO_MODEL;
import static android.car.VehiclePropertyIds.INFO_MODEL_YEAR;
import static android.car.VehiclePropertyIds.PERF_ODOMETER;
import static android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED;
import static android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY;
import static android.car.VehiclePropertyIds.RANGE_REMAINING;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.hardware.common.CarValue.STATUS_UNKNOWN;
import static androidx.car.app.hardware.common.CarValueUtils.getCarValue;

import static java.util.Objects.requireNonNull;

import android.os.Build;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarPropertyResponse;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.CarZone;
import androidx.car.app.hardware.common.GetPropertyRequest;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.common.OnCarPropertyResponseListener;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.hardware.common.PropertyUtils;
import androidx.car.app.utils.LogTags;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Manages access to vehicle specific info, for example, energy info, model info.
 */
@RestrictTo(LIBRARY)
public class AutomotiveCarInfo implements CarInfo {
    static final List<CarZone> GLOBAL_CAR_ZONE = Arrays.asList(getGlobalCarZone());

    @VisibleForTesting
    static final float DEFAULT_SAMPLE_RATE = 5f;

    @OptIn(markerClass = ExperimentalCarApi.class)
    private static CarZone getGlobalCarZone() {
        return CarZone.CAR_ZONE_GLOBAL;
    }

    /*
     * ELECTRONIC_TOLL_COLLECTION_CARD_STATUS in VehiclePropertyIds. The property is added after
     * Android Q.
     */
    public static final int TOLL_CARD_STATUS_ID = 289410874;

    // VEHICLE_SPEED_DISPLAY_UNIT in VehiclePropertyIds. The property is added after Android Q.
    public static final int SPEED_DISPLAY_UNIT_ID = 289408516;

    // INFO_EXTERIOR_DIMENSIONS in VehiclePropertyIds added in API 30
    // https://developer.android.com/reference/android/car/VehiclePropertyIds#INFO_EXTERIOR_DIMENSIONS
    public static final int INFO_EXTERIOR_DIMENSIONS_ID = 289472779;

    static final ImmutableMap<Integer, List<CarZone>> ENERGY_LEVEL_REQUEST = ImmutableMap.<Integer,
                    List<CarZone>>builder()
            .put(EV_BATTERY_LEVEL, GLOBAL_CAR_ZONE)
            .put(FUEL_LEVEL, GLOBAL_CAR_ZONE)
            .put(FUEL_LEVEL_LOW, GLOBAL_CAR_ZONE)
            .put(RANGE_REMAINING, GLOBAL_CAR_ZONE)
            .put(DISTANCE_DISPLAY_UNITS, GLOBAL_CAR_ZONE)
            .put(FUEL_VOLUME_DISPLAY_UNITS, GLOBAL_CAR_ZONE)
            .buildKeepingLast();
    private static final ImmutableMap<Integer, List<CarZone>> MILEAGE_REQUEST =
            ImmutableMap.<Integer,
                            List<CarZone>>builder()
                    .put(PERF_ODOMETER, GLOBAL_CAR_ZONE)
                    .put(DISTANCE_DISPLAY_UNITS, GLOBAL_CAR_ZONE)
                    .buildKeepingLast();
    static final ImmutableMap<Integer, List<CarZone>> TOLL_REQUEST = ImmutableMap.<Integer,
                    List<CarZone>>builder()
            .put(TOLL_CARD_STATUS_ID, GLOBAL_CAR_ZONE)
            .buildKeepingLast();
    private static final ImmutableMap<Integer, List<CarZone>> SPEED_REQUEST = ImmutableMap.<Integer,
                    List<CarZone>>builder()
            .put(PERF_VEHICLE_SPEED, GLOBAL_CAR_ZONE)
            .put(PERF_VEHICLE_SPEED_DISPLAY, GLOBAL_CAR_ZONE)
            .put(SPEED_DISPLAY_UNIT_ID, GLOBAL_CAR_ZONE)
            .buildKeepingLast();
    private static final ImmutableMap<Integer, List<CarZone>> EV_STATUS_REQUEST =
            ImmutableMap.<Integer,
                            List<CarZone>>builder()
                    .put(EV_CHARGE_PORT_OPEN, GLOBAL_CAR_ZONE)
                    .put(EV_CHARGE_PORT_CONNECTED, GLOBAL_CAR_ZONE)
                    .buildKeepingLast();
    private final Map<OnCarDataAvailableListener<?>, OnCarPropertyResponseListener> mListenerMap =
            new HashMap<>();
    private final PropertyManager mPropertyManager;

    /**
     * AutomotiveCarInfo class constructor initializing PropertyWorkManager object.
     *
     * @throws NullPointerException if {@code manager} is null
     */
    public AutomotiveCarInfo(@NonNull PropertyManager manager) {
        mPropertyManager = requireNonNull(manager);
    }

    @Override
    public void fetchModel(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Model> listener) {
        // Prepare request GetPropertyRequest
        List<GetPropertyRequest> request = new ArrayList<>();

        // Add "make", "model", "year" of the vehicle to the requests.
        request.add(GetPropertyRequest.create(INFO_MAKE));
        request.add(GetPropertyRequest.create(INFO_MODEL));
        request.add(GetPropertyRequest.create(INFO_MODEL_YEAR));
        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(request, executor);
        populateModelData(executor, listener, future);
    }

    @Override
    public void fetchEnergyProfile(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyProfile> listener) {
        // Prepare request GetPropertyRequest
        List<GetPropertyRequest> request = new ArrayList<>();

        // Add "evConnector" and "fuel" type of the vehicle to the requests.
        request.add(GetPropertyRequest.create(INFO_EV_CONNECTOR_TYPE));
        request.add(GetPropertyRequest.create(INFO_FUEL_TYPE));

        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(request, executor);
        populateEnergyProfileData(executor, listener, future);
    }

    @Override
    public void fetchExteriorDimensions(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<ExteriorDimensions> listener) {
        // Exterior dimensions were only introduced in API 30 and above
        if (Build.VERSION.SDK_INT < 30) {
            executor.execute(() -> listener.onCarDataAvailable(new ExteriorDimensions()));
            return;
        }

        List<GetPropertyRequest> request = new ArrayList<>();
        request.add(GetPropertyRequest.create(INFO_EXTERIOR_DIMENSIONS_ID));

        ListenableFuture<List<CarPropertyResponse<?>>> future =
                mPropertyManager.submitGetPropertyRequest(request, executor);

        Api30Impl.populateExteriorDimensionsData(executor, listener, future);
    }

    @Override
    public void addTollListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<TollCard> listener) {
        if (Build.VERSION.SDK_INT > 30) {
            Api31Impl.addTollListener(executor, listener, mPropertyManager, mListenerMap);
        } else {
            TollCard unimplementedTollCard = new TollCard.Builder()
                    .setCardState(CarValue.UNIMPLEMENTED_INTEGER).build();
            executor.execute(() -> listener.onCarDataAvailable(unimplementedTollCard));
        }
    }

    @Override
    public void removeTollListener(@NonNull OnCarDataAvailableListener<TollCard> listener) {
        OnCarPropertyResponseListener responseListener = mListenerMap.remove(listener);
        if (responseListener == null) {
            return;
        }
        if (Build.VERSION.SDK_INT > 30) {
            Api31Impl.removeTollListener(responseListener, mPropertyManager);
        }
    }

    @Override
    public void addEnergyLevelListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        getCapacitiesThenEnergyLevel(executor, listener);
    }

    @Override
    public void removeEnergyLevelListener(
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        removeListenerImpl(listener);
    }

    @Override
    public void addSpeedListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Speed> listener) {
        SpeedListener speedListener = new SpeedListener(listener, executor);
        mPropertyManager.submitRegisterListenerRequest(SPEED_REQUEST, DEFAULT_SAMPLE_RATE,
                speedListener, executor);
        mListenerMap.put(listener, speedListener);
    }

    @Override
    public void removeSpeedListener(@NonNull OnCarDataAvailableListener<Speed> listener) {
        removeListenerImpl(listener);
    }

    @Override
    public void addMileageListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<Mileage> listener) {
        MileageListener mileageListener = new MileageListener(listener, executor);
        mPropertyManager.submitRegisterListenerRequest(MILEAGE_REQUEST, DEFAULT_SAMPLE_RATE,
                mileageListener, executor);
        mListenerMap.put(listener, mileageListener);
    }

    @Override
    public void removeMileageListener(@NonNull OnCarDataAvailableListener<Mileage> listener) {
        removeListenerImpl(listener);
    }

    // TODO(b/216177515): Remove this annotation once EvStatus is ready.
    @OptIn(markerClass = ExperimentalCarApi.class)
    @Override
    public void addEvStatusListener(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EvStatus> listener) {
        EvStatusListener evStatusListener = new EvStatusListener(listener, executor);
        mPropertyManager.submitRegisterListenerRequest(EV_STATUS_REQUEST, DEFAULT_SAMPLE_RATE,
                evStatusListener, executor);
        mListenerMap.put(listener, evStatusListener);
    }

    // TODO(b/216177515): Remove this annotation once EvStatus is ready.
    @OptIn(markerClass = ExperimentalCarApi.class)
    @Override
    public void removeEvStatusListener(@NonNull OnCarDataAvailableListener<EvStatus> listener) {
        removeListenerImpl(listener);
    }

    void getCapacitiesThenEnergyLevel(@NonNull Executor executor,
            @NonNull OnCarDataAvailableListener<EnergyLevel> listener) {
        // Prepare request GetPropertyRequest for battery and fuel capacities.
        List<GetPropertyRequest> request = new ArrayList<>();

        // Add "evConnector" and "fuel" type of the vehicle to the requests.
        request.add(GetPropertyRequest.create(INFO_EV_BATTERY_CAPACITY));
        request.add(GetPropertyRequest.create(INFO_FUEL_CAPACITY));
        ListenableFuture<List<CarPropertyResponse<?>>> capacityFuture =
                mPropertyManager.submitGetPropertyRequest(request, executor);
        EnergyLevelListener energyLevelListener = new EnergyLevelListener(listener, executor);

        // This future will get EV battery capacity and fuel capacity for calculating the
        // percentage of battery level and fuel level. Without those values, we still can provide
        // fuel_level_low, distance_units, fuel_volume_display_units and range_remaining information
        // in EnergyLevelListener.
        capacityFuture.addListener(() -> {
            List<CarPropertyResponse<?>> carPropertyResponses;
            try {
                carPropertyResponses = capacityFuture.get();
            } catch (ExecutionException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Level", e);
                return;
            } catch (InterruptedException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Level", e);
                Thread.currentThread().interrupt();
                return;
            }
            for (CarPropertyResponse<?> carPropertyResponse : carPropertyResponses) {
                if (carPropertyResponse.getPropertyId() == INFO_EV_BATTERY_CAPACITY) {
                    energyLevelListener.updateEvBatteryCapacityPropertyResponse(
                            carPropertyResponse);
                }
                if (carPropertyResponse.getPropertyId() == INFO_FUEL_CAPACITY) {
                    energyLevelListener.updateFuelCapacityPropertyResponse(carPropertyResponse);
                }
            }
        }, executor);
        mPropertyManager.submitRegisterListenerRequest(ENERGY_LEVEL_REQUEST,
                DEFAULT_SAMPLE_RATE, energyLevelListener, executor);
        mListenerMap.put(listener, energyLevelListener);
    }

    private void populateModelData(@NonNull Executor executor,
            OnCarDataAvailableListener<Model> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            List<CarPropertyResponse<?>> carPropertyResponses;
            try {
                carPropertyResponses = future.get();
            } catch (ExecutionException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Model", e);
                return;
            } catch (InterruptedException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Model", e);
                Thread.currentThread().interrupt();
                return;
            }
            Model.Builder modelBuilder = new Model.Builder();
            for (CarPropertyResponse<?> carPropertyResponse : carPropertyResponses) {
                if (carPropertyResponse.getPropertyId() == INFO_MAKE) {
                    modelBuilder.setManufacturer(getCarValue(carPropertyResponse));
                }
                if (carPropertyResponse.getPropertyId() == INFO_MODEL) {
                    modelBuilder.setName(getCarValue(carPropertyResponse));
                }
                if (carPropertyResponse.getPropertyId() == INFO_MODEL_YEAR) {
                    modelBuilder.setYear(getCarValue(carPropertyResponse));
                }
            }
            listener.onCarDataAvailable(modelBuilder.build());
        }, executor);
    }

    private void populateEnergyProfileData(@NonNull Executor executor,
            OnCarDataAvailableListener<EnergyProfile> listener,
            ListenableFuture<List<CarPropertyResponse<?>>> future) {
        future.addListener(() -> {
            List<CarPropertyResponse<?>> carPropertyResponses;
            try {
                carPropertyResponses = future.get();
            } catch (ExecutionException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Profile", e);
                return;
            } catch (InterruptedException e) {
                Log.e(LogTags.TAG_CAR_HARDWARE,
                        "Failed to get CarPropertyResponse for Energy Profile", e);
                Thread.currentThread().interrupt();
                return;
            }
            EnergyProfile.Builder energyProfileBuilder = new EnergyProfile.Builder();
            for (CarPropertyResponse<?> carPropertyResponse : carPropertyResponses) {
                switch (carPropertyResponse.getPropertyId()) {
                    case INFO_EV_CONNECTOR_TYPE:
                        if (carPropertyResponse.getValue() != null) {
                            Integer[] evConnectorsInVehicle =
                                    (Integer[]) carPropertyResponse.getValue();
                            List<Integer> evConnectorsInCarValue = new ArrayList<>();
                            for (Integer connectorType : evConnectorsInVehicle) {
                                evConnectorsInCarValue.add(
                                        PropertyUtils.convertEvConnectorType(connectorType));
                            }
                            energyProfileBuilder.setEvConnectorTypes(
                                    getCarValue(carPropertyResponse,
                                            evConnectorsInCarValue));
                        } else {
                            energyProfileBuilder.setEvConnectorTypes(
                                    getCarValue(carPropertyResponse));
                        }
                        break;
                    case INFO_FUEL_TYPE:
                        if (carPropertyResponse.getValue() != null) {
                            energyProfileBuilder.setFuelTypes(
                                    getCarValue(carPropertyResponse,
                                            Arrays.stream((Integer[]) requireNonNull(
                                                    carPropertyResponse.getValue())).collect(
                                                    Collectors.toList())));
                        } else {
                            energyProfileBuilder.setFuelTypes(getCarValue(carPropertyResponse));
                        }
                        break;
                    default:
                        Log.e(LogTags.TAG_CAR_HARDWARE,
                                "Invalid response callback in populateEnergyProfileData.");
                        break;
                }
            }
            listener.onCarDataAvailable(energyProfileBuilder.build());
        }, executor);
    }

    @RequiresApi(31)
    private static class Api31Impl {
        static void addTollListener(Executor executor,
                OnCarDataAvailableListener<TollCard> listener, PropertyManager propertyManager,
                Map<OnCarDataAvailableListener<?>, OnCarPropertyResponseListener> listenerMap) {
            TollListener tollListener = new TollListener(listener, executor);
            propertyManager.submitRegisterListenerRequest(TOLL_REQUEST, DEFAULT_SAMPLE_RATE,
                    tollListener, executor);
            listenerMap.put(listener, tollListener);
        }

        static void removeTollListener(OnCarPropertyResponseListener listener,
                PropertyManager propertyManager) {
            propertyManager.submitUnregisterListenerRequest(listener);
        }
    }

    @RequiresApi(30)
    private static class Api30Impl {
        static void populateExteriorDimensionsData(
                @NonNull Executor executor,
                OnCarDataAvailableListener<ExteriorDimensions> listener,
                ListenableFuture<List<CarPropertyResponse<?>>> future) {
            future.addListener(() -> {
                List<CarPropertyResponse<?>> responses;
                try {
                    responses = future.get();
                } catch (ExecutionException e) {
                    Log.e(LogTags.TAG_CAR_HARDWARE,
                            "ExecutionException when fetching exterior dimensions", e);
                    listener.onCarDataAvailable(new ExteriorDimensions());
                    return;
                } catch (InterruptedException e) {
                    Log.e(LogTags.TAG_CAR_HARDWARE,
                            "InterruptedException when fetching exterior dimensions", e);
                    listener.onCarDataAvailable(new ExteriorDimensions());
                    Thread.currentThread().interrupt();
                    return;
                }

                if (responses.isEmpty()) {
                    Log.e(LogTags.TAG_CAR_HARDWARE,
                            "Response empty when fetching exterior dimensions");
                    listener.onCarDataAvailable(new ExteriorDimensions());
                    return;
                }

                CarPropertyResponse<?> response = responses.get(0);
                if (response.getPropertyId() != INFO_EXTERIOR_DIMENSIONS_ID) {
                    Log.e(LogTags.TAG_CAR_HARDWARE, "Invalid response callback in exterior "
                            + "dimensions listener. Expected " + INFO_EXTERIOR_DIMENSIONS_ID + ","
                            + " but got " + response.getPropertyId());

                    listener.onCarDataAvailable(new ExteriorDimensions());
                    return;
                }

                listener.onCarDataAvailable(new ExteriorDimensions(getCarValue(response)));
            }, executor);
        }
    }

    private void removeListenerImpl(OnCarDataAvailableListener<?> listener) {
        OnCarPropertyResponseListener responseListener = mListenerMap.remove(listener);
        if (responseListener != null) {
            mPropertyManager.submitUnregisterListenerRequest(responseListener);
        }
    }

    private static class TollListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<TollCard> mTollOnCarDataListener;
        private final Executor mExecutor;

        TollListener(OnCarDataAvailableListener<TollCard> listener, Executor executor) {
            mTollOnCarDataListener = listener;
            mExecutor = executor;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            mExecutor.execute(() -> {
                mTollOnCarDataListener.onCarDataAvailable(new TollCard.Builder().setCardState(
                        getCarValue(carPropertyResponses.get(0))).build());
            });
        }
    }

    private static class SpeedListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<Speed> mSpeedOnCarDataListener;
        private final Executor mExecutor;

        SpeedListener(OnCarDataAvailableListener<Speed> listener, Executor executor) {
            mSpeedOnCarDataListener = listener;
            mExecutor = executor;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            mExecutor.execute(() -> {
                Speed.Builder speedBuilder = new Speed.Builder();
                for (CarPropertyResponse<?> response : carPropertyResponses) {
                    switch (response.getPropertyId()) {
                        case PERF_VEHICLE_SPEED:
                            speedBuilder.setRawSpeedMetersPerSecond(getCarValue(response));
                            break;
                        case PERF_VEHICLE_SPEED_DISPLAY:
                            speedBuilder.setDisplaySpeedMetersPerSecond(getCarValue(response));
                            break;
                        case SPEED_DISPLAY_UNIT_ID:
                            Integer speedUnitResponse = (Integer) response.getValue();
                            if (speedUnitResponse != null && speedUnitResponse != 0) {
                                speedBuilder.setSpeedDisplayUnit(getCarValue(response,
                                        PropertyUtils.convertSpeedUnit(speedUnitResponse)));
                            } else {
                                speedBuilder.setSpeedDisplayUnit(getCarValue(response));
                            }
                            break;
                        default:
                            Log.e(LogTags.TAG_CAR_HARDWARE,
                                    "Invalid response callback in SpeedListener.");
                    }
                }
                mSpeedOnCarDataListener.onCarDataAvailable(speedBuilder.build());
            });
        }
    }

    /**
     * EvStatus listener to get EV port status updates by {@link CarPropertyResponse}.
     */
    static class EvStatusListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<EvStatus> mEvStatusOnCarDataAvailableListener;
        private final Executor mExecutor;

        EvStatusListener(OnCarDataAvailableListener<EvStatus> listener, Executor executor) {
            mEvStatusOnCarDataAvailableListener = listener;
            mExecutor = executor;
        }

        @Override
        // TODO(b/216177515): Remove this annotation once EvStatus is ready.
        @OptIn(markerClass = ExperimentalCarApi.class)
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            mExecutor.execute(() -> {
                EvStatus.Builder evStatusBuilder = new EvStatus.Builder();
                for (CarPropertyResponse<?> response : carPropertyResponses) {
                    switch (response.getPropertyId()) {
                        case EV_CHARGE_PORT_OPEN:
                            evStatusBuilder.setEvChargePortOpen(getCarValue(response));
                            break;
                        case EV_CHARGE_PORT_CONNECTED:
                            evStatusBuilder.setEvChargePortConnected(getCarValue(response));
                            break;
                        default:
                            Log.e(LogTags.TAG_CAR_HARDWARE,
                                    "Invalid response callback in EvStatusListener");
                    }
                }
                mEvStatusOnCarDataAvailableListener.onCarDataAvailable(evStatusBuilder.build());
            });
        }
    }

    /**
     * Mileage listener to get distance display unit and odometer updates by {@link
     * CarPropertyResponse}.
     */
    static class MileageListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<Mileage> mMileageOnCarDataAvailableListener;
        private final Executor mExecutor;

        MileageListener(OnCarDataAvailableListener<Mileage> listener, Executor executor) {
            mMileageOnCarDataAvailableListener = listener;
            mExecutor = executor;
        }

        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            mExecutor.execute(() -> {
                Mileage.Builder mileageBuilder = new Mileage.Builder();
                for (CarPropertyResponse<?> response : carPropertyResponses) {
                    switch (response.getPropertyId()) {
                        case PERF_ODOMETER:
                            mileageBuilder.setOdometerMeters(getCarValue(response));
                            break;
                        case DISTANCE_DISPLAY_UNITS:
                            if (response.getValue() != null) {
                                mileageBuilder.setDistanceDisplayUnit(getCarValue(response,
                                        PropertyUtils.convertDistanceUnit(
                                                (Integer) response.getValue())));
                            } else {
                                mileageBuilder.setDistanceDisplayUnit(getCarValue(response));
                            }
                            break;
                        default:
                            Log.e(LogTags.TAG_CAR_HARDWARE,
                                    "Invalid response callback in MileageListener");
                    }
                }
                mMileageOnCarDataAvailableListener.onCarDataAvailable(mileageBuilder.build());
            });
        }
    }

    /**
     * EnergyLevel listener to get battery, energy updates by {@link CarPropertyResponse}.
     */
    static class EnergyLevelListener implements OnCarPropertyResponseListener {
        private final OnCarDataAvailableListener<EnergyLevel>
                mEnergyLevelOnCarDataAvailableListener;
        private final Executor mExecutor;
        private CarPropertyResponse<?> mEvBatteryCapacityPropertyResponse = CarPropertyResponse
                .builder().setPropertyId(INFO_EV_BATTERY_CAPACITY).setStatus(STATUS_UNKNOWN)
                .build();
        private CarPropertyResponse<?> mFuelCapacityPropertyResponse = CarPropertyResponse.builder()
                .setPropertyId(INFO_FUEL_CAPACITY).setStatus(STATUS_UNKNOWN).build();

        EnergyLevelListener(OnCarDataAvailableListener<EnergyLevel> listener, Executor executor) {
            mEnergyLevelOnCarDataAvailableListener = listener;
            mExecutor = executor;
        }

        void updateEvBatteryCapacityPropertyResponse(
                CarPropertyResponse<?> evBatteryCapacityPropertyResponse) {
            mEvBatteryCapacityPropertyResponse = evBatteryCapacityPropertyResponse;
        }

        void updateFuelCapacityPropertyResponse(
                CarPropertyResponse<?> fuelCapacityPropertyResponse) {
            mFuelCapacityPropertyResponse = fuelCapacityPropertyResponse;
        }

        // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
        @OptIn(markerClass = ExperimentalCarApi.class)
        @Override
        public void onCarPropertyResponses(
                @NonNull List<CarPropertyResponse<?>> carPropertyResponses) {
            mExecutor.execute(() -> {
                EnergyLevel.Builder energyLevelBuilder = new EnergyLevel.Builder();
                for (CarPropertyResponse<?> response : carPropertyResponses) {
                    switch (response.getPropertyId()) {
                        case EV_BATTERY_LEVEL:
                            if (response.getValue() == null) {
                                energyLevelBuilder.setBatteryPercent(getCarValue(response));
                            } else if (mEvBatteryCapacityPropertyResponse.getValue() == null) {
                                energyLevelBuilder.setBatteryPercent(
                                        getCarValue(mEvBatteryCapacityPropertyResponse));
                            } else {
                                energyLevelBuilder.setBatteryPercent(getCarValue(response,
                                        (Float) response.getValue() / (Float)
                                        mEvBatteryCapacityPropertyResponse.getValue() * 100));
                            }
                            break;
                        case FUEL_LEVEL:
                            if (response.getValue() == null) {
                                energyLevelBuilder.setFuelPercent(getCarValue(response));
                            } else if (mFuelCapacityPropertyResponse.getValue() == null) {
                                energyLevelBuilder.setFuelPercent(
                                        getCarValue(mFuelCapacityPropertyResponse));
                            } else {
                                energyLevelBuilder.setFuelPercent(getCarValue(response,
                                        (Float) response.getValue() / (Float)
                                        mFuelCapacityPropertyResponse.getValue() * 100));
                            }
                            break;
                        case FUEL_LEVEL_LOW:
                            energyLevelBuilder.setEnergyIsLow(getCarValue(response));
                            break;
                        case RANGE_REMAINING:
                            energyLevelBuilder.setRangeRemainingMeters(getCarValue(response));
                            break;
                        case DISTANCE_DISPLAY_UNITS:
                            if (response.getValue() != null) {
                                energyLevelBuilder.setDistanceDisplayUnit(getCarValue(response,
                                        PropertyUtils.convertDistanceUnit(
                                                (Integer) response.getValue())));
                            } else {
                                energyLevelBuilder.setDistanceDisplayUnit(getCarValue(response));
                            }
                            break;
                        case FUEL_VOLUME_DISPLAY_UNITS:
                            if (response.getValue() != null) {
                                energyLevelBuilder.setFuelVolumeDisplayUnit(getCarValue(response,
                                        PropertyUtils.convertVolumeUnit(
                                                (Integer) response.getValue())));
                            } else {
                                energyLevelBuilder.setFuelVolumeDisplayUnit(getCarValue(response));
                            }
                            break;
                        default:
                            Log.e(LogTags.TAG_CAR_HARDWARE,
                                    "Invalid response callback in EnergyLevelListener");
                    }
                }
                mEnergyLevelOnCarDataAvailableListener.onCarDataAvailable(
                        energyLevelBuilder.build());
            });
        }
    }
}
