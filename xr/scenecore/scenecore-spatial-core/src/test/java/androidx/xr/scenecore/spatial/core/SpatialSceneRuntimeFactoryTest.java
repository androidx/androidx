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

package androidx.xr.scenecore.spatial.core;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;

import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider;
import androidx.xr.scenecore.runtime.SceneRuntime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link SpatialSceneRuntimeFactory}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class SpatialSceneRuntimeFactoryTest {

    @Before
    public void setUp() {
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(1);
    }

    @After
    public void tearDown() {
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(null);
    }

    @Test
    public void createSceneRuntime_returnsNonNullInstance() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().get();
        SpatialSceneRuntimeFactory factory = new SpatialSceneRuntimeFactory();

        SceneRuntime sceneRuntime = factory.create(activity, false);

        assertThat(sceneRuntime).isNotNull();
        assertThat(sceneRuntime).isInstanceOf(SpatialSceneRuntime.class);
    }
}
