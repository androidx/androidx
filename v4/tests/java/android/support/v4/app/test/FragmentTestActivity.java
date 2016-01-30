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
package android.support.v4.app.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.test.R;
import android.transition.Transition;
import android.transition.Transition.TransitionListener;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple activity used for Fragment Transitions and lifecycle event ordering
 */
public class FragmentTestActivity extends FragmentActivity {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_content);
    }

    public static class TestFragment extends Fragment {
        public static final int ENTER = 0;
        public static final int RETURN = 1;
        public static final int EXIT = 2;
        public static final int REENTER = 3;
        public static final int SHARED_ELEMENT_ENTER = 4;
        public static final int SHARED_ELEMENT_RETURN = 5;
        private static final int TRANSITION_COUNT = 6;

        private static final String LAYOUT_ID = "layoutId";
        private static final String TRANSITION_KEY = "transition_";
        private int mLayoutId = R.layout.fragment_start;
        private final int[] mTransitionIds = new int[] {
                android.R.transition.explode,
                android.R.transition.explode,
                android.R.transition.fade,
                android.R.transition.fade,
                android.R.transition.move,
                android.R.transition.move,
        };
        private final TransitionCalledListener[] mListeners =
                new TransitionCalledListener[TRANSITION_COUNT];
        private OnTransitionListener mOnTransitionListener;

        public TestFragment() {
            for (int i = 0; i < TRANSITION_COUNT; i++) {
                mListeners[i] = new TransitionCalledListener();
            }
        }

        public static TestFragment create(int layoutId) {
            TestFragment testFragment = new TestFragment();
            testFragment.mLayoutId = layoutId;
            return testFragment;
        }

        public void clearTransitions() {
            for (int i = 0; i < TRANSITION_COUNT; i++) {
                mTransitionIds[i] = 0;
            }
        }

        public void clearNotifications() {
            for (int i = 0; i < TRANSITION_COUNT; i++) {
                mListeners[i].transitionStarted = false;
                mListeners[i].transitionEnded = false;
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mLayoutId = savedInstanceState.getInt(LAYOUT_ID, mLayoutId);
                for (int i = 0; i < TRANSITION_COUNT; i++) {
                    String key = TRANSITION_KEY + i;
                    mTransitionIds[i] = savedInstanceState.getInt(key, mTransitionIds[i]);
                }
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(LAYOUT_ID, mLayoutId);
            for (int i = 0; i < TRANSITION_COUNT; i++) {
                String key = TRANSITION_KEY + i;
                outState.putInt(key, mTransitionIds[i]);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(mLayoutId, container, false);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if (VERSION.SDK_INT > VERSION_CODES.KITKAT) {
                setEnterTransition(loadTransition(ENTER));
                setReenterTransition(loadTransition(REENTER));
                setExitTransition(loadTransition(EXIT));
                setReturnTransition(loadTransition(RETURN));
                setSharedElementEnterTransition(loadTransition(SHARED_ELEMENT_ENTER));
                setSharedElementReturnTransition(loadTransition(SHARED_ELEMENT_RETURN));
            }
        }

        public void setOnTransitionListener(OnTransitionListener listener) {
            mOnTransitionListener = listener;
        }

        public boolean wasStartCalled(int transitionKey) {
            return mListeners[transitionKey].transitionStarted;
        }

        public boolean wasEndCalled(int transitionKey) {
            return mListeners[transitionKey].transitionEnded;
        }

        private Transition loadTransition(int key) {
            final int id = mTransitionIds[key];
            if (id == 0) {
                return null;
            }
            Transition transition = TransitionInflater.from(getActivity()).inflateTransition(id);
            transition.addListener(mListeners[key]);
            return transition;
        }

        private void notifyTransition() {
            if (mOnTransitionListener != null) {
                mOnTransitionListener.onTransition(this);
            }
        }

        private class TransitionCalledListener implements TransitionListener {
            public boolean transitionStarted;
            public boolean transitionEnded;

            public TransitionCalledListener() {
            }

            @Override
            public void onTransitionStart(Transition transition) {
                transitionStarted = true;
                notifyTransition();
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                transitionEnded = true;
                notifyTransition();
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        }
    }

    public interface OnTransitionListener {
        void onTransition(TestFragment fragment);
    }

    public static class ParentFragment extends Fragment {
        public boolean wasAttachedInTime;
        public ChildFragment childFragment;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            childFragment = new ChildFragment();
            FragmentManager fm = getChildFragmentManager();
            fm.beginTransaction().add(childFragment, "foo").commit();
            fm.executePendingTransactions();
            wasAttachedInTime = childFragment.attached;
        }
    }

    public static class ChildFragment extends Fragment {
        public boolean attached;
        public boolean onActivityResultCalled;
        public int onActivityResultRequestCode;
        public int onActivityResultResultCode;

        @Override
        public void onAttach(Context activity) {
            super.onAttach(activity);
            attached = true;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            onActivityResultCalled = true;
            onActivityResultRequestCode = requestCode;
            onActivityResultResultCode = resultCode;
        }
    }
}
