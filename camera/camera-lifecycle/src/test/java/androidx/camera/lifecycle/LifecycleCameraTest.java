/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.lifecycle;

import static androidx.camera.core.featuregroup.GroupableFeature.FPS_60;
import static androidx.camera.core.featuregroup.GroupableFeature.HDR_HLG10;
import static androidx.camera.core.featuregroup.GroupableFeature.IMAGE_ULTRA_HDR;
import static androidx.camera.core.featuregroup.GroupableFeature.PREVIEW_STABILIZATION;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Build;
import android.util.Rational;
import android.view.Surface;

import androidx.camera.core.CameraEffect;
import androidx.camera.core.CameraUseCaseAdapterProvider;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.LegacySessionConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.featuregroup.GroupableFeature;
import androidx.camera.core.impl.AdapterCameraInfo;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.core.internal.StreamSpecsCalculatorImpl;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator;
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner;
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect;
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessor;
import androidx.camera.testing.impl.fakes.FakeUseCase;
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class LifecycleCameraTest {
    private LifecycleCamera mLifecycleCamera;
    private FakeLifecycleOwner mLifecycleOwner;
    private CameraCoordinator mCameraCoordinator;
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private FakeCamera mFakeCamera;
    private FakeUseCase mFakeUseCase;
    private FakeUseCase mFakeUseCase2;
    private ViewPort mViewPort;
    private CameraEffect mEffect;
    private final FakeSurfaceProcessor mFakeSurfaceProcessor = new FakeSurfaceProcessor(
            directExecutor());

    @Before
    public void setUp() {
        mLifecycleOwner = new FakeLifecycleOwner();
        mFakeCamera = new FakeCamera();
        mCameraCoordinator = new FakeCameraCoordinator();
        mCameraUseCaseAdapter = new CameraUseCaseAdapter(
                mFakeCamera,
                mCameraCoordinator,
                new StreamSpecsCalculatorImpl(new FakeUseCaseConfigFactory(),
                        new FakeCameraDeviceSurfaceManager()),
                new FakeUseCaseConfigFactory());

        ((FakeCameraInfoInternal) mFakeCamera.getCameraInfo()).setCameraUseCaseAdapterProvider(
                new CameraUseCaseAdapterProvider() {
                    @Override
                    public @NonNull CameraUseCaseAdapter provide(@NonNull String cameraId)
                            throws IllegalArgumentException {
                        return mCameraUseCaseAdapter;
                    }

                    @Override
                    public @NonNull CameraUseCaseAdapter provide(@NonNull CameraInternal camera,
                            @Nullable CameraInternal secondaryCamera,
                            @NonNull AdapterCameraInfo adapterCameraInfo,
                            @Nullable AdapterCameraInfo secondaryAdapterCameraInfo,
                            @NonNull CompositionSettings compositionSettings,
                            @NonNull CompositionSettings secondaryCompositionSettings) {
                        return mCameraUseCaseAdapter;
                    }
                }
        );


        mFakeUseCase = new FakeUseCase();
        mFakeUseCase2 = new FakeUseCase();
        mViewPort = new ViewPort.Builder(
                new Rational(4, 3), Surface.ROTATION_0).build();
        mEffect = new FakeSurfaceEffect(directExecutor(), mFakeSurfaceProcessor);
    }

    @After
    public void tearDown() {
        mFakeSurfaceProcessor.cleanUp();
    }

    @Test
    public void lifecycleCameraCanBeMadeObserverOfLifecycle() {
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);

        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);
    }

    @Test
    public void lifecycleCameraCanStopObservingALifecycle() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);

        mLifecycleCamera.release();

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);
    }

    @Test
    public void lifecycleCameraCanBeReleasedMultipleTimes() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleCamera.release();
        mLifecycleCamera.release();
    }

    @Test
    public void lifecycleStart_triggersOnActive() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        assertThat(mLifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void lifecycleStop_triggersOnInactive() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        mLifecycleOwner.stop();

        assertThat(mLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleStart_doesNotTriggerOnActiveIfSuspended() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleCamera.suspend();
        mLifecycleOwner.start();

        assertThat(mLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleStart_restoreInteropConfig() {
        FakeLifecycleOwner lifecycle1 = new FakeLifecycleOwner();
        FakeLifecycleOwner lifecycle2 = new FakeLifecycleOwner();

        CameraUseCaseAdapter adapter1 = new CameraUseCaseAdapter(
                mFakeCamera,
                mCameraCoordinator,
                new StreamSpecsCalculatorImpl(new FakeUseCaseConfigFactory(),
                        new FakeCameraDeviceSurfaceManager()),
                new FakeUseCaseConfigFactory());
        CameraUseCaseAdapter adapter2 = new CameraUseCaseAdapter(
                mFakeCamera,
                mCameraCoordinator,
                new StreamSpecsCalculatorImpl(new FakeUseCaseConfigFactory(),
                        new FakeCameraDeviceSurfaceManager()),
                new FakeUseCaseConfigFactory());
        LifecycleCamera lifecycleCamera1 = new LifecycleCamera(lifecycle1, adapter1);
        LifecycleCamera lifecycleCamera2 = new LifecycleCamera(lifecycle2, adapter2);

        // Set an config to CameraControl internally.
        Config.Option<Integer> option = Config.Option.create("OPTION_ID", Integer.class);
        Integer value = 1;
        MutableOptionsBundle originalConfig = MutableOptionsBundle.create();
        originalConfig.insertOption(option, value);
        mFakeCamera.getCameraControlInternal().addInteropConfig(originalConfig);

        lifecycle1.start();
        // Stop the lifecycle. The original config is cached and the config in CameraControl is
        // cleared internally.
        lifecycle1.stop();

        // Start the second lifecycle and set a different config.
        lifecycle2.start();
        MutableOptionsBundle newConfig = MutableOptionsBundle.create();
        newConfig.insertOption(Config.Option.create("OPTION_ID_2", Integer.class), 2);
        mFakeCamera.getCameraControlInternal().addInteropConfig(newConfig);
        lifecycle2.stop();

        // Starts the first lifecycle and the cached config is restored internally.
        lifecycle1.start();

        Config finalConfig = mFakeCamera.getCameraControlInternal().getInteropConfig();
        // Check the final config in CameraControl has the same value as the original config.
        assertThat(finalConfig.listOptions().containsAll(originalConfig.listOptions())).isTrue();
        assertThat(finalConfig.retrieveOption(option)).isEqualTo(value);
        // Check the final config doesn't contain the options set before it's attached again.
        assertThat(finalConfig.listOptions().containsAll(newConfig.listOptions())).isFalse();
    }

    @Test
    public void lifecycleStop_clearInteropConfig() {
        // Set an config to CameraControl.
        Config config = MutableOptionsBundle.create();
        mFakeCamera.getCameraControlInternal().addInteropConfig(config);

        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        // Stop the lifecycle. The original config is cached and the config in CameraControl is
        // cleared internally.
        mLifecycleOwner.stop();

        // Check the config in CameraControl is empty.
        assertThat(
                mFakeCamera.getCameraControlInternal().getInteropConfig().listOptions()).isEmpty();
    }

    @Test
    public void unsuspendOfStartedLifecycle_triggersOnActive() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleCamera.suspend();
        mLifecycleOwner.start();
        mLifecycleCamera.unsuspend();

        assertThat(mLifecycleCamera.isActive()).isTrue();
    }

    private LegacySessionConfig createLegacySessonConfig(UseCase... useCase) {
        return new LegacySessionConfig(Arrays.asList(useCase), null, Collections.emptyList());
    }

    private SessionConfig createSessionConfig(UseCase useCase) {
        return new SessionConfig.Builder(Collections.singletonList(useCase)).build();
    }

    @Test
    public void bindSessionConfig_willBindToCameraInternal()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig sessionConfig =
                new SessionConfig.Builder(Arrays.asList(mFakeUseCase))
                        .setViewPort(mViewPort)
                        .addEffect(mEffect)
                        .build();
        mLifecycleCamera.bind(sessionConfig);

        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getViewPort())
                .isEqualTo(sessionConfig.getViewPort());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getEffects())
                .isEqualTo(sessionConfig.getEffects());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getSessionType()).isEqualTo(
                sessionConfig.getSessionType());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getFrameRate())
                .isEqualTo(sessionConfig.getFrameRateRange());
    }

    @Test
    public void bindSessionConfig_withFeatures_featuresSetToAttachedUseCases() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();
        ((FakeCameraInfoInternal) mFakeCamera.getCameraInfo()).setSupportedDynamicRanges(
                Collections.singleton(DynamicRange.HLG_10_BIT));
        Preview preview = new Preview.Builder().build();

        SessionConfig sessionConfig =
                new SessionConfig.Builder(Collections.singletonList(preview))
                        .setRequiredFeatureGroup(HDR_HLG10)
                        .setPreferredFeatureGroup(FPS_60, PREVIEW_STABILIZATION)
                        .build();
        Threads.runOnMainSync(() -> {
            try {
                mLifecycleCamera.bind(sessionConfig);
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new RuntimeException(e);
            }
        });

        // All features are added since the fake surface manager supports all combinations.
        assertThat(preview.getFeatureGroup()).containsExactly(HDR_HLG10, FPS_60,
                PREVIEW_STABILIZATION);
    }

    @Test
    public void bindSessionConfig_withUnsupportedPreferredFeatures_correctFeaturesAttached() {
        // Arrange: Set up resources; HLG10, FPS_60, and PREVIEW_STABILIZATION are supported
        //   features while Ultra HDR is unsupported by the default behavior of fake surface manager
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();
        ((FakeCameraInfoInternal) mFakeCamera.getCameraInfo()).setSupportedDynamicRanges(
                Collections.singleton(DynamicRange.HLG_10_BIT));
        Preview preview = new Preview.Builder().build();

        SessionConfig sessionConfig =
                new SessionConfig.Builder(Collections.singletonList(preview))
                        .setRequiredFeatureGroup(HDR_HLG10)
                        .setPreferredFeatureGroup(FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR)
                        .build();

        // Act
        Threads.runOnMainSync(() -> {
            try {
                mLifecycleCamera.bind(sessionConfig);
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new RuntimeException(e);
            }
        });

        // Assert: Ultra HDR is not added while the other features are added.
        assertThat(preview.getFeatureGroup()).containsExactly(HDR_HLG10, FPS_60,
                PREVIEW_STABILIZATION);
    }

    @Test
    public void bindSessionConfig_withUnsupportedPreferredFeatures_correctFeaturesNotified()
            throws InterruptedException {
        // Arrange: Set up resources; HLG10, FPS_60, and PREVIEW_STABILIZATION are supported
        //   features while Ultra HDR is unsupported by the default behavior of fake surface manager
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();
        ((FakeCameraInfoInternal) mFakeCamera.getCameraInfo()).setSupportedDynamicRanges(
                Collections.singleton(DynamicRange.HLG_10_BIT));
        Preview preview = new Preview.Builder().build();

        SessionConfig sessionConfig =
                new SessionConfig.Builder(Collections.singletonList(preview))
                        .setRequiredFeatureGroup(HDR_HLG10)
                        .setPreferredFeatureGroup(FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR)
                        .build();

        CountDownLatch latch = new CountDownLatch(1);
        Set<GroupableFeature> selectedFeatures = new HashSet<>();

        sessionConfig.setFeatureSelectionListener(directExecutor(),
                features -> {
                    selectedFeatures.addAll(features);
                    latch.countDown();
                });

        // Act
        try {
            mLifecycleCamera.bind(sessionConfig);
        } catch (CameraUseCaseAdapter.CameraException e) {
            throw new RuntimeException(e);
        }

        // Assert: All the features except Ultra HDR are selected.
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(selectedFeatures).containsExactly(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION);
    }

    @Test
    public void bindSessionConfig_featureSelectionListenerSetButNoFeatureSet_emptySetNotified()
            throws InterruptedException {
        // Arrange: Set up resources; feature selection listener is set, but no required or
        // preferred feature is added.
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();
        Preview preview = new Preview.Builder().build();

        SessionConfig sessionConfig =
                new SessionConfig.Builder(Collections.singletonList(preview)).build();

        CountDownLatch latch = new CountDownLatch(1);
        Set<GroupableFeature> selectedFeatures = new HashSet<>();

        sessionConfig.setFeatureSelectionListener(directExecutor(),
                features -> {
                    selectedFeatures.addAll(features);
                    latch.countDown();
                });

        // Act
        try {
            mLifecycleCamera.bind(sessionConfig);
        } catch (CameraUseCaseAdapter.CameraException e) {
            throw new RuntimeException(e);
        }

        // Assert: GroupableFeature selection listener is invoked with an empty set.
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(selectedFeatures).isEmpty();
    }

    @Test
    public void bindSessionConfig_isBoundIsCorrect() throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig sessionConfig =
                new SessionConfig.Builder(Arrays.asList(mFakeUseCase))
                        .setViewPort(mViewPort)
                        .addEffect(mEffect)
                        .build();
        assertThat(mLifecycleCamera.isBound(sessionConfig)).isFalse();
        assertThat(mLifecycleCamera.isBound(mFakeUseCase)).isFalse();
        mLifecycleCamera.bind(sessionConfig);

        assertThat(mLifecycleCamera.isBound(sessionConfig)).isTrue();
        assertThat(mLifecycleCamera.isBound(mFakeUseCase)).isTrue();
    }

    @Test
    public void bindLegacySessionConfig_willBindToCameraInternal()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig legacySessionConfig = new LegacySessionConfig(
                Arrays.asList(mFakeUseCase),
                mViewPort,
                Arrays.asList(mEffect));
        mLifecycleCamera.bind(legacySessionConfig);

        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getViewPort())
                .isEqualTo(legacySessionConfig.getViewPort());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getEffects())
                .isEqualTo(legacySessionConfig.getEffects());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getSessionType()).isEqualTo(
                legacySessionConfig.getSessionType());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getFrameRate())
                .isEqualTo(legacySessionConfig.getFrameRateRange());
    }

    @Test
    public void unbind_willUnbindFromCameraInternal() throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase));
        mLifecycleCamera.unbind(createLegacySessonConfig(mFakeUseCase));

        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();
    }

    @Test
    public void unbindAll_willUnbindFromCameraInternal()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase));
        mLifecycleCamera.unbindAll();

        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();
    }

    @Test
    public void bindLegacySessonConfig_thenSessionConfig_throwExceptions()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase));

        assertThrows(IllegalStateException.class, () ->
                mLifecycleCamera.bind(createSessionConfig(mFakeUseCase2)));
    }

    @Test
    public void bindLegacySessonConfigWithSessionConfig_throwExceptions()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createSessionConfig(mFakeUseCase));

        assertThrows(IllegalStateException.class, () ->
                mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase2)));

        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
    }

    @Test
    public void bindMultipleSessonConfig_latestSessionConfigIsBound()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig sessionConfig1 = createSessionConfig(mFakeUseCase);
        SessionConfig sessionConfig2 = createSessionConfig(mFakeUseCase2);
        mLifecycleCamera.bind(sessionConfig1);
        mLifecycleCamera.bind(sessionConfig2);

        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase2);
        assertThat(mLifecycleCamera.isBound(sessionConfig1)).isFalse();
        assertThat(mLifecycleCamera.isBound(sessionConfig2)).isTrue();
        assertThat(mLifecycleCamera.isBound(mFakeUseCase)).isFalse();
        assertThat(mLifecycleCamera.isBound(mFakeUseCase2)).isTrue();
    }

    @Test
    public void bindSessonConfigWithLegacySessionConfig_throwExceptions()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase));

        assertThrows(IllegalStateException.class, () ->
                mLifecycleCamera.bind(createSessionConfig(mFakeUseCase2)));

        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
    }

    @Test
    public void bindLegacySessionConfigMultipleTimes_parametersUpdate_notThrowExceptions()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig legacySessionConfig1 = new LegacySessionConfig(
                Arrays.asList(mFakeUseCase),
                mViewPort,
                Arrays.asList(mEffect)
        );
        SessionConfig legacySessionConfig2 = createLegacySessonConfig(mFakeUseCase2);

        mLifecycleCamera.bind(legacySessionConfig1);
        mLifecycleCamera.bind(legacySessionConfig2);
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase, mFakeUseCase2);
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getViewPort())
                .isEqualTo(legacySessionConfig2.getViewPort());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getEffects())
                .isEqualTo(legacySessionConfig2.getEffects());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getSessionType())
                .isEqualTo(legacySessionConfig2.getSessionType());
        assertThat(mLifecycleCamera.getCameraUseCaseAdapter().getFrameRate())
                .isEqualTo(legacySessionConfig2.getFrameRateRange());
    }

    @Test
    public void canBindUnbindLegacySessionConfigMultipleTimes_withDuplicateUseCases()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase, mFakeUseCase));
        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase, mFakeUseCase2));
        assertThat(mLifecycleCamera.getBoundSessionConfig().getUseCases())
                .containsExactly(mFakeUseCase, mFakeUseCase2);

        mLifecycleCamera.unbind(createLegacySessonConfig(mFakeUseCase, mFakeUseCase));
        mLifecycleCamera.unbind(createLegacySessonConfig(mFakeUseCase2));
        assertThat(mLifecycleCamera.getBoundSessionConfig()).isNull();
    }

    @Test
    public void unbindSessionConfig_canBindSessionConfigAgain()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig sessionConfig = createSessionConfig(mFakeUseCase);
        mLifecycleCamera.bind(sessionConfig);
        mLifecycleCamera.unbind(sessionConfig);

        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();

        // After unbinding, it can now bind new SessionConfig.
        mLifecycleCamera.bind(createSessionConfig(mFakeUseCase2));
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase2);
    }

    @Test
    public void unbindSessionConfig_noSessionConfigBoundPreviously_noOps() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig sessionConfig = createSessionConfig(mFakeUseCase);
        mLifecycleCamera.unbind(sessionConfig);
        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();
    }

    @Test
    public void unbindLegacyConfig_noSessionConfigBoundPreviously_noExceptions() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig legacyConfig = createLegacySessonConfig(mFakeUseCase);
        mLifecycleCamera.unbind(legacyConfig);
        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();
    }

    @Test
    public void unbindNonBoundSessionConfig_noOps()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        SessionConfig sessionConfig1 = createSessionConfig(mFakeUseCase);
        SessionConfig sessionConfig2 = createSessionConfig(mFakeUseCase);

        mLifecycleCamera.bind(sessionConfig1);
        mLifecycleCamera.unbind(sessionConfig2);

        // camera state is not changed
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
        assertThat(mLifecycleCamera.isBound(sessionConfig1)).isTrue();
        assertThat(mLifecycleCamera.isBound(mFakeUseCase)).isTrue();
    }

    @Test
    public void bindLegacySessionConfig_thenUnbindSessionConfig_noOps()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase));

        mLifecycleCamera.unbind(createSessionConfig(mFakeUseCase)); // no-ops
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
    }

    @Test
    public void bindSessionConfig_thenUnbindLegacySessionConfig_noOps()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createSessionConfig(mFakeUseCase));

        mLifecycleCamera.unbind(createLegacySessonConfig(mFakeUseCase)); // no-ops
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
    }

    @Test
    public void unbindLegacyConfigsMultipleTimes_canBindSessionConfigAgain()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase));
        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase2));
        mLifecycleCamera.unbind(createLegacySessonConfig(mFakeUseCase2));
        mLifecycleCamera.unbind(createLegacySessonConfig(mFakeUseCase));

        // After unbinding all of the use cases, it can now bind new SessionConfig.
        mLifecycleCamera.bind(createSessionConfig(mFakeUseCase2));
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase2);
    }

    @Test
    public void canNotBindSessionConfig_whenThereAreBoundUseCases()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase));
        mLifecycleCamera.bind(createLegacySessonConfig(mFakeUseCase2));
        mLifecycleCamera.unbind(createLegacySessonConfig(mFakeUseCase2));

        // still has bound UseCases, can't allow binding SessionConfig.
        assertThrows(IllegalStateException.class, () ->
                mLifecycleCamera.bind(createSessionConfig(mFakeUseCase)));
    }

    @Test
    public void unbindAll_canBindSessionConfigAgain() throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(createSessionConfig(mFakeUseCase));
        mLifecycleCamera.unbindAll();

        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();

        // After unbinding, it can now bind new SessionConfig.
        mLifecycleCamera.bind(createSessionConfig(mFakeUseCase2));
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase2);
    }
}
