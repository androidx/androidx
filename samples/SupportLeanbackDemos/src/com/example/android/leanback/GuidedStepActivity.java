/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.leanback;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;
import android.support.v17.leanback.widget.GuidedActionsStylist.ViewHolder;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;

import java.util.List;

/**
 * Activity that showcases different aspects of GuidedStepFragments.
 */
public class GuidedStepActivity extends Activity {

    private static final int BACK = 2;

    private static final int FIRST_NAME = 3;
    private static final int LAST_NAME = 4;
    private static final int PASSWORD = 5;
    private static final int PAYMENT = 6;

    private static final int OPTION_CHECK_SET_ID = 10;
    private static final int DEFAULT_OPTION = 0;
    private static final String[] OPTION_NAMES = { "Option A", "Option B", "Option C" };
    private static final String[] OPTION_DESCRIPTIONS = { "Here's one thing you can do",
            "Here's another thing you can do", "Here's one more thing you can do" };

    private static final String TAG = GuidedStepActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guided_step_activity);
        GuidedStepFragment.addAsRoot(this, new FirstStepFragment(), R.id.lb_guidedstep_host);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.v(TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    private static void addEditableAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .editable(true)
                .build());
    }

    private static void addEditableAction(List<GuidedAction> actions, long id, String title,
            String editTitle, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .editTitle(editTitle)
                .description(desc)
                .editable(true)
                .build());
    }

    private static void addEditableAction(List<GuidedAction> actions, long id, String title,
            String editTitle, int editInputType, String desc, String editDesc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .editTitle(editTitle)
                .editInputType(editInputType)
                .description(desc)
                .editDescription(editDesc)
                .editable(true)
                .build());
    }

    private static void addEditableDescriptionAction(List<GuidedAction> actions, long id,
            String title, String desc, String editDescription, int descriptionEditInputType) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .editDescription(editDescription)
                .descriptionEditInputType(descriptionEditInputType)
                .descriptionEditable(true)
                .build());
    }

    private static void addCheckedAction(List<GuidedAction> actions, Context context,
            String title, String desc, int checkSetId) {
        actions.add(new GuidedAction.Builder()
                .title(title)
                .description(desc)
                .checkSetId(checkSetId)
                .build());
    }

    public static class FirstStepFragment extends GuidedStepFragment {
        @Override
        public int onProvideTheme() {
            return R.style.Theme_Example_Leanback_GuidedStep_First;
        }

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_first_title);
            String breadcrumb = getString(R.string.guidedstep_first_breadcrumb);
            String description = getString(R.string.guidedstep_first_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder().constructContinue(getActivity())
                    .description("Let's do it")
                    .build());
            actions.add(new GuidedAction.Builder().constructCancel(getActivity())
                    .description("Never mind")
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();
            if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
                GuidedStepFragment.add(fm, new SecondStepFragment(), R.id.lb_guidedstep_host);
            } else if (action.getId() == GuidedAction.ACTION_ID_CANCEL){
                finishGuidedStepFragments();
            }
        }

    }

    public static class SecondStepFragment extends GuidedStepFragment {

        public GuidedActionsStylist onCreateActionsStylist() {
            return new GuidedActionsStylist() {
                protected void setupImeOptions(GuidedActionsStylist.ViewHolder vh,
                        GuidedAction action) {
                    if (action.getId() == PASSWORD) {
                        vh.getEditableDescriptionView().setImeActionLabel("Confirm!",
                                EditorInfo.IME_ACTION_DONE);
                    } else {
                        super.setupImeOptions(vh, action);
                    }
                }
            };
        }

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_second_title);
            String breadcrumb = getString(R.string.guidedstep_second_breadcrumb);
            String description = getString(R.string.guidedstep_second_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            addEditableAction(actions, FIRST_NAME, "Pat", "Your first name");
            addEditableAction(actions, LAST_NAME, "Smith", "Your last name");
            addEditableAction(actions, PAYMENT, "Payment", "", InputType.TYPE_CLASS_NUMBER,
                    "Input credit card number", "Input credit card number");
            addEditableDescriptionAction(actions, PASSWORD, "Password", "", "",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        @Override
        public void onCreateButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder().constructContinue(getActivity())
                    .description("Continue")
                    .build());
            actions.get(actions.size() - 1).setEnabled(false);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
                FragmentManager fm = getFragmentManager();
                GuidedStepFragment.add(fm, new ThirdStepFragment(), R.id.lb_guidedstep_host);
            }
        }

        @Override
        public long onGuidedActionEditedAndProceed(GuidedAction action) {
            if (action.getId() == PAYMENT) {
                CharSequence editTitle = action.getEditTitle();
                if (TextUtils.isDigitsOnly(editTitle) && editTitle.length() == 16) {
                    editTitle = editTitle.subSequence(editTitle.length() - 4, editTitle.length());
                    action.setDescription("Visa XXXX-XXXX-XXXX-"+editTitle);
                    updateContinue(isPasswordValid());
                    return GuidedAction.ACTION_ID_NEXT;
                } else if (editTitle.length() == 0){
                    action.setDescription("Input credit card number");
                    updateContinue(false);
                    return GuidedAction.ACTION_ID_CURRENT;
                } else {
                    action.setDescription("Error credit card number");
                    updateContinue(false);
                    return GuidedAction.ACTION_ID_CURRENT;
                }
            } else if (action.getId() == PASSWORD) {
                CharSequence password = action.getEditDescription();
                if (password.length() > 0) {
                    if (isPaymentValid()) {
                        updateContinue(true);
                        return GuidedAction.ACTION_ID_NEXT;
                    } else {
                        updateContinue(false);
                        return GuidedAction.ACTION_ID_CURRENT;
                    }
                } else {
                    updateContinue(false);
                    return GuidedAction.ACTION_ID_CURRENT;
                }
            }
            return GuidedAction.ACTION_ID_NEXT;
        }

        boolean isPaymentValid() {
            return findActionById(PAYMENT).getDescription().subSequence(0, 4).toString().equals("Visa");
        }

        boolean isPasswordValid() {
            return findActionById(PASSWORD).getEditDescription().length() > 0;
        }

        void updateContinue(boolean enabled) {
            findButtonActionById(GuidedAction.ACTION_ID_CONTINUE).setEnabled(enabled);
            notifyButtonActionChanged(findButtonActionPositionById(
                    GuidedAction.ACTION_ID_CONTINUE));
        }
    }

    public static class ThirdStepFragment extends GuidedStepFragment {

        private int mSelectedOption = DEFAULT_OPTION;

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_third_title);
            String breadcrumb = getString(R.string.guidedstep_third_breadcrumb);
            String description = getString(R.string.guidedstep_third_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public GuidanceStylist onCreateGuidanceStylist() {
            return new GuidanceStylist() {
                @Override
                public int onProvideLayoutId() {
                    return R.layout.guidedstep_second_guidance;
                }
            };
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            String desc = "The description can be quite long as well.  ";
            desc += "Just be sure to set multilineDescription to true in the GuidedAction.";
            actions.add(new GuidedAction.Builder()
                    .title("Note that Guided Actions can have titles that are quite long.")
                    .description(desc)
                    .multilineDescription(true)
                    .infoOnly(true)
                    .enabled(true)
                    .focusable(false)
                    .build());
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                addCheckedAction(actions, getActivity(), OPTION_NAMES[i],
                        OPTION_DESCRIPTIONS[i], GuidedAction.DEFAULT_CHECK_SET_ID);
                if (i == DEFAULT_OPTION) {
                    actions.get(actions.size() -1).setChecked(true);
                }
            }
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                addCheckedAction(actions, getActivity(), OPTION_NAMES[i],
                        OPTION_DESCRIPTIONS[i], GuidedAction.CHECKBOX_CHECK_SET_ID);
            }
        }

        @Override
        public void onCreateButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder().constructContinue(getActivity())
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
                FragmentManager fm = getFragmentManager();
                FourthStepFragment f = new FourthStepFragment();
                Bundle arguments = new Bundle();
                arguments.putInt(FourthStepFragment.EXTRA_OPTION, mSelectedOption);
                f.setArguments(arguments);
                GuidedStepFragment.add(fm, f, R.id.lb_guidedstep_host);
            } else if (action.getCheckSetId() == GuidedAction.DEFAULT_CHECK_SET_ID) {
                mSelectedOption = getSelectedActionPosition()-1;
            }
        }

    }

    public static class FourthStepFragment extends GuidedStepFragment {
        public static final String EXTRA_OPTION = "extra_option";

        public FourthStepFragment() {
        }

        public int getOption() {
            Bundle b = getArguments();
            if (b == null) return 0;
            return b.getInt(EXTRA_OPTION, 0);
        }

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_fourth_title);
            String breadcrumb = getString(R.string.guidedstep_fourth_breadcrumb);
            String description = "You chose: " + OPTION_NAMES[getOption()];
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder().constructFinish(getActivity())
                    .description("All Done...")
                    .build());
            addAction(actions, BACK, "Start Over", "Let's try this again...");
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_FINISH) {
                finishGuidedStepFragments();
            } else if (action.getId() == BACK) {
                // pop 4, 3, 2
                popBackStackToGuidedStepFragment(SecondStepFragment.class,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }

    }

}
