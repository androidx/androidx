/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.appcompat.app;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.appcompat.R;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ActionBarActivity extends FragmentActivity implements ActionBar.Callback {

    private static final int FEATURE_ACTION_BAR = 8;
    private static final int FEATURE_ACTION_BAR_OVERLAY = 9;

    interface ActionBarActivityImpl {
        void onCreate(ActionBarActivity activity, Bundle savedInstanceState);
        void setContentView(ActionBarActivity activity, View v);
        void setContentView(ActionBarActivity activity, int resId);
        void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp);
        void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp);
        ActionBar createActionBar(ActionBarActivity activity);
        void requestWindowFeature(ActionBarActivity activity, int feature);
    }

    static class ActionBarActivityImplBase implements ActionBarActivityImpl {

        @Override
        public void onCreate(ActionBarActivity activity, Bundle savedInstanceState) {
        }

        private void ensureSubDecor(ActionBarActivity activity) {
            if (activity.mHasActionBar && !activity.mSubDecorInstalled) {
                if (activity.mOverlayActionBar) {
                    activity.superSetContentView(R.layout.action_bar_decor_overlay);
                } else {
                    activity.superSetContentView(R.layout.action_bar_decor);
                }
            }
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                contentParent.addView(v);
            } else {
                activity.superSetContentView(v);
            }
        }

        @Override
        public void setContentView(ActionBarActivity activity, int resId) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                final LayoutInflater inflater = activity.getLayoutInflater();
                inflater.inflate(resId, contentParent);
            } else {
                activity.superSetContentView(resId);
            }
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.removeAllViews();
                contentParent.addView(v, lp);
            } else {
                activity.superSetContentView(v, lp);
            }
        }

        @Override
        public void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            ensureSubDecor(activity);
            if (activity.mHasActionBar) {
                final ViewGroup contentParent =
                        (ViewGroup) activity.findViewById(R.id.action_bar_activity_content);
                contentParent.addView(v, lp);
            } else {
                activity.superSetContentView(v, lp);
            }
        }

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplCompat(activity, activity);
        }

        @Override
        public void requestWindowFeature(ActionBarActivity activity, int feature) {
            if (feature == FEATURE_ACTION_BAR) {
                activity.mHasActionBar = true;
            } else if (feature == FEATURE_ACTION_BAR_OVERLAY) {
                activity.mOverlayActionBar = true;
            }
        }

    }

    static class ActionBarActivityImplHC implements ActionBarActivityImpl {

        @Override
        public void onCreate(ActionBarActivity activity, Bundle savedInstanceState) {
            if (activity.mHasActionBar) {
                // If action bar is requested by inheriting from the appcompat theme,
                // the system will not know about that. So explicitly request for an action bar.
                activity.superRequestWindowFeature(FEATURE_ACTION_BAR);
            }
            if (activity.mOverlayActionBar) {
                activity.superRequestWindowFeature(FEATURE_ACTION_BAR_OVERLAY);
            }
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v) {
            activity.superSetContentView(v);
        }

        @Override
        public void setContentView(ActionBarActivity activity, int resId) {
            activity.superSetContentView(resId);
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            activity.superSetContentView(v, lp);
        }

        @Override
        public void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            activity.superAddContentView(v, lp);
        }

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplHC(activity, activity);
        }

        @Override
        public void requestWindowFeature(ActionBarActivity activity, int feature) {
            activity.superRequestWindowFeature(feature);
        }

    }

    static class ActionBarActivityImplICS extends ActionBarActivityImplHC {

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplICS(activity, activity);
        }
    }

    static final ActionBarActivityImpl IMPL;

    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 14) {
            IMPL = new ActionBarActivityImplICS();
        } else if (version >= 11) {
            IMPL = new ActionBarActivityImplHC();
        } else {
            IMPL = new ActionBarActivityImplBase();
        }
    }

    private ActionBar mActionBar;

    // true if the compatibility implementation has installed a window sub-decor layout.
    boolean mSubDecorInstalled;

    // true if this activity has an action bar.
    boolean mHasActionBar;

    // true if this activity's action bar overlays other activity content.
    boolean mOverlayActionBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TypedArray a = obtainStyledAttributes(R.styleable.ActionBarWindow);
        mHasActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBar, false);
        mOverlayActionBar = a.getBoolean(R.styleable.ActionBarWindow_windowActionBarOverlay,
                false);
        a.recycle();

        IMPL.onCreate(this, savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        initActionBar();
        return mActionBar;
    }

    private void initActionBar() {
        if (mActionBar == null && mHasActionBar) {
            mActionBar = IMPL.createActionBar(this);
        }
    }

    /**
     * Set the activity content from a layout resource.  The resource will be inflated, adding all
     * top-level views to the activity.
     *
     * @param layoutResID Resource ID to be inflated.
     * @see #setContentView(android.view.View)
     * @see #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)
     */
    @Override
    public void setContentView(int layoutResID) {
        IMPL.setContentView(this, layoutResID);
    }

    /**
     * Set the activity content to an explicit view.  This view is placed directly into the
     * activity's view hierarchy.  It can itself be a complex view hierarchy.  When calling this
     * method, the layout parameters of the specified view are ignored.  Both the width and the
     * height of the view are set by default to {@link ViewGroup.LayoutParams#MATCH_PARENT}. To use
     * your own layout parameters, invoke {@link #setContentView(android.view.View,
     * android.view.ViewGroup.LayoutParams)} instead.
     *
     * @param view The desired content to display.
     * @see #setContentView(int)
     * @see #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)
     */
    @Override
    public void setContentView(View view) {
        IMPL.setContentView(this, view);
    }

    /**
     * Set the activity content to an explicit view.  This view is placed directly into the
     * activity's view hierarchy.  It can itself be a complex view hierarchy.
     *
     * @param view   The desired content to display.
     * @param params Layout parameters for the view.
     * @see #setContentView(android.view.View)
     * @see #setContentView(int)
     */
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        IMPL.setContentView(this, view, params);
    }

    void superSetContentView(int resId) {
        super.setContentView(resId);
    }

    void superSetContentView(View v) {
        super.setContentView(v);
    }

    void superSetContentView(View v, ViewGroup.LayoutParams lp) {
        super.setContentView(v, lp);
    }

    void superAddContentView(View v, ViewGroup.LayoutParams lp) {
        super.addContentView(v, lp);
    }

    void superRequestWindowFeature(int feature) {
        super.requestWindowFeature(feature);
    }
}
