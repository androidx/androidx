package android.support.v17.leanback.supportleanbackshowcase.app.wizard;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedDatePickerAction;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.List;

/**
 * Created by keyvana on 2/8/16.
 */
public class WizardNewPaymentStepFragment extends WizardExampleBaseStepFragment {

    private static final int ACTION_ID_CARD_NUMBER = 1;
    private static final int ACTION_ID_PAYMENT_EXP = ACTION_ID_CARD_NUMBER + 1;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.wizard_example_new_payment_guidance_title);
        String description = getString(R.string.wizard_example_new_payment_guidance_description);
        String breadcrumb = mMovie.getBreadcrump();

        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);
        return guidance;
    }

    @Override
    public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
        actions.add(new GuidedAction.Builder(getActivity())
                        .id(ACTION_ID_CARD_NUMBER)
                        .title(R.string.wizard_example_input_card)
                        .editTitle("")
                        .description(R.string.wizard_example_input_card)
                        .editDescription("Card number")
                        .editable(true)
                        .build()
        );

        actions.add(new GuidedDatePickerAction.Builder(getActivity())
                        .id(ACTION_ID_PAYMENT_EXP)
                        .title(R.string.wizard_example_expiration_date)
                        .datePickerFormat("MY")
                        .build()
        );
    }

    @Override
    public void onCreateButtonActions(@NonNull List<GuidedAction> actions,
                                      Bundle savedInstanceState) {
        actions.add(new GuidedAction.Builder(getActivity())
                        .clickAction(GuidedAction.ACTION_ID_OK)
                        .build()
        );
        actions.get(actions.size() - 1).setEnabled(false);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == GuidedAction.ACTION_ID_OK) {
            CharSequence cardNumber = findActionById(ACTION_ID_CARD_NUMBER).getDescription();
            WizardExample2ndStepFragment.sSelectedCard = WizardExample2ndStepFragment.sCards.size();
            WizardExample2ndStepFragment.sCards.add(cardNumber.toString());
            popBackStackToGuidedStepFragment(WizardNewPaymentStepFragment.class,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {

        boolean cardNumberCheck = false;
        boolean expDateCheck = false;

        if (action.getId() == ACTION_ID_CARD_NUMBER) {
            CharSequence cardNumber = action.getEditTitle();
            cardNumberCheck = isCardNumberValid(cardNumber);
            expDateCheck = isExpDateValid(findActionById(ACTION_ID_PAYMENT_EXP));
            updateOkButton(cardNumberCheck && expDateCheck);

            if (cardNumberCheck) {
                String last4Digits = cardNumber.subSequence(cardNumber.length() - 4,
                        cardNumber.length()).toString();

                if ( (Integer.parseInt(last4Digits) & 1) == 0 )
                    action.setDescription(getString(R.string.wizard_example_visa,
                            last4Digits));
                else
                    action.setDescription(getString(R.string.wizard_example_master,
                            last4Digits));

                return GuidedAction.ACTION_ID_NEXT;
            } else if (cardNumber.length() == 0) {
                action.setDescription(getString(R.string.wizard_example_input_card));
                return GuidedAction.ACTION_ID_CURRENT;
            } else {
                action.setDescription(getString(R.string.wizard_example_input_credit_wrong));
                return GuidedAction.ACTION_ID_CURRENT;
            }

        } else if (action.getId() == ACTION_ID_PAYMENT_EXP) {
            expDateCheck = isExpDateValid(action);
            cardNumberCheck = isCardNumberValid(findActionById(ACTION_ID_CARD_NUMBER)
                    .getEditTitle());
            updateOkButton(cardNumberCheck && expDateCheck);
            if (expDateCheck) {
                return GuidedAction.ACTION_ID_NEXT;
            }
        }
        return GuidedAction.ACTION_ID_CURRENT;
    }

    private void updateOkButton(boolean enabled) {
        findButtonActionById(GuidedAction.ACTION_ID_OK).setEnabled(enabled);
        notifyButtonActionChanged(findButtonActionPositionById(GuidedAction.ACTION_ID_OK));
    }

    private static boolean isCardNumberValid(CharSequence number) {
        return (TextUtils.isDigitsOnly(number) && number.length() == 16);
    }

    private static boolean isExpDateValid(GuidedAction dateAction) {
        long date = ((GuidedDatePickerAction) dateAction).getDate();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return Calendar.getInstance().before(c);
    }
}
