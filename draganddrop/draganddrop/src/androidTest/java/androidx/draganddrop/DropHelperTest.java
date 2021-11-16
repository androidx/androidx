/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.draganddrop;

import static android.view.DragEvent.ACTION_DRAG_ENDED;
import static android.view.DragEvent.ACTION_DRAG_ENTERED;
import static android.view.DragEvent.ACTION_DRAG_EXITED;
import static android.view.DragEvent.ACTION_DRAG_STARTED;
import static android.view.DragEvent.ACTION_DROP;

import static androidx.draganddrop.DragAndDropTestUtils.makeImageDragEvent;
import static androidx.draganddrop.DragAndDropTestUtils.makeTextDragEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static java.lang.Math.max;
import static java.lang.Math.round;

import android.app.Activity;
import android.content.ClipData;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.Pair;
import android.view.DragEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentListener;
import androidx.draganddrop.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/** Tests for {@link androidx.draganddrop.DropHelper}. */
@RunWith(AndroidJUnit4.class)
public class DropHelperTest {

    private static final int UI_REFRESH_TIMEOUT_MS = 2000;

    private Activity mActivity;
    private EditText mDropTarget;
    private AppCompatEditText mAppCompatEditText;
    private View mOuterNestedDropTarget;
    private EditText mInnerNestedEditText;
    private OnReceiveContentListener mListener;
    private final AtomicBoolean mListenerCalled = new AtomicBoolean(false);
    private final ArrayList<ClipData.Item> mDroppedUriItems = new ArrayList<>();

    private static final @ColorInt int COLOR_FULL_OPACITY = 0xFF112233;
    private static final @ColorInt int COLOR_INACTIVE_OPACITY = 0x33112233;
    private static final @ColorInt int COLOR_ACTIVE_OPACITY = 0xA5112233;

    @SuppressWarnings("deprecation")
    @Rule public androidx.test.rule.ActivityTestRule<DragAndDropActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(DragAndDropActivity.class);

    @Before
    @UiThreadTest
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mDropTarget = mActivity.findViewById(R.id.drop_target);
        mAppCompatEditText = mActivity.findViewById(R.id.app_compat_edit_text_drop_target);
        mOuterNestedDropTarget = mActivity.findViewById(R.id.outer_drop_target);
        mInnerNestedEditText = mActivity.findViewById(R.id.inner_edit_text);
        mListenerCalled.set(false);
        mDroppedUriItems.clear();
        mListener = (view, payload) -> {
            mListenerCalled.set(true);
            Pair<ContentInfoCompat, ContentInfoCompat> parts =
                    payload.partition(item -> item.getUri() != null);
            ContentInfoCompat uriItems = parts.first;
            if (uriItems != null) {
                for (int i = 0; i < uriItems.getClip().getItemCount(); i++) {
                    mDroppedUriItems.add(uriItems.getClip().getItemAt(i));
                }
            }
            return parts.second;
        };
    }

    // We attempted to add a test that drops onto mOuterNestedDropTarget and asserts that the drop
    // is handled. This fails in tests, but works in practice, as seen in the sample app.

    @Test
    public void testDropHelper_startDrag_hasInactiveHighlight() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);
    }

    @Test
    public void testDropHelper_endDrag_hasNoHighlight() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForNoHighlight(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENDED),
                mDropTarget);
    }

    @Test
    public void testDropHelper_wrongMimeTypeImage_hasNoHighlight() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"image/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForNoHighlight(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget);

    }

    @Test
    public void testDropHelper_wrongMimeTypeText_hasNoHighlight() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForNoHighlight(
                mDropTarget,
                makeImageDragEvent(ACTION_DRAG_STARTED),
                mDropTarget);
    }

    @Test
    public void testDropHelper_enterAndExit_togglesHighlight() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENTERED),
                mDropTarget,
                COLOR_ACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_EXITED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENTERED),
                mDropTarget,
                COLOR_ACTIVE_OPACITY);
    }

    @Test
    public void testDropHelper_dragToInnerNestedView_retainsActiveHighlight()
            throws Exception {
        DropHelper.configureView(
                mActivity,
                mOuterNestedDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder()
                        .setHighlightColor(COLOR_FULL_OPACITY)
                        .addInnerEditTexts(mInnerNestedEditText)
                        .build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mInnerNestedEditText,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mOuterNestedDropTarget,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mInnerNestedEditText,
                makeTextDragEvent(ACTION_DRAG_ENTERED),
                mOuterNestedDropTarget,
                COLOR_ACTIVE_OPACITY);
    }

    @Test
    public void testDropHelper_usesColorFromSystemAsDefault() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                mListener);

        @ColorInt int color = mActivity.obtainStyledAttributes(
                new int[]{androidx.appcompat.R.attr.colorAccent}).getColor(0, 0);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                colorWithOpacity(color, DropAffordanceHighlighter.FILL_OPACITY_INACTIVE));

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENTERED),
                mDropTarget,
                colorWithOpacity(color, DropAffordanceHighlighter.FILL_OPACITY_ACTIVE));
    }

    @Test
    public void testDropHelper_usesDefaultCornerRadius() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        GradientDrawable foreground = (GradientDrawable) mDropTarget.getForeground();
        assertEquals(dpToPx(DropAffordanceHighlighter.DEFAULT_CORNER_RADIUS_DP),
                (int) foreground.getCornerRadius());
    }

    @Test
    public void testDropHelper_usesCornerRadius() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder()
                        .setHighlightCornerRadiusPx(1)
                        .setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        GradientDrawable foreground = (GradientDrawable) mDropTarget.getForeground();
        assertEquals(1, (int) foreground.getCornerRadius());
    }

    @Test
    public void testDropHelper_drop_callsListener() throws Exception {
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENTERED),
                mDropTarget,
                COLOR_ACTIVE_OPACITY);

        sendEvent(mDropTarget, makeTextDragEvent(ACTION_DROP));
        sendEventAndWaitForNoHighlight(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENDED),
                mDropTarget);

        assertTrue(mListenerCalled.get());
    }

    @Test
    public void testDropHelper_drop_editText_insertsText() throws Exception {
        assertFalse(mDropTarget instanceof AppCompatEditText);
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENTERED),
                mDropTarget,
                COLOR_ACTIVE_OPACITY);

        sendEvent(mDropTarget, makeTextDragEvent(ACTION_DROP));
        sendEventAndWaitForNoHighlight(
                mDropTarget,
                makeTextDragEvent(ACTION_DRAG_ENDED),
                mDropTarget);

        String foundText = ((TextView) mDropTarget).getText().toString();
        assertTrue(foundText.contains(DragAndDropTestUtils.SAMPLE_TEXT));
    }

    @Test
    public void testDropHelper_drop_editText_handlesUri() throws Exception {
        assertFalse(mDropTarget instanceof AppCompatEditText);
        DropHelper.configureView(
                mActivity,
                mDropTarget,
                new String[]{"image/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeImageDragEvent(ACTION_DRAG_STARTED),
                mDropTarget,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mDropTarget,
                makeImageDragEvent(ACTION_DRAG_ENTERED),
                mDropTarget,
                COLOR_ACTIVE_OPACITY);

        sendEvent(mDropTarget, makeImageDragEvent(ACTION_DROP));
        sendEventAndWaitForNoHighlight(
                mDropTarget,
                makeImageDragEvent(ACTION_DRAG_ENDED),
                mDropTarget);

        assertTrue(mDroppedUriItems.get(0).getUri().equals(DragAndDropTestUtils.SAMPLE_URI));
    }

    @Test
    public void testDropHelper_drop_appCompatEditText_insertsText() throws Exception {
        DropHelper.configureView(
                mActivity,
                mAppCompatEditText,
                new String[]{"text/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mAppCompatEditText,
                makeTextDragEvent(ACTION_DRAG_STARTED),
                mAppCompatEditText,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mAppCompatEditText,
                makeTextDragEvent(ACTION_DRAG_ENTERED),
                mAppCompatEditText,
                COLOR_ACTIVE_OPACITY);

        sendEvent(mAppCompatEditText, makeTextDragEvent(ACTION_DROP));
        sendEventAndWaitForNoHighlight(
                mAppCompatEditText,
                makeTextDragEvent(ACTION_DRAG_ENDED),
                mAppCompatEditText);

        String foundText = mAppCompatEditText.getText().toString();
        assertTrue(foundText.contains(DragAndDropTestUtils.SAMPLE_TEXT));
    }

    @Test
    public void testDropHelper_drop_appCompatEditText_handlesUri() throws Exception {
        DropHelper.configureView(
                mActivity,
                mAppCompatEditText,
                new String[]{"image/*"},
                new DropHelper.Options.Builder().setHighlightColor(COLOR_FULL_OPACITY).build(),
                mListener);

        sendEventAndWaitForHighlightColor(
                mAppCompatEditText,
                makeImageDragEvent(ACTION_DRAG_STARTED),
                mAppCompatEditText,
                COLOR_INACTIVE_OPACITY);

        sendEventAndWaitForHighlightColor(
                mAppCompatEditText,
                makeImageDragEvent(ACTION_DRAG_ENTERED),
                mAppCompatEditText,
                COLOR_ACTIVE_OPACITY);

        sendEvent(mAppCompatEditText, makeImageDragEvent(ACTION_DROP));
        sendEventAndWaitForNoHighlight(
                mAppCompatEditText,
                makeImageDragEvent(ACTION_DRAG_ENDED),
                mAppCompatEditText);

        assertTrue(mDroppedUriItems.get(0).getUri().equals(DragAndDropTestUtils.SAMPLE_URI));
    }

    private void sendEventAndWaitForHighlightColor(View viewToReceiveEvent, DragEvent event,
            View viewToAssertHighlight, @ColorInt int color) throws Exception {
        sendEventAndWait(viewToReceiveEvent, event,
                () -> hasHighlightWithColor(viewToAssertHighlight, color));
    }

    private void sendEventAndWaitForNoHighlight(View viewToReceiveEvent, DragEvent event,
            View viewToAssertNoHighlight) throws Exception {
        sendEventAndWait(viewToReceiveEvent, event,
                () -> viewToAssertNoHighlight.getForeground() == null);
    }

    private void sendEventAndWait(View view, DragEvent dragEvent, Callable<Boolean> criteria)
            throws Exception {
        sendEvent(view, dragEvent);
        long endTime = SystemClock.uptimeMillis() + UI_REFRESH_TIMEOUT_MS;
        while (!criteria.call()) {
            if (SystemClock.uptimeMillis() > endTime) {
                fail("Criteria not met after timeout.");
            }
            Thread.sleep(20);
        }
    }

    private void sendEvent(View view, DragEvent dragEvent) {
        mActivity.runOnUiThread(() -> view.dispatchDragEvent(dragEvent));
    }

    private static boolean hasHighlightWithColor(View view, @ColorInt int color) {
        GradientDrawable foreground = (GradientDrawable) view.getForeground();
        return foreground != null && foreground.getColor().equals(ColorStateList.valueOf(color));
    }

    private int dpToPx(int valueDp) {
        return round(max(0, valueDp) * mActivity.getResources().getDisplayMetrics().density);
    }

    private static @ColorInt int colorWithOpacity(@ColorInt int color, float opacity) {
        return (0x00ffffff & color) | (((int) (255 * opacity)) << 24);
    }
}
