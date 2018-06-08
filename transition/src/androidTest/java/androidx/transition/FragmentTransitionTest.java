/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.transition.test.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


@MediumTest
@RunWith(Parameterized.class)
public class FragmentTransitionTest extends BaseTest {

    @Parameterized.Parameters
    public static Object[] data() {
        return new Boolean[]{
                false, true
        };
    }

    private final boolean mReorderingAllowed;

    public FragmentTransitionTest(boolean reorderingAllowed) {
        mReorderingAllowed = reorderingAllowed;
    }

    @Test
    public void preconditions() {
        final TransitionFragment fragment1 = TransitionFragment.newInstance(R.layout.scene2);
        final TransitionFragment fragment2 = TransitionFragment.newInstance(R.layout.scene3);
        showFragment(fragment1, false, null);
        assertNull(fragment1.mRed);
        assertNotNull(fragment1.mGreen);
        assertNotNull(fragment1.mBlue);
        showFragment(fragment2, true, new Pair<>(fragment1.mGreen, "green"));
        assertNotNull(fragment2.mRed);
        assertNotNull(fragment2.mGreen);
        assertNotNull(fragment2.mBlue);
    }

    @Test
    public void nonSharedTransition() {
        final TransitionFragment fragment1 = TransitionFragment.newInstance(R.layout.scene2);
        final TransitionFragment fragment2 = TransitionFragment.newInstance(R.layout.scene3);
        showFragment(fragment1, false, null);
        showFragment(fragment2, true, null);
        verify(fragment1.mListeners.get(TransitionFragment.TRANSITION_EXIT))
                .onTransitionStart(any(Transition.class));
        verify(fragment1.mListeners.get(TransitionFragment.TRANSITION_EXIT), timeout(3000))
                .onTransitionEnd(any(Transition.class));
        verify(fragment2.mListeners.get(TransitionFragment.TRANSITION_ENTER))
                .onTransitionStart(any(Transition.class));
        verify(fragment2.mListeners.get(TransitionFragment.TRANSITION_ENTER), timeout(3000))
                .onTransitionEnd(any(Transition.class));
        popBackStack();
        verify(fragment1.mListeners.get(TransitionFragment.TRANSITION_REENTER))
                .onTransitionStart(any(Transition.class));
        verify(fragment2.mListeners.get(TransitionFragment.TRANSITION_RETURN))
                .onTransitionStart(any(Transition.class));
    }

    @Test
    public void sharedTransition() {
        final TransitionFragment fragment1 = TransitionFragment.newInstance(R.layout.scene2);
        final TransitionFragment fragment2 = TransitionFragment.newInstance(R.layout.scene3);
        showFragment(fragment1, false, null);
        showFragment(fragment2, true, new Pair<>(fragment1.mGreen, "green"));
        verify(fragment2.mListeners.get(TransitionFragment.TRANSITION_SHARED_ENTER))
                .onTransitionStart(any(Transition.class));
        verify(fragment2.mListeners.get(TransitionFragment.TRANSITION_SHARED_ENTER), timeout(3000))
                .onTransitionEnd(any(Transition.class));
        popBackStack();
        verify(fragment2.mListeners.get(TransitionFragment.TRANSITION_SHARED_RETURN))
                .onTransitionStart(any(Transition.class));
    }

    private void showFragment(final Fragment fragment, final boolean addToBackStack,
            final Pair<View, String> sharedElement) {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.root, fragment);
                transaction.setReorderingAllowed(mReorderingAllowed);
                if (sharedElement != null) {
                    transaction.addSharedElement(sharedElement.first, sharedElement.second);
                }
                if (addToBackStack) {
                    transaction.addToBackStack(null);
                    transaction.commit();
                    getFragmentManager().executePendingTransactions();
                } else {
                    transaction.commitNow();
                }
            }
        });
        instrumentation.waitForIdleSync();
    }

    private void popBackStack() {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().popBackStackImmediate();
            }
        });
        instrumentation.waitForIdleSync();
    }

    private FragmentManager getFragmentManager() {
        return rule.getActivity().getSupportFragmentManager();
    }

    /**
     * A {@link Fragment} with all kinds of {@link Transition} with tracking listeners.
     */
    public static class TransitionFragment extends Fragment {

        static final int TRANSITION_ENTER = 1;
        static final int TRANSITION_EXIT = 2;
        static final int TRANSITION_REENTER = 3;
        static final int TRANSITION_RETURN = 4;
        static final int TRANSITION_SHARED_ENTER = 5;
        static final int TRANSITION_SHARED_RETURN = 6;

        private static final String ARG_LAYOUT_ID = "layout_id";

        View mRed;
        View mGreen;
        View mBlue;

        SparseArrayCompat<Transition.TransitionListener> mListeners = new SparseArrayCompat<>();

        public static TransitionFragment newInstance(@LayoutRes int layout) {
            final Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_ID, layout);
            final TransitionFragment fragment = new TransitionFragment();
            fragment.setArguments(args);
            return fragment;
        }

        public TransitionFragment() {
            setEnterTransition(createTransition(TRANSITION_ENTER));
            setExitTransition(createTransition(TRANSITION_EXIT));
            setReenterTransition(createTransition(TRANSITION_REENTER));
            setReturnTransition(createTransition(TRANSITION_RETURN));
            setSharedElementEnterTransition(createTransition(TRANSITION_SHARED_ENTER));
            setSharedElementReturnTransition(createTransition(TRANSITION_SHARED_RETURN));
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(getArguments().getInt(ARG_LAYOUT_ID), container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            mRed = view.findViewById(R.id.redSquare);
            mGreen = view.findViewById(R.id.greenSquare);
            mBlue = view.findViewById(R.id.blueSquare);
            if (mRed != null) {
                ViewCompat.setTransitionName(mRed, "red");
            }
            if (mGreen != null) {
                ViewCompat.setTransitionName(mGreen, "green");
            }
            if (mBlue != null) {
                ViewCompat.setTransitionName(mBlue, "blue");
            }
        }

        private Transition createTransition(int type) {
            final Transition.TransitionListener listener = mock(
                    Transition.TransitionListener.class);
            final AutoTransition transition = new AutoTransition();
            transition.addListener(listener);
            transition.setDuration(10);
            mListeners.put(type, listener);
            return transition;
        }

    }

}
