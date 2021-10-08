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

package androidx.car.app.connection;

import androidx.car.app.connection.CarConnection.ConnectionType;
import androidx.lifecycle.LiveData;

/**
 * A {@link LiveData} that always returns that it is connected natively to a car head unit.
 */
final class AutomotiveCarConnectionTypeLiveData extends LiveData<@ConnectionType Integer> {
    @Override
    protected void onActive() {
        setValue(CarConnection.CONNECTION_TYPE_NATIVE);
    }
}
