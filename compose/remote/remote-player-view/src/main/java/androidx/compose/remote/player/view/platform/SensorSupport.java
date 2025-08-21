/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.view.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;

@RestrictTo(LIBRARY_GROUP)
/** Implements sensors management (used by RemoteComposePlayer) */
public class SensorSupport {
    SensorManager mSensorManager;
    Sensor mAcc = null, mGyro = null, mMag = null, mLight = null;
    SensorEventListener mListener;
    RemoteComposeView mRemoteComposeView;

    /**
     * Sets up the sensors based on the listeners present in the RemoteComposeView. This method
     * initializes the necessary sensors (accelerometer, gyroscope, magnetometer, light) if there
     * are corresponding listeners registered in the provided RemoteComposeView.
     *
     * @param application The application context, used to get the SensorManager service.
     * @param view The RemoteComposeView instance that contains sensor listeners and will receive
     *     sensor data.
     */
    public void setupSensors(@NonNull Context application, @NonNull RemoteComposeView view) {
        mRemoteComposeView = view;
        int minId = RemoteContext.ID_ACCELERATION_X;
        int maxId = RemoteContext.ID_LIGHT;
        int[] ids = new int[1 + maxId - minId];

        int count = mRemoteComposeView.copySensorListeners(ids);
        mAcc = null;
        mGyro = null;
        mMag = null;
        mLight = null;
        if (count > 0) {
            mSensorManager = (SensorManager) application.getSystemService(Context.SENSOR_SERVICE);
            for (int i = 0; i < count; i++) {
                switch (ids[i]) {
                    case RemoteContext.ID_ACCELERATION_X:
                    case RemoteContext.ID_ACCELERATION_Y:
                    case RemoteContext.ID_ACCELERATION_Z:
                        if (mAcc == null) {
                            mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                        }
                        break;
                    case RemoteContext.ID_GYRO_ROT_X:
                    case RemoteContext.ID_GYRO_ROT_Y:
                    case RemoteContext.ID_GYRO_ROT_Z:
                        if (mGyro == null) {
                            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                        }
                        break;
                    case RemoteContext.ID_MAGNETIC_X:
                    case RemoteContext.ID_MAGNETIC_Y:
                    case RemoteContext.ID_MAGNETIC_Z:
                        if (mMag == null) {
                            mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                        }
                        break;
                    case RemoteContext.ID_LIGHT:
                        if (mLight == null) {
                            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                        }
                }
            }
        }
        registerListener();
    }

    private void registerListener() {
        Sensor[] s = {mAcc, mGyro, mMag, mLight};
        if (mListener != null) {
            unregisterListener();
        }
        SensorEventListener listener =
                new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (event.sensor == mAcc) {
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_ACCELERATION_X, event.values[0]);
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_ACCELERATION_Y, event.values[1]);
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_ACCELERATION_Z, event.values[2]);
                        } else if (event.sensor == mGyro) {
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_GYRO_ROT_X, event.values[0]);
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_GYRO_ROT_Y, event.values[1]);
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_GYRO_ROT_Z, event.values[2]);
                        } else if (event.sensor == mMag) {
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_MAGNETIC_X, event.values[0]);
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_MAGNETIC_Y, event.values[1]);
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_MAGNETIC_Z, event.values[2]);
                        } else if (event.sensor == mLight) {
                            mRemoteComposeView.setExternalFloat(
                                    RemoteContext.ID_LIGHT, event.values[0]);
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
                };

        Sensor[] sensors = {mAcc, mGyro, mMag, mLight};
        for (int i = 0; i < sensors.length; i++) {
            Sensor sensor = sensors[i];
            if (sensor != null) {
                mListener = listener;
                mSensorManager.registerListener(
                        mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    /**
     * Unregisters the sensor listener from the SensorManager. This method stops listening for
     * sensor events by unregistering the current listener.
     */
    public void unregisterListener() {
        if (mListener != null && mSensorManager != null) {
            mSensorManager.unregisterListener(mListener);
        }
        mListener = null;
    }
}
