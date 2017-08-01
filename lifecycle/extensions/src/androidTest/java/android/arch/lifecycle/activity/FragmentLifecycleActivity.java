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

package android.arch.lifecycle.activity;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.extensions.test.R;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FragmentLifecycleActivity extends AppCompatActivity {
    public static final String NESTED_TAG = "nested_fragment";
    public static final String MAIN_TAG = "main_fragment";
    private static final String EXTRA_NESTED = "nested";

    private final List<Lifecycle.Event> mLoggedEvents = Collections
            .synchronizedList(new ArrayList<Lifecycle.Event>());
    private LifecycleOwner mObservedOwner;
    private final CountDownLatch mDestroyLatch = new CountDownLatch(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainFragment fragment;
        fragment = new MainFragment();
        boolean nested = getIntent().getBooleanExtra(EXTRA_NESTED, false);
        if (nested) {
            fragment.mNestedFragment = new NestedFragment();
        }
        observe(nested ? fragment.mNestedFragment : fragment);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, MAIN_TAG)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyLatch.countDown();
    }

    public void resetEvents() {
        mLoggedEvents.clear();
    }

    public static class MainFragment extends LifecycleFragment {
        @Nullable
        LifecycleFragment mNestedFragment;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (mNestedFragment != null) {
                getChildFragmentManager().beginTransaction()
                        .add(mNestedFragment, NESTED_TAG)
                        .commit();
            }
        }
    }

    public static class NestedFragment extends LifecycleFragment {
    }

    public static Intent intentFor(Context context, boolean nested) {
        Intent intent = new Intent(context, FragmentLifecycleActivity.class);
        intent.putExtra(EXTRA_NESTED, nested);
        return intent;
    }

    public void observe(LifecycleOwner provider) {
        mObservedOwner = provider;
        provider.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            public void anyEvent(LifecycleOwner owner, Lifecycle.Event event) {
                mLoggedEvents.add(event);
            }
        });
    }

    public List<Lifecycle.Event> getLoggedEvents() {
        return mLoggedEvents;
    }

    public LifecycleOwner getObservedOwner() {
        return mObservedOwner;
    }

    public boolean awaitForDestruction(long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        return mDestroyLatch.await(timeout, timeUnit);
    }
}
