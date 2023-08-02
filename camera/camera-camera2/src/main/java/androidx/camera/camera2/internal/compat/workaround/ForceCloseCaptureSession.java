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

package androidx.camera.camera2.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.SynchronizedCaptureSession;
import androidx.camera.camera2.internal.compat.quirk.CaptureSessionOnClosedNotCalledQuirk;
import androidx.camera.core.impl.Quirks;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The workaround is used to set the {@link androidx.camera.camera2.internal.CaptureSession} to
 * closed state since the
 * {@link android.hardware.camera2.CameraCaptureSession.StateCallback#onClosed} may not be called
 * after the {@link android.hardware.camera2.CameraCaptureSession} is closed.
 *
 * @see CaptureSessionOnClosedNotCalledQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ForceCloseCaptureSession {

    @Nullable
    private final CaptureSessionOnClosedNotCalledQuirk mCaptureSessionOnClosedNotCalledQuirk;

    /** Constructor of the ForceCloseCaptureSession workaround */
    public ForceCloseCaptureSession(@NonNull Quirks deviceQuirks) {
        mCaptureSessionOnClosedNotCalledQuirk =
                deviceQuirks.get(CaptureSessionOnClosedNotCalledQuirk.class);
    }

    /** Return true if the obsolete non-closed capture sessions should be forced closed. */
    public boolean shouldForceClose() {
        return mCaptureSessionOnClosedNotCalledQuirk != null;
    }

    /**
     * For b/144817309, the onClosed() callback on
     * {@link android.hardware.camera2.CameraCaptureSession.StateCallback}
     * might not be invoked if the capture session is not the latest one. To align the fixed
     * framework behavior, we manually call the onClosed() when a new CameraCaptureSession is
     * created.
     */
    public void onSessionConfigured(@NonNull SynchronizedCaptureSession session,
            @NonNull List<SynchronizedCaptureSession> creatingSessions,
            @NonNull List<SynchronizedCaptureSession> sessions,
            @NonNull OnConfigured onConfigured) {
        if (shouldForceClose()) {
            Set<SynchronizedCaptureSession> staleCreatingSessions = new LinkedHashSet<>();
            for (SynchronizedCaptureSession s : creatingSessions) {
                // Collect the sessions that started configuring before the current session. The
                // current session and the session that starts configure after the current session
                // are not included since they don't need to be closed.
                if (s == session) {
                    break;
                }
                staleCreatingSessions.add(s);
            }
            // Once the CaptureSession is configured, the stale CaptureSessions should not have
            // chance to complete the configuration flow. Force change to configure fail since
            // the configureFail will treat the CaptureSession is closed. More detail please see
            // b/158540776.
            forceOnConfigureFailed(staleCreatingSessions);
        }

        onConfigured.run(session);

        // Once the new CameraCaptureSession is created, all the previous opened
        // CameraCaptureSession can be treated as closed (more detail in b/144817309),
        // trigger its associated StateCallback#onClosed callback to finish the
        // session close flow.
        if (shouldForceClose()) {
            Set<SynchronizedCaptureSession> openedSessions = new LinkedHashSet<>();
            for (SynchronizedCaptureSession s : sessions) {

                // The entrySet keys of the LinkedHashMap should be insertion-ordered, so we
                // get the previous capture sessions by iterate it from the beginning.
                if (s == session) {
                    break;
                }
                openedSessions.add(s);
            }

            forceOnClosed(openedSessions);
        }
    }

    private void forceOnConfigureFailed(@NonNull Set<SynchronizedCaptureSession> sessions) {
        for (SynchronizedCaptureSession session : sessions) {
            session.getStateCallback().onConfigureFailed(session);
        }
    }

    private void forceOnClosed(@NonNull Set<SynchronizedCaptureSession> sessions) {
        for (SynchronizedCaptureSession session : sessions) {
            session.getStateCallback().onClosed(session);
        }
    }

    /** Interface to forward call of the onConfigured() method. */
    @FunctionalInterface
    public interface OnConfigured {
        /** Run the onConfigured() method. */
        void run(@NonNull SynchronizedCaptureSession session);
    }
}
