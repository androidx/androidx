/*
 * Copyright 2020 The Android Open Source Project
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

package android.support.wearable.complications;

import static android.support.wearable.complications.ComplicationData.IMAGE_STYLE_ICON;
import static android.support.wearable.complications.ComplicationData.IMAGE_STYLE_PHOTO;
import static android.support.wearable.complications.ComplicationData.TYPE_LONG_TEXT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ComplicationDataTest {

    private static final CharSequence TEST_CONTENT_DESCRIPTION = "This is a test description!";
    private static final String TEST_LONG_TITLE = "what a long title such a long title";
    private static final String TEST_LONG_TEXT = "such long text so much text omg";

    private PendingIntent mPendingIntent;

    @Before
    public void setUp() throws Exception {
        mPendingIntent =
                PendingIntent.getBroadcast(
                        ApplicationProvider.getApplicationContext(), 0, new Intent("ACTION"), 0);
    }

    @Test
    public void testShortTextFields() {
        // GIVEN complication data of the SHORT_TEXT type created by the Builder...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setShortTitle(ComplicationText.plainText("title"))
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertThat(data.getShortText().getText(null, 0)).isEqualTo("text");
        assertThat(data.getShortTitle().getText(null, 0)).isEqualTo("title");
    }

    @Test
    public void testLongTextFields() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder...
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                        .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(TEST_LONG_TITLE, data.getLongTitle().getText(null, 0));
        assertEquals(TEST_LONG_TEXT, data.getLongText().getText(null, 0));
    }

    @Test
    public void testRangedValueFields() {
        // GIVEN complication data of the RANGED_VALUE type created by the Builder...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedValue(57f)
                        .setRangedMinValue(5f)
                        .setRangedMaxValue(150f)
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setShortText(ComplicationText.plainText("text"))
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(data.getRangedValue(), 57f, 0);
        assertEquals(data.getRangedMinValue(), 5f, 0);
        assertEquals(data.getRangedMaxValue(), 150f, 0);
        assertThat(data.getShortTitle().getText(null, 0)).isEqualTo("title");
        assertThat(data.getShortText().getText(null, 0)).isEqualTo("text");
    }

    @Test
    public void testShortTextMustContainShortText() {
        // GIVEN a complication builder of the SHORT_TEXT type, with the short text field not
        // populated...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortTitle(ComplicationText.plainText("title"));

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testLongTextMustContainLongText() {
        // GIVEN a complication builder of the LONG_TEXT type, with the long text field not
        // populated...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText("title"));

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testRangedValueMustContainValue() {
        // GIVEN a complication builder of the RANGED_VALUE type, with the value field not
        // populated...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedMinValue(5f)
                        .setRangedMaxValue(150f)
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setShortText(ComplicationText.plainText("text"));

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testRangedValueMustContainMinValue() {
        // GIVEN a complication builder of the RANGED_VALUE type, with the min value field not
        // populated...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedValue(75f)
                        .setRangedMaxValue(150f)
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setShortText(ComplicationText.plainText("text"));

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testRangedValueMustContainMaxValue() {
        // GIVEN a complication builder of the RANGED_VALUE type, with the max value field not
        // populated...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedValue(75f)
                        .setRangedMinValue(15f)
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setShortText(ComplicationText.plainText("text"));

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testLongTitleNotValidForShortText() {
        // GIVEN complication data of the SHORT_TEXT type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setShortText(ComplicationText.plainText("text"))
                        .build();

        // WHEN getLongTitle is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getLongTitle);
    }

    @Test
    public void testValueNotValidForShortText() {
        // GIVEN complication data of the SHORT_TEXT type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setShortText(ComplicationText.plainText("text"))
                        .build();

        // WHEN getValue is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getRangedValue);
    }

    @Test
    public void testValueNotValidForLongText() {
        // GIVEN complication data of the LONG_TEXT type...
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText("title"))
                        .setLongText(ComplicationText.plainText("long"))
                        .build();

        // WHEN getValue is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getRangedValue);
    }

    @Test
    public void testShortTitleNotValidForLongText() {
        // GIVEN complication data of the LONG_TEXT type...
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText("title"))
                        .setLongText(ComplicationText.plainText("long"))
                        .build();

        // WHEN getShortTitle is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getShortTitle);
    }

    @Test
    public void testLongTitleNotValidForRangedValue() {
        // GIVEN complication data of the RANGED_VALUE type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedValue(57f)
                        .setRangedMinValue(5f)
                        .setRangedMaxValue(150f)
                        .build();

        // WHEN getLongTitle is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getLongTitle);
    }

    @Test
    public void testIconTypeIconField() {
        // GIVEN complication data of the ICON type created by the Builder...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON).setIcon(icon).build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(data.getIcon(), icon);
    }

    @Test
    public void testGetShortTextNotValidForIcon() {
        // GIVEN complication data of the ICON type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(Icon.createWithContentUri("someuri"))
                        .build();

        // WHEN getLongTitle is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getShortText);
    }

    @Test
    public void testGetLongTextNotValidForIcon() {
        // GIVEN complication data of the ICON type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(Icon.createWithContentUri("someuri"))
                        .build();

        // WHEN getLongText is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getLongText);
    }

    @Test
    public void testSetShortTitleNotValidForIcon() {
        // GIVEN a complication data builder of the ICON type...
        ComplicationData.Builder builder = new ComplicationData.Builder(ComplicationData.TYPE_ICON);

        // WHEN setShortTitle is called
        // THEN IllegalStateException is thrown.
        assertThrows(
                IllegalStateException.class,
                () -> builder.setShortTitle(ComplicationText.plainText("title")));
    }

    @Test
    public void testShortTextFieldsIncludingIcon() {
        // GIVEN complication data of the SHORT_TEXT type created by the Builder, including an
        // icon...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setIcon(icon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertThat(data.getShortText().getText(null, 0)).isEqualTo("text");
        assertEquals(data.getIcon(), icon);
    }

    @Test
    public void testLongTextFieldsIncludingIcon() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder, including an
        // icon...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                        .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                        .setIcon(icon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(TEST_LONG_TITLE, data.getLongTitle().getText(null, 0));
        assertEquals(TEST_LONG_TEXT, data.getLongText().getText(null, 0));
        assertEquals(icon, data.getIcon());
    }

    @Test
    public void testRangedValueFieldsIncludingIcon() {
        // GIVEN complication data of the RANGED_VALUE type created by the Builder, including an
        // icon...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedValue(57f)
                        .setRangedMinValue(5f)
                        .setRangedMaxValue(150f)
                        .setIcon(icon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(data.getRangedValue(), 57f, 0);
        assertEquals(data.getRangedMinValue(), 5f, 0);
        assertEquals(data.getRangedMaxValue(), 150f, 0);
        assertEquals(data.getIcon(), icon);
    }

    @Test
    public void testGetLongTextNotValidForEmpty() {
        // GIVEN complication data of the EMPTY type...
        ComplicationData data = new ComplicationData.Builder(ComplicationData.TYPE_EMPTY).build();

        // WHEN getLongTitle is called
        // THEN IllegalStateException is thrown.
        assertThrows(
                IllegalStateException.class, data::getLongText);
    }

    @Test
    public void testSetShortTextNotValidForEmpty() {
        // GIVEN a complication data builder of the EMPTY type...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_EMPTY);

        // WHEN setShortText is called
        // THEN IllegalStateException is thrown.
        assertThrows(
                IllegalStateException.class,
                () -> builder.setShortText(ComplicationText.plainText("text")));
    }

    @Test
    public void testSetIconNotValidForEmpty() {
        // GIVEN a complication data builder of the EMPTY type...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_EMPTY);

        // WHEN setIcon is called
        // THEN IllegalStateException is thrown.
        assertThrows(
                IllegalStateException.class,
                () -> builder.setIcon(Icon.createWithContentUri("uri")));
    }

    @Test
    public void testGetLongTextNotValidForNotConfigured() {
        // GIVEN complication data of the NOT_CONFIGURED type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_NOT_CONFIGURED).build();

        // WHEN getLongText is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getLongText);
    }

    @Test
    public void testSetShortTextNotValidForNotConfigured() {
        // GIVEN a complication data builder of the NOT_CONFIGURED type...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_NOT_CONFIGURED);

        // WHEN setShortText is called
        // THEN IllegalStateException is thrown.
        assertThrows(
                IllegalStateException.class,
                () -> builder.setShortText(ComplicationText.plainText("text")));
    }

    @Test
    public void testSetIconNotValidForNotConfigured() {
        // GIVEN a complication data builder of the NOT_CONFIGURED type...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_NOT_CONFIGURED);

        // WHEN setIcon is called
        // THEN IllegalStateException is thrown.
        assertThrows(
                IllegalStateException.class,
                () -> builder.setIcon(Icon.createWithContentUri("uri")));
    }

    @Test
    public void testLongTextFieldsIncludingSmallImage() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder, including an
        // icon...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                        .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                        .setSmallImage(icon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(TEST_LONG_TITLE, data.getLongTitle().getText(null, 0));
        assertEquals(TEST_LONG_TEXT, data.getLongText().getText(null, 0));
        assertEquals(icon, data.getSmallImage());
    }

    @Test
    public void testGetShortTextNotValidForSmallImage() {
        // GIVEN complication data of the SMALL IMAGE type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(Icon.createWithContentUri("someuri"))
                        .build();

        // WHEN getShortText is called
        // THEN null is returned.
        assertThrows(IllegalStateException.class, data::getShortText);
    }

    @Test
    public void testSmallImageTypeSmallImageField() {
        // GIVEN complication data of the SMALL IMAGE type created by the Builder...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(icon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(data.getSmallImage(), icon);
    }

    @Test
    public void testSmallImageTypeSmallImageFieldAndBurnInSmallImage() {
        // GIVEN complication data of the SMALL IMAGE type created by the Builder...
        Icon icon = Icon.createWithContentUri("someuri");
        Icon burnInIcon = Icon.createWithContentUri("burnInSomeuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(icon)
                        .setBurnInProtectionSmallImage(burnInIcon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(data.getSmallImage(), icon);
        assertEquals(data.getBurnInProtectionSmallImage(), burnInIcon);
    }

    @Test
    public void testLargeImageTypeLargeImageField() {
        // GIVEN complication data of the LARGE IMAGE type created by the Builder...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                        .setLargeImage(icon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(data.getLargeImage(), icon);
    }

    @Test
    public void testGetLongTextNotValidForLargeImage() {
        // GIVEN complication data of the LARGE IMAGE type...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                        .setLargeImage(Icon.createWithContentUri("someuri"))
                        .build();

        // WHEN getLongText is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, data::getLongText);
    }

    @Test
    public void testSmallImageRequiresSmallImage() {
        // GIVEN a complication data builder of the SMALL IMAGE type...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE);

        // WHEN build is called and setSmallImage has not been called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testLargeImageRequiresLargeImage() {
        // GIVEN a complication data builder of the LARGE IMAGE type...
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE);

        // WHEN build is called and setLargeImage has not been called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testIconTypeTapActionField() {
        // GIVEN complication data of the ICON type created by the Builder...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(icon)
                        .setTapAction(mPendingIntent)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(data.getTapAction(), mPendingIntent);
    }

    @Test
    public void testNoStartEndAlwaysActive() {
        // GIVEN complication data with no start or end time specified...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setShortTitle(ComplicationText.plainText("title"))
                        .build();

        // WHEN isActive is called for any time
        // THEN result is true
        assertTrue(data.isActive(1000));
        assertTrue(data.isActive(100000000000L));
        assertTrue(data.isActive(1000000000));
        assertTrue(data.isActive(100000000000000000L));
        assertTrue(data.isActive(999999999));
    }

    @Test
    public void testStartNoEnd() {
        // GIVEN complication data with a start time but no end time specified...
        long startTime = 1000000;
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setStartDateTimeMillis(startTime)
                        .build();

        // WHEN isActive is called for times before startTime
        // THEN result is false
        assertFalse(data.isActive(startTime - 1));
        assertFalse(data.isActive(startTime - 1000));
        assertFalse(data.isActive(0));

        // WHEN isActive is called for times at or after startTime
        // THEN result is true
        assertTrue(data.isActive(startTime));
        assertTrue(data.isActive(startTime + 1));
        assertTrue(data.isActive(startTime + 1000000000));
        assertTrue(data.isActive(startTime + 100000000000000L));
    }

    @Test
    public void testEndNoStart() {
        // GIVEN complication data with an end time but no start time specified...
        long endTime = 1000000000000L;
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setEndDateTimeMillis(endTime)
                        .build();

        // WHEN isActive is called for times after endTime
        // THEN result is false
        assertFalse(data.isActive(endTime + 1));
        assertFalse(data.isActive(endTime + 1000));
        assertFalse(data.isActive(endTime + 100000000000000L));

        // WHEN isActive is called for times before endTime
        // THEN result is true
        assertTrue(data.isActive(endTime - 1));
        assertTrue(data.isActive(endTime - 10));
        assertTrue(data.isActive(endTime - 100000000));
        assertTrue(data.isActive(endTime - 10000000000L));
    }

    @Test
    public void testStartAndEnd() {
        // GIVEN complication data with a start time and end time specified...
        long startTime = 1000000;
        long endTime = 1000000000000L;
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setShortTitle(ComplicationText.plainText("title"))
                        .setStartDateTimeMillis(startTime)
                        .setEndDateTimeMillis(endTime)
                        .build();

        // WHEN isActive is called for times before startTime
        // THEN result is false
        assertFalse(data.isActive(startTime - 1));
        assertFalse(data.isActive(startTime - 1000));
        assertFalse(data.isActive(0));

        // WHEN isActive is called for times after endTime
        // THEN result is false
        assertFalse(data.isActive(endTime + 1));
        assertFalse(data.isActive(endTime + 1000));
        assertFalse(data.isActive(endTime + 100000000000000L));

        // WHEN isActive is called for times between the start and end time
        // THEN result is true
        assertTrue(data.isActive(endTime - 1));
        assertTrue(data.isActive((startTime + endTime) / 2));
        assertTrue(data.isActive(endTime - 100000000));
        assertTrue(data.isActive(startTime + 1000));
    }

    @Test
    public void testSmallImageDefaultImageStyle() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, but with
        // the image style not set...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(Icon.createWithContentUri("someuri"))
                        .build();

        // WHEN the image style is retrieved
        // THEN the default of IMAGE_STYLE_PHOTO is returned.
        assertThat(data.getSmallImageStyle()).isEqualTo(IMAGE_STYLE_PHOTO);
    }

    @Test
    public void testLongTextDefaultImageStyle() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder, but with
        // the image style not set...
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                        .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                        .setSmallImage(Icon.createWithContentUri("someuri"))
                        .build();

        // WHEN the image style is retrieved
        // THEN the default of IMAGE_STYLE_PHOTO is returned.
        assertThat(data.getSmallImageStyle()).isEqualTo(IMAGE_STYLE_PHOTO);
    }

    @Test
    public void testSmallImageSetImageStyle() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(Icon.createWithContentUri("someuri"))
                        .setSmallImageStyle(IMAGE_STYLE_ICON)
                        .build();

        // WHEN the image style is retrieved
        // THEN the chosen style is returned.
        assertThat(data.getSmallImageStyle()).isEqualTo(IMAGE_STYLE_ICON);
    }

    @Test
    public void testSettingFieldThenSettingNullClearsField() {
        // GIVEN a complication data builder of the SHORT_TEXT type, including an
        // icon...
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setIcon(icon);

        // WHEN the icon is subsequently set to null, and the data is built
        builder.setIcon(null);
        ComplicationData data = builder.build();

        // THEN the icon is null in the resulting data.
        assertNull(data.getIcon());
    }

    @Test
    public void testSetBurnInIconWithoutIconThrowsExceptionOnBuild() {
        // GIVEN a complication data builder of the SHORT_TEXT type, with a burn-in-protection icon
        // specified, but no regular icon
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setBurnInProtectionIcon(Icon.createWithContentUri("someuri"));

        // WHEN build is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testSetBurnInSmallImageWithoutSmallImageThrowsExceptionOnBuild() {
        // GIVEN a complication data builder of the SMALL_IMAGE type, with a burn-in-protection
        // small
        // image specified, but no regular small image
        ComplicationData.Builder builder =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setBurnInProtectionSmallImage(Icon.createWithContentUri("someuri"));

        // WHEN build is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testBothIconFields() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        Icon regularIcon = Icon.createWithContentUri("regular-icon");
        Icon burnInIcon = Icon.createWithContentUri("burn-in-icon");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(regularIcon)
                        .setBurnInProtectionIcon(burnInIcon)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(regularIcon, data.getIcon());
        assertEquals(burnInIcon, data.getBurnInProtectionIcon());
    }

    @Test
    public void testBothSmallImageFields() {
        // GIVEN complication data of the SMALL_IMAGE type, with both icon fields populated
        Icon regularSmallImage = Icon.createWithContentUri("regular-small-image");
        Icon burnInSmallImage = Icon.createWithContentUri("burn-in-small-image");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(regularSmallImage)
                        .setBurnInProtectionSmallImage(burnInSmallImage)
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(regularSmallImage, data.getSmallImage());
        assertEquals(burnInSmallImage, data.getBurnInProtectionSmallImage());
    }

    @Test
    public void testSmallImageWithContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(Icon.createWithContentUri("someuri"))
                        .setSmallImageStyle(IMAGE_STYLE_ICON)
                        .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                        .build();

        assertEquals(TEST_CONTENT_DESCRIPTION, data.getContentDescription().getText(null, 0));
    }

    @Test
    public void testLargeImageWithContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                        .setLargeImage(Icon.createWithContentUri("someuri"))
                        .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                        .build();

        assertEquals(TEST_CONTENT_DESCRIPTION, data.getContentDescription().getText(null, 0));
    }

    @Test
    public void testIconWithNullContentDescription() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        Icon regularIcon = Icon.createWithContentUri("regular-icon");
        Icon burnInIcon = Icon.createWithContentUri("burn-in-icon");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(regularIcon)
                        .setBurnInProtectionIcon(burnInIcon)
                        .build();

        assertNull(data.getContentDescription());
    }

    @Test
    public void testLongTextWithContentDescription() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder...
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                        .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                        .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertEquals(TEST_LONG_TITLE, data.getLongTitle().getText(null, 0));
        assertEquals(TEST_LONG_TEXT, data.getLongText().getText(null, 0));
        assertEquals(TEST_CONTENT_DESCRIPTION, data.getContentDescription().getText(null, 0));
    }

    @Test
    public void testLongTextWithNullContentDescription() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder...
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                        .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                        .build();

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        assertNull(data.getContentDescription());
    }

    @Test
    public void testSmallImageWithImageContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(Icon.createWithContentUri("someuri"))
                        .setSmallImageStyle(IMAGE_STYLE_ICON)
                        .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                        .build();

        assertEquals(TEST_CONTENT_DESCRIPTION, data.getContentDescription().getText(null, 0));
    }

    @Test
    public void testLargeImageWithImageContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                        .setLargeImage(Icon.createWithContentUri("someuri"))
                        .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                        .build();

        assertEquals(TEST_CONTENT_DESCRIPTION, data.getContentDescription().getText(null, 0));
    }

    @Test
    public void testIconWithContentDescription() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        Icon regularIcon = Icon.createWithContentUri("regular-icon");
        Icon burnInIcon = Icon.createWithContentUri("burn-in-icon");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(regularIcon)
                        .setBurnInProtectionIcon(burnInIcon)
                        .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                        .build();

        assertEquals(TEST_CONTENT_DESCRIPTION, data.getContentDescription().getText(null, 0));
    }

    @Test
    public void testIconWithEmptyContentDescription() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        Icon regularIcon = Icon.createWithContentUri("regular-icon");
        Icon burnInIcon = Icon.createWithContentUri("burn-in-icon");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(regularIcon)
                        .setBurnInProtectionIcon(burnInIcon)
                        .setContentDescription(ComplicationText.plainText(""))
                        .build();

        assertEquals(0, data.getContentDescription().getText(null, 0).length());
    }

    @Test
    public void iconNotTimeDependent() {
        Icon icon = Icon.createWithContentUri("someuri");
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_ICON).setIcon(icon).build();

        assertThat(data.isTimeDependent()).isFalse();
    }

    @Test
    public void plainShortTextNotTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("text"))
                        .setShortTitle(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isFalse();
    }

    @Test
    public void timeFormatShortTextIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(
                                new ComplicationText.TimeFormatBuilder().setFormat("mm").build())
                        .setShortTitle(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }

    @Test
    public void timeFormatShortTitleIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortTitle(
                                new ComplicationText.TimeFormatBuilder().setFormat("mm").build())
                        .setShortText(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }

    @Test
    public void timeDifferenceShortTextIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(
                                new ComplicationText.TimeDifferenceBuilder()
                                        .setReferencePeriodStartMillis(100000)
                                        .setReferencePeriodEndMillis(200000)
                                        .build())
                        .setShortTitle(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }

    @Test
    public void timeDifferenceShortTitleIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortTitle(
                                new ComplicationText.TimeDifferenceBuilder()
                                        .setReferencePeriodStartMillis(100000)
                                        .setReferencePeriodEndMillis(200000)
                                        .build())
                        .setShortText(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }

    @Test
    public void plainLongTextNotTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText("text"))
                        .setLongTitle(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isFalse();
    }

    @Test
    public void timeFormatLongTextIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongText(
                                new ComplicationText.TimeFormatBuilder().setFormat("mm").build())
                        .setLongTitle(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }

    @Test
    public void timeFormatLongTitleIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(
                                new ComplicationText.TimeFormatBuilder().setFormat("mm").build())
                        .setLongText(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }

    @Test
    public void timeDifferenceLongTextIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongText(
                                new ComplicationText.TimeDifferenceBuilder()
                                        .setReferencePeriodStartMillis(100000)
                                        .setReferencePeriodEndMillis(200000)
                                        .build())
                        .setLongTitle(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }

    @Test
    public void timeDifferenceLongTitleIsTimeDependent() {
        ComplicationData data =
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongTitle(
                                new ComplicationText.TimeDifferenceBuilder()
                                        .setReferencePeriodStartMillis(100000)
                                        .setReferencePeriodEndMillis(200000)
                                        .build())
                        .setLongText(ComplicationText.plainText("hello"))
                        .build();

        assertThat(data.isTimeDependent()).isTrue();
    }
}
