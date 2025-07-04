/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

// This class tests the logic for calling listeners. The actual logic for specific listener is
// tested separately.
@RunWith(AndroidJUnit4.class)
public class OneOffLayoutChangeListenerTest {
    @Test
    public void test_onLayoutChangeLogic_calledOnlyOnce() {
        TestView view = new TestView();
        TestRunnable runnable = new TestRunnable();

        OneOffLayoutChangeListener listener = OneOffLayoutChangeListener.add(view, runnable::run);

        assertThat(view.hasOnAttachStateListener).isTrue();
        assertThat(view.isOnAttachStateListenerRemoved).isFalse();
        // Runnable hasn't been called yet.
        assertThat(runnable.runCnt).isEqualTo(0);

        // Runnable hasn't been called yet, but we attached listener, state listener stayed
        // attached.
        listener.onViewAttachedToWindow(view);
        assertThat(view.isOnAttachStateListenerRemoved).isFalse();
        assertThat(runnable.runCnt).isEqualTo(0);

        // Now onLayoutChange is called so should the logic.
        listener.onLayoutChange(view, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(runnable.runCnt).isEqualTo(1);
        // And state listener should be removed.
        assertThat(view.isOnAttachStateListenerRemoved).isTrue();

        // Now it shouldn't after detach.
        listener.onViewDetachedFromWindow(view);
        listener.onLayoutChange(view, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(runnable.runCnt).isEqualTo(1);

        // But with new attach it should be called.
        listener.onViewAttachedToWindow(view);
        listener.onLayoutChange(view, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(runnable.runCnt).isEqualTo(2);
    }

    private static final class TestRunnable {
        public int runCnt = 0;

        void run() {
            runCnt++;
        }
    }

    private static final class TestView extends View {
        // We need both as initial case is when neither of them is set.
        public boolean hasOnAttachStateListener = false;
        public boolean isOnAttachStateListenerRemoved = false;

        TestView() {
            super(getApplicationContext());
        }

        @Override
        public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
            hasOnAttachStateListener = false;
            isOnAttachStateListenerRemoved = true;
            super.removeOnAttachStateChangeListener(listener);
        }

        @Override
        public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
            hasOnAttachStateListener = true;
            isOnAttachStateListenerRemoved = false;
            super.addOnAttachStateChangeListener(listener);
        }
    }
}
