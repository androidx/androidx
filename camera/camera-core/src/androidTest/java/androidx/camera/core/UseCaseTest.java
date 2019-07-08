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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.util.Size;

import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class UseCaseTest {
    private UseCase.StateChangeListener mMockUseCaseListener;

    @Before
    public void setup() {
        mMockUseCaseListener = Mockito.mock(UseCase.StateChangeListener.class);
    }

    @Test
    public void getAttachedCamera() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        SessionConfig sessionToAttach = new SessionConfig.Builder().build();
        testUseCase.attachToCamera("Camera", sessionToAttach);

        Set<String> attachedCameras = testUseCase.getAttachedCameraIds();

        assertThat(attachedCameras).contains("Camera");
    }

    @Test
    public void getAttachedSessionConfig() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        SessionConfig sessionToAttach = new SessionConfig.Builder().build();
        testUseCase.attachToCamera("Camera", sessionToAttach);

        SessionConfig attachedSession = testUseCase.getSessionConfig("Camera");

        assertThat(attachedSession).isEqualTo(sessionToAttach);
    }

    @Test
    public void removeListener() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeListener(mMockUseCaseListener);
        testUseCase.removeStateChangeListener(mMockUseCaseListener);

        testUseCase.activate();

        verify(mMockUseCaseListener, never()).onUseCaseActive(any(UseCase.class));
    }

    @Test
    public void clearListeners() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeListener(mMockUseCaseListener);
        testUseCase.clear();

        testUseCase.activate();
        verify(mMockUseCaseListener, never()).onUseCaseActive(any(UseCase.class));
    }

    @Test
    public void notifyActiveState() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeListener(mMockUseCaseListener);

        testUseCase.activate();
        verify(mMockUseCaseListener, times(1)).onUseCaseActive(testUseCase);
    }

    @Test
    public void notifyInactiveState() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeListener(mMockUseCaseListener);

        testUseCase.deactivate();
        verify(mMockUseCaseListener, times(1)).onUseCaseInactive(testUseCase);
    }

    @Test
    public void notifyUpdatedSettings() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeListener(mMockUseCaseListener);

        testUseCase.update();
        verify(mMockUseCaseListener, times(1)).onUseCaseUpdated(testUseCase);
    }

    @Test
    public void notifyResetUseCase() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName("UseCase").build();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeListener(mMockUseCaseListener);

        testUseCase.notifyReset();
        verify(mMockUseCaseListener, times(1)).onUseCaseReset(testUseCase);
    }

    @Test
    public void useCaseConfig_canBeUpdated() {
        String originalName = "UseCase";
        FakeUseCaseConfig.Builder configBuilder =
                new FakeUseCaseConfig.Builder().setTargetName(originalName);

        TestUseCase testUseCase = new TestUseCase(configBuilder.build());
        String originalRetrievedName = testUseCase.getUseCaseConfig().getTargetName();

        // NOTE: Updating the use case name is probably a very bad idea in most cases. However,
        // we'll do
        // it here for the sake of this test.
        String newName = "UseCase-New";
        configBuilder.setTargetName(newName);
        testUseCase.updateUseCaseConfig(configBuilder.build());
        String newRetrievedName = testUseCase.getUseCaseConfig().getTargetName();

        assertThat(originalRetrievedName).isEqualTo(originalName);
        assertThat(newRetrievedName).isEqualTo(newName);
    }

    static class TestUseCase extends FakeUseCase {
        TestUseCase(FakeUseCaseConfig config) {
            super(config);
        }

        void activate() {
            notifyActive();
        }

        void deactivate() {
            notifyInactive();
        }

        void update() {
            notifyUpdated();
        }

        @Override
        protected void updateUseCaseConfig(UseCaseConfig<?> useCaseConfig) {
            super.updateUseCaseConfig(useCaseConfig);
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            return suggestedResolutionMap;
        }
    }
}
