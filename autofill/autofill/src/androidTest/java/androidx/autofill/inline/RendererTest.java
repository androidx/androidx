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

package androidx.autofill.inline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Instrumentation;
import android.app.PendingIntent;
import android.app.slice.Slice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.autofill.R;
import androidx.autofill.inline.common.TextViewStyle;
import androidx.autofill.inline.v1.InlineSuggestionUi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 30) // Needed only on 30 and above
public class RendererTest {

    @SuppressWarnings("deprecation")
    @Rule
    @NonNull
    public final androidx.test.rule.ActivityTestRule<InlineUiActivity> mActivityTestRule =
            new androidx.test.rule.ActivityTestRule<>(InlineUiActivity.class);

    private Instrumentation mInstrumentation;
    private Context mContext;
    private LinearLayout mLinearLayout;
    private PendingIntent mAttributionIntent;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getContext();
        mLinearLayout = mActivityTestRule.getActivity().findViewById(
                androidx.autofill.test.R.id.linear_layout);
        mAttributionIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
    }

    @Test
    public void testRender_validStyleAndContent() throws Exception {
        Bundle rendererVersions = Renderer.getSupportedInlineUiVersionsAsBundle();

        Bundle styles = getStyles(rendererVersions);
        assertNotNull(styles);

        Slice slice = getContent(styles);
        assertNotNull(slice);

        View suggestionView = Renderer.render(mContext, slice, styles);
        assertNotNull(suggestionView);

        // put the view on the linear layout and verify the UI
        final CountDownLatch viewAddedLatch = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mLinearLayout.addView(suggestionView);
            viewAddedLatch.countDown();
        });
        viewAddedLatch.await(5, TimeUnit.SECONDS);

        TextView titleView = mLinearLayout.findViewById(R.id.autofill_inline_suggestion_title);
        assertNotNull(titleView);
        assertEquals("Hello world!", titleView.getText());
        assertEquals(Color.GREEN, titleView.getCurrentTextColor());
    }

    @Test
    public void testRender_emptyStyle_nullView() {
        Bundle rendererVersions = Renderer.getSupportedInlineUiVersionsAsBundle();

        Bundle styles = getStyles(rendererVersions);
        assertNotNull(styles);

        Slice slice = getContent(styles);
        assertNotNull(slice);

        View suggestionView = Renderer.render(mContext, slice, /*styles*/ Bundle.EMPTY);
        assertNull(suggestionView);
    }

    @Nullable
    private Slice getContent(Bundle styles) {
        List<String> versions = UiVersions.getVersions(styles);
        if (versions.contains(UiVersions.INLINE_UI_VERSION_1)) {
            UiVersions.Content content = InlineSuggestionUi.newContentBuilder(mAttributionIntent)
                    .setTitle("Hello world!").build();
            return content.getSlice();
        }
        return null;
    }

    @Nullable
    private static Bundle getStyles(Bundle supportedVersions) {
        List<String> versions = UiVersions.getVersions(supportedVersions);
        UiVersions.StylesBuilder styles = UiVersions.newStylesBuilder();
        boolean hasStyle = false;
        if (versions.contains(UiVersions.INLINE_UI_VERSION_1)) {
            InlineSuggestionUi.Style style = InlineSuggestionUi.newStyleBuilder()
                    .setTitleStyle(
                            new TextViewStyle.Builder().setTextColor(
                                    Color.GREEN).build()).build();
            styles.addStyle(style);
            hasStyle = true;
        }
        return hasStyle ? styles.build() : null;
    }
}
