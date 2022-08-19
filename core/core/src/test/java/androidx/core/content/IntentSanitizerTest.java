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

package androidx.core.content;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.content.UriMatcher;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Tests for {@link IntentSanitizer}
 */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class IntentSanitizerTest {
    //private Context mContext;
    private static final ComponentName TEST_COMPONENT = new ComponentName(
            "com.test.intent.sanitizer", "SanitizerService");
    private static final ComponentName TEST_COMPONENT_2 = new ComponentName(
            "com.test.intent.sanitizer", "SanitizerService2");
    private static final String TEST_INTENT_FILTER_ACTION = "testIntentFilterAction";
    private static final String TEST_INTENT_FILTER_DATATYPE = "testIntentFilter/DataType";
    public static final String TEST_PACKAGE_NAME = "com.test.intent.sanitizer";

    private static Intent basicIntent() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        return intent;
    }

    private static IntentSanitizer.Builder basicSanitizerBuilder() {
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(TEST_COMPONENT);
        return builder;
    }

    @Test
    public void sanitizeByThrowing_noException() {
        Intent intent = basicIntent();
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        builder.build().sanitizeByThrowing(intent);
    }

    @Test
    public void sanitizeByThrowing_throwException() {
        Intent intent = basicIntent();
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByThrowing(intent));
    }

    @Test
    public void intentWithFlags_builderNoAllowedFlags_filterOut() {
        Intent intent = basicIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        // custom consumer to test error message.
        StringBuilder result = new StringBuilder();
        intent = builder.build().sanitize(intent, result::append);

        assertThat(intent.getFlags()).isEqualTo(0);
        assertThat(result.toString()).isEqualTo("The intent contains flags that are not allowed: "
                + "0x10400000");
    }

    @Test
    public void intentWithFlags_builderAllowedSomeExtraFlags_filterInSomeFlags() {
        Intent intent = basicIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentSanitizer.Builder builder = basicSanitizerBuilder().allowFlags(
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // custom consumer to test error message.
        StringBuilder result = new StringBuilder();
        intent = builder.build().sanitize(intent, result::append);

        assertThat(intent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        assertThat(result.toString()).isEqualTo("The intent contains flags that are not allowed: "
                + "0x10000000");
    }

    @Test
    public void intentWithFlags_builderAllowedSomeFlags_filterInSomeFlags() {
        Intent intent = basicIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentSanitizer.Builder builder = basicSanitizerBuilder().allowFlags(
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        // custom consumer to test error message.
        StringBuilder result = new StringBuilder();
        intent = builder.build().sanitize(intent, result::append);

        assertThat(intent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        assertThat(result.toString()).isEqualTo("The intent contains flags that are not allowed: "
                + "0x10000000");
    }

    @Test
    public void intentWithFlags_builderAllowedAllFlags_filterIn() {
        Intent intent = basicIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentSanitizer.Builder builder = basicSanitizerBuilder()
                .allowFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                .allowFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getFlags()).isEqualTo(
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void intentWithFlags_builderAllowedMoreFlags_filterIn() {
        Intent intent = basicIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentSanitizer.Builder builder = basicSanitizerBuilder()
                .allowFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                .allowFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .allowFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getFlags()).isEqualTo(
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void intentWithAction_builderNoActionAllowed_filterOut() {
        Intent intent = basicIntent();
        intent.setAction(Intent.ACTION_VIEW);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        Intent newIntent = builder.build().sanitizeByFiltering(intent);

        assertThat(newIntent.getAction()).isNull();
    }

    @Test
    public void intentWithAction_builderAllowedSameAction_filterIn() {
        Intent intent = basicIntent();
        intent.setAction(Intent.ACTION_VIEW);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowAction(Intent.ACTION_VIEW);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void intentWithAction_builderAllowedDifferentAction_filterOut() {
        Intent intent = basicIntent();
        intent.setAction(Intent.ACTION_VIEW);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowAction(Intent.ACTION_SEND);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getAction()).isNull();
    }

    @Test
    public void intentWithAction_builderAllowedMoreActions_filterIn() {
        Intent intent = basicIntent();
        intent.setAction(Intent.ACTION_VIEW);
        IntentSanitizer.Builder builder = basicSanitizerBuilder()
                .allowAction(Intent.ACTION_SEND)
                .allowAction(Intent.ACTION_VIEW);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void intentWithData_builderWithNoDataAllowed_filterOut() {
        Intent intent = basicIntent();
        intent.setData(Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getData()).isNull();
    }

    @Test
    public void intentWithData_builderWithDifferentAuthority_filterOut() {
        Intent intent = basicIntent();
        intent.setData(Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowDataWithAuthority("dog");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getData()).isNull();
    }

    @Test
    public void intentWithData_builderWithSameAuthority_filterIn() {
        Intent intent = basicIntent();
        intent.setData(Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowDataWithAuthority("people");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getData()).isEqualTo(Uri.parse("content://people/1"));
    }

    @Test
    public void intentWithData_builderWithInCorrectUriMatcher_filterOut() {
        Intent intent = basicIntent();
        intent.setData(Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("people", "2", 2);
        builder.allowData(UriMatcherCompat.asPredicate(uriMatcher));

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getData()).isNull();
    }

    @Test
    public void intentWithData_builderWithCorrectUriMatcher_filterIn() {
        Intent intent = basicIntent();
        intent.setData(Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("people", "1", 2);
        builder.allowData(UriMatcherCompat.asPredicate(uriMatcher));

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getData()).isEqualTo(Uri.parse("content://people/1"));
    }

    @Test
    public void intentWithType_builderNoTypeAllowed_filterOut() {
        Intent intent = basicIntent();
        intent.setType("image/jpeg");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getType()).isNull();
    }

    @Test
    public void intentWithType_builderAllowedDifferentType_filterOut() {
        Intent intent = basicIntent();
        intent.setType("image/jpeg");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowType("image/png");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getType()).isNull();
    }

    @Test
    public void intentWithType_builderAllowedSameType_filterIn() {
        Intent intent = basicIntent();
        intent.setType("image/jpeg");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowType("image/jpeg");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getType()).isEqualTo("image/jpeg");
    }

    @Test
    public void intentWithType_builderAllowedMoreTypes_filterIn() {
        Intent intent = basicIntent();
        intent.setType("image/jpeg");
        IntentSanitizer.Builder builder = basicSanitizerBuilder()
                .allowType("text/plain")
                .allowType("image/jpeg");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getType()).isEqualTo("image/jpeg");
    }

    @Test
    public void intentWithCategories_builderAllowsNoCategory_filterOut() {
        Intent intent = basicIntent();
        intent.addCategory(Intent.CATEGORY_APP_BROWSER);
        intent.addCategory(Intent.CATEGORY_APP_CALCULATOR);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getCategories()).isNull();
    }

    @Test
    public void intentWithCategories_builderAllowsOtherCategory_filterOut() {
        Intent intent = basicIntent();
        intent.addCategory(Intent.CATEGORY_APP_BROWSER);
        intent.addCategory(Intent.CATEGORY_APP_CALCULATOR);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowCategory(Intent.CATEGORY_APP_CALENDAR);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getCategories()).isNull();
    }

    @Test
    public void intentWithCategories_builderAllowOneCategory_OneFilteredIn() {
        Intent intent = basicIntent();
        intent.addCategory(Intent.CATEGORY_APP_BROWSER);
        intent.addCategory(Intent.CATEGORY_APP_CALCULATOR);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowCategory(Intent.CATEGORY_APP_CALCULATOR);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getCategories()).containsExactly(Intent.CATEGORY_APP_CALCULATOR);
    }

    @Test
    public void intentWithCategories_builderAllowAllCategories_AllFilteredIn() {
        Intent intent = basicIntent();
        intent.addCategory(Intent.CATEGORY_APP_BROWSER);
        intent.addCategory(Intent.CATEGORY_APP_CALCULATOR);
        IntentSanitizer.Builder builder = basicSanitizerBuilder()
                .allowCategory(Intent.CATEGORY_APP_CALCULATOR)
                .allowCategory(Intent.CATEGORY_APP_BROWSER);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getCategories()).isNotNull();
        assertThat(intent.getCategories()).containsExactly(Intent.CATEGORY_APP_CALCULATOR,
                Intent.CATEGORY_APP_BROWSER);
    }

    @Test
    public void intentWithCategories_builderAllowMoreCategories_AllFilteredIn() {
        Intent intent = basicIntent();
        intent.addCategory(Intent.CATEGORY_APP_BROWSER);
        intent.addCategory(Intent.CATEGORY_APP_CALCULATOR);
        IntentSanitizer.Builder builder = basicSanitizerBuilder()
                .allowCategory(Intent.CATEGORY_APP_CALCULATOR)
                .allowCategory(Intent.CATEGORY_APP_BROWSER)
                .allowCategory(Intent.CATEGORY_APP_EMAIL);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getCategories()).isNotNull();
        assertThat(intent.getCategories()).containsExactly(Intent.CATEGORY_APP_CALCULATOR,
                Intent.CATEGORY_APP_BROWSER);
    }

    @Test
    public void intentWithComponent_builderAllowsNoComponent_ThrowException() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByThrowing(intent));
    }

    @Test
    public void intentWithComponent_builderAllowsIncorrectComponent_ThrowException() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(TEST_COMPONENT_2);

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByThrowing(intent));
    }

    @Test
    public void intentWithComponent_builderAllowsCorrectComponent_filterIn() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(TEST_COMPONENT);

        Intent newIntent = builder.build().sanitizeByFiltering(intent);

        assertThat(newIntent.getComponent()).isEqualTo(TEST_COMPONENT);
    }

    @Test
    public void intentWithComponent_builderAllowsMoreComponents_filterIn() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(TEST_COMPONENT)
                .allowComponent(TEST_COMPONENT_2);

        Intent newIntent = builder.build().sanitizeByFiltering(intent);

        assertThat(newIntent.getComponent()).isEqualTo(TEST_COMPONENT);
    }

    @Test
    public void intentWithComponent_builderAllowsComponentWithPredicate_filterIn() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(v -> v.getClassName().startsWith("Sanitizer"));

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getComponent()).isEqualTo(TEST_COMPONENT);
    }

    @Test
    public void intentWithComponent_builderAllowsComponentWithPackage_filterIn() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponentWithPackage("com.test.intent.sanitizer");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getComponent()).isEqualTo(TEST_COMPONENT);
    }

    @Test
    public void intentWithComponent_builderAllowsPackage_filterIn() {
        Intent intent = new Intent();
        intent.setPackage(TEST_PACKAGE_NAME);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowAnyComponent()
                .allowPackage(TEST_PACKAGE_NAME);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getPackage()).isEqualTo(TEST_PACKAGE_NAME);
    }

    @Test
    public void intentWithComponent_builderAllowsPackageWithPredicate_filterIn() {
        Intent intent = new Intent();
        intent.setPackage(TEST_PACKAGE_NAME);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowAnyComponent()
                .allowPackage(v -> v.startsWith("com.test.intent"));

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getPackage()).isEqualTo(TEST_PACKAGE_NAME);
    }

    @Test
    public void intentWithComponent_builderAllowsAnyComponent_filterIn() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowAnyComponent();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getComponent()).isEqualTo(TEST_COMPONENT);
    }

    @Test
    public void intentWithComponent_builderAllowsAnyComponentAndAllowsComponent_throwException() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(TEST_COMPONENT)
                .allowAnyComponent();

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByFiltering(intent));
    }

    @Test
    public void intentWithComponent_builderMixAllowsAnyComponentAndComponentPred_throwException() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowAnyComponent()
                .allowComponent(v -> v.getPackageName().equals(TEST_COMPONENT.getPackageName()));

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByFiltering(intent));
    }

    @Test
    public void intentWithComponent_builderMixAllowsAnyComponentAndCompWithPack_throwException() {
        Intent intent = new Intent();
        intent.setComponent(TEST_COMPONENT);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponentWithPackage(TEST_COMPONENT.getPackageName());
        builder.allowAnyComponent();

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByFiltering(intent));
    }

    @Test
    public void implicitIntent_builderAllowAnyComponent_filterIn() {
        Intent intent = new Intent(TEST_INTENT_FILTER_ACTION);
        intent.setType(TEST_INTENT_FILTER_DATATYPE);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowAnyComponent()
                .allowAction(TEST_INTENT_FILTER_ACTION)
                .allowType(TEST_INTENT_FILTER_DATATYPE);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getAction()).isEqualTo(TEST_INTENT_FILTER_ACTION);
        assertThat(intent.getType()).isEqualTo(TEST_INTENT_FILTER_DATATYPE);
    }

    @Test
    public void implicitIntent_builderDoNotCallAllowComponent_throwException() {
        Intent intent = new Intent(TEST_INTENT_FILTER_ACTION);
        intent.setType(TEST_INTENT_FILTER_DATATYPE);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowAction(TEST_INTENT_FILTER_ACTION)
                .allowType(TEST_INTENT_FILTER_DATATYPE);

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByFiltering(intent));
    }

    @Test
    public void implicitIntent_builderAllowSomeComponent_throwException() {
        Intent intent = new Intent(TEST_INTENT_FILTER_ACTION);
        intent.setType(TEST_INTENT_FILTER_DATATYPE);
        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(TEST_COMPONENT)
                .allowAction(TEST_INTENT_FILTER_ACTION)
                .allowType(TEST_INTENT_FILTER_DATATYPE);

        assertThrows(SecurityException.class, () -> builder.build().sanitizeByThrowing(intent));
    }

    @Test
    public void intentWithExtras_builderAllowsNoExtra_filterOutAllExtras() {
        Intent intent = basicIntent();
        intent.putExtra("IntValue", 1);
        intent.putExtra("StringValue", "string");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getExtras()).isNull();
    }

    @Test
    public void intentWithExtras_builderAllowsStringExtra_filterIn() {
        Intent intent = basicIntent();
        intent.putExtra("IntValue", 1);
        intent.putExtra("StringValue", "string");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowExtra("StringValue", String.class);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getStringExtra("StringValue")).isEqualTo("string");
        assertThat(intent.hasExtra("IntValue")).isFalse();
    }

    @Test
    public void intentWithExtras_builderAllowsIntExtraWithWrongType_filterOut() {
        Intent intent = basicIntent();
        intent.putExtra("IntValue", 1);
        intent.putExtra("StringValue", "string");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowExtra("IntValue", String.class);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.hasExtra("IntValue")).isFalse();
        assertThat(intent.hasExtra("StringValue")).isFalse();
    }

    @Test
    public void intentWithExtras_builderAllowsIntExtraWithFailPredicate_filterOut() {
        Intent intent = basicIntent();
        intent.putExtra("IntValue", 1);
        intent.putExtra("StringValue", "string");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowExtra("IntValue", Integer.class, v -> v > 10);

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.hasExtra("IntValue")).isFalse();
        assertThat(intent.hasExtra("StringValue")).isFalse();
    }

    @Test
    public void intentWithExtras_builderAllowsBothExtrasWithPassPredicate_filterIn() {
        Intent intent = basicIntent();
        intent.putExtra("IntValue", 1);
        intent.putExtra("StringValue", "string");
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowExtra("IntValue", Integer.class, v -> v < 5)
                .allowExtra("StringValue", String.class, v -> v.startsWith("str"));

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getIntExtra("IntValue", -1)).isEqualTo(1);
        assertThat(intent.getStringExtra("StringValue")).isEqualTo("string");
    }

    @Test
    @Config(minSdk = 16)
    public void intentWithTextOnlyClipData_builderAllowsNoClipData_filterOut() {
        Intent intent = basicIntent();
        ClipDescription description = new ClipDescription("test",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        ClipData clipData = new ClipData(description, new ClipData.Item("text"));
        intent.setClipData(clipData);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getClipData()).isNull();
    }

    @Test
    @Config(minSdk = 16)
    public void intentWithTextOnlyClipData_builderAllowsClipDataText_filterIn() {
        Intent intent = basicIntent();
        ClipDescription description = new ClipDescription("test",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        ClipData clipData = new ClipData(description, new ClipData.Item("text"));
        intent.setClipData(clipData);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowClipDataText();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getClipData().toString()).isEqualTo(clipData.toString());
    }

    @Test
    @Config(minSdk = 16)
    public void intentWithClipDataWithTextAndUri_builderAllowsClipDataText_filterInTextButOutUri() {
        Intent intent = basicIntent();
        ClipDescription description = new ClipDescription("test",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        ClipData clipData = new ClipData(description, new ClipData.Item("text", null,
                Uri.parse("content://people/1")));
        intent.setClipData(clipData);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowClipDataText();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getClipData()).isNotNull();
        assertThat(intent.getClipData().getItemAt(0).getText()).isEqualTo("text");
        assertThat(intent.getClipData().getItemAt(0).getUri()).isNull();
    }

    @Test
    @Config(minSdk = 16)
    public void intentWithClipDataWithTextAndUri_builderAllowsClipDataUri_filterInUriButOutText() {
        Intent intent = basicIntent();
        ClipDescription description = new ClipDescription("test",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        ClipData clipData = new ClipData(description, new ClipData.Item("text", null,
                Uri.parse("content://people/1")));
        intent.setClipData(clipData);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowClipDataUriWithAuthority("people");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getClipData()).isNotNull();
        assertThat(intent.getClipData().getItemAt(0).getText()).isNull();
        assertThat(intent.getClipData().getItemAt(0).getUri()).isEqualTo(
                Uri.parse("content://people/1"));
    }

    @Test
    @Config(minSdk = 16)
    public void intentWithClipDataWithTextAndUri_builderAllowsClipDataTextAndUri_filterIn() {
        Intent intent = basicIntent();
        ClipDescription description = new ClipDescription("test",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        ClipData clipData = new ClipData(description, new ClipData.Item("text", null,
                Uri.parse("content://people/1")));
        intent.setClipData(clipData);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        UriMatcher uriMatcher = new UriMatcher(0);
        uriMatcher.addURI("people", "#", 1);
        builder.allowClipDataText();
        builder.allowClipDataUri(UriMatcherCompat.asPredicate(uriMatcher));

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getClipData().toString()).isEqualTo(clipData.toString());
    }

    @Test
    @Config(minSdk = 16)
    public void intentWithClipDataMultiItems_builderAllowsTextAndUri_filterInItemPassUriFilter() {
        Intent intent = basicIntent();
        ClipDescription description = new ClipDescription("test",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        ClipData clipData = new ClipData(description, new ClipData.Item("text", null,
                Uri.parse("content://people/1")));
        clipData.addItem(new ClipData.Item("text2", null, Uri.parse("content://address/1")));
        intent.setClipData(clipData);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowClipDataText();
        builder.allowClipDataUriWithAuthority("people");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getClipData().getItemAt(0).toString()).isEqualTo(
                clipData.getItemAt(0).toString());
        assertThat(intent.getClipData().getItemAt(1).getUri()).isNull();
    }

    @Test
    @Config(minSdk = 16)
    public void intentWithClipDataWithPlainMimeType_builderAllowsHtmlMimeTypePredicate_filterOut() {
        Intent intent = basicIntent();
        ClipDescription description = new ClipDescription("test",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        ClipData clipData = new ClipData(description, new ClipData.Item("text", null,
                Uri.parse("content://people/1")));
        clipData.addItem(new ClipData.Item("text2", null, Uri.parse("content://address/1")));
        intent.setClipData(clipData);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowClipData(
                v -> v.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML));

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getClipData()).isNull();
    }

    @Test
    public void intentWithExtraStream_builderAllowsNoExtraStream_filterOut() {
        Intent intent = basicIntent();
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getExtras()).isNull();
    }

    @Test
    public void intentWithExtraStream_builderAllowsExtraStreamNoFlags_filterOut() {
        Intent intent = basicIntent();
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowExtraStreamUriWithAuthority("people");

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getExtras()).isNull();
    }

    @Test
    public void intentWithExtraStream_builderAllowsExtraStreamAndFlags_filterIn() {
        Intent intent = basicIntent();
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://people/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        UriMatcher uriMatcher = new UriMatcher(0);
        uriMatcher.addURI("people", "#", 1);
        builder.allowExtraStream(UriMatcherCompat.asPredicate(uriMatcher));
        builder.allowFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent = builder.build().sanitizeByFiltering(intent);

        Uri extraStream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        assertThat(extraStream).isEqualTo(Uri.parse("content://people/1"));
    }

    @Test
    public void intentWithExtraOutput_builderAllowsExtraOutputNoFlags_filterOut() {
        Intent intent = basicIntent();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("content://address/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowExtraOutput("address");

        intent = builder.build().sanitizeByFiltering(intent);

        Uri extraOutput = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        assertThat(extraOutput).isNull();
    }

    @Test
    public void intentWithExtraOutput_builderAllowsExtraOutputOneFlag_filterOut() {
        Intent intent = basicIntent();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("content://address/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowExtraOutput("address");
        builder.allowFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent = builder.build().sanitizeByFiltering(intent);

        Uri extraOutput = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        assertThat(extraOutput).isNull();
    }

    @Test
    public void intentWithExtraOutput_builderAllowsExtraOutputAndFlags_filterIn() {
        Intent intent = basicIntent();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("content://address/1"));
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        UriMatcher uriMatcher = new UriMatcher(0);
        uriMatcher.addURI("address", "#", 1);
        builder.allowExtraOutput(UriMatcherCompat.asPredicate(uriMatcher))
                .allowFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .allowFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent = builder.build().sanitizeByFiltering(intent);

        Uri extraOutput = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        assertThat(extraOutput).isEqualTo(Uri.parse("content://address/1"));
    }

    @Test
    public void intentWithSourceBounds_builderAllowsNoSourceBounds_filterOut() {
        Intent intent = basicIntent();
        Rect sourceBounds = new Rect(0, 0, 1, 1);
        intent.setSourceBounds(sourceBounds);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getSourceBounds()).isNull();
    }

    @Test
    public void intentWithSourceBounds_builderAllowsSourceBounds_filterIn() {
        Intent intent = basicIntent();
        Rect sourceBounds = new Rect(0, 0, 1, 1);
        intent.setSourceBounds(sourceBounds);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowSourceBounds();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getSourceBounds()).isEqualTo(sourceBounds);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void intentWithSelector_builderAllowsNoSelector_filterOut() {
        Intent intent = basicIntent();
        Intent selector = new Intent(Intent.ACTION_VIEW);
        intent.setSelector(selector);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getSelector()).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void intentWithSelector_builderAllowsSelector_filterIn() {
        Intent intent = basicIntent();
        Intent selector = new Intent(Intent.ACTION_VIEW);
        intent.setSelector(selector);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowSelector();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getSelector()).isEqualTo(selector);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void intentWithIdentifier_builderAllowsNoIdentifier_filterOut() {
        Intent intent = basicIntent();
        String identifier = "identifier";
        intent.setIdentifier(identifier);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getIdentifier()).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void intentWithIdentifier_builderAllowsIdentifier_filterIn() {
        Intent intent = basicIntent();
        String identifier = "identifier";
        intent.setIdentifier(identifier);
        IntentSanitizer.Builder builder = basicSanitizerBuilder();
        builder.allowIdentifier();

        intent = builder.build().sanitizeByFiltering(intent);

        assertThat(intent.getIdentifier()).isEqualTo(identifier);
    }

    @Test
    public void intentWithAllTypesOfExtra_builderAllowsAllTypesOfExtra_filterIn() {
        ComponentName componentName = new ComponentName("com.test.intent.sanitizer",
                "SanitizerService");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.putExtra("bool", true);
        intent.putExtra("int", 100000);
        intent.putExtra("short", (short) 1000);
        intent.putExtra("byte", (byte) 1);
        intent.putExtra("char", 'a');
        intent.putExtra("long", 3_000_000_000L);
        intent.putExtra("float", 1.23f);
        intent.putExtra("double", 1.23);
        intent.putExtra("String", "test string");
        intent.putExtra("CharSequence", (CharSequence) (new StringBuilder("test String")));
        intent.putExtra("Parcelable", new Intent(Intent.ACTION_VIEW));
        intent.putExtra("Serializable", new FileNotFoundException("test file not found"));
        intent.putExtra("bool[]", new boolean[]{true});
        intent.putExtra("int[]", new int[]{100000});
        intent.putExtra("short[]", new short[]{(short) 1000});
        intent.putExtra("byte[]", new byte[]{0x1});
        intent.putExtra("char[]", new char[]{'a'});
        intent.putExtra("long[]", new long[]{3_000_000_000L});
        intent.putExtra("float[]", new float[]{1.23f});
        intent.putExtra("double[]", new double[]{1.23});
        intent.putExtra("String[]", new String[]{"test string"});
        intent.putExtra("CharSequence[]", new CharSequence[]{(new StringBuilder(
                "test String"))});
        intent.putExtra("Parcelable[]", new Parcelable[]{new Intent(Intent.ACTION_VIEW)});
        Bundle bundle = new Bundle();
        bundle.putString("testKey", "test value");
        intent.putExtra("Bundle", bundle);
        ArrayList<Integer> integerArrayList = new ArrayList<>();
        integerArrayList.add(1);
        intent.putIntegerArrayListExtra("IntArrayList", integerArrayList);
        ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add("test");
        intent.putStringArrayListExtra("StrArrayList", stringArrayList);
        ArrayList<CharSequence> charSequenceArrayList = new ArrayList<>();
        charSequenceArrayList.add(new StringBuilder("test"));
        intent.putCharSequenceArrayListExtra("CharSeqArrayList", charSequenceArrayList);
        ArrayList<Parcelable> parcelableArrayList = new ArrayList<>();
        parcelableArrayList.add(new Intent(Intent.ACTION_VIEW));
        intent.putParcelableArrayListExtra("ParcelArrayList", parcelableArrayList);

        IntentSanitizer.Builder builder = new IntentSanitizer.Builder();
        builder.allowComponent(componentName)
                .allowExtra("int", Integer.class)
                .allowExtra("bool", Boolean.class)
                .allowExtra("short", Short.class)
                .allowExtra("long", Long.class)
                .allowExtra("byte", Byte.class)
                .allowExtra("char", Character.class)
                .allowExtra("float", Float.class)
                .allowExtra("double", Double.class)
                .allowExtra("String", String.class)
                .allowExtra("CharSequence", CharSequence.class)
                .allowExtra("Parcelable", Parcelable.class)
                .allowExtra("Serializable", Serializable.class)
                .allowExtra("int[]", int[].class)
                .allowExtra("bool[]", boolean[].class)
                .allowExtra("short[]", short[].class)
                .allowExtra("long[]", long[].class)
                .allowExtra("byte[]", byte[].class)
                .allowExtra("char[]", char[].class)
                .allowExtra("float[]", float[].class)
                .allowExtra("double[]", double[].class)
                .allowExtra("String[]", String[].class)
                .allowExtra("CharSequence[]", CharSequence[].class)
                .allowExtra("Parcelable[]", Parcelable[].class)
                .allowExtra("Serializable", Serializable.class)
                .allowExtra("IntArrayList", ArrayList.class,
                        v -> v != null && (v.isEmpty() || v.get(0) instanceof Integer))
                .allowExtra("StrArrayList", ArrayList.class,
                        v -> v != null && (v.isEmpty() || v.get(0) instanceof String))
                .allowExtra("CharSeqArrayList", ArrayList.class,
                        v -> v != null && (v.isEmpty() || v.get(0) instanceof CharSequence))
                .allowExtra("ParcelArrayList", ArrayList.class,
                        v -> v != null && (v.isEmpty() || v.get(0) instanceof Parcelable));

        Intent newIntent = builder.build().sanitizeByFiltering(intent);

        assertThat(newIntent.getIntExtra("int", 0)).isEqualTo(100000);
        assertThat(newIntent.getBooleanExtra("bool", false)).isTrue();
        assertThat(newIntent.getByteExtra("byte", (byte) 0)).isEqualTo(1);
        assertThat(newIntent.getShortExtra("short", (short) 0)).isEqualTo(1000);
        assertThat(newIntent.getLongExtra("long", 0L))
                .isEqualTo(3_000_000_000L);
        assertThat(newIntent.getFloatExtra("float", 0f)).isEqualTo(1.23f);
        assertThat(newIntent.getDoubleExtra("double", 1.23)).isEqualTo(1.23);
        assertThat(newIntent.getStringExtra("String")).isEqualTo("test string");
        assertThat(newIntent.getCharSequenceExtra("CharSequence").toString())
                .isEqualTo("test String");
        Intent extraIntent = newIntent.getParcelableExtra("Parcelable");
        assertThat(extraIntent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(newIntent.getSerializableExtra("Serializable").toString())
                .isEqualTo(new FileNotFoundException("test file not found").toString());
        assertThat(newIntent.getIntArrayExtra("int[]")[0]).isEqualTo(100000);
        assertThat(newIntent.getBooleanArrayExtra("bool[]")[0]).isTrue();
        assertThat(newIntent.getByteArrayExtra("byte[]")[0]).isEqualTo(1);
        assertThat(newIntent.getShortArrayExtra("short[]")[0]).isEqualTo(1000);
        assertThat(newIntent.getLongArrayExtra("long[]")[0])
                .isEqualTo(3_000_000_000L);
        assertThat(newIntent.getFloatArrayExtra("float[]")[0]).isEqualTo(1.23f);
        assertThat(newIntent.getDoubleArrayExtra("double[]")[0]).isEqualTo(1.23);
        assertThat(newIntent.getStringArrayExtra("String[]")[0])
                .isEqualTo("test string");
        assertThat(newIntent.getCharSequenceArrayExtra("CharSequence[]")[0].toString())
                .isEqualTo("test String");
        extraIntent = (Intent) newIntent.getParcelableArrayExtra("Parcelable[]")[0];
        assertThat(extraIntent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(newIntent.getIntegerArrayListExtra("IntArrayList"))
                .isEqualTo(integerArrayList);
        assertThat(newIntent.getCharSequenceArrayListExtra("CharSeqArrayList"))
                .isEqualTo(charSequenceArrayList);
        assertThat(newIntent.getStringArrayListExtra("StrArrayList"))
                .isEqualTo(stringArrayList);
        assertThat(newIntent.getParcelableArrayListExtra("ParcelArrayList"))
                .isEqualTo(parcelableArrayList);
    }
}
