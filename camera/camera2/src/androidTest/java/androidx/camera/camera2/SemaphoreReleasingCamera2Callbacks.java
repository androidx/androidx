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

package androidx.camera.camera2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import java.util.concurrent.Semaphore;

/** Camera2 callbacks which release specific semaphores on each event. */
final class SemaphoreReleasingCamera2Callbacks {

    private SemaphoreReleasingCamera2Callbacks() {
    }

    /** A device state callback which releases a different semaphore for each method. */
    static final class DeviceStateCallback extends CameraDevice.StateCallback {
        private static final String TAG = DeviceStateCallback.class.getSimpleName();

        private final Semaphore onOpenedSemaphore = new Semaphore(0);
        private final Semaphore onClosedSemaphore = new Semaphore(0);
        private final Semaphore onDisconnectedSemaphore = new Semaphore(0);
        private final Semaphore onErrorSemaphore = new Semaphore(0);

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            onOpenedSemaphore.release();
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            onClosedSemaphore.release();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            onDisconnectedSemaphore.release();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            onErrorSemaphore.release();
        }

        void waitForOnOpened(int count) throws InterruptedException {
            onOpenedSemaphore.acquire(count);
        }

        void waitForOnClosed(int count) throws InterruptedException {
            onClosedSemaphore.acquire(count);
        }

        void waitForOnDisconnected(int count) throws InterruptedException {
            onDisconnectedSemaphore.acquire(count);
        }

        void waitForOnError(int count) throws InterruptedException {
            onErrorSemaphore.acquire(count);
        }
    }

    /** A session state callback which releases a different semaphore for each method. */
    static final class SessionStateCallback extends CameraCaptureSession.StateCallback {
        private static final String TAG = SessionStateCallback.class.getSimpleName();

        private final Semaphore onConfiguredSemaphore = new Semaphore(0);
        private final Semaphore onActiveSemaphore = new Semaphore(0);
        private final Semaphore onClosedSemaphore = new Semaphore(0);
        private final Semaphore onReadySemaphore = new Semaphore(0);
        private final Semaphore onCaptureQueueEmptySemaphore = new Semaphore(0);
        private final Semaphore onSurfacePreparedSemaphore = new Semaphore(0);
        private final Semaphore onConfigureFailedSemaphore = new Semaphore(0);

        @Override
        public void onConfigured(CameraCaptureSession session) {
            onConfiguredSemaphore.release();
        }

        @Override
        public void onActive(CameraCaptureSession session) {
            onActiveSemaphore.release();
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            onClosedSemaphore.release();
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            onReadySemaphore.release();
        }

        @Override
        public void onCaptureQueueEmpty(CameraCaptureSession session) {
            onCaptureQueueEmptySemaphore.release();
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            onSurfacePreparedSemaphore.release();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            onConfigureFailedSemaphore.release();
        }

        void waitForOnConfigured(int count) throws InterruptedException {
            onConfiguredSemaphore.acquire(count);
        }

        void waitForOnActive(int count) throws InterruptedException {
            onActiveSemaphore.acquire(count);
        }

        void waitForOnClosed(int count) throws InterruptedException {
            onClosedSemaphore.acquire(count);
        }

        void waitForOnReady(int count) throws InterruptedException {
            onReadySemaphore.acquire(count);
        }

        void waitForOnCaptureQueueEmpty(int count) throws InterruptedException {
            onCaptureQueueEmptySemaphore.acquire(count);
        }

        void waitForOnSurfacePrepared(int count) throws InterruptedException {
            onSurfacePreparedSemaphore.acquire(count);
        }

        void waitForOnConfigureFailed(int count) throws InterruptedException {
            onConfigureFailedSemaphore.acquire(count);
        }
    }

    /** A session capture callback which releases a different semaphore for each method. */
    static final class SessionCaptureCallback extends CameraCaptureSession.CaptureCallback {
        private static final String TAG = SessionCaptureCallback.class.getSimpleName();

        private final Semaphore onCaptureBufferLostSemaphore = new Semaphore(0);
        private final Semaphore onCaptureCompletedSemaphore = new Semaphore(0);
        private final Semaphore onCaptureFailedSemaphore = new Semaphore(0);
        private final Semaphore onCaptureProgressedSemaphore = new Semaphore(0);
        private final Semaphore onCaptureSequenceAbortedSemaphore = new Semaphore(0);
        private final Semaphore onCaptureSequenceCompletedSemaphore = new Semaphore(0);
        private final Semaphore onCaptureStartedSemaphore = new Semaphore(0);

        @Override
        public void onCaptureBufferLost(
                CameraCaptureSession session, CaptureRequest request, Surface surface, long frame) {
            onCaptureBufferLostSemaphore.release();
        }

        @Override
        public void onCaptureCompleted(
                CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            onCaptureCompletedSemaphore.release();
        }

        @Override
        public void onCaptureFailed(
                CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            onCaptureFailedSemaphore.release();
        }

        @Override
        public void onCaptureProgressed(
                CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            onCaptureProgressedSemaphore.release();
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            onCaptureSequenceAbortedSemaphore.release();
        }

        @Override
        public void onCaptureSequenceCompleted(
                CameraCaptureSession session, int sequenceId, long frame) {
            onCaptureSequenceCompletedSemaphore.release();
        }

        @Override
        public void onCaptureStarted(
                CameraCaptureSession session, CaptureRequest request, long timestamp, long frame) {
            onCaptureStartedSemaphore.release();
        }

        void waitForOnCaptureBufferLost(int count) throws InterruptedException {
            onCaptureBufferLostSemaphore.acquire(count);
        }

        void waitForOnCaptureCompleted(int count) throws InterruptedException {
            onCaptureCompletedSemaphore.acquire(count);
        }

        void waitForOnCaptureFailed(int count) throws InterruptedException {
            onCaptureFailedSemaphore.acquire(count);
        }

        void waitForOnCaptureProgressed(int count) throws InterruptedException {
            onCaptureProgressedSemaphore.acquire(count);
        }

        void waitForOnCaptureSequenceAborted(int count) throws InterruptedException {
            onCaptureSequenceAbortedSemaphore.acquire(count);
        }

        void waitForOnCaptureSequenceCompleted(int count) throws InterruptedException {
            onCaptureSequenceCompletedSemaphore.acquire(count);
        }

        void waitForOnCaptureStarted(int count) throws InterruptedException {
            onCaptureStartedSemaphore.acquire(count);
        }
    }
}
