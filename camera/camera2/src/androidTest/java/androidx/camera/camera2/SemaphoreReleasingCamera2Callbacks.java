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

        private final Semaphore mOnOpenedSemaphore = new Semaphore(0);
        private final Semaphore mOnClosedSemaphore = new Semaphore(0);
        private final Semaphore mOnDisconnectedSemaphore = new Semaphore(0);
        private final Semaphore mOnErrorSemaphore = new Semaphore(0);

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mOnOpenedSemaphore.release();
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            mOnClosedSemaphore.release();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mOnDisconnectedSemaphore.release();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mOnErrorSemaphore.release();
        }

        void waitForOnOpened(int count) throws InterruptedException {
            mOnOpenedSemaphore.acquire(count);
        }

        void waitForOnClosed(int count) throws InterruptedException {
            mOnClosedSemaphore.acquire(count);
        }

        void waitForOnDisconnected(int count) throws InterruptedException {
            mOnDisconnectedSemaphore.acquire(count);
        }

        void waitForOnError(int count) throws InterruptedException {
            mOnErrorSemaphore.acquire(count);
        }
    }

    /** A session state callback which releases a different semaphore for each method. */
    static final class SessionStateCallback extends CameraCaptureSession.StateCallback {
        private static final String TAG = SessionStateCallback.class.getSimpleName();

        private final Semaphore mOnConfiguredSemaphore = new Semaphore(0);
        private final Semaphore mOnActiveSemaphore = new Semaphore(0);
        private final Semaphore mOnClosedSemaphore = new Semaphore(0);
        private final Semaphore mOnReadySemaphore = new Semaphore(0);
        private final Semaphore mOnCaptureQueueEmptySemaphore = new Semaphore(0);
        private final Semaphore mOnSurfacePreparedSemaphore = new Semaphore(0);
        private final Semaphore mOnConfigureFailedSemaphore = new Semaphore(0);

        @Override
        public void onConfigured(CameraCaptureSession session) {
            mOnConfiguredSemaphore.release();
        }

        @Override
        public void onActive(CameraCaptureSession session) {
            mOnActiveSemaphore.release();
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            mOnClosedSemaphore.release();
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            mOnReadySemaphore.release();
        }

        @Override
        public void onCaptureQueueEmpty(CameraCaptureSession session) {
            mOnCaptureQueueEmptySemaphore.release();
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            mOnSurfacePreparedSemaphore.release();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            mOnConfigureFailedSemaphore.release();
        }

        void waitForOnConfigured(int count) throws InterruptedException {
            mOnConfiguredSemaphore.acquire(count);
        }

        void waitForOnActive(int count) throws InterruptedException {
            mOnActiveSemaphore.acquire(count);
        }

        void waitForOnClosed(int count) throws InterruptedException {
            mOnClosedSemaphore.acquire(count);
        }

        void waitForOnReady(int count) throws InterruptedException {
            mOnReadySemaphore.acquire(count);
        }

        void waitForOnCaptureQueueEmpty(int count) throws InterruptedException {
            mOnCaptureQueueEmptySemaphore.acquire(count);
        }

        void waitForOnSurfacePrepared(int count) throws InterruptedException {
            mOnSurfacePreparedSemaphore.acquire(count);
        }

        void waitForOnConfigureFailed(int count) throws InterruptedException {
            mOnConfigureFailedSemaphore.acquire(count);
        }
    }

    /** A session capture callback which releases a different semaphore for each method. */
    static final class SessionCaptureCallback extends CameraCaptureSession.CaptureCallback {
        private static final String TAG = SessionCaptureCallback.class.getSimpleName();

        private final Semaphore mOnCaptureBufferLostSemaphore = new Semaphore(0);
        private final Semaphore mOnCaptureCompletedSemaphore = new Semaphore(0);
        private final Semaphore mOnCaptureFailedSemaphore = new Semaphore(0);
        private final Semaphore mOnCaptureProgressedSemaphore = new Semaphore(0);
        private final Semaphore mOnCaptureSequenceAbortedSemaphore = new Semaphore(0);
        private final Semaphore mOnCaptureSequenceCompletedSemaphore = new Semaphore(0);
        private final Semaphore mOnCaptureStartedSemaphore = new Semaphore(0);

        @Override
        public void onCaptureBufferLost(
                CameraCaptureSession session, CaptureRequest request, Surface surface, long frame) {
            mOnCaptureBufferLostSemaphore.release();
        }

        @Override
        public void onCaptureCompleted(
                CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            mOnCaptureCompletedSemaphore.release();
        }

        @Override
        public void onCaptureFailed(
                CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            mOnCaptureFailedSemaphore.release();
        }

        @Override
        public void onCaptureProgressed(
                CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            mOnCaptureProgressedSemaphore.release();
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            mOnCaptureSequenceAbortedSemaphore.release();
        }

        @Override
        public void onCaptureSequenceCompleted(
                CameraCaptureSession session, int sequenceId, long frame) {
            mOnCaptureSequenceCompletedSemaphore.release();
        }

        @Override
        public void onCaptureStarted(
                CameraCaptureSession session, CaptureRequest request, long timestamp, long frame) {
            mOnCaptureStartedSemaphore.release();
        }

        void waitForOnCaptureBufferLost(int count) throws InterruptedException {
            mOnCaptureBufferLostSemaphore.acquire(count);
        }

        void waitForOnCaptureCompleted(int count) throws InterruptedException {
            mOnCaptureCompletedSemaphore.acquire(count);
        }

        void waitForOnCaptureFailed(int count) throws InterruptedException {
            mOnCaptureFailedSemaphore.acquire(count);
        }

        void waitForOnCaptureProgressed(int count) throws InterruptedException {
            mOnCaptureProgressedSemaphore.acquire(count);
        }

        void waitForOnCaptureSequenceAborted(int count) throws InterruptedException {
            mOnCaptureSequenceAbortedSemaphore.acquire(count);
        }

        void waitForOnCaptureSequenceCompleted(int count) throws InterruptedException {
            mOnCaptureSequenceCompletedSemaphore.acquire(count);
        }

        void waitForOnCaptureStarted(int count) throws InterruptedException {
            mOnCaptureStartedSemaphore.acquire(count);
        }
    }
}
