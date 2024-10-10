/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.model.CarColor.GREEN;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** A screen demonstrating selectable lists. */
public final class ToggleButtonListDemoScreen extends Screen {

    public ToggleButtonListDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    private boolean mFirstToggleState;
    private boolean mSecondToggleState;
    private boolean mSecondToggleEnabled = true;
    private int mImageType = Row.IMAGE_TYPE_ICON;
    private boolean mSetTintToVector;

    @Override
    public @NonNull Template onGetTemplate() {
        Toggle mToggleForVector = new Toggle.Builder((checked) -> {
            mSetTintToVector = !mSetTintToVector;
            invalidate();
        }).setChecked(mSetTintToVector).build();

        Toggle mFirstToggle = new Toggle.Builder((checked) -> {
            mSecondToggleEnabled = checked;
            if (checked) {
                CarToast.makeText(getCarContext(), R.string.toggle_test_enabled,
                        LENGTH_LONG).show();
            } else {
                CarToast.makeText(getCarContext(), R.string.toggle_test_disabled,
                        LENGTH_LONG).show();
            }
            mFirstToggleState = !mFirstToggleState;
            invalidate();
        }).setChecked(mFirstToggleState).build();

        Toggle mSecondToggle = new Toggle.Builder((checked) -> {
            mSecondToggleState = !mSecondToggleState;
            invalidate();
        }).setChecked(mSecondToggleState).setEnabled(mSecondToggleEnabled).build();

        ItemList.Builder builder = new ItemList.Builder();
        builder.addItem(buildRowForTemplate(R.string.toggle_test_first_toggle_title,
                R.string.toggle_test_first_toggle_text, mFirstToggle));

        builder.addItem(buildRowForTemplate(R.string.toggle_test_second_toggle_title,
                R.string.toggle_test_second_toggle_text, mSecondToggle));

        builder.addItem(buildRowForTemplate(titleForVectorDrawable(),
                R.string.vector_toggle_details, mToggleForVector, buildCarIconForVectorDrawable(),
                null, Row.IMAGE_TYPE_ICON));

        builder.addItem(buildRowForTemplate(R.string.image_test_title,
                R.string.image_test_text, null, buildCarIconForImageTest(),
                buildOnClickListenerForImageTest(), mImageType));

        return new ListTemplate.Builder()
                .setSingleList(builder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.toggle_button_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .addEndHeaderAction(new Action.Builder()
                                .setTitle(getCarContext().getString(
                                        R.string.home_caps_action_title))
                                .setOnClickListener(
                                        () -> getScreenManager().popToRoot())
                                .build())
                        .build())
                .build();
    }

    private CarIcon buildCarIconForVectorDrawable() {
        CarIcon.Builder carIconBuilder = new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        R.drawable.ic_fastfood_white_48dp));

        if (mSetTintToVector) {
            carIconBuilder.setTint(GREEN);
        }
        return carIconBuilder.build();
    }

    private CarIcon buildCarIconForImageTest() {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        R.drawable.ic_fastfood_yellow_48dp))
                .build();
    }

    private int titleForVectorDrawable() {
        if (mSetTintToVector) {
            return R.string.vector_with_tint_title;
        } else {
            return R.string.vector_no_tint_title;
        }
    }

    private OnClickListener buildOnClickListenerForImageTest() {
        return () -> {
            mImageType =
                    mImageType == Row.IMAGE_TYPE_ICON
                            ? Row.IMAGE_TYPE_LARGE
                            : Row.IMAGE_TYPE_ICON;
            invalidate();
        };
    }

    private Row buildRowForTemplate(int title, int text, Toggle toggle) {
        return new Row.Builder()
                .addText(getCarContext().getString(text))
                .setTitle(getCarContext().getString(title))
                .setToggle(toggle)
                .build();

    }

    private Row buildRowForTemplate(int title, int text, Toggle toggle, CarIcon icon,
            OnClickListener onClickListener, int imageType) {

        Row.Builder rowBuilder = new Row.Builder();
        rowBuilder
                .addText(getCarContext().getString(text))
                .setTitle(getCarContext().getString(title))
                .setImage(icon, imageType);

        if (toggle != null) rowBuilder.setToggle(toggle);
        if (onClickListener != null) rowBuilder.setOnClickListener(onClickListener);

        return rowBuilder.build();
    }
}
