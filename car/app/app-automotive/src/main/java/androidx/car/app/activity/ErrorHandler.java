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

package androidx.car.app.activity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.car.app.automotive.R;

/**
 * Error handling abstraction
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public interface ErrorHandler {
    /**
     * Possible actions to take when the user clicks on the action button associated with an
     * error
     */
    enum ActionType {
        /** Redirect the user to the "vending application" (e.g.: Google Play Store) */
        UPDATE_HOST(R.string.error_action_update_host),
        /** Finish the application */
        FINISH(R.string.error_action_finish),
        ;

        private final @StringRes int mActionResId;

        ActionType(@StringRes int actionResId) {
            mActionResId = actionResId;
        }

        /** Returns the title of the action button for this type of error */
        public @StringRes int getActionResId() {
            return mActionResId;
        }
    }

    /**
     * All possible error conditions
     */
    enum ErrorType {
        CLIENT_SIDE_ERROR(R.string.error_message_client_side_error, ActionType.FINISH),
        HOST_ERROR(R.string.error_message_host_error, ActionType.FINISH),
        HOST_CONNECTION_LOST(R.string.error_message_host_connection_lost, ActionType.FINISH),
        HOST_NOT_FOUND(R.string.error_message_host_not_found, ActionType.UPDATE_HOST),
        HOST_INCOMPATIBLE(R.string.error_message_host_incompatible, ActionType.UPDATE_HOST),
        MULTIPLE_HOSTS(R.string.error_message_multiple_hosts, ActionType.FINISH),
        UNKNOWN_ERROR(R.string.error_message_unknown_error, ActionType.FINISH),
        ;

        private final @StringRes int mMessageResId;
        private final @NonNull ActionType mActionType;

        ErrorType(@StringRes int messageResId, @NonNull ActionType actionType) {
            mMessageResId = messageResId;
            mActionType = actionType;
        }

        /** Returns a human-readable message to show for this type of error */
        public @StringRes int getMessageResId() {
            return mMessageResId;
        }

        /** Returns the type of action to execute when the user clicks on the associated button */
        public @NonNull ActionType getActionType() {
            return mActionType;
        }
    }

    /**
     * Notifies of an error condition to be displayed to the user. While the error is presented,
     * the {@link CarAppActivity} will be disconnected from the host service.
     *
     * @param errorType type of error to display
     * @param exception additional error information, used for logging
     */
    void onError(@NonNull ErrorType errorType, @NonNull Throwable exception);
}
