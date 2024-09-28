/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.demo.embedding;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import static androidx.window.embedding.SplitController.SplitSupportStatus.SPLIT_ERROR_PROPERTY_NOT_DECLARED;
import static androidx.window.embedding.SplitController.SplitSupportStatus.SPLIT_UNAVAILABLE;
import static androidx.window.embedding.SplitRule.FinishBehavior.ADJACENT;
import static androidx.window.embedding.SplitRule.FinishBehavior.ALWAYS;
import static androidx.window.embedding.SplitRule.FinishBehavior.NEVER;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.window.WindowSdkExtensions;
import androidx.window.demo.R;
import androidx.window.demo.common.EdgeToEdgeActivity;
import androidx.window.demo.databinding.ActivitySplitActivityLayoutBinding;
import androidx.window.embedding.ActivityEmbeddingController;
import androidx.window.embedding.ActivityEmbeddingOptions;
import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.ActivityStack;
import androidx.window.embedding.DividerAttributes;
import androidx.window.embedding.DividerAttributes.DraggableDividerAttributes;
import androidx.window.embedding.DividerAttributes.FixedDividerAttributes;
import androidx.window.embedding.EmbeddedActivityWindowInfo;
import androidx.window.embedding.EmbeddingRule;
import androidx.window.embedding.RuleController;
import androidx.window.embedding.SplitAttributes;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitInfo;
import androidx.window.embedding.SplitPairFilter;
import androidx.window.embedding.SplitPairRule;
import androidx.window.embedding.SplitPinRule;
import androidx.window.embedding.SplitPlaceholderRule;
import androidx.window.java.embedding.ActivityEmbeddingControllerCallbackAdapter;
import androidx.window.java.embedding.SplitControllerCallbackAdapter;

import kotlin.Unit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sample showcase of split activity rules. Allows the user to select some split configuration
 * options with checkboxes and launch activities with those options applied.
 */
public class SplitActivityBase extends EdgeToEdgeActivity {

    private static final String TAG = "SplitActivityTest";
    static final int MIN_SPLIT_WIDTH_DP = 600;
    static final float SPLIT_RATIO = 0.3f;
    static final String EXTRA_LAUNCH_C_TO_SIDE = "launch_c_to_side";

    /**
     * The {@link SplitController} adapter to use callback shaped APIs to get {@link SplitInfo}
     * changes
     */
    private SplitControllerCallbackAdapter mSplitControllerAdapter;

    private RuleController mRuleController;
    private SplitInfoCallback mSplitInfoCallback;

    private ActivityEmbeddingController mActivityEmbeddingController;
    private ActivityEmbeddingControllerCallbackAdapter mActivityEmbeddingControllerCallbackAdapter;
    private EmbeddedActivityWindowInfoCallback mEmbeddedActivityWindowInfoCallbackCallback;

    private SplitActivityRecyclerViewBindingData mRecyclerViewBindingData;

    /** In the process of updating checkboxes based on split rule. */
    private boolean mUpdatingConfigs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SplitController splitController = SplitController.getInstance(this);
        final SplitController.SplitSupportStatus splitSupportStatus =
                splitController.getSplitSupportStatus();
        if (splitSupportStatus == SPLIT_UNAVAILABLE) {
            Toast.makeText(this, R.string.toast_split_not_available, Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else if (splitSupportStatus == SPLIT_ERROR_PROPERTY_NOT_DECLARED) {
            Toast.makeText(this, R.string.toast_split_not_support, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mRecyclerViewBindingData = new SplitActivityRecyclerViewBindingData();
        final ActivitySplitActivityLayoutBinding viewBinding =
                ActivitySplitActivityLayoutBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        viewBinding.rootSplitActivityLayout.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.rootSplitActivityLayout.setAdapter(
                new SplitActivityRecyclerViewAdapter(mRecyclerViewBindingData.getItems()));
        mActivityEmbeddingController = ActivityEmbeddingController.getInstance(this);

        final int extensionVersion = WindowSdkExtensions.getInstance().getExtensionVersion();
        setupRecyclerViewItems(splitController, extensionVersion);

        mSplitControllerAdapter = new SplitControllerCallbackAdapter(splitController);
        if (extensionVersion >= 6) {
            mActivityEmbeddingControllerCallbackAdapter =
                    new ActivityEmbeddingControllerCallbackAdapter(mActivityEmbeddingController);

            // The EmbeddedActivityWindowInfoListener will only be triggered when the activity is
            // embedded and visible (just like Activity#onConfigurationChanged).
            // Register it in #onCreate instead of #onStart so that when the embedded status is
            // changed to non-embedded before #onStart (like screen rotation when this activity is
            // in background), the listener will be triggered right after #onStart.
            // Otherwise, if registered in #onStart, it will not be triggered on registration
            // because the activity is not embedded, which results it shows the stale info.
            mEmbeddedActivityWindowInfoCallbackCallback = new EmbeddedActivityWindowInfoCallback();
            mActivityEmbeddingControllerCallbackAdapter.addEmbeddedActivityWindowInfoListener(
                    this, Runnable::run, mEmbeddedActivityWindowInfoCallbackCallback);
        }
        mRuleController = RuleController.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mActivityEmbeddingControllerCallbackAdapter != null) {
            mActivityEmbeddingControllerCallbackAdapter.removeEmbeddedActivityWindowInfoListener(
                    mEmbeddedActivityWindowInfoCallbackCallback);
            mEmbeddedActivityWindowInfoCallbackCallback = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSplitInfoCallback = new SplitInfoCallback();
        mSplitControllerAdapter.addSplitListener(this, Runnable::run, mSplitInfoCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSplitControllerAdapter.removeSplitListener(mSplitInfoCallback);
        mSplitInfoCallback = null;
    }

    /** Updates the embedding status when receives callback from the extension. */
    private class SplitInfoCallback implements Consumer<List<SplitInfo>> {
        @Override
        public void accept(List<SplitInfo> splitInfoList) {
            runOnUiThread(
                    () -> {
                        if (mActivityEmbeddingControllerCallbackAdapter == null) {
                            // Otherwise, the embedded status will be updated from
                            // EmbeddedActivityWindowInfoCallback.
                            updateEmbeddedStatus(
                                    mActivityEmbeddingController.isActivityEmbedded(
                                            SplitActivityBase.this));
                        }
                        updateCheckboxesFromCurrentConfig();
                    });
        }
    }

    /** Updates the embedding status when receives callback from the extension. */
    private class EmbeddedActivityWindowInfoCallback
            implements Consumer<EmbeddedActivityWindowInfo> {
        @Override
        public void accept(EmbeddedActivityWindowInfo embeddedActivityWindowInfo) {
            runOnUiThread(
                    () -> {
                        updateEmbeddedStatus(embeddedActivityWindowInfo.isEmbedded());
                        updateEmbeddedWindowInfo(embeddedActivityWindowInfo);
                    });
        }
    }

    /** Updates the checkboxes states after the split rules are changed by other activity. */
    void updateCheckboxesFromCurrentConfig() {
        mUpdatingConfigs = true;

        SplitPairRule splitMainConfig = getRuleFor(SplitActivityA.class, null);
        mRecyclerViewBindingData.splitMainCheckBox.setChecked(splitMainConfig != null);

        SplitPlaceholderRule placeholderForBConfig = getPlaceholderRule(SplitActivityB.class);
        mRecyclerViewBindingData.usePlaceholderCheckBox.setChecked(placeholderForBConfig != null);
        mRecyclerViewBindingData.useStickyPlaceholderCheckBox.setEnabled(
                placeholderForBConfig != null);
        mRecyclerViewBindingData.useStickyPlaceholderCheckBox.setChecked(
                placeholderForBConfig != null && placeholderForBConfig.isSticky());

        SplitPairRule bAndCPairConfig = getRuleFor(SplitActivityB.class, SplitActivityC.class);
        mRecyclerViewBindingData.splitBCCheckBox.setChecked(bAndCPairConfig != null);
        mRecyclerViewBindingData.finishBCCheckBox.setEnabled(bAndCPairConfig != null);
        mRecyclerViewBindingData.finishBCCheckBox.setChecked(
                bAndCPairConfig != null
                        && bAndCPairConfig.getFinishPrimaryWithSecondary() == ALWAYS
                        && bAndCPairConfig.getFinishSecondaryWithPrimary() == ALWAYS);

        SplitPairRule fConfig = getRuleFor(null, SplitActivityF.class);
        mRecyclerViewBindingData.splitWithFCheckBox.setChecked(fConfig != null);

        ActivityRule configE = getRuleFor(SplitActivityE.class);
        mRecyclerViewBindingData.fullscreenECheckBox.setChecked(
                configE != null && configE.getAlwaysExpand());
        mUpdatingConfigs = false;
    }

    /** Gets the split rule for the given activity pair. */
    private SplitPairRule getRuleFor(Class<? extends Activity> a, Class<? extends Activity> b) {
        Set<EmbeddingRule> currentRules = mRuleController.getRules();
        for (EmbeddingRule rule : currentRules) {
            if (rule instanceof SplitPairRule && isRuleFor(a, b, (SplitPairRule) rule)) {
                return (SplitPairRule) rule;
            }
        }
        return null;
    }

    /** Gets the placeholder rule for the given activity. */
    private SplitPlaceholderRule getPlaceholderRule(@NonNull Class<? extends Activity> a) {
        Set<EmbeddingRule> currentRules = mRuleController.getRules();
        for (EmbeddingRule rule : currentRules) {
            if (rule instanceof SplitPlaceholderRule) {
                for (ActivityFilter filter : ((SplitPlaceholderRule) rule).getFilters()) {
                    if (filter.getComponentName().getClassName().equals(a.getName())) {
                        return (SplitPlaceholderRule) rule;
                    }
                }
            }
        }
        return null;
    }

    /** Gets the split rule for the given activity. */
    private ActivityRule getRuleFor(Class<? extends Activity> a) {
        Set<EmbeddingRule> currentRules = mRuleController.getRules();
        for (EmbeddingRule rule : currentRules) {
            if (rule instanceof ActivityRule && isRuleFor(a, (ActivityRule) rule)) {
                return (ActivityRule) rule;
            }
        }
        return null;
    }

    /** Whether the given rule is for splitting the given activity pair. */
    private boolean isRuleFor(
            Class<? extends Activity> a, Class<? extends Activity> b, SplitPairRule pairConfig) {
        return isRuleFor(a != null ? a.getName() : "*", b != null ? b.getName() : "*", pairConfig);
    }

    /** Whether the given rule is for splitting the given activity pair. */
    private boolean isRuleFor(
            String primaryActivityName, String secondaryActivityName, SplitPairRule pairConfig) {
        for (SplitPairFilter filter : pairConfig.getFilters()) {
            if (filter.getPrimaryActivityName().getClassName().contains(primaryActivityName)
                    && filter.getSecondaryActivityName()
                            .getClassName()
                            .contains(secondaryActivityName)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the given rule is for splitting the given activity with another. */
    private boolean isRuleFor(@NonNull Class<? extends Activity> a, @NonNull ActivityRule config) {
        for (ActivityFilter filter : config.getFilters()) {
            if (filter.getComponentName().getClassName().equals(a.getName())) {
                return true;
            }
        }
        return false;
    }

    private void setupRecyclerViewItems(
            @NonNull SplitController splitController, int extensionVersion) {
        // Setup activity launch buttons and config options.
        mRecyclerViewBindingData.launchBButton.onClicked =
                () -> {
                    startActivity(new Intent(this, SplitActivityB.class));
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchBCButton.onClicked =
                () -> {
                    Intent bStartIntent = new Intent(this, SplitActivityB.class);
                    bStartIntent.putExtra(EXTRA_LAUNCH_C_TO_SIDE, true);
                    startActivity(bStartIntent);
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchEButton.onClicked =
                () -> {
                    Bundle bundle = null;
                    if (mRecyclerViewBindingData.launchingEInActivityStackCheckBox.isChecked()) {
                        try {
                            final ActivityStack activityStack =
                                    mActivityEmbeddingController.getActivityStack(this);
                            if (activityStack != null) {
                                bundle =
                                        ActivityEmbeddingOptions.setLaunchingActivityStack(
                                                ActivityOptions.makeBasic().toBundle(),
                                                this,
                                                activityStack);
                            } else {
                                Log.w(TAG, "#getActivityStack returns null");
                            }
                        } catch (UnsupportedOperationException ex) {
                            Log.w(TAG, "#setLaunchingActivityStack is not supported", ex);
                        }
                    }
                    startActivity(new Intent(this, SplitActivityE.class), bundle);
                    return Unit.INSTANCE;
                };
        if (extensionVersion < 3) {
            mRecyclerViewBindingData.launchingEInActivityStackCheckBox.setVisible(true);
        }
        mRecyclerViewBindingData.launchFButton.onClicked =
                () -> {
                    startActivity(new Intent(this, SplitActivityF.class));
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchFPendingIntentButton.onClicked =
                () -> {
                    try {
                        PendingIntent.getActivity(
                                        this,
                                        0,
                                        new Intent(this, SplitActivityF.class),
                                        FLAG_IMMUTABLE)
                                .send();
                    } catch (PendingIntent.CanceledException e) {
                        final String errorMsg = e.getMessage();
                        if (errorMsg != null) {
                            Log.e(TAG, errorMsg);
                        }
                    }
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchUid2TrustedButton.onClicked =
                () -> {
                    final Intent intent = new Intent();
                    // Use an explicit package and class name to start an Activity from a different
                    // package/UID.
                    intent.setClassName(
                            "androidx.window.demo2",
                            "androidx.window.demo2.embedding.TrustedEmbeddingActivity");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, R.string.install_samples_2, Toast.LENGTH_LONG).show();
                    }
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchUid2UntrustedButton.onClicked =
                () -> {
                    final Intent intent = new Intent();
                    // Use an explicit package and class name to start an Activity from a different
                    // package/UID.
                    intent.setClassName(
                            "androidx.window.demo2",
                            "androidx.window.demo2.embedding.UntrustedEmbeddingActivity");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, R.string.install_samples_2, Toast.LENGTH_LONG).show();
                    }
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchUid2UntrustedDisplayFeaturesButton.onClicked =
                () -> {
                    final Intent intent = new Intent();
                    // Use an explicit package and class name to start an Activity from a different
                    // package/UID.
                    intent.setClassName(
                            "androidx.window.demo2",
                            "androidx.window.demo.common.DisplayFeaturesActivity");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, R.string.install_samples_2, Toast.LENGTH_LONG).show();
                    }
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchExpandedDialogButton.onClicked =
                () -> {
                    startActivity(new Intent(this, ExpandedDialogActivity.class));
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchDialogActivityButton.onClicked =
                () -> {
                    startActivity(new Intent(this, DialogActivity.class));
                    return Unit.INSTANCE;
                };
        mRecyclerViewBindingData.launchDialogButton.onClicked =
                () -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Alert dialog demo")
                            .setMessage("This is a dialog demo")
                            .create()
                            .show();
                    return Unit.INSTANCE;
                };

        if (extensionVersion < 5) {
            mRecyclerViewBindingData.pinTopActivityStackButton.setVisible(false);
            mRecyclerViewBindingData.unpinTopActivityStackButton.setVisible(false);
        } else {
            mRecyclerViewBindingData.pinTopActivityStackButton.onClicked =
                    () -> {
                        splitController.pinTopActivityStack(
                                getTaskId(),
                                new SplitPinRule.Builder()
                                        .setSticky(
                                                mRecyclerViewBindingData.stickyPinRuleCheckBox
                                                        .isChecked())
                                        .build());
                        return Unit.INSTANCE;
                    };
            mRecyclerViewBindingData.unpinTopActivityStackButton.onClicked =
                    () -> {
                        splitController.unpinTopActivityStack(getTaskId());
                        return Unit.INSTANCE;
                    };
        }

        if (extensionVersion < 6) {
            mRecyclerViewBindingData.dividerCheckBox.setVisible(false);
            mRecyclerViewBindingData.draggableDividerCheckBox.setVisible(false);
        } else {
            setCheckedChanged(mRecyclerViewBindingData.dividerCheckBox);
            setCheckedChanged(mRecyclerViewBindingData.draggableDividerCheckBox);
        }

        // Listen for split configuration checkboxes to update the rules before launching
        // activities.
        setCheckedChanged(
                mRecyclerViewBindingData.splitBCCheckBox,
                (isChecked) -> {
                    if (isChecked) {
                        mRecyclerViewBindingData.finishBCCheckBox.setEnabled(true);
                    } else {
                        mRecyclerViewBindingData.finishBCCheckBox.setEnabled(false);
                        mRecyclerViewBindingData.finishBCCheckBox.setChecked(false);
                    }
                });
        setCheckedChanged(
                mRecyclerViewBindingData.usePlaceholderCheckBox,
                (isChecked) -> {
                    if (isChecked) {
                        mRecyclerViewBindingData.useStickyPlaceholderCheckBox.setEnabled(true);
                    } else {
                        mRecyclerViewBindingData.useStickyPlaceholderCheckBox.setEnabled(false);
                        mRecyclerViewBindingData.useStickyPlaceholderCheckBox.setChecked(false);
                    }
                });

        setCheckedChanged(mRecyclerViewBindingData.splitMainCheckBox);
        setCheckedChanged(mRecyclerViewBindingData.useStickyPlaceholderCheckBox);
        setCheckedChanged(mRecyclerViewBindingData.finishBCCheckBox);
        setCheckedChanged(mRecyclerViewBindingData.fullscreenECheckBox);
        setCheckedChanged(mRecyclerViewBindingData.splitWithFCheckBox);

        if (extensionVersion < 6) {
            mRecyclerViewBindingData.launchOverlayAssociatedActivityButton.setVisible(false);
        } else {
            mRecyclerViewBindingData.launchOverlayAssociatedActivityButton.onClicked =
                    () -> {
                        startActivity(new Intent(this, OverlayAssociatedActivityA.class));
                        return Unit.INSTANCE;
                    };
        }
    }

    private interface CheckedChangedPreviousOperation {
        void perform(Boolean isChecked);
    }

    private void setCheckedChanged(SplitActivityRecyclerViewBindingData.Item checkBox) {
        setCheckedChanged(checkBox, null);
    }
    private void setCheckedChanged(
            SplitActivityRecyclerViewBindingData.Item checkBox,
            @Nullable CheckedChangedPreviousOperation previousOp) {
        checkBox.onCheckedChange =
                (Boolean isChecked) -> {
                    if (previousOp != null) {
                        previousOp.perform(isChecked);
                    }
                    updateRulesFromCheckboxes();
                    return Unit.INSTANCE;
                };
    }

    /** Updates the split rules based on the current selection on checkboxes. */
    private void updateRulesFromCheckboxes() {
        if (mUpdatingConfigs) {
            return;
        }
        mRuleController.clearRules();

        final DividerAttributes dividerAttributes;
        if (mRecyclerViewBindingData.dividerCheckBox.isChecked()) {
            if (mRecyclerViewBindingData.draggableDividerCheckBox.isChecked()) {
                dividerAttributes =
                        new DraggableDividerAttributes.Builder()
                                .setWidthDp(1)
                                .build();
            } else {
                dividerAttributes = new FixedDividerAttributes.Builder().setWidthDp(1).build();
            }
        } else {
            dividerAttributes = DividerAttributes.NO_DIVIDER;
        }

        final SplitAttributes defaultSplitAttributes =
                new SplitAttributes.Builder()
                        .setSplitType(SplitAttributes.SplitType.ratio(SPLIT_RATIO))
                        .setDividerAttributes(dividerAttributes)
                        .build();

        if (mRecyclerViewBindingData.splitMainCheckBox.isChecked()) {
            // Split main with any activity.
            final Set<SplitPairFilter> pairFilters = new HashSet<>();
            pairFilters.add(
                    new SplitPairFilter(
                            componentName(SplitActivityA.class),
                            new ComponentName("*", "*"),
                            null));
            final SplitPairRule rule =
                    new SplitPairRule.Builder(pairFilters)
                            .setMinWidthDp(MIN_SPLIT_WIDTH_DP)
                            .setMinHeightDp(0)
                            .setMinSmallestWidthDp(0)
                            .setFinishPrimaryWithSecondary(NEVER)
                            .setFinishSecondaryWithPrimary(NEVER)
                            .setClearTop(true)
                            .setDefaultSplitAttributes(defaultSplitAttributes)
                            .build();
            mRuleController.addRule(rule);
        }

        mRecyclerViewBindingData.draggableDividerCheckBox.setEnabled(
                mRecyclerViewBindingData.dividerCheckBox.isChecked());

        if (mRecyclerViewBindingData.usePlaceholderCheckBox.isChecked()) {
            // Split B with placeholder.
            final Set<ActivityFilter> activityFilters = new HashSet<>();
            activityFilters.add(new ActivityFilter(componentName(SplitActivityB.class), null));
            final Intent intent = new Intent();
            intent.setComponent(componentName(SplitActivityPlaceholder.class));
            final SplitPlaceholderRule rule =
                    new SplitPlaceholderRule.Builder(activityFilters, intent)
                            .setMinWidthDp(MIN_SPLIT_WIDTH_DP)
                            .setMinHeightDp(0)
                            .setMinSmallestWidthDp(0)
                            .setSticky(
                                    mRecyclerViewBindingData.useStickyPlaceholderCheckBox
                                            .isChecked())
                            .setFinishPrimaryWithPlaceholder(ADJACENT)
                            .setDefaultSplitAttributes(defaultSplitAttributes)
                            .build();
            mRuleController.addRule(rule);
        }

        if (mRecyclerViewBindingData.splitBCCheckBox.isChecked()) {
            // Split B with C.
            final Set<SplitPairFilter> pairFilters = new HashSet<>();
            pairFilters.add(
                    new SplitPairFilter(
                            componentName(SplitActivityB.class),
                            componentName(SplitActivityC.class),
                            null));
            final SplitPairRule rule =
                    new SplitPairRule.Builder(pairFilters)
                            .setMinWidthDp(MIN_SPLIT_WIDTH_DP)
                            .setMinHeightDp(0)
                            .setMinSmallestWidthDp(0)
                            .setFinishPrimaryWithSecondary(
                                    mRecyclerViewBindingData.finishBCCheckBox.isChecked()
                                            ? ALWAYS
                                            : NEVER)
                            .setFinishSecondaryWithPrimary(
                                    mRecyclerViewBindingData.finishBCCheckBox.isChecked()
                                            ? ALWAYS
                                            : NEVER)
                            .setClearTop(true)
                            .setDefaultSplitAttributes(defaultSplitAttributes)
                            .build();
            mRuleController.addRule(rule);
        }

        if (mRecyclerViewBindingData.splitWithFCheckBox.isChecked()) {
            // Split any activity with F.
            final Set<SplitPairFilter> pairFilters = new HashSet<>();
            pairFilters.add(
                    new SplitPairFilter(
                            new ComponentName("*", "*"),
                            componentName(SplitActivityF.class),
                            null));
            final SplitPairRule rule =
                    new SplitPairRule.Builder(pairFilters)
                            .setMinWidthDp(MIN_SPLIT_WIDTH_DP)
                            .setMinHeightDp(0)
                            .setMinSmallestWidthDp(0)
                            .setFinishPrimaryWithSecondary(NEVER)
                            .setFinishSecondaryWithPrimary(NEVER)
                            .setClearTop(true)
                            .setDefaultSplitAttributes(defaultSplitAttributes)
                            .build();
            mRuleController.addRule(rule);
        }

        if (mRecyclerViewBindingData.fullscreenECheckBox.isChecked()) {
            // Launch E in fullscreen.
            final Set<ActivityFilter> activityFilters = new HashSet<>();
            activityFilters.add(new ActivityFilter(componentName(SplitActivityE.class), null));
            final ActivityRule activityRule =
                    new ActivityRule.Builder(activityFilters).setAlwaysExpand(true).build();
            mRuleController.addRule(activityRule);
        }

        // Always expand the dialog activity.
        final Set<ActivityFilter> dialogActivityFilters = new HashSet<>();
        dialogActivityFilters.add(
                new ActivityFilter(componentName(ExpandedDialogActivity.class), null));
        mRuleController.addRule(
                new ActivityRule.Builder(dialogActivityFilters).setAlwaysExpand(true).build());
    }

    ComponentName componentName(Class<? extends Activity> activityClass) {
        return new ComponentName(
                getPackageName(), activityClass != null ? activityClass.getName() : "*");
    }

    ComponentName componentName(String className) {
        return new ComponentName(getPackageName(), className);
    }

    /** Updates the status label that says when an activity is embedded. */
    private void updateEmbeddedStatus(boolean isEmbedded) {
        mRecyclerViewBindingData.embeddedStatusTextView.setVisible(isEmbedded);
    }

    private void updateEmbeddedWindowInfo(@NonNull EmbeddedActivityWindowInfo info) {
        Log.d(TAG, "EmbeddedActivityWindowInfo changed for r=" + this + "\ninfo=" + info);
        if (!info.isEmbedded()) {
            mRecyclerViewBindingData.embeddedBoundsTextView.setVisible(false);
            return;
        }
        mRecyclerViewBindingData.embeddedBoundsTextView.setVisible(true);
        mRecyclerViewBindingData.embeddedBoundsTextView.setText(
                "Embedded bounds=" + info.getBoundsInParentHost());
    }
}
