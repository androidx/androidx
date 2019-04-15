/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.integration.timing;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An activity used to run performance test case.
 *
 * <p>To run performance test case, please implement this Activity. Camerax Use Case can be
 * implement in prepareUseCase and runUseCase. For performance result, you can set currentTimeMillis
 * to startTime and store the execution time into totalTime. At the end of test case, please call
 * onUseCaseFinish() to notify the lock.
 */
public abstract class BaseActivity extends AppCompatActivity {
    public static final long MICROS_IN_SECOND = TimeUnit.SECONDS.toMillis(1);
    public static final long PREVIEW_FILL_BUFFER_TIME = 1500;
    private static final String TAG = "BaseActivity";
    public long startTime;
    public long totalTime;
    public long openCameraStartTime;
    public long openCameraTotalTime;
    public long startRreviewTime;
    public long startPreviewTotalTime;
    public long previewFrameRate;
    public long closeCameraStartTime;
    public long closeCameraTotalTime;
    public String imageResolution;
    public CountDownLatch latch;

    /**
     * Prepares the use case.
     */
    public abstract void prepareUseCase();

    /**
     * Activates use case so it will receive data from camera.
     *
     * @throws InterruptedException on fatal errors.
     */
    public abstract void runUseCase() throws InterruptedException;

    /**
     * Called when the test case finishes.
     * <p>Could be called in CameraDevice's state callbacks.
     */
    public void onUseCaseFinish() {
        latch.countDown();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        latch = new CountDownLatch(1);
    }
}
