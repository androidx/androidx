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

package androidx.car.moderator;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsHelper;

/**
 * A controller for the actual monitoring of when interaction should be allowed in a
 * {@link SpeedBumpView}.
 */
class SpeedBumpController {
    /**
     * The number of permitted actions that are acquired per second that the user has not
     * interacted with the {@code SpeedBumpView}.
     */
    private static final double ACQUIRED_PERMITS_PER_SECOND = 0.5d;

    /** The maximum number of permits that can be acquired when the user is idling. */
    private static final double MAX_PERMIT_POOL = 5d;

    /** The delay between when the permit pool has been depleted and when it begins to refill. */
    private static final long PERMIT_FILL_DELAY_MS = 600L;

    private final ContentRateLimiter mContentRateLimiter = new ContentRateLimiter(
            ACQUIRED_PERMITS_PER_SECOND,
            MAX_PERMIT_POOL,
            PERMIT_FILL_DELAY_MS);

    /**
     * Whether or not the user is currently allowed to interact with any child views of
     * {@code SpeedBumpView}.
     */
    private boolean mInteractionPermitted = true;

    private final int mLockOutMessageDurationMs;
    private final Handler mHandler = new Handler();

    private final Context mContext;
    private final View mLockoutMessageView;
    private final ImageView mLockoutImageView;

    private final CarUxRestrictionsHelper mUxRestrictionsHelper;

    /**
     * Creates the {@code SpeedBumpController} and associate it with the given
     * {@code SpeedBumpView}.
     */
    SpeedBumpController(SpeedBumpView speedBumpView) {
        mContext = speedBumpView.getContext();

        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        mLockoutMessageView =
                layoutInflater.inflate(R.layout.lock_out_message, speedBumpView, false);
        mLockoutImageView = mLockoutMessageView.findViewById(R.id.lock_out_drawable);
        mLockOutMessageDurationMs =
                mContext.getResources().getInteger(R.integer.speed_bump_lock_out_duration_ms);

        mUxRestrictionsHelper = new CarUxRestrictionsHelper(mContext,
                carUxRestrictions -> updateUnlimitedModeEnabled(carUxRestrictions));

        // By default, no limiting until UXR restrictions kick in.
        mContentRateLimiter.setUnlimitedMode(true);
    }

    /**
     * Starts this {@code SpeedBumpController} for monitoring any changes in driving restrictions.
     */
    void start() {
        mUxRestrictionsHelper.start();
    }

    /**
     * Stops this {@code SpeedBumpController} from monitoring any changes in driving restrictions.
     */
    void stop() {
        mUxRestrictionsHelper.stop();
    }

    /**
     * Returns the view that is used by this {@code SpeedBumpController} for displaying a lock-out
     * message saying that further interaction is blocked.
     *
     * @return The view that contains the lock-out message.
     */
    View getLockoutMessageView() {
        return mLockoutMessageView;
    }

    /**
     * Notifies this {@code SpeedBumpController} that the given {@link MotionEvent} has occurred.
     * This method will return whether or not further interaction should be allowed.
     *
     * @param ev The {@link MotionEvent} that represents a touch event.
     * @return {@code true} if the touch event should be allowed.
     */
    boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();

        // Check if the user has just finished an MotionEvent and count that as an action. Check
        // the ContentRateLimiter to see if interaction is currently permitted.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            boolean nextActionPermitted = mContentRateLimiter.tryAcquire();

            // Indicates that this is the first action that is not permitted. In this case, the
            // child view should at least handle the ACTION_CANCEL or ACTION_UP, so call
            // super.dispatchTouchEvent(), but lock out further interaction.
            if (mInteractionPermitted && !nextActionPermitted) {
                mInteractionPermitted = false;
                showLockOutMessage();
                return true;
            }
        }

        // Otherwise, return if interaction is permitted.
        return mInteractionPermitted;
    }

    /**
     * Displays a message that informs the user that they are not permitted to interact any further
     * with the current view.
     */
    private void showLockOutMessage() {
        // If the message is visible, then it's already showing or animating in. So, do nothing.
        if (mLockoutMessageView.getVisibility() == View.VISIBLE) {
            return;
        }

        Animation lockOutMessageIn =
                AnimationUtils.loadAnimation(mContext, R.anim.lock_out_message_in);
        lockOutMessageIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mLockoutMessageView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // When the lock-out message is completely shown, let it display for
                // mLockOutMessageDurationMs milliseconds before hiding it.
                mHandler.postDelayed(SpeedBumpController.this::hideLockOutMessage,
                        mLockOutMessageDurationMs);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        mLockoutMessageView.clearAnimation();
        mLockoutMessageView.startAnimation(lockOutMessageIn);
        ((AnimatedVectorDrawable) mLockoutImageView.getDrawable()).start();
    }

    /**
     * Hides any lock-out messages. Once the message is hidden, interaction with the view is
     * permitted.
     */
    private void hideLockOutMessage() {
        if (mLockoutMessageView.getVisibility() != View.VISIBLE) {
            return;
        }

        Animation lockOutMessageOut =
                AnimationUtils.loadAnimation(mContext, R.anim.lock_out_message_out);
        lockOutMessageOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mLockoutMessageView.setVisibility(View.GONE);
                mInteractionPermitted = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        mLockoutMessageView.startAnimation(lockOutMessageOut);
    }

    /**
     * Updates whether or not the {@link #mContentRateLimiter} is set in unlimited mode based on
     * the given {@link CarUxRestrictions}.
     *
     * <p>If driver optimization is required, then unlimited mode is off.
     */
    private void updateUnlimitedModeEnabled(CarUxRestrictions restrictions) {
        // If driver optimization is not required, then there is no need to limit anything.
        mContentRateLimiter.setUnlimitedMode(!restrictions.isRequiresDistractionOptimization());
    }
}
