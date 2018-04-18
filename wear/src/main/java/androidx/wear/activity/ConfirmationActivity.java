/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.wear.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseIntArray;

import androidx.wear.R;
import androidx.wear.widget.ConfirmationOverlay;

/**
 * This Activity is used to display confirmation animations after the user completes an action on
 * the wearable. There are three types of confirmations: Success: the action was completed
 * successfully on the wearable. Failure: the action failed to complete. Open on Phone: the action
 * has caused something to display on the phone, or in order to complete the action, the user will
 * need to go to their phone to continue.
 *
 * <p>It is the responsibility of the wearable application developer to determine whether the action
 * has succeeded, failed, or requires the user to go to their phone, and trigger the appropriate
 * confirmation.
 *
 * <p>To configure the confirmation according to the result of the action, set the extra {@link
 * #EXTRA_ANIMATION_TYPE} to one of the following values:
 *
 * <dl>
 * <dt>{@link #SUCCESS_ANIMATION}
 * <dd>Displays a positive confirmation animation with an optional message.
 * <dt>{@link #OPEN_ON_PHONE_ANIMATION}
 * <dd>Displays an animation indicating an action has been sent to a paired device.
 * <dt>{@link #FAILURE_ANIMATION}
 * <dd>Displays a generic failure page with an optional message.
 * </dl>
 *
 * An optional message, included in the extra {@link #EXTRA_MESSAGE} will be displayed horizontally
 * centered below the animation.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ConfirmationActivity extends Activity {

    public static final String EXTRA_MESSAGE = "androidx.wear.activity.extra.MESSAGE";
    public static final String EXTRA_ANIMATION_TYPE =
            "androidx.wear.activity.extra.ANIMATION_TYPE";

    public static final int SUCCESS_ANIMATION = 1;
    public static final int OPEN_ON_PHONE_ANIMATION = 2;
    public static final int FAILURE_ANIMATION = 3;

    private static final SparseIntArray CONFIRMATION_OVERLAY_TYPES = new SparseIntArray();

    static {
        CONFIRMATION_OVERLAY_TYPES.append(SUCCESS_ANIMATION, ConfirmationOverlay.SUCCESS_ANIMATION);
        CONFIRMATION_OVERLAY_TYPES.append(
                OPEN_ON_PHONE_ANIMATION, ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION);
        CONFIRMATION_OVERLAY_TYPES.append(FAILURE_ANIMATION, ConfirmationOverlay.FAILURE_ANIMATION);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.ConfirmationActivity);

        Intent intent = getIntent();

        int requestedType = intent.getIntExtra(EXTRA_ANIMATION_TYPE, SUCCESS_ANIMATION);
        if (CONFIRMATION_OVERLAY_TYPES.indexOfKey(requestedType) < 0) {
            throw new IllegalArgumentException("Unknown type of animation: " + requestedType);
        }

        @ConfirmationOverlay.OverlayType int type = CONFIRMATION_OVERLAY_TYPES.get(requestedType);
        String message = intent.getStringExtra(EXTRA_MESSAGE);

        new ConfirmationOverlay()
                .setType(type)
                .setMessage(message)
                .setFinishedAnimationListener(
                        new ConfirmationOverlay.OnAnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                ConfirmationActivity.this.onAnimationFinished();
                            }
                        })
                .showOn(this);
    }

    /**
     * Override this method if you wish to provide different than out-of-the-box behavior when the
     * confirmation animation finishes. By default this method will finish the ConfirmationActivity.
     */
    protected void onAnimationFinished() {
        finish();
    }
}
