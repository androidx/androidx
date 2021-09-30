/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP, shadows = {
        CameraManagerCompatTest.ShadowInteractionCameraManager.class})
public final class CameraManagerCompatTest {

    private static final String CAMERA_ID = "0";

    private Context mContext;
    private ShadowInteractionCameraManager.Callback mInteractionCallback;
    private ShadowInteractionCameraManager mShadowCameraManager;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        mContext = ApplicationProvider.getApplicationContext();

        // Install the mock camera manager
        mInteractionCallback = mock(ShadowInteractionCameraManager.Callback.class);
        mShadowCameraManager = Shadow.extract(
                mContext.getSystemService(Context.CAMERA_SERVICE));
        mShadowCameraManager.addCallback(mInteractionCallback);
    }

    @Test
    public void getCameraCharacteristicsCompat_callUnderlyingMethod()
            throws CameraAccessExceptionCompat {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        manager.getCameraCharacteristicsCompat(CAMERA_ID);

        verify(mInteractionCallback, times(1)).getCameraCharacteristics(any(String.class));
    }


    @Test
    public void getCameraCharacteristicsCompat_getSameInstanceForSameId()
            throws CameraAccessExceptionCompat {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        CameraCharacteristicsCompat cameraCharacteristicsCompat1 =
                manager.getCameraCharacteristicsCompat(CAMERA_ID);

        CameraCharacteristicsCompat cameraCharacteristicsCompat2 =
                manager.getCameraCharacteristicsCompat(CAMERA_ID);

        assertThat(cameraCharacteristicsCompat1).isSameInstanceAs(cameraCharacteristicsCompat2);
    }

    @Test
    public void getCameraCharacteristicsCompat_canGetItems() throws CameraAccessExceptionCompat {
        mShadowCameraManager
                .putCameraCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 2);
        mShadowCameraManager
                .putCameraCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 1);

        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        CameraCharacteristicsCompat cameraCharacteristicsCompat =
                manager.getCameraCharacteristicsCompat(CAMERA_ID);

        assertThat(cameraCharacteristicsCompat.get(
                CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)).isEqualTo(2);
        assertThat(cameraCharacteristicsCompat.get(
                CameraCharacteristics.CONTROL_MAX_REGIONS_AF)).isEqualTo(1);
    }

    @Test
    @Config(maxSdk = 27)
    public void openCamera_callsHandlerMethod() throws CameraAccessExceptionCompat {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        manager.openCamera(CAMERA_ID, mock(Executor.class),
                mock(CameraDevice.StateCallback.class));

        verify(mInteractionCallback, times(1)).openCamera(any(String.class),
                any(CameraDevice.StateCallback.class), any(Handler.class));
    }

    @Test
    @Config(minSdk = 28)
    public void openCamera_callsExecutorMethod() throws CameraAccessExceptionCompat {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        manager.openCamera(CAMERA_ID, mock(Executor.class),
                mock(CameraDevice.StateCallback.class));

        verify(mInteractionCallback, times(1)).openCamera(any(String.class),
                any(Executor.class), any(CameraDevice.StateCallback.class));
    }

    @Test
    public void unwrap_allowsAccessToUnderlyingMethods() throws CameraAccessException {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        manager.unwrap().getCameraIdList();

        verify(mInteractionCallback, times(1)).getCameraIdList();
    }

    @Test
    @Config(maxSdk = 27)
    public void registerAvailabilityCallback_callsHandlerMethod() {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        manager.registerAvailabilityCallback(mock(Executor.class),
                mock(CameraManager.AvailabilityCallback.class));

        verify(mInteractionCallback, times(1)).registerAvailabilityCallback(
                any(CameraManager.AvailabilityCallback.class), any(Handler.class));
    }

    @Test
    @Config(minSdk = 28)
    public void registerAvailabilityCallback_callsExecutorMethod() {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        manager.registerAvailabilityCallback(mock(Executor.class),
                mock(CameraManager.AvailabilityCallback.class));

        verify(mInteractionCallback, times(1)).registerAvailabilityCallback(any(Executor.class),
                any(CameraManager.AvailabilityCallback.class));
    }

    @Test
    @Config(maxSdk = 27) // API 28 and above does not wrap the callback
    public void unregisterAvailabilityCallback_unregistersCorrectCallback() {
        // Capture the wrapper callback to check that the same wrapper is what is unregistered.
        CameraManager.AvailabilityCallback originalCallback = mock(
                CameraManager.AvailabilityCallback.class);
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);

        // Register the callback
        manager.registerAvailabilityCallback(mock(Executor.class), originalCallback);

        // Unregister the callback
        manager.unregisterAvailabilityCallback(originalCallback);

        ArgumentCaptor<CameraManager.AvailabilityCallback> registerCaptor = ArgumentCaptor.forClass(
                CameraManager.AvailabilityCallback.class);
        ArgumentCaptor<CameraManager.AvailabilityCallback> unregisterCaptor =
                ArgumentCaptor.forClass(CameraManager.AvailabilityCallback.class);


        verify(mInteractionCallback).registerAvailabilityCallback(registerCaptor.capture(),
                any(Handler.class));
        verify(mInteractionCallback).unregisterAvailabilityCallback(unregisterCaptor.capture());

        assertThat(registerCaptor.getValue()).isSameInstanceAs(unregisterCaptor.getValue());
    }

    @Test
    @Config(minSdk = 28)
    public void unregisterAvailabilityCallback_unregistersCorrectCallback_afterApi27() {
        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        CameraManager.AvailabilityCallback originalCallback = mock(
                CameraManager.AvailabilityCallback.class);

        manager.unregisterAvailabilityCallback(originalCallback);

        ArgumentCaptor<CameraManager.AvailabilityCallback> unregisterCaptor =
                ArgumentCaptor.forClass(CameraManager.AvailabilityCallback.class);

        verify(mInteractionCallback).unregisterAvailabilityCallback(unregisterCaptor.capture());

        assertThat(unregisterCaptor.getValue()).isSameInstanceAs(originalCallback);
    }

    @Test(expected = CameraAccessExceptionCompat.class)
    public void throwCameraAccessExceptionCompat_whenCallingGetCharacteristicsThrowAssertionError()
            throws CameraAccessExceptionCompat {
        when(mInteractionCallback.getCameraCharacteristics(any(String.class))).thenThrow(
                new AssertionError("CameraManager#getCameraCharacteristics AssertionError!"));

        CameraManagerCompat manager = CameraManagerCompat.from(mContext);
        manager.getCameraCharacteristicsCompat(CAMERA_ID);
    }

    /**
     * A Shadow of {@link CameraManager} which forwards invocations to callbacks to record
     * interactions.
     */
    @Implements(
            value = CameraManager.class,
            minSdk = 21
    )
    public static final class ShadowInteractionCameraManager {

        private static final String[] EMPTY_ID_LIST = new String[]{};
        private final List<Callback> mCallbacks = new ArrayList<>();
        private final CameraCharacteristics mCameraCharacteristics =
                mock(CameraCharacteristics.class);

        void addCallback(Callback callback) {
            mCallbacks.add(callback);
        }

        @NonNull
        @Implementation
        protected String[] getCameraIdList() throws CameraAccessException {
            for (Callback cb : mCallbacks) {
                String[] ids = cb.getCameraIdList();
            }

            return EMPTY_ID_LIST;
        }

        @NonNull
        @Implementation
        protected CameraCharacteristics getCameraCharacteristics(@NonNull String cameraId) {
            for (Callback cb : mCallbacks) {
                cb.getCameraCharacteristics(cameraId);
            }
            return mCameraCharacteristics;
        }

        public <T> void putCameraCharacteristic(CameraCharacteristics.Key<T> key, T value) {
            when(mCameraCharacteristics.get(key)).thenReturn(value);
        }

        @Implementation
        protected void openCamera(@NonNull String cameraId,
                @NonNull CameraDevice.StateCallback callback, @Nullable Handler handler) {
            for (Callback cb : mCallbacks) {
                cb.openCamera(cameraId, callback, handler);
            }
        }

        @Implementation
        protected void openCamera(@NonNull String cameraId,
                @NonNull Executor executor,
                @NonNull CameraDevice.StateCallback callback) {
            for (Callback cb : mCallbacks) {
                cb.openCamera(cameraId, executor, callback);
            }
        }

        @Implementation
        protected void registerAvailabilityCallback(
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraManager.AvailabilityCallback callback) {
            for (Callback cb : mCallbacks) {
                cb.registerAvailabilityCallback(executor, callback);
            }
        }

        @Implementation
        protected void registerAvailabilityCallback(
                @NonNull CameraManager.AvailabilityCallback callback, @Nullable Handler handler) {
            for (Callback cb : mCallbacks) {
                cb.registerAvailabilityCallback(callback, handler);
            }
        }

        @Implementation
        protected void unregisterAvailabilityCallback(
                @NonNull CameraManager.AvailabilityCallback callback) {
            for (Callback cb : mCallbacks) {
                cb.unregisterAvailabilityCallback(callback);
            }
        }

        interface Callback {
            @NonNull
            String[] getCameraIdList();

            @NonNull
            CameraCharacteristics getCameraCharacteristics(@NonNull String cameraId);

            void openCamera(@NonNull String cameraId,
                    @NonNull CameraDevice.StateCallback callback, @Nullable Handler handler);

            void openCamera(@NonNull String cameraId,
                    @NonNull Executor executor,
                    @NonNull CameraDevice.StateCallback callback);

            void registerAvailabilityCallback(@NonNull /* @CallbackExecutor */ Executor executor,
                    @NonNull CameraManager.AvailabilityCallback callback);

            void registerAvailabilityCallback(@NonNull CameraManager.AvailabilityCallback callback,
                    @Nullable Handler handler);

            void unregisterAvailabilityCallback(
                    @NonNull CameraManager.AvailabilityCallback callback);
        }
    }
}
