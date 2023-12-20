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

package androidx.camera.testing.fakes;


import static com.google.common.truth.Truth.assertThat;

import static java.util.Collections.singletonList;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Identifier;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.impl.fakes.FakeUseCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class FakeCameraTest {

    private FakeCamera mCamera;
    private CameraInternal.State mLatestState;
    private Observable.Observer<CameraInternal.State> mStateObserver =
            new Observable.Observer<CameraInternal.State>() {
                @Override
                public void onNewData(@Nullable CameraInternal.State value) {
                    mLatestState = value;
                }

                @Override
                public void onError(@NonNull Throwable t) {

                }
            };

    @Before
    public void setUp() {
        mCamera = new FakeCamera();
        mCamera.getCameraState().addObserver(CameraXExecutors.directExecutor(), mStateObserver);
        ShadowLooper.runUiThreadTasks();
    }

    @After
    public void tearDown() {
        mCamera.getCameraState().removeObserver(mStateObserver);
    }

    @Test
    public void cameraEntersOpenState_whenOpened() {
        mCamera.open();
        ShadowLooper.runUiThreadTasks();
        assertThat(mLatestState).isEqualTo(CameraInternal.State.OPEN);
    }

    @Test
    public void cameraIsInClosedState_whenInitialized() {
        assertThat(mLatestState).isEqualTo(CameraInternal.State.CLOSED);
    }

    @Test
    public void cameraEntersPendingState_whenOpened_withZeroCamerasAvailable() {
        mCamera.setAvailableCameraCount(0);
        mCamera.open();
        ShadowLooper.runUiThreadTasks();
        assertThat(mLatestState).isEqualTo(CameraInternal.State.PENDING_OPEN);
    }

    @Test
    public void cameraEntersOpenState_whenCameraBecomesAvailable() {
        mCamera.setAvailableCameraCount(0);
        mCamera.open();
        ShadowLooper.runUiThreadTasks();
        CameraInternal.State intermediateState = mLatestState;
        mCamera.setAvailableCameraCount(1);
        ShadowLooper.runUiThreadTasks();

        assertThat(intermediateState).isEqualTo(CameraInternal.State.PENDING_OPEN);
        assertThat(mLatestState).isEqualTo(CameraInternal.State.OPEN);
    }

    @Test
    public void cameraCanBeClosed_afterOpened() {
        mCamera.open();
        ShadowLooper.runUiThreadTasks();
        CameraInternal.State intermediateState = mLatestState;
        mCamera.close();
        ShadowLooper.runUiThreadTasks();

        assertThat(intermediateState).isEqualTo(CameraInternal.State.OPEN);
        assertThat(mLatestState).isEqualTo(CameraInternal.State.CLOSED);
    }

    @Test
    public void cameraCanBeClosed_fromPendingState() {
        mCamera.setAvailableCameraCount(0);
        mCamera.open();
        ShadowLooper.runUiThreadTasks();
        CameraInternal.State intermediateState = mLatestState;
        mCamera.close();
        ShadowLooper.runUiThreadTasks();

        assertThat(intermediateState).isEqualTo(CameraInternal.State.PENDING_OPEN);
        assertThat(mLatestState).isEqualTo(CameraInternal.State.CLOSED);
    }

    @Test
    public void canSetAndRetrieveAvailableCameraCount() {
        mCamera.setAvailableCameraCount(400);
        assertThat(mCamera.getAvailableCameraCount()).isEqualTo(400);
    }

    @Test
    public void canAttachUseCase() {
        mCamera.open();

        UseCase useCase = new FakeUseCase();
        mCamera.attachUseCases(singletonList(useCase));

        assertThat(mCamera.getAttachedUseCases()).containsExactly(useCase);
    }

    @Test
    public void canDetachUseCase() {
        mCamera.open();
        UseCase useCase = new FakeUseCase();
        mCamera.attachUseCases(singletonList(useCase));

        mCamera.detachUseCases(singletonList(useCase));

        assertThat(mCamera.getAttachedUseCases()).isEmpty();
    }

    @Test
    public void canProvideUseCaseActiveHistory() {
        mCamera.open();

        UseCase useCase1 = new FakeUseCase();
        UseCase useCase2 = new FakeUseCase();
        mCamera.onUseCaseActive(useCase1);
        mCamera.onUseCaseActive(useCase2);

        assertThat(mCamera.getUseCaseActiveHistory()).containsExactly(useCase1, useCase2).inOrder();
    }

    @Test
    public void canProvideUseCaseInactiveHistory() {
        mCamera.open();

        UseCase useCase1 = new FakeUseCase();
        UseCase useCase2 = new FakeUseCase();
        mCamera.onUseCaseInactive(useCase1);
        mCamera.onUseCaseInactive(useCase2);

        assertThat(mCamera.getUseCaseInactiveHistory()).containsExactly(useCase1,
                useCase2).inOrder();
    }

    @Test
    public void canProvideUseCaseUpdateHistory() {
        mCamera.open();

        UseCase useCase1 = new FakeUseCase();
        UseCase useCase2 = new FakeUseCase();
        mCamera.onUseCaseUpdated(useCase1);
        mCamera.onUseCaseUpdated(useCase2);

        assertThat(mCamera.getUseCaseUpdateHistory()).containsExactly(useCase1, useCase2).inOrder();
    }

    @Test
    public void canProvideUseCaseResetHistory() {
        mCamera.open();

        UseCase useCase1 = new FakeUseCase();
        UseCase useCase2 = new FakeUseCase();
        mCamera.onUseCaseReset(useCase1);
        mCamera.onUseCaseReset(useCase2);

        assertThat(mCamera.getUseCaseResetHistory()).containsExactly(useCase1, useCase2).inOrder();
    }

    @Test
    public void hasTransformByDefault() {
        assertThat(mCamera.getHasTransform()).isTrue();
    }

    @Test
    public void canDisableHasTransform() {
        mCamera.setHasTransform(false);
        assertThat(mCamera.getHasTransform()).isFalse();
    }

    @Test
    public void canUpdateExtendedConfig() {
        CameraConfig config = new CameraConfig() {
            @NonNull
            @Override
            public androidx.camera.core.impl.Config getConfig() {
                return MutableOptionsBundle.create();
            }

            @NonNull
            @Override
            public Identifier getCompatibilityId() {
                return Identifier.create(0);
            }
        };
        mCamera.setExtendedConfig(config);

        assertThat(mCamera.getExtendedConfig()).isSameInstanceAs(config);
    }
}
