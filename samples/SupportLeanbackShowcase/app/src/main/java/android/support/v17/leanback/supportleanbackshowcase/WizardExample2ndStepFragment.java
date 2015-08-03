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

package android.support.v17.leanback.supportleanbackshowcase;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.widget.Toast;

import java.util.List;

/**
 * TODO: JavaDoc
 */
public class WizardExample2ndStepFragment extends WizardExampleBaseStepFragment {

    private static final int ACTION_ID_CONFIRM = 1;
    private static final int ACTION_ID_PAYMENT_METHOD = ACTION_ID_CONFIRM + 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWizardActivity().setStep(2);
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
    public GuidedActionsStylist onCreateActionsStylist() {
        GuidedActionsStylist stylist = new GuidedActionsStylist() {

            @Override
            public void onBindViewHolder(ViewHolder vh, GuidedAction action) {
                super.onBindViewHolder(vh, action);

                if (ACTION_ID_CONFIRM == action.getId()) {
                    Drawable background = getResources().getDrawable(
                            R.drawable.wizard_important_action_item_background, null);
                    vh.view.setBackground(background);
                    vh.getTitleView().setTextColor(Color.parseColor("#666666"));
                    vh.getDescriptionView().setTextColor(Color.parseColor("#666666"));
                } else {
                    vh.view.setBackground(null);
                    vh.getTitleView().setTextColor(getResources().getColor(android.support.v17.leanback.R.color.lb_guidedactions_item_unselected_text_color,
                            getActivity().getTheme()));
                    vh.getDescriptionView().setTextColor(getResources().getColor(android.support.v17.leanback.R.color.lb_guidedactions_item_unselected_text_color,
                            getActivity().getTheme()));
                }
            }
        };
        return stylist;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        boolean rentHighDefinition = getArguments().getBoolean("hd");

        GuidedAction action = new GuidedAction.Builder()
                .id(ACTION_ID_CONFIRM)
                .title(getString(R.string.wizard_example_rent))
                .description(rentHighDefinition ? mMovie.getPriceHd() : mMovie.getPriceSd())
                .build();
        actions.add(action);
        action = new GuidedAction.Builder()
                .id(ACTION_ID_PAYMENT_METHOD)
                .title(getString(R.string.wizard_example_payment_method))
                .description("Visa - 1234 Balance $60.00")
                .build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (ACTION_ID_PAYMENT_METHOD == action.getId()) {
            Toast.makeText(getActivity(),
                    getString(R.string.wizard_example_toast_payment_method_clicked),
                    Toast.LENGTH_SHORT).show();
        } else {
            GuidedStepFragment fragment = new WizardExample3rdStepFragment();
            fragment.setArguments(getArguments());
            add(getFragmentManager(), fragment);
        }
    }
}
