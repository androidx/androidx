/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.leanback.graphics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.leanback.graphics.BoundsRule.ValueRule;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for {@link CompositeDrawableTest}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CompositeDrawableTest {

    private static final int HEIGHT = 800;
    private static final int WIDTH = 600;
    Bitmap mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);

    @Test
    public void updateBounds_noBoundsRule() {
        CompositeDrawable parentDrawable = new CompositeDrawable();
        FitWidthBitmapDrawable drawable = new FitWidthBitmapDrawable();
        drawable.setBitmap(mBitmap);
        Rect bounds = new Rect(0, 0, WIDTH, HEIGHT);
        parentDrawable.addChildDrawable(drawable);
        parentDrawable.updateBounds(bounds);

        Rect adjustedBounds = drawable.getBounds();
        assertEquals(bounds, adjustedBounds);
    }

    @Test
    public void updateBounds_withBoundsRule() {
        CompositeDrawable parentDrawable = new CompositeDrawable();
        float fraction = 0.5f;
        FitWidthBitmapDrawable drawable = new FitWidthBitmapDrawable();
        drawable.setBitmap(mBitmap);
        Rect bounds = new Rect(0, 0, WIDTH, HEIGHT);
        assertEquals(HEIGHT, bounds.height());
        assertEquals(WIDTH, bounds.width());

        // inherit from parent
        parentDrawable.addChildDrawable(drawable);
        parentDrawable.getChildAt(0).getBoundsRule().bottom = ValueRule.inheritFromParent(
                fraction);
        parentDrawable.updateBounds(bounds);

        Rect adjustedBounds = drawable.getBounds();
        Rect expectedBounds = new Rect(bounds);
        expectedBounds.bottom = bounds.top + (int) (HEIGHT * fraction);
        assertEquals(expectedBounds, adjustedBounds);

        // absolute value
        drawable.setBounds(bounds);
        parentDrawable.getChildAt(0).getBoundsRule().bottom = ValueRule.absoluteValue(200);
        parentDrawable.updateBounds(bounds);

        adjustedBounds = drawable.getBounds();
        expectedBounds = new Rect(bounds);
        expectedBounds.bottom = 200;
        assertEquals(expectedBounds, adjustedBounds);

        // inherit with offset
        parentDrawable.getChildAt(0).getBoundsRule().bottom =
                ValueRule.inheritFromParentWithOffset(fraction, 100);
        parentDrawable.updateBounds(bounds);

        adjustedBounds = drawable.getBounds();
        expectedBounds = new Rect(bounds);
        expectedBounds.bottom = bounds.top + (int) (HEIGHT * fraction + 100);
        assertEquals(expectedBounds, adjustedBounds);

        // inherit from parent 2
        bounds = new Rect(100, 200, WIDTH, HEIGHT);
        parentDrawable.getChildAt(0).getBoundsRule().bottom =
                ValueRule.inheritFromParent(fraction);
        parentDrawable.updateBounds(bounds);

        adjustedBounds = drawable.getBounds();
        expectedBounds = new Rect(bounds);
        expectedBounds.bottom = bounds.top + (int) ((HEIGHT - 200) * fraction);
        assertEquals(expectedBounds, adjustedBounds);
    }

    @Test
    public void updateBounds_withOverride() {
        CompositeDrawable parentDrawable = new CompositeDrawable();
        float fraction = 0.5f;
        FitWidthBitmapDrawable drawable = new FitWidthBitmapDrawable();
        drawable.setBitmap(mBitmap);
        Rect bounds = new Rect(0, 0, WIDTH, HEIGHT);
        drawable.setBounds(bounds);
        assertEquals(HEIGHT, drawable.getBounds().height());
        assertEquals(WIDTH, drawable.getBounds().width());

        parentDrawable.addChildDrawable(drawable);

        // inherit from parent
        BoundsRule boundsRule = parentDrawable.getChildAt(0).getBoundsRule();
        boundsRule.top = ValueRule.absoluteValue(-200);
        boundsRule.bottom = ValueRule.inheritFromParent(fraction);
        parentDrawable.getChildAt(0).getBoundsRule().top.setAbsoluteValue(-100);

        parentDrawable.updateBounds(bounds);

        Rect adjustedBounds = drawable.getBounds();
        Rect expectedBounds = new Rect(bounds);
        expectedBounds.top = -100;
        expectedBounds.bottom = (int) (HEIGHT * fraction);
        assertEquals(expectedBounds, adjustedBounds);

        // inherit from parent with offset
        boundsRule.bottom = ValueRule.inheritFromParentWithOffset(1f, -100);

        parentDrawable.updateBounds(bounds);

        adjustedBounds = drawable.getBounds();
        expectedBounds = new Rect(bounds);
        expectedBounds.top = -100;
        expectedBounds.bottom = HEIGHT - 100;
        assertEquals(expectedBounds, adjustedBounds);

        // using property would change type:
        CompositeDrawable.ChildDrawable.BOTTOM_ABSOLUTE.set(parentDrawable.getChildAt(0), 0);
        CompositeDrawable.ChildDrawable.BOTTOM_FRACTION.set(parentDrawable.getChildAt(0), 0.5f);
        parentDrawable.updateBounds(bounds);
        adjustedBounds = drawable.getBounds();
        expectedBounds = new Rect(bounds);
        expectedBounds.top = -100;
        expectedBounds.bottom = (int) (0.5f * HEIGHT);
        assertEquals(expectedBounds, adjustedBounds);
    }


    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    @Test
    public void constantState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CompositeDrawable parentDrawable = new CompositeDrawable();
        BitmapDrawable childDrawable = new BitmapDrawable(context.getResources(), mBitmap);
        parentDrawable.addChildDrawable(childDrawable);

        // getConstantState().newDrawable() will create a new CompositeDrawable with shared states:
        CompositeDrawable parentDrawble2 = (CompositeDrawable)
                parentDrawable.getConstantState().newDrawable();
        BitmapDrawable childDrawable2 = (BitmapDrawable) parentDrawble2.getChildAt(0).getDrawable();
        parentDrawable.setAlpha(128);
        assertEquals(128, parentDrawble2.getAlpha());
        assertEquals(128, childDrawable2.getAlpha());

        // after mutate(), parentDrawble2 will have its own state
        parentDrawble2.mutate();
        childDrawable2 = (BitmapDrawable) parentDrawble2.getChildAt(0).getDrawable();
        parentDrawable.setAlpha(64);
        assertEquals(64, parentDrawable.getAlpha());
        assertEquals(64, childDrawable.getAlpha());
        assertEquals(128, parentDrawble2.getAlpha());
        assertEquals(128, childDrawable2.getAlpha());
        childDrawable.setAlpha(100);
        assertEquals(128, parentDrawble2.getAlpha());
        assertEquals(128, childDrawable2.getAlpha());
    }

    @Test
    public void copyChildDrawableTest() {
        double delta = .005f;
        CompositeDrawable parent = new CompositeDrawable();
        FitWidthBitmapDrawable child = new FitWidthBitmapDrawable();
        parent.addChildDrawable(child);
        parent.getChildAt(0).getBoundsRule().bottom =
                ValueRule.inheritFromParentWithOffset(.5f, 100);

        CompositeDrawable.ChildDrawable newChild = new CompositeDrawable.ChildDrawable(
                parent.getChildAt(0),
                parent,
                null);
        assertEquals(100, newChild.getBoundsRule().bottom.getAbsoluteValue());
        assertEquals(.5f, newChild.getBoundsRule().bottom.getFraction(), delta);
    }

    @Test
    public void mutateTest() {
        double delta = .005f;
        CompositeDrawable parent = new CompositeDrawable();
        FitWidthBitmapDrawable child = new FitWidthBitmapDrawable();
        parent.addChildDrawable(child);
        parent.getChildAt(0).getBoundsRule().bottom =
                ValueRule.inheritFromParentWithOffset(.5f, 100);

        CompositeDrawable newDrawable = (CompositeDrawable) parent.getConstantState().newDrawable();

        parent.mutate();
        assertTrue(parent.mMutated);

        CompositeDrawable.ChildDrawable newChild = newDrawable.getChildAt(0);
        assertNotSame(parent.getChildAt(0), newChild);
        assertEquals(parent.getChildAt(0).getBoundsRule().bottom.getAbsoluteValue(),
                newChild.getBoundsRule().bottom.getAbsoluteValue());
        assertEquals(parent.getChildAt(0).getBoundsRule().bottom.getFraction(),
                newChild.getBoundsRule().bottom.getFraction(), delta);
    }

}
