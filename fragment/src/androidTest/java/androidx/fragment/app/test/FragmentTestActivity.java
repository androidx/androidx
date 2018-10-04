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
package androidx.fragment.app.test;

import static org.junit.Assert.assertFalse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.Transition.TransitionListener;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.test.R;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A simple activity used for Fragment Transitions and lifecycle event ordering
 */
public class FragmentTestActivity extends FragmentActivity {
    public final CountDownLatch onDestroyLatch = new CountDownLatch(1);

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_content);
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("finishEarly", false)) {
            finish();
            getSupportFragmentManager().beginTransaction()
                    .add(new AssertNotDestroyed(), "not destroyed")
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onDestroyLatch.countDown();
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
                R.transition.fade,
                R.transition.fade,
                R.transition.fade,
                R.transition.fade,
                R.transition.change_bounds,
                R.transition.change_bounds,
        };
        private final Object[] mListeners = new Object[TRANSITION_COUNT];

        public TestFragment() {
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                for (int i = 0; i < TRANSITION_COUNT; i++) {
                    mListeners[i] = new TransitionCalledListener();
                }
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
                ((TransitionCalledListener)mListeners[i]).startLatch = new CountDownLatch(1);
                ((TransitionCalledListener)mListeners[i]).endLatch = new CountDownLatch(1);
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

        public boolean wasStartCalled(int transitionKey) {
            return ((TransitionCalledListener)mListeners[transitionKey]).startLatch.getCount() == 0;
        }

        public boolean wasEndCalled(int transitionKey) {
            return ((TransitionCalledListener)mListeners[transitionKey]).endLatch.getCount() == 0;
        }

        public boolean waitForStart(int transitionKey)
                throws InterruptedException {
            TransitionCalledListener l = ((TransitionCalledListener)mListeners[transitionKey]);
            return l.startLatch.await(500,TimeUnit.MILLISECONDS);
        }

        public boolean waitForEnd(int transitionKey)
                throws InterruptedException {
            TransitionCalledListener l = ((TransitionCalledListener)mListeners[transitionKey]);
            return l.endLatch.await(500,TimeUnit.MILLISECONDS);
        }

        private Transition loadTransition(int key) {
            final int id = mTransitionIds[key];
            if (id == 0) {
                return null;
            }
            Transition transition = TransitionInflater.from(getActivity()).inflateTransition(id);
            transition.addListener(((TransitionCalledListener)mListeners[key]));
            return transition;
        }

        private class TransitionCalledListener implements TransitionListener {
            public CountDownLatch startLatch = new CountDownLatch(1);
            public CountDownLatch endLatch = new CountDownLatch(1);

            public TransitionCalledListener() {
            }

            @Override
            public void onTransitionStart(Transition transition) {
                startLatch.countDown();
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                endLatch.countDown();
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

    public static class ParentFragment extends Fragment {
        static final String CHILD_FRAGMENT_TAG = "childFragment";
        public boolean wasAttachedInTime;

        private boolean mRetainChild;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            ChildFragment f = getChildFragment();
            if (f == null) {
                f = new ChildFragment();
                if (mRetainChild) {
                    f.setRetainInstance(true);
                }
                getChildFragmentManager().beginTransaction().add(f, CHILD_FRAGMENT_TAG).commitNow();
            }
            wasAttachedInTime = f.attached;
        }

        public ChildFragment getChildFragment() {
            return (ChildFragment) getChildFragmentManager().findFragmentByTag(CHILD_FRAGMENT_TAG);
        }

        public void setRetainChildInstance(boolean retainChild) {
            mRetainChild = retainChild;
        }
    }

    public static class ChildFragment extends Fragment {
        private OnAttachListener mOnAttachListener;

        public boolean attached;
        public boolean onActivityResultCalled;
        public int onActivityResultRequestCode;
        public int onActivityResultResultCode;

        @Override
        public void onAttach(Context activity) {
            super.onAttach(activity);
            attached = true;
            if (mOnAttachListener != null) {
                mOnAttachListener.onAttach(activity, this);
            }
        }

        public void setOnAttachListener(OnAttachListener listener) {
            mOnAttachListener = listener;
        }

        public interface OnAttachListener {
            void onAttach(Context activity, ChildFragment fragment);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            onActivityResultCalled = true;
            onActivityResultRequestCode = requestCode;
            onActivityResultResultCode = resultCode;
        }
    }

    public static class AssertNotDestroyed extends Fragment {
        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                assertFalse(getActivity().isDestroyed());
            }
        }
    }
}
