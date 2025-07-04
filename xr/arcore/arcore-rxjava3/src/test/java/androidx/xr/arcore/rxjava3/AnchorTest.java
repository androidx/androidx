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

import static androidx.xr.arcore.rxjava3.RxJava3Anchor.getStateAsFlowable;

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.arcore.Anchor;
import androidx.xr.arcore.AnchorCreateSuccess;
import androidx.xr.arcore.XrResourcesManager;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.TrackingState;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;

import io.reactivex.rxjava3.subscribers.TestSubscriber;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AnchorTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;
    private XrResourcesManager mXrResourcesManager;

    @Before
    public void setUp() {
        mXrResourcesManager = new XrResourcesManager();
    }

    @Test
    public void anchor_stateAsFlowable_returnsAnchorState() {
        createTestSessionAndRunTest(() -> {
            Pose anchorPose = new Pose(Vector3.One, Quaternion.Identity);
            AnchorCreateSuccess anchorResult = (AnchorCreateSuccess) Anchor.create(mSession,
                    anchorPose);
            Anchor underTest = anchorResult.getAnchor();
            TestSubscriber<Anchor.State> testSubscriber = new TestSubscriber<>();

            getStateAsFlowable(underTest).subscribe(testSubscriber);

            assertThat(testSubscriber.values().get(0).getTrackingState()).isEqualTo(
                    TrackingState.TRACKING);
            assertThat(testSubscriber.values().get(0).getPose()).isEqualTo(
                    anchorPose);
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
