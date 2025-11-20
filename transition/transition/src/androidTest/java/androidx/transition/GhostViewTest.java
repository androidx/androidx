/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.transition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
public class GhostViewTest extends BaseTest {

    private static final int SIZE = 50;

    Context mContext;
    ViewGroup mRoot;

    @UiThreadTest
    @Before
    public void setUp() {
        mContext = rule.getActivity();
        mRoot = new FrameLayout(mContext);
        rule.getActivity().getRoot().addView(mRoot, new ViewGroup.LayoutParams(SIZE, SIZE));
        rule.getActivity().getRoot().setBackground(new ColorDrawable(Color.WHITE));
    }

    @Test
    public void testAddingViewAsGhost() throws Throwable {
        final FrameLayout parent1 = new FrameLayout(mContext);
        final View view = makeColorView(Color.RED);
        final FrameLayout parent2 = createParent2();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.addView(view);
                mRoot.addView(parent1);
                mRoot.addView(parent2);
            }
        });

        waitForDraw(view);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GhostViewUtils.addGhost(view, parent2, new Matrix());
            }
        });

        waitForDraw(parent2);
        assertColor(Color.RED, drawBitmap(parent2));
    }

    private FrameLayout createParent2() {
        FrameLayout layout = new FrameLayout(mContext);
        // we need this as overlay port on pre18 works like this:
        // if finds the view with such id's and add overlay group inside
        // with this overlays would be added into the view we want.
        layout.setId(android.R.id.content);
        return layout;
    }

    @Test
    public void testMaintainingOriginalViewOrder() throws Throwable {
        final FrameLayout parent1 = new FrameLayout(mContext);
        final View redView = makeColorView(Color.RED);
        final View greenView = makeColorView(Color.GREEN);
        final FrameLayout parent2 = createParent2();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.addView(redView);
                parent1.addView(greenView);
                mRoot.addView(parent1);
                mRoot.addView(parent2);
            }
        });

        waitForDraw(parent1);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GhostViewUtils.addGhost(greenView, parent2, new Matrix());
                GhostViewUtils.addGhost(redView, parent2, new Matrix());
            }
        });

        waitForDraw(parent2);
        assertColor(Color.GREEN, drawBitmap(parent2));
    }

    @Test
    public void testMaintainingOriginalViewOrderWithCustomOrdering() throws Throwable {
        final FrameLayout parent1 = new ReverseOrderFrameLayout(mContext);
        final View redView = makeColorView(Color.RED);
        final View greenView = makeColorView(Color.GREEN);
        final FrameLayout parent2 = createParent2();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.addView(redView);
                parent1.addView(greenView);
                mRoot.addView(parent1);
                mRoot.addView(parent2);
            }
        });

        waitForDraw(parent1);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GhostViewUtils.addGhost(greenView, parent2, new Matrix());
                GhostViewUtils.addGhost(redView, parent2, new Matrix());
            }
        });

        waitForDraw(parent2);
        assertColor(Color.RED, drawBitmap(parent2));
    }

    @Test
    public void testMaintainingOriginalViewOrderWithCustomOrderingAndZ() throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return; // no Z prior lollipop
        }
        final FrameLayout parent1 = new ReverseOrderFrameLayout(mContext);
        final View redView = makeColorView(Color.RED);
        final View greenView = makeColorView(Color.GREEN);
        greenView.setElevation(10); // it will make greenView drawn on top of redView.
        final FrameLayout parent2 = createParent2();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.addView(redView);
                parent1.addView(greenView);
                mRoot.addView(parent1);
                mRoot.addView(parent2);
            }
        });

        waitForDraw(parent1);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GhostViewUtils.addGhost(greenView, parent2, new Matrix());
                GhostViewUtils.addGhost(redView, parent2, new Matrix());
            }
        });

        waitForDraw(parent2);
        assertColor(Color.GREEN, drawBitmap(parent2));
    }

    @Test
    public void testPoppingGhostViewsOnTopOfOtherOverlayViews() throws Throwable {
        final FrameLayout parent1 = new FrameLayout(mContext);
        final View redView = makeColorView(Color.RED);
        final View greenView = makeColorView(Color.GREEN);

        final FrameLayout parent2 = createParent2();

        final View blueView = makeColorView(Color.BLUE);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.addView(redView);
                parent1.addView(greenView);
                parent1.addView(blueView);
                mRoot.addView(parent1);
                mRoot.addView(parent2);
            }
        });

        waitForDraw(parent1);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GhostViewUtils.addGhost(greenView, parent2, new Matrix());
                parent2.getOverlay().add(blueView);
                GhostViewUtils.addGhost(redView, parent2, new Matrix());
                GhostViewUtils.removeGhost(redView);
            }
        });

        waitForDraw(parent2);
        assertColor(Color.GREEN, drawBitmap(parent2));
    }

    @Test
    public void testGhostViewDrawsWithShadow() throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return; // no shadows prior lollipop
        }
        final FrameLayout parent1 = new FrameLayout(mContext);
        final View shadowView = new View(mContext);
        final FrameLayout parent2 = createParent2();
        shadowView.setElevation(1000);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.addView(shadowView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
                mRoot.addView(parent1);
                mRoot.addView(parent2);
            }
        });

        waitForDraw(shadowView);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.setVisibility(View.INVISIBLE);
                GhostViewUtils.addGhost(shadowView, parent2, new Matrix());
            }
        });

        waitForDraw(parent2);
        Bitmap bitmap = drawBitmap(parent2);
        int color = bitmap.getPixel(bitmap.getWidth() / 2, 3);
        assertNotEquals(Color.WHITE, color); // we have a shadow if the pixel is not white
    }

    @Test
    public void testGhostViewIsNotClippingChildren() throws Throwable {
        // Sometimes we apply an animation matrix for a view added into GhostView.
        // This means the view will not be drawn inside their nominal bounds -
        // [mLeft, mTop, mRight, mBottom]. If GhostViewPort has clipChildren() == true
        // then because of Canvas.quickReject(mLeft, mTop, mRight, mBottom) returning true
        // the drawing of such child would be skipped, even if in fact this child is
        // visible due to the animation matrix transformation.
        final FrameLayout parent1 = new FrameLayout(mContext);
        final View view = makeColorView(Color.RED);
        final FrameLayout parent2 = createParent2();
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent1.addView(view);
                mRoot.addView(parent1);
                mRoot.addView(parent2);
            }
        });

        waitForDraw(view);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                float offsetLargerThanSize = SIZE * 2;
                Matrix matrix = new Matrix();
                matrix.postTranslate(0f, offsetLargerThanSize);
                GhostViewUtils.addGhost(view, parent2, matrix);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // this test uses setAnimationMatrix which applies the matrix for the
                    // RenderNode starting from 21 and RenderNode is only used with the
                    // hardware acceleration. and the logic we use in drawBitmap() is drawing
                    // without the hardware acceleration for the API before 26. so instead of
                    // setAnimationMatrix we will just use TranslationY for this API versions
                    view.setTranslationY(-offsetLargerThanSize);
                } else {
                    Matrix invertedMatrix = new Matrix();
                    invertedMatrix.postTranslate(0f, -offsetLargerThanSize);
                    ViewUtils.setAnimationMatrix(view, invertedMatrix);
                }
            }
        });

        waitForDraw(parent2);
        assertColor(Color.RED, drawBitmap(parent2));
    }


    private View makeColorView(int color) {
        View view = new View(mContext);
        view.setBackground(new ColorDrawable(color));
        return view;
    }

    private void assertColor(final int color, final Bitmap bitmap) {
        int actualColor = bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        assertEquals(String.format("Expected color 0x%08X, but got 0x%08X", color, actualColor),
                color, actualColor);

    }

    private Bitmap drawBitmap(final ViewGroup view) throws Throwable {
        int width = view.getWidth();
        int height = view.getHeight();
        final Bitmap dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final CountDownLatch latch = new CountDownLatch(1);
        int[] pixelCopyResult = new int[1];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int[] offset = new int[2];
            view.getLocationInWindow(offset);
            Rect srcRect = new Rect(0, 0, width, height);
            srcRect.offset(offset[0], offset[1]);
            PixelCopy.OnPixelCopyFinishedListener onCopyFinished =
                    new PixelCopy.OnPixelCopyFinishedListener() {
                        @Override
                        public void onPixelCopyFinished(int copyResult) {
                            pixelCopyResult[0] = copyResult;
                            latch.countDown();
                        }

                    };
            PixelCopy.request(rule.getActivity().getWindow(), srcRect, dest, onCopyFinished,
                    new Handler(Looper.getMainLooper()));
        } else {
            rule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.draw(new Canvas(dest));
                    pixelCopyResult[0] = PixelCopy.SUCCESS;
                    latch.countDown();
                }
            });
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(PixelCopy.SUCCESS, pixelCopyResult[0]);
        return dest;
    }

    private void waitForDraw(final View view) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                view.getViewTreeObserver().removeOnPreDrawListener(this);
                                latch.countDown();
                                return true;
                            }
                        });
                view.invalidate();
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    private class ReverseOrderFrameLayout extends FrameLayout {

        private ReverseOrderFrameLayout(Context context) {
            super(context);
            setChildrenDrawingOrderEnabled(true);
        }

        @Override
        protected int getChildDrawingOrder(int childCount, int i) {
            return childCount - i - 1;
        }
    }
}
