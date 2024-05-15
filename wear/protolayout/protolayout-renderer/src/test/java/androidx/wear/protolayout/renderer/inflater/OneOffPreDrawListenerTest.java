/*
 * Copyright 2024 The Android Open Source Project
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
public class OneOffPreDrawListenerTest {
    @Test
    public void test_onPreDrawLogic_calledOnlyOnce() {
        TestView view = new TestView();
        TestSupplier supplier = new TestSupplier();

        OneOffPreDrawListener listener = OneOffPreDrawListener.add(view, supplier::run);

        assertThat(view.hasOnAttachStateListener).isTrue();
        assertThat(view.isOnAttachStateListenerRemoved).isFalse();
        // Supplier hasn't been called yet.
        assertThat(supplier.runCnt).isEqualTo(0);

        // Supplier hasn't been called yet, but we attached listener, state listener stayed
        // attached.
        listener.onViewAttachedToWindow(view);
        assertThat(view.isOnAttachStateListenerRemoved).isFalse();
        assertThat(supplier.runCnt).isEqualTo(0);

        // Now preDraw is called so should the logic.
        supplier.preDrawReturnValue = false;
        assertThat(listener.onPreDraw()).isFalse();
        // And state listener should be removed.
        assertThat(view.isOnAttachStateListenerRemoved).isTrue();

        assertThat(supplier.runCnt).isEqualTo(1);

        // Now it shouldn't after detach.
        listener.onViewDetachedFromWindow(view);
        assertThat(listener.onPreDraw()).isTrue();
        assertThat(supplier.runCnt).isEqualTo(1);

        // But with new attach it should be called.
        listener.onViewAttachedToWindow(view);
        supplier.preDrawReturnValue = true;
        assertThat(listener.onPreDraw()).isTrue();
        assertThat(supplier.runCnt).isEqualTo(2);
    }

    private static final class TestSupplier {
        public boolean preDrawReturnValue = true;
        public int runCnt = 0;

        boolean run() {
            runCnt++;
            return preDrawReturnValue;
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
