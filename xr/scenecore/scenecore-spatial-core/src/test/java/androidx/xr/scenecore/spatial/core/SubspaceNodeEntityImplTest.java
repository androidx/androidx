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

import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public final class SubspaceNodeEntityImplTest {
    private XrExtensions mXrExtensions;
    private Activity mActivity;
    private SubspaceNodeEntityImpl mSubspaceNodeEntity;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        EntityManager entityManager = new EntityManager();
        FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
        ActivitySpaceImpl activitySpace =
                new ActivitySpaceImpl(
                        mXrExtensions.createNode(),
                        mActivity,
                        mXrExtensions,
                        entityManager,
                        () -> mXrExtensions.getSpatialState(mActivity),
                        executor);

        Dimensions size = new Dimensions(1.0f, 2.0f, 3.0f);

        mSubspaceNodeEntity =
                new SubspaceNodeEntityImpl(
                        mActivity,
                        mXrExtensions,
                        mXrExtensions.createNode(),
                        entityManager,
                        executor);
        mSubspaceNodeEntity.setParent(activitySpace);
    }

    @Test
    public void setSize_sizeIsUpdated() {
        Dimensions size = new Dimensions(3.0f, 4.0f, 5.0f);

        mSubspaceNodeEntity.setSize(size);

        assertThat(mSubspaceNodeEntity.getSize()).isEqualTo(size);
    }

    @Test
    public void setScale_scaleIsUpdated() {
        Dimensions size = new Dimensions(3.0f, 4.0f, 5.0f);
        Vector3 scale = new Vector3(1.0f, 2.0f, 3.0f);

        mSubspaceNodeEntity.setSize(size);
        mSubspaceNodeEntity.setScale(scale);

        assertThat(mSubspaceNodeEntity.getScale()).isEqualTo(scale);
    }

    @Test
    public void setAlpha_alphaIsUpdated() {
        float alpha = 0.5f;

        mSubspaceNodeEntity.setAlpha(alpha);

        assertThat(mSubspaceNodeEntity.getAlpha()).isEqualTo(alpha);
    }

    @Test
    public void setHidden_visibilityIsUpdated() {
        boolean hidden = true;

        mSubspaceNodeEntity.setHidden(hidden);

        assertThat(mSubspaceNodeEntity.isHidden(false)).isEqualTo(hidden);
    }
}
