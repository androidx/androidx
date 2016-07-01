/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v17.leanback.supportleanbackshowcase.app.wizard;

import android.app.FragmentManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the second screen of the rental wizard which requires the user to confirm his purchase.
 */
public class WizardExample2ndStepFragment extends WizardExampleBaseStepFragment {

    private static final String ARG_HD = "hd";
    private static final int ACTION_ID_CONFIRM = 1;
    private static final int ACTION_ID_PAYMENT_METHOD = ACTION_ID_CONFIRM + 1;
    private static final int ACTION_ID_NEW_PAYMENT = ACTION_ID_PAYMENT_METHOD + 1;

    protected static ArrayList<String> sCards = new ArrayList();
    protected static int sSelectedCard = -1;

    static {
        sCards.add("Visa-1234");
        sCards.add("Master-4321");
    }


    public static GuidedStepFragment build(boolean hd, WizardExampleBaseStepFragment previousFragment) {
        GuidedStepFragment fragment = new WizardExample2ndStepFragment();
        // Reuse the same arguments this fragment was given.
        Bundle args = previousFragment.getArguments();
        args.putBoolean(ARG_HD, hd);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(mMovie.getTitle(),
                getString(R.string.wizard_example_rental_period),
                mMovie.getBreadcrump(), null);
        return guidance;

    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        boolean rentHighDefinition = getArguments().getBoolean(ARG_HD);

        GuidedAction action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_CONFIRM)
                .title(R.string.wizard_example_rent)
                .description(rentHighDefinition ? mMovie.getPriceHd() : mMovie.getPriceSd())
                .editable(false)
                .build();
        action.setEnabled(false);
        actions.add(action);
        List<GuidedAction> subActions = new ArrayList();
        action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_PAYMENT_METHOD)
                .title(R.string.wizard_example_payment_method)
                .editTitle("")
                .description(R.string.wizard_example_input_credit)
                .subActions(subActions)
                .build();
        actions.add(action);
    }

    @Override
    public void onResume() {
        super.onResume();
        GuidedAction payment = findActionById(ACTION_ID_PAYMENT_METHOD);

        List<GuidedAction> paymentSubActions = payment.getSubActions();
        paymentSubActions.clear();
        for (int i = 0; i < sCards.size(); i++) {
            paymentSubActions.add(new GuidedAction.Builder(getActivity())
                            .title(sCards.get(i))
                            .description("")
                            .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                            .build()
            );
        }
        paymentSubActions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_NEW_PAYMENT)
                .title("Add New Card")
                .description("")
                .editable(false)
                .build()
        );
        if ( sSelectedCard >= 0 && sSelectedCard < sCards.size() ) {
            payment.setDescription(sCards.get(sSelectedCard));
            findActionById(ACTION_ID_CONFIRM).setEnabled(true);
        } else
            findActionById(ACTION_ID_CONFIRM).setEnabled(false);
        notifyActionChanged(findActionPositionById(ACTION_ID_CONFIRM));
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {

        if (action.isChecked()) {
            String payment = action.getTitle().toString();
            if ( (sSelectedCard = sCards.indexOf(payment)) != -1 ) {
                findActionById(ACTION_ID_PAYMENT_METHOD).setDescription(payment);
                notifyActionChanged(findActionPositionById(ACTION_ID_PAYMENT_METHOD));
                findActionById(ACTION_ID_CONFIRM).setEnabled(true);
                notifyActionChanged(findActionPositionById(ACTION_ID_CONFIRM));
            }
            return true;
        } else {
            FragmentManager fm = getFragmentManager();
            GuidedStepFragment fragment = new WizardNewPaymentStepFragment();
            fragment.setArguments(getArguments());
            add(fm, fragment);
            return false;
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (ACTION_ID_CONFIRM == action.getId()) {
            GuidedStepFragment fragment = new WizardExample3rdStepFragment();
            fragment.setArguments(getArguments());
            add(getFragmentManager(), fragment);
        }
    }
}
