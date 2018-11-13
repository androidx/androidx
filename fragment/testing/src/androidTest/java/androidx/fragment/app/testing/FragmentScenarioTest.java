/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.fragment.app.testing;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for FragmentScenario's implementation.
 * Verifies FragmentScenario API works consistently across different Android framework versions.
 */
@RunWith(AndroidJUnit4.class)
public final class FragmentScenarioTest {
    @Test
    @LargeTest
    public void launchFragment() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.RESUMED);
                        // FragmentScenario#launch doesn't attach view to the hierarchy.
                        // To test graphical Fragment, use FragmentScenario#launchInContainer.
                        assertThat(fragment.isViewAttachedToWindow()).isFalse();
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void launchFragmentWithArgs() {
        Bundle args = new Bundle();
        args.putString("my_arg_is", "androidx");

        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class, args);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getArguments().getString("my_arg_is")).isEqualTo(
                                "androidx");
                        // FragmentScenario#launch doesn't attach view to the hierarchy.
                        // To test graphical Fragment, use FragmentScenario#launchInContainer.
                        assertThat(fragment.isViewAttachedToWindow()).isFalse();
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void launchFragmentInContainer() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launchInContainer(StateRecordingFragment.class);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.RESUMED);
                        assertThat(fragment.isViewAttachedToWindow()).isTrue();
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void launchFragmentInContainerWithArgs() {
        Bundle args = new Bundle();
        args.putString("my_arg_is", "androidx");

        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launchInContainer(StateRecordingFragment.class, args);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getArguments().getString("my_arg_is")).isEqualTo(
                                "androidx");
                        assertThat(fragment.isViewAttachedToWindow()).isTrue();
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void launchFragmentWithFragmentFactory() {
        FragmentFactory factory = new FragmentFactory() {
            @NonNull
            @Override
            public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className,
                    @Nullable Bundle args) {
                if (!NoDefaultConstructorFragment.class.getName().equals(className)) {
                    return super.instantiate(classLoader, className, args);
                } else {
                    return new NoDefaultConstructorFragment("my constructor param");
                }
            }
        };
        FragmentScenario<NoDefaultConstructorFragment> scenario =
                FragmentScenario.launch(
                        NoDefaultConstructorFragment.class, /*fragmentArgs=*/null, factory);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<NoDefaultConstructorFragment>() {
                    @Override
                    public void perform(@NonNull NoDefaultConstructorFragment fragment) {
                        assertThat(fragment.getName()).isEqualTo("my constructor param");
                    }
                });
    }

    @Test
    @LargeTest
    public void fromResumedToCreated() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.CREATED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.CREATED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromResumedToStarted() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.STARTED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.STARTED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromResumedToResumed() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.RESUMED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.RESUMED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromResumedToDestroyed() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.DESTROYED);
    }

    @Test
    @LargeTest
    public void fromCreatedToCreated() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.CREATED);
        scenario.moveToState(State.CREATED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.CREATED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromCreatedToStarted() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.CREATED);
        scenario.moveToState(State.STARTED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.STARTED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromCreatedToResumed() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.CREATED);
        scenario.moveToState(State.RESUMED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.RESUMED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromCreatedToDestroyed() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.CREATED);
        scenario.moveToState(State.DESTROYED);
    }

    @Test
    @LargeTest
    public void fromStartedToCreated() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.STARTED);
        scenario.moveToState(State.CREATED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.CREATED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromStartedToStarted() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.STARTED);
        scenario.moveToState(State.STARTED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.STARTED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromStartedToResumed() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.STARTED);
        scenario.moveToState(State.RESUMED);
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.RESUMED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(0);
                    }
                });
    }

    @Test
    @LargeTest
    public void fromStartedToDestroyed() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.STARTED);
        scenario.moveToState(State.DESTROYED);
    }

    @Test
    @LargeTest
    public void fromDestroyedToDestroyed() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.DESTROYED);
        scenario.moveToState(State.DESTROYED);
    }

    @Test
    @LargeTest
    public void recreateCreatedFragment() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.CREATED);
        scenario.recreate();
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.CREATED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(1);
                    }
                });
    }

    @Test
    @LargeTest
    public void recreateStartedFragment() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.moveToState(State.STARTED);
        scenario.recreate();
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.STARTED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(1);
                    }
                });
    }

    @Test
    @LargeTest
    public void recreateResumedFragment() {
        FragmentScenario<StateRecordingFragment> scenario =
                FragmentScenario.launch(StateRecordingFragment.class);
        scenario.recreate();
        scenario.onFragment(
                new FragmentScenario.FragmentAction<StateRecordingFragment>() {
                    @Override
                    public void perform(@NonNull StateRecordingFragment fragment) {
                        assertThat(fragment.getState()).isEqualTo(State.RESUMED);
                        assertThat(fragment.getNumberOfRecreations()).isEqualTo(1);
                    }
                });
    }

    @Test
    @LargeTest
    public void recreateFragmentWithFragmentFactory() {
        FragmentFactory factory = new FragmentFactory() {
            @NonNull
            @Override
            public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className,
                    @Nullable Bundle args) {
                if (!NoDefaultConstructorFragment.class.getName().equals(className)) {
                    return super.instantiate(classLoader, className, args);
                } else {
                    return new NoDefaultConstructorFragment("my constructor param");
                }
            }
        };
        FragmentScenario<NoDefaultConstructorFragment> scenario =
                FragmentScenario.launch(
                        NoDefaultConstructorFragment.class, /*fragmentArgs=*/null, factory);
        scenario.recreate();
        scenario.onFragment(
                new FragmentScenario.FragmentAction<NoDefaultConstructorFragment>() {
                    @Override
                    public void perform(@NonNull NoDefaultConstructorFragment fragment) {
                        assertThat(fragment.getName()).isEqualTo("my constructor param");
                    }
                });
    }
}
