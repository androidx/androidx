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

import androidx.car.app.model.Template;
import androidx.lifecycle.Lifecycle.Event;

import org.jspecify.annotations.NonNull;

/** A {@link Screen} for testing that wraps a mockable {@link Screen}. */
public class TestScreen extends Screen {
    private final Screen mScreenForMocking;

    public TestScreen(CarContext carContext, Screen screenForMocking) {
        super(carContext);
        this.mScreenForMocking = screenForMocking;
    }

    @Override
    public @NonNull Template onGetTemplate() {
        return mScreenForMocking.onGetTemplate();
    }

    @Override
    public void dispatchLifecycleEvent(@NonNull Event event) {
        // Calls the mock first, then the actual so that changes like finishing another screen
        // due to a setResult callback go through in the expected order for the mock.
        mScreenForMocking.dispatchLifecycleEvent(event);
        super.dispatchLifecycleEvent(event);
    }
}
