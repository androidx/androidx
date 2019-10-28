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

package androidx.navigation.dynamicfeatures.fragment.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.dynamicfeatures.Constants;
import androidx.navigation.dynamicfeatures.DynamicExtras;
import androidx.navigation.dynamicfeatures.DynamicInstallMonitor;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

/**
 * The base class for fragments that handle dynamic feature installation.
 *
 * <p>When extending from this class, you are responsible for updating the UI in
 * {@link #onCancelled()}, {@link #onFailed(int)}, {@link #onProgress(int, long, long)}.</p>
 * <p>The installation process is handled automatically and navigation will happen
 * once the install is completed.</p>
 */
public abstract class AbstractProgressFragment extends Fragment {

    private static final int INSTALL_REQUEST_CODE = 1;
    private static final String TAG = "AbstractProgress";
    private boolean mNavigated = false;
    private NavController mNavController;
    private int mDestinationId;
    private Bundle mDestinationArgs;
    private InstallViewModel mInstallViewModel;

    public AbstractProgressFragment() {
    }

    public AbstractProgressFragment(int contentLayoutId) {
        super(contentLayoutId);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mNavController = NavHostFragment.findNavController(this);
        mInstallViewModel = InstallViewModel.getInstance(getViewModelStore());
        if (savedInstanceState != null) {
            mNavigated = savedInstanceState.getBoolean(Constants.KEY_NAVIGATED, false);
        }
        Bundle arguments = requireArguments();
        mDestinationId = arguments.getInt(Constants.DESTINATION_ID);
        mDestinationArgs = arguments.getBundle(Constants.DESTINATION_ARGS);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNavigated) {
            mNavController.popBackStack();
            return;
        }
        DynamicInstallMonitor monitor = mInstallViewModel.getInstallMonitor();
        if (monitor == null) {
            Log.i(TAG, "onResume: monitor is null, navigating");
            navigate();
            monitor = mInstallViewModel.getInstallMonitor();
        }
        if (monitor != null) {
            Log.i(TAG, "onResume: monitor is now not null, observing");
            monitor.getStatus().observe(this, new StateObserver(monitor));
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected void navigate() {
        Log.i(TAG, "navigate: ");
        DynamicInstallMonitor installMonitor = new DynamicInstallMonitor();
        DynamicExtras extras =
                new DynamicExtras.Builder()
                        .setInstallMonitor(installMonitor)
                        .build();
        mNavController.navigate(mDestinationId, mDestinationArgs, null, extras);
        if (!installMonitor.isInstallRequired()) {
            Log.i(TAG, "navigate: install not required");
            mNavigated = true;
        } else {
            Log.i(TAG, "navigate: setting install monitor");
            mInstallViewModel.setInstallMonitor(installMonitor);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Constants.KEY_NAVIGATED, mNavigated);
    }

    private class StateObserver implements Observer<SplitInstallSessionState> {
        private final DynamicInstallMonitor mMonitor;

        StateObserver(@NonNull DynamicInstallMonitor monitor) {
            this.mMonitor = monitor;
        }

        @Override
        public void onChanged(SplitInstallSessionState sessionState) {
            if (sessionState != null) {
                if (sessionState.hasTerminalStatus()) {
                    mMonitor.getStatus().removeObserver(this);
                }
                switch (sessionState.status()) {
                    case SplitInstallSessionStatus.INSTALLED:
                        Log.i(TAG, "onChanged: INSTALLED");
                        navigate();
                        break;
                    case SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                        try {
                            startIntentSenderForResult(
                                    sessionState.resolutionIntent().getIntentSender(),
                                    INSTALL_REQUEST_CODE,
                                    null, 0, 0, 0, null);
                        } catch (IntentSender.SendIntentException e) {
                            onFailed(SplitInstallErrorCode.INTERNAL_ERROR);
                        }
                        break;
                    case SplitInstallSessionStatus.CANCELED:
                        onCancelled();
                        break;
                    case SplitInstallSessionStatus.FAILED:
                        onFailed(sessionState.errorCode());
                        break;
                    case SplitInstallSessionStatus.UNKNOWN:
                        onFailed(SplitInstallErrorCode.INTERNAL_ERROR);
                        break;
                    case SplitInstallSessionStatus.CANCELING:
                    case SplitInstallSessionStatus.DOWNLOADED:
                    case SplitInstallSessionStatus.DOWNLOADING:
                    case SplitInstallSessionStatus.INSTALLING:
                    case SplitInstallSessionStatus.PENDING:
                        Log.i(TAG, "onChanged: status " + sessionState.status());
                        onProgress(
                                sessionState.status(),
                                sessionState.bytesDownloaded(),
                                sessionState.totalBytesToDownload()
                        );
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                onCancelled();
            }
        }
    }

    /**
     * Called when there was a progress update for an active module download.
     *
     * @param status          the current installation status from SplitInstallSessionStatus
     * @param bytesDownloaded The bytes downloaded so far.
     * @param bytesTotal      The total bytes to be downloaded (can be 0 for some status updates)
     */
    protected abstract void onProgress(@SplitInstallSessionStatus int status,
            long bytesDownloaded, long bytesTotal);

    /**
     * Called when the user decided to cancel installation.
     */
    protected abstract void onCancelled();

    /**
     * Called when the installation has failed due to non-user issues.
     *
     * <p>Please check {@link SplitInstallErrorCode} for error code constants.</p>
     *
     * @param errorCode contains the error code of the installation failure.
     */
    protected abstract void onFailed(@SplitInstallErrorCode int errorCode);
}
