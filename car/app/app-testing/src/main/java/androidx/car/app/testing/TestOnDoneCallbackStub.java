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

package androidx.car.app.testing;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.serialization.Bundleable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A stub for use in API calls for the car app testing library.
 *
 */
@RestrictTo(Scope.LIBRARY)
public class TestOnDoneCallbackStub extends IOnDoneCallback.Stub {
    @Override
    public void onSuccess(@Nullable Bundleable response) {
    }

    @Override
    public void onFailure(@NonNull Bundleable failureResponse) {
    }
}
