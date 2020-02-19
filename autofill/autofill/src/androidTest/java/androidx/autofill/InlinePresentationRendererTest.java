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

package androidx.autofill;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.app.PendingIntent;
import android.app.slice.Slice;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28) // Slice is added after API level 28.
public class InlinePresentationRendererTest {
    private Instrumentation mInstrumentation;
    private Context mContext;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getContext();
    }

    @Test
    public void renderSlice_title() {
        Slice slice = new InlinePresentationBuilder("title").build();
        View suggestionView = InlinePresentationRenderer.renderSlice(mContext, slice, null);
        verifyViewVisibility(suggestionView, /* titleVisible= */ true, /* subtitleVisible= */false,
                /* startIconVisible= */ false, /* endIconVisible= */ false);
        assertFalse(suggestionView.callOnClick());
    }

    @Test
    public void renderSlice_titleAndSubtitle() {
        Slice slice = new InlinePresentationBuilder("title").setSubtitle("subtitle").build();
        View suggestionView = InlinePresentationRenderer.renderSlice(mContext, slice, null);
        verifyViewVisibility(suggestionView, /* titleVisible= */ true, /* subtitleVisible= */true,
                /* startIconVisible= */ false, /* endIconVisible= */ false);
        assertFalse(suggestionView.callOnClick());
    }

    @Test
    public void renderSlice_startIconAndTitle() {
        Icon icon = Icon.createWithResource(mContext,
                androidx.autofill.test.R.drawable.ic_settings);
        Slice slice = new InlinePresentationBuilder("title").setStartIcon(icon).build();
        View suggestionView = InlinePresentationRenderer.renderSlice(mContext, slice, null);
        verifyViewVisibility(suggestionView, /* titleVisible= */ true, /* subtitleVisible= */false,
                /* startIconVisible= */ true, /* endIconVisible= */ false);
        assertFalse(suggestionView.callOnClick());
    }

    @Test
    public void renderSlice_titleAndEndIcon() {
        Icon icon = Icon.createWithResource(mContext,
                androidx.autofill.test.R.drawable.ic_settings);
        Slice slice = new InlinePresentationBuilder("title").setEndIcon(icon).build();
        View suggestionView = InlinePresentationRenderer.renderSlice(mContext, slice, null);
        verifyViewVisibility(suggestionView, /* titleVisible= */ true, /* subtitleVisible= */false,
                /* startIconVisible= */ false, /* endIconVisible= */ true);
        assertFalse(suggestionView.callOnClick());
    }

    @Test
    public void renderSlice_titleAndAction() {
        PendingIntent action = PendingIntent.getActivity(mContext, 0, new Intent(), 0);
        Slice slice = new InlinePresentationBuilder("title").setAction(action).build();
        View suggestionView = InlinePresentationRenderer.renderSlice(mContext, slice, null);
        verifyViewVisibility(suggestionView, /* titleVisible= */ true, /* subtitleVisible= */false,
                /* startIconVisible= */ false, /* endIconVisible= */ false);
        assertTrue(suggestionView.callOnClick());
    }

    private static void verifyViewVisibility(View suggestionView, boolean titleVisible,
            boolean subtitleVisible,
            boolean startIconVisible, boolean endIconVisible) {
        final ImageView startIconView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_start_icon);
        final TextView titleView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_title);
        final TextView subtitleView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_subtitle);
        final ImageView endIconView =
                suggestionView.findViewById(R.id.autofill_inline_suggestion_end_icon);
        assertEquals(toVisibility(titleVisible), titleView.getVisibility());
        assertEquals(toVisibility(subtitleVisible), subtitleView.getVisibility());
        assertEquals(toVisibility(startIconVisible), startIconView.getVisibility());
        assertEquals(toVisibility(endIconVisible), endIconView.getVisibility());
    }

    private static int toVisibility(boolean visible) {
        return visible ? View.VISIBLE : View.GONE;
    }
}
