/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.testing.ScreenController;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.testing.TestScreenManager;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for {@link Screen}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ScreenTest {
    private TestCarContext mCarContext;

    @Mock
    OnScreenResultCallback mMockOnScreenResultCallback;

    private Screen mScreen;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mCarContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
        mCarContext.reset();

        // Initialize the screen manager with a screen so that finishing a test screen will allow
        // it to be popped off of the stack.
        mCarContext.getCarService(ScreenManager.class)
                .push(
                        new Screen(mCarContext) {
                            @Override
                            @NonNull
                            public Template onGetTemplate() {
                                return new Template() {
                                };
                            }
                        });

        mScreen = new Screen(mCarContext) {
            @Override
            @NonNull
            public Template onGetTemplate() {
                return PlaceListMapTemplate.builder().setItemList(
                        ItemList.builder().build()).build();
            }
        };

        ScreenController.of(mCarContext, mScreen).create().start().resume();
    }

    @Test
    @UiThreadTest
    public void finish_removesSelf() {
        mScreen.finish();
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensRemoved())
                .containsExactly(mScreen);
    }

    @Test
    @UiThreadTest
    public void onCreate_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_CREATE);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.CREATED);
    }

    @Test
    @UiThreadTest
    public void onStart_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_START);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.STARTED);
    }

    @Test
    public void onResume_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_RESUME);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.RESUMED);
    }

    @Test
    @UiThreadTest
    public void onPause_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_PAUSE);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.STARTED);
    }

    @Test
    @UiThreadTest
    public void onStop_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_STOP);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.CREATED);
    }

    @Test
    @UiThreadTest
    public void onDestroy_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.DESTROYED);
    }

    @Test
    @UiThreadTest
    public void setResult_callsThemockOnScreenResultCallback() {
        mScreen.setOnResultCallback(mMockOnScreenResultCallback);

        String foo = "yo";
        mScreen.setResult(foo);

        verify(mMockOnScreenResultCallback, never()).onScreenResult(any());

        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);

        verify(mMockOnScreenResultCallback).onScreenResult(foo);
    }

    @Test
    @UiThreadTest
    public void finish_screenIsDestroyed() {
        mScreen.finish();
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.DESTROYED);
    }
}
