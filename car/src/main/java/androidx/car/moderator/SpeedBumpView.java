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

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.car.R;

/**
 * A wrapping view that will monitor all touch events on its children views and prevent the user
 * from interacting if they have performed a preset number of interactions within a preset amount
 * of time.
 *
 * <p>When the user has performed the maximum number of interactions per the set unit of time, a
 * message explaining that they are no longer able to interact with the view is also displayed.
 */
public class SpeedBumpView extends FrameLayout {
    /**
     * The number of permitted actions that are acquired per second that the user has not
     * interacted with the {@code SpeedBumpView}.
     */
    private static final float ACQUIRED_PERMITS_PER_SECOND = 0.5f;

    /** The maximum number of permits that can be acquired when the user is idling. */
    private static final float MAX_PERMIT_POOL = 5f;

    /** The delay between when the permit pool has been depleted and when it begins to refill. */
    private static final long PERMIT_FILL_DELAY_MS = 600L;

    private int mLockOutMessageDurationMs;

    private final ContentRateLimiter mContentRateLimiter = new ContentRateLimiter(
            ACQUIRED_PERMITS_PER_SECOND,
            MAX_PERMIT_POOL,
            PERMIT_FILL_DELAY_MS);

    /**
     * Whether or not the user is currently allowed to interact with any child views of
     * {@code SpeedBumpView}.
     */
    private boolean mInteractionPermitted = true;

    private final Handler mHandler = new Handler();

    private View mLockoutMessageView;
    private ImageView mLockoutImageView;

    public SpeedBumpView(Context context) {
        super(context);
        init();
    }

    public SpeedBumpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpeedBumpView(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs);
        init();
    }

    public SpeedBumpView(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);
        init();
    }

    private void init() {
        mLockOutMessageDurationMs =
                getResources().getInteger(R.integer.speed_bump_lock_out_duration_ms);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        mLockoutMessageView = layoutInflater.inflate(R.layout.lock_out_message, this, false);
        mLockoutImageView = mLockoutMessageView.findViewById(R.id.lock_out_drawable);

        addView(mLockoutMessageView);
        mLockoutMessageView.bringToFront();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        // Always ensure that the lock out view has the highest Z-index so that it will show
        // above all other views.
        mLockoutMessageView.bringToFront();
    }


    // Overriding dispatchTouchEvent to intercept all touch events on child views.
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
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
                return super.dispatchTouchEvent(ev);
            }
        }

        // Otherwise, if interaction permitted, allow child views to handle touch events.
        return mInteractionPermitted && super.dispatchTouchEvent(ev);
    }

    /**
     * Displays a message that informs the user that they are not permitted to interact any further
     * with the current view.
     */
    private void showLockOutMessage() {
        // If the message is visible, then it's already showing or animating in. So, do nothing.
        if (mLockoutMessageView.getVisibility() == VISIBLE) {
            return;
        }

        Animation lockOutMessageIn =
                AnimationUtils.loadAnimation(getContext(), R.anim.lock_out_message_in);
        lockOutMessageIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mLockoutMessageView.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // When the lock-out message is completely shown, let it display for
                // mLockOutMessageDurationMs milliseconds before hiding it.
                mHandler.postDelayed(SpeedBumpView.this::hideLockOutMessage,
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
        if (mLockoutMessageView.getVisibility() != VISIBLE) {
            return;
        }

        Animation lockOutMessageOut =
                AnimationUtils.loadAnimation(getContext(), R.anim.lock_out_message_out);
        lockOutMessageOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mLockoutMessageView.setVisibility(GONE);
                mInteractionPermitted = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        mLockoutMessageView.startAnimation(lockOutMessageOut);
    }
}
