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

package androidx.xr.arcore.rxjava3;

import static androidx.xr.arcore.rxjava3.RxJava3Plane.getStateAsFlowable;
import static androidx.xr.arcore.rxjava3.RxJava3Plane.subscribeAsFlowable;

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import static org.junit.Assert.assertThrows;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.arcore.Plane;
import androidx.xr.arcore.XrResourcesManager;
import androidx.xr.runtime.Config;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.testing.FakeRuntimePlane;

import io.reactivex.rxjava3.subscribers.TestSubscriber;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RxJava3PlaneTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;
    private XrResourcesManager mXrResourcesManager;

    @Before
    public void setUp() {
        mXrResourcesManager = new XrResourcesManager();
    }

    @Test
    public void plane_stateFlowable_returnsPlaneState() {
        createTestSessionAndRunTest(() -> {
            Plane underTest = new Plane(new FakeRuntimePlane(), mXrResourcesManager);
            TestSubscriber<Plane.State> testSubscriber = new TestSubscriber<>();

            getStateAsFlowable(underTest).subscribe(testSubscriber);

            assertThat(testSubscriber.values().get(0).getCenterPose()).isEqualTo(Pose.Identity);
        });
    }

    @Test
    public void subscribeAsFlowable_planeTrackingDisabled_throwsIllegalStateException() {
        createTestSessionAndRunTest(
                () -> {
                    Config config = new Config(Config.PlaneTrackingMode.DISABLED);
                    mSession.configure(config);
                    IllegalStateException thrown =
                            assertThrows(
                                    IllegalStateException.class,
                                    () -> subscribeAsFlowable(mSession).subscribe());
                });
    }

    @Test
    public void subscribeAsFlowable_collectReturnsPlane() {
        createTestSessionAndRunTest(
                () -> {
                    TestSubscriber<Collection<Plane>> testSubscriber =
                            subscribeAsFlowable(mSession).test();

                    testSubscriber.assertValueCount(1);

                    List<Collection<Plane>> emittedValues = testSubscriber.values();
                    Collection<Plane> firstEmission = emittedValues.get(0);

                    assertThat(firstEmission).hasSize(0);

                    testSubscriber.assertValueCount(1);
                    testSubscriber.cancel();
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
