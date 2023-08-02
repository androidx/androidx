/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** A listener of dismiss events. */
@CarProtocol
@RequiresCarApi(5)
public interface AlertCallback {

    /**
     * The reason for which the alert was cancelled.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef(
            value = {
                    REASON_TIMEOUT,
                    REASON_USER_ACTION,
                    REASON_NOT_SUPPORTED,
            })
    @Retention(RetentionPolicy.SOURCE)
    @interface Reason {
    }

    /**
     * Indicates the cancellation is due to timeout.
     */
    int REASON_TIMEOUT = 1;

    /**
     * Indicates the cancellation is due to user generated action, e.g. close button being pressed.
     */
    int REASON_USER_ACTION = 2;

    /**
     * Indicates the cancellation is due to the request not being supported, e.g. when current
     * template cannot display the alert.
     */
    int REASON_NOT_SUPPORTED = 3;

    /** Notifies that a cancel event happened with given {@code reason}. */
    void onCancel(@Reason int reason);

    /** Notifies that a dismiss happened. */
    void onDismiss();
}
