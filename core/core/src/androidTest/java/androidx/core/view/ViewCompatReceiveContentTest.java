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

package androidx.core.view;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ViewCompatReceiveContentTest {
    @Rule
    public final ActivityTestRule<ViewCompatActivity> mActivityTestRule =
            new ActivityTestRule<>(ViewCompatActivity.class);

    private View mView;
    private OnReceiveContentListener mMockReceiver;

    @UiThreadTest
    @Before
    public void before() {
        final Activity activity = mActivityTestRule.getActivity();
        mView = activity.findViewById(androidx.core.test.R.id.view);
        mMockReceiver = Mockito.mock(OnReceiveContentListener.class);
    }

    @SdkSuppress(maxSdkVersion = 30) // b/202524605
    @UiThreadTest
    @Test
    public void testSetOnReceiveContentListener() throws Exception {
        // Verify that by default the getters returns null.
        assertThat(ViewCompat.getOnReceiveContentMimeTypes(mView)).isNull();
        assertThat(getListener(mView)).isNull();

        // Verify that setting non-null MIME types and a non-null receiver works.
        String[] mimeTypes = new String[] {"image/*", "video/mp4"};
        ViewCompat.setOnReceiveContentListener(mView, mimeTypes, mMockReceiver);
        assertThat(ViewCompat.getOnReceiveContentMimeTypes(mView)).isSameInstanceAs(mimeTypes);
        assertThat(getListener(mView)).isSameInstanceAs(mMockReceiver);

        // Verify that setting null MIME types and a null receiver works.
        ViewCompat.setOnReceiveContentListener(mView, null, null);
        assertThat(ViewCompat.getOnReceiveContentMimeTypes(mView)).isNull();
        assertThat(getListener(mView)).isNull();

        // Verify that setting empty MIME types and a null receiver works.
        ViewCompat.setOnReceiveContentListener(mView, new String[0], null);
        assertThat(ViewCompat.getOnReceiveContentMimeTypes(mView)).isNull();
        assertThat(getListener(mView)).isNull();

        // Verify that setting MIME types with a null receiver works.
        ViewCompat.setOnReceiveContentListener(mView, mimeTypes, null);
        assertThat(ViewCompat.getOnReceiveContentMimeTypes(mView)).isSameInstanceAs(mimeTypes);
        assertThat(getListener(mView)).isNull();

        // Verify that setting null or empty MIME types with a non-null receiver is not allowed.
        try {
            ViewCompat.setOnReceiveContentListener(mView, null, mMockReceiver);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { }
        try {
            ViewCompat.setOnReceiveContentListener(mView, new String[0], mMockReceiver);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { }

        // Verify that passing "*/*" as a MIME type is not allowed.
        try {
            ViewCompat.setOnReceiveContentListener(mView, new String[] {"image/gif", "*/*"},
                    mMockReceiver);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { }
    }

    @Nullable
    private static OnReceiveContentListener getListener(@NonNull View view) {
        return (OnReceiveContentListener) view.getTag(R.id.tag_on_receive_content_listener);
    }
}
