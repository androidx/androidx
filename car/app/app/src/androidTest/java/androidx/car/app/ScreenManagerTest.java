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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.car.app.model.ItemList;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.testing.TestLifecycleOwner;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for {@link ScreenManager}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ScreenManagerTest {

    private TestScreen mScreen1;
    private TestScreen mScreen2;
    private TestScreen mScreen3;
    @Mock
    private Screen mMockScreen1;
    @Mock
    private Screen mMockScreen2;
    @Mock
    private Screen mMockScreen3;
    @Mock
    private OnScreenResultCallback mOnScreenResultCallback;

    @Mock
    private AppManager mMockAppManager;

    private TestLifecycleOwner mLifecycleOwner;

    private ScreenManager mScreenManager;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        TestCarContext testCarContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
        testCarContext.overrideCarService(AppManager.class, mMockAppManager);

        mScreen1 = new TestScreen(testCarContext, mMockScreen1);
        mScreen2 = new TestScreen(testCarContext, mMockScreen2);
        mScreen3 = new TestScreen(testCarContext, mMockScreen3);

        mLifecycleOwner = new TestLifecycleOwner();

        mScreenManager = ScreenManager.create(testCarContext, mLifecycleOwner.mRegistry);
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_CREATE);
    }

    @Test
    public void getTop_emptyStack_throws() {
        assertThrows(NullPointerException.class, mScreenManager::getTop);
    }

    @Test
    public void getTop_returnsScreenAtTop() {
        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        assertThat(mScreenManager.getTop()).isEqualTo(mScreen2);
    }

    @Test
    public void push_stackWasEmpty_addsToStack_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.push(mScreen1);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);
        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    public void push_stackWasEmpty_screenManagerNotStarted_addsToStack_callsOnCreate() {
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.push(mScreen1);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void push_stackHadScreen_addsToStack_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);
        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(2);
    }

    @Test
    @UiThreadTest
    public void push_screenAlreadyInStack_movesToTop_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);
        mScreenManager.push(mScreen1);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen1 again
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(2);
    }

    @Test
    @UiThreadTest
    public void push_screenAlreadyTopOfStack_noFurtherLifecycleCalls() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);
        mScreenManager.push(mScreen2);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(2);
    }

    @Test
    @UiThreadTest
    public void pushForResult_addsToStack_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager,
                mOnScreenResultCallback);

        mScreenManager.push(mScreen1);
        mScreenManager.pushForResult(mScreen2, mOnScreenResultCallback);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);
        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(2);
    }

    @Test
    @UiThreadTest
    public void pushForResult_screenSetsResult_firstScreenGetsCalled() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);

        mScreenManager.push(mScreen1);
        mScreenManager.pushForResult(mScreen2, mOnScreenResultCallback);

        String result = "done";
        mScreen2.setResult(result);

        verify(mOnScreenResultCallback, never()).onScreenResult(any());

        mScreenManager.remove(mScreen2);

        verify(mOnScreenResultCallback).onScreenResult(result);
    }

    @Test
    @UiThreadTest
    public void pushForResult_screenSetsResult_firstScreenGetsCalled_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager,
                mOnScreenResultCallback);

        mScreenManager.push(mScreen1);
        mScreenManager.pushForResult(mScreen2, mOnScreenResultCallback);

        String result = "foo";
        mScreen2.setResult(result);

        mScreenManager.remove(mScreen2);

        // Pushing screen 1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen 2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Removing screen 2
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);

        inOrder.verify(mOnScreenResultCallback).onScreenResult(result);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    public void pop_stackWasEmpty_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.pop();

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).isEmpty();
    }

    @Test
    public void pop_onlyScreenInStack_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.push(mScreen1);

        mScreenManager.pop();

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void pop_removes_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        mScreenManager.pop();

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Removing screen2
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    public void popTo_stackWasEmpty_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        String marker = "foo";
        mScreen1.setMarker(marker);

        mScreenManager.popTo(marker);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).isEmpty();
    }

    @Test
    public void popTo_onlyScreenInStack_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        String marker = "foo";
        mScreen1.setMarker(marker);
        mScreenManager.push(mScreen1);

        mScreenManager.popTo(marker);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    public void popTo_markerScreenNotInStack_onlyRootScreenInStack_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.push(mScreen1);

        String marker = "foo";
        mScreen2.setMarker(marker);

        mScreenManager.popTo(marker);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void popTo_pops_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        String marker = "foo";
        mScreen1.setMarker(marker);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        mScreenManager.popTo(marker);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Popping to screen1
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void popTo_markerScreenNotInStack_popsToRoot_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockScreen3, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);
        mScreenManager.push(mScreen3);

        mScreenManager.popTo("foo");

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen3
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_RESUME);

        // Popping to screen not in stack (goes to root)
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void popTo_multipleToPop_pops_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockScreen3, mMockAppManager);

        String marker = "foo";
        mScreen1.setMarker(marker);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);
        mScreenManager.push(mScreen3);

        mScreenManager.popTo(marker);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen3
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_RESUME);

        // Popping to screen1
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void popTo_notARootTarget_popsExpectedScreens_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockScreen3, mMockAppManager);

        String marker = "foo";
        mScreen2.setMarker(marker);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);
        mScreenManager.push(mScreen3);

        mScreenManager.popTo(marker);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen3
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_RESUME);

        // Popping to screen2
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(2);
    }

    @Test
    public void popToRoot_stackWasEmpty_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.popTo(Screen.ROOT);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).isEmpty();
    }

    @Test
    public void popTo_onlyRootInStack_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.push(mScreen1);

        mScreenManager.popTo(Screen.ROOT);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void popToRoot_pops_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        mScreenManager.popTo(Screen.ROOT);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Popping to root
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void popToRoot_multipleToPop_pops_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockScreen3, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);
        mScreenManager.push(mScreen3);

        mScreenManager.popTo(Screen.ROOT);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen3
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_RESUME);

        // Popping to root
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void popTo_multipleToPop_withResult_pops_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder =
                inOrder(mMockScreen1, mMockScreen2, mMockScreen3, mMockAppManager,
                        mOnScreenResultCallback);

        mScreenManager.push(mScreen1);

        mScreenManager.pushForResult(mScreen2, mOnScreenResultCallback);
        Object result1 = "foo";
        mScreen2.setResult(result1);

        mScreenManager.pushForResult(mScreen3, mOnScreenResultCallback);
        Object result2 = "bar";
        mScreen3.setResult(result2);

        mScreenManager.popTo(Screen.ROOT);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen3
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_RESUME);

        // Popping to root
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mOnScreenResultCallback).onScreenResult(result2);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mOnScreenResultCallback).onScreenResult(result1);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    public void remove_stackWasEmpty_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.remove(mScreen1);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).isEmpty();
    }

    @Test
    public void remove_onlyScreenInStack_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockAppManager);

        mScreenManager.push(mScreen1);

        mScreenManager.remove(mScreen1);
        assertThat(mScreen1.getLifecycle().getCurrentState()).isNotEqualTo(State.DESTROYED);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void remove_wasTop_removes_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        mScreenManager.remove(mScreen2);
        assertThat(mScreen2.getLifecycle().getCurrentState()).isEqualTo(State.DESTROYED);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Removing screen2
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void remove_wasNotTop_removes_callsProperLifecycleMethods() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        mScreenManager.remove(mScreen1);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        // Removing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(1);
    }

    @Test
    @UiThreadTest
    public void remove_notInStack_noop() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockScreen3, mMockAppManager);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        mScreenManager.remove(mScreen3);

        // Pushing screen1
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        // Pushing screen2
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_CREATE);
        inOrder.verify(mMockAppManager).invalidate();
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();

        assertThat(mScreenManager.getScreenStack()).hasSize(2);
    }

    @Test
    @UiThreadTest
    public void getTopTemplate_returnsTemplateFromTopOfStack() {
        Template template =
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(ItemList.builder().build())
                        .build();
        Template template2 =
                PlaceListMapTemplate.builder()
                        .setTitle("Title2")
                        .setItemList(ItemList.builder().build())
                        .build();
        when(mMockScreen1.onGetTemplate()).thenReturn(template);
        when(mMockScreen2.onGetTemplate()).thenReturn(template2);

        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);

        TemplateWrapper wrapper = mScreenManager.getTopTemplate();
        assertThat(wrapper.getTemplate()).isEqualTo(template2);

        assertThat(wrapper.getTemplateInfosForScreenStack())
                .containsExactly(mScreen2.getLastTemplateInfo(), mScreen1.getLastTemplateInfo());
    }

    @Test
    public void dispatchAppLifecycleEvent_onCreate_expectedLifecycleChange() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_CREATE);

        assertThat(mLifecycleOwner.getLifecycle().getCurrentState()).isEqualTo(State.CREATED);
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onStart_expectedLifecycleChange() {
        mScreenManager.push(mScreen1);
        reset(mMockScreen1);
        InOrder inOrder = inOrder(mMockScreen1);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_START);

        assertThat(mLifecycleOwner.getLifecycle().getCurrentState()).isEqualTo(State.STARTED);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onResume_expectedLifecycleChange() {
        mScreenManager.push(mScreen1);
        reset(mMockScreen1);
        InOrder inOrder = inOrder(mMockScreen1);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        assertThat(mLifecycleOwner.getLifecycle().getCurrentState()).isEqualTo(State.RESUMED);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_START);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_RESUME);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onPause_expectedLifecycleChange() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        mScreenManager.push(mScreen1);
        reset(mMockScreen1);
        InOrder inOrder = inOrder(mMockScreen1);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_PAUSE);

        assertThat(mLifecycleOwner.getLifecycle().getCurrentState()).isEqualTo(State.STARTED);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onStop_expectedLifecycleChange() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        mScreenManager.push(mScreen1);
        reset(mMockScreen1);
        InOrder inOrder = inOrder(mMockScreen1);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_STOP);

        assertThat(mLifecycleOwner.getLifecycle().getCurrentState()).isEqualTo(State.CREATED);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void dispatchAppLifecycleEvent_onDestroy_expectedLifecycleChange() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_DESTROY);

        assertThat(mLifecycleOwner.getLifecycle().getCurrentState()).isEqualTo(State.DESTROYED);
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onDestroy_screenStopped_onlyDestroys() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        mScreenManager.push(mScreen1);
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_STOP);
        reset(mMockScreen1);
        InOrder inOrder = inOrder(mMockScreen1);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onDestroy_screenPaused_stopsAndDestroys() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        mScreenManager.push(mScreen1);
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_PAUSE);
        reset(mMockScreen1);
        InOrder inOrder = inOrder(mMockScreen1);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onDestroy_screenResumed_pausesStopsAndDestroys() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        mScreenManager.push(mScreen1);
        reset(mMockScreen1);
        InOrder inOrder = inOrder(mMockScreen1);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @UiThreadTest
    public void dispatchAppLifecycleEvent_onDestroy_pausesStopsAndDestroysTop_destroysOthers() {
        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_RESUME);
        mScreenManager.push(mScreen1);
        mScreenManager.push(mScreen2);
        mScreenManager.push(mScreen3);
        reset(mMockScreen1);
        reset(mMockScreen2);
        reset(mMockScreen3);
        InOrder inOrder = inOrder(mMockScreen1, mMockScreen2, mMockScreen3);

        mLifecycleOwner.mRegistry.handleLifecycleEvent(Event.ON_DESTROY);

        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_PAUSE);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_STOP);
        inOrder.verify(mMockScreen3).dispatchLifecycleEvent(Event.ON_DESTROY);
        inOrder.verify(mMockScreen2).dispatchLifecycleEvent(Event.ON_DESTROY);
        inOrder.verify(mMockScreen1).dispatchLifecycleEvent(Event.ON_DESTROY);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @UiThreadTest
    public void pop_screenReuseLastTemplateId() {
        Template template =
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(ItemList.builder().build())
                        .build();
        Template template2 =
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(ItemList.builder().build())
                        .build();
        when(mMockScreen1.onGetTemplate()).thenReturn(template);
        when(mMockScreen2.onGetTemplate()).thenReturn(template);
        when(mMockScreen3.onGetTemplate()).thenReturn(template);

        mScreenManager.push(mScreen1);
        TemplateWrapper wrapper1 = mScreenManager.getTopTemplate();
        String id1 = wrapper1.getId();

        mScreenManager.push(mScreen2);
        TemplateWrapper wrapper2 = mScreenManager.getTopTemplate();
        String id2 = wrapper2.getId();

        mScreenManager.push(mScreen3);
        mScreenManager.pop();

        when(mMockScreen1.onGetTemplate()).thenReturn(template2);
        when(mMockScreen2.onGetTemplate()).thenReturn(template2);

        // Popping should reuse the last template's id of screen2.
        wrapper2 = mScreenManager.getTopTemplate();
        assertThat(wrapper2.getId()).isEqualTo(id2);

        // The next getTopTemplate should not reuse the id anymore.
        wrapper2 = mScreenManager.getTopTemplate();
        assertThat(wrapper2.getId()).isNotEqualTo(id2);

        mScreenManager.pop();
        // Popping should reuse the last template's id of screen1.
        wrapper1 = mScreenManager.getTopTemplate();
        assertThat(wrapper1.getId()).isEqualTo(id1);

        // The next getTopTemplate should not reuse the id anymore.
        wrapper1 = mScreenManager.getTopTemplate();
        assertThat(wrapper1.getId()).isNotEqualTo(id1);
    }
}
