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

package androidx.car.app.sample.showcase.common.misc;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.Executor;

/**
 * Creates a screen that show the static information (such as model and energy profile) available
 * via CarHardware interfaces.
 */
public final class CarHardwareInfoScreen extends Screen {
    private static final String TAG = "showcase";

    boolean mHasModelPermission;
    boolean mHasEnergyProfilePermission;
    final Executor mCarHardwareExecutor;

    /**
     * Value fetched from CarHardwareManager containing model information.
     *
     * <p>It is requested asynchronously and can be {@code null} until the response is
     * received.
     */
    @Nullable
    Model mModel;

    /**
     * Value fetched from CarHardwareManager containing what type of fuel/ports the car has.
     *
     * <p>It is requested asynchronously and can be {@code null} until the response is
     * received.
     */
    @Nullable
    EnergyProfile mEnergyProfile;

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
                mModel = null;
                try {
                    carInfo.fetchModel(mCarHardwareExecutor, mModelListener);
                    mHasModelPermission = true;
                } catch (SecurityException e) {
                    mHasModelPermission = false;
                }

                mEnergyProfile = null;
                try {
                    carInfo.fetchModel(mCarHardwareExecutor, mModelListener);
                    mHasEnergyProfilePermission = true;
                } catch (SecurityException e) {
                    mHasEnergyProfilePermission = false;
                }
                carInfo.fetchEnergyProfile(mCarHardwareExecutor, mEnergyProfileListener);
            }

        });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();
        if (allInfoAvailable()) {
            Row.Builder modelRowBuilder = new Row.Builder()
                    .setTitle("Model Information");
            if (!mHasModelPermission) {
                modelRowBuilder.addText("No Model Permission.");
            } else {
                StringBuilder info = new StringBuilder();
                if (mModel.getManufacturer().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Manufacturer unavailable, ");
                } else {
                    info.append(mModel.getManufacturer().getValue());
                    info.append(", ");
                }
                if (mModel.getName().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Model unavailable, ");
                } else {
                    info.append(mModel.getName().getValue());
                    info.append(", ");
                }
                if (mModel.getYear().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Year unavailable");
                } else {
                    info.append(mModel.getYear().getValue());
                }
                modelRowBuilder.addText(info);
            }
            paneBuilder.addRow(modelRowBuilder.build());

            Row.Builder energyProfileRowBuilder = new Row.Builder()
                    .setTitle("Energy Profile");
            if (!mHasModelPermission) {
                energyProfileRowBuilder.addText("No Energy Profile Permission.");
            } else {
                StringBuilder fuelInfo = new StringBuilder();
                if (mEnergyProfile.getFuelTypes().getStatus() != CarValue.STATUS_SUCCESS) {
                    fuelInfo.append("Fuel Types: Unavailable.");
                } else {
                    fuelInfo.append("Fuel Types: ");
                    for (int fuelType : mEnergyProfile.getFuelTypes().getValue()) {
                        fuelInfo.append(fuelTypeAsString(fuelType));
                        fuelInfo.append(" ");
                    }
                }
                energyProfileRowBuilder.addText(fuelInfo);
                StringBuilder evInfo = new StringBuilder();
                if (mEnergyProfile.getEvConnectorTypes().getStatus() != CarValue.STATUS_SUCCESS) {
                    evInfo.append(" EV Connector Types: Unavailable.");
                } else {
                    evInfo.append("EV Connector Types: ");
                    for (int connectorType : mEnergyProfile.getEvConnectorTypes().getValue()) {
                        evInfo.append(evConnectorAsString(connectorType));
                        evInfo.append(" ");
                    }
                }
                energyProfileRowBuilder.addText(evInfo);
            }
            paneBuilder.addRow(energyProfileRowBuilder.build());
        } else {
            paneBuilder.setLoading(true);
        }
        return new PaneTemplate.Builder(paneBuilder.build())
                .setHeaderAction(Action.BACK)
                .setTitle("Car Hardware Information")
                .build();
    }

    private boolean allInfoAvailable() {
        if (mHasModelPermission && mModel == null) {
            return false;
        }
        if (mHasEnergyProfilePermission && mEnergyProfile == null) {
            return false;
        }
        return true;
    }

    private String fuelTypeAsString(int fuelType) {
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

    private String evConnectorAsString(int evConnectorType) {
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
}
