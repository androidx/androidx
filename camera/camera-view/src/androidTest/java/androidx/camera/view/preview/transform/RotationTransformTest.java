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

package androidx.camera.view.preview.transform;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;

import androidx.camera.testing.fakes.FakeActivity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RotationTransformTest {

    @Rule
    public final ActivityTestRule<FakeActivity> mActivityTestRule =
            new ActivityTestRule<>(FakeActivity.class);

    private View mView;

    @Before
    public void setUp() {
        mActivityTestRule.getActivity().runOnUiThread(() -> {
            final Context context = mActivityTestRule.getActivity();
            mView = new View(context);
            mActivityTestRule.getActivity().setContentView(mView);
        });
    }

    @Test
    @UiThreadTest
    public void portrait() {
        mActivityTestRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        assertThat(RotationTransform.getRotationDegrees(mView)).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void reversePortrait() {
        mActivityTestRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        assertThat(RotationTransform.getRotationDegrees(mView)).isEqualTo(180);
    }

    @Test
    @UiThreadTest
    public void landscape() {
        mActivityTestRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        assertThat(RotationTransform.getRotationDegrees(mView)).isEqualTo(90);
    }

    @Test
    @UiThreadTest
    public void reverseLandscape() {
        mActivityTestRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        assertThat(RotationTransform.getRotationDegrees(mView)).isEqualTo(270);
    }
}
