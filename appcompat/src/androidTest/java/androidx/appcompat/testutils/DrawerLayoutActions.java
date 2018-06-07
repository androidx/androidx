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

package androidx.appcompat.testutils;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import org.hamcrest.Matcher;

public class DrawerLayoutActions {
    /**
     * Drawer listener that serves as Espresso's {@link IdlingResource} and notifies the registered
     * callback when the drawer gets to STATE_IDLE state.
     */
    private static class CustomDrawerListener
            implements DrawerLayout.DrawerListener, IdlingResource {
        private int mCurrState = DrawerLayout.STATE_IDLE;

        @Nullable private IdlingResource.ResourceCallback mCallback;

        private boolean mNeedsIdle = false;

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            mCallback = resourceCallback;
        }

        @Override
        public String getName() {
            return "Drawer listener";
        }

        @Override
        public boolean isIdleNow() {
            if (!mNeedsIdle) {
                return true;
            } else {
                return mCurrState == DrawerLayout.STATE_IDLE;
            }
        }

        @Override
        public void onDrawerClosed(View drawer) {
            if (mCurrState == DrawerLayout.STATE_IDLE) {
                if (mCallback != null) {
                    mCallback.onTransitionToIdle();
                }
            }
        }

        @Override
        public void onDrawerOpened(View drawer) {
            if (mCurrState == DrawerLayout.STATE_IDLE) {
                if (mCallback != null) {
                    mCallback.onTransitionToIdle();
                }
            }
        }

        @Override
        public void onDrawerSlide(View drawer, float slideOffset) {
        }

        @Override
        public void onDrawerStateChanged(int state) {
            mCurrState = state;
            if (state == DrawerLayout.STATE_IDLE) {
                if (mCallback != null) {
                    mCallback.onTransitionToIdle();
                }
            }
        }
    }

    private abstract static class WrappedViewAction implements ViewAction {
    }

    public static ViewAction wrap(final ViewAction baseAction) {
        if (baseAction instanceof WrappedViewAction) {
            throw new IllegalArgumentException("Don't wrap and already wrapped action");
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
            public final void perform(UiController uiController, View view) {
                final DrawerLayout drawer = (DrawerLayout) view;
                // Add a custom tracker listener
                final CustomDrawerListener customListener = new CustomDrawerListener();
                drawer.addDrawerListener(customListener);

                // Note that we're running the following block in a try-finally construct. This
                // is needed since some of the wrapped actions are going to throw (expected)
                // exceptions. If that happens, we still need to clean up after ourselves to
                // leave the system (Espesso) in a good state.
                try {
                    // Register our listener as idling resource so that Espresso waits until the
                    // wrapped action results in the drawer getting to the STATE_IDLE state
                    Espresso.registerIdlingResources(customListener);
                    baseAction.perform(uiController, view);
                    customListener.mNeedsIdle = true;
                    uiController.loopMainThreadUntilIdle();
                    customListener.mNeedsIdle = false;
                } finally {
                    // Unregister our idling resource
                    Espresso.unregisterIdlingResources(customListener);
                    // And remove our tracker listener from DrawerLayout
                    drawer.removeDrawerListener(customListener);
                }
            }
        };
    }

    /**
     * Opens the drawer at the specified edge gravity.
     */
    public static ViewAction openDrawer(final int drawerEdgeGravity, final boolean animate) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Opens the drawer";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.openDrawer(drawerEdgeGravity, animate);
            }
        });
    }

    /**
     * Opens the drawer at the specified edge gravity.
     */
    public static ViewAction openDrawer(final int drawerEdgeGravity) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Opens the drawer";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.openDrawer(drawerEdgeGravity);
            }
        });
    }

    /**
     * Opens the drawer.
     */
    public static ViewAction openDrawer(final View drawerView) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Opens the drawer";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.openDrawer(drawerView);
            }
        });
    }

    /**
     * Closes the drawer at the specified edge gravity.
     */
    public static ViewAction closeDrawer(final int drawerEdgeGravity) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Closes the drawer";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.closeDrawer(drawerEdgeGravity);
            }
        });
    }

    /**
     * Closes the drawer at the specified edge gravity.
     */
    public static ViewAction closeDrawer(final int drawerEdgeGravity, final boolean animate) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Closes the drawer";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.closeDrawer(drawerEdgeGravity, animate);
            }
        });
    }

    /**
     * Closes the drawer.
     */
    public static ViewAction closeDrawer(final View drawerView) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Closes the drawer";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.closeDrawer(drawerView);
            }
        });
    }

    /**
     * Sets the lock mode for the drawer at the specified edge gravity.
     */
    public static ViewAction setDrawerLockMode(final int lockMode, final int drawerEdgeGravity) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets drawer lock mode";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.setDrawerLockMode(lockMode, drawerEdgeGravity);

                uiController.loopMainThreadUntilIdle();
            }
        });
    }

    /**
     * Sets the lock mode for the drawer.
     */
    public static ViewAction setDrawerLockMode(final int lockMode, final View drawerView) {
        return wrap(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DrawerLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets drawer lock mode";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                DrawerLayout drawerLayout = (DrawerLayout) view;
                drawerLayout.setDrawerLockMode(lockMode, drawerView);

                uiController.loopMainThreadUntilIdle();
            }
        });
    }
}
