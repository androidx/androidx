/*
 * Copyright (C) 2017 The Android Open Source Project
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


import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.util.SparseIntArray;
import android.view.Window;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * This class can be used to record and return data about per-frame durations. It returns those
 * results in an array per metric type, with the results indicating how many samples were
 * recorded for each duration value. The details of the durations data are described in
 * {@link #getMetrics()}.
 * <p>
 * For more information on the various metrics tracked, see the documentation for the
 * <a href="https://developer.android.com/reference/android/view/FrameMetrics.html">FrameMetrics
 * </a> API added in API 24 as well as the
 * <a href="https://developer.android.com/studio/profile/dev-options-rendering.html">GPU Profiling
 * guide</a>.
 */
public class FrameMetricsAggregator {

    private static final String TAG = "FrameMetrics";
    private static final boolean DBG = false;

    /**
     * The index in the metrics array where the data for {@link #TOTAL_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int TOTAL_INDEX          = 0;
    /**
     * The index in the metrics array where the data for {@link #INPUT_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int INPUT_INDEX          = 1;
    /**
     * The index in the metrics array where the data for {@link #LAYOUT_MEASURE_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int LAYOUT_MEASURE_INDEX = 2;
    /**
     * The index in the metrics array where the data for {@link #DRAW_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int DRAW_INDEX           = 3;
    /**
     * The index in the metrics array where the data for {@link #SYNC_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int SYNC_INDEX           = 4;
    /**
     * The index in the metrics array where the data for {@link #SYNC_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int COMMAND_INDEX        = 5;
    /**
     * The index in the metrics array where the data for {@link #COMMAND_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int SWAP_INDEX           = 6;
    /**
     * The index in the metrics array where the data for {@link #DELAY_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int DELAY_INDEX          = 7;
    /**
     * The index in the metrics array where the data for {@link #ANIMATION_DURATION}
     * is stored.
     * @see #getMetrics()
     */
    public static final int ANIMATION_INDEX      = 8;
    private static final int LAST_INDEX          = 8;

    /**
     * A flag indicating that the metrics should track the total duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int TOTAL_DURATION          = 1 << TOTAL_INDEX;
    /**
     * A flag indicating that the metrics should track the input duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int INPUT_DURATION          = 1 << INPUT_INDEX;
    /**
     * A flag indicating that the metrics should track the layout duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int LAYOUT_MEASURE_DURATION = 1 << LAYOUT_MEASURE_INDEX;
    /**
     * A flag indicating that the metrics should track the draw duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int DRAW_DURATION           = 1 << DRAW_INDEX;
    /**
     * A flag indicating that the metrics should track the sync duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int SYNC_DURATION           = 1 << SYNC_INDEX;
    /**
     * A flag indicating that the metrics should track the command duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int COMMAND_DURATION        = 1 << COMMAND_INDEX;
    /**
     * A flag indicating that the metrics should track the swap duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int SWAP_DURATION           = 1 << SWAP_INDEX;
    /**
     * A flag indicating that the metrics should track the delay duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int DELAY_DURATION          = 1 << DELAY_INDEX;
    /**
     * A flag indicating that the metrics should track the animation duration. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate all of the metrics that should be tracked for that activity.
     */
    public static final int ANIMATION_DURATION      = 1 << ANIMATION_INDEX;
    /**
     * A flag indicating that the metrics should track all durations. This is
     * a shorthand for OR'ing all of the duration flags. This
     * flag may be OR'd with the other flags here when calling {@link #FrameMetricsAggregator(int)}
     * to indicate the metrics that should be tracked for that activity.
     */
    public static final int EVERY_DURATION          = 0x1ff;

    private FrameMetricsBaseImpl mInstance;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {
                    TOTAL_DURATION,
                    INPUT_DURATION,
                    LAYOUT_MEASURE_DURATION,
                    DRAW_DURATION,
                    SYNC_DURATION,
                    COMMAND_DURATION,
                    SWAP_DURATION,
                    DELAY_DURATION,
                    ANIMATION_DURATION,
                    EVERY_DURATION
            })
    public @interface MetricType {}

    /**
     * Constructs a FrameMetricsAggregator object that will track {@link #TOTAL_DURATION}
     * metrics. If more fine-grained metrics are needed, use {@link #FrameMetricsAggregator(int)}
     * instead.
     */
    public FrameMetricsAggregator() {
        this(TOTAL_DURATION);
    }

    /**
     * Constructs a FrameMetricsAggregator object that will track the metrics specified bty
     * {@code metricTypeFlags}, which is a value derived by OR'ing together metrics constants
     * such as {@link #TOTAL_DURATION} to specify all metrics that should be tracked. For example,
     * {@code TOTAL_DURATION | DRAW_DURATION} will track both the total and draw durations
     * for every frame.
     *
     * @param metricTypeFlags A bitwise collection of flags indicating which metrics should
     * be recorded.
     */
    public FrameMetricsAggregator(@MetricType int metricTypeFlags) {
        if (Build.VERSION.SDK_INT >= 24) {
            mInstance = new FrameMetricsApi24Impl(metricTypeFlags);
        } else {
            mInstance = new FrameMetricsBaseImpl();
        }
    }

    /**
     * Starts recording frame metrics for the given activity.
     *
     * @param activity The Activity object which will have its metrics measured.
     */
    public void add(@NonNull Activity activity) {
        mInstance.add(activity);
    }

    /**
     * Stops recording metrics for {@code activity} and returns the collected metrics so far.
     * Recording will continue if there are still other activities being tracked. Calling
     * remove() does not reset the metrics array; you must call {@link #reset()} to clear the
     * data.
     *
     * @param activity The Activity to stop tracking metrics for.
     * @return An array whose index refers to the type of metric stored in that item's
     * SparseIntArray object, e.g., data for {@code TOTAL_DURATION} is stored in
     * the {@code [TOTAL_INDEX]} item.
     * @see #getMetrics()
     */
    @Nullable
    public SparseIntArray[] remove(@NonNull Activity activity) {
        return mInstance.remove(activity);
    }

    /**
     * Stops recording metrics for all Activities currently being tracked. Like {@link
     * #remove(Activity)}, this method returns the currently-collected metrics. Calling
     * stop() does not reset the metrics array; you must call {@link #reset()} to clear the
     * data.
     *
     * @return An array whose index refers to the type of metric stored in that item's
     * SparseIntArray object, e.g., data for {@code TOTAL_DURATION} is stored in
     * the {@code [TOTAL_INDEX]} item.
     * @see #remove(Activity)
     * @see #getMetrics()
     */
    @Nullable
    public SparseIntArray[] stop() {
        return mInstance.stop();
    }

    /**
     * Resets the metrics data and returns the currently-collected metrics.
     *
     * @return An array whose index refers to the type of metric stored in that item's
     * SparseIntArray object, e.g., data for {@code TOTAL_DURATION} is stored in
     * the {@code [TOTAL_INDEX]} item.
     * @see #getMetrics()
     */
    @Nullable
    public SparseIntArray[] reset() {
        return mInstance.reset();
    }

    /**
     * Returns the currently-collected metrics in an array of SparseIntArray objects.
     * The index of the array indicates which metric's data is stored in that
     * SparseIntArray object. For example, results for total duration will be in
     * the {@code [TOTAL_INDEX]} item.
     * <p>
     * The return value may be null if no metrics were tracked. This is especially true on releases
     * earlier than API 24, as the FrameMetrics system does not exist on these earlier release.
     * If the return value is not null, any of the objects at a given index in the array
     * may still be null, which indicates that data was not being tracked for that type of metric.
     * For example, if the FrameMetricsAggregator was created with a call to
     * {@code new FrameMetricsAggregator(TOTAL_DURATION | DRAW_DURATION)}, then the SparseIntArray
     * at index {@code INPUT_INDEX} will be null.
     * <p>
     * For a given non-null SparseIntArray, the results stored are the number of samples at
     * each millisecond value (rounded). For example, if a data sample consisted of total
     * durations of 5.1ms, 5.8ms, 6.1ms, and 8.2ms, the SparseIntArray at {@code [TOTAL_DURATION]}
     * would have key-value pairs (5, 1), (6, 2), (8, 1).
     *
     * @return An array whose index refers to the type of metric stored in that item's
     * SparseIntArray object, e.g., data for {@code TOTAL_DURATION} is stored in
     * the {@code [TOTAL_INDEX]} item.
     */
    @Nullable
    public SparseIntArray[] getMetrics() {
        return mInstance.getMetrics();
    }

    /**
     * Base implementation noops everything - there's no data to return on pre-API24 releases.
     */
    private static class FrameMetricsBaseImpl {

        public void add(Activity activity) {
        }

        public SparseIntArray[] remove(Activity activity) {
            return null;
        }

        public SparseIntArray[] stop() {
            return null;
        }

        public SparseIntArray[] getMetrics() {
            return null;
        }

        public SparseIntArray[] reset() {
            return null;
        }
    }

    @RequiresApi(24)
    private static class FrameMetricsApi24Impl extends FrameMetricsBaseImpl {

        private static final int NANOS_PER_MS = 1000000;
        // rounding value adds half a millisecond, for rounding to nearest ms
        private static final int NANOS_ROUNDING_VALUE = NANOS_PER_MS / 2;
        private int mTrackingFlags;
        private SparseIntArray[] mMetrics = new SparseIntArray[LAST_INDEX + 1];
        private ArrayList<WeakReference<Activity>> mActivities = new ArrayList<>();
        private static HandlerThread sHandlerThread = null;
        private static Handler sHandler = null;

        FrameMetricsApi24Impl(int trackingFlags) {
            mTrackingFlags = trackingFlags;
        }

        Window.OnFrameMetricsAvailableListener mListener =
                new Window.OnFrameMetricsAvailableListener() {
            @Override
            public void onFrameMetricsAvailable(Window window,
                    android.view.FrameMetrics frameMetrics, int dropCountSinceLastInvocation) {
                if ((mTrackingFlags & TOTAL_DURATION) != 0) {
                    addDurationItem(mMetrics[TOTAL_INDEX],
                            frameMetrics.getMetric(android.view.FrameMetrics.TOTAL_DURATION));
                }
                if ((mTrackingFlags & INPUT_DURATION) != 0) {
                    addDurationItem(mMetrics[INPUT_INDEX],
                            frameMetrics.getMetric(
                                    android.view.FrameMetrics.INPUT_HANDLING_DURATION));
                }
                if ((mTrackingFlags & LAYOUT_MEASURE_DURATION) != 0) {
                    addDurationItem(mMetrics[LAYOUT_MEASURE_INDEX],
                            frameMetrics.getMetric(
                                    android.view.FrameMetrics.LAYOUT_MEASURE_DURATION));
                }
                if ((mTrackingFlags & DRAW_DURATION) != 0) {
                    addDurationItem(mMetrics[DRAW_INDEX],
                            frameMetrics.getMetric(android.view.FrameMetrics.DRAW_DURATION));
                }
                if ((mTrackingFlags & SYNC_DURATION) != 0) {
                    addDurationItem(mMetrics[SYNC_INDEX],
                            frameMetrics.getMetric(android.view.FrameMetrics.SYNC_DURATION));
                }
                if ((mTrackingFlags & SWAP_DURATION) != 0) {
                    addDurationItem(mMetrics[SWAP_INDEX],
                            frameMetrics.getMetric(
                                    android.view.FrameMetrics.SWAP_BUFFERS_DURATION));
                }
                if ((mTrackingFlags & COMMAND_DURATION) != 0) {
                    addDurationItem(mMetrics[COMMAND_INDEX],
                            frameMetrics.getMetric(
                                    android.view.FrameMetrics.COMMAND_ISSUE_DURATION));
                }
                if ((mTrackingFlags & DELAY_DURATION) != 0) {
                    addDurationItem(mMetrics[DELAY_INDEX],
                            frameMetrics.getMetric(
                                    android.view.FrameMetrics.UNKNOWN_DELAY_DURATION));
                }
                if ((mTrackingFlags & ANIMATION_DURATION) != 0) {
                    addDurationItem(mMetrics[ANIMATION_INDEX],
                            frameMetrics.getMetric(
                                    android.view.FrameMetrics.ANIMATION_DURATION));
                }
            }
        };

        void addDurationItem(SparseIntArray buckets, long duration) {
            if (buckets != null) {
                int durationMs = (int) ((duration + NANOS_ROUNDING_VALUE) / NANOS_PER_MS);
                if (duration >= 0) {
                    // ignore values < 0; something must have gone wrong
                    int oldValue = buckets.get(durationMs);
                    buckets.put(durationMs, (oldValue + 1));
                }
            }
        }

        @Override
        public void add(Activity activity) {
            if (sHandlerThread == null) {
                sHandlerThread = new HandlerThread("FrameMetricsAggregator");
                sHandlerThread.start();
                sHandler = new Handler(sHandlerThread.getLooper());
            }
            for (int i = 0; i <= LAST_INDEX; ++i) {
                if (mMetrics[i] == null && (mTrackingFlags & (1 << i)) != 0) {
                    mMetrics[i] = new SparseIntArray();
                }
            }
            activity.getWindow().addOnFrameMetricsAvailableListener(mListener, sHandler);
            mActivities.add(new WeakReference<>(activity));
        }

        @Override
        public SparseIntArray[] remove(Activity activity) {
            for (WeakReference<Activity> activityRef : mActivities) {
                if (activityRef.get() == activity) {
                    mActivities.remove(activityRef);
                    break;
                }
            }
            activity.getWindow().removeOnFrameMetricsAvailableListener(mListener);
            return mMetrics;
        }

        @Override
        public SparseIntArray[] stop() {
            int size = mActivities.size();
            for (int i = size - 1; i >= 0; i--) {
                WeakReference<Activity> ref = mActivities.get(i);
                Activity activity = ref.get();
                if (ref.get() != null) {
                    activity.getWindow().removeOnFrameMetricsAvailableListener(mListener);
                    mActivities.remove(i);
                }
            }
            return mMetrics;
        }

        @Override
        public SparseIntArray[] getMetrics() {
            return mMetrics;
        }

        @Override
        public SparseIntArray[] reset() {
            SparseIntArray[] returnVal = mMetrics;
            mMetrics = new SparseIntArray[LAST_INDEX + 1];
            return returnVal;
        }

    }

}
