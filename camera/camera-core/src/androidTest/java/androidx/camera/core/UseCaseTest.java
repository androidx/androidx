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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class UseCaseTest {
    private UseCase.StateChangeCallback mMockUseCaseCallback;

    @Before
    public void setup() {
        mMockUseCaseCallback = mock(UseCase.StateChangeCallback.class);
    }

    @Test
    public void getAttachedSessionConfig() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        SessionConfig sessionToAttach = new SessionConfig.Builder().build();
        testUseCase.attachToCamera(sessionToAttach);

        SessionConfig attachedSession = testUseCase.getSessionConfig();

        assertThat(attachedSession).isEqualTo(sessionToAttach);
    }

    @Test
    public void removeListener() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeCallback(mMockUseCaseCallback);
        testUseCase.removeStateChangeCallback(mMockUseCaseCallback);

        testUseCase.activate();

        verify(mMockUseCaseCallback, never()).onUseCaseActive(any(UseCase.class));
    }

    @Test
    public void clearListeners() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeCallback(mMockUseCaseCallback);
        testUseCase.clear();

        testUseCase.activate();
        verify(mMockUseCaseCallback, never()).onUseCaseActive(any(UseCase.class));
    }

    @Test
    public void notifyActiveState() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeCallback(mMockUseCaseCallback);

        testUseCase.activate();
        verify(mMockUseCaseCallback, times(1)).onUseCaseActive(testUseCase);
    }

    @Test
    public void notifyInactiveState() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeCallback(mMockUseCaseCallback);

        testUseCase.deactivate();
        verify(mMockUseCaseCallback, times(1)).onUseCaseInactive(testUseCase);
    }

    @Test
    public void notifyUpdatedSettings() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeCallback(mMockUseCaseCallback);

        testUseCase.update();
        verify(mMockUseCaseCallback, times(1)).onUseCaseUpdated(testUseCase);
    }

    @Test
    public void notifyResetUseCase() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.addStateChangeCallback(mMockUseCaseCallback);

        testUseCase.notifyReset();
        verify(mMockUseCaseCallback, times(1)).onUseCaseReset(testUseCase);
    }

    @Test
    public void useCaseConfig_canBeUpdated() {
        String originalName = "UseCase";
        FakeUseCaseConfig.Builder configBuilder =
                new FakeUseCaseConfig.Builder().setTargetName(originalName);

        TestUseCase testUseCase = new TestUseCase(configBuilder.getUseCaseConfig());
        String originalRetrievedName = testUseCase.getUseCaseConfig().getTargetName();

        // NOTE: Updating the use case name is probably a very bad idea in most cases. However,
        // we'll do it here for the sake of this test.
        String newName = "UseCase-New";
        configBuilder.setTargetName(newName);
        testUseCase.updateUseCaseConfig(configBuilder.getUseCaseConfig());
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
        @NonNull
        protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
            return suggestedResolution;
        }
    }
}
