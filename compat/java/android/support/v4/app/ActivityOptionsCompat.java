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

package android.support.v4.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.view.View;

/**
 * Helper for accessing features in {@link android.app.ActivityOptions}
 * introduced in API level 16 in a backwards compatible fashion.
 */
public class ActivityOptionsCompat {
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
    public static ActivityOptionsCompat makeCustomAnimation(Context context,
            int enterResId, int exitResId) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new ActivityOptionsImplJB(
                ActivityOptionsCompatJB.makeCustomAnimation(context, enterResId, exitResId));
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
    public static ActivityOptionsCompat makeScaleUpAnimation(View source,
            int startX, int startY, int startWidth, int startHeight) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new ActivityOptionsImplJB(
                ActivityOptionsCompatJB.makeScaleUpAnimation(source, startX, startY,
                        startWidth, startHeight));
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
    public static ActivityOptionsCompat makeThumbnailScaleUpAnimation(View source,
            Bitmap thumbnail, int startX, int startY) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new ActivityOptionsImplJB(
                ActivityOptionsCompatJB.makeThumbnailScaleUpAnimation(source, thumbnail,
                        startX, startY));
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
    public static ActivityOptionsCompat makeSceneTransitionAnimation(Activity activity,
            View sharedElement, String sharedElementName) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new ActivityOptionsCompat.ActivityOptionsImpl21(
                    ActivityOptionsCompat21.makeSceneTransitionAnimation(activity,
                            sharedElement, sharedElementName));
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
    public static ActivityOptionsCompat makeSceneTransitionAnimation(Activity activity,
            Pair<View, String>... sharedElements) {
        if (Build.VERSION.SDK_INT >= 21) {
            View[] views = null;
            String[] names = null;
            if (sharedElements != null) {
                views = new View[sharedElements.length];
                names = new String[sharedElements.length];
                for (int i = 0; i < sharedElements.length; i++) {
                    views[i] = sharedElements[i].first;
                    names[i] = sharedElements[i].second;
                }
            }
            return new ActivityOptionsCompat.ActivityOptionsImpl21(
                    ActivityOptionsCompat21.makeSceneTransitionAnimation(activity, views, names));
        }
        return new ActivityOptionsCompat();
    }

    private static class ActivityOptionsImplJB extends ActivityOptionsCompat {
        private final ActivityOptionsCompatJB mImpl;

        ActivityOptionsImplJB(ActivityOptionsCompatJB impl) {
            mImpl = impl;
        }

        @Override
        public Bundle toBundle() {
            return mImpl.toBundle();
        }

        @Override
        public void update(ActivityOptionsCompat otherOptions) {
            if (otherOptions instanceof ActivityOptionsImplJB) {
                ActivityOptionsImplJB otherImpl = (ActivityOptionsImplJB)otherOptions;
                mImpl.update(otherImpl.mImpl);
            }
        }
    }

    private static class ActivityOptionsImpl21 extends ActivityOptionsCompat {
        private final ActivityOptionsCompat21 mImpl;

        ActivityOptionsImpl21(ActivityOptionsCompat21 impl) {
            mImpl = impl;
        }

        @Override
        public Bundle toBundle() {
            return mImpl.toBundle();
        }

        @Override
        public void update(ActivityOptionsCompat otherOptions) {
            if (otherOptions instanceof ActivityOptionsCompat.ActivityOptionsImpl21) {
                ActivityOptionsCompat.ActivityOptionsImpl21
                        otherImpl = (ActivityOptionsCompat.ActivityOptionsImpl21)otherOptions;
                mImpl.update(otherImpl.mImpl);
            }
        }
    }

    protected ActivityOptionsCompat() {
    }

    /**
     * Returns the created options as a Bundle, which can be passed to
     * {@link ActivityCompat#startActivity(android.app.Activity, android.content.Intent, android.os.Bundle)}.
     * Note that the returned Bundle is still owned by the ActivityOptions
     * object; you must not modify it, but can supply it to the startActivity
     * methods that take an options Bundle.
     */
    public Bundle toBundle() {
        return null;
    }

    /**
     * Update the current values in this ActivityOptions from those supplied in
     * otherOptions. Any values defined in otherOptions replace those in the
     * base options.
     */
    public void update(ActivityOptionsCompat otherOptions) {
        // Do nothing.
    }
}
