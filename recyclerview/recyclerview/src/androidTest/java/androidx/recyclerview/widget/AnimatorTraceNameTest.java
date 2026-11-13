/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.recyclerview.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNoException;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/**
 * Verifies that RecyclerView's animators have a descriptive trace name, so that processes which
 * require trace names (e.g. System UI) don't log a {@code Log.wtf} when they start.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AnimatorTraceNameTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void recoverAnimation_animatorHasNoTarget() {
        assertNull(((ObjectAnimator) newRecoverAnimation(0f).mValueAnimator).getTarget());
    }

    @Test
    public void recoverAnimation_animatorUpdatesFraction() {
        ItemTouchHelper.RecoverAnimation recoverAnimation = newRecoverAnimation(100f);

        recoverAnimation.mValueAnimator.setInterpolator(null);
        recoverAnimation.mValueAnimator.setCurrentFraction(0.5f);
        recoverAnimation.update();

        assertEquals(50f, recoverAnimation.mX, 0.01f);
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    public void recoverAnimation_hasDescriptiveTraceName() throws Exception {
        assertEquals(
                "animator:RecoverAnimation#fraction",
                getTraceName(newRecoverAnimation(0f).mValueAnimator));
    }

    /** ItemTouchHelper.select() creates anonymous RecoverAnimation subclasses. */
    @Test
    @SdkSuppress(minSdkVersion = 37)
    public void recoverAnimationSubclass_hasDescriptiveTraceName() throws Exception {
        RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(new View(mContext)) {};
        ItemTouchHelper.RecoverAnimation recoverAnimation =
                new ItemTouchHelper.RecoverAnimation(
                        viewHolder,
                        ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS,
                        ItemTouchHelper.ACTION_STATE_SWIPE,
                        0f,
                        0f,
                        100f,
                        0f) {};

        assertEquals(
                "animator:RecoverAnimation#fraction",
                getTraceName(recoverAnimation.mValueAnimator));
    }

    @Test
    public void fastScroller_animatorHasNoTarget() {
        assertNull(((ObjectAnimator) newFastScroller().mShowHideAnimator).getTarget());
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    public void fastScroller_hasDescriptiveTraceName() throws Exception {
        assertEquals(
                "animator:FastScroller#showHide",
                getTraceName(newFastScroller().mShowHideAnimator));
    }

    private ItemTouchHelper.RecoverAnimation newRecoverAnimation(float targetX) {
        RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(new View(mContext)) {};
        return new ItemTouchHelper.RecoverAnimation(
                viewHolder,
                ItemTouchHelper.ANIMATION_TYPE_DRAG,
                ItemTouchHelper.ACTION_STATE_DRAG,
                0f,
                0f,
                targetX,
                0f);
    }

    private FastScroller newFastScroller() {
        FastScroller[] fastScroller = new FastScroller[1];
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                fastScroller[0] =
                                        new FastScroller(
                                                new RecyclerView(mContext),
                                                new StateListDrawable(),
                                                new ColorDrawable(),
                                                new StateListDrawable(),
                                                new ColorDrawable(),
                                                10,
                                                10,
                                                0));
        return fastScroller[0];
    }

    /** Reads the trace name, skipping the test on platforms without the trace name API. */
    private static String getTraceName(ValueAnimator animator) throws Exception {
        Method getTraceName = null;
        try {
            getTraceName = ValueAnimator.class.getMethod("getTraceName");
        } catch (NoSuchMethodException e) {
            assumeNoException(e);
        }
        return (String) getTraceName.invoke(animator);
    }
}
