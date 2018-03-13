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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.leanback.testutils.PollingCheck;

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
    public TestActivity.TestActivityTestRule mRule;

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
                return manager.mLayerDrawable != null && manager.mChangeRunnable == null
                        && !manager.mAnimator.isRunning();
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

    /**
     * Launch TestActivity without using TestActivityRule.
     */
    TestActivity launchActivity(String name, final TestActivity.Provider provider2)
            throws Throwable {
        final String providerName2 = generateProviderName(name);
        TestActivity.setProvider(providerName2, provider2);
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mRule.getActivity(), TestActivity.class);
                intent.putExtra(TestActivity.EXTRA_PROVIDER, providerName2);
                mRule.getActivity().startActivity(intent);
            }
        });

        PollingCheck.waitFor(5000/*timeout*/, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canPreProceed() {
                return false;
            }

            @Override
            public boolean canProceed() {
                return provider2.getActivity() != null && provider2.getActivity().isStarted();
            }
        });
        return provider2.getActivity();
    }

    void assertIsColorDrawable(BackgroundManager manager, int color) {
        assertNull(manager.mLayerDrawable.mWrapper[0]);
        assertTrue(manager.mLayerDrawable.getDrawable(0)
                instanceof BackgroundManager.EmptyDrawable);
        assertEquals(manager.mLayerDrawable.mWrapper[1].mAlpha, 255);
        assertEquals(((ColorDrawable) manager.mLayerDrawable.getDrawable(1)).getColor(), color);
        assertNull(manager.mBackgroundDrawable);
    }

    void assertIsBitmapDrawable(BackgroundManager manager, Bitmap bitmap) {
        assertNull(manager.mLayerDrawable.mWrapper[0]);
        assertTrue(manager.mLayerDrawable.getDrawable(0)
                instanceof BackgroundManager.EmptyDrawable);
        assertEquals(manager.mLayerDrawable.mWrapper[1].mAlpha, 255);
        assertSame(((BackgroundManager.BitmapDrawable) manager.mLayerDrawable.getDrawable(1))
                .mState.mBitmap, bitmap);
        assertSame(((BackgroundManager.BitmapDrawable) manager.mBackgroundDrawable)
                .mState.mBitmap, bitmap);
    }

    void assertIsDrawable(BackgroundManager manager, Drawable drawable) {
        assertNull(manager.mLayerDrawable.mWrapper[0]);
        assertTrue(manager.mLayerDrawable.getDrawable(0)
                instanceof BackgroundManager.EmptyDrawable);
        assertEquals(manager.mLayerDrawable.mWrapper[1].mAlpha, 255);
        assertSame(manager.mLayerDrawable.getDrawable(1), drawable);
        assertSame(manager.mBackgroundDrawable, drawable);
    }

    Bitmap createBitmap(int width, int height, int color) {
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

    @Test
    public void establishInOnAttachToWindow() throws Throwable {
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onAttachedToWindow(TestActivity activity) {
                BackgroundManager.getInstance(activity).attach(activity.getWindow());
            }

            @Override
            public void onStart(TestActivity activity) {
                BackgroundManager.getInstance(activity).setColor(Color.BLUE);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

        BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        testSwitchBackgrounds(manager);
    }

    @Test
    public void multipleSetBitmaps() throws Throwable {
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onAttachedToWindow(TestActivity activity) {
                BackgroundManager.getInstance(activity).attach(activity.getWindow());
            }

            @Override
            public void onStart(TestActivity activity) {
                BackgroundManager.getInstance(activity).setColor(Color.BLUE);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1,
                generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

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

    @Test
    public void multipleSetBitmapsAndColor() throws Throwable {
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onAttachedToWindow(TestActivity activity) {
                BackgroundManager.getInstance(activity).attach(activity.getWindow());
            }

            @Override
            public void onStart(TestActivity activity) {
                BackgroundManager.getInstance(activity).setColor(Color.BLUE);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

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

    @Test
    public void establishInOnCreate() throws Throwable {
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager.getInstance(activity).attach(activity.getWindow());
            }

            @Override
            public void onStart(TestActivity activity) {
                BackgroundManager.getInstance(activity).setColor(Color.BLUE);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

        BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        testSwitchBackgrounds(manager);
    }

    @Test
    public void establishInOnStart() throws Throwable {
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onStart(TestActivity activity) {
                BackgroundManager m = BackgroundManager.getInstance(activity);
                if (!m.isAttached()) {
                    // onStart will be called mutliple times, attach() can only be called once.
                    m.attach(activity.getWindow());
                }
                m.setColor(Color.BLUE);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

        BackgroundManager manager = BackgroundManager.getInstance(activity1);
        waitForBackgroundAnimationFinish(manager);
        assertIsColorDrawable(manager, Color.BLUE);

        testSwitchBackgrounds(manager);
    }

    @Test
    public void assignColorImmediately() throws Throwable {
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                // if we set color before attach, it will be assigned immediately
                m.setColor(Color.BLUE);
                m.attach(activity.getWindow());
                assertIsColorDrawable(m, Color.BLUE);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

        BackgroundManager manager = BackgroundManager.getInstance(activity1);

        testSwitchBackgrounds(manager);
    }

    @Test
    public void assignBitmapImmediately() throws Throwable {
        final Bitmap bitmap = createBitmap(200, 100, Color.BLUE);
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                // if we set bitmap before attach, it will be assigned immediately
                m.setBitmap(bitmap);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

        BackgroundManager manager = BackgroundManager.getInstance(activity1);

        testSwitchBackgrounds(manager);
    }


    @Test
    public void inheritBitmapByNewActivity() throws Throwable {
        final Bitmap bitmap = createBitmap(200, 100, Color.BLUE);
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                // if we set bitmap before attach, it will be assigned immediately
                m.setBitmap(bitmap);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

        TestActivity.Provider provider2 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }
        };

        TestActivity activity2 = launchActivity("activity2", provider2);
        waitForActivityStop(activity1);

        BackgroundManager manager2 = BackgroundManager.getInstance(activity2);
        assertIsBitmapDrawable(manager2, bitmap);
        activity2.finish();
    }

    @Test
    public void inheritColorByNewActivity() throws Throwable {
        final int color = Color.BLUE;
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                // if we set color before attach, it will be assigned immediately
                m.setColor(color);
                m.attach(activity.getWindow());
                assertIsColorDrawable(m, color);
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();

        TestActivity.Provider provider2 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.attach(activity.getWindow());
                assertIsColorDrawable(m, color);
            }
        };
        TestActivity activity2 = launchActivity("activity2", provider2);
        waitForActivityStop(activity1);

        BackgroundManager manager2 = BackgroundManager.getInstance(activity2);
        assertIsColorDrawable(manager2, color);
        activity2.finish();
    }

    @Test
    public void returnFromNewActivity() throws Throwable {
        final int color = Color.RED;
        final Bitmap bitmap = createBitmap(200, 100, Color.BLUE);
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                // if we set bitmap before attach, it will be assigned immediately
                m.setColor(color);
                m.setBitmap(bitmap);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }

        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);

        TestActivity.Provider provider2 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }
        };
        TestActivity activity2 = launchActivity("activity2", provider2);
        waitForActivityStop(activity1);
        final BackgroundManager manager2 = BackgroundManager.getInstance(activity2);

        final Bitmap bitmap2 = createBitmap(200, 100, Color.GREEN);
        setBitmapAndVerify(manager2, bitmap2);

        // after activity2 is launched, activity will lose its bitmap and released LayerDrawable
        assertNull(manager1.mBackgroundDrawable);
        assertNull(manager1.mLayerDrawable);

        activity2.finish();

        // when return from the other app, last drawable is cleared.
        waitForBackgroundAnimationFinish(manager1);
        assertIsColorDrawable(manager1, color);
    }

    @Test
    public void manuallyReleaseInOnStop() throws Throwable {
        final int color = Color.RED;
        final Bitmap bitmap = createBitmap(200, 100, Color.BLUE);
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.setAutoReleaseOnStop(false);
                // if we set bitmap before attach, it will be assigned immediately
                m.setColor(color);
                m.setBitmap(bitmap);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }

            @Override
            public void onStop(TestActivity activity) {
                BackgroundManager.getInstance(activity).release();
            }
        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);

        TestActivity.Provider provider2 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }
        };
        TestActivity activity2 = launchActivity("activity2", provider2);
        waitForActivityStop(activity1);
        final BackgroundManager manager2 = BackgroundManager.getInstance(activity2);

        final Bitmap bitmap2 = createBitmap(200, 100, Color.GREEN);
        setBitmapAndVerify(manager2, bitmap2);

        // after activity2 is launched, activity will lose its bitmap and released LayerDrawable
        assertNull(manager1.mBackgroundDrawable);
        assertNull(manager1.mLayerDrawable);

        activity2.finish();

        // when return from the other app, last drawable is cleared.
        waitForBackgroundAnimationFinish(manager1);
        assertIsColorDrawable(manager1, color);
    }

    @Test
    public void disableAutoRelease() throws Throwable {
        final int color = Color.RED;
        final Bitmap bitmap = createBitmap(200, 100, Color.BLUE);
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.setAutoReleaseOnStop(false);
                // if we set bitmap before attach, it will be assigned immediately
                m.setColor(color);
                m.setBitmap(bitmap);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }

        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);

        TestActivity.Provider provider2 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.attach(activity.getWindow());
                assertIsBitmapDrawable(m, bitmap);
            }
        };
        TestActivity activity2 = launchActivity("activity2", provider2);
        waitForActivityStop(activity1);
        final BackgroundManager manager2 = BackgroundManager.getInstance(activity2);

        final Bitmap bitmap2 = createBitmap(200, 100, Color.GREEN);
        setBitmapAndVerify(manager2, bitmap2);

        // after activity2 is launched, activity will keep its drawable because
        // setAutoReleaseOnStop(false)
        assertIsBitmapDrawable(manager1, bitmap);

        activity2.finish();

        // when return from the activity, it's still the same bitmap
        waitForBackgroundAnimationFinish(manager1);
        assertIsBitmapDrawable(manager1, bitmap);
    }

    @Test
    public void delayDrawableChangeUntilFullAlpha() throws Throwable {
        final Bitmap bitmap = createBitmap(200, 100, Color.BLUE);
        TestActivity.Provider provider1 = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);
                BackgroundManager m = BackgroundManager.getInstance(activity);
                m.setAutoReleaseOnStop(false);
                m.attach(activity.getWindow());
                m.setColor(Color.RED);
            }

        };
        mRule = new TestActivity.TestActivityTestRule(provider1, generateProviderName("activity1"));
        final TestActivity activity1 = mRule.launchActivity();
        final BackgroundManager manager1 = BackgroundManager.getInstance(activity1);
        assertIsColorDrawable(manager1, Color.RED);

        // when set drawable, the change will be pending because alpha is 128
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertSame(manager1.mLayerDrawable,
                        activity1.getWindow().getDecorView().getBackground());
                activity1.getWindow().getDecorView().getBackground().setAlpha(128);
                manager1.setBitmap(bitmap);
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
        assertIsBitmapDrawable(manager1, bitmap);
    }
}
