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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;
import android.view.Surface;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class SessionConfigurationCompatTest {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;
    private static final int FORMAT = ImageFormat.YUV_420_888;
    private static final int DEFAULT_SESSION_TYPE = SessionConfigurationCompat.SESSION_REGULAR;

    private List<OutputConfigurationCompat> mOutputs;
    private Executor mCallbackExecutor;
    private CameraCaptureSession.StateCallback mStateCallback;

    @Before
    public void setUp() {
        mOutputs = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            Surface surface = mock(Surface.class);
            OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);
            mOutputs.add(outputConfigCompat);
        }

        mCallbackExecutor = mock(Executor.class);

        mStateCallback = mock(CameraCaptureSession.StateCallback.class);
    }

    private SessionConfigurationCompat createDefaultSessionConfig() {
        return new SessionConfigurationCompat(
                DEFAULT_SESSION_TYPE,
                mOutputs,
                mCallbackExecutor,
                mStateCallback);
    }

    @Test
    public void canCreateSessionConfiguration() {
        SessionConfigurationCompat sessionConfigCompat = createDefaultSessionConfig();

        assertThat(sessionConfigCompat.getSessionType()).isEqualTo(DEFAULT_SESSION_TYPE);
        assertThat(sessionConfigCompat.getOutputConfigurations()).containsExactlyElementsIn(
                mOutputs);
        assertThat(sessionConfigCompat.getExecutor()).isSameInstanceAs(mCallbackExecutor);
        assertThat(sessionConfigCompat.getStateCallback()).isSameInstanceAs(mStateCallback);
    }

    @Test
    @Config(minSdk = 28)
    public void canWrapAndUnwrapSessionConfiguration() {
        List<OutputConfiguration> outputConfigs = new ArrayList<>(mOutputs.size());
        for (OutputConfigurationCompat outputConfigCompat : mOutputs) {
            outputConfigs.add((OutputConfiguration) outputConfigCompat.unwrap());
        }

        SessionConfiguration sessionConfig = new SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigs,
                mCallbackExecutor,
                mStateCallback);

        SessionConfigurationCompat sessionConfigCompat = SessionConfigurationCompat.wrap(
                sessionConfig);

        assertThat(sessionConfigCompat.getSessionType()).isEqualTo(
                SessionConfigurationCompat.SESSION_REGULAR);
        assertThat(sessionConfigCompat.getOutputConfigurations()).containsExactlyElementsIn(
                mOutputs);
        assertThat(sessionConfigCompat.getExecutor()).isSameInstanceAs(mCallbackExecutor);
        assertThat(sessionConfigCompat.getStateCallback()).isSameInstanceAs(mStateCallback);

        assertThat(sessionConfigCompat.unwrap()).isSameInstanceAs(sessionConfig);
        assertThat(
                ((SessionConfiguration) sessionConfigCompat.unwrap()).getOutputConfigurations())
                .containsExactlyElementsIn(
                outputConfigs);
    }

    @Test
    public void canSetAndRetrieveInputConfiguration() {
        SessionConfigurationCompat sessionConfigCompat = createDefaultSessionConfig();

        InputConfigurationCompat inputConfigurationCompat = new InputConfigurationCompat(WIDTH,
                HEIGHT, FORMAT);

        sessionConfigCompat.setInputConfiguration(inputConfigurationCompat);

        // getInputConfiguration() is not necessarily the same instance, but should be equivalent
        // by comparison
        assertThat(sessionConfigCompat.getInputConfiguration()).isEqualTo(inputConfigurationCompat);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void cannotSetInputConfiguration_onHighSpeedSession() {
        SessionConfigurationCompat sessionConfigCompat = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_HIGH_SPEED,
                mOutputs,
                mCallbackExecutor,
                mStateCallback);

        InputConfigurationCompat inputConfigurationCompat = new InputConfigurationCompat(WIDTH,
                HEIGHT, FORMAT);

        sessionConfigCompat.setInputConfiguration(inputConfigurationCompat);
    }

    @Test
    @Config(minSdk = 28)
    public void constantsMatchNonCompatVersion() {
        assertThat(SessionConfigurationCompat.SESSION_REGULAR).isEqualTo(
                SessionConfiguration.SESSION_REGULAR);
        assertThat(SessionConfigurationCompat.SESSION_HIGH_SPEED).isEqualTo(
                SessionConfiguration.SESSION_HIGH_SPEED);
    }
}
