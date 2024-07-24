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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.window.WindowSdkExtensions;
import androidx.window.demo.R;
import androidx.window.demo.common.EdgeToEdgeActivity;
import androidx.window.demo.databinding.ActivitySplitActivityLayoutBinding;
import androidx.window.embedding.ActivityEmbeddingController;
import androidx.window.embedding.ActivityEmbeddingOptions;
import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.ActivityRule;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sample showcase of split activity rules. Allows the user to select some split configuration
 * options with checkboxes and launch activities with those options applied.
 */
public class SplitActivityBase extends EdgeToEdgeActivity
        implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "SplitActivityTest";
    static final int MIN_SPLIT_WIDTH_DP = 600;
    static final float SPLIT_RATIO = 0.3f;
    static final String EXTRA_LAUNCH_C_TO_SIDE = "launch_c_to_side";

    /**
     * The {@link SplitController} adapter to use callback shaped APIs to get {@link SplitInfo}
     *  changes
     */
    private SplitControllerCallbackAdapter mSplitControllerAdapter;
    private RuleController mRuleController;
    private SplitInfoCallback mSplitInfoCallback;

    private ActivityEmbeddingController mActivityEmbeddingController;
    private ActivityEmbeddingControllerCallbackAdapter mActivityEmbeddingControllerCallbackAdapter;
    private EmbeddedActivityWindowInfoCallback mEmbeddedActivityWindowInfoCallbackCallback;

    private ActivitySplitActivityLayoutBinding mViewBinding;

    /** In the process of updating checkboxes based on split rule. */
    private boolean mUpdatingConfigs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SplitController splitController = SplitController.getInstance(this);
        final SplitController.SplitSupportStatus splitSupportStatus =
                splitController.getSplitSupportStatus();
        if (splitSupportStatus == SPLIT_UNAVAILABLE) {
            Toast.makeText(this, R.string.toast_split_not_available,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else if (splitSupportStatus == SPLIT_ERROR_PROPERTY_NOT_DECLARED) {
            Toast.makeText(this, R.string.toast_split_not_support,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mViewBinding = ActivitySplitActivityLayoutBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        final int extensionVersion = WindowSdkExtensions.getInstance().getExtensionVersion();
        mActivityEmbeddingController = ActivityEmbeddingController.getInstance(this);

        // Setup activity launch buttons and config options.
        mViewBinding.launchB.setOnClickListener((View v) ->
                startActivity(new Intent(this, SplitActivityB.class)));
        mViewBinding.launchBAndC.setOnClickListener((View v) -> {
            Intent bStartIntent = new Intent(this, SplitActivityB.class);
            bStartIntent.putExtra(EXTRA_LAUNCH_C_TO_SIDE, true);
            startActivity(bStartIntent);
        });
        mViewBinding.launchE.setOnClickListener((View v) -> {
            Bundle bundle = null;
            if (mViewBinding.setLaunchingEInActivityStack.isChecked()) {
                try {
                    bundle = ActivityEmbeddingOptions.setLaunchingActivityStack(
                            ActivityOptions.makeBasic().toBundle(), this,
                            mActivityEmbeddingController.getActivityStack(this));
                } catch (UnsupportedOperationException ex) {
                    Log.w(TAG, "#setLaunchingActivityStack is not supported", ex);
                }
            }
            startActivity(new Intent(this, SplitActivityE.class), bundle);
        });
        if (extensionVersion < 3) {
            mViewBinding.setLaunchingEInActivityStack.setVisibility(View.GONE);
        }
        mViewBinding.launchF.setOnClickListener((View v) ->
                startActivity(new Intent(this, SplitActivityF.class)));
        mViewBinding.launchFPendingIntent.setOnClickListener((View v) -> {
            try {
                PendingIntent.getActivity(this, 0, new Intent(this, SplitActivityF.class),
                        FLAG_IMMUTABLE).send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
            }
        });
        mViewBinding.launchUid2Trusted.setOnClickListener((View v) -> {
            final Intent intent = new Intent();
            // Use an explicit package and class name to start an Activity from a different
            // package/UID.
            intent.setClassName(
                    "androidx.window.demo2",
                    "androidx.window.demo2.embedding.TrustedEmbeddingActivity"
            );
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.install_samples_2, Toast.LENGTH_LONG).show();
            }
        });
        mViewBinding.launchUid2Untrusted.setOnClickListener((View v) -> {
            final Intent intent = new Intent();
            // Use an explicit package and class name to start an Activity from a different
            // package/UID.
            intent.setClassName(
                    "androidx.window.demo2",
                    "androidx.window.demo2.embedding.UntrustedEmbeddingActivity"
            );
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.install_samples_2, Toast.LENGTH_LONG).show();
            }
        });
        mViewBinding.launchUid2UntrustedDisplayFeatures.setOnClickListener((View v) -> {
            final Intent intent = new Intent();
            // Use an explicit package and class name to start an Activity from a different
            // package/UID.
            intent.setClassName(
                    "androidx.window.demo2",
                    "androidx.window.demo.common.DisplayFeaturesActivity"
            );
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.install_samples_2, Toast.LENGTH_LONG).show();
            }
        });
        mViewBinding.launchExpandedDialogButton.setOnClickListener((View v) ->
                startActivity(new Intent(this, ExpandedDialogActivity.class)));
        mViewBinding.launchDialogActivityButton.setOnClickListener((View v) ->
                startActivity(new Intent(this, DialogActivity.class)));
        mViewBinding.launchDialogButton.setOnClickListener((View v) ->
                new AlertDialog.Builder(this)
                        .setTitle("Alert dialog demo")
                        .setMessage("This is a dialog demo").create().show());

        if (extensionVersion < 5) {
            mViewBinding.pinTopActivityStackButton.setVisibility(View.GONE);
            mViewBinding.unpinTopActivityStackButton.setVisibility(View.GONE);
        } else {
            mViewBinding.pinTopActivityStackButton.setOnClickListener((View v) -> {
                        splitController.pinTopActivityStack(getTaskId(),
                                new SplitPinRule.Builder().setSticky(
                                        mViewBinding.stickyPinRule.isChecked()).build());
                    }
            );
            mViewBinding.unpinTopActivityStackButton.setOnClickListener((View v) -> {
                        splitController.unpinTopActivityStack(getTaskId());
                    }
            );
        }
        if (extensionVersion < 6) {
            mViewBinding.dividerCheckBox.setVisibility(View.GONE);
            mViewBinding.draggableDividerCheckBox.setVisibility(View.GONE);
        } else {
            mViewBinding.dividerCheckBox.setOnCheckedChangeListener(this);
            mViewBinding.draggableDividerCheckBox.setOnCheckedChangeListener(this);
        }

        // Listen for split configuration checkboxes to update the rules before launching
        // activities.
        mViewBinding.splitMainCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.usePlaceholderCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.useStickyPlaceholderCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.splitBCCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.finishBCCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.fullscreenECheckBox.setOnCheckedChangeListener(this);
        mViewBinding.splitWithFCheckBox.setOnCheckedChangeListener(this);

        if (extensionVersion < 6) {
            mViewBinding.buttonLaunchOverlayAssociatedActivity.setVisibility(View.GONE);
        } else {
            mViewBinding.buttonLaunchOverlayAssociatedActivity.setOnClickListener((View v) ->
                    startActivity(new Intent(this, OverlayAssociatedActivityA.class)));
        }

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
            runOnUiThread(() -> {
                if (mActivityEmbeddingControllerCallbackAdapter == null) {
                    // Otherwise, the embedded status will be updated from
                    // EmbeddedActivityWindowInfoCallback.
                    updateEmbeddedStatus(mActivityEmbeddingController.isActivityEmbedded(
                            SplitActivityBase.this));
                }
                updateCheckboxesFromCurrentConfig();
            });
        }
    }

    /** Updates the embedding status when receives callback from the extension. */
    private class EmbeddedActivityWindowInfoCallback implements
            Consumer<EmbeddedActivityWindowInfo> {
        @Override
        public void accept(EmbeddedActivityWindowInfo embeddedActivityWindowInfo) {
            runOnUiThread(() -> {
                updateEmbeddedStatus(embeddedActivityWindowInfo.isEmbedded());
                updateEmbeddedWindowInfo(embeddedActivityWindowInfo);
            });
        }
    }

    /** Called on checkbox changed. */
    @Override
    public void onCheckedChanged(@NonNull CompoundButton c, boolean isChecked) {
        if (c.getId() == mViewBinding.splitBCCheckBox.getId()) {
            if (isChecked) {
                mViewBinding.finishBCCheckBox.setEnabled(true);
            } else {
                mViewBinding.finishBCCheckBox.setEnabled(false);
                mViewBinding.finishBCCheckBox.setChecked(false);
            }
        } else if (c.getId() == mViewBinding.usePlaceholderCheckBox.getId()) {
            if (isChecked) {
                mViewBinding.useStickyPlaceholderCheckBox.setEnabled(true);
            } else {
                mViewBinding.useStickyPlaceholderCheckBox.setEnabled(false);
                mViewBinding.useStickyPlaceholderCheckBox.setChecked(false);
            }
        }
        if (!mUpdatingConfigs) {
            updateRulesFromCheckboxes();
        }
    }

    /** Updates the checkboxes states after the split rules are changed by other activity. */
    void updateCheckboxesFromCurrentConfig() {
        mUpdatingConfigs = true;

        SplitPairRule splitMainConfig = getRuleFor(SplitActivityA.class, null);
        mViewBinding.splitMainCheckBox.setChecked(splitMainConfig != null);

        SplitPlaceholderRule placeholderForBConfig = getPlaceholderRule(SplitActivityB.class);
        mViewBinding.usePlaceholderCheckBox.setChecked(placeholderForBConfig != null);
        mViewBinding.useStickyPlaceholderCheckBox.setEnabled(placeholderForBConfig != null);
        mViewBinding.useStickyPlaceholderCheckBox.setChecked(placeholderForBConfig != null
                && placeholderForBConfig.isSticky());

        SplitPairRule bAndCPairConfig = getRuleFor(SplitActivityB.class,
                SplitActivityC.class);
        mViewBinding.splitBCCheckBox.setChecked(bAndCPairConfig != null);
        mViewBinding.finishBCCheckBox.setEnabled(bAndCPairConfig != null);
        mViewBinding.finishBCCheckBox.setChecked(bAndCPairConfig != null
                && bAndCPairConfig.getFinishPrimaryWithSecondary() == ALWAYS
                && bAndCPairConfig.getFinishSecondaryWithPrimary() == ALWAYS);

        SplitPairRule fConfig = getRuleFor(null, SplitActivityF.class);
        mViewBinding.splitWithFCheckBox.setChecked(fConfig != null);

        ActivityRule configE = getRuleFor(SplitActivityE.class);
        mViewBinding.fullscreenECheckBox.setChecked(configE != null && configE.getAlwaysExpand());

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
    SplitPlaceholderRule getPlaceholderRule(@NonNull Class<? extends Activity> a) {
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
    private boolean isRuleFor(Class<? extends Activity> a, Class<? extends Activity> b,
            SplitPairRule pairConfig) {
        return isRuleFor(a != null ? a.getName() : "*", b != null ? b.getName() : "*",
                pairConfig);
    }

    /** Whether the given rule is for splitting the given activity pair. */
    private boolean isRuleFor(String primaryActivityName, String secondaryActivityName,
            SplitPairRule pairConfig) {
        for (SplitPairFilter filter : pairConfig.getFilters()) {
            if (filter.getPrimaryActivityName().getClassName().contains(primaryActivityName)
                    && filter.getSecondaryActivityName().getClassName()
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

    /** Updates the split rules based on the current selection on checkboxes. */
    private void updateRulesFromCheckboxes() {
        mRuleController.clearRules();

        final DividerAttributes dividerAttributes;
        if (mViewBinding.dividerCheckBox.isChecked()) {
            dividerAttributes = mViewBinding.draggableDividerCheckBox.isChecked()
                    ? new DraggableDividerAttributes.Builder().setWidthDp(1).build()
                    : new FixedDividerAttributes.Builder().setWidthDp(1).build();
        } else {
            dividerAttributes = DividerAttributes.NO_DIVIDER;
        }

        final SplitAttributes defaultSplitAttributes = new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(SPLIT_RATIO))
                .setDividerAttributes(dividerAttributes)
                .build();

        if (mViewBinding.splitMainCheckBox.isChecked()) {
            // Split main with any activity.
            final Set<SplitPairFilter> pairFilters = new HashSet<>();
            pairFilters.add(new SplitPairFilter(componentName(SplitActivityA.class),
                    new ComponentName("*", "*"), null));
            final SplitPairRule rule = new SplitPairRule.Builder(pairFilters)
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

        mViewBinding.draggableDividerCheckBox.setEnabled(mViewBinding.dividerCheckBox.isChecked());

        if (mViewBinding.usePlaceholderCheckBox.isChecked()) {
            // Split B with placeholder.
            final Set<ActivityFilter> activityFilters = new HashSet<>();
            activityFilters.add(new ActivityFilter(componentName(SplitActivityB.class), null));
            final Intent intent = new Intent();
            intent.setComponent(componentName(SplitActivityPlaceholder.class));
            final SplitPlaceholderRule rule = new SplitPlaceholderRule.Builder(
                    activityFilters,
                    intent
            )
                    .setMinWidthDp(MIN_SPLIT_WIDTH_DP)
                    .setMinHeightDp(0)
                    .setMinSmallestWidthDp(0)
                    .setSticky(mViewBinding.useStickyPlaceholderCheckBox.isChecked())
                    .setFinishPrimaryWithPlaceholder(ADJACENT)
                    .setDefaultSplitAttributes(defaultSplitAttributes)
                    .build();
            mRuleController.addRule(rule);
        }

        if (mViewBinding.splitBCCheckBox.isChecked()) {
            // Split B with C.
            final Set<SplitPairFilter> pairFilters = new HashSet<>();
            pairFilters.add(new SplitPairFilter(componentName(SplitActivityB.class),
                    componentName(SplitActivityC.class), null));
            final SplitPairRule rule = new SplitPairRule.Builder(pairFilters)
                    .setMinWidthDp(MIN_SPLIT_WIDTH_DP)
                    .setMinHeightDp(0)
                    .setMinSmallestWidthDp(0)
                    .setFinishPrimaryWithSecondary(
                            mViewBinding.finishBCCheckBox.isChecked() ? ALWAYS : NEVER
                    )
                    .setFinishSecondaryWithPrimary(
                            mViewBinding.finishBCCheckBox.isChecked() ? ALWAYS : NEVER
                    )
                    .setClearTop(true)
                    .setDefaultSplitAttributes(defaultSplitAttributes)
                    .build();
            mRuleController.addRule(rule);
        }

        if (mViewBinding.splitWithFCheckBox.isChecked()) {
            // Split any activity with F.
            final Set<SplitPairFilter> pairFilters = new HashSet<>();
            pairFilters.add(new SplitPairFilter(new ComponentName("*", "*"),
                    componentName(SplitActivityF.class), null));
            final SplitPairRule rule = new SplitPairRule.Builder(pairFilters)
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

        if (mViewBinding.fullscreenECheckBox.isChecked()) {
            // Launch E in fullscreen.
            final Set<ActivityFilter> activityFilters = new HashSet<>();
            activityFilters.add(new ActivityFilter(componentName(SplitActivityE.class), null));
            final ActivityRule activityRule = new ActivityRule.Builder(activityFilters)
                    .setAlwaysExpand(true)
                    .build();
            mRuleController.addRule(activityRule);
        }

        // Always expand the dialog activity.
        final Set<ActivityFilter> dialogActivityFilters = new HashSet<>();
        dialogActivityFilters.add(new ActivityFilter(componentName(
                ExpandedDialogActivity.class), null));
        mRuleController.addRule(new ActivityRule.Builder(dialogActivityFilters)
                .setAlwaysExpand(true)
                .build());
    }

    ComponentName componentName(Class<? extends Activity> activityClass) {
        return new ComponentName(getPackageName(),
                activityClass != null ? activityClass.getName() : "*");
    }

    ComponentName componentName(String className) {
        return new ComponentName(getPackageName(), className);
    }

    /** Updates the status label that says when an activity is embedded. */
    private void updateEmbeddedStatus(boolean isEmbedded) {
        mViewBinding.activityEmbeddedStatusTextView.setVisibility(isEmbedded
                ? View.VISIBLE
                : View.GONE);
    }

    private void updateEmbeddedWindowInfo(
            @NonNull EmbeddedActivityWindowInfo info) {
        Log.d(TAG, "EmbeddedActivityWindowInfo changed for r=" + this + "\ninfo=" + info);
        if (!info.isEmbedded()) {
            mViewBinding.activityEmbeddedBoundsTextView.setVisibility(View.GONE);
            return;
        }
        mViewBinding.activityEmbeddedBoundsTextView.setVisibility(View.VISIBLE);
        mViewBinding.activityEmbeddedBoundsTextView.setText(
                "Embedded bounds=" + info.getBoundsInParentHost());
    }
}
