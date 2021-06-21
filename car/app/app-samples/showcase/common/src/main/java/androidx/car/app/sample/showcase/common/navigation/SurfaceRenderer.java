/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.navigation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataListener;
import androidx.car.app.hardware.info.Accelerometer;
import androidx.car.app.hardware.info.CarHardwareLocation;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;
import androidx.car.app.hardware.info.Compass;
import androidx.car.app.hardware.info.EnergyLevel;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Gyroscope;
import androidx.car.app.hardware.info.Mileage;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;
import androidx.car.app.hardware.info.Toll;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.List;
import java.util.concurrent.Executor;

/** A very simple implementation of a renderer for the app's background surface. */
public final class SurfaceRenderer implements DefaultLifecycleObserver {
    private static final String TAG = "showcase";

    private static final int HORIZONTAL_TEXT_MARGIN = 10;
    private static final int VERTICAL_TEXT_MARGIN_FROM_TOP = 20;
    private static final int VERTICAL_TEXT_MARGIN_FROM_BOTTOM = 10;

    private final CarContext mCarContext;
    private final Executor mCarHardwareExecutor;
    @Nullable
    Surface mSurface;
    @Nullable
    Rect mVisibleArea;
    @Nullable
    Rect mStableArea;
    private final Paint mLeftInsetPaint = new Paint();
    private final Paint mRightInsetPaint = new Paint();
    private final Paint mCenterPaint = new Paint();
    private final Paint mCarInfoPaint = new Paint();
    private boolean mShowCarHardwareSurfaceInfo;

    @Nullable Model mModel;
    @Nullable EnergyProfile mEnergyProfile;
    @Nullable Toll mToll;
    @Nullable EnergyLevel mEnergyLevel;
    @Nullable Speed mSpeed;
    @Nullable Mileage mMileage;
    @Nullable Accelerometer mAccelerometer;
    @Nullable Gyroscope mGyroscope;
    @Nullable Compass mCompass;
    @Nullable CarHardwareLocation mCarHardwareLocation;

    private OnCarDataListener<Model> mModelListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received model information %s", data));
            mModel = data;
            renderFrame();
        }
    };

    private OnCarDataListener<EnergyProfile> mEnergyProfileListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received energy profile information %s", data));
            mEnergyProfile = data;
            renderFrame();
        }
    };

    private OnCarDataListener<Toll> mTollListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received toll information %s", data));
            mToll = data;
            renderFrame();
        }
    };

    private OnCarDataListener<EnergyLevel> mEnergyLevelListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received energy level information %s", data));
            mEnergyLevel = data;
            renderFrame();
        }
    };

    private OnCarDataListener<Speed> mSpeedListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received speed information %s", data));
            mSpeed = data;
            renderFrame();
        }
    };

    private OnCarDataListener<Mileage> mMileageListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received mileage %s", data));
            mMileage = data;
            renderFrame();
        }
    };

    private OnCarDataListener<Accelerometer> mAccelerometerListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received accelerometer %s", data));
            mAccelerometer = data;
            renderFrame();
        }
    };

    private OnCarDataListener<Gyroscope> mGyroscopeListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received gyroscope %s", data));
            mGyroscope = data;
            renderFrame();
        }
    };

    private OnCarDataListener<Compass> mCompassListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received compass %s", data));
            mCompass = data;
            renderFrame();
        }
    };

    private OnCarDataListener<CarHardwareLocation> mCarLocationListener = data -> {
        synchronized (SurfaceRenderer.this) {
            Log.i(TAG, String.format("Received car location %s", data));
            mCarHardwareLocation = data;
            renderFrame();
        }
    };

    private final SurfaceCallback mSurfaceCallback =
            new SurfaceCallback() {
                @Override
                public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
                    Log.i(TAG, String.format("Surface available %s", surfaceContainer));
                    mSurface = surfaceContainer.getSurface();
                    renderFrame();
                }

                @Override
                public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
                    synchronized (SurfaceRenderer.this) {
                        Log.i(
                                TAG,
                                String.format(
                                        "Visible area changed %s. stableArea:%s visibleArea:%s",
                                        mSurface, mStableArea, visibleArea));
                        mVisibleArea = visibleArea;
                        renderFrame();
                    }
                }

                @Override
                public void onStableAreaChanged(@NonNull Rect stableArea) {
                    synchronized (SurfaceRenderer.this) {
                        Log.i(
                                TAG,
                                String.format(
                                        "Stable area changed %s. stable:%s inset:%s",
                                        mSurface, stableArea, mVisibleArea));
                        mStableArea = stableArea;
                        renderFrame();
                    }
                }

                @Override
                public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
                    synchronized (SurfaceRenderer.this) {
                        mSurface = null;
                    }
                }
            };

    public SurfaceRenderer(@NonNull CarContext carContext, @NonNull Lifecycle lifecycle) {
        mCarContext = carContext;

        mLeftInsetPaint.setColor(Color.RED);
        mLeftInsetPaint.setAntiAlias(true);
        mLeftInsetPaint.setStyle(Style.STROKE);

        mRightInsetPaint.setColor(Color.RED);
        mRightInsetPaint.setAntiAlias(true);
        mRightInsetPaint.setStyle(Style.STROKE);
        mRightInsetPaint.setTextAlign(Align.RIGHT);

        mCenterPaint.setColor(Color.BLUE);
        mCenterPaint.setAntiAlias(true);
        mCenterPaint.setStyle(Style.STROKE);

        mCarInfoPaint.setColor(Color.BLACK);
        mCarInfoPaint.setAntiAlias(true);
        mCarInfoPaint.setStyle(Style.STROKE);
        mCarInfoPaint.setTextAlign(Align.CENTER);
        mCarHardwareExecutor = ContextCompat.getMainExecutor(mCarContext);

        lifecycle.addObserver(this);
    }

    /** Callback called when the car configuration changes. */
    public void onCarConfigurationChanged() {
        renderFrame();
    }

    /** Tells the renderer whether to subscribe and show car hardware information. */
    public void setCarHardwareSurfaceRendererEnabledState(boolean isEnabled) {
        if (isEnabled == mShowCarHardwareSurfaceInfo) {
            return;
        }
        CarHardwareManager carHardwareManager =
                mCarContext.getCarService(CarHardwareManager.class);
        CarInfo carInfo = carHardwareManager.getCarInfo();
        CarSensors carSensors = carHardwareManager.getCarSensors();
        if (isEnabled) {
            // Request any single shot values.
            mModel = null;
            carInfo.getModel(mCarHardwareExecutor, mModelListener);

            mEnergyProfile = null;
            carInfo.getEnergyProfile(mCarHardwareExecutor, mEnergyProfileListener);

            // Request car info subscription items.
            mToll = null;
            carInfo.addTollListener(mCarHardwareExecutor, mTollListener);

            mEnergyLevel = null;
            carInfo.addEnergyLevelListener(mCarHardwareExecutor, mEnergyLevelListener);

            mSpeed = null;
            carInfo.addSpeedListener(mCarHardwareExecutor, mSpeedListener);

            mMileage = null;
            carInfo.addMileageListener(mCarHardwareExecutor, mMileageListener);

            // Request sensors
            mCompass = null;
            carSensors.addCompassListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor,
                    mCompassListener);
            mGyroscope = null;
            carSensors.addGyroscopeListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor,
                    mGyroscopeListener);
            mAccelerometer = null;
            carSensors.addAccelerometerListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor,
                    mAccelerometerListener);
            mCarHardwareLocation = null;
            carSensors.addCarHardwareLocationListener(CarSensors.UPDATE_RATE_NORMAL,
                    mCarHardwareExecutor, mCarLocationListener);
        } else {
            // Unsubscribe carinfo
            carInfo.removeTollListener(mTollListener);
            mToll = null;
            carInfo.removeEnergyLevelListener(mEnergyLevelListener);
            mEnergyLevel = null;
            carInfo.removeSpeedListener(mSpeedListener);
            mSpeed = null;
            carInfo.removeMileageListener(mMileageListener);
            mMileage = null;

            // Unsubscribe sensors
            carSensors.removeCompassListener(mCompassListener);
            mCompass = null;
            carSensors.removeGyroscopeListener(mGyroscopeListener);
            mGyroscope = null;
            carSensors.removeAccelerometerListener(mAccelerometerListener);
            mAccelerometer = null;
            carSensors.removeCarHardwareLocationListener(mCarLocationListener);
            mCarHardwareLocation = null;
        }
        mShowCarHardwareSurfaceInfo = isEnabled;
        renderFrame();
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Log.i(TAG, "SurfaceRenderer created");
        mCarContext.getCarService(AppManager.class).setSurfaceCallback(mSurfaceCallback);
    }

    void renderFrame() {
        if (mSurface == null || !mSurface.isValid()) {
            // Surface is not available, or has been destroyed, skip this frame.
            return;
        }
        Canvas canvas = mSurface.lockCanvas(null);

        // Clear the background.
        canvas.drawColor(mCarContext.isDarkMode() ? Color.DKGRAY : Color.LTGRAY);

        if (mShowCarHardwareSurfaceInfo) {
            renderCarInfoFrame(canvas);
        } else {
            renderStandardFrame(canvas);
        }
        mSurface.unlockCanvasAndPost(canvas);

    }

    private void renderCarInfoFrame(Canvas canvas) {
        Rect visibleArea = mVisibleArea;
        if (visibleArea != null) {
            if (visibleArea.isEmpty()) {
                // No inset set. The entire area is considered safe to draw.
                visibleArea.set(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
            }

            Paint.FontMetrics fm = mCarInfoPaint.getFontMetrics();
            float height = fm.descent - fm.ascent;
            float verticalPos = visibleArea.top + VERTICAL_TEXT_MARGIN_FROM_TOP;

            // Prepare text for Make, Model, Year
            StringBuilder info = new StringBuilder();
            if (mModel == null) {
                info.append("Fetching model info.");
            } else {
                if (mModel.getManufacturer().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Manufacturer unavailable, ");
                } else {
                    info.append(mModel.getManufacturer().getValue());
                    info.append(",");
                }
                if (mModel.getName().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Model unavailable, ");
                } else {
                    info.append(mModel.getName());
                    info.append(",");
                }
                if (mModel.getYear().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Year unavailable.");
                } else {
                    info.append(mModel.getYear());
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Energy Profile
            info = new StringBuilder();
            if (mEnergyProfile == null) {
                info.append("Fetching EnergyProfile.");
            } else {
                if (mEnergyProfile.getFuelTypes().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Fuel Types: Unavailable. ");
                } else {
                    info.append("Fuel Types: [");
                    for (int fuelType: mEnergyProfile.getFuelTypes().getValue()) {
                        info.append(fuelType);
                        info.append(" ");
                    }
                    info.append("].");
                }
                if (mEnergyProfile.getEvConnectorTypes().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append(" EV Connector Types: Unavailable. ");
                } else {
                    info.append("EV Connector Types:[");
                    for (int connectorType: mEnergyProfile.getEvConnectorTypes().getValue()) {
                        info.append(connectorType);
                        info.append(" ");
                    }
                    info.append("]");
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Toll card status
            if (mToll == null) {
                info.append("Fetching Toll information.");
            } else {
                if (mToll.getCardState().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Toll card state: Unavailable. ");
                } else {
                    info.append("Toll card state: ");
                    info.append(mToll.getCardState().getValue());
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Energy Level
            if (mEnergyLevel == null) {
                info.append("Fetching Energy Level.");
            } else {
                if (mEnergyLevel.getEnergyIsLow().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Low energy: Unavailable. ");
                } else {
                    info.append("Low energy: ");
                    info.append(mEnergyLevel.getEnergyIsLow().getValue());
                    info.append(" ");
                }
                if (mEnergyLevel.getRangeRemaining().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Range: Unavailable. ");
                } else {
                    info.append("Range: ");
                    info.append(mEnergyLevel.getRangeRemaining().getValue());
                    info.append(" m. ");
                }
                if (mEnergyLevel.getFuelPercent().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Fuel Percent: Unavailable. ");
                } else {
                    info.append("Fuel Percent: ");
                    info.append(mEnergyLevel.getFuelPercent().getValue());
                    info.append("% ");
                }
                if (mEnergyLevel.getBatteryPercent().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Battery Percent: Unavailable. ");
                } else {
                    info.append("Battery Percent: ");
                    info.append(mEnergyLevel.getBatteryPercent().getValue());
                    info.append("% ");
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Speed
            if (mSpeed == null) {
                info.append("Fetching Speed.");
            } else {
                if (mSpeed.getDisplaySpeed().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Display Speed: Unavailable. ");
                } else {
                    info.append("Display Speed: ");
                    info.append(mSpeed.getDisplaySpeed().getValue());
                    info.append(" m/s. ");
                }
                if (mSpeed.getRawSpeed().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Raw Speed: Unavailable. ");
                } else {
                    info.append("Raw Speed: ");
                    info.append(mSpeed.getRawSpeed().getValue());
                    info.append(" m/s. ");
                }
                if (mSpeed.getSpeedDisplayUnit().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Speed Display Unit: Unavailable.");
                } else {
                    info.append("Speed Display Unit: ");
                    info.append(mSpeed.getSpeedDisplayUnit().getValue());
                    info.append(" ");
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Odometer
            if (mMileage == null) {
                info.append("Fetching mileage.");
            } else {
                if (mMileage.getOdometer().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Odometer: Unavailable. ");
                } else {
                    info.append("Odometer: ");
                    info.append(mMileage.getOdometer().getValue());
                    info.append(" m. ");
                }
                if (mMileage.getDistanceDisplayUnit().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Mileage Display Unit: Unavailable.");
                } else {
                    info.append("Mileage Display Unit: ");
                    info.append(mMileage.getDistanceDisplayUnit().getValue());
                    info.append(" ");
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Accelerometer
            if (mAccelerometer == null) {
                info.append("Fetching accelerometer");
            } else {
                if (mAccelerometer.getForces().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Accelerometer unavailable.");
                } else {
                    info.append("Accelerometer: ");
                    appendFloatList(info, mAccelerometer.getForces().getValue());
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Gyroscope
            if (mGyroscope == null) {
                info.append("Fetching gyroscope");
            } else {
                if (mGyroscope.getRotations().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Gyroscope unavailable.");
                } else {
                    info.append("Gyroscope: ");
                    appendFloatList(info, mGyroscope.getRotations().getValue());
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Compass
            if (mCompass == null) {
                info.append("Fetching compass");
            } else {
                if (mCompass.getOrientations().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Compass unavailable.");
                } else {
                    info.append("Compass: ");
                    appendFloatList(info, mCompass.getOrientations().getValue());
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
            verticalPos += height;

            // Prepare text for Location
            if (mCarHardwareLocation == null) {
                info.append("Fetching location");
            } else {
                if (mCarHardwareLocation.getLocation().getStatus() != CarValue.STATUS_SUCCESS) {
                    info.append("Car Hardware Location unavailable");
                } else {
                    info.append("Car Hardware location: ");
                    info.append(mCarHardwareLocation.getLocation().getValue().toString());
                }
            }
            canvas.drawText(info.toString(), visibleArea.centerX(), verticalPos, mCarInfoPaint);
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

    private void renderStandardFrame(Canvas canvas) {

        // Draw a rectangle showing the inset.
        Rect visibleArea = mVisibleArea;
        if (visibleArea != null) {
            if (visibleArea.isEmpty()) {
                // No inset set. The entire area is considered safe to draw.
                visibleArea.set(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
            }

            canvas.drawRect(visibleArea, mLeftInsetPaint);
            canvas.drawLine(
                    visibleArea.left,
                    visibleArea.top,
                    visibleArea.right,
                    visibleArea.bottom,
                    mLeftInsetPaint);
            canvas.drawLine(
                    visibleArea.right,
                    visibleArea.top,
                    visibleArea.left,
                    visibleArea.bottom,
                    mLeftInsetPaint);
            canvas.drawText(
                    "(" + visibleArea.left + " , " + visibleArea.top + ")",
                    visibleArea.left + HORIZONTAL_TEXT_MARGIN,
                    visibleArea.top + VERTICAL_TEXT_MARGIN_FROM_TOP,
                    mLeftInsetPaint);
            canvas.drawText(
                    "(" + visibleArea.right + " , " + visibleArea.bottom + ")",
                    visibleArea.right - HORIZONTAL_TEXT_MARGIN,
                    visibleArea.bottom - VERTICAL_TEXT_MARGIN_FROM_BOTTOM,
                    mRightInsetPaint);
        } else {
            Log.d(TAG, "Visible area not available.");
        }

        if (mStableArea != null) {
            // Draw a cross-hairs at the stable center.
            final int lengthPx = 15;
            int centerX = mStableArea.centerX();
            int centerY = mStableArea.centerY();
            canvas.drawLine(centerX - lengthPx, centerY, centerX + lengthPx, centerY, mCenterPaint);
            canvas.drawLine(centerX, centerY - lengthPx, centerX, centerY + lengthPx, mCenterPaint);
            canvas.drawText(
                    "(" + centerX + ", " + centerY + ")",
                    centerX + HORIZONTAL_TEXT_MARGIN,
                    centerY,
                    mCenterPaint);
        } else {
            Log.d(TAG, "Stable area not available.");
        }
    }
}
