/*
 * Copyright (C) 2012 The Android Open Source Project
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

package androidx.core.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

/**
 * Helper for accessing features in {@link android.app.ActivityOptions} in a backwards compatible
 * fashion.
 */
public class ActivityOptionsCompat {
    /**
     * A long in the extras delivered by {@link #requestUsageTimeReport} that contains
     * the total time (in ms) the user spent in the app flow.
     */
    public static final String EXTRA_USAGE_TIME_REPORT = "android.activity.usage_time";

    /**
     * A Bundle in the extras delivered by {@link #requestUsageTimeReport} that contains
     * detailed information about the time spent in each package associated with the app;
     * each key is a package name, whose value is a long containing the time (in ms).
     */
    public static final String EXTRA_USAGE_TIME_REPORT_PACKAGES = "android.usage_time_packages";

    /**
     * Create an ActivityOptions specifying a custom animation to run when the
     * activity is displayed.
     *
     * @param context Who is defining this. This is the application that the
     * animation resources will be loaded from.
     * @param enterResId A resource ID of the animation resource to use for the
     * incoming activity. Use 0 for no animation.
     * @param exitResId A resource ID of the animation resource to use for the
     * outgoing activity. Use 0 for no animation.
     * @return Returns a new ActivityOptions object that you can use to supply
     * these options as the options Bundle when starting an activity.
     */
    @NonNull
    public static ActivityOptionsCompat makeCustomAnimation(@NonNull Context context,
            int enterResId, int exitResId) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new ActivityOptionsCompatImpl(ActivityOptions.makeCustomAnimation(context,
                    enterResId, exitResId));
        }
        return new ActivityOptionsCompat();
    }

    /**
     * Create an ActivityOptions specifying an animation where the new activity is
     * scaled from a small originating area of the screen to its final full
     * representation.
     * <p/>
     * If the Intent this is being used with has not set its
     * {@link android.content.Intent#setSourceBounds(android.graphics.Rect)},
     * those bounds will be filled in for you based on the initial bounds passed
     * in here.
     *
     * @param source The View that the new activity is animating from. This
     * defines the coordinate space for startX and startY.
     * @param startX The x starting location of the new activity, relative to
     * source.
     * @param startY The y starting location of the activity, relative to source.
     * @param startWidth The initial width of the new activity.
     * @param startHeight The initial height of the new activity.
     * @return Returns a new ActivityOptions object that you can use to supply
     * these options as the options Bundle when starting an activity.
     */
    @NonNull
    public static ActivityOptionsCompat makeScaleUpAnimation(@NonNull View source,
            int startX, int startY, int startWidth, int startHeight) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new ActivityOptionsCompatImpl(ActivityOptions.makeScaleUpAnimation(
                    source, startX, startY, startWidth, startHeight));
        }
        return new ActivityOptionsCompat();
    }

    /**
     * Create an ActivityOptions specifying an animation where the new
     * activity is revealed from a small originating area of the screen to
     * its final full representation.
     *
     * @param source The View that the new activity is animating from.  This
     * defines the coordinate space for <var>startX</var> and <var>startY</var>.
     * @param startX The x starting location of the new activity, relative to <var>source</var>.
     * @param startY The y starting location of the activity, relative to <var>source</var>.
     * @param width The initial width of the new activity.
     * @param height The initial height of the new activity.
     * @return Returns a new ActivityOptions object that you can use to
     * supply these options as the options Bundle when starting an activity.
     */
    @NonNull
    public static ActivityOptionsCompat makeClipRevealAnimation(@NonNull View source,
            int startX, int startY, int width, int height) {
        if (Build.VERSION.SDK_INT >= 23) {
            return new ActivityOptionsCompatImpl(ActivityOptions.makeClipRevealAnimation(
                    source, startX, startY, width, height));
        }
        return new ActivityOptionsCompat();
    }

    /**
     * Create an ActivityOptions specifying an animation where a thumbnail is
     * scaled from a given position to the new activity window that is being
     * started.
     * <p/>
     * If the Intent this is being used with has not set its
     * {@link android.content.Intent#setSourceBounds(android.graphics.Rect)},
     * those bounds will be filled in for you based on the initial thumbnail
     * location and size provided here.
     *
     * @param source The View that this thumbnail is animating from. This
     * defines the coordinate space for startX and startY.
     * @param thumbnail The bitmap that will be shown as the initial thumbnail
     * of the animation.
     * @param startX The x starting location of the bitmap, relative to source.
     * @param startY The y starting location of the bitmap, relative to source.
     * @return Returns a new ActivityOptions object that you can use to supply
     * these options as the options Bundle when starting an activity.
     */
    @NonNull
    public static ActivityOptionsCompat makeThumbnailScaleUpAnimation(@NonNull View source,
            @NonNull Bitmap thumbnail, int startX, int startY) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new ActivityOptionsCompatImpl(ActivityOptions.makeThumbnailScaleUpAnimation(
                    source, thumbnail, startX, startY));
        }
        return new ActivityOptionsCompat();
    }

    /**
     * Create an ActivityOptions to transition between Activities using cross-Activity scene
     * animations. This method carries the position of one shared element to the started Activity.
     * The position of <code>sharedElement</code> will be used as the epicenter for the
     * exit Transition. The position of the shared element in the launched Activity will be the
     * epicenter of its entering Transition.
     *
     * <p>This requires {@link android.view.Window#FEATURE_CONTENT_TRANSITIONS} to be
     * enabled on the calling Activity to cause an exit transition. The same must be in
     * the called Activity to get an entering transition.</p>
     * @param activity The Activity whose window contains the shared elements.
     * @param sharedElement The View to transition to the started Activity. sharedElement must
     *                      have a non-null sharedElementName.
     * @param sharedElementName The shared element name as used in the target Activity. This may
     *                          be null if it has the same name as sharedElement.
     * @return Returns a new ActivityOptions object that you can use to
     *         supply these options as the options Bundle when starting an activity.
     */
    @NonNull
    public static ActivityOptionsCompat makeSceneTransitionAnimation(@NonNull Activity activity,
            @NonNull View sharedElement, @NonNull String sharedElementName) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new ActivityOptionsCompatImpl(ActivityOptions.makeSceneTransitionAnimation(
                    activity, sharedElement, sharedElementName));
        }
        return new ActivityOptionsCompat();
    }

    /**
     * Create an ActivityOptions to transition between Activities using cross-Activity scene
     * animations. This method carries the position of multiple shared elements to the started
     * Activity. The position of the first element in sharedElements
     * will be used as the epicenter for the exit Transition. The position of the associated
     * shared element in the launched Activity will be the epicenter of its entering Transition.
     *
     * <p>This requires {@link android.view.Window#FEATURE_CONTENT_TRANSITIONS} to be
     * enabled on the calling Activity to cause an exit transition. The same must be in
     * the called Activity to get an entering transition.</p>
     * @param activity The Activity whose window contains the shared elements.
     * @param sharedElements The names of the shared elements to transfer to the called
     *                       Activity and their associated Views. The Views must each have
     *                       a unique shared element name.
     * @return Returns a new ActivityOptions object that you can use to
     *         supply these options as the options Bundle when starting an activity.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static ActivityOptionsCompat makeSceneTransitionAnimation(@NonNull Activity activity,
            Pair<View, String>... sharedElements) {
        if (Build.VERSION.SDK_INT >= 21) {
            android.util.Pair<View, String>[] pairs = null;
            if (sharedElements != null) {
                pairs = new android.util.Pair[sharedElements.length];
                for (int i = 0; i < sharedElements.length; i++) {
                    pairs[i] = android.util.Pair.create(
                            sharedElements[i].first, sharedElements[i].second);
                }
            }
            return new ActivityOptionsCompatImpl(
                    ActivityOptions.makeSceneTransitionAnimation(activity, pairs));
        }
        return new ActivityOptionsCompat();
    }

    /**
     * If set along with Intent.FLAG_ACTIVITY_NEW_DOCUMENT then the task being launched will not be
     * presented to the user but will instead be only available through the recents task list.
     * In addition, the new task wil be affiliated with the launching activity's task.
     * Affiliated tasks are grouped together in the recents task list.
     *
     * <p>This behavior is not supported for activities with
     * {@link android.R.attr#launchMode launchMode} values of
     * <code>singleInstance</code> or <code>singleTask</code>.
     */
    @NonNull
    public static ActivityOptionsCompat makeTaskLaunchBehind() {
        if (Build.VERSION.SDK_INT >= 21) {
            return new ActivityOptionsCompatImpl(ActivityOptions.makeTaskLaunchBehind());
        }
        return new ActivityOptionsCompat();
    }

    /**
     * Create a basic ActivityOptions that has no special animation associated with it.
     * Other options can still be set.
     */
    @NonNull
    public static ActivityOptionsCompat makeBasic() {
        if (Build.VERSION.SDK_INT >= 23) {
            return new ActivityOptionsCompatImpl(ActivityOptions.makeBasic());
        }
        return new ActivityOptionsCompat();
    }

    @RequiresApi(16)
    private static class ActivityOptionsCompatImpl extends ActivityOptionsCompat {
        private final ActivityOptions mActivityOptions;

        ActivityOptionsCompatImpl(ActivityOptions activityOptions) {
            mActivityOptions = activityOptions;
        }

        @Override
        public Bundle toBundle() {
            return mActivityOptions.toBundle();
        }

        @Override
        public void update(ActivityOptionsCompat otherOptions) {
            if (otherOptions instanceof ActivityOptionsCompatImpl) {
                ActivityOptionsCompatImpl otherImpl =
                        (ActivityOptionsCompatImpl) otherOptions;
                mActivityOptions.update(otherImpl.mActivityOptions);
            }
        }

        @Override
        public void requestUsageTimeReport(PendingIntent receiver) {
            if (Build.VERSION.SDK_INT >= 23) {
                mActivityOptions.requestUsageTimeReport(receiver);
            }
        }

        @Override
        public ActivityOptionsCompat setLaunchBounds(@Nullable Rect screenSpacePixelRect) {
            if (Build.VERSION.SDK_INT < 24) {
                return this;
            }
            return new ActivityOptionsCompatImpl(
                    mActivityOptions.setLaunchBounds(screenSpacePixelRect));
        }

        @Override
        public Rect getLaunchBounds() {
            if (Build.VERSION.SDK_INT < 24) {
                return null;
            }
            return mActivityOptions.getLaunchBounds();
        }
    }

    protected ActivityOptionsCompat() {
    }

    /**
     * Sets the bounds (window size) that the activity should be launched in.
     * Rect position should be provided in pixels and in screen coordinates.
     * Set to null explicitly for fullscreen.
     * <p>
     * <strong>NOTE:<strong/> This value is ignored on devices that don't have
     * {@link android.content.pm.PackageManager#FEATURE_FREEFORM_WINDOW_MANAGEMENT} or
     * {@link android.content.pm.PackageManager#FEATURE_PICTURE_IN_PICTURE} enabled.
     * @param screenSpacePixelRect Launch bounds to use for the activity or null for fullscreen.
     */
    @NonNull
    public ActivityOptionsCompat setLaunchBounds(@Nullable Rect screenSpacePixelRect) {
        return this;
    }

    /**
     * Returns the bounds that should be used to launch the activity.
     * @see #setLaunchBounds(Rect)
     * @return Bounds used to launch the activity.
     */
    @Nullable
    public Rect getLaunchBounds() {
        return null;
    }

    /**
     * Returns the created options as a Bundle, which can be passed to
     * {@link androidx.core.content.ContextCompat#startActivity(Context, android.content.Intent, Bundle)}.
     * Note that the returned Bundle is still owned by the ActivityOptions
     * object; you must not modify it, but can supply it to the startActivity
     * methods that take an options Bundle.
     */
    @Nullable
    public Bundle toBundle() {
        return null;
    }

    /**
     * Update the current values in this ActivityOptions from those supplied in
     * otherOptions. Any values defined in otherOptions replace those in the
     * base options.
     */
    public void update(@NonNull ActivityOptionsCompat otherOptions) {
        // Do nothing.
    }

    /**
     * Ask the the system track that time the user spends in the app being launched, and
     * report it back once done.  The report will be sent to the given receiver, with
     * the extras {@link #EXTRA_USAGE_TIME_REPORT} and {@link #EXTRA_USAGE_TIME_REPORT_PACKAGES}
     * filled in.
     *
     * <p>The time interval tracked is from launching this activity until the user leaves
     * that activity's flow.  They are considered to stay in the flow as long as
     * new activities are being launched or returned to from the original flow,
     * even if this crosses package or task boundaries.  For example, if the originator
     * starts an activity to view an image, and while there the user selects to share,
     * which launches their email app in a new task, and they complete the share, the
     * time during that entire operation will be included until they finally hit back from
     * the original image viewer activity.</p>
     *
     * <p>The user is considered to complete a flow once they switch to another
     * activity that is not part of the tracked flow.  This may happen, for example, by
     * using the notification shade, launcher, or recents to launch or switch to another
     * app.  Simply going in to these navigation elements does not break the flow (although
     * the launcher and recents stops time tracking of the session); it is the act of
     * going somewhere else that completes the tracking.</p>
     *
     * @param receiver A broadcast receiver that will receive the report.
     */
    public void requestUsageTimeReport(@NonNull PendingIntent receiver) {
        // Do nothing.
    }
}
