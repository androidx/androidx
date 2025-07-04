/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.guava;

import static androidx.xr.arcore.guava.GuavaAnchor.persistAsync;
import static androidx.xr.arcore.guava.GuavaAnchor.updateAsync;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import static org.junit.Assert.assertThrows;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.arcore.Anchor;
import androidx.xr.arcore.XrResourcesManager;
import androidx.xr.runtime.Config;
import androidx.xr.runtime.Config.AnchorPersistenceMode;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.testing.FakePerceptionManager;

import com.google.common.util.concurrent.ListenableFuture;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class AnchorGuavaTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;
    private XrResourcesManager mXrResourcesManager;

    @Before
    public void setUp() {
        mXrResourcesManager = new XrResourcesManager();
    }

    @Test
    public void persist_runtimeAnchorIsPersisted() {
        createTestSessionAndRunTest(() -> {
            FakePerceptionManager fakePerceptionManager =
                    (FakePerceptionManager) mSession.getRuntime().getPerceptionManager();
            androidx.xr.runtime.internal.Anchor runtimeAnchor =
                    fakePerceptionManager.createAnchor(new Pose());
            Anchor underTest = new Anchor(runtimeAnchor, mXrResourcesManager);
            checkState(runtimeAnchor.getPersistenceState()
                            == androidx.xr.runtime.internal.Anchor.PersistenceState.NOT_PERSISTED,
                    "Expected anchor to be NOT_PERSISTED initially");

            ListenableFuture<UUID> persistFuture = persistAsync(underTest, mSession);
            updateAsync(underTest, mSession);
            mTestDispatcher.getScheduler().advanceUntilIdle();
            try {
                UUID uuid = persistFuture.get();

                assertThat(uuid).isNotNull();
                assertThat(runtimeAnchor.getPersistenceState())
                        .isEqualTo(androidx.xr.runtime.internal.Anchor.PersistenceState.PERSISTED);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void persist_anchorPersistenceDisabled_throwsIllegalStateException() {
        createTestSessionAndRunTest(() -> {
            FakePerceptionManager fakePerceptionManager =
                    (FakePerceptionManager) mSession.getRuntime().getPerceptionManager();
            androidx.xr.runtime.internal.Anchor runtimeAnchor =
                    fakePerceptionManager.createAnchor(new Pose());
            Anchor underTest = new Anchor(runtimeAnchor, mXrResourcesManager);
            mSession.configure(new Config(Config.PlaneTrackingMode.DISABLED,
                    Config.HandTrackingMode.DISABLED,
                    Config.HeadTrackingMode.DISABLED,
                    Config.DepthEstimationMode.DISABLED, AnchorPersistenceMode.DISABLED));

            assertThrows(
                    ExecutionException.class,
                    () ->  {
                        ListenableFuture<UUID> persistFuture = persistAsync(underTest, mSession);
                        mTestDispatcher.getScheduler().advanceUntilIdle();
                        persistFuture.get();
                    });
        });
    }

    private void createTestSessionAndRunTest(Runnable testBody) {
        try (ActivityScenario<ComponentActivity> scenario = ActivityScenario.launch(
                ComponentActivity.class)) {
            scenario.onActivity(activity -> {
                mTestDispatcher = StandardTestDispatcher(/* scheduler= */ null, /* name= */ null);
                mSession = ((SessionCreateSuccess) Session.create(activity,
                        mTestDispatcher)).getSession();
                mXrResourcesManager.setLifecycleManager$arcore_release(
                        mSession.getRuntime().getLifecycleManager());
                try {
                    testBody.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error during ActivityScenario setup or teardown", e);
        }
    }
}
