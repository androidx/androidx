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

package androidx.car.uxrestrictions;

/**
 * Listener Interface for clients to implement to get updated on
 * User Experience Restrictions (UXR, or UX restrictions) related changes.
 */
public interface OnUxRestrictionsChangedListener {
    /**
     * Called when the UX restrictions changes due to a car's driving state.
     *
     * @param restrictionInfo The new UX restriction information
     */
    void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo);
}
