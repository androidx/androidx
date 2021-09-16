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

package android.support.wearable.complications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationText.TimeDifferenceBuilder
import android.support.wearable.complications.ComplicationText.TimeFormatBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.SharedRobolectricTestRunner
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SharedRobolectricTestRunner::class)
public class ComplicationDataTest {
    private var mPendingIntent: PendingIntent? = PendingIntent.getBroadcast(
        ApplicationProvider.getApplicationContext(),
        0,
        Intent("ACTION"),
        0
    )
    private val mResources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    public fun testShortTextFields() {
        // GIVEN complication data of the SHORT_TEXT type created by the Builder...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setShortTitle(ComplicationText.plainText("title"))
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Truth.assertThat(data.shortText!!.getTextAt(mResources, 0))
            .isEqualTo("text")
        Truth.assertThat(data.shortTitle!!.getTextAt(mResources, 0))
            .isEqualTo("title")
    }

    @Test
    public fun testLongTextFields() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(
            TEST_LONG_TITLE,
            data.longTitle!!.getTextAt(mResources, 0)
        )
        Assert.assertEquals(
            TEST_LONG_TEXT,
            data.longText!!.getTextAt(mResources, 0)
        )
    }

    @Test
    public fun testRangedValueFields() {
        // GIVEN complication data of the RANGED_VALUE type created by the Builder...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(57f)
                .setRangedMinValue(5f)
                .setRangedMaxValue(150f)
                .setShortTitle(ComplicationText.plainText("title"))
                .setShortText(ComplicationText.plainText("text"))
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(data.rangedValue, 57f, 0f)
        Assert.assertEquals(data.rangedMinValue, 5f, 0f)
        Assert.assertEquals(data.rangedMaxValue, 150f, 0f)
        Truth.assertThat(data.shortTitle!!.getTextAt(mResources, 0))
            .isEqualTo("title")
        Truth.assertThat(data.shortText!!.getTextAt(mResources, 0))
            .isEqualTo("text")
    }

    @Test
    public fun testShortTextMustContainShortText() {
        // GIVEN a complication builder of the SHORT_TEXT type, with the short text field not
        // populated...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(ComplicationText.plainText("title"))

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testLongTextMustContainLongText() {
        // GIVEN a complication builder of the LONG_TEXT type, with the long text field not
        // populated...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText("title"))

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testRangedValueMustContainValue() {
        // GIVEN a complication builder of the RANGED_VALUE type, with the value field not
        // populated...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                .setRangedMinValue(5f)
                .setRangedMaxValue(150f)
                .setShortTitle(ComplicationText.plainText("title"))
                .setShortText(ComplicationText.plainText("text"))

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testRangedValueMustContainMinValue() {
        // GIVEN a complication builder of the RANGED_VALUE type, with the min value field not
        // populated...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(75f)
                .setRangedMaxValue(150f)
                .setShortTitle(ComplicationText.plainText("title"))
                .setShortText(ComplicationText.plainText("text"))

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testRangedValueMustContainMaxValue() {
        // GIVEN a complication builder of the RANGED_VALUE type, with the max value field not
        // populated...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(75f)
                .setRangedMinValue(15f)
                .setShortTitle(ComplicationText.plainText("title"))
                .setShortText(ComplicationText.plainText("text"))

        // WHEN build() is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testLongTitleNotValidForShortText() {
        // GIVEN complication data of the SHORT_TEXT type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(ComplicationText.plainText("title"))
                .setShortText(ComplicationText.plainText("text"))
                .build()

        // WHEN getLongTitle is called
        // THEN null is returned.
        Assert.assertNull(data.longTitle)
    }

    @Test
    public fun testValueNotValidForShortText() {
        // GIVEN complication data of the SHORT_TEXT type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(ComplicationText.plainText("title"))
                .setShortText(ComplicationText.plainText("text"))
                .build()

        // WHEN getValue is called
        // THEN zero is returned.
        Assert.assertEquals(data.rangedValue, 0.0f, 0f)
    }

    @Test
    public fun testValueNotValidForLongText() {
        // GIVEN complication data of the LONG_TEXT type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText("title"))
                .setLongText(ComplicationText.plainText("long"))
                .build()

        // WHEN getValue is called
        // THEN zero is returned.
        Assert.assertEquals(data.rangedValue, 0.0f, 0f)
    }

    @Test
    public fun testShortTitleNotValidForLongText() {
        // GIVEN complication data of the LONG_TEXT type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText("title"))
                .setLongText(ComplicationText.plainText("long"))
                .build()

        // WHEN getShortTitle is called
        // THEN null is returned.
        Assert.assertNull(data.shortTitle)
    }

    @Test
    public fun testLongTitleNotValidForRangedValue() {
        // GIVEN complication data of the RANGED_VALUE type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(57f)
                .setRangedMinValue(5f)
                .setRangedMaxValue(150f)
                .build()

        // WHEN getLongTitle is called
        // THEN null is returned.
        Assert.assertNull(data.longTitle)
    }

    @Test
    public fun testIconTypeIconField() {
        // GIVEN complication data of the ICON type created by the Builder...
        val icon =
            Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(icon).build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(data.icon, icon)
    }

    @Test
    public fun testGetShortTextNotValidForIcon() {
        // GIVEN complication data of the ICON type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(Icon.createWithContentUri("someuri"))
                .build()

        // WHEN getShortText is called
        // THEN null is returned.
        Assert.assertNull(data.shortText)
    }

    @Test
    public fun testGetLongTextNotValidForIcon() {
        // GIVEN complication data of the ICON type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(Icon.createWithContentUri("someuri"))
                .build()

        // WHEN getLongText is called
        // THEN null is returned.
        Assert.assertNull(data.longText)
    }

    @Test
    public fun testSetShortTitleNotValidForIcon() {
        // GIVEN a complication data builder of the ICON type...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)

        // WHEN setShortTitle is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) {
            builder.setShortTitle(ComplicationText.plainText("title"))
        }
    }

    @Test
    public fun testShortTextFieldsIncludingIcon() {
        // GIVEN complication data of the SHORT_TEXT type created by the Builder, including an
        // icon...
        val icon =
            Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setIcon(icon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Truth.assertThat(data.shortText!!.getTextAt(mResources, 0))
            .isEqualTo("text")
        Assert.assertEquals(data.icon, icon)
    }

    @Test
    public fun testLongTextFieldsIncludingIcon() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder, including an
        // icon...
        val icon =
            Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                .setIcon(icon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(
            TEST_LONG_TITLE,
            data.longTitle!!.getTextAt(mResources, 0)
        )
        Assert.assertEquals(
            TEST_LONG_TEXT,
            data.longText!!.getTextAt(mResources, 0)
        )
        Assert.assertEquals(icon, data.icon)
    }

    @Test
    public fun testRangedValueFieldsIncludingIcon() {
        // GIVEN complication data of the RANGED_VALUE type created by the Builder, including an
        // icon...
        val icon =
            Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(57f)
                .setRangedMinValue(5f)
                .setRangedMaxValue(150f)
                .setIcon(icon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(data.rangedValue, 57f, 0f)
        Assert.assertEquals(data.rangedMinValue, 5f, 0f)
        Assert.assertEquals(data.rangedMaxValue, 150f, 0f)
        Assert.assertEquals(data.icon, icon)
    }

    @Test
    public fun testGetLongTextNotValidForEmpty() {
        // GIVEN complication data of the EMPTY type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_EMPTY)
                .build()

        // WHEN getLongText is called
        // THEN null is returned.
        Assert.assertNull(data.longText)
    }

    @Test
    public fun testSetShortTextNotValidForEmpty() {
        // GIVEN a complication data builder of the EMPTY type...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_EMPTY)

        // WHEN setShortText is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) {
            builder.setShortText(
                ComplicationText.plainText(
                    "text"
                )
            )
        }
    }

    @Test
    public fun testSetIconNotValidForEmpty() {
        // GIVEN a complication data builder of the EMPTY type...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_EMPTY)

        // WHEN setIcon is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) {
            builder.setIcon(Icon.createWithContentUri("uri"))
        }
    }

    @Test
    public fun testGetLongTextNotValidForNotConfigured() {
        // GIVEN complication data of the NOT_CONFIGURED type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_NOT_CONFIGURED)
                .build()

        // WHEN getLongText is called
        // THEN null is returned.
        Assert.assertNull(data.longText)
    }

    @Test
    public fun testSetShortTextNotValidForNotConfigured() {
        // GIVEN a complication data builder of the NOT_CONFIGURED type...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_NOT_CONFIGURED)

        // WHEN setShortText is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) {
            builder.setShortText(ComplicationText.plainText("text"))
        }
    }

    @Test
    public fun testSetIconNotValidForNotConfigured() {
        // GIVEN a complication data builder of the NOT_CONFIGURED type...
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_NOT_CONFIGURED)

        // WHEN setIcon is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) {
            builder.setIcon(Icon.createWithContentUri("uri"))
        }
    }

    @Test
    public fun testLongTextFieldsIncludingSmallImage() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder, including an
        // icon...
        val icon =
            Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                .setSmallImage(icon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(
            TEST_LONG_TITLE,
            data.longTitle!!.getTextAt(mResources, 0)
        )
        Assert.assertEquals(
            TEST_LONG_TEXT,
            data.longText!!.getTextAt(mResources, 0)
        )
        Assert.assertEquals(icon, data.smallImage)
    }

    @Test
    public fun testGetShortTextNotValidForSmallImage() {
        // GIVEN complication data of the SMALL IMAGE type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(Icon.createWithContentUri("someuri"))
                .build()

        // WHEN getShortText is called
        // THEN null is returned.
        Assert.assertNull(data.shortText)
    }

    @Test
    public fun testSmallImageTypeSmallImageField() {
        // GIVEN complication data of the SMALL IMAGE type created by the Builder...
        val icon =
            Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(icon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(data.smallImage, icon)
    }

    @Test
    public fun testSmallImageTypeSmallImageFieldAndBurnInSmallImage() {
        // GIVEN complication data of the SMALL IMAGE type created by the Builder...
        val icon = Icon.createWithContentUri("someuri")
        val burnInIcon = Icon.createWithContentUri("burnInSomeuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(icon)
                .setBurnInProtectionSmallImage(burnInIcon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(data.smallImage, icon)
        Assert.assertEquals(data.burnInProtectionSmallImage, burnInIcon)
    }

    @Test
    public fun testLargeImageTypeLargeImageField() {
        // GIVEN complication data of the LARGE IMAGE type created by the Builder...
        val icon = Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                .setLargeImage(icon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(data.largeImage, icon)
    }

    @Test
    public fun testGetLongTextNotValidForLargeImage() {
        // GIVEN complication data of the LARGE IMAGE type...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                .setLargeImage(Icon.createWithContentUri("someuri"))
                .build()

        // WHEN getLongText is called
        // THEN null is returned.
        Assert.assertNull(data.longText)
    }

    @Test
    public fun testSmallImageRequiresSmallImage() {
        // GIVEN a complication data builder of the SMALL IMAGE type...
        val builder = ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)

        // WHEN build is called and setSmallImage has not been called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testLargeImageRequiresLargeImage() {
        // GIVEN a complication data builder of the LARGE IMAGE type...
        val builder = ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)

        // WHEN build is called and setLargeImage has not been called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testIconTypeTapActionField() {
        // GIVEN complication data of the ICON type created by the Builder...
        val icon = Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(icon)
                .setTapAction(mPendingIntent)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(data.tapAction, mPendingIntent)
    }

    @Test
    public fun testNoStartEndAlwaysActive() {
        // GIVEN complication data with no start or end time specified...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setShortTitle(ComplicationText.plainText("title"))
                .build()

        // WHEN isActive is called for any time
        // THEN result is true
        Assert.assertTrue(data.isActiveAt(1000))
        Assert.assertTrue(data.isActiveAt(100000000000L))
        Assert.assertTrue(data.isActiveAt(1000000000))
        Assert.assertTrue(data.isActiveAt(100000000000000000L))
        Assert.assertTrue(data.isActiveAt(999999999))
    }

    @Test
    public fun testStartNoEnd() {
        // GIVEN complication data with a start time but no end time specified...
        val startTime: Long = 1000000
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setShortTitle(ComplicationText.plainText("title"))
                .setStartDateTimeMillis(startTime)
                .build()

        // WHEN isActive is called for times before startTime
        // THEN result is false
        Assert.assertFalse(data.isActiveAt(startTime - 1))
        Assert.assertFalse(data.isActiveAt(startTime - 1000))
        Assert.assertFalse(data.isActiveAt(0))

        // WHEN isActive is called for times at or after startTime
        // THEN result is true
        Assert.assertTrue(data.isActiveAt(startTime))
        Assert.assertTrue(data.isActiveAt(startTime + 1))
        Assert.assertTrue(data.isActiveAt(startTime + 1000000000))
        Assert.assertTrue(data.isActiveAt(startTime + 100000000000000L))
    }

    @Test
    public fun testEndNoStart() {
        // GIVEN complication data with an end time but no start time specified...
        val endTime = 1000000000000L
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setShortTitle(ComplicationText.plainText("title"))
                .setEndDateTimeMillis(endTime)
                .build()

        // WHEN isActive is called for times after endTime
        // THEN result is false
        Assert.assertFalse(data.isActiveAt(endTime + 1))
        Assert.assertFalse(data.isActiveAt(endTime + 1000))
        Assert.assertFalse(data.isActiveAt(endTime + 100000000000000L))

        // WHEN isActive is called for times before endTime
        // THEN result is true
        Assert.assertTrue(data.isActiveAt(endTime - 1))
        Assert.assertTrue(data.isActiveAt(endTime - 10))
        Assert.assertTrue(data.isActiveAt(endTime - 100000000))
        Assert.assertTrue(data.isActiveAt(endTime - 10000000000L))
    }

    @Test
    public fun testStartAndEnd() {
        // GIVEN complication data with a start time and end time specified...
        val startTime: Long = 1000000
        val endTime = 1000000000000L
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setShortTitle(ComplicationText.plainText("title"))
                .setStartDateTimeMillis(startTime)
                .setEndDateTimeMillis(endTime)
                .build()

        // WHEN isActive is called for times before startTime
        // THEN result is false
        Assert.assertFalse(data.isActiveAt(startTime - 1))
        Assert.assertFalse(data.isActiveAt(startTime - 1000))
        Assert.assertFalse(data.isActiveAt(0))

        // WHEN isActive is called for times after endTime
        // THEN result is false
        Assert.assertFalse(data.isActiveAt(endTime + 1))
        Assert.assertFalse(data.isActiveAt(endTime + 1000))
        Assert.assertFalse(data.isActiveAt(endTime + 100000000000000L))

        // WHEN isActive is called for times between the start and end time
        // THEN result is true
        Assert.assertTrue(data.isActiveAt(endTime - 1))
        Assert.assertTrue(data.isActiveAt((startTime + endTime) / 2))
        Assert.assertTrue(data.isActiveAt(endTime - 100000000))
        Assert.assertTrue(data.isActiveAt(startTime + 1000))
    }

    @Test
    public fun testSmallImageDefaultImageStyle() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, but with
        // the image style not set...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(Icon.createWithContentUri("someuri"))
                .build()

        // WHEN the image style is retrieved
        // THEN the default of IMAGE_STYLE_PHOTO is returned.
        Truth.assertThat(data.smallImageStyle)
            .isEqualTo(ComplicationData.IMAGE_STYLE_PHOTO)
    }

    @Test
    public fun testLongTextDefaultImageStyle() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder, but with
        // the image style not set...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                .setSmallImage(Icon.createWithContentUri("someuri"))
                .build()

        // WHEN the image style is retrieved
        // THEN the default of IMAGE_STYLE_PHOTO is returned.
        Truth.assertThat(data.smallImageStyle)
            .isEqualTo(ComplicationData.IMAGE_STYLE_PHOTO)
    }

    @Test
    public fun testSmallImageSetImageStyle() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(Icon.createWithContentUri("someuri"))
                .setSmallImageStyle(ComplicationData.IMAGE_STYLE_ICON)
                .build()

        // WHEN the image style is retrieved
        // THEN the chosen style is returned.
        Truth.assertThat(data.smallImageStyle)
            .isEqualTo(ComplicationData.IMAGE_STYLE_ICON)
    }

    @Test
    public fun testSettingFieldThenSettingNullClearsField() {
        // GIVEN a complication data builder of the SHORT_TEXT type, including an
        // icon...
        val icon =
            Icon.createWithContentUri("someuri")
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setIcon(icon)

        // WHEN the icon is subsequently set to null, and the data is built
        builder.setIcon(null)
        val data = builder.build()

        // THEN the icon is null in the resulting data.
        Assert.assertNull(data.icon)
    }

    @Test
    public fun testSetBurnInIconWithoutIconThrowsExceptionOnBuild() {
        // GIVEN a complication data builder of the SHORT_TEXT type, with a burn-in-protection icon
        // specified, but no regular icon
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setBurnInProtectionIcon(Icon.createWithContentUri("someuri"))

        // WHEN build is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testSetBurnInSmallImageWithoutSmallImageThrowsExceptionOnBuild() {
        // GIVEN a complication data builder of the SMALL_IMAGE type, with a burn-in-protection
        // small
        // image specified, but no regular small image
        val builder =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setBurnInProtectionSmallImage(Icon.createWithContentUri("someuri"))

        // WHEN build is called
        // THEN IllegalStateException is thrown.
        assertThrows(IllegalStateException::class.java) { builder.build() }
    }

    @Test
    public fun testBothIconFields() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        val regularIcon =
            Icon.createWithContentUri("regular-icon")
        val burnInIcon =
            Icon.createWithContentUri("burn-in-icon")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(regularIcon)
                .setBurnInProtectionIcon(burnInIcon)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(regularIcon, data.icon)
        Assert.assertEquals(burnInIcon, data.burnInProtectionIcon)
    }

    @Test
    public fun testBothSmallImageFields() {
        // GIVEN complication data of the SMALL_IMAGE type, with both icon fields populated
        val regularSmallImage =
            Icon.createWithContentUri("regular-small-image")
        val burnInSmallImage =
            Icon.createWithContentUri("burn-in-small-image")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(regularSmallImage)
                .setBurnInProtectionSmallImage(burnInSmallImage)
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(regularSmallImage, data.smallImage)
        Assert.assertEquals(burnInSmallImage, data.burnInProtectionSmallImage)
    }

    @Test
    public fun testSmallImageWithContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(Icon.createWithContentUri("someuri"))
                .setSmallImageStyle(ComplicationData.IMAGE_STYLE_ICON)
                .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                .build()
        Assert.assertEquals(
            TEST_CONTENT_DESCRIPTION,
            data.contentDescription!!.getTextAt(mResources, 0)
        )
    }

    @Test
    public fun testLargeImageWithContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                .setLargeImage(Icon.createWithContentUri("someuri"))
                .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                .build()
        Assert.assertEquals(
            TEST_CONTENT_DESCRIPTION,
            data.contentDescription!!.getTextAt(mResources, 0)
        )
    }

    @Test
    public fun testIconWithNullContentDescription() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        val regularIcon =
            Icon.createWithContentUri("regular-icon")
        val burnInIcon =
            Icon.createWithContentUri("burn-in-icon")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(regularIcon)
                .setBurnInProtectionIcon(burnInIcon)
                .build()
        Assert.assertNull(data.contentDescription)
    }

    @Test
    public fun testLongTextWithContentDescription() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertEquals(
            TEST_LONG_TITLE,
            data.longTitle!!.getTextAt(mResources, 0)
        )
        Assert.assertEquals(
            TEST_LONG_TEXT,
            data.longText!!.getTextAt(mResources, 0)
        )
        Assert.assertEquals(
            TEST_CONTENT_DESCRIPTION,
            data.contentDescription!!.getTextAt(mResources, 0)
        )
    }

    @Test
    public fun testLongTextWithNullContentDescription() {
        // GIVEN complication data of the LONG_TEXT type created by the Builder...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(ComplicationText.plainText(TEST_LONG_TITLE))
                .setLongText(ComplicationText.plainText(TEST_LONG_TEXT))
                .build()

        // WHEN the relevant getters are called on the resulting data
        // THEN the correct values are returned.
        Assert.assertNull(data.contentDescription)
    }

    @Test
    public fun testSmallImageWithImageContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(Icon.createWithContentUri("someuri"))
                .setSmallImageStyle(ComplicationData.IMAGE_STYLE_ICON)
                .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                .build()
        Assert.assertEquals(
            TEST_CONTENT_DESCRIPTION,
            data.contentDescription!!.getTextAt(mResources, 0)
        )
    }

    @Test
    public fun testLargeImageWithImageContentDescription() {
        // GIVEN complication data of the SMALL_IMAGE type created by the Builder, with
        // image style set...
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                .setLargeImage(Icon.createWithContentUri("someuri"))
                .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                .build()
        Assert.assertEquals(
            TEST_CONTENT_DESCRIPTION,
            data.contentDescription!!.getTextAt(mResources, 0)
        )
    }

    @Test
    public fun testIconWithContentDescription() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        val regularIcon = Icon.createWithContentUri("regular-icon")
        val burnInIcon = Icon.createWithContentUri("burn-in-icon")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(regularIcon)
                .setBurnInProtectionIcon(burnInIcon)
                .setContentDescription(ComplicationText.plainText(TEST_CONTENT_DESCRIPTION))
                .build()
        Assert.assertEquals(
            TEST_CONTENT_DESCRIPTION,
            data.contentDescription!!.getTextAt(mResources, 0)
        )
    }

    @Test
    public fun testIconWithEmptyContentDescription() {
        // GIVEN complication data of the SHORT_TEXT type, with both icon fields populated
        val regularIcon = Icon.createWithContentUri("regular-icon")
        val burnInIcon = Icon.createWithContentUri("burn-in-icon")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(regularIcon)
                .setBurnInProtectionIcon(burnInIcon)
                .setContentDescription(ComplicationText.plainText(""))
                .build()
        Assert.assertEquals(
            0,
            data.contentDescription!!.getTextAt(mResources, 0).length.toLong()
        )
    }

    @Test
    public fun iconNotTimeDependent() {
        val icon = Icon.createWithContentUri("someuri")
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(icon).build()
        Truth.assertThat(data.isTimeDependent).isFalse()
    }

    @Test
    public fun plainShortTextNotTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("text"))
                .setShortTitle(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isFalse()
    }

    @Test
    public fun timeFormatShortTextIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(TimeFormatBuilder().setFormat("mm").build())
                .setShortTitle(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    @Test
    public fun timeFormatShortTitleIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(TimeFormatBuilder().setFormat("mm").build())
                .setShortText(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    @Test
    public fun timeDifferenceShortTextIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(
                    TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(100000)
                        .setReferencePeriodEndMillis(200000)
                        .build()
                )
                .setShortTitle(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    @Test
    public fun timeDifferenceShortTitleIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(
                    TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(100000)
                        .setReferencePeriodEndMillis(200000)
                        .build()
                )
                .setShortText(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    @Test
    public fun plainLongTextNotTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongText(ComplicationText.plainText("text"))
                .setLongTitle(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isFalse()
    }

    @Test
    public fun timeFormatLongTextIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongText(TimeFormatBuilder().setFormat("mm").build())
                .setLongTitle(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    @Test
    public fun timeFormatLongTitleIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(TimeFormatBuilder().setFormat("mm").build())
                .setLongText(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    @Test
    public fun timeDifferenceLongTextIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongText(
                    TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(100000)
                        .setReferencePeriodEndMillis(200000)
                        .build()
                )
                .setLongTitle(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    @Test
    public fun timeDifferenceLongTitleIsTimeDependent() {
        val data =
            ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                .setLongTitle(
                    TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(100000)
                        .setReferencePeriodEndMillis(200000)
                        .build()
                )
                .setLongText(ComplicationText.plainText("hello"))
                .build()
        Truth.assertThat(data.isTimeDependent).isTrue()
    }

    private companion object {
        private val TEST_CONTENT_DESCRIPTION: CharSequence = "This is a test description!"
        private const val TEST_LONG_TITLE = "what a long title such a long title"
        private const val TEST_LONG_TEXT = "such long text so much text omg"
    }
}
