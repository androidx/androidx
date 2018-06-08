/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.tvprovider.media.tv;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;

import androidx.tvprovider.media.tv.TvContractCompat.Channels;
import androidx.tvprovider.test.R;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

/**
 * Tests that PreviewChannels can be created correctly. Additional test, the ones involving the
 * System Content Provider, are inside {@link PreviewChannelHelperTest}
 */
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4.class)
public class PreviewChannelTest extends TestCase {

    private static final String TAG = "PreviewChannelTest";
    private Context mContext;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mContext = InstrumentationRegistry.getContext();
    }

    @After
    public void tearDown() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        ContentResolver resolver = mContext.getContentResolver();
        resolver.delete(Channels.CONTENT_URI, null, null);
        mContext = null;
    }

    @Test
    public void testEmptyPreviewChannel() throws Exception {
        PreviewChannel.Builder builder = new PreviewChannel.Builder();
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Need channel name. "
                + "Use method setDisplayName(String) to set it.");
        PreviewChannel emptyChannel = builder.build();
    }

    @Test
    public void testPartiallyPopulatedPreviewChannel() {
        final String displayName = "Google";
        final String description = "This is a test preview channel";
        final Uri uri = Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(Intent.URI_INTENT_SCHEME));

        PreviewChannel channel = new PreviewChannel.Builder()
                .setDisplayName(displayName)
                .setDescription(description)
                .setAppLinkIntentUri(uri)
                .setLogo(createLogo()).build();

        assertEquals(displayName, channel.getDisplayName());
        assertEquals(description, channel.getDescription());
        assertEquals(uri, channel.getAppLinkIntentUri());
        assertNotNull(channel.getLogo(mContext));
        assertNull(channel.getPackageName());
        assertNull(channel.getInternalProviderDataByteArray());
        assertNull(channel.getInternalProviderFlag1());
        assertNull(channel.getInternalProviderFlag2());
        assertNull(channel.getInternalProviderFlag3());
        assertNull(channel.getInternalProviderFlag4());
        assertNull(channel.getInternalProviderId());
        assertFalse(channel.isBrowsable());
    }

    @Test
    public void testFullyPopulatedPreviewChannel() {
        //test cloning and database I/O
        PreviewChannel channel = createFullyPopulatedPreviewChannel();
        PreviewChannel clonedChannelFromCursor = PreviewChannel.fromCursor(
                getPreviewChannelCursor(channel.toContentValues()));
        assertTrue(channelsEqual(channel, clonedChannelFromCursor));

        PreviewChannel clonedChannelFromBuilder = new PreviewChannel.Builder(channel).build();
        assertTrue(channelsEqual(channel, clonedChannelFromBuilder));
    }

    @Test
    public void testChannelEquals() {
        assertEquals(createFullyPopulatedPreviewChannel(), createFullyPopulatedPreviewChannel());
    }

    private boolean channelsEqual(PreviewChannel channelA, PreviewChannel channelB) {
        boolean result = channelA.getDisplayName().equals(channelB.getDisplayName())
                && channelA.getType().equals(channelB.getType())
                && channelA.getAppLinkIntentUri().equals(channelB.getAppLinkIntentUri())
                && channelA.getDescription().equals(channelB.getDescription())
                && channelA.getPackageName().equals(channelB.getPackageName())
                && channelA.getInternalProviderFlag1() == channelB.getInternalProviderFlag1()
                && channelA.getInternalProviderFlag2() == channelB.getInternalProviderFlag2()
                && channelA.getInternalProviderFlag3() == channelB.getInternalProviderFlag3()
                && channelA.getInternalProviderFlag4() == channelB.getInternalProviderFlag4()
                && channelA.getInternalProviderId().equals(channelB.getInternalProviderId())
                && Arrays.equals(channelA.getInternalProviderDataByteArray(),
                channelB.getInternalProviderDataByteArray());
        return result;
    }

    private PreviewChannel createFullyPopulatedPreviewChannel() {
        return new PreviewChannel.Builder()
                .setAppLinkIntent(new Intent())
                .setDescription("Test Preview Channel Description")
                .setDisplayName("Test Display Name")
                .setPackageName("androidx.tvprovider.media.tv.test")
                .setInternalProviderFlag1(0x1)
                .setInternalProviderFlag2(0x2)
                .setInternalProviderFlag3(0x3)
                .setInternalProviderFlag4(0x4)
                .setInternalProviderId("Test Internal provider id")
                .setLogo(createLogo()).build();
    }

    private Bitmap createLogo() {
        Bitmap logo = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.test_icon);
        assertNotNull(logo);
        return logo;
    }

    private MatrixCursor getPreviewChannelCursor(ContentValues contentValues) {
        MatrixCursor cursor = new MatrixCursor(PreviewChannel.Columns.PROJECTION);
        MatrixCursor.RowBuilder rowBuilder = cursor.newRow();
        for (String col : PreviewChannel.Columns.PROJECTION) {
            rowBuilder.add(col, contentValues.get(col));
        }
        cursor.moveToFirst();
        return cursor;
    }
}
