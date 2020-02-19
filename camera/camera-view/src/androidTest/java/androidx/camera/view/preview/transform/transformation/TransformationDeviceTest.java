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

package androidx.camera.view.preview.transform.transformation;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.view.View;

import androidx.camera.testing.fakes.FakeActivity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

public class TransformationDeviceTest {

    @Rule
    public final ActivityTestRule<FakeActivity> mRule = new ActivityTestRule<>(FakeActivity.class);

    @Test
    @UiThreadTest
    public void getTransformationFromView() {
        final Activity activity = mRule.getActivity();
        final View view = new View(activity);
        view.setScaleX(2F);
        view.setScaleY(0.5F);
        view.setTranslationX(100);
        view.setTranslationY(35.5F);
        view.setRotation(3.14F);
        activity.setContentView(view);

        final Transformation transformation = Transformation.getTransformation(view);

        assertThat(transformation.getScaleX()).isEqualTo(2F);
        assertThat(transformation.getScaleY()).isEqualTo(0.5F);
        assertThat(transformation.getTransX()).isEqualTo(100);
        assertThat(transformation.getTransY()).isEqualTo(35.5F);
        assertThat(transformation.getRotation()).isEqualTo(3.14F);
    }
}
