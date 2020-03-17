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
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;

import androidx.camera.testing.fakes.FakeActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PreviewTransformTest {

    private static final Size CONTAINER = new Size(800, 450);
    private static final Size BUFFER = new Size(400, 300);

    @Rule
    public final ActivityTestRule<FakeActivity> mActivityTestRule = new ActivityTestRule<>(
            FakeActivity.class);

    private FrameLayout mContainer;
    private View mView;

    @Before
    public void setUp() throws Throwable {
        mActivityTestRule.runOnUiThread(() -> {
            final Context context = mActivityTestRule.getActivity();

            final FrameLayout root = new FrameLayout(context);

            mContainer = new FrameLayout(context);
            mContainer.setLayoutParams(
                    new FrameLayout.LayoutParams(CONTAINER.getWidth(), CONTAINER.getHeight()));

            mView = new View(context);
            mView.setLayoutParams(
                    new FrameLayout.LayoutParams(BUFFER.getWidth(), BUFFER.getHeight()));
            mView.setBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_blue_dark));

            mContainer.addView(mView);
            root.addView(mContainer);
            mActivityTestRule.getActivity().setContentView(root);
        });
    }

    @Test
    @UiThreadTest
    public void fillStart() {
        final PreviewTransform previewTransform = new PreviewTransform();
        previewTransform.setScaleType(PreviewView.ScaleType.FILL_START);
        previewTransform.applyCurrentScaleType(mContainer, mView, BUFFER);

        // Assert the preview fills its container
        assertThat(mView.getWidth() * mView.getScaleX()).isAtLeast(mContainer.getWidth());
        assertThat(mView.getHeight() * mView.getScaleY()).isAtLeast(mContainer.getHeight());
    }

    @Test
    @UiThreadTest
    public void fillCenter() {
        final PreviewTransform previewTransform = new PreviewTransform();
        previewTransform.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewTransform.applyCurrentScaleType(mContainer, mView, BUFFER);

        // Assert the preview fills its container
        assertThat(mView.getWidth() * mView.getScaleX()).isAtLeast(mContainer.getWidth());
        assertThat(mView.getHeight() * mView.getScaleY()).isAtLeast(mContainer.getHeight());
    }

    @Test
    @UiThreadTest
    public void fillEnd() {
        final PreviewTransform previewTransform = new PreviewTransform();
        previewTransform.setScaleType(PreviewView.ScaleType.FILL_END);
        previewTransform.applyCurrentScaleType(mContainer, mView, BUFFER);

        // Assert the preview fills its container
        assertThat(mView.getWidth() * mView.getScaleX()).isAtLeast(mContainer.getWidth());
        assertThat(mView.getHeight() * mView.getScaleY()).isAtLeast(mContainer.getHeight());
    }
}
