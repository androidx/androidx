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

package androidx.camera.camera2.impl.compat.params;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.impl.compat.CameraDeviceCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Helper for accessing features in SessionConfiguration in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class SessionConfigurationCompat {

    /**
     * A regular session type containing instances of {@link OutputConfigurationCompat} running
     * at regular non high speed FPS ranges and optionally {@link InputConfigurationCompat} for
     * reprocessable sessions.
     *
     * @see CameraDevice#createCaptureSession
     * @see CameraDevice#createReprocessableCaptureSession
     */
    public static final int SESSION_REGULAR = CameraDeviceCompat.SESSION_OPERATION_MODE_NORMAL;
    /**
     * A high speed session type that can only contain instances of
     * {@link OutputConfigurationCompat}.
     * The outputs can run using high speed FPS ranges. Calls to {@link #setInputConfiguration}
     * are not supported.
     *
     * @see CameraDevice#createConstrainedHighSpeedCaptureSession
     */
    public static final int SESSION_HIGH_SPEED =
            CameraDeviceCompat.SESSION_OPERATION_MODE_CONSTRAINED_HIGH_SPEED;
    private final SessionConfigurationCompatImpl mImpl;

    /**
     * Create a new {@link SessionConfigurationCompat}.
     *
     * @param sessionType   The session type.
     * @param outputsCompat A list of output configurations for the capture session.
     * @param executor      The executor which should be used to invoke the callback. In general
     *                      it is
     *                      recommended that camera operations are not done on the main (UI) thread.
     * @param cb            A state callback interface implementation.
     * @see #SESSION_REGULAR
     * @see #SESSION_HIGH_SPEED
     */
    public SessionConfigurationCompat(@SessionMode int sessionType,
            @NonNull List<OutputConfigurationCompat> outputsCompat,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.StateCallback cb) {
        if (Build.VERSION.SDK_INT < 28) {
            mImpl = new SessionConfigurationCompatBaseImpl(sessionType, outputsCompat, executor,
                    cb);
        } else {
            mImpl = new SessionConfigurationCompatApi28Impl(sessionType, outputsCompat, executor,
                    cb);
        }
    }

    private SessionConfigurationCompat(@NonNull SessionConfigurationCompatImpl impl) {
        mImpl = impl;
    }

    /**
     * Creates an instance from a framework android.hardware.camera2.params.SessionConfiguration
     * object.
     *
     * <p>This method always returns {@code null} on API &lt;= 27.</p>
     *
     * @param sessionConfiguration an android.hardware.camera2.params.SessionConfiguration object,
     *                             or {@code null} if none.
     * @return an equivalent {@link SessionConfigurationCompat} object, or {@code null} if not
     * supported.
     */
    @Nullable
    public static SessionConfigurationCompat wrap(@Nullable Object sessionConfiguration) {
        if (sessionConfiguration == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT < 28) {
            return null;
        }

        return new SessionConfigurationCompat(
                new SessionConfigurationCompatApi28Impl(sessionConfiguration));
    }

    @RequiresApi(24)
    static List<OutputConfigurationCompat> transformToCompat(
            @NonNull List<OutputConfiguration> outputConfigurations) {
        ArrayList<OutputConfigurationCompat> outList = new ArrayList<>(outputConfigurations.size());
        for (OutputConfiguration outputConfiguration : outputConfigurations) {
            outList.add(OutputConfigurationCompat.wrap(outputConfiguration));
        }

        return outList;
    }

    /** @hide */
    @RequiresApi(24)
    @RestrictTo(Scope.LIBRARY)
    public static List<OutputConfiguration> transformFromCompat(
            @NonNull List<OutputConfigurationCompat> outputConfigurations) {
        ArrayList<OutputConfiguration> outList = new ArrayList<>(outputConfigurations.size());
        for (OutputConfigurationCompat outputConfiguration : outputConfigurations) {
            outList.add((OutputConfiguration) outputConfiguration.unwrap());
        }

        return outList;
    }

    /**
     * Retrieve the type of the capture session.
     *
     * @return The capture session type.
     */
    @SessionMode
    public int getSessionType() {
        return mImpl.getSessionType();
    }

    /**
     * Retrieve the {@link OutputConfigurationCompat} list for the capture session.
     *
     * @return A list of output configurations for the capture session.
     */
    public List<OutputConfigurationCompat> getOutputConfigurations() {
        return mImpl.getOutputConfigurations();
    }

    /**
     * Retrieve the {@link CameraCaptureSession.StateCallback} for the capture session.
     *
     * @return A state callback interface implementation.
     */
    public CameraCaptureSession.StateCallback getStateCallback() {
        return mImpl.getStateCallback();
    }

    /**
     * Retrieve the {@link Executor} for the capture session.
     *
     * @return The Executor on which the callback will be invoked.
     */
    public Executor getExecutor() {
        return mImpl.getExecutor();
    }

    /**
     * Retrieve the {@link InputConfigurationCompat}.
     *
     * @return The capture session input configuration.
     */
    public InputConfigurationCompat getInputConfiguration() {
        return mImpl.getInputConfiguration();
    }

    /**
     * Sets the {@link InputConfigurationCompat} for a reprocessable session. Input configuration
     * are not supported for {@link #SESSION_HIGH_SPEED}.
     *
     * @param input Input configuration.
     * @throws UnsupportedOperationException In case it is called for {@link #SESSION_HIGH_SPEED}
     *                                       type session configuration.
     */
    public void setInputConfiguration(@NonNull InputConfigurationCompat input) {
        mImpl.setInputConfiguration(input);
    }

    /**
     * Retrieve the session wide camera parameters (see {@link CaptureRequest}).
     *
     * @return A capture request that includes the initial values for any available
     * session wide capture keys.
     */
    public CaptureRequest getSessionParameters() {
        return mImpl.getSessionParameters();
    }

    /**
     * Sets the session wide camera parameters (see {@link CaptureRequest}). This argument can
     * be set for every supported session type and will be passed to the camera device as part
     * of the capture session initialization. Session parameters are a subset of the available
     * capture request parameters (see {@code CameraCharacteristics.getAvailableSessionKeys})
     * and their application can introduce internal camera delays. To improve camera performance
     * it is suggested to change them sparingly within the lifetime of the capture session and
     * to pass their initial values as part of this method.
     *
     * @param params A capture request that includes the initial values for any available
     *               session wide capture keys. Tags (see {@link CaptureRequest.Builder#setTag}) and
     *               output targets (see {@link CaptureRequest.Builder#addTarget}) are ignored if
     *               set. Parameter values not part of
     *               {@code CameraCharacteristics.getAvailableSessionKeys} will also be ignored. It
     *               is recommended to build the session parameters using the same template type as
     *               the initial capture request, so that the session and initial request parameters
     *               match as much as possible.
     */
    public void setSessionParameters(CaptureRequest params) {
        mImpl.setSessionParameters(params);
    }

    /**
     * Gets the underlying framework android.hardware.camera2.params.SessionConfiguration object.
     *
     * <p>This method always returns {@code null} on API &lt;= 27.</p>
     *
     * @return an equivalent android.hardware.camera2.params.SessionConfiguration object, or
     * {@code null} if not supported.
     */
    @Nullable
    public Object unwrap() {
        return mImpl.getSessionConfiguration();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SessionConfigurationCompat)) {
            return false;
        }

        return mImpl.equals(((SessionConfigurationCompat) obj).mImpl);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value =
            {SESSION_REGULAR, SESSION_HIGH_SPEED})
    public @interface SessionMode {
    }

    private interface SessionConfigurationCompatImpl {
        @SessionMode
        int getSessionType();

        List<OutputConfigurationCompat> getOutputConfigurations();

        CameraCaptureSession.StateCallback getStateCallback();

        Executor getExecutor();

        InputConfigurationCompat getInputConfiguration();

        void setInputConfiguration(@NonNull InputConfigurationCompat input);

        CaptureRequest getSessionParameters();

        void setSessionParameters(CaptureRequest params);

        @Nullable
        Object getSessionConfiguration();
    }

    private static final class SessionConfigurationCompatBaseImpl implements
            SessionConfigurationCompatImpl {

        private final List<OutputConfigurationCompat> mOutputConfigurations;
        private final CameraCaptureSession.StateCallback mStateCallback;
        private final Executor mExecutor;
        private int mSessionType;
        private InputConfigurationCompat mInputConfig = null;
        private CaptureRequest mSessionParameters = null;

        SessionConfigurationCompatBaseImpl(@SessionMode int sessionType,
                @NonNull List<OutputConfigurationCompat> outputs,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraCaptureSession.StateCallback cb) {
            mSessionType = sessionType;
            mOutputConfigurations = Collections.unmodifiableList(new ArrayList<>(outputs));
            mStateCallback = cb;
            mExecutor = executor;
        }

        @Override
        public int getSessionType() {
            return mSessionType;
        }

        @Override
        public List<OutputConfigurationCompat> getOutputConfigurations() {
            return mOutputConfigurations;
        }

        @Override
        public CameraCaptureSession.StateCallback getStateCallback() {
            return mStateCallback;
        }

        @Override
        public Executor getExecutor() {
            return mExecutor;
        }

        @Nullable
        @Override
        public InputConfigurationCompat getInputConfiguration() {
            return mInputConfig;
        }

        @Override
        public void setInputConfiguration(@NonNull InputConfigurationCompat input) {
            if (mSessionType != SESSION_HIGH_SPEED) {
                mInputConfig = input;
            } else {
                throw new UnsupportedOperationException(
                        "Method not supported for high speed session types");
            }
        }

        @Override
        public CaptureRequest getSessionParameters() {
            return mSessionParameters;
        }

        @Override
        public void setSessionParameters(CaptureRequest params) {
            mSessionParameters = params;
        }

        @Nullable
        @Override
        public Object getSessionConfiguration() {
            return null;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof SessionConfigurationCompatBaseImpl) {
                SessionConfigurationCompatBaseImpl other = (SessionConfigurationCompatBaseImpl) obj;
                if (mInputConfig != other.mInputConfig
                        || mSessionType != other.mSessionType
                        || mOutputConfigurations.size() != other.mOutputConfigurations.size()) {
                    return false;
                }

                for (int i = 0; i < mOutputConfigurations.size(); i++) {
                    if (!mOutputConfigurations.get(i).equals(other.mOutputConfigurations.get(i))) {
                        return false;
                    }
                }

                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            int h = 1;
            // Strength reduction; in case the compiler has illusions about divisions being faster
            h = ((h << 5) - h)
                    ^ mOutputConfigurations.hashCode(); // (h * 31) XOR mOutputConfigurations
            // .hashCode()
            h = ((h << 5) - h) ^ (mInputConfig == null ? 0
                    : mInputConfig.hashCode()); // (h * 31) XOR mInputConfig.hashCode()
            h = ((h << 5) - h) ^ mSessionType; // (h * 31) XOR mSessionType

            return h;
        }
    }

    @RequiresApi(28)
    private static final class SessionConfigurationCompatApi28Impl implements
            SessionConfigurationCompatImpl {

        private final SessionConfiguration mObject;
        private final List<OutputConfigurationCompat> mOutputConfigurations;

        SessionConfigurationCompatApi28Impl(@NonNull Object sessionConfiguration) {
            mObject = (SessionConfiguration) sessionConfiguration;
            mOutputConfigurations = Collections.unmodifiableList(transformToCompat(
                    ((SessionConfiguration) sessionConfiguration).getOutputConfigurations()));
        }

        SessionConfigurationCompatApi28Impl(@SessionMode int sessionType,
                @NonNull List<OutputConfigurationCompat> outputs,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraCaptureSession.StateCallback cb) {
            this(new SessionConfiguration(sessionType, transformFromCompat(outputs), executor, cb));
        }

        @Override
        public int getSessionType() {
            return mObject.getSessionType();
        }

        @Override
        public List<OutputConfigurationCompat> getOutputConfigurations() {
            // Return cached compat version of list
            return mOutputConfigurations;
        }

        @Override
        public CameraCaptureSession.StateCallback getStateCallback() {
            return mObject.getStateCallback();
        }

        @Override
        public Executor getExecutor() {
            return mObject.getExecutor();
        }

        @Override
        public InputConfigurationCompat getInputConfiguration() {
            return InputConfigurationCompat.wrap(mObject.getInputConfiguration());
        }

        @Override
        public void setInputConfiguration(@NonNull InputConfigurationCompat input) {
            mObject.setInputConfiguration((InputConfiguration) input.unwrap());
        }

        @Override
        public CaptureRequest getSessionParameters() {
            return mObject.getSessionParameters();
        }

        @Override
        public void setSessionParameters(CaptureRequest params) {
            mObject.setSessionParameters(params);
        }

        @Nullable
        @Override
        public Object getSessionConfiguration() {
            return mObject;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof SessionConfigurationCompatApi28Impl)) {
                return false;
            }

            return Objects.equals(mObject, ((SessionConfigurationCompatApi28Impl) obj).mObject);
        }

        @Override
        public int hashCode() {
            return mObject.hashCode();
        }
    }
}
