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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.testing.ScreenController;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.testing.TestScreenManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Screen}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class ScreenTest {
    private TestCarContext mCarContext;

    @Mock
    OnScreenResultListener mMockOnScreenResultListener;

    private Screen mScreen;

    @Before
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
                return new PlaceListMapTemplate.Builder().setItemList(
                        new ItemList.Builder().build()).build();
            }
        };

        new ScreenController(mCarContext, mScreen).moveToState(State.RESUMED);
    }

    @Test
    public void finish_removesSelf() {
        mScreen.finish();
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensRemoved())
                .containsExactly(mScreen);
    }

    @Test
    public void onCreate_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_CREATE);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.CREATED);
    }

    @Test
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
    public void onPause_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_PAUSE);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.STARTED);
    }

    @Test
    public void onStop_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_STOP);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.CREATED);
    }

    @Test
    public void onDestroy_expectedLifecycleChange() {
        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.DESTROYED);
    }

    @Test
    public void alreadyDestroyed_willNotDispatchMoreEvents() {
        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);

        DefaultLifecycleObserver observer = mock(DefaultLifecycleObserver.class);
        mScreen.getLifecycle().addObserver(observer);

        mScreen.dispatchLifecycleEvent(Event.ON_CREATE);
        mScreen.dispatchLifecycleEvent(Event.ON_START);
        mScreen.dispatchLifecycleEvent(Event.ON_RESUME);
        mScreen.dispatchLifecycleEvent(Event.ON_PAUSE);
        mScreen.dispatchLifecycleEvent(Event.ON_STOP);
        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);

        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.DESTROYED);
        verify(observer, never()).onCreate(any());
        verify(observer, never()).onStart(any());
        verify(observer, never()).onResume(any());
        verify(observer, never()).onPause(any());
        verify(observer, never()).onStop(any());
        verify(observer, never()).onDestroy(any());
    }

    @Test
    public void alreadyDestroyed_willNotSendResults() {
        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);
        mScreen.setOnScreenResultListener(mMockOnScreenResultListener);
        mScreen.dispatchLifecycleEvent(Event.ON_RESUME);
        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);
        verify(mMockOnScreenResultListener, never()).onScreenResult(any());
    }

    @Test
    public void setResult_callsThemockOnScreenResultCallback() {
        mScreen.setOnScreenResultListener(mMockOnScreenResultListener);

        String foo = "yo";
        mScreen.setResult(foo);

        verify(mMockOnScreenResultListener, never()).onScreenResult(any());

        mScreen.dispatchLifecycleEvent(Event.ON_DESTROY);

        verify(mMockOnScreenResultListener).onScreenResult(foo);
    }

    @Test
    public void finish_screenIsDestroyed() {
        mScreen.finish();
        assertThat(mScreen.getLifecycle().getCurrentState()).isEqualTo(State.DESTROYED);
    }
}
