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
            TypedArray a = activity.obtainStyledAttributes(R.styleable.ActionBarWindow);
            activity.mHasActionBar = a.getBoolean(
                    R.styleable.ActionBarWindow_windowActionBar, false);
            activity.mOverlayActionBar = a.getBoolean(
                    R.styleable.ActionBarWindow_windowActionBarOverlay, false);
            a.recycle();
        }

        private void ensureSubDecor(ActionBarActivity activity) {
            if (activity.mHasActionBar && !activity.mSubDecorInstalled) {
                if (activity.mOverlayActionBar) {
                    activity.setContentView(R.layout.action_bar_decor_overlay);
                } else {
                    activity.setContentView(R.layout.action_bar_decor);
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
                activity.setContentView(v);
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
                activity.setContentView(resId);
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
                activity.setContentView(v, lp);
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
                activity.setContentView(v, lp);
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
            // Not needed; the native action bar will take care of things.
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v) {
            activity.setContentView(v);
        }

        @Override
        public void setContentView(ActionBarActivity activity, int resId) {
            activity.setContentView(resId);
        }

        @Override
        public void setContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            activity.setContentView(v, lp);
        }

        @Override
        public void addContentView(ActionBarActivity activity, View v, ViewGroup.LayoutParams lp) {
            activity.addContentView(v, lp);
        }

        @Override
        public ActionBar createActionBar(ActionBarActivity activity) {
            return new ActionBarImplHC(activity, activity);
        }

        @Override
        public void requestWindowFeature(ActionBarActivity activity, int feature) {
            activity.requestWindowFeature(feature);
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
}
