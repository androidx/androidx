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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.dynamicfeatures.Constants;
import androidx.navigation.dynamicfeatures.DynamicExtras;
import androidx.navigation.dynamicfeatures.DynamicInstallMonitor;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

/**
 * The default fragment to display during installation progress.
 */
public abstract class AbstractProgressFragment extends Fragment {

    private static final int INSTALL_REQUEST_CODE = 1;
    private static final String TAG = "AbstractProgress";
    private boolean mNavigated = false;
    private NavController mNavController;
    private int mDestinationId;
    private Bundle mDestinationArgs;
    private NavOptions mDestinationNavOpts;
    private StateObserver mObserver = new StateObserver();

    @Nullable
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
        mDestinationNavOpts = arguments.getParcelable(Constants.DESTINATION_NAVOPTS);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNavigated) {
            mNavController.popBackStack();
            return;
        }
        LiveData<SplitInstallSessionState> status = mInstallViewModel.getInstallStatus();
        if (status == null) {
            Log.i(TAG, "onResume: STATUS is null, navigating");
            navigate();
            status = mInstallViewModel.getInstallStatus();
        }
        if (status != null) {
            Log.i(TAG, "onResume: STATUS is now not null, observing");
            status.observe(this, mObserver);
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
        mNavController.navigate(mDestinationId, mDestinationArgs, mDestinationNavOpts, extras);
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

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected class StateObserver implements Observer<SplitInstallSessionState> {
        @Override
        public void onChanged(SplitInstallSessionState sessionState) {
            Log.i(TAG, String.valueOf(sessionState != null ? sessionState.status() : 0));
            @SuppressLint("SyntheticAccessor")
            DynamicInstallMonitor monitor = mInstallViewModel.getInstallMonitor();
            if (sessionState == null) {
                Log.i(TAG, "onChanged: sessionstate is null");
                if (monitor.hasException()) {
                    onException(monitor.getException());
                } else if (!monitor.isInstallRequired()) {
                    Log.i(TAG, "onChanged: install not required");
                    monitor.getStatus().removeObserver(this);
                    navigate();
                }
            } else {
                onProgress(sessionState.bytesDownloaded(),
                        sessionState.totalBytesToDownload());

                if (DynamicInstallMonitor.isEndState(sessionState.status())) {
                    monitor.getStatus().removeObserver(this);
                }
                switch (sessionState.status()) {
                    case SplitInstallSessionStatus.INSTALLED:
                        Log.i(TAG, "onChanged: INSTALLED");
                        navigate();
                        break;
                    case SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                        try {
                            // TODO: request Play Core to support fragments for resolution handling
                            startIntentSenderForResult(
                                    sessionState.resolutionIntent().getIntentSender(),
                                    INSTALL_REQUEST_CODE,
                                    null, 0, 0, 0, null);
                        } catch (IntentSender.SendIntentException e) {
                            onException(e);
                        }
                        break;
                    case SplitInstallSessionStatus.CANCELED:
                        onCancelled();
                        break;
                    case SplitInstallSessionStatus.FAILED:
                        onFailed();
                        break;
                    default:
                        Log.i(TAG, "onChanged: DEFAULT" + sessionState.status());
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
     * @param exception The occurred exception.
     */
    protected abstract void onException(@NonNull Exception exception);

    /**
     * Called when there was a progress update for an active module download.
     *
     * @param bytesDownloaded The bytes downloaded so far.
     * @param bytesTotal      The total bytes to be downloaded.
     */
    protected abstract void onProgress(long bytesDownloaded, long bytesTotal);

    /**
     * Called when the user decided to cancel installation.
     */
    protected abstract void onCancelled();

    /**
     * Called when the installation has failed due to non-user issues.
     */
    protected abstract void onFailed();

    /**
     * Set arguments for an AbstractProgressFragment.
     *
     * @param fragment The target fragment.
     */
    public static void setArguments(@NonNull AbstractProgressFragment fragment, int destinationId,
            @Nullable Bundle destinationArguments) {
        Bundle args = new Bundle();
        args.putInt(Constants.DESTINATION_ID, destinationId);
        if (destinationArguments != null) {
            args.putBundle(Constants.DESTINATION_ARGS, destinationArguments);
        }
        fragment.setArguments(args);
    }
}
