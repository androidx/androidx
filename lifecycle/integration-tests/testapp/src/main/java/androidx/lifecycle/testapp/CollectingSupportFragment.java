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
package androidx.lifecycle.testapp;

import static androidx.lifecycle.testapp.TestEvent.OWNER_CALLBACK;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * A support fragment that collects all of its events.
 */
@SuppressLint("ValidFragment")
public class CollectingSupportFragment extends Fragment implements CollectingLifecycleOwner {
    private final List<Pair<TestEvent, Lifecycle.Event>> mCollectedEvents =
            new ArrayList<>();
    private TestObserver mTestObserver = new TestObserver(mCollectedEvents);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_CREATE));
        getLifecycle().addObserver(mTestObserver);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        //noinspection ConstantConditions
        FrameLayout layout = new FrameLayout(container.getContext());
        layout.setId(R.id.child_fragment_container);
        return layout;
    }

    /**
     * Runs a replace fragment transaction with 'fragment' on this Fragment.
     */
    public void replaceFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.child_fragment_container, fragment)
                .commitNow();
    }

    @Override
    public void onStart() {
        super.onStart();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_START));
    }

    @Override
    public void onResume() {
        super.onResume();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_RESUME));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_DESTROY));
    }

    @Override
    public void onStop() {
        super.onStop();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_STOP));
    }

    @Override
    public void onPause() {
        super.onPause();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_PAUSE));
    }

    @Override
    public List<Pair<TestEvent, Lifecycle.Event>> copyCollectedEvents() {
        return new ArrayList<>(mCollectedEvents);
    }
}
