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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;

import androidx.camera.testing.fakes.FakeActivity;
import androidx.camera.view.PreviewView;
import androidx.test.annotation.UiThreadTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PreviewTransformTest {

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
            mContainer = new FrameLayout(context);
            mContainer.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            mView = new View(context);
            mView.setLayoutParams(
                    new FrameLayout.LayoutParams(BUFFER.getWidth(), BUFFER.getHeight()));

            mContainer.addView(mView);
            mActivityTestRule.getActivity().setContentView(mContainer);
        });
    }

    @Test
    @UiThreadTest
    public void fillStart() {
        PreviewTransform.transform(mContainer, mView, BUFFER, PreviewView.ScaleType.FILL_START);

        // Assert the preview fills its container
        assertThat(mView.getWidth() * mView.getScaleX()).isAtLeast(mContainer.getWidth());
        assertThat(mView.getHeight() * mView.getScaleY()).isAtLeast(mContainer.getHeight());

        // Assert the preview is at the start (top left) of its container
        assertThat(mView.getX()).isEqualTo(0);
        assertThat(mView.getY()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void fillCenter() {
        PreviewTransform.transform(mContainer, mView, BUFFER, PreviewView.ScaleType.FILL_CENTER);

        // Assert the preview fills its container
        assertThat(mView.getWidth() * mView.getScaleX()).isAtLeast(mContainer.getWidth());
        assertThat(mView.getHeight() * mView.getScaleY()).isAtLeast(mContainer.getHeight());

        // Assert the preview is centered in its container
        assertThat(mView.getX() + mView.getWidth() / 2).isEqualTo(mContainer.getWidth() / 2);
        assertThat(mView.getY() + mView.getHeight() / 2).isEqualTo(mContainer.getHeight() / 2);
    }

    @Test
    @UiThreadTest
    public void fillEnd() {
        PreviewTransform.transform(mContainer, mView, BUFFER, PreviewView.ScaleType.FILL_END);

        // Assert the preview fills its container
        assertThat(mView.getWidth() * mView.getScaleX()).isAtLeast(mContainer.getWidth());
        assertThat(mView.getHeight() * mView.getScaleY()).isAtLeast(mContainer.getHeight());

        // Assert the preview is at the end (bottom right) of its container
        assertThat(mView.getX() + mView.getWidth()).isEqualTo(mContainer.getWidth());
        assertThat(mView.getY() + mView.getHeight()).isEqualTo(mContainer.getHeight());
    }
}
