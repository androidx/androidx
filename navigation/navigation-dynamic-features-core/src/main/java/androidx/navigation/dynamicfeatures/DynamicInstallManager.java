/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigator;
import androidx.navigation.dynamicfeatures.DynamicGraphNavigator.DynamicNavGraph;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

/**
 * Install manager for dynamic features.
 * <p/>
 * Enables installation of dynamic features.
 */
public class DynamicInstallManager {
    @NonNull
    private final SplitInstallManager mSplitInstallManager;
    @NonNull
    private final Context mContext;

    /**
     * Create a DynamicInstallManager.
     *
     * @param context             The context the manager is using.
     * @param splitInstallManager The {@link SplitInstallManager} to use.
 *                            This is provided through the PlayCore library.
     */
    public DynamicInstallManager(
            @NonNull Context context, @NonNull SplitInstallManager splitInstallManager) {
        mContext = context;
        mSplitInstallManager = splitInstallManager;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public NavDestination performInstall(
            @NonNull NavDestination destination,
            @Nullable Bundle args,
            @Nullable DynamicExtras extras,
            @NonNull String moduleName) {
        if (extras != null && extras.getInstallMonitor() != null) {
            requestInstall(moduleName, extras.getInstallMonitor());
            return null;
        } else {
            Bundle progressArgs = new Bundle();
            progressArgs.putInt(Constants.DESTINATION_ID, destination.getId());
            progressArgs.putBundle(Constants.DESTINATION_ARGS, args);
            DynamicNavGraph dynamicNavGraph =
                    DynamicNavGraph.getOrThrow(destination);
            Navigator<?> navigator = dynamicNavGraph.getNavigatorProvider().getNavigator(
                    dynamicNavGraph.getNavigatorName());
            if (navigator instanceof DynamicGraphNavigator) {
                return ((DynamicGraphNavigator) navigator).navigateToProgressDestination(
                        dynamicNavGraph, progressArgs);
            } else {
                throw new IllegalStateException("You must use a DynamicNavGraph to perform a "
                        + "module installation.");
            }
        }
    }

    /**
     * @param module The module to install.
     * @return Whether the requested module needs installation.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean needsInstall(@NonNull String module) {
        return !mSplitInstallManager.getInstalledModules().contains(module);
    }

    private void requestInstall(@NonNull String module,
            @NonNull DynamicInstallMonitor installMonitor) {

        if (installMonitor.isUsed()) {
            // We don't want an installMonitor in an undefined state or used by another install
            throw new IllegalStateException("You must pass in a fresh DynamicInstallMonitor "
                    + "in DynamicExtras every time you call navigate().");
        }

        MutableLiveData<SplitInstallSessionState> status =
                (MutableLiveData<SplitInstallSessionState>) installMonitor.getStatus();
        installMonitor.setInstallRequired(true);

        SplitInstallRequest request =
                SplitInstallRequest
                        .newBuilder()
                        .addModule(module)
                        .build();

        mSplitInstallManager
                .startInstall(request)
                .addOnSuccessListener(sessionId -> {
                    if (sessionId == 0) { // The feature is already installed, nothing to do here.
                        installMonitor.setInstallRequired(false);
                        status.setValue(null);
                        terminateLiveData(status);
                    } else {
                        installMonitor.setSessionId(sessionId);
                        installMonitor.setSplitInstallManager(mSplitInstallManager);
                        SplitInstallStateUpdatedListener listener =
                                new SplitInstallListenerWrapper(mContext, status,
                                        installMonitor);
                        mSplitInstallManager.registerListener(listener);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.i("DynamicInstallManager",
                            "Error requesting install of "
                                    + module + ": " + exception.getMessage());
                    installMonitor.setException(exception);
                    status.setValue(null);
                    terminateLiveData(status);
                });
    }

    private static void terminateLiveData(
            @NonNull MutableLiveData<SplitInstallSessionState> status) {
        // Best effort leak prevention, will only work for active observers
        if (status.hasActiveObservers()) {
            throw new IllegalStateException("This DynamicInstallMonitor will not "
                    + "emit any more status updates. You should remove all "
                    + "Observers after null has been emitted.");
        }
    }

    private static class SplitInstallListenerWrapper
            implements SplitInstallStateUpdatedListener {

        @NonNull
        private final Context mContext;
        @NonNull
        private final MutableLiveData<SplitInstallSessionState> mStatus;
        @NonNull
        private final DynamicInstallMonitor mInstallMonitor;

        SplitInstallListenerWrapper(@NonNull Context context,
                @NonNull MutableLiveData<SplitInstallSessionState> status,
                @NonNull DynamicInstallMonitor installMonitor) {
            mContext = context;
            mStatus = status;
            mInstallMonitor = installMonitor;
        }

        @SuppressLint("SyntheticAccessor")
        @Override
        public void onStateUpdate(
                SplitInstallSessionState splitInstallSessionState) {
            if (splitInstallSessionState.sessionId() == mInstallMonitor.getSessionId()) {
                if (splitInstallSessionState.status() == SplitInstallSessionStatus.INSTALLED) {
                    SplitCompat.install(mContext);
                }
                mStatus.setValue(splitInstallSessionState);
                if (DynamicInstallMonitor.isEndState(splitInstallSessionState.status())) {
                    mInstallMonitor.getSplitInstallManager().unregisterListener(this);
                    terminateLiveData(mStatus);
                }
            }
        }
    }
}
