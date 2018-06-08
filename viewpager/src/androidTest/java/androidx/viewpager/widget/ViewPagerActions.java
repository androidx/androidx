/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager.widget;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.hamcrest.Matcher;

public class ViewPagerActions {
    /**
     * View pager listener that serves as Espresso's {@link IdlingResource} and notifies the
     * registered callback when the view pager gets to STATE_IDLE state.
     */
    private static class CustomViewPagerListener
            implements ViewPager.OnPageChangeListener, IdlingResource {
        private int mCurrState = ViewPager.SCROLL_STATE_IDLE;

        @Nullable
        private IdlingResource.ResourceCallback mCallback;

        private boolean mNeedsIdle = false;

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            mCallback = resourceCallback;
        }

        @Override
        public String getName() {
            return "View pager listener";
        }

        @Override
        public boolean isIdleNow() {
            if (!mNeedsIdle) {
                return true;
            } else {
                return mCurrState == ViewPager.SCROLL_STATE_IDLE;
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (mCurrState == ViewPager.SCROLL_STATE_IDLE) {
                if (mCallback != null) {
                    mCallback.onTransitionToIdle();
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mCurrState = state;
            if (mCurrState == ViewPager.SCROLL_STATE_IDLE) {
                if (mCallback != null) {
                    mCallback.onTransitionToIdle();
                }
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }
    }

    private abstract static class WrappedViewAction implements ViewAction {
    }

    public static ViewAction wrap(final ViewAction baseAction) {
        if (baseAction instanceof WrappedViewAction) {
            throw new IllegalArgumentException("Don't wrap an already wrapped action");
        }

        return new WrappedViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return baseAction.getConstraints();
            }

            @Override
            public String getDescription() {
                return baseAction.getDescription();
            }

            @Override
            public void perform(UiController uiController, View view) {
                final ViewPager viewPager = (ViewPager) view;
                // Add a custom tracker listener
                final CustomViewPagerListener customListener = new CustomViewPagerListener();
                viewPager.addOnPageChangeListener(customListener);

                // Note that we're running the following block in a try-finally construct. This
                // is needed since some of the wrapped actions are going to throw (expected)
                // exceptions. If that happens, we still need to clean up after ourselves to
                // leave the system (Espesso) in a good state.
                try {
                    // Register our listener as idling resource so that Espresso waits until the
                    // wrapped action results in the view pager getting to the STATE_IDLE state
                    Espresso.registerIdlingResources(customListener);
                    baseAction.perform(uiController, view);
                    customListener.mNeedsIdle = true;
                    uiController.loopMainThreadUntilIdle();
                    customListener.mNeedsIdle = false;
                } finally {
                    // Unregister our idling resource
                    Espresso.unregisterIdlingResources(customListener);
                    // And remove our tracker listener from ViewPager
                    viewPager.removeOnPageChangeListener(customListener);
                }
            }
        };
    }

    /**
     * Scrolls <code>ViewPager</code> using arrowScroll method in a specified direction.
     */
    public static ViewAction arrowScroll(final int direction) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayingAtLeast(90);
            }

            @Override
            public String getDescription() {
                return "ViewPager arrow scroll in direction: " + direction;
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewPager viewPager = (ViewPager) view;
                viewPager.arrowScroll(direction);
                uiController.loopMainThreadUntilIdle();
            }
        });
    }

    /**
     * Moves <code>ViewPager</code> to the right by one page.
     */
    public static ViewAction scrollRight(final boolean smoothScroll) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayingAtLeast(90);
            }

            @Override
            public String getDescription() {
                return "ViewPager move one page to the right";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewPager viewPager = (ViewPager) view;
                int current = viewPager.getCurrentItem();
                viewPager.setCurrentItem(current + 1, smoothScroll);

                uiController.loopMainThreadUntilIdle();
            }
        });
    }

    /**
     * Moves <code>ViewPager</code> to the left by one page.
     */
    public static ViewAction scrollLeft(final boolean smoothScroll) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayingAtLeast(90);
            }

            @Override
            public String getDescription() {
                return "ViewPager move one page to the left";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewPager viewPager = (ViewPager) view;
                int current = viewPager.getCurrentItem();
                viewPager.setCurrentItem(current - 1, smoothScroll);

                uiController.loopMainThreadUntilIdle();
            }
        });
    }

    /**
     * Moves <code>ViewPager</code> to the last page.
     */
    public static ViewAction scrollToLast(final boolean smoothScroll) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayingAtLeast(90);
            }

            @Override
            public String getDescription() {
                return "ViewPager move to last page";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewPager viewPager = (ViewPager) view;
                int size = viewPager.getAdapter().getCount();
                if (size > 0) {
                    viewPager.setCurrentItem(size - 1, smoothScroll);
                }

                uiController.loopMainThreadUntilIdle();
            }
        });
    }

    /**
     * Moves <code>ViewPager</code> to the first page.
     */
    public static ViewAction scrollToFirst(final boolean smoothScroll) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayingAtLeast(90);
            }

            @Override
            public String getDescription() {
                return "ViewPager move to first page";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewPager viewPager = (ViewPager) view;
                int size = viewPager.getAdapter().getCount();
                if (size > 0) {
                    viewPager.setCurrentItem(0, smoothScroll);
                }

                uiController.loopMainThreadUntilIdle();
            }
        });
    }

    /**
     * Moves <code>ViewPager</code> to specific page.
     */
    public static ViewAction scrollToPage(final int page, final boolean smoothScroll) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayingAtLeast(90);
            }

            @Override
            public String getDescription() {
                return "ViewPager move to page";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewPager viewPager = (ViewPager) view;
                viewPager.setCurrentItem(page, smoothScroll);

                uiController.loopMainThreadUntilIdle();
            }
        });
    }

    /**
     * Moves <code>ViewPager</code> to specific page.
     */
    public static ViewAction setAdapter(final PagerAdapter adapter) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(ViewPager.class);
            }

            @Override
            public String getDescription() {
                return "ViewPager set adapter";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewPager viewPager = (ViewPager) view;
                viewPager.setAdapter(adapter);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Clicks between two titles in a <code>ViewPager</code> title strip
     */
    public static ViewAction clickBetweenTwoTitles(final String title1, final String title2) {
        return new GeneralClickAction(
                Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {
                        PagerTitleStrip pagerStrip = (PagerTitleStrip) view;

                        // Get the screen position of the pager strip
                        final int[] viewScreenPosition = new int[2];
                        pagerStrip.getLocationOnScreen(viewScreenPosition);

                        // Get the left / right of the first title
                        int title1Left = 0, title1Right = 0, title2Left = 0, title2Right = 0;
                        final int childCount = pagerStrip.getChildCount();
                        for (int i = 0; i < childCount; i++) {
                            final View child = pagerStrip.getChildAt(i);
                            if (child instanceof TextView) {
                                final TextView textViewChild = (TextView) child;
                                final CharSequence childText = textViewChild.getText();
                                if (title1.equals(childText)) {
                                    title1Left = textViewChild.getLeft();
                                    title1Right = textViewChild.getRight();
                                } else if (title2.equals(childText)) {
                                    title2Left = textViewChild.getLeft();
                                    title2Right = textViewChild.getRight();
                                }
                            }
                        }

                        if (title1Right < title2Left) {
                            // Title 1 is to the left of title 2
                            return new float[] {
                                    viewScreenPosition[0] + (title1Right + title2Left) / 2,
                                    viewScreenPosition[1] + pagerStrip.getHeight() / 2 };
                        } else {
                            // The assumption here is that PagerTitleStrip prevents titles
                            // from overlapping, so if we get here it means that title 1
                            // is to the right of title 2
                            return new float[] {
                                    viewScreenPosition[0] + (title2Right + title1Left) / 2,
                                    viewScreenPosition[1] + pagerStrip.getHeight() / 2 };
                        }
                    }
                },
                Press.FINGER);
    }
}
