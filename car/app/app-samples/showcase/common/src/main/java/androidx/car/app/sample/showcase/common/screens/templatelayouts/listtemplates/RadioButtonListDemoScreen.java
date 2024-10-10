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

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.SectionedItemList;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** A screen demonstrating selectable lists. */
public final class RadioButtonListDemoScreen extends Screen {

    public RadioButtonListDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    private boolean mIsEnabled = true;

    @Override
    public @NonNull Template onGetTemplate() {
        ListTemplate.Builder templateBuilder = new ListTemplate.Builder();
        ItemList radioList =
                new ItemList.Builder()

                        .addItem(buildRowForTemplate(R.string.option_row_radio_title,
                                R.string.additional_text))

                        .addItem(buildRowForTemplate(R.string.option_row_radio_icon_title,
                                R.string.additional_text,
                                buildImageWithResource(R.drawable
                                        .ic_fastfood_white_48dp), Row.IMAGE_TYPE_ICON))

                        .addItem(buildRowForTemplate(
                                R.string.option_row_radio_icon_colored_text_title,
                                R.string.additional_text,
                                buildImageWithResource(R.drawable
                                        .test_image_square), Row.IMAGE_TYPE_LARGE))

                        .setOnSelectedListener(this::onSelected)
                        .build();
        templateBuilder.addSectionedList(
                SectionedItemList.create(radioList,
                        getCarContext().getString(R.string.sample_additional_list)));

        return templateBuilder
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.radio_button_list_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .addEndHeaderAction(new Action.Builder()
                                .setTitle(mIsEnabled
                                        ? getCarContext().getString(
                                        R.string.disable_all_rows)
                                        : getCarContext().getString(
                                                R.string.enable_all_rows))
                                .setOnClickListener(
                                        () -> {
                                            mIsEnabled = !mIsEnabled;
                                            invalidate();
                                        })
                                .build())
                        .build())
                .build();
    }

    private CarIcon buildImageWithResource(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        imageId))
                .build();
    }

    private Row buildRowForTemplate(int title, int text, CarIcon icon, int imageType) {
        return new Row.Builder()
                .addText(getCarContext().getString(text))
                .setTitle(getCarContext().getString(title))
                .setImage(icon, imageType)
                .setEnabled(mIsEnabled)
                .build();

    }

    private Row buildRowForTemplate(int title, int text) {
        return new Row.Builder()
                .addText(getCarContext().getString(text))
                .setTitle(getCarContext().getString(title))
                .setEnabled(mIsEnabled)
                .build();

    }

    private void onSelected(int index) {
        CarToast.makeText(getCarContext(),
                        getCarContext()
                                .getString(R.string.changes_selection_to_index_toast_msg_prefix)
                                + ":"
                                + " " + index, LENGTH_LONG)
                .show();
    }
}
