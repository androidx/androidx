/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.design.widget;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.design.test.R;
import android.support.test.filters.MediumTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

@MediumTest
public class TextInputLayoutPseudoLocaleTest extends
        BaseInstrumentationTestCase<TextInputLayoutActivity> {

    private static final String ORIGINAL_LANGUAGE = Locale.getDefault().getLanguage();
    private static final String ORIGINAL_COUNTRY = Locale.getDefault().getLanguage();

    @BeforeClass
    public static void setup() {
        // Change language to pseudo locale.
        setLocale("ar", "XB", getContext());
    }

    public TextInputLayoutPseudoLocaleTest() {
        super(TextInputLayoutActivity.class);
    }

    private static void setLocale(String language,  String country,  Context context) {
        context = context.getApplicationContext();
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(new Locale(language, country));
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    @Test
    public void testSimpleEdit() {
        // Type some text
        onView(withId(R.id.textinput_edittext)).perform(typeText("123"));
    }

    @AfterClass
    public static void  cleanup() {
        setLocale(ORIGINAL_LANGUAGE, ORIGINAL_COUNTRY, getContext());
    }
}
