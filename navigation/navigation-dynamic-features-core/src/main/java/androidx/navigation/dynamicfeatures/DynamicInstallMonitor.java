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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

/**
 * Monitor installation progress of dynamic feature modules.
 * This class enables you to subscribe to the current installation state via {@link #getStatus()}.
 * You also can perform various checks on installation state directly through this monitor.
 *
 * In order to enable installation and monitoring of progress you'll have to provide an instance
 * of this class to {@link DynamicExtras}.
 */
public final class DynamicInstallMonitor {

    @Nullable
    private Exception mException;
    @NonNull
    private LiveData<SplitInstallSessionState> mStatus;
    private boolean mInstallRequired;
    private int mSessionId;
    @Nullable
    private SplitInstallManager mSplitInstallManager;
    private boolean mIsUsed;

    /**
     * Create a {@link DynamicInstallMonitor}.
     */
    public DynamicInstallMonitor() {
        mStatus = new MutableLiveData<>();
    }

    /**
     * Check if the installation status is in a final state.
     *
     * Soon this method will be provided through PlayCore and removed from this library before
     * the first stable release.
     *
     * @param status The {@link SplitInstallSessionStatus} to check.
     * @return <code>true</code> if the state is final, <code>false</code> otherwise.
     *
     */
    public static boolean isEndState(@SplitInstallSessionStatus int status) {
        // TODO: b/142674186 Remove once PlayCore supports this.
        return status == SplitInstallSessionStatus.UNKNOWN
                || status == SplitInstallSessionStatus.INSTALLED
                || status == SplitInstallSessionStatus.FAILED
                || status == SplitInstallSessionStatus.CANCELED;
    }

    /**
     * Get the current status.
     *
     * @return The current {@link SplitInstallSessionStatus} wrapped in a {@link LiveData}.
     */
    @NonNull
    public LiveData<SplitInstallSessionState> getStatus() {
        return mStatus;
    }

    /**
     * @param installRequired <code>true</code> if installation is required, else
     *                        <code>false</code>.
     */
    void setInstallRequired(boolean installRequired) {
        mInstallRequired = installRequired;
        if (installRequired) {
            mIsUsed = true;
        }
    }

    /**
     * Checks whether an installation is required.
     *
     * @return <code>true</code> if installation is required, <code>false</code> otherwise.
     */
    public boolean isInstallRequired() {
        return mInstallRequired;
    }

    /**
     * @param exception The exception if any occurred.
     */
    void setException(@Nullable Exception exception) {
        mException = exception;
    }

    /**
     * <code>true</code> if the monitor has been used to request an install, else
     * <code>false</code>.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    boolean isUsed() {
        return mIsUsed;
    }

    /**
     * The occurred exception, if any.
     */
    @Nullable
    public Exception getException() {
        return mException;
    }

    /**
     * @return <code>true</code> if an Exception is set, <code>false</code> otherwise.
     */
    public boolean hasException() {
        return mException != null;
    }

    /**
     * Cancel the running installation with the current session.
     */
    public void cancelInstall() {
        if (mSplitInstallManager != null && mSessionId != 0) {
            mSplitInstallManager.cancelInstall(mSessionId);
        }
    }

    /**
     * @param sessionId The PlayCore session id.
     */
    public void setSessionId(int sessionId) {
        mSessionId = sessionId;
    }

    /**
     * @return The session id for this installation session.
     */
    public int getSessionId() {
        return mSessionId;
    }

    /**
     * @param splitInstallManager The {@link SplitInstallManager} used to monitor the installation.
     */
    void setSplitInstallManager(@Nullable SplitInstallManager splitInstallManager) {
        mSplitInstallManager = splitInstallManager;
    }

    /**
     * @return The {@link SplitInstallManager} used for monitoring if any was set.
     */
    @Nullable
    SplitInstallManager getSplitInstallManager() {
        return mSplitInstallManager;
    }
}
