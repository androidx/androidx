/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.testing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import androidx.car.app.Screen;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ScreenController}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ScreenControllerTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private DefaultLifecycleObserver mMockObserver;

    private final Template mTemplate = new Template() {
    };

    private Screen mTestScreen;
    private ScreenController mScreenController;
    private TestCarContext mCarContext;

    @Before
    public void setup() {
        mCarContext = TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
        mTestScreen = new Screen(mCarContext) {
            @Override
            public @NonNull Template onGetTemplate() {
                return mTemplate;
            }
        };
        mScreenController = new ScreenController(mTestScreen);

        mTestScreen.getLifecycle().addObserver(mMockObserver);
    }

    @Test
    public void create_movesLifecycleAndAddsToStack() {
        mScreenController.moveToState(Lifecycle.State.CREATED);

        verify(mMockObserver).onCreate(any());
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensPushed())
                .containsExactly(mTestScreen);
    }

    @Test
    public void create_wasInStack_movesLifecycle() {
        mCarContext.getCarService(TestScreenManager.class).push(mTestScreen);

        mScreenController.moveToState(Lifecycle.State.CREATED);

        verify(mMockObserver).onCreate(any());
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensPushed())
                .containsExactly(mTestScreen);
    }

    @Test
    public void start_movesLifecycle() {
        mScreenController.moveToState(Lifecycle.State.STARTED);

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
    }

    @Test
    public void resume_movesLifecycle() {
        mScreenController.moveToState(Lifecycle.State.RESUMED);

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
    }

    @Test
    public void pause_movesLifecycle() {
        mScreenController.moveToState(Lifecycle.State.RESUMED);
        mScreenController.moveToState(Lifecycle.State.STARTED);

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
        verify(mMockObserver).onPause(any());
    }

    @Test
    public void stop_movesLifecycle() {
        mScreenController.moveToState(Lifecycle.State.RESUMED);
        mScreenController.moveToState(Lifecycle.State.CREATED);

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
        verify(mMockObserver).onPause(any());
        verify(mMockObserver).onStop(any());
    }

    @Test
    public void destroy_movesLifecycle() {
        mScreenController.moveToState(Lifecycle.State.RESUMED);
        mScreenController.moveToState(Lifecycle.State.DESTROYED);

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
        verify(mMockObserver).onPause(any());
        verify(mMockObserver).onStop(any());
        verify(mMockObserver).onDestroy(any());
    }

    @Test
    public void getReturnedTemplates() {
        mScreenController.moveToState(Lifecycle.State.STARTED);
        mScreenController.reset();

        mTestScreen.invalidate();

        assertThat(mScreenController.getTemplatesReturned()).containsExactly(mTemplate);

        mTestScreen.invalidate();
        mTestScreen.invalidate();

        assertThat(mScreenController.getTemplatesReturned())
                .containsExactly(mTemplate, mTemplate, mTemplate);
    }

    @Test
    public void reset() {
        mScreenController.moveToState(Lifecycle.State.STARTED);
        mScreenController.reset();

        mTestScreen.invalidate();
        mTestScreen.invalidate();
        mTestScreen.invalidate();

        assertThat(mScreenController.getTemplatesReturned())
                .containsExactly(mTemplate, mTemplate, mTemplate);

        mScreenController.reset();
        assertThat(mScreenController.getTemplatesReturned()).isEmpty();
    }
}
