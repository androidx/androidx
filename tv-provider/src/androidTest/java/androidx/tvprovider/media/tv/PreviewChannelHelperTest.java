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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Test that {@link PreviewChannelHelper} can perform CRUD operations on
 * {@link PreviewChannel PreviewChannels} and {@link PreviewProgram PreviewPrograms} correctly.
 * All of the following tests involve the system content provider.
 */
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4.class)
public class PreviewChannelHelperTest {


    private Context mContext;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * taken from {@link PreviewProgram}
     */
    private static PreviewProgram.Builder createFullyPopulatedPreviewProgram(long channelId) {
        return new PreviewProgram.Builder()
                .setTitle("Google")
                .setInternalProviderId("ID-4321")
                .setChannelId(channelId)
                .setWeight(100)
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setTransient(false)
                .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3)
                .setThumbnailAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(TvContractCompat.PreviewPrograms.AVAILABILITY_AVAILABLE)
                .setStartingPrice("12.99 USD")
                .setOfferPrice("4.99 USD")
                .setReleaseDate("1997")
                .setItemCount(3)
                .setLive(false)
                .setInteractionType(TvContractCompat.PreviewPrograms.INTERACTION_TYPE_LIKES)
                .setInteractionCount(10200)
                .setAuthor("author_name")
                .setReviewRatingStyle(TvContractCompat.PreviewPrograms.REVIEW_RATING_STYLE_STARS)
                .setReviewRating("4.5")
                .setSearchable(false)
                .setThumbnailUri(Uri.parse("http://example.com/thumbnail.png"))
                .setAudioLanguages(new String[]{"eng", "kor"})
                .setCanonicalGenres(new String[]{TvContractCompat.Programs.Genres.MOVIES})
                .setContentRatings(new TvContentRating[]{
                        TvContentRating.createRating("com.android.tv", "US_TV", "US_TV_Y7")})
                .setDescription("This is a sample program")
                .setEpisodeNumber("Pilot", 0)
                .setEpisodeTitle("Hello World")
                .setLongDescription("This is a longer description than the previous description")
                .setPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setSeasonNumber("The Final Season", 7)
                .setSeasonTitle("The Final Season")
                .setVideoHeight(1080)
                .setVideoWidth(1920)
                .setInternalProviderFlag1(0x4)
                .setInternalProviderFlag2(0x3)
                .setInternalProviderFlag3(0x2)
                .setInternalProviderFlag4(0x1)
                .setBrowsable(true)
                .setContentId("CID-8642");
    }

    private static void compareProgram(PreviewProgram programA, PreviewProgram programB) {
        assertTrue(Arrays.equals(programA.getAudioLanguages(), programB.getAudioLanguages()));
        assertTrue(Arrays.deepEquals(programA.getCanonicalGenres(), programB.getCanonicalGenres()));
        assertEquals(programA.getChannelId(), programB.getChannelId());
        assertTrue(Arrays.deepEquals(programA.getContentRatings(), programB.getContentRatings()));
        assertEquals(programA.getDescription(), programB.getDescription());
        assertEquals(programA.getEpisodeNumber(), programB.getEpisodeNumber());
        assertEquals(programA.getEpisodeTitle(), programB.getEpisodeTitle());
        assertEquals(programA.getLongDescription(), programB.getLongDescription());
        assertEquals(programA.getPosterArtUri(), programB.getPosterArtUri());
        assertEquals(programA.getSeasonNumber(), programB.getSeasonNumber());
        assertEquals(programA.getThumbnailUri(), programB.getThumbnailUri());
        assertEquals(programA.getTitle(), programB.getTitle());
        assertEquals(programA.getVideoHeight(), programB.getVideoHeight());
        assertEquals(programA.getVideoWidth(), programB.getVideoWidth());
        assertEquals(programA.isSearchable(), programB.isSearchable());
        assertEquals(programA.getInternalProviderFlag1(), programB.getInternalProviderFlag1());
        assertEquals(programA.getInternalProviderFlag2(), programB.getInternalProviderFlag2());
        assertEquals(programA.getInternalProviderFlag3(), programB.getInternalProviderFlag3());
        assertEquals(programA.getInternalProviderFlag4(), programB.getInternalProviderFlag4());
        assertTrue(Objects.equals(programA.getSeasonTitle(), programB.getSeasonTitle()));
        assertEquals(programA.getInternalProviderId(), programB.getInternalProviderId());
        assertEquals(programA.getPreviewVideoUri(), programB.getPreviewVideoUri());
        assertEquals(programA.getLastPlaybackPositionMillis(),
                programB.getLastPlaybackPositionMillis());
        assertEquals(programA.getDurationMillis(), programB.getDurationMillis());
        assertEquals(programA.getIntentUri(), programB.getIntentUri());
        assertEquals(programA.getWeight(), programB.getWeight());
        assertEquals(programA.isTransient(), programB.isTransient());
        assertEquals(programA.getType(), programB.getType());
        assertEquals(programA.getPosterArtAspectRatio(), programB.getPosterArtAspectRatio());
        assertEquals(programA.getThumbnailAspectRatio(), programB.getThumbnailAspectRatio());
        assertEquals(programA.getLogoUri(), programB.getLogoUri());
        assertEquals(programA.getAvailability(), programB.getAvailability());
        assertEquals(programA.getStartingPrice(), programB.getStartingPrice());
        assertEquals(programA.getOfferPrice(), programB.getOfferPrice());
        assertEquals(programA.getReleaseDate(), programB.getReleaseDate());
        assertEquals(programA.getItemCount(), programB.getItemCount());
        assertEquals(programA.isLive(), programB.isLive());
        assertEquals(programA.getInteractionType(), programB.getInteractionType());
        assertEquals(programA.getInteractionCount(), programB.getInteractionCount());
        assertEquals(programA.getAuthor(), programB.getAuthor());
        assertEquals(programA.getReviewRatingStyle(), programB.getReviewRatingStyle());
        assertEquals(programA.getReviewRating(), programB.getReviewRating());
        assertEquals(programA.getContentId(), programB.getContentId());
    }

    private static WatchNextProgram.Builder createFullyPopulatedWatchNextProgram() {
        return new WatchNextProgram.Builder()
                .setTitle("Google")
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setTransient(false)
                .setType(TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
                .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setPosterArtAspectRatio(TvContractCompat.WatchNextPrograms.ASPECT_RATIO_2_3)
                .setThumbnailAspectRatio(TvContractCompat.WatchNextPrograms.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(TvContractCompat.WatchNextPrograms.AVAILABILITY_AVAILABLE)
                .setStartingPrice("12.99 USD")
                .setOfferPrice("4.99 USD")
                .setReleaseDate("1997")
                .setItemCount(3)
                .setLive(false)
                .setInteractionType(TvContractCompat.WatchNextPrograms.INTERACTION_TYPE_LIKES)
                .setInteractionCount(10200)
                .setAuthor("author_name")
                .setReviewRatingStyle(TvContractCompat.WatchNextPrograms.REVIEW_RATING_STYLE_STARS)
                .setReviewRating("4.5")
                .setSearchable(false)
                .setThumbnailUri(Uri.parse("http://example.com/thumbnail.png"))
                .setAudioLanguages(new String[]{"eng", "kor"})
                .setCanonicalGenres(new String[]{TvContractCompat.Programs.Genres.MOVIES})
                .setContentRatings(new TvContentRating[]{
                        TvContentRating.createRating("com.android.tv", "US_TV", "US_TV_Y7")})
                .setDescription("This is a sample program")
                .setEpisodeNumber("Pilot", 0)
                .setEpisodeTitle("Hello World")
                .setLongDescription("This is a longer description than the previous description")
                .setPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setSeasonNumber("The Final Season", 7)
                .setSeasonTitle("The Final Season")
                .setVideoHeight(1080)
                .setVideoWidth(1920)
                .setInternalProviderFlag1(0x4)
                .setInternalProviderFlag2(0x3)
                .setInternalProviderFlag3(0x2)
                .setInternalProviderFlag4(0x1)
                .setBrowsable(true)
                .setContentId("CID-8442");
    }

    private static void compareProgram(WatchNextProgram programA, WatchNextProgram programB) {
        assertTrue(Arrays.equals(programA.getAudioLanguages(), programB.getAudioLanguages()));
        assertTrue(Arrays.deepEquals(programA.getCanonicalGenres(), programB.getCanonicalGenres()));
        assertTrue(Arrays.deepEquals(programA.getContentRatings(), programB.getContentRatings()));
        assertEquals(programA.getDescription(), programB.getDescription());
        assertEquals(programA.getEpisodeNumber(), programB.getEpisodeNumber());
        assertEquals(programA.getEpisodeTitle(), programB.getEpisodeTitle());
        assertEquals(programA.getLongDescription(), programB.getLongDescription());
        assertEquals(programA.getPosterArtUri(), programB.getPosterArtUri());
        assertEquals(programA.getSeasonNumber(), programB.getSeasonNumber());
        assertEquals(programA.getThumbnailUri(), programB.getThumbnailUri());
        assertEquals(programA.getTitle(), programB.getTitle());
        assertEquals(programA.getVideoHeight(), programB.getVideoHeight());
        assertEquals(programA.getVideoWidth(), programB.getVideoWidth());
        assertEquals(programA.isSearchable(), programB.isSearchable());
        assertEquals(programA.getInternalProviderFlag1(), programB.getInternalProviderFlag1());
        assertEquals(programA.getInternalProviderFlag2(), programB.getInternalProviderFlag2());
        assertEquals(programA.getInternalProviderFlag3(), programB.getInternalProviderFlag3());
        assertEquals(programA.getInternalProviderFlag4(), programB.getInternalProviderFlag4());
        assertTrue(Objects.equals(programA.getSeasonTitle(), programB.getSeasonTitle()));
        assertEquals(programA.getInternalProviderId(), programB.getInternalProviderId());
        assertEquals(programA.getPreviewVideoUri(), programB.getPreviewVideoUri());
        assertEquals(programA.getLastPlaybackPositionMillis(),
                programB.getLastPlaybackPositionMillis());
        assertEquals(programA.getDurationMillis(), programB.getDurationMillis());
        assertEquals(programA.getIntentUri(), programB.getIntentUri());
        assertEquals(programA.isTransient(), programB.isTransient());
        assertEquals(programA.getType(), programB.getType());
        assertEquals(programA.getWatchNextType(), programB.getWatchNextType());
        assertEquals(programA.getPosterArtAspectRatio(), programB.getPosterArtAspectRatio());
        assertEquals(programA.getThumbnailAspectRatio(), programB.getThumbnailAspectRatio());
        assertEquals(programA.getLogoUri(), programB.getLogoUri());
        assertEquals(programA.getAvailability(), programB.getAvailability());
        assertEquals(programA.getStartingPrice(), programB.getStartingPrice());
        assertEquals(programA.getOfferPrice(), programB.getOfferPrice());
        assertEquals(programA.getReleaseDate(), programB.getReleaseDate());
        assertEquals(programA.getItemCount(), programB.getItemCount());
        assertEquals(programA.isLive(), programB.isLive());
        assertEquals(programA.getInteractionType(), programB.getInteractionType());
        assertEquals(programA.getInteractionCount(), programB.getInteractionCount());
        assertEquals(programA.getAuthor(), programB.getAuthor());
        assertEquals(programA.getReviewRatingStyle(), programB.getReviewRatingStyle());
        assertEquals(programA.getReviewRating(), programB.getReviewRating());
        assertEquals(programA.getContentId(), programB.getContentId());
    }

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getContext();

    }

    @After
    public void tearDown() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        mContext.getContentResolver().delete(
                TvContractCompat.Channels.CONTENT_URI, null, null);
        mContext = null;
    }

    /**
     * Test CR of CRUD
     * Test that the PreviewChannelHelper can correctly create and read preview channels.
     */
    @Test
    public void testPreviewChannelCreation() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder builder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishDefaultChannel(builder.build());
        PreviewChannel channelFromTvProvider = getPreviewChannel(helper, channelId);
        assertTrue(channelsEqual(builder.build(), channelFromTvProvider));
    }

    @Test
    public void testLogoRequiredForChannelCreation() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder builder = createFullyPopulatedPreviewChannel();
        builder.setLogo(Uri.parse("bogus"));
        thrown.expect(IOException.class);
        helper.publishDefaultChannel(builder.build());
        List<PreviewChannel> channels = helper.getAllChannels();
        assertEquals(0, channels.size());
    }

    /**
     * Test CR of CRUD
     * Test that the PreviewChannelHelper can correctly create and read preview channels, when
     * internalProviderId is null.
     */
    @Test
    public void testPreviewChannelCreationWithNullProviderId() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannel.Builder builder = createFullyPopulatedPreviewChannel();
        builder.setInternalProviderId(null);
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        long channelId = helper.publishChannel(builder.build());
        PreviewChannel channelFromTvProvider = getPreviewChannel(helper, channelId);
        assertTrue(channelsEqual(builder.build(), channelFromTvProvider));
    }

    /**
     * All this method is actually doing is
     * <pre>
     *
     *     PreviewChannel channelFromTvProvider = helper.getPreviewChannel(channelId);
     * </pre>
     * However, due to a known issue, when logo is persisted, the file status is not consistent
     * between openInputStream and openOutputStream. So as a workaround, a wait period is applied
     * to make sure that the logo file is written into the disk.
     */
    private PreviewChannel getPreviewChannel(PreviewChannelHelper helper,
            long channelId) {
        boolean logoReady = false;
        PreviewChannel channel = null;
        while (!logoReady) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            channel = helper.getPreviewChannel(channelId);
            logoReady = null != channel.getLogo(mContext);
        }
        return channel;
    }

    /**
     * Test CR of CRUD
     * Test that all published preview channels can be read at once.
     */
    @Test
    public void testAllPublishedChannelsRead() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder builder = createFullyPopulatedPreviewChannel();
        builder.setInternalProviderId("1");
        helper.publishChannel(builder.build());
        builder.setInternalProviderId("11");
        helper.publishChannel(builder.build());
        builder.setInternalProviderId("111");
        helper.publishChannel(builder.build());
        builder.setInternalProviderId("1111");
        helper.publishChannel(builder.build());
        List<PreviewChannel> allChannels = helper.getAllChannels();
        assertEquals(4, allChannels.size());
    }

    /**
     * Test UR of CRUD
     * Test that the PreviewChannelHelper can correctly update and read preview channels.
     */
    @Test
    public void testPreviewChannelUpdate() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder builder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishChannel(builder.build());
        PreviewChannel channelFromTvProvider = getPreviewChannel(helper, channelId);
        PreviewChannel channel = builder.build();
        assertTrue(channelsEqual(channel, channelFromTvProvider));

        PreviewChannel patch = new PreviewChannel.Builder()
                .setDisplayName(channel.getDisplayName())
                .setAppLinkIntentUri(channel.getAppLinkIntentUri())
                .setDescription("Patch description").build();
        helper.updatePreviewChannel(channelId, patch);
        channelFromTvProvider = helper.getPreviewChannel(channelId);
        assertFalse(channelsEqual(channel, channelFromTvProvider));
        assertEquals(channelFromTvProvider.getDescription(), "Patch description");
    }

    /**
     * Tests that data is not being updated unnecessarily
     */
    @Test
    public void testDefensiveUpdatePreviewChannel() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        final int[] channelUpdateCount = {0};
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext) {
            @Override
            protected void updatePreviewChannelInternal(long channelId, PreviewChannel channel) {
                channelUpdateCount[0]++;
            }
        };
        PreviewChannel.Builder builder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishChannel(builder.build());
        PreviewChannel fromProvider = helper.getPreviewChannel(channelId);
        channelsEqual(builder.build(), fromProvider);
        helper.updatePreviewChannel(channelId, builder.build());
        assertEquals(0, channelUpdateCount[0]);

        final Uri uri = Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(Intent.URI_INTENT_SCHEME));
        PreviewChannel channel = new PreviewChannel.Builder()
                .setDisplayName("Test Display Name Udpate")
                .setDescription("Test Preview Channel Description")
                .setAppLinkIntentUri(uri).build();

        helper.updatePreviewChannel(channelId, channel);
        assertEquals(1, channelUpdateCount[0]);
    }

    @Test
    public void testPreviewResolverChannelDeletion() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder builder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishChannel(builder.build());
        PreviewChannel channelFromTvProvider = getPreviewChannel(helper, channelId);
        assertTrue(channelsEqual(builder.build(), channelFromTvProvider));

        helper.deletePreviewChannel(channelId);
        channelFromTvProvider = helper.getPreviewChannel(channelId);
        assertNull(channelFromTvProvider);
    }

    @Test
    public void testPreviewProgramCreation() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder channelBuilder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishChannel(channelBuilder.build());
        PreviewProgram program = createFullyPopulatedPreviewProgram(channelId).build();
        long programId = helper.publishPreviewProgram(program);
        PreviewProgram programFromProvider = helper.getPreviewProgram(programId);
        compareProgram(program, programFromProvider);
    }

    @Test
    public void testPreviewProgramUpdate() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder channelBuilder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishChannel(channelBuilder.build());
        PreviewProgram.Builder programBuilder = createFullyPopulatedPreviewProgram(channelId);
        long programId = helper.publishPreviewProgram(programBuilder.build());

        programBuilder.setReleaseDate("2000");

        helper.updatePreviewProgram(programId, programBuilder.build());
        PreviewProgram programFromProvider = helper.getPreviewProgram(programId);
        compareProgram(programBuilder.build(), programFromProvider);
    }

    /**
     * Tests that data is not being updated unnecessarily
     */
    @Test
    public void testDefensivePreviewProgramUpdateRequests() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        final int[] programUpdateCount = {0};
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext) {

            @Override
            public void updatePreviewProgramInternal(long programId, PreviewProgram upgrade) {
                programUpdateCount[0]++;
            }
        };
        PreviewChannel.Builder channelBuilder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishChannel(channelBuilder.build());
        PreviewProgram.Builder programBuilder = createFullyPopulatedPreviewProgram(channelId);
        long programId = helper.publishPreviewProgram(programBuilder.build());
        PreviewProgram programFromProvider = helper.getPreviewProgram(programId);
        compareProgram(programBuilder.build(), programFromProvider);
        helper.updatePreviewProgram(programId, programBuilder.build());
        assertEquals(0, programUpdateCount[0]);
        programBuilder.setDurationMillis(61 * 1000);
        helper.updatePreviewProgram(programId, programBuilder.build());
        assertEquals(1, programUpdateCount[0]);
    }

    @Test
    public void testDeletePreviewProgram() throws IOException {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        PreviewChannel.Builder channelBuilder = createFullyPopulatedPreviewChannel();
        long channelId = helper.publishChannel(channelBuilder.build());
        PreviewProgram.Builder programBuilder = createFullyPopulatedPreviewProgram(channelId);
        long programId = helper.publishPreviewProgram(programBuilder.build());

        helper.deletePreviewProgram(programId);
        PreviewProgram programFromProvider = helper.getPreviewProgram(programId);
        assertNull(programFromProvider);
    }

    @Test
    public void testWatchNextProgramCreation() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        WatchNextProgram program = createFullyPopulatedWatchNextProgram().build();
        long programId = helper.publishWatchNextProgram(program);
        WatchNextProgram programFromProvider = helper.getWatchNextProgram(programId);
        compareProgram(program, programFromProvider);
    }

    @Test
    public void testUpdateWatchNextProgram() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext);
        WatchNextProgram.Builder builder = createFullyPopulatedWatchNextProgram();
        long programId = helper.publishWatchNextProgram(builder.build());
        builder.setOfferPrice("10.99 USD");
        helper.updateWatchNextProgram(builder.build(), programId);

        WatchNextProgram fromProvider = helper.getWatchNextProgram(programId);
        compareProgram(builder.build(), fromProvider);
    }

    /**
     * Tests that data is not being updated unnecessarily
     */
    @Test
    public void testDefensiveUpdateWatchNextProgram() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        final int[] programUpdateCount = {0};
        PreviewChannelHelper helper = new PreviewChannelHelper(mContext) {
            @Override
            protected void updateWatchNextProgram(long programId, WatchNextProgram upgrade) {
                programUpdateCount[0]++;
            }
        };
        WatchNextProgram.Builder builder = createFullyPopulatedWatchNextProgram();
        long programId = helper.publishWatchNextProgram(builder.build());
        WatchNextProgram fromProvider = helper.getWatchNextProgram(programId);
        compareProgram(builder.build(), fromProvider);
        helper.updateWatchNextProgram(builder.build(), programId);
        assertEquals(0, programUpdateCount[0]);
        builder.setReleaseDate("2000");
        helper.updateWatchNextProgram(builder.build(), programId);
        assertEquals(1, programUpdateCount[0]);
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
                && (channelA.getInternalProviderId() == null
                && channelB.getInternalProviderId() == null
                || channelA.getInternalProviderId().equals(channelB.getInternalProviderId()))
                && (null != channelA.getLogo(mContext) && null != channelB.getLogo(
                mContext))
                && Arrays.equals(channelA.getInternalProviderDataByteArray(),
                channelB.getInternalProviderDataByteArray());
        return result;
    }

    public PreviewChannel.Builder createFullyPopulatedPreviewChannel() {
        Bitmap logo = BitmapFactory.decodeResource(mContext.getResources(),
                androidx.tvprovider.test.R.drawable.test_icon);
        assertNotNull(logo);
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
                .setInternalProviderData("Test byte array".getBytes())
                .setLogo(logo);
    }
}
