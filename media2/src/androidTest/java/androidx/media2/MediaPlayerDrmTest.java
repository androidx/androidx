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
package androidx.media2;

import static android.content.Context.KEYGUARD_SERVICE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.Manifest;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.media2.MediaPlayer.DrmInfo;
import androidx.media2.MediaPlayer.DrmResult;
import androidx.media2.SessionPlayer2.PlayerResult;
import androidx.media2.TestUtils.Monitor;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DRM tests which use MediaPlayer to play audio or video.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@Suppress // Disabled as it 100% fails b/79682973
public class MediaPlayerDrmTest {
    private static final int STREAM_RETRIES = 3;

    private Monitor mOnVideoSizeChangedCalled = new Monitor();
    private Monitor mOnPlaybackCompleted = new Monitor();
    private Monitor mOnDrmInfoCalled = new Monitor();
    private Monitor mOnDrmPreparedCalled = new Monitor();

    private Context mContext;
    private Resources mResources;

    private MediaPlayer mPlayer = null;
    private MediaStubActivity mActivity;
    private Instrumentation mInstrumentation;

    private ExecutorService mExecutor;
    private MediaPlayer.PlayerCallback mECb = null;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public ActivityTestRule<MediaStubActivity> mActivityRule =
            new ActivityTestRule<>(MediaStubActivity.class);
    public PowerManager.WakeLock mScreenLock;
    private KeyguardManager mKeyguardManager;

    @Before
    @CallSuper
    public void setUp() throws Throwable {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mKeyguardManager = (KeyguardManager)
                mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
        mActivity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Keep screen on while testing.
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mActivity.setTurnScreenOn(true);
                mActivity.setShowWhenLocked(true);
                mKeyguardManager.requestDismissKeyguard(mActivity, null);
            }
        });
        mInstrumentation.waitForIdleSync();

        try {
            mActivityRule.runOnUiThread(new Runnable() {
                public void run() {
                    mPlayer = new MediaPlayer(mActivity);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }

        mContext = mInstrumentation.getTargetContext();
        mResources = mContext.getResources();

        mExecutor = Executors.newFixedThreadPool(2);
    }

    @After
    @CallSuper
    public void tearDown() throws Throwable {
        if (mPlayer != null) {
            mPlayer.close();
            mPlayer = null;
        }
        mExecutor.shutdown();
        mActivity = null;
    }

    private static class PrepareFailedException extends Exception {}

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Asset helpers

    private static Uri getUriFromFile(String path) {
        return Uri.fromFile(new File(getDownloadedPath(path)));
    }

    private static String getDownloadedPath(String fileName) {
        return getDownloadedFolder() + File.separator + fileName;
    }

    private static String getDownloadedFolder() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath();
    }

    private static final class Resolution {
        public final boolean isHD;
        public final int width;
        public final int height;

        Resolution(boolean isHD, int width, int height) {
            this.isHD = isHD;
            this.width = width;
            this.height = height;
        }
    }

    private static final Resolution RES_720P  = new Resolution(true, 1280,  720);
    private static final Resolution RES_AUDIO = new Resolution(false,   0,    0);


    // Assets

    private static final Uri CENC_AUDIO_URL = Uri.parse(
            "https://storage.googleapis.com/wvmedia/cenc/clearkey/car_cenc-20120827-8c-pssh.mp4");
    private static final Uri CENC_AUDIO_URL_DOWNLOADED = getUriFromFile("car_cenc-20120827-8c.mp4");

    private static final Uri CENC_VIDEO_URL = Uri.parse(
            "https://storage.googleapis.com/wvmedia/cenc/clearkey/car_cenc-20120827-88-pssh.mp4");
    private static final Uri CENC_VIDEO_URL_DOWNLOADED = getUriFromFile("car_cenc-20120827-88.mp4");


    // Tests

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V0_SYNC() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V0_SYNC_TEST);
    }

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V1_ASYNC() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V1_ASYNC_TEST);
    }

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V2_SYNC_CONFIG() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V2_SYNC_CONFIG_TEST);
    }

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V3_ASYNC_DRMPREPARED() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V3_ASYNC_DRMPREPARED_TEST);
    }

    // helpers

    private void stream(Uri uri, Resolution res, ModularDrmTestType testType) throws Exception {
        playModularDrmVideo(uri, res.width, res.height, testType);
    }

    private void download(Uri remote, Uri local, Resolution res, ModularDrmTestType testType)
            throws Exception {
        playModularDrmVideoDownload(remote, local, res.width, res.height, testType);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // Modular DRM

    private static final String TAG = "MediaPlayerDrmTest";

    private static final int PLAY_TIME_MS = 60 * 1000;
    private byte[] mKeySetId;
    private boolean mAudioOnly;

    private static final byte[] CLEAR_KEY_CENC = {
            (byte) 0x1a, (byte) 0x8a, (byte) 0x20, (byte) 0x95,
            (byte) 0xe4, (byte) 0xde, (byte) 0xb2, (byte) 0xd2,
            (byte) 0x9e, (byte) 0xc8, (byte) 0x16, (byte) 0xac,
            (byte) 0x7b, (byte) 0xae, (byte) 0x20, (byte) 0x82
            };

    private static final UUID CLEARKEY_SCHEME_UUID =
            new UUID(0x1077efecc0b24d02L, 0xace33c1e52e2fb4bL);

    final byte[] mClearKeyPssh = hexStringToByteArray(
            "0000003470737368"    // BMFF box header (4 bytes size + 'pssh')
            + "01000000"          // Full box header (version = 1 flags = 0)
            + "1077efecc0b24d02"  // SystemID
            + "ace33c1e52e2fb4b"
            + "00000001"          // Number of key ids
            + "60061e017e477e87"  // Key id
            + "7e57d00d1ed00d1e"
            + "00000000"          // Size of Data, must be zero
            );


    private enum ModularDrmTestType {
        V0_SYNC_TEST,
        V1_ASYNC_TEST,
        V2_SYNC_CONFIG_TEST,
        V3_ASYNC_DRMPREPARED_TEST,
        V4_SYNC_OFFLINE_KEY,
    }

    // TODO: After living on these tests for a while, we can consider grouping them based on
    // the asset such that each asset is downloaded once and played back with multiple tests.
    private void playModularDrmVideoDownload(Uri uri, Uri path, int width, int height,
            ModularDrmTestType testType) throws Exception {
        final long downloadTimeOutSeconds = 600;
        Log.i(TAG, "Downloading file:" + path);
        MediaDownloadManager mediaDownloadManager = new MediaDownloadManager(mContext);
        final long id = mediaDownloadManager.downloadFileWithRetries(
                uri, path, downloadTimeOutSeconds, STREAM_RETRIES);
        assertFalse("Download " + uri + " failed.", id == -1);
        Uri file = mediaDownloadManager.getUriForDownloadedFile(id);
        Log.i(TAG, "Downloaded file:" + path + " id:" + id + " uri:" + file);

        try {
            playModularDrmVideo(file, width, height, testType);
        } finally {
            mediaDownloadManager.removeFile(id);
        }
    }

    private void playModularDrmVideo(Uri uri, int width, int height,
            ModularDrmTestType testType) throws Exception {
        // Force gc for a clean start
        System.gc();

        playModularDrmVideoWithRetries(uri, width, height, PLAY_TIME_MS, testType);
    }

    private void playModularDrmVideoWithRetries(Uri file, Integer width, Integer height,
            int playTime, ModularDrmTestType testType) throws Exception {

        // first the synchronous variation
        boolean playedSuccessfully = false;
        for (int i = 0; i < STREAM_RETRIES; i++) {
            try {
                Log.v(TAG, "playVideoWithRetries(" + testType + ") try " + i);
                playLoadedModularDrmVideo(file, width, height, playTime, testType);

                playedSuccessfully = true;
                break;
            } catch (PrepareFailedException e) {
                // we can fail because of network issues, so try again
                Log.w(TAG, "playVideoWithRetries(" + testType + ") failed on try " + i
                        + ", trying playback again");
                mPlayer.reset();
            }
        }
        assertTrue("Stream did not play successfully after all attempts (syncDrmSetup)",
                playedSuccessfully);
    }

    /**
     * Play a video which has already been loaded with setMediaItem().
     * The DRM setup is performed synchronously.
     *
     * @param file media item
     * @param width width of the video to verify, or null to skip verification
     * @param height height of the video to verify, or null to skip verification
     * @param playTime length of time to play video, or 0 to play entire video
     * @param testType test type
     */
    private void playLoadedModularDrmVideo(final Uri file, final Integer width,
            final Integer height, int playTime, ModularDrmTestType testType) throws Exception {

        switch (testType) {
            case V0_SYNC_TEST:
            case V1_ASYNC_TEST:
            case V2_SYNC_CONFIG_TEST:
            case V3_ASYNC_DRMPREPARED_TEST:
                playLoadedModularDrmVideo_Generic(file, width, height, playTime, testType);
                break;

            case V4_SYNC_OFFLINE_KEY:
                playLoadedModularDrmVideo_V4_offlineKey(file, width, height, playTime);
                break;
        }
    }

    private void playLoadedModularDrmVideo_Generic(final Uri file, final Integer width,
            final Integer height, int playTime, ModularDrmTestType testType) throws Exception {

        final float volume = 0.5f;

        mAudioOnly = (width == 0);

        mECb = new MediaPlayer.PlayerCallback() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, MediaItem2 item, int w, int h) {
                    Log.v(TAG, "VideoSizeChanged" + " w:" + w + " h:" + h);
                    mOnVideoSizeChangedCalled.signal();
                }

                @Override
                public void onError(MediaPlayer mp, MediaItem2 item, int what, int extra) {
                    fail("Media player had error " + what + " playing video");
                }

                @Override
                public void onInfo(MediaPlayer mp, MediaItem2 item, int what, int extra) {
                    if (what == MediaPlayer.MEDIA_INFO_MEDIA_ITEM_END) {
                        Log.v(TAG, "playLoadedVideo: onInfo_PlaybackComplete");
                        mOnPlaybackCompleted.signal();
                    }
                }
            };

        mPlayer.registerPlayerCallback(mExecutor, mECb);
        Log.v(TAG, "playLoadedVideo: setMediaItem()");
        ListenableFuture<PlayerResult> future =
                mPlayer.setMediaItem(new UriMediaItem2.Builder(mContext, file).build());
        assertEquals(PlayerResult.RESULT_CODE_SUCCESS, future.get().getResultCode());

        SurfaceHolder surfaceHolder = mActivity.getSurfaceHolder();
        surfaceHolder.setKeepScreenOn(true);
        mPlayer.setSurface(surfaceHolder.getSurface());

        try {
            switch (testType) {
                case V0_SYNC_TEST:
                    preparePlayerAndDrm_V0_syncDrmSetup();
                    break;

                case V1_ASYNC_TEST:
                    preparePlayerAndDrm_V1_asyncDrmSetup();
                    break;

                case V2_SYNC_CONFIG_TEST:
                    preparePlayerAndDrm_V2_syncDrmSetupPlusConfig();
                    break;

                case V3_ASYNC_DRMPREPARED_TEST:
                    preparePlayerAndDrm_V3_asyncDrmSetupPlusDrmPreparedListener();
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new PrepareFailedException();
        }

        Log.v(TAG, "playLoadedVideo: play()");
        mPlayer.play();
        if (!mAudioOnly) {
            mOnVideoSizeChangedCalled.waitForSignal();
        }
        mPlayer.setPlayerVolume(volume);

        // waiting to complete
        if (playTime == 0) {
            Log.v(TAG, "playLoadedVideo: waiting for playback completion");
            mOnPlaybackCompleted.waitForSignal();
        } else {
            Log.v(TAG, "playLoadedVideo: waiting while playing for " + playTime);
            mOnPlaybackCompleted.waitForSignal(playTime);
        }

        try {
            Log.v(TAG, "playLoadedVideo: releaseDrm");
            mPlayer.releaseDrm();
        } catch (Exception e) {
            e.printStackTrace();
            throw new PrepareFailedException();
        }
    }

    private void preparePlayerAndDrm_V0_syncDrmSetup() throws Exception {
        Log.v(TAG, "preparePlayerAndDrm_V0: calling prepare()");
        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(PlayerResult.RESULT_CODE_SUCCESS, future.get().getResultCode());

        DrmInfo drmInfo = mPlayer.getDrmInfo();
        if (drmInfo != null) {
            setupDrm(drmInfo, true /* prepareDrm */, true /* synchronousNetworking */,
                    MediaDrm.KEY_TYPE_STREAMING);
            Log.v(TAG, "preparePlayerAndDrm_V0: setupDrm done!");
        }
    }

    private void preparePlayerAndDrm_V1_asyncDrmSetup()
            throws InterruptedException, ExecutionException {
        final AtomicBoolean asyncSetupDrmError = new AtomicBoolean(false);

        mPlayer.registerPlayerCallback(mExecutor, new MediaPlayer.PlayerCallback() {
            @Override
            public void onDrmInfo(MediaPlayer mp, MediaItem2 item, DrmInfo drmInfo) {
                Log.v(TAG, "preparePlayerAndDrm_V1: onDrmInfo" + drmInfo);

                // in the callback (async mode) so handling exceptions here
                try {
                    setupDrm(drmInfo, true /* prepareDrm */, true /* synchronousNetworking */,
                            MediaDrm.KEY_TYPE_STREAMING);
                } catch (Exception e) {
                    Log.v(TAG, "preparePlayerAndDrm_V1: setupDrm EXCEPTION " + e);
                    asyncSetupDrmError.set(true);
                }

                mOnDrmInfoCalled.signal();
                Log.v(TAG, "preparePlayerAndDrm_V1: onDrmInfo done!");
            }
        });

        Log.v(TAG, "preparePlayerAndDrm_V1: calling prepare()");
        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(PlayerResult.RESULT_CODE_SUCCESS, future.get().getResultCode());

        mOnDrmInfoCalled.waitForSignal();

        // to handle setupDrm error (async) in the main thread rather than the callback
        if (asyncSetupDrmError.get()) {
            fail("preparePlayerAndDrm_V1: setupDrm");
        }
    }

    private void preparePlayerAndDrm_V2_syncDrmSetupPlusConfig() throws Exception {
        mPlayer.setOnDrmConfigHelper(new MediaPlayer.OnDrmConfigHelper() {
            @Override
            public void onDrmConfig(MediaPlayer mp, MediaItem2 item) {
                String widevineSecurityLevel3 = "L3";
                String securityLevelProperty = "securityLevel";

                try {
                    String level = mp.getDrmPropertyString(securityLevelProperty);
                    Log.v(TAG, "preparePlayerAndDrm_V2: getDrmPropertyString: "
                            + securityLevelProperty + " -> " + level);
                    mp.setDrmPropertyString(securityLevelProperty, widevineSecurityLevel3);
                    level = mp.getDrmPropertyString(securityLevelProperty);
                    Log.v(TAG, "preparePlayerAndDrm_V2: getDrmPropertyString: "
                            + securityLevelProperty + " -> " + level);
                } catch (MediaPlayer.NoDrmSchemeException e) {
                    Log.v(TAG, "preparePlayerAndDrm_V2: NoDrmSchemeException");
                } catch (Exception e) {
                    Log.v(TAG, "preparePlayerAndDrm_V2: onDrmConfig EXCEPTION " + e);
                }
            }
        });

        Log.v(TAG, "preparePlayerAndDrm_V2: calling prepare()");
        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(PlayerResult.RESULT_CODE_SUCCESS, future.get().getResultCode());

        DrmInfo drmInfo = mPlayer.getDrmInfo();
        if (drmInfo != null) {
            setupDrm(drmInfo, true /* prepareDrm */, true /* synchronousNetworking */,
                    MediaDrm.KEY_TYPE_STREAMING);
            Log.v(TAG, "preparePlayerAndDrm_V2: setupDrm done!");
        }
    }

    private void preparePlayerAndDrm_V3_asyncDrmSetupPlusDrmPreparedListener()
            throws InterruptedException, ExecutionException {
        final AtomicBoolean asyncSetupDrmError = new AtomicBoolean(false);

        mPlayer.registerPlayerCallback(mExecutor, new MediaPlayer.PlayerCallback() {
            @Override
            public void onDrmInfo(MediaPlayer mp, MediaItem2 item, DrmInfo drmInfo) {
                Log.v(TAG, "preparePlayerAndDrm_V3: onDrmInfo" + drmInfo);

                // DRM preperation
                List<UUID> supportedSchemes = drmInfo.getSupportedSchemes();
                if (supportedSchemes.isEmpty()) {
                    Log.e(TAG, "preparePlayerAndDrm_V3: onDrmInfo: No supportedSchemes");
                    asyncSetupDrmError.set(true);
                    mOnDrmInfoCalled.signal();
                    // we won't call prepareDrm anymore but need to get passed the wait
                    mOnDrmPreparedCalled.signal();
                    return;
                }

                // setting up with the first supported UUID
                // instead of supportedSchemes[0] in GTS
                UUID drmScheme = CLEARKEY_SCHEME_UUID;
                Log.d(TAG, "preparePlayerAndDrm_V3: onDrmInfo: selected " + drmScheme);

                Log.v(TAG, "preparePlayerAndDrm_V3: onDrmInfo: calling prepareDrm");
                final ListenableFuture<DrmResult> future = mp.prepareDrm(drmScheme);
                Log.v(TAG, "preparePlayerAndDrm_V3: onDrmInfo: called prepareDrm");
                future.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DrmResult result = future.get();
                            Log.v(TAG, "preparePlayerAndDrm_V3: prepareDrm status: "
                                    + result.getResultCode());

                            assertTrue("preparePlayerAndDrm_V3: onDrmPrepared did not succeed",
                                    result.getResultCode() == DrmResult.RESULT_CODE_SUCCESS);

                            DrmInfo drmInfo = mPlayer.getDrmInfo();

                            // in the callback (async mode) so handling exceptions here
                            try {
                                setupDrm(drmInfo, false /* prepareDrm */,
                                        true /* synchronousNetworking */,
                                        MediaDrm.KEY_TYPE_STREAMING);
                            } catch (Exception e) {
                                Log.v(TAG, "preparePlayerAndDrm_V3: setupDrm EXCEPTION ", e);
                                asyncSetupDrmError.set(true);
                            }

                            mOnDrmPreparedCalled.signal();
                            Log.v(TAG, "preparePlayerAndDrm_V3: onDrmPrepared done!");
                        } catch (ExecutionException | InterruptedException e) {
                            Log.e(TAG, "preparePlayerAndDrm_V3: onDrmInfo: prepareDrm exception ",
                                    e);
                            asyncSetupDrmError.set(true);
                            mOnDrmInfoCalled.signal();
                            // need to get passed the wait
                            mOnDrmPreparedCalled.signal();
                        }
                    }
                }, mExecutor);

                mOnDrmInfoCalled.signal();
                Log.v(TAG, "preparePlayerAndDrm_V3: onDrmInfo done!");
            }
        });

        Log.v(TAG, "preparePlayerAndDrm_V3: calling prepare()");
        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(PlayerResult.RESULT_CODE_SUCCESS, future.get().getResultCode());

        // Unlike v3, onDrmPrepared is not synced to onPrepared b/c of its own thread handler
        mOnDrmPreparedCalled.waitForSignal();

        // to handle setupDrm error (async) in the main thread rather than the callback
        if (asyncSetupDrmError.get()) {
            fail("preparePlayerAndDrm_V3: setupDrm");
        }
    }

    private void playLoadedModularDrmVideo_V4_offlineKey(final Uri file, final Integer width,
            final Integer height, int playTime) throws Exception {
        final float volume = 0.5f;

        mAudioOnly = (width == 0);

        SurfaceHolder surfaceHolder = mActivity.getSurfaceHolder();
        Log.v(TAG, "playLoadedModularDrmVideo_V4_offlineKey: setSurface " + surfaceHolder);
        mPlayer.setSurface(surfaceHolder.getSurface());
        surfaceHolder.setKeepScreenOn(true);

        DrmInfo drmInfo = null;

        for (int round = 0; round < 2; round++) {
            boolean keyRequestRound = (round == 0);
            boolean restoreRound = (round == 1);
            Log.v(TAG, "playLoadedVideo: round " + round);

            try {
                mPlayer.registerPlayerCallback(mExecutor, mECb);

                Log.v(TAG, "playLoadedVideo: setMediaItem()");
                mPlayer.setMediaItem(
                        new UriMediaItem2.Builder(mContext, file).build());

                Log.v(TAG, "playLoadedVideo: prepare()");
                ListenableFuture<PlayerResult> future = mPlayer.prepare();
                assertEquals(PlayerResult.RESULT_CODE_SUCCESS, future.get().getResultCode());

                // but preparing the DRM every time with proper key request type
                drmInfo = mPlayer.getDrmInfo();
                if (drmInfo != null) {
                    if (keyRequestRound) {
                        // asking for offline keys
                        setupDrm(drmInfo, true /* prepareDrm */, true /* synchronousNetworking */,
                                 MediaDrm.KEY_TYPE_OFFLINE);
                    } else if (restoreRound) {
                        setupDrmRestore(drmInfo, true /* prepareDrm */);
                    } else {
                        fail("preparePlayer: unexpected round " + round);
                    }
                    Log.v(TAG, "preparePlayer: setupDrm done!");
                }

            } catch (IOException e) {
                e.printStackTrace();
                throw new PrepareFailedException();
            }

            Log.v(TAG, "playLoadedVideo: play()");
            mPlayer.play();
            if (!mAudioOnly) {
                mOnVideoSizeChangedCalled.waitForSignal();
            }
            mPlayer.setPlayerVolume(volume);

            // waiting to complete
            if (playTime == 0) {
                Log.v(TAG, "playLoadedVideo: waiting for playback completion");
                mOnPlaybackCompleted.waitForSignal();
            } else {
                Log.v(TAG, "playLoadedVideo: waiting while playing for " + playTime);
                mOnPlaybackCompleted.waitForSignal(playTime);
            }

            try {
                if (drmInfo != null) {
                    if (restoreRound) {
                        // releasing the offline key
                        setupDrm(null /* drmInfo */, false /* prepareDrm */,
                                 true /* synchronousNetworking */, MediaDrm.KEY_TYPE_RELEASE);
                        Log.v(TAG, "playLoadedVideo: released offline keys");
                    }

                    Log.v(TAG, "playLoadedVideo: releaseDrm");
                    mPlayer.releaseDrm();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new PrepareFailedException();
            }

            if (keyRequestRound) {
                mOnVideoSizeChangedCalled.reset();
                mOnPlaybackCompleted.reset();
                final int sleepBetweenRounds = 1000;
                Thread.sleep(sleepBetweenRounds);

                Log.v(TAG, "playLoadedVideo: reset");
                mPlayer.reset();
            }
        }  // for
    }

    // Converts a BMFF PSSH initData to a raw cenc initData
    private byte[] makeCencPSSH(UUID uuid, byte[] bmffPsshData) {
        byte[] pssh_header = new byte[] { (byte) 'p', (byte) 's', (byte) 's', (byte) 'h' };
        byte[] pssh_version = new byte[] { 1, 0, 0, 0 };
        int boxSizeByteCount = 4;
        int uuidByteCount = 16;
        int dataSizeByteCount = 4;
        // Per "W3C cenc Initialization Data Format" document:
        // box size + 'pssh' + version + uuid + payload + size of data
        int boxSize = boxSizeByteCount + pssh_header.length + pssh_version.length
                + uuidByteCount + bmffPsshData.length + dataSizeByteCount;
        int dataSize = 0;

        // the default write is big-endian, i.e., network byte order
        ByteBuffer rawPssh = ByteBuffer.allocate(boxSize);
        rawPssh.putInt(boxSize);
        rawPssh.put(pssh_header);
        rawPssh.put(pssh_version);
        rawPssh.putLong(uuid.getMostSignificantBits());
        rawPssh.putLong(uuid.getLeastSignificantBits());
        rawPssh.put(bmffPsshData);
        rawPssh.putInt(dataSize);

        return rawPssh.array();
    }

    /*
     * Sets up the DRM for the first DRM scheme from the supported list.
     *
     * @param drmInfo DRM info of the source
     * @param prepareDrm whether prepareDrm should be called
     * @param synchronousNetworking whether the network operation of key request/response will
     *        be performed synchronously
     */
    private void setupDrm(DrmInfo drmInfo, boolean prepareDrm, boolean synchronousNetworking,
            int keyType) throws Exception {
        Log.d(TAG, "setupDrm: drmInfo: " + drmInfo + " prepareDrm: " + prepareDrm
                + " synchronousNetworking: " + synchronousNetworking);
        try {
            byte[] initData = null;
            String mime = null;
            String keyTypeStr = "Unexpected";

            switch (keyType) {
                case MediaDrm.KEY_TYPE_STREAMING:
                case MediaDrm.KEY_TYPE_OFFLINE:
                    // DRM preparation
                    List<UUID> supportedSchemes = drmInfo.getSupportedSchemes();
                    if (supportedSchemes.isEmpty()) {
                        fail("setupDrm: No supportedSchemes");
                    }

                    // instead of supportedSchemes[0] in GTS
                    UUID drmScheme = CLEARKEY_SCHEME_UUID;
                    Log.d(TAG, "setupDrm: selected " + drmScheme);

                    if (prepareDrm) {
                        ListenableFuture<DrmResult> future = mPlayer.prepareDrm(drmScheme);
                        assertEquals(DrmResult.RESULT_CODE_SUCCESS, future.get().getResultCode());
                    }

                    byte[] psshData = drmInfo.getPssh().get(drmScheme);
                    // diverging from GTS
                    if (psshData == null) {
                        initData = mClearKeyPssh;
                        Log.d(TAG, "setupDrm: CLEARKEY scheme not found in PSSH."
                                + " Using default data.");
                    } else {
                        // Can skip conversion if ClearKey adds support for BMFF initData b/64863112
                        initData = makeCencPSSH(CLEARKEY_SCHEME_UUID, psshData);
                    }
                    Log.d(TAG, "setupDrm: initData[" + drmScheme + "]: "
                            + Arrays.toString(initData));

                    // diverging from GTS
                    mime = "cenc";

                    keyTypeStr = (keyType == MediaDrm.KEY_TYPE_STREAMING)
                            ? "KEY_TYPE_STREAMING" : "KEY_TYPE_OFFLINE";
                    break;

                case MediaDrm.KEY_TYPE_RELEASE:
                    if (mKeySetId == null) {
                        fail("setupDrm: KEY_TYPE_RELEASE requires a valid keySetId.");
                    }
                    keyTypeStr = "KEY_TYPE_RELEASE";
                    break;

                default:
                    fail("setupDrm: Unexpected keyType " + keyType);
            }

            final MediaDrm.KeyRequest request = mPlayer.getDrmKeyRequest(
                    (keyType == MediaDrm.KEY_TYPE_RELEASE) ? mKeySetId : null,
                    initData,
                    mime,
                    keyType,
                    null /* optionalKeyRequestParameters */
                    );

            Log.d(TAG, "setupDrm: mPlayer.getDrmKeyRequest(" + keyTypeStr
                    + ") request -> " + request);

            // diverging from GTS
            byte[][] clearKeys = new byte[][] { CLEAR_KEY_CENC };
            byte[] response = createKeysResponse(request, clearKeys);

            // null is returned when the response is for a streaming or release request.
            byte[] keySetId = mPlayer.provideDrmKeyResponse(
                    (keyType == MediaDrm.KEY_TYPE_RELEASE) ? mKeySetId : null,
                    response);
            Log.d(TAG, "setupDrm: provideDrmKeyResponse -> " + Arrays.toString(keySetId));
            // storing offline key for a later restore
            mKeySetId = (keyType == MediaDrm.KEY_TYPE_OFFLINE) ? keySetId : null;

        } catch (MediaPlayer.NoDrmSchemeException e) {
            Log.d(TAG, "setupDrm: NoDrmSchemeException");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            Log.d(TAG, "setupDrm: Exception " + e);
            e.printStackTrace();
            throw e;
        }
    } // setupDrm

    private void setupDrmRestore(DrmInfo drmInfo, boolean prepareDrm) throws Exception {
        Log.d(TAG, "setupDrmRestore: drmInfo: " + drmInfo + " prepareDrm: " + prepareDrm);
        try {
            if (prepareDrm) {
                // DRM preparation
                List<UUID> supportedSchemes = drmInfo.getSupportedSchemes();
                if (supportedSchemes.isEmpty()) {
                    fail("setupDrmRestore: No supportedSchemes");
                }

                // instead of supportedSchemes[0] in GTS
                UUID drmScheme = CLEARKEY_SCHEME_UUID;
                Log.d(TAG, "setupDrmRestore: selected " + drmScheme);

                ListenableFuture<DrmResult> future = mPlayer.prepareDrm(drmScheme);
                assertEquals(DrmResult.RESULT_CODE_SUCCESS, future.get().getResultCode());
            }

            if (mKeySetId == null) {
                fail("setupDrmRestore: Offline key has not been setup.");
            }

            mPlayer.restoreDrmKeys(mKeySetId);

        } catch (MediaPlayer.NoDrmSchemeException e) {
            Log.v(TAG, "setupDrmRestore: NoDrmSchemeException");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            Log.v(TAG, "setupDrmRestore: Exception " + e);
            e.printStackTrace();
            throw e;
        }
    } // setupDrmRestore

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Diverging from GTS

    // Clearkey helpers

    /**
     * Convert a hex string into byte array.
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Extracts key ids from the pssh blob returned by getDrmKeyRequest() and
     * places it in keyIds.
     * keyRequestBlob format (section 5.1.3.1):
     * https://dvcs.w3.org/hg/html-media/raw-file/default/encrypted-media/encrypted-media.html
     *
     * @return size of keyIds vector that contains the key ids, 0 for error
     */
    private int getKeyIds(byte[] keyRequestBlob, Vector<String> keyIds) {
        if (0 == keyRequestBlob.length || keyIds == null) {
            Log.e(TAG, "getKeyIds: Empty keyRequestBlob or null keyIds.");
            return 0;
        }

        String jsonLicenseRequest = new String(keyRequestBlob);
        keyIds.clear();

        try {
            JSONObject license = new JSONObject(jsonLicenseRequest);
            Log.v(TAG, "getKeyIds: license: " + license);
            final JSONArray ids = license.getJSONArray("kids");
            Log.v(TAG, "getKeyIds: ids: " + ids);
            for (int i = 0; i < ids.length(); ++i) {
                keyIds.add(ids.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON license = " + jsonLicenseRequest);
            return 0;
        }
        return keyIds.size();
    }

    /**
     * Creates the JSON Web Key string.
     *
     * @return JSON Web Key string.
     */
    private String createJsonWebKeySet(Vector<String> keyIds, Vector<String> keys) {
        String jwkSet = "{\"keys\":[";
        for (int i = 0; i < keyIds.size(); ++i) {
            String id = new String(keyIds.get(i).getBytes(Charset.forName("UTF-8")));
            String key = new String(keys.get(i).getBytes(Charset.forName("UTF-8")));

            jwkSet += "{\"kty\":\"oct\",\"kid\":\"" + id + "\",\"k\":\"" + key + "\"}";
        }
        jwkSet += "]}";
        return jwkSet;
    }

    /**
     * Retrieves clear key ids from KeyRequest and creates the response in place.
     */
    private byte[] createKeysResponse(MediaDrm.KeyRequest keyRequest, byte[][] clearKeys) {

        Vector<String> keyIds = new Vector<String>();
        if (0 == getKeyIds(keyRequest.getData(), keyIds)) {
            Log.e(TAG, "No key ids found in initData");
            return null;
        }

        if (clearKeys.length != keyIds.size()) {
            Log.e(TAG, "Mismatch number of key ids and keys: ids="
                    + keyIds.size() + ", keys=" + clearKeys.length);
            return null;
        }

        // Base64 encodes clearkeys. Keys are known to the application.
        Vector<String> keys = new Vector<String>();
        for (int i = 0; i < clearKeys.length; ++i) {
            String clearKey = Base64.encodeToString(clearKeys[i],
                    Base64.NO_PADDING | Base64.NO_WRAP);
            keys.add(clearKey);
        }

        String jwkSet = createJsonWebKeySet(keyIds, keys);
        byte[] jsonResponse = jwkSet.getBytes(Charset.forName("UTF-8"));

        return jsonResponse;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Playback/download helpers

    private static class MediaDownloadManager {
        private static final String TAG = "MediaDownloadManager";

        private final Context mContext;
        private final DownloadManager mDownloadManager;

        MediaDownloadManager(Context context) {
            mContext = context;
            mDownloadManager =
                    (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        }

        public long downloadFileWithRetries(Uri uri, Uri file, long timeout, int retries)
                throws Exception {
            long id = -1;
            for (int i = 0; i < retries; i++) {
                try {
                    id = downloadFile(uri, file, timeout);
                    if (id != -1) {
                        break;
                    }
                } catch (Exception e) {
                    removeFile(id);
                    Log.w(TAG, "Download failed " + i + " times ");
                }
            }
            return id;
        }

        public long downloadFile(Uri uri, Uri file, long timeout) throws Exception {
            Log.i(TAG, "uri:" + uri + " file:" + file + " wait:" + timeout + " Secs");
            final DownloadReceiver receiver = new DownloadReceiver();
            long id = -1;
            try {
                IntentFilter intentFilter =
                        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                mContext.registerReceiver(receiver, intentFilter);

                Request request = new Request(uri);
                request.setDestinationUri(file);
                id = mDownloadManager.enqueue(request);
                Log.i(TAG, "enqueue:" + id);

                receiver.waitForDownloadComplete(timeout, id);
            } finally {
                mContext.unregisterReceiver(receiver);
            }
            return id;
        }

        public void removeFile(long id) {
            Log.i(TAG, "removeFile:" + id);
            mDownloadManager.remove(id);
        }

        public Uri getUriForDownloadedFile(long id) {
            return mDownloadManager.getUriForDownloadedFile(id);
        }

        private final class DownloadReceiver extends BroadcastReceiver {
            private HashSet<Long> mCompleteIds = new HashSet<>();

            @Override
            public void onReceive(Context context, Intent intent) {
                synchronized (mCompleteIds) {
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                        mCompleteIds.add(
                                intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1));
                        mCompleteIds.notifyAll();
                    }
                }
            }

            private boolean isCompleteLocked(long... ids) {
                for (long id : ids) {
                    if (!mCompleteIds.contains(id)) {
                        return false;
                    }
                }
                return true;
            }

            public void waitForDownloadComplete(long timeoutSecs, long... waitForIds)
                    throws InterruptedException {
                if (waitForIds.length == 0) {
                    throw new IllegalArgumentException("Missing IDs to wait for");
                }

                final long startTime = SystemClock.elapsedRealtime();
                do {
                    synchronized (mCompleteIds) {
                        mCompleteIds.wait(1000);
                        if (isCompleteLocked(waitForIds)) {
                            return;
                        }
                    }
                } while ((SystemClock.elapsedRealtime() - startTime) < timeoutSecs * 1000);

                throw new InterruptedException(
                        "Timeout waiting for IDs " + Arrays.toString(waitForIds)
                        + "; received " + mCompleteIds.toString()
                        + ".  Make sure you have WiFi or some other connectivity for this test.");
            }
        }

    }  // MediaDownloadManager

}
