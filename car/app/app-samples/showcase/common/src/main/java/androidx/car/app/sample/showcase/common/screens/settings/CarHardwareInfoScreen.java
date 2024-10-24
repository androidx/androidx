/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.settings;

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.ExteriorDimensions;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Creates a screen that show the static information (such as model and energy profile) available
 * via CarHardware interfaces.
 */
public final class CarHardwareInfoScreen extends Screen {
    private static final String TAG = "showcase";

    // Package private for inner class reference
    boolean mHasModelPermission;
    boolean mHasEnergyProfilePermission;
    boolean mHasExteriorDimensionsPermission;
    final Executor mCarHardwareExecutor;

    /**
     * Value fetched from CarHardwareManager containing model information.
     *
     * <p>It is requested asynchronously and can be {@code null} until the response is
     * received.
     */
    @GuardedBy("this")
    @Nullable Model mModel;

    /**
     * Value fetched from CarHardwareManager containing what type of fuel/ports the car has.
     *
     * <p>It is requested asynchronously and can be {@code null} until the response is
     * received.
     */
    @GuardedBy("this")
    @Nullable EnergyProfile mEnergyProfile;

    @GuardedBy("this")
    @Nullable ExteriorDimensions mExteriorDimensions;

    OnCarDataAvailableListener<Model> mModelListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received model information: " + data);
            mModel = data;
            invalidate();
        }
    };

    OnCarDataAvailableListener<EnergyProfile> mEnergyProfileListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received energy profile information: " + data);
            mEnergyProfile = data;
            invalidate();
        }
    };

    OnCarDataAvailableListener<ExteriorDimensions> mExteriorDimensionsListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received exterior dimensions: " + data);
            mExteriorDimensions = data;
            invalidate();
        }
    };

    public CarHardwareInfoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mCarHardwareExecutor = ContextCompat.getMainExecutor(getCarContext());
        Lifecycle lifecycle = getLifecycle();
        lifecycle.addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                CarHardwareManager carHardwareManager =
                        getCarContext().getCarService(CarHardwareManager.class);
                CarInfo carInfo = carHardwareManager.getCarInfo();

                // Request any single shot values.
                synchronized (CarHardwareInfoScreen.this) {
                    mModel = null;

                    try {
                        carInfo.fetchModel(mCarHardwareExecutor, mModelListener);
                        mHasModelPermission = true;
                    } catch (SecurityException e) {
                        mHasModelPermission = false;
                    }

                    mEnergyProfile = null;
                    try {
                        carInfo.fetchEnergyProfile(mCarHardwareExecutor, mEnergyProfileListener);
                        mHasEnergyProfilePermission = true;
                    } catch (SecurityException e) {
                        mHasEnergyProfilePermission = false;
                    }

                    mExteriorDimensions = null;
                    try {
                        carInfo.fetchExteriorDimensions(mCarHardwareExecutor,
                                mExteriorDimensionsListener);
                        mHasExteriorDimensionsPermission = true;
                    } catch (SecurityException e) {
                        mHasExteriorDimensionsPermission = false;
                    }
                }
            }

        });
    }

    @Override
    public @NonNull Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();
        if (allInfoAvailable()) {
            Row.Builder modelRowBuilder = new Row.Builder()
                    .setTitle(getCarContext().getString(R.string.model_info));
            if (!mHasModelPermission) {
                modelRowBuilder.addText(getCarContext().getString(R.string.no_model_permission));
            } else {
                StringBuilder info = new StringBuilder();
                synchronized (CarHardwareInfoScreen.this) {
                    if (mModel.getManufacturer().getStatus() != CarValue.STATUS_SUCCESS) {
                        info.append(getCarContext().getString(R.string.manufacturer_unavailable));
                        info.append(", ");
                    } else {
                        info.append(mModel.getManufacturer().getValue());
                        info.append(", ");
                    }
                    if (mModel.getName().getStatus() != CarValue.STATUS_SUCCESS) {
                        info.append(getCarContext().getString(R.string.model_unavailable));
                        info.append(", ");
                    } else {
                        info.append(mModel.getName().getValue());
                        info.append(", ");
                    }
                    if (mModel.getYear().getStatus() != CarValue.STATUS_SUCCESS) {
                        info.append(getCarContext().getString(R.string.year_unavailable));
                    } else {
                        info.append(mModel.getYear().getValue());
                    }
                }
                modelRowBuilder.addText(info);
            }
            paneBuilder.addRow(modelRowBuilder.build());

            Row.Builder energyProfileRowBuilder = new Row.Builder()
                    .setTitle(getCarContext().getString(R.string.energy_profile));
            if (!mHasEnergyProfilePermission) {
                energyProfileRowBuilder.addText(getCarContext()
                        .getString(R.string.no_energy_profile_permission));
            } else {
                StringBuilder fuelInfo = new StringBuilder();

                synchronized (this) {
                    if (mEnergyProfile.getFuelTypes().getStatus() != CarValue.STATUS_SUCCESS) {
                        fuelInfo.append(getCarContext().getString(R.string.fuel_types));
                        fuelInfo.append(": ");
                        fuelInfo.append(getCarContext().getString(R.string.unavailable));
                    } else {
                        fuelInfo.append(getCarContext().getString(R.string.fuel_types));
                        fuelInfo.append(": ");
                        for (int fuelType : mEnergyProfile.getFuelTypes().getValue()) {
                            fuelInfo.append(fuelTypeAsString(fuelType));
                            fuelInfo.append(" ");
                        }
                    }
                    energyProfileRowBuilder.addText(fuelInfo);
                    StringBuilder evInfo = new StringBuilder();
                    if (mEnergyProfile.getEvConnectorTypes().getStatus()
                            != CarValue.STATUS_SUCCESS) {
                        evInfo.append(" ");
                        evInfo.append(getCarContext().getString(R.string.ev_connector_types));
                        evInfo.append(": ");
                        evInfo.append(getCarContext().getString(R.string.unavailable));
                    } else {
                        evInfo.append(getCarContext().getString(R.string.ev_connector_types));
                        evInfo.append(": ");
                        for (int connectorType : mEnergyProfile.getEvConnectorTypes().getValue()) {
                            evInfo.append(evConnectorAsString(connectorType));
                            evInfo.append(" ");
                        }
                    }
                    energyProfileRowBuilder.addText(evInfo);
                }
            }
            paneBuilder.addRow(energyProfileRowBuilder.build());

            synchronized (this) {
                paneBuilder.addRow(buildExteriorDimensionsRow(mExteriorDimensions,
                        mHasExteriorDimensionsPermission));
            }
        } else {
            paneBuilder.setLoading(true);
        }
        return new PaneTemplate.Builder(paneBuilder.build())
                .setHeader(new Header.Builder()
                        .setStartHeaderAction(Action.BACK)
                        .setTitle(getCarContext().getString(R.string.car_hardware_info))
                        .build())
                .build();
    }

    private boolean allInfoAvailable() {
        synchronized (this) {
            if (mHasModelPermission && mModel == null) {
                return false;
            }
            if (mHasEnergyProfilePermission && mEnergyProfile == null) {
                return false;
            }
            if (mHasExteriorDimensionsPermission && mExteriorDimensions == null) {
                return false;
            }
        }
        return true;
    }

    private static String fuelTypeAsString(int fuelType) {
        switch (fuelType) {
            case EnergyProfile.FUEL_TYPE_UNLEADED:
                return "UNLEADED";
            case EnergyProfile.FUEL_TYPE_LEADED:
                return "LEADED";
            case EnergyProfile.FUEL_TYPE_DIESEL_1:
                return "DIESEL_1";
            case EnergyProfile.FUEL_TYPE_DIESEL_2:
                return "DIESEL_2";
            case EnergyProfile.FUEL_TYPE_BIODIESEL:
                return "BIODIESEL";
            case EnergyProfile.FUEL_TYPE_E85:
                return "E85";
            case EnergyProfile.FUEL_TYPE_LPG:
                return "LPG";
            case EnergyProfile.FUEL_TYPE_CNG:
                return "CNG";
            case EnergyProfile.FUEL_TYPE_LNG:
                return "LNG";
            case EnergyProfile.FUEL_TYPE_ELECTRIC:
                return "ELECTRIC";
            case EnergyProfile.FUEL_TYPE_HYDROGEN:
                return "HYDROGEN";
            case EnergyProfile.FUEL_TYPE_OTHER:
                return "OTHER";
            case EnergyProfile.FUEL_TYPE_UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    private static String evConnectorAsString(int evConnectorType) {
        switch (evConnectorType) {
            case EnergyProfile.EVCONNECTOR_TYPE_J1772:
                return "J1772";
            case EnergyProfile.EVCONNECTOR_TYPE_MENNEKES:
                return "MENNEKES";
            case EnergyProfile.EVCONNECTOR_TYPE_CHADEMO:
                return "CHADEMO";
            case EnergyProfile.EVCONNECTOR_TYPE_COMBO_1:
                return "COMBO_1";
            case EnergyProfile.EVCONNECTOR_TYPE_COMBO_2:
                return "COMBO_2";
            case EnergyProfile.EVCONNECTOR_TYPE_TESLA_ROADSTER:
                return "TESLA_ROADSTER";
            case EnergyProfile.EVCONNECTOR_TYPE_TESLA_HPWC:
                return "TESLA_HPWC";
            case EnergyProfile.EVCONNECTOR_TYPE_TESLA_SUPERCHARGER:
                return "TESLA_SUPERCHARGER";
            case EnergyProfile.EVCONNECTOR_TYPE_GBT:
                return "GBT";
            case EnergyProfile.EVCONNECTOR_TYPE_GBT_DC:
                return "GBT_DC";
            case EnergyProfile.EVCONNECTOR_TYPE_SCAME:
                return "SCAME";
            case EnergyProfile.EVCONNECTOR_TYPE_OTHER:
                return "OTHER";
            case EnergyProfile.EVCONNECTOR_TYPE_UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    private static Row buildExteriorDimensionsRow(
            @Nullable ExteriorDimensions exteriorDimensions, boolean hasPermissions) {
        Row.Builder builder = new Row.Builder().setTitle("Exterior dimensions");
        if (!hasPermissions) {
            builder.addText("Permissions not granted. This vehicle property requires CAR_INFO");
            return builder.build();
        }

        if (exteriorDimensions == null) {
            builder.addText("Pending callback from vehicle fetch request");
            return builder.build();
        }

        CarValue<Integer[]> carValue = exteriorDimensions.getExteriorDimensions();
        if (carValue.getStatus() != CarValue.STATUS_SUCCESS) {
            builder.addText("Fetch failed because the vehicle hasn't implemented this field");
            return builder.build();
        }

        Integer[] dimensionsArray = carValue.getValue();
        if (dimensionsArray == null || dimensionsArray.length != 8) {
            builder.addText("Fetch succeeded, but the reply was not an int array of length 8: "
                    + Arrays.toString(dimensionsArray));
            return builder.build();
        }

        builder.addText("Height: " + dimensionsArray[ExteriorDimensions.HEIGHT_INDEX]
                + ", Length: " + dimensionsArray[ExteriorDimensions.LENGTH_INDEX]
                + ", Width: " + dimensionsArray[ExteriorDimensions.WIDTH_INDEX]
                + ", Width + mirrors: "
                + dimensionsArray[ExteriorDimensions.WIDTH_INCLUDING_MIRRORS_INDEX]);
        builder.addText("Wheel base: " + dimensionsArray[ExteriorDimensions.WHEEL_BASE_INDEX]
                + ", Front width: " + dimensionsArray[ExteriorDimensions.TRACK_WIDTH_FRONT_INDEX]
                + ", Rear width: " + dimensionsArray[ExteriorDimensions.TRACK_WIDTH_REAR_INDEX]
                + ", Turning radius: "
                + dimensionsArray[ExteriorDimensions.CURB_TO_CURB_TURNING_RADIUS_INDEX]);
        return builder.build();
    }
}
