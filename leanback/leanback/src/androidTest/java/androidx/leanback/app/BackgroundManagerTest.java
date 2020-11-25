/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.leanback.testutils.PollingCheck;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BackgroundManagerTest {

    @Rule
    public TestName mUnitTestName = new TestName();

    @Rule
    public final TestActivity.TestActivityTestRule mRule = new TestActivity.TestActivityTestRule();

    @Rule
    public final TestActivity.TestActivityTestRule2 mRule2 =
            new TestActivity.TestActivityTestRule2();

    String generateProviderName(String name) {
        return mUnitTestName.getMethodName() + "_" + name;
    }

    void waitForActivityStop(final TestActivity activity) {
        PollingCheck.waitFor(5000/* timeout */, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canPreProceed() {
                return false;
            }

            @Override
            public boolean canProceed() {
                // two step: first ChangeRunnable gets run.
                // then Animator is finished.
                return !activity.isStarted();
            }
        });
    }

    void waitForBackgroundAnimationFinish(final BackgroundManager manager) {
        PollingCheck.waitFor(5000/* timeout */, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canPreProceed() {
                return false;
            }

            @Override
            public boolean canProceed() {
                // two step: first ChangeRunnable gets run.
                // then Animator is finished.
                boolean[] finished = new boolean[1];
                try {
                    mRule.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finished[0] = manager.mLayerDrawable != null
                                    && manager.mChangeRunnable == null
                                    && !manager.mAnimator.isRunning()
                                    && manager.mLayerDrawable.mWrapper[0] == null;
                        }
                    });
                } catch (Throwable ex) {
                    return false;
                }
                return finished[0];
            }
        });
    }

    void setBitmapAndVerify(final BackgroundManager manager, final  Bitmap bitmap)
            throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setBitmap(bitmap);
            }
        });
        waitForBackgroundAnimationFinish(manager);
        assertIsBitmapDrawable(manager, bitmap);
    }

    void setDrawableAndVerify(final BackgroundManager manager, final Drawable drawable)
            throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setDrawable(drawable);
            }
        });
        waitForBackgroundAnimationFinish(manager);
        assertIsDrawable(manager, drawable);
    }

    void setBitmapNullAndVerifyColor(final BackgroundManager manager, final int color)
            throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setBitmap(null);
            }
        });
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, color);
    }

    void setDrawableNullAndVerifyColor(final BackgroundManager manager, final int color)
            throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setDrawable(null);
            }
        });
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, color);
    }

    void setColorAndVerify(final BackgroundManager manager, final  int color)
            throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setColor(color);
            }
        });
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, color);
    }

    static void assertIsColorDrawable(BackgroundManager manager, int color) {
        assertNull(manager.mLayerDrawable.mWrapper[0]);
        assertTrue(manager.mLayerDrawable.getDrawable(0)
                instanceof BackgroundManager.EmptyDrawable);
        assertEquals(manager.mLayerDrawable.mWrapper[1].mAlpha, 255);
        assertEquals(((ColorDrawable) manager.mLayerDrawable.getDrawable(1)).getColor(), color);
        assertNull(manager.mBackgroundDrawable);
    }

    static void assertIsBitmapDrawable(BackgroundManager manager, Bitmap bitmap) {
        assertNull(manager.mLayerDrawable.mWrapper[0]);
        assertTrue(manager.mLayerDrawable.getDrawable(0)
                instanceof BackgroundManager.EmptyDrawable);
        assertEquals(manager.mLayerDrawable.mWrapper[1].mAlpha, 255);
        assertSame(((BackgroundManager.BitmapDrawable) manager.mLayerDrawable.getDrawable(1))
                .mState.mBitmap, bitmap);
        assertSame(((BackgroundManager.BitmapDrawable) manager.mBackgroundDrawable)
                .mState.mBitmap, bitmap);
    }

    static void assertIsDrawable(BackgroundManager manager, Drawable drawable) {
        assertNull(manager.mLayerDrawable.mWrapper[0]);
        assertTrue(manager.mLayerDrawable.getDrawable(0)
                instanceof BackgroundManager.EmptyDrawable);
        assertEquals(manager.mLayerDrawable.mWrapper[1].mAlpha, 255);
        assertSame(manager.mLayerDrawable.getDrawable(1), drawable);
        assertSame(manager.mBackgroundDrawable, drawable);
    }

    static Bitmap createBitmap(int width, int height, int color) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0, 0, width, height, paint);
        return bitmap;
    }

    Drawable createDrawable(int width, int height, int color) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0, 0, width, height, paint);
        return new BitmapDrawable(mRule.getActivity().getResources(), bitmap);
    }

    void testSwitchBackgrounds(final BackgroundManager manager) throws Throwable {
        setBitmapAndVerify(manager, createBitmap(200, 100, Color.GREEN));

        // Drawable -> Drawable
        setDrawableAndVerify(manager, createDrawable(200, 100, Color.MAGENTA));

        setBitmapAndVerify(manager, createBitmap(200, 100, Color.GRAY));

        // Drawable -> Color
        setColorAndVerify(manager, Color.RED);

        // Color -> Drawable
        setBitmapAndVerify(manager, createBitmap(200, 100, Color.BLACK));

        // Set Drawable to null -> show last Color
        setBitmapNullAndVerifyColor(manager, Color.RED);

        // Color -> Drawable
        setBitmapAndVerify(manager, createBitmap(200, 100, Color.MAGENTA));
    }

    public static class EstablishInOnAttachToWindow extends TestActivity.Provider {
        @Override
        public void onAttachedToWindow(TestActivity activity) {
            BackgroundManager.getInstance(activity).attach(activity.getWindow());
        }

        @Override
        public void onStart(TestActivity activity) {
            BackgroundManager.getInstance(activity).setColor(Color.BLUE);
        }
    }

    @Test
    public void establishInOnAttachToWindow() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(EstablishInOnAttachToWindow.class);

        BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        testSwitchBackgrounds(manager);
    }

    public static class MultipleSetBitmaps extends TestActivity.Provider {
        @Override
        public void onAttachedToWindow(TestActivity activity) {
            BackgroundManager.getInstance(activity).attach(activity.getWindow());
        }

        @Override
        public void onStart(TestActivity activity) {
            BackgroundManager.getInstance(activity).setColor(Color.BLUE);
        }
    }

    @Test
    public void multipleSetBitmaps() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(MultipleSetBitmaps.class);

        final BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        final Bitmap bitmap1 = createBitmap(200, 100, Color.RED);
        final Bitmap bitmap2 = createBitmap(200, 100, Color.GRAY);
        final Bitmap bitmap3 = createBitmap(200, 100, Color.GREEN);
        final Bitmap bitmap4 = createBitmap(200, 100, Color.MAGENTA);
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setBitmap(bitmap1);
                manager.setBitmap(bitmap2);
                manager.setBitmap(bitmap3);
                manager.setBitmap(bitmap4);
            }
        });
        waitForBackgroundAnimationFinish(manager);
        assertIsBitmapDrawable(manager, bitmap4);
    }

    public static class MultipleSetBitmapsAndColor extends TestActivity.Provider {
        @Override
        public void onAttachedToWindow(TestActivity activity) {
            BackgroundManager.getInstance(activity).attach(activity.getWindow());
        }

        @Override
        public void onStart(TestActivity activity) {
            BackgroundManager.getInstance(activity).setColor(Color.BLUE);
        }
    }

    @Test
    public void multipleSetBitmapsAndColor() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(MultipleSetBitmapsAndColor.class);

        final BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        final Bitmap bitmap1 = createBitmap(200, 100, Color.RED);
        final Bitmap bitmap2 = createBitmap(200, 100, Color.GRAY);
        final Bitmap bitmap3 = createBitmap(200, 100, Color.GREEN);
        final int color = Color.MAGENTA;
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setBitmap(bitmap1);
                manager.setBitmap(bitmap2);
            }
        });
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                manager.setBitmap(bitmap3);
                manager.setColor(color);
            }
        });
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, color);
    }

    public static class EstablishInOnCreate extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager.getInstance(activity).attach(activity.getWindow());
        }

        @Override
        public void onStart(TestActivity activity) {
            BackgroundManager.getInstance(activity).setColor(Color.BLUE);
        }
    }

    @Test
    public void establishInOnCreate() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(EstablishInOnCreate.class);

        BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        testSwitchBackgrounds(manager);
    }

    public static class EstablishInOnStart extends TestActivity.Provider {
        @Override
        public void onStart(TestActivity activity) {
            BackgroundManager m = BackgroundManager.getInstance(activity);
            if (!m.isAttached()) {
                // onStart will be called multiple times, attach() can only be called once.
                m.attach(activity.getWindow());
            }
            m.setColor(Color.BLUE);
        }
    }

    @Test
    public void establishInOnStart() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(EstablishInOnStart.class);

        BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        testSwitchBackgrounds(manager);
    }

    public static class AssignColorImmediately extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            // if we set color before attach, it will be assigned immediately
            m.setColor(Color.BLUE);
            m.attach(activity.getWindow());
            assertIsColorDrawable(m, Color.BLUE);
        }
    }

    @Test
    public void assignColorImmediately() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(AssignColorImmediately.class);

        BackgroundManager manager = BackgroundManager.getInstance(activity1);

        testSwitchBackgrounds(manager);
    }

    public static class AssignBitmapImmediately extends TestActivity.Provider {
        final Bitmap mBitmap = createBitmap(200, 100, Color.BLUE);

        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            // if we set bitmap before attach, it will be assigned immediately
            m.setBitmap(mBitmap);
            m.attach(activity.getWindow());
            assertIsBitmapDrawable(m, mBitmap);
        }
    }

    @Test
    public void assignBitmapImmediately() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(AssignBitmapImmediately.class);

        BackgroundManager manager = BackgroundManager.getInstance(activity1);

        testSwitchBackgrounds(manager);
    }

    public static class InheritBitmapByNewActivity extends TestActivity.Provider {
        final Bitmap mBitmap = createBitmap(200, 100, Color.BLUE);
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            // if we set bitmap before attach, it will be assigned immediately
            m.setBitmap(mBitmap);
            m.attach(activity.getWindow());
            assertIsBitmapDrawable(m, mBitmap);
        }
    }

    public static class InheritBitmapByNewActivity_2 extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.attach(activity.getWindow());
        }
    };
    @Test
    public void inheritBitmapByNewActivity() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(InheritBitmapByNewActivity.class);

        InheritBitmapByNewActivity provider = (InheritBitmapByNewActivity) activity1.getProvider();
        TestActivity activity2 = mRule2.launchActivity(InheritBitmapByNewActivity_2.class);

        waitForActivityStop(activity1);
        assertIsBitmapDrawable(BackgroundManager.getInstance(activity2), provider.mBitmap);

        BackgroundManager manager2 = BackgroundManager.getInstance(activity2);
        assertIsBitmapDrawable(manager2, provider.mBitmap);
        activity2.finish();
    }

    public static class InheritColorByNewActivity extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            // if we set color before attach, it will be assigned immediately
            m.setColor(Color.BLUE);
            m.attach(activity.getWindow());
            assertIsColorDrawable(m, Color.BLUE);
        }
    }

    public static class InheritColorByNewActivity2 extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.attach(activity.getWindow());
            assertIsColorDrawable(m, Color.BLUE);
        }
    };

    @Test
    public void inheritColorByNewActivity() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(InheritColorByNewActivity.class);

        TestActivity activity2 = mRule2.launchActivity(InheritColorByNewActivity2.class);
        waitForActivityStop(activity1);

        BackgroundManager manager2 = BackgroundManager.getInstance(activity2);
        assertIsColorDrawable(manager2, Color.BLUE);
        activity2.finish();
    }

    public static class ReturnFromNewActivity extends TestActivity.Provider {
        final Bitmap mBitmap = createBitmap(200, 100, Color.BLUE);

        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            // if we set bitmap before attach, it will be assigned immediately
            m.setColor(Color.RED);
            m.setBitmap(mBitmap);
            m.attach(activity.getWindow());
            assertIsBitmapDrawable(m, mBitmap);
        }
    }

    public static class ReturnFromNewActivity2 extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.attach(activity.getWindow());
        }
    }

    @Test
    public void returnFromNewActivity() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(ReturnFromNewActivity.class);
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);

        ReturnFromNewActivity provider = (ReturnFromNewActivity) activity1.getProvider();
        TestActivity activity2 = mRule2.launchActivity(ReturnFromNewActivity2.class);
        waitForActivityStop(activity1);

        final BackgroundManager manager2 = BackgroundManager.getInstance(activity2);
        assertIsBitmapDrawable(manager2, provider.mBitmap);

        final Bitmap bitmap2 = createBitmap(200, 100, Color.GREEN);
        setBitmapAndVerify(manager2, bitmap2);

        // after activity2 is launched, activity will lose its bitmap and released LayerDrawable
        assertNull(manager1.mBackgroundDrawable);
        assertNull(manager1.mLayerDrawable);

        activity2.finish();

        // when return from the other app, last drawable is cleared.
        waitForBackgroundAnimationFinish(manager1);
        assertIsColorDrawable(manager1, Color.RED);
    }

    public static class ManuallyReleaseInOnStop extends TestActivity.Provider {
        final Bitmap mBitmap = createBitmap(200, 100, Color.BLUE);
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.setAutoReleaseOnStop(false);
            // if we set bitmap before attach, it will be assigned immediately
            m.setColor(Color.RED);
            m.setBitmap(mBitmap);
            m.attach(activity.getWindow());
            assertIsBitmapDrawable(m, mBitmap);
        }

        @Override
        public void onStop(TestActivity activity) {
            BackgroundManager.getInstance(activity).release();
        }
    }

    public static class ManuallyReleaseInOnStop2 extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.attach(activity.getWindow());
        }
    }

    @Test
    public void manuallyReleaseInOnStop() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(ManuallyReleaseInOnStop.class);
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);

        ManuallyReleaseInOnStop provider = (ManuallyReleaseInOnStop) activity1.getProvider();
        TestActivity activity2 = mRule2.launchActivity(ManuallyReleaseInOnStop2.class);
        waitForActivityStop(activity1);
        final BackgroundManager manager2 = BackgroundManager.getInstance(activity2);
        assertIsBitmapDrawable(manager2, provider.mBitmap);

        final Bitmap bitmap2 = createBitmap(200, 100, Color.GREEN);
        setBitmapAndVerify(manager2, bitmap2);

        // after activity2 is launched, activity will lose its bitmap and released LayerDrawable
        assertNull(manager1.mBackgroundDrawable);
        assertNull(manager1.mLayerDrawable);

        activity2.finish();

        // when return from the other app, last drawable is cleared.
        waitForBackgroundAnimationFinish(manager1);
        assertIsColorDrawable(manager1, Color.RED);
    }

    public static class DisableAutoRelease extends TestActivity.Provider {
        final Bitmap mBitmap = createBitmap(200, 100, Color.BLUE);
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.setAutoReleaseOnStop(false);
            // if we set bitmap before attach, it will be assigned immediately
            m.setColor(Color.RED);
            m.setBitmap(mBitmap);
            m.attach(activity.getWindow());
            assertIsBitmapDrawable(m, mBitmap);
        }

    }

    public static class DisableAutoRelease2 extends TestActivity.Provider {
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.attach(activity.getWindow());
        }
    }

    @Test
    public void disableAutoRelease() throws Throwable {
        final TestActivity activity1 = mRule.launchActivity(DisableAutoRelease.class);
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);

        DisableAutoRelease provider = (DisableAutoRelease) activity1.getProvider();
        TestActivity activity2 = mRule2.launchActivity(DisableAutoRelease2.class);
        waitForActivityStop(activity1);
        final BackgroundManager manager2 = BackgroundManager.getInstance(activity2);
        assertIsBitmapDrawable(manager2, provider.mBitmap);

        final Bitmap bitmap2 = createBitmap(200, 100, Color.GREEN);
        setBitmapAndVerify(manager2, bitmap2);

        // after activity2 is launched, activity will keep its drawable because
        // setAutoReleaseOnStop(false)
        assertIsBitmapDrawable(manager1, provider.mBitmap);

        activity2.finish();

        // when return from the activity, it's still the same bitmap
        waitForBackgroundAnimationFinish(manager1);
        assertIsBitmapDrawable(manager1, provider.mBitmap);
    }

    public static class DelayDrawableChangeUntilFullAlpha extends TestActivity.Provider {
        final Bitmap mBitmap = createBitmap(200, 100, Color.BLUE);
        @Override
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            super.onCreate(activity, savedInstanceState);
            BackgroundManager m = BackgroundManager.getInstance(activity);
            m.setAutoReleaseOnStop(false);
            m.attach(activity.getWindow());
            m.setColor(Color.RED);
        }

    }

    @Test
    public void delayDrawableChangeUntilFullAlpha() throws Throwable {
        final TestActivity activity1 =
                mRule.launchActivity(DelayDrawableChangeUntilFullAlpha.class);
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);
        assertIsColorDrawable(manager1, Color.RED);

        DelayDrawableChangeUntilFullAlpha provider =
                (DelayDrawableChangeUntilFullAlpha) activity1.getProvider();
        // when set drawable, the change will be pending because alpha is 128
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertSame(manager1.mLayerDrawable,
                        activity1.getWindow().getDecorView().getBackground());
                activity1.getWindow().getDecorView().getBackground().setAlpha(128);
                manager1.setBitmap(provider.mBitmap);
            }
        });
        assertEquals(Color.RED,
                ((ColorDrawable) manager1.mLayerDrawable.getDrawable(1)).getColor());

        // Pending updates executed when set to FULL_ALPHA
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity1.getWindow().getDecorView().getBackground().setAlpha(
                        BackgroundManager.FULL_ALPHA);
            }
        });
        waitForBackgroundAnimationFinish(manager1);
        assertIsBitmapDrawable(manager1, provider.mBitmap);
    }
}
