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

package androidx.car.app.sample.showcase.common.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.Accelerometer;
import androidx.car.app.hardware.info.CarHardwareLocation;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;
import androidx.car.app.hardware.info.Compass;
import androidx.car.app.hardware.info.EnergyLevel;
import androidx.car.app.hardware.info.Gyroscope;
import androidx.car.app.hardware.info.Mileage;
import androidx.car.app.hardware.info.Speed;
import androidx.car.app.hardware.info.TollCard;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.Executor;

/** Renderer which aggregates information about the car hardware to be drawn on a surface. */
public final class CarHardwareRenderer implements Renderer {
    private static final String TAG = "showcase";

    private static final int ROW_SPACING = 10;
    private static final int LEFT_MARGIN = 15;

    private final Executor mCarHardwareExecutor;
    private final Paint mCarInfoPaint = new Paint();
    private final CarContext mCarContext;

    @Nullable
    TollCard mTollCard;
    @Nullable
    EnergyLevel mEnergyLevel;
    @Nullable
    Speed mSpeed;
    @Nullable
    Mileage mMileage;
    @Nullable
    Accelerometer mAccelerometer;
    @Nullable
    Gyroscope mGyroscope;
    @Nullable
    Compass mCompass;
    @Nullable
    CarHardwareLocation mCarHardwareLocation;
    @Nullable
    private Runnable mRequestRenderRunnable;
    private boolean mHasTollCardPermission;
    private boolean mHasEnergyLevelPermission;
    private boolean mHasSpeedPermission;
    private boolean mHasMileagePermission;
    private boolean mHasAccelerometerPermission;
    private boolean mHasGyroscopePermission;
    private boolean mHasCompassPermission;
    private boolean mHasCarHardwareLocationPermission;

    private OnCarDataAvailableListener<TollCard> mTollListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received toll information:" + data);
            mTollCard = data;
            requestRenderFrame();
        }
    };
    private OnCarDataAvailableListener<EnergyLevel> mEnergyLevelListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received energy level information: " + data);
            mEnergyLevel = data;
            requestRenderFrame();
        }
    };
    private OnCarDataAvailableListener<Speed> mSpeedListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received speed information: " + data);
            mSpeed = data;
            requestRenderFrame();
        }
    };
    private OnCarDataAvailableListener<Mileage> mMileageListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received mileage: " + data);
            mMileage = data;
            requestRenderFrame();
        }
    };
    private OnCarDataAvailableListener<Accelerometer> mAccelerometerListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received accelerometer: " + data);
            mAccelerometer = data;
            requestRenderFrame();
        }
    };
    private OnCarDataAvailableListener<Gyroscope> mGyroscopeListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received gyroscope: " + data);
            mGyroscope = data;
            requestRenderFrame();
        }
    };
    private OnCarDataAvailableListener<Compass> mCompassListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received compass: " + data);
            mCompass = data;
            requestRenderFrame();
        }
    };
    private OnCarDataAvailableListener<CarHardwareLocation> mCarLocationListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received car location: " + data);
            mCarHardwareLocation = data;
            requestRenderFrame();
        }
    };

    public CarHardwareRenderer(@NonNull CarContext carContext) {
        mCarContext = carContext;
        mCarInfoPaint.setColor(Color.BLACK);
        mCarInfoPaint.setAntiAlias(true);
        mCarInfoPaint.setStyle(Paint.Style.STROKE);
        mCarHardwareExecutor = ContextCompat.getMainExecutor(mCarContext);
    }

    @Override
    public void enable(@NonNull Runnable onChangeListener) {
        mRequestRenderRunnable = onChangeListener;
        CarHardwareManager carHardwareManager =
                mCarContext.getCarService(CarHardwareManager.class);
        CarInfo carInfo = carHardwareManager.getCarInfo();
        CarSensors carSensors = carHardwareManager.getCarSensors();

        // Request car info subscription items.
        mTollCard = null;
        try {
            carInfo.addTollListener(mCarHardwareExecutor, mTollListener);
            mHasTollCardPermission = true;
        } catch (SecurityException e) {
            mHasTollCardPermission = false;
        }

        mEnergyLevel = null;
        try {
            carInfo.addEnergyLevelListener(mCarHardwareExecutor, mEnergyLevelListener);
            mHasEnergyLevelPermission = true;
        } catch (SecurityException e) {
            mHasEnergyLevelPermission = false;
        }

        mSpeed = null;
        try {
            carInfo.addSpeedListener(mCarHardwareExecutor, mSpeedListener);
            mHasSpeedPermission = true;
        } catch (SecurityException e) {
            mHasSpeedPermission = false;
        }

        mMileage = null;
        try {
            carInfo.addMileageListener(mCarHardwareExecutor, mMileageListener);
            mHasMileagePermission = true;
        } catch (SecurityException e) {
            mHasMileagePermission = false;
        }

        // Request sensors
        mCompass = null;
        try {
            carSensors.addCompassListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor,
                    mCompassListener);
            mHasCompassPermission = true;
        } catch (SecurityException e) {
            mHasCompassPermission = false;
        }

        mGyroscope = null;
        try {
            carSensors.addGyroscopeListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor,
                    mGyroscopeListener);
            mHasGyroscopePermission = true;
        } catch (SecurityException e) {
            mHasGyroscopePermission = false;
        }

        mAccelerometer = null;
        try {
            carSensors.addAccelerometerListener(CarSensors.UPDATE_RATE_NORMAL,
                    mCarHardwareExecutor,
                    mAccelerometerListener);
            mHasAccelerometerPermission = true;
        } catch (SecurityException e) {
            mHasAccelerometerPermission = false;
        }

        mCarHardwareLocation = null;
        try {
            carSensors.addCarHardwareLocationListener(CarSensors.UPDATE_RATE_NORMAL,
                    mCarHardwareExecutor, mCarLocationListener);
            mHasCarHardwareLocationPermission = true;
        } catch (SecurityException e) {
            mHasCarHardwareLocationPermission = false;
        }
    }

    @Override
    public void disable() {
        mRequestRenderRunnable = null;
        CarHardwareManager carHardwareManager =
                mCarContext.getCarService(CarHardwareManager.class);
        CarInfo carInfo = carHardwareManager.getCarInfo();
        CarSensors carSensors = carHardwareManager.getCarSensors();

        try {
            // Unsubscribe carinfo
            carInfo.removeTollListener(mTollListener);
            mHasTollCardPermission = true;
        } catch (SecurityException e) {
            mHasTollCardPermission = false;
        }

        mTollCard = null;
        try {
            carInfo.removeEnergyLevelListener(mEnergyLevelListener);
            mHasEnergyLevelPermission = true;
        } catch (SecurityException e) {
            mHasEnergyLevelPermission = false;
        }

        mEnergyLevel = null;
        try {
            carInfo.removeSpeedListener(mSpeedListener);
            mHasSpeedPermission = true;
        } catch (SecurityException e) {
            mHasSpeedPermission = false;
        }

        mSpeed = null;
        try {
            carInfo.removeMileageListener(mMileageListener);
            mHasMileagePermission = true;
        } catch (SecurityException e) {
            mHasMileagePermission = false;
        }

        mMileage = null;
        try {
            // Unsubscribe sensors
            carSensors.removeCompassListener(mCompassListener);
            mHasCompassPermission = true;
        } catch (SecurityException e) {
            mHasCompassPermission = false;
        }

        mCompass = null;
        try {
            carSensors.removeGyroscopeListener(mGyroscopeListener);
            mHasGyroscopePermission = true;
        } catch (SecurityException e) {
            mHasGyroscopePermission = false;
        }

        mGyroscope = null;
        try {
            carSensors.removeAccelerometerListener(mAccelerometerListener);
            mHasAccelerometerPermission = true;
        } catch (SecurityException e) {
            mHasAccelerometerPermission = false;
        }

        mAccelerometer = null;
        try {
            carSensors.removeCarHardwareLocationListener(mCarLocationListener);
            mHasCarHardwareLocationPermission = true;
        } catch (SecurityException e) {
            mHasCarHardwareLocationPermission = false;
        }

        mCarHardwareLocation = null;
    }

    @Override
    public void renderFrame(@NonNull Canvas canvas, @Nullable Rect visibleArea,
            @Nullable Rect stableArea) {
        if (stableArea != null) {
            if (stableArea.isEmpty()) {
                // No inset set. The entire area is considered safe to draw.
                stableArea.set(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
            }

            int height = stableArea.height() / 8;
            int updatedSize = height - ROW_SPACING;
            mCarInfoPaint.setTextSize(updatedSize);

            canvas.drawRect(stableArea, mCarInfoPaint);

            Paint.FontMetrics fm = mCarInfoPaint.getFontMetrics();
            float verticalPos = stableArea.top - fm.ascent;

            // Prepare text for Toll card status
            StringBuilder info = new StringBuilder();
            if (!mHasTollCardPermission) {
                info.append("No TollCard Permission.");
            } else if (mTollCard == null) {
                info.append("Fetching Toll information.");
            } else {
                if (mTollCard.getCardState().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Toll card state: N/A. ");
                } else {
                    info.append("Toll card state: ");
                    info.append(mTollCard.getCardState().getValue());
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Energy Level
            info = new StringBuilder();
            if (!mHasEnergyLevelPermission) {
                info.append("No EnergyLevel Permission.");
            } else if (mEnergyLevel == null) {
                info.append("Fetching Energy Level.");
            } else {
                if (mEnergyLevel.getEnergyIsLow().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Low energy: N/A. ");
                } else {
                    info.append("Low energy: ");
                    info.append(mEnergyLevel.getEnergyIsLow().getValue());
                    info.append(" ");
                }
                if (mEnergyLevel.getRangeRemainingMeters().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Range: N/A. ");
                } else {
                    info.append("Range: ");
                    info.append(mEnergyLevel.getRangeRemainingMeters().getValue());
                    info.append(" m. ");
                }
                if (mEnergyLevel.getFuelPercent().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Fuel: N/A. ");
                } else {
                    info.append("Fuel: ");
                    info.append(mEnergyLevel.getFuelPercent().getValue());
                    info.append("% ");
                }
                if (mEnergyLevel.getBatteryPercent().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Battery: N/A. ");
                } else {
                    info.append("Battery: ");
                    info.append(mEnergyLevel.getBatteryPercent().getValue());
                    info.append("% ");
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Speed
            info = new StringBuilder();
            if (!mHasSpeedPermission) {
                info.append("No Speed Permission.");
            } else if (mSpeed == null) {
                info.append("Fetching Speed.");
            } else {
                if (mSpeed.getDisplaySpeedMetersPerSecond().getStatus()
                        != CarValue.STATUS_SUCCESS) {
                    info.append("Display Speed: N/A. ");
                } else {
                    info.append("Display Speed: ");
                    info.append(mSpeed.getDisplaySpeedMetersPerSecond().getValue());
                    info.append(" m/s. ");
                }
                if (mSpeed.getRawSpeedMetersPerSecond().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Raw Speed: N/A. ");
                } else {
                    info.append("Raw Speed: ");
                    info.append(mSpeed.getRawSpeedMetersPerSecond().getValue());
                    info.append(" m/s. ");
                }
                if (mSpeed.getSpeedDisplayUnit().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Unit: N/A.");
                } else {
                    info.append("Unit: ");
                    info.append(mSpeed.getSpeedDisplayUnit().getValue());
                    info.append(" ");
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Odometer
            info = new StringBuilder();
            if (!mHasMileagePermission) {
                info.append("No Mileage Permission.");
            } else if (mMileage == null) {
                info.append("Fetching mileage.");
            } else {
                if (mMileage.getOdometerMeters().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Odometer: N/A. ");
                } else {
                    info.append("Odometer: ");
                    info.append(mMileage.getOdometerMeters().getValue());
                    info.append(" m. ");
                }
                if (mMileage.getDistanceDisplayUnit().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Unit: N/A.");
                } else {
                    info.append("Unit: ");
                    info.append(mMileage.getDistanceDisplayUnit().getValue());
                    info.append(" ");
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Accelerometer
            info = new StringBuilder();
            if (!mHasAccelerometerPermission) {
                info.append("No Accelerometer Permission.");
            } else if (mAccelerometer == null) {
                info.append("Fetching accelerometer");
            } else {
                if (mAccelerometer.getForces().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Accelerometer N/A.");
                } else {
                    info.append("Accelerometer: ");
                    appendFloatList(info, mAccelerometer.getForces().getValue());
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Gyroscope
            info = new StringBuilder();
            if (!mHasGyroscopePermission) {
                info.append("No Gyroscope Permission.");
            } else if (mGyroscope == null) {
                info.append("Fetching gyroscope");
            } else {
                if (mGyroscope.getRotations().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Gyroscope N/A.");
                } else {
                    info.append("Gyroscope: ");
                    appendFloatList(info, mGyroscope.getRotations().getValue());
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Compass
            info = new StringBuilder();
            if (!mHasCompassPermission) {
                info.append("No Compass Permission.");
            } else if (mCompass == null) {
                info.append("Fetching compass");
            } else {
                if (mCompass.getOrientations().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Compass N/A.");
                } else {
                    info.append("Compass: ");
                    appendFloatList(info, mCompass.getOrientations().getValue());
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Location
            info = new StringBuilder();
            if (!mHasCarHardwareLocationPermission) {
                info.append("No CarHardwareLocation Permission.");
            } else if (mCarHardwareLocation == null) {
                info.append("Fetching location");
            } else {
                if (mCarHardwareLocation.getLocation().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Car Hardware Location N/A");
                } else {
                    info.append("Car Hardware location: ");
                    info.append(mCarHardwareLocation.getLocation().getValue().toString());
                }
            }
            canvas.drawText(info.toString(), LEFT_MARGIN, verticalPos, mCarInfoPaint);
        }
    }

    private void requestRenderFrame() {
        if (mRequestRenderRunnable != null) {
            mRequestRenderRunnable.run();
        }
    }

    private void appendFloatList(StringBuilder builder, List<Float> values) {
        builder.append("[ ");
        for (Float value : values) {
            builder.append(value);
            builder.append(" ");
        }
        builder.append("]");
    }
}
