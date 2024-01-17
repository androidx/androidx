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
package androidx.wear.ambient;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.wearable.compat.WearableActivityController;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Use this as a headless Fragment to add ambient support to an Activity on Wearable devices.
 * <p>
 * The application that uses this should add the {@link android.Manifest.permission#WAKE_LOCK}
 * permission to its manifest.
 * <p>
 * The following describes the general use of this class:
 * <p>
 * Create a subclass of one of the {@link FragmentActivity} classes and implement the
 * {@link AmbientCallbackProvider} interface. Override the
 * {@link AmbientCallbackProvider#getAmbientCallback()} method to provide the callbacks required
 * for reacting to the ambient events from the Android system. If a valid
 * {@link AmbientCallback} is not provided (either no implementation of the
 * {@link AmbientCallbackProvider} interface, or returning null from
 * {@link AmbientCallbackProvider#getAmbientCallback()}), then ambient mode will NOT be enabled.
 * <p>
 * The primary entry  point for this code is the {@link #attach(FragmentActivity)} method.
 * It should be called with an {@link FragmentActivity} as an argument and that
 * {@link FragmentActivity} will then be able to receive ambient lifecycle events through
 * an {@link AmbientCallback}. The {@link FragmentActivity} will also receive a
 * {@link AmbientController} object from the attachment which can be used to query the current
 * status of the ambient mode.
 * <p>
 * An example of how to implement the {@link AmbientCallbackProvider} interface, attach
 * {@link AmbientModeSupport} to your {@link FragmentActivity} and use the
 * {@link AmbientController} can be found below:
 *
 * <pre class="prettyprint">
 * public class MyActivity extends FragmentActivity
 *     implements AmbientModeSupport.AmbientCallbackProvider {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState)
 *         ...
 *         AmbientModeSupport.AmbientController controller = AmbientModeSupport.attach(this);
 *         boolean isAmbient = controller.isAmbient();
 *     }
 *     {@literal @}Override
 *     AmbientModeSupport.AmbientCallback getAmbientCallback() {
 *         return new AmbientModeSupport.AmbientCallback() {
 *             public void onEnterAmbient(Bundle ambientDetails) {...}
 *             public void onExitAmbient(Bundle ambientDetails) {...}
 *         }
 *     }
 * }</pre>
 *
 * @deprecated Use {@link AmbientLifecycleObserver} instead. These classes use lifecycle
 * components instead, preventing the need to hook these events using fragments.
 */
@Deprecated
public final class AmbientModeSupport extends Fragment {
    private static final String TAG = "AmbientModeSupport";

    /**
     * Property in bundle passed to {@code AmbientCallback#onEnterAmbient(Bundle)} to indicate
     * whether burn-in protection is required. When this property is set to true, views must be
     * shifted around periodically in ambient mode. To ensure that content isn't shifted off
     * the screen, avoid placing content within 10 pixels of the edge of the screen. Activities
     * should also avoid solid white areas to prevent pixel burn-in. Both of these requirements
     * only apply in ambient mode, and only when this property is set to true.
     */
    public static final String EXTRA_BURN_IN_PROTECTION =
            WearableActivityController.EXTRA_BURN_IN_PROTECTION;

    /**
     * Property in bundle passed to {@code AmbientCallback#onEnterAmbient(Bundle)} to indicate
     * whether the device has low-bit ambient mode. When this property is set to true, the screen
     * supports fewer bits for each color in ambient mode. In this case, activities should disable
     * anti-aliasing in ambient mode.
     */
    public static final String EXTRA_LOWBIT_AMBIENT =
            WearableActivityController.EXTRA_LOWBIT_AMBIENT;

    /**
     * Fragment tag used by default when adding {@link AmbientModeSupport} to add ambient support to
     * a {@link FragmentActivity}.
     */
    public static final String FRAGMENT_TAG = "android.support.wearable.ambient.AmbientMode";

    /**
     * Interface for any {@link Activity} that wishes to implement Ambient Mode. Use the
     * {@link #getAmbientCallback()} method to return an {@link AmbientCallback} which can be used
     * to bind the {@link AmbientModeSupport} to the instantiation of this interface.
     * <p>
     * <pre class="prettyprint">{@code
     * return new AmbientModeSupport.AmbientCallback() {
     *     public void onEnterAmbient(Bundle ambientDetails) {...}
     *     public void onExitAmbient(Bundle ambientDetails) {...}
     *  }
     * }</pre>
     */
    public interface AmbientCallbackProvider {
        /**
         * @return the {@link AmbientCallback} to be used by this class to communicate with the
         * entity interested in ambient events.
         */
        AmbientCallback getAmbientCallback();
    }

    /**
     * Callback to receive ambient mode state changes. It must be used by all users of
     * AmbientModeSupport.
     */
    public abstract static class AmbientCallback {
        /**
         * Called when an activity is entering ambient mode. This event is sent while an activity is
         * running (after onResume, before onPause). All drawing should complete by the conclusion
         * of this method. Note that {@code invalidate()} calls will be executed before resuming
         * lower-power mode.
         *
         * @param ambientDetails bundle containing information about the display being used.
         *                      It includes information about low-bit color and burn-in protection.
         */
        public void onEnterAmbient(Bundle ambientDetails) {}

        /**
         * Called when the system is updating the display for ambient mode. Activities may use this
         * opportunity to update or invalidate views.
         */
        public void onUpdateAmbient() {}

        /**
         * Called when an activity should exit ambient mode. This event is sent while an activity is
         * running (after onResume, before onPause).
         */
        public void onExitAmbient() {}

        /**
         * Called to inform an activity that whatever decomposition it has sent to Sidekick is no
         * longer valid and should be re-sent before enabling ambient offload.
         */
        public void onAmbientOffloadInvalidated() {}
    }

    private final AmbientDelegate.AmbientCallback mCallback =
            new AmbientDelegate.AmbientCallback() {
                @Override
                public void onEnterAmbient(Bundle ambientDetails) {
                    if (mSuppliedCallback != null) {
                        mSuppliedCallback.onEnterAmbient(ambientDetails);
                    }
                }

                @Override
                public void onExitAmbient() {
                    if (mSuppliedCallback != null) {
                        mSuppliedCallback.onExitAmbient();
                    }
                }

                @Override
                public void onUpdateAmbient() {
                    if (mSuppliedCallback != null) {
                        mSuppliedCallback.onUpdateAmbient();
                    }
                }

                @Override
                public void onAmbientOffloadInvalidated() {
                    if (mSuppliedCallback != null) {
                        mSuppliedCallback.onAmbientOffloadInvalidated();
                    }
                }
            };
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    AmbientDelegate mDelegate;
    @Nullable
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    AmbientCallback mSuppliedCallback;
    private AmbientController mController;

    /**
     * Constructor
     */
    public AmbientModeSupport() {
        mController = new AmbientController();
    }

    @Override
    @CallSuper
    public void onAttach(Context context) {
        super.onAttach(context);
        mDelegate = new AmbientDelegate(getActivity(), new WearableControllerProvider(), mCallback);

        if (context instanceof AmbientCallbackProvider) {
            mSuppliedCallback = ((AmbientCallbackProvider) context).getAmbientCallback();
        } else {
            Log.w(TAG, "No callback provided - enabling only smart resume");
        }
    }

    @Override
    @CallSuper
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDelegate.onCreate();
        if (mSuppliedCallback != null) {
            mDelegate.setAmbientEnabled();
        }
    }

    @Override
    @CallSuper
    public void onResume() {
        super.onResume();
        mDelegate.onResume();
    }

    @Override
    @CallSuper
    public void onPause() {
        mDelegate.onPause();
        super.onPause();
    }

    @Override
    @CallSuper
    public void onStop() {
        mDelegate.onStop();
        super.onStop();
    }

    @Override
    @CallSuper
    public void onDestroy() {
        mDelegate.onDestroy();
        super.onDestroy();
    }

    @Override
    @CallSuper
    public void onDetach() {
        mDelegate = null;
        super.onDetach();
    }

    /**
     * Attach ambient support to the given activity. Calling this method with an Activity
     * implementing the {@link AmbientCallbackProvider} interface will provide you with an
     * opportunity to react to ambient events such as {@code onEnterAmbient}. Alternatively,
     * you can call this method with an Activity which does not implement
     * the {@link AmbientCallbackProvider} interface and that will only enable the auto-resume
     * functionality. This is equivalent to providing (@code null} from
     * the {@link AmbientCallbackProvider}.
     *
     * @param activity the activity to attach ambient support to.
     * @return the associated {@link AmbientController} which can be used to query the state of
     * ambient mode.
     */
    public static <T extends FragmentActivity> AmbientController attach(T activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        AmbientModeSupport ambientFragment =
                (AmbientModeSupport) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (ambientFragment == null) {
            AmbientModeSupport fragment = new AmbientModeSupport();
            fragmentManager
                    .beginTransaction()
                    .add(fragment, FRAGMENT_TAG)
                    .commit();
            ambientFragment = fragment;
        }
        return ambientFragment.mController;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        if (mDelegate != null) {
            mDelegate.dump(prefix, fd, writer, args);
        }
    }

    @VisibleForTesting
    void setAmbientDelegate(AmbientDelegate delegate) {
        mDelegate = delegate;
    }

    /**
     * A class for interacting with the ambient mode on a wearable device. This class can be used to
     * query the current state of ambient mode. An instance of this class is returned to the user
     * when they attach their {@link Activity} to {@link AmbientModeSupport}.
     */
    public final class AmbientController {
        private static final String TAG = "AmbientController";

        // Do not initialize outside of this class.
        AmbientController() {}

        /**
         * @return {@code true} if the activity is currently in ambient.
         */
        public boolean isAmbient() {
            return mDelegate == null ? false : mDelegate.isAmbient();
        }

        /**
         * Sets whether this activity is currently in a state that supports ambient offload mode.
         */
        public void setAmbientOffloadEnabled(boolean enabled) {
            if (mDelegate != null) {
                mDelegate.setAmbientOffloadEnabled(enabled);
            }
        }

        /**
         * Sets whether this activity's task should be moved to the front when the system exits
         * ambient mode. If true, the activity's task may be moved to the front if it was the
         * last activity to be running when ambient started, depending on how much time the
         * system spent in ambient mode.
         */
        public void setAutoResumeEnabled(boolean enabled) {
            if (mDelegate != null) {
                mDelegate.setAutoResumeEnabled(enabled);
            }
        }
    }
}
