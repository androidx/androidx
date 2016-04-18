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
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.support.v17.leanback.widget.GuidedActionsStylist.ViewHolder;
import android.support.v17.leanback.widget.GuidedDatePickerAction;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.Calendar;
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
    private static final int NEW_PAYMENT = 7;
    private static final int PAYMENT_EXPIRE = 8;

    private static final long RADIO_ID_BASE = 0;
    private static final long CHECKBOX_ID_BASE = 100;

    private static final long DEFAULT_OPTION = RADIO_ID_BASE;

    private static final String[] OPTION_NAMES = { "Option A", "Option B", "Option C" };
    private static final String[] OPTION_DESCRIPTIONS = { "Here's one thing you can do",
            "Here's another thing you can do", "Here's one more thing you can do" };

    private static final String TAG = GuidedStepActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guided_step_activity);
        if (savedInstanceState == null) {
            GuidedStepFragment.addAsRoot(this, new FirstStepFragment(), R.id.lb_guidedstep_host);
        }
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

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc,
            List<GuidedAction> subActions) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .subActions(subActions)
                .build());
    }

    private static void addEditableAction(Context context, List<GuidedAction> actions,
            long id, String title, String desc) {
        actions.add(new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .editable(true)
                .icon(R.drawable.lb_ic_search_mic)
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

    private static void addDatePickerAction(List<GuidedAction> actions, long id, String title) {
        actions.add(new GuidedDatePickerAction.Builder(null)
                .id(id)
                .title(title)
                .datePickerFormat("MY")
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

    private static void addCheckedAction(List<GuidedAction> actions, long id, Context context,
            String title, String desc, int checkSetId) {
        actions.add(new GuidedAction.Builder()
                .id(id)
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
            Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            Context context = getActivity();
            actions.add(new GuidedAction.Builder(context)
                    .clickAction(GuidedAction.ACTION_ID_CONTINUE)
                    .description("Let's do it")
                    .build());
            actions.add(new GuidedAction.Builder(context)
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
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

    static ArrayList<String> sCards = new ArrayList<String>();
    static int sSelectedCard = -1;
    static {
        sCards.add("Visa-1234");
        sCards.add("Master-4321");
    }

    public static class NewPaymentStepFragment extends GuidedStepFragment {

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_newpayment_title);
            String breadcrumb = getString(R.string.guidedstep_newpayment_breadcrumb);
            String description = getString(R.string.guidedstep_newpayment_description);
            Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            addEditableAction(actions, NEW_PAYMENT, "Input credit card number", "",
                    InputType.TYPE_CLASS_NUMBER,
                    "Input credit card number", "Input credit card number");
            addDatePickerAction(actions, PAYMENT_EXPIRE, "Exp:");
        }

        @Override
        public void onCreateButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            Context context = getActivity();
            actions.add(new GuidedAction.Builder(context).clickAction(GuidedAction.ACTION_ID_OK)
                    .build());
            actions.get(actions.size() - 1).setEnabled(false);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_OK) {
                CharSequence desc = findActionById(NEW_PAYMENT).getDescription();
                String cardNumber = desc.subSequence(desc.length() - 4, desc.length()).toString();
                String card;
                if ((Integer.parseInt(cardNumber) & 1) == 0) {
                    card = "Visa "+cardNumber;
                } else {
                    card = "Master "+cardNumber;
                }
                sSelectedCard = sCards.size();
                sCards.add(card);
                popBackStackToGuidedStepFragment(NewPaymentStepFragment.class,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }

        @Override
        public long onGuidedActionEditedAndProceed(GuidedAction action) {
            if (action.getId() == NEW_PAYMENT) {
                CharSequence editTitle = action.getEditTitle();
                if (isCardNumberValid(editTitle)) {
                    editTitle = editTitle.subSequence(editTitle.length() - 4, editTitle.length());
                    action.setDescription("Visa XXXX-XXXX-XXXX-" + editTitle);
                    updateOkButton(isExpDateValid(findActionById(PAYMENT_EXPIRE)));
                    return GuidedAction.ACTION_ID_NEXT;
                } else if (editTitle.length() == 0) {
                    action.setDescription("Input credit card number");
                    updateOkButton(false);
                    return GuidedAction.ACTION_ID_CURRENT;
                } else {
                    action.setDescription("Error credit card number");
                    updateOkButton(false);
                    return GuidedAction.ACTION_ID_CURRENT;
                }
            } else if (action.getId() == PAYMENT_EXPIRE) {
                updateOkButton(isExpDateValid(action) &&
                        isCardNumberValid(findActionById(NEW_PAYMENT).getEditTitle()));
            }
            return GuidedAction.ACTION_ID_NEXT;
        }

        boolean isCardNumberValid(CharSequence number) {
            return TextUtils.isDigitsOnly(number) && number.length() == 16;
        }

        boolean isExpDateValid(GuidedAction action) {
            long date = ((GuidedDatePickerAction) action).getDate();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(date);
            return Calendar.getInstance().before(c);
        }

        void updateOkButton(boolean enabled) {
            findButtonActionById(GuidedAction.ACTION_ID_OK).setEnabled(enabled);
            notifyButtonActionChanged(findButtonActionPositionById(GuidedAction.ACTION_ID_OK));
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
            Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            addEditableAction(getContext(), actions, FIRST_NAME, "Pat", "Your first name");
            addEditableAction(getContext(), actions, LAST_NAME, "Smith", "Your last name");
            List<GuidedAction> subActions = new ArrayList<GuidedAction>();
            addAction(actions, PAYMENT, "Select Payment", "", subActions);
            addEditableDescriptionAction(actions, PASSWORD, "Password", "", "",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        @Override
        public void onCreateButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder(getActivity())
                    .clickAction(GuidedAction.ACTION_ID_CONTINUE)
                    .description("Continue")
                    .build());
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
            if (action.getId() == PASSWORD) {
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

        @Override
        public boolean onSubGuidedActionClicked(GuidedAction action) {
            if (action.isChecked()) {
                String payment = action.getTitle().toString();
                for (int i = 0; i < sCards.size(); i++) {
                    if (payment.equals(sCards.get(i))) {
                        sSelectedCard = i;
                        findActionById(PAYMENT).setDescription(payment);
                        notifyActionChanged(findActionPositionById(PAYMENT));
                        updateContinue(isPasswordValid());
                        break;
                    }
                }
                return true;
            } else {
                FragmentManager fm = getFragmentManager();
                GuidedStepFragment.add(fm, new NewPaymentStepFragment(), R.id.lb_guidedstep_host);
                return false;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            // when resumed, update sub actions list and selected index from data model.
            GuidedAction payments = findActionById(PAYMENT);
            payments.getSubActions().clear();
            for (int i = 0; i < sCards.size(); i++) {
                addCheckedAction(payments.getSubActions(), -1, getActivity(), sCards.get(i), "",
                        GuidedAction.DEFAULT_CHECK_SET_ID);
                if (i == sSelectedCard) {
                    payments.getSubActions().get(i).setChecked(true);
                }
            }
            addAction(payments.getSubActions(), NEW_PAYMENT, "Add New Card", "");
            if (sSelectedCard != -1) {
                payments.setDescription(sCards.get(sSelectedCard));
            }
            notifyActionChanged(findActionPositionById(PAYMENT));
            updateContinue(isPasswordValid() && isPaymentValid());
        }

        boolean isPaymentValid() {
            CharSequence paymentType = findActionById(PAYMENT).getDescription();
            return (paymentType.length() >= 4 &&
                    paymentType.subSequence(0, 4).toString().equals("Visa")) ||
                    (paymentType.length() >= 6 &&
                    paymentType.subSequence(0, 6).toString().equals("Master"));
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

        private long mSelectedOption = DEFAULT_OPTION;

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_third_title);
            String breadcrumb = getString(R.string.guidedstep_third_breadcrumb);
            String description = getString(R.string.guidedstep_third_description);
            Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_main_icon);
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
            String desc = "The description can be quite long as well.  " +
                    "Just be sure to set multilineDescription to true in the GuidedAction.";
            actions.add(new GuidedAction.Builder()
                    .title("Note that Guided Actions can have titles that are quite long.")
                    .description(desc)
                    .multilineDescription(true)
                    .infoOnly(true)
                    .enabled(true)
                    .focusable(false)
                    .build());
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                addCheckedAction(actions, RADIO_ID_BASE + i, getActivity(), OPTION_NAMES[i],
                        OPTION_DESCRIPTIONS[i], GuidedAction.DEFAULT_CHECK_SET_ID);
                if (i == DEFAULT_OPTION) {
                    actions.get(actions.size() -1).setChecked(true);
                }
            }
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                addCheckedAction(actions, CHECKBOX_ID_BASE + i, getActivity(), OPTION_NAMES[i],
                        OPTION_DESCRIPTIONS[i], GuidedAction.CHECKBOX_CHECK_SET_ID);
            }
        }

        @Override
        public void onCreateButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder(getActivity())
                    .clickAction(GuidedAction.ACTION_ID_CONTINUE)
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
                FragmentManager fm = getFragmentManager();
                FourthStepFragment f = new FourthStepFragment();
                Bundle arguments = new Bundle();
                arguments.putLong(FourthStepFragment.EXTRA_OPTION, mSelectedOption);
                f.setArguments(arguments);
                GuidedStepFragment.add(fm, f, R.id.lb_guidedstep_host);
            } else if (action.getCheckSetId() == GuidedAction.DEFAULT_CHECK_SET_ID) {
                mSelectedOption = action.getId();
            }
        }

    }

    public static class FourthStepFragment extends GuidedStepFragment {
        public static final String EXTRA_OPTION = "extra_option";

        public FourthStepFragment() {
        }

        public long getOption() {
            Bundle b = getArguments();
            if (b == null) return 0;
            return b.getLong(EXTRA_OPTION, 0);
        }

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_fourth_title);
            String breadcrumb = getString(R.string.guidedstep_fourth_breadcrumb);
            String description = "You chose: " + OPTION_NAMES[(int) getOption()];
            Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder(getActivity())
                    .clickAction(GuidedAction.ACTION_ID_FINISH)
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
