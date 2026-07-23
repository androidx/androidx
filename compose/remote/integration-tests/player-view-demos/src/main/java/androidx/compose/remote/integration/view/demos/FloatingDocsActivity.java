/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos;

import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.get;
import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.getp;
import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.getpc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.compose.remote.integration.view.demos.dsl.DslDataVizActivityRingsKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizBatteryRadialGaugeKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizCalendarHeatmapKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizHeartRateKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizHydrationWaveKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizMoonPhaseDialKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizSleepQualityRingsKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizStepProgressArcKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizStockSparklineKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizWeatherForecastKt;
import androidx.compose.remote.integration.view.demos.dsl.DslExampleTimerKt;
import androidx.compose.remote.integration.view.demos.dsl.DslMetronomeKt;
import androidx.compose.remote.integration.view.demos.dsl.DslModernShowcaseDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslPressureGaugeKt;
import androidx.compose.remote.integration.view.demos.dsl.DslStopwatchKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslClockKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslTickerKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dFinancialDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dIntervalDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dLineDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dMoreDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dPolarDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dRelationDemosKt;
import androidx.compose.remote.integration.view.demos.examples.Cube3DKt;
import androidx.compose.remote.integration.view.demos.examples.CubeSphereTransitionShader2Kt;
import androidx.compose.remote.integration.view.demos.examples.CubeSphereTransitionShaderKt;
import androidx.compose.remote.integration.view.demos.examples.DemoGraphsKt;
import androidx.compose.remote.integration.view.demos.examples.DemoMetalClockKt;
import androidx.compose.remote.integration.view.demos.examples.DemoParticlesKt;
import androidx.compose.remote.integration.view.demos.examples.LinearRegressionKt;
import androidx.compose.remote.integration.view.demos.examples.MClockKt;
import androidx.compose.remote.integration.view.demos.examples.ParticleDotsKt;
import androidx.compose.remote.integration.view.demos.examples.ParticleSphereKt;
import androidx.compose.remote.integration.view.demos.examples.Particles3Kt;
import androidx.compose.remote.integration.view.demos.examples.SphereTimeShaderKt;
import androidx.compose.remote.integration.view.demos.examples.old.ClockDemo1;
import androidx.compose.remote.integration.view.demos.examples.old.ClockDemo2;
import androidx.compose.remote.integration.view.demos.examples.old.DemoPathExpression;
import androidx.compose.remote.integration.view.demos.examples.old.FancyClocks;
import androidx.compose.remote.integration.view.demos.utils.RCDoc;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Landscape fullscreen swarm of 60 floating RemoteCompose documents.
 *
 * <p>The featured card's SwarmCard container is physically resized to the display pixel
 * dimensions so RemoteComposePlayer renders at native resolution (no GPU upscale).
 * On demotion the container is resized back to CARD_BASE, maintaining visual continuity
 * via a compensating scale factor so the card doesn't jump size.
 *
 * <p>Edit {@link #makeDocs()} to curate the document list.
 */
@SuppressLint("RestrictedApiAndroidX")
public class FloatingDocsActivity extends Activity {

    private static final int NUM_CARDS = 75;
    private static final int MAX_PLAYERS = 10;
    /** Base layout size for all swarm cards (px). */
    private static final int CARD_BASE = 256;
    private static final int THUMB_SIZE = 256;
    private static final float THRESH_PX = 128f;  // on-screen px: player vs image
    private static final float MIN_SCALE = 0.19f; // swarm min (~49 px on screen)
    private static final float MAX_SCALE = 0.58f; // swarm max (~149 px on screen)
    private static final float MIN_SPEED = 20f;
    private static final float MAX_SPEED = 80f;
    private static final float LERP_K = 5f;
    private static final long PROMOTE_MS = 4_000L;
    private static final long PROMOTE_GAP_MS = 1200L;

    /**
     * Physical pixel size of the featured card square (= min(w,h) * 0.9).
     * The featured SwarmCard is resized to this so the player renders at native res.
     */
    private int mFeatPx = CARD_BASE;

    private FrameLayout mRoot;
    private List<RCDoc> mDocs;
    private CardState[] mCards;

    private int mFeaturedIdx = -1;  // -1 during the 200 ms promotion gap
    private int mIncomingIdx = -1;  // card waiting to grow after the gap

    private final RemoteComposePlayer[] mPool = new RemoteComposePlayer[MAX_PLAYERS];
    private final int[] mPoolOwner = new int[MAX_PLAYERS];

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final ExecutorService mBg = Executors.newSingleThreadExecutor();
    private Choreographer.FrameCallback mAnim;
    private long mLastNs = -1;
    private boolean mReassignPending = false;
    private final Random mRng = new Random(13);
    private static final boolean TITLE_MAIN_WINDOW = false;

    // -----------------------------------------------------------------------
    // Curated document list — edit freely
    // -----------------------------------------------------------------------

    private static List<RCDoc> makeDocs() {
        ArrayList<RCDoc> d = new ArrayList<>();
        // Data Viz


        d.add(get("01dslClock", RcDslClockKt::dslClock));
        d.add(get("ActivityRings", DslDataVizActivityRingsKt::dslDemoActivityRings));
        d.add(get("BasicTimer", DslExampleTimerKt::dslBasicTimer));
        d.add(get("BatteryGauge", DslDataVizBatteryRadialGaugeKt::dslDemoBatteryRadialGauge));
        d.add(get("CalendarHeatmap", DslDataVizCalendarHeatmapKt::dslDemoCalendarHeatmap));
        d.add(get("DataVizActivityRings", DslDataVizActivityRingsKt::dslDemoActivityRings));
        d.add(get("HeartRate", DslDataVizHeartRateKt::dslDemoHeartRateTimeline));
        d.add(get("HydrationWave", DslDataVizHydrationWaveKt::dslDemoHydrationWave));
        d.add(get("Metronome", DslMetronomeKt::dslMetronomeDemo));
        d.add(get("ModernShowcase", DslModernShowcaseDemoKt::dslModernShowcaseDemo));
        d.add(get("MoonPhaseDial", DslDataVizMoonPhaseDialKt::dslDemoMoonPhaseDial));
        d.add(get("PressureGauge", DslPressureGaugeKt::dslDemoPressureGauge));
        d.add(get("RcClock", RcDslClockKt::dslClock));
        d.add(get("SleepRings", DslDataVizSleepQualityRingsKt::dslDemoSleepQualityRings));
        d.add(get("StepProgressArc", DslDataVizStepProgressArcKt::dslDemoStepProgressArc));
        d.add(get("StockSparkline", DslDataVizStockSparklineKt::dslDemoStockSparkline));
        d.add(get("Stopwatch", DslStopwatchKt::dslStopwatchDemo));
        d.add(get("Ticker", RcDslTickerKt::dslTicker));
        d.add(get("WeatherForecast", DslDataVizWeatherForecastKt::dslDemoWeatherForecast));
        d.add(get("animated", Graph2dLineDemosKt::graph2dLineAnimated));
        d.add(get("areaBasic", Graph2dLineDemosKt::graph2dAreaBasic));
        d.add(get("areaPercent", Graph2dLineDemosKt::graph2dAreaPercent));
        d.add(get("areaStacked", Graph2dLineDemosKt::graph2dAreaStacked));
        d.add(get("band", Graph2dIntervalDemosKt::graph2dBand));
        d.add(get("bar", Graph2dPolarDemosKt::graph2dPolarBar));
        d.add(get("basic", Graph2dLineDemosKt::graph2dLineBasic));
        d.add(get("bubble", Graph2dRelationDemosKt::graph2dBubble));
        d.add(get("bullet", Graph2dFinancialDemosKt::graph2dBullet));
        d.add(get("confusion", Graph2dMoreDemosKt::graph2dConfusion));
        d.add(get("connectedScatter", Graph2dRelationDemosKt::graph2dConnectedScatter));
        d.add(get("dark", Graph2dLineDemosKt::graph2dLineDark));
        d.add(get("donut", Graph2dRelationDemosKt::graph2dDonut));
        d.add(get("donut", Graph2dRelationDemosKt::graph2dDonut));
        d.add(get("errorBar", Graph2dIntervalDemosKt::graph2dErrorBar));
        d.add(get("fan", Graph2dIntervalDemosKt::graph2dFan));
        d.add(get("forest", Graph2dIntervalDemosKt::graph2dForest));
        d.add(get("function", Graph2dMoreDemosKt::graph2dFunction));
        d.add(get("funnel", Graph2dFinancialDemosKt::graph2dFunnel));
        d.add(get("funnel", Graph2dFinancialDemosKt::graph2dFunnel));
        d.add(get("Gantt", Graph2dMoreDemosKt::graph2dGantt));
        d.add(get("gauge", Graph2dPolarDemosKt::graph2dGauge));
        d.add(get("interactive", Graph2dLineDemosKt::graph2dLineInteractive));
        d.add(get("likert", Graph2dMoreDemosKt::graph2dLikert));
        d.add(get("multi", Graph2dLineDemosKt::graph2dLineMulti));
        d.add(get("pareto", Graph2dFinancialDemosKt::graph2dPareto));
        d.add(get("pie", Graph2dRelationDemosKt::graph2dPie));
        d.add(get("qq", Graph2dMoreDemosKt::graph2dQQ));
        d.add(get("quadrant", Graph2dMoreDemosKt::graph2dQuadrant));
        d.add(get("radar", Graph2dRelationDemosKt::graph2dRadar));
        d.add(get("radialBar", Graph2dPolarDemosKt::graph2dRadialBar));
        d.add(get("ridgeline", Graph2dMoreDemosKt::graph2dRidgeline));
        d.add(get("rose", Graph2dPolarDemosKt::graph2dRose));
        d.add(get("scatter", Graph2dRelationDemosKt::graph2dScatter));
        d.add(get("spline", Graph2dLineDemosKt::graph2dLineSpline));
        d.add(get("step", Graph2dLineDemosKt::graph2dLineStep));
        d.add(get("treemap", Graph2dMoreDemosKt::graph2dTreemap));
        d.add(get("waterfall", Graph2dFinancialDemosKt::graph2dWaterfall));
        d.add(getp("DemoGraphs2_og", DemoGraphsKt::demoGraphs2));
        d.add(getp("DemoPathExpression_pathTest3", DemoPathExpression::pathTest3));
        d.add(getp("clock1", ClockDemo1::clock1));
        d.add(getp("fancyClock1", FancyClocks::fancyClock1));
        d.add(getp("fancyClock2", ClockDemo2::fancyClock2));
        d.add(getp("fancyClock2", DemoMetalClockKt::fancyClock2));
        d.add(getp("fancyClock2", FancyClocks::fancyClock2));
        d.add(getp("fancyClock3", FancyClocks::fancyClock3));
        d.add(getp("maze", DemoParticlesKt::pmaze));
        d.add(getp("mclock", MClockKt::MClock));
        d.add(getpc("Cube3D", Cube3DKt::cube3d));
        d.add(getpc("CubeSphere",
                CubeSphereTransitionShaderKt::createCubeSphereTransitionShader));
        d.add(getpc("CubeSphere2",
                CubeSphereTransitionShader2Kt::createCubeSphereTransitionShader2));
        d.add(getpc("LinearRegression3", LinearRegressionKt::demoLinearRegression));
        d.add(getpc("ParticleDots", ParticleDotsKt::particleDots));
        d.add(getpc("ParticleDots2", ParticleDotsKt::particleDots2));
        d.add(getpc("SphereTimeShader", SphereTimeShaderKt::sphereTimeShader));
        d.add(getpc("particleSphere", ParticleSphereKt::particleSphere));
        d.add(getpc("warp", Particles3Kt::warp));

        System.out.println(">>>>>>>>>> " + d.size() + " demos");
        return d;
    }

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    static class CardState {
        int mDocIndex;
        float mCx, mCy, mVx, mVy, mScale, mTargetCx, mTargetCy, mTargetScale;
        boolean mFeatured;
        byte[] mRcBytes;
        boolean mBytesReady;
        Bitmap mThumb;
        boolean mThumbReady;
        SwarmCard mView;
        String mName;
    }

    class SwarmCard extends FrameLayout {
        final ImageView mThumbImg;
        RemoteComposePlayer mPlayer;
        private final Paint mFrame = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint mDim = new Paint();
        private float mDimAlpha;
        String mName;

        SwarmCard() {
            super(FloatingDocsActivity.this);
            mThumbImg = new ImageView(getContext());
            mThumbImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mThumbImg.setBackgroundColor(0xFF0D1117);
            // MATCH_PARENT so the child tracks whatever size the container is resized to
            addView(mThumbImg,
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mFrame.setStyle(Paint.Style.STROKE);
            mFrame.setColor(0xAA5555FF);
            mFrame.setStrokeWidth(3f);
            mText.setTextSize(128f);
            mText.setColor(0xFFFF3333);
            mDim.setColor(Color.BLACK);
        }

        void setDimAlpha(float alpha) {
            if (Math.abs(alpha - mDimAlpha) > 0.004f) {
                mDimAlpha = alpha;
                invalidate();
            }
        }

        void attach(RemoteComposePlayer p, byte[] bytes) {
            detach();
            mPlayer = p;
            addView(p, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mThumbImg.setVisibility(GONE);
            p.setDocument(bytes);
        }

        void detach() {
            if (mPlayer == null) return;
            removeView(mPlayer);
            mPlayer = null;
            mThumbImg.setVisibility(VISIBLE);
        }

        @Override
        protected void dispatchDraw(@NonNull Canvas c) {
            super.dispatchDraw(c);
            if (mDimAlpha > 0f) {
                mDim.setAlpha((int) (mDimAlpha * 255f));
                c.drawRect(0, 0, getWidth(), getHeight(), mDim);
            }
            c.drawRect(2, 2, getWidth() - 2f, getHeight() - 2f, mFrame);
            if (TITLE_MAIN_WINDOW && mName != null && getWidth() > 500) {
                c.drawText(mName, 10, getHeight() - 4, mText);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        boolean clearCache = false;
        if (clearCache) {
            File[] files = getCacheDir().listFiles();
            for (File file : files) {
                System.out.println(file.getName());
                file.delete();
            }
        }
        WindowInsetsControllerCompat wic =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        wic.hide(WindowInsetsCompat.Type.systemBars());
        wic.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        mRoot = new FrameLayout(this);
        mRoot.setBackgroundColor(Color.BLACK);
        setContentView(mRoot);

        for (int i = 0; i < MAX_PLAYERS; i++) {
            mPool[i] = new RemoteComposePlayer(this);
            mPool[i].setShaderControl((s) -> true);
            mPoolOwner[i] = -1;
        }

        mRoot.post(this::boot);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAnim != null) Choreographer.getInstance().removeFrameCallback(mAnim);
        mMain.removeCallbacksAndMessages(null);
        mBg.shutdownNow();
    }

    // -----------------------------------------------------------------------
    // Boot
    // -----------------------------------------------------------------------

    private void boot() {
        int w = mRoot.getWidth(), h = mRoot.getHeight();
        if (w == 0 || h == 0) {
            mRoot.post(this::boot);
            return;
        }

        // Physical pixel size the featured card container will be resized to.
        // RemoteComposePlayer measures to MATCH_PARENT, so it renders at this resolution.
        mFeatPx = (int) (Math.min(w, h) * 0.8f);

        mDocs = makeDocs();
        if (mDocs.isEmpty()) return;

        mCards = new CardState[NUM_CARDS];
        float margin = CARD_BASE * 0.3f;

        for (int i = 0; i < NUM_CARDS; i++) {
            CardState c = new CardState();
            c.mDocIndex = i % mDocs.size();


            c.mView = new SwarmCard();
            // All cards start at CARD_BASE; featured card is resized after the loop.
            mRoot.addView(c.mView, new FrameLayout.LayoutParams(CARD_BASE, CARD_BASE));

            if (i == 0) {
                c.mFeatured = true;
                c.mCx = w / 2f;
                c.mCy = h / 2f;
                c.mScale = 1f;
                c.mTargetCx = c.mCx;
                c.mTargetCy = c.mCy;
                c.mTargetScale = 1f;
                mFeaturedIdx = 0;
            } else {
                c.mFeatured = false;
                c.mCx = margin + mRng.nextFloat() * (w - 2 * margin);
                c.mCy = margin + mRng.nextFloat() * (h - 2 * margin);
                c.mScale = MIN_SCALE + mRng.nextFloat() * (MAX_SCALE - MIN_SCALE);
                c.mTargetCx = c.mCx;
                c.mTargetCy = c.mCy;
                c.mTargetScale = c.mScale;
                float ang = mRng.nextFloat() * (float) (Math.PI * 2);
                float spd = MIN_SPEED + mRng.nextFloat() * (MAX_SPEED - MIN_SPEED);
                c.mVx = (float) Math.cos(ang) * spd;
                c.mVy = (float) Math.sin(ang) * spd;
            }

            mCards[i] = c;

            final int ci = i;
            c.mView.setOnClickListener(v -> {
                if (!mCards[ci].mFeatured) promote(ci);
            });
        }

        // Resize the initial featured card to native resolution immediately.
        resizeCardView(mCards[0], mFeatPx);
        mRoot.bringChildToFront(mCards[0].mView);

        for (CardState c : mCards) placeCard(c);

        for (int i = 0; i < NUM_CARDS; i++) {
            final int ci = i;
            mBg.execute(() -> loadCard(ci));
        }

        startAnim(w, h);
        mMain.postDelayed(this::autoCycle, PROMOTE_MS);
    }

    // -----------------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------------

    private void loadCard(int ci) {
        CardState c = mCards[ci];
        RCDoc doc = mDocs.get(c.mDocIndex);

        String key = sanitize(doc.toString());
        c.mName = key;
        System.out.println("Loading " + key);
        File rcFile = new File(getCacheDir(), "rc_" + key + ".rc");
        File thFile = new File(getCacheDir(), "th_" + key + ".png");

        byte[] bytes = null;
        if (rcFile.exists() && rcFile.length() > 0) bytes = readBytes(rcFile);
        if (bytes == null || bytes.length == 0) {
            bytes = ExperimentRecyclerActivity.docToBytes(doc);
            if (bytes != null && bytes.length > 0) writeBytes(rcFile, bytes);
        }
        final byte[] fb = bytes;
        Bitmap ft = thFile.exists()
                ? BitmapFactory.decodeFile(thFile.getAbsolutePath()) : null;
        final boolean ren = (ft == null && fb != null && fb.length > 0);

        mMain.post(() -> {
            if (mCards == null) return;
            c.mRcBytes = fb;
            c.mBytesReady = fb != null && fb.length > 0;
            if (ft != null) setThumb(c, ft);
            if (ren) renderThumb(ci, fb, thFile);
            reassignPlayers();
        });
    }

    private void renderThumb(int ci, byte[] bytes, File out) {
        RemoteComposePlayer off = new RemoteComposePlayer(this);
        off.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        off.setDocument(bytes);
        int spec = View.MeasureSpec.makeMeasureSpec(THUMB_SIZE, View.MeasureSpec.EXACTLY);
        off.measure(spec, spec);
        off.layout(0, 0, THUMB_SIZE, THUMB_SIZE);
        Bitmap bmp = Bitmap.createBitmap(THUMB_SIZE, THUMB_SIZE, Bitmap.Config.ARGB_8888);
        off.draw(new Canvas(bmp));
        setThumb(mCards[ci], bmp);
        mBg.execute(() -> {
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 85, fos);
            } catch (IOException ignored) {
            }
        });
    }

    private void setThumb(CardState c, Bitmap bmp) {
        c.mThumb = bmp;
        c.mThumbReady = true;
        c.mView.mThumbImg.setImageBitmap(bmp);
    }

    // -----------------------------------------------------------------------
    // Player pool
    // -----------------------------------------------------------------------

    private void reassignPlayers() {
        if (mCards == null) return;
        for (int s = 0; s < MAX_PLAYERS; s++) {
            if (mPoolOwner[s] >= 0) {
                mCards[mPoolOwner[s]].mView.detach();
                mPoolOwner[s] = -1;
            }
        }
        if (mFeaturedIdx >= 0) give(0, mFeaturedIdx);

        float thresh = THRESH_PX / CARD_BASE;
        @SuppressLint("PrimitiveInCollection") List<Integer> cands = new ArrayList<>();
        for (int i = 0; i < NUM_CARDS; i++) {
            if (!mCards[i].mFeatured && mCards[i].mScale >= thresh) cands.add(i);
        }
        cands.sort((a, b) -> Float.compare(mCards[b].mScale, mCards[a].mScale));
        int slot = 1;
        for (int c : cands) {
            if (slot >= MAX_PLAYERS) break;
            give(slot++, c);
        }
    }

    private void give(int slot, int ci) {
        if (!mCards[ci].mBytesReady) return;
        mPoolOwner[slot] = ci;
        mCards[ci].mView.attach(mPool[slot], mCards[ci].mRcBytes);
        mCards[ci].mView.mName = mCards[ci].mName;
    }

    // -----------------------------------------------------------------------
    // Animation
    // -----------------------------------------------------------------------

    private void startAnim(int w, int h) {
        mAnim = ns -> {
            if (mLastNs < 0) mLastNs = ns;
            float dt = Math.min((ns - mLastNs) * 1e-9f, 0.05f);
            mLastNs = ns;
            tick(dt, w, h);
            Choreographer.getInstance().postFrameCallback(mAnim);
        };
        Choreographer.getInstance().postFrameCallback(mAnim);
    }

    private void tick(float dt, int w, int h) {
        if (mCards == null) return;
        float thresh = THRESH_PX / CARD_BASE;
        float lp = 1f - (float) Math.exp(-LERP_K * dt);

        for (CardState c : mCards) {
            float prev = c.mScale;
            if (c.mFeatured) {
                c.mCx += (c.mTargetCx - c.mCx) * lp;
                c.mCy += (c.mTargetCy - c.mCy) * lp;
                c.mScale += (c.mTargetScale - c.mScale) * lp;
            } else {
                c.mCx += c.mVx * dt;
                c.mCy += c.mVy * dt;
                float hs = CARD_BASE * c.mScale * 0.5f;
                if (c.mCx - hs < 0) {
                    c.mCx = hs;
                    c.mVx = Math.abs(c.mVx);
                }
                if (c.mCx + hs > w) {
                    c.mCx = w - hs;
                    c.mVx = -Math.abs(c.mVx);
                }
                if (c.mCy - hs < 0) {
                    c.mCy = hs;
                    c.mVy = Math.abs(c.mVy);
                }
                if (c.mCy + hs > h) {
                    c.mCy = h - hs;
                    c.mVy = -Math.abs(c.mVy);
                }
                c.mScale += (c.mTargetScale - c.mScale) * lp;
            }
            if ((prev < thresh) != (c.mScale < thresh)) mReassignPending = true;
            placeCard(c);
        }
        if (mReassignPending) {
            reassignPlayers();
            mReassignPending = false;
        }
    }

    /**
     * Position, scale, and dim the card view.
     * Featured cards use mFeatPx as their layout base (already resized); swarm cards use CARD_BASE.
     * cx/cy always represents the on-screen centre of the card.
     * Dim overlay: 0% at MAX_SCALE → 30% at MIN_SCALE; featured cards are always undimmed.
     */
    private void placeCard(CardState c) {
        int base = c.mFeatured ? mFeatPx : CARD_BASE;
        c.mView.setTranslationX(c.mCx - base / 2f);
        c.mView.setTranslationY(c.mCy - base / 2f);
        c.mView.setScaleX(c.mScale);
        c.mView.setScaleY(c.mScale);
        c.mView.setPivotX(base / 2f);
        c.mView.setPivotY(base / 2f);
        c.mView.setElevation(c.mFeatured ? 200f : c.mScale * 20f);

        float dimAlpha;
        if (c.mFeatured) {
            dimAlpha = 0f;
        } else {
            float t = (c.mScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
            t = Math.max(0f, Math.min(1f, t));
            dimAlpha = 0.30f * (1f - t);
        }
        c.mView.setDimAlpha(dimAlpha);
    }

    // -----------------------------------------------------------------------
    // Two-phase promotion
    // -----------------------------------------------------------------------

    private void autoCycle() {
        if (mCards != null && mIncomingIdx < 0) {
            @SuppressLint("PrimitiveInCollection")
            List<Integer> pool = new ArrayList<>();
            for (int i = 0; i < NUM_CARDS; i++) {
                if (!mCards[i].mFeatured) {
                    pool.add(i);
                }
            }
            if (!pool.isEmpty()) promote(pool.get(mRng.nextInt(pool.size())));
        }
        mMain.postDelayed(this::autoCycle, PROMOTE_MS);
    }

    /** Phase 1: shrink the current featured card; schedule phase 2 after the gap. */
    private void promote(int newIdx) {
        if (mCards == null || newIdx < 0 || newIdx >= NUM_CARDS) return;
        if (mCards[newIdx].mFeatured) return;
        if (mIncomingIdx >= 0) return;

        if (mFeaturedIdx >= 0) {
            CardState old = mCards[mFeaturedIdx];
            // Convert: old.scale ≈ 1.0 at mFeatPx layout → equivalent scale at CARD_BASE layout
            float displayPx = mFeatPx * old.mScale;
            old.mFeatured = false;
            resizeCardView(old, CARD_BASE);
            old.mScale = displayPx / CARD_BASE;
            float ang = mRng.nextFloat() * (float) (Math.PI * 2);
            float spd = MIN_SPEED + mRng.nextFloat() * (MAX_SPEED - MIN_SPEED);
            old.mVx = (float) Math.cos(ang) * spd;
            old.mVy = (float) Math.sin(ang) * spd;
            old.mTargetScale = MIN_SCALE + mRng.nextFloat() * (MAX_SCALE - MIN_SCALE);
        }

        mFeaturedIdx = -1;
        mIncomingIdx = newIdx;
        reassignPlayers();

        mMain.postDelayed(this::completePromotion, PROMOTE_GAP_MS);
    }

    /** Phase 2: resize the incoming card to native resolution and animate it to centre. */
    private void completePromotion() {
        int ni = mIncomingIdx;
        if (ni < 0 || mCards == null) return;
        mIncomingIdx = -1;
        mFeaturedIdx = ni;

        CardState neu = mCards[ni];
        // Convert: neu.scale is in CARD_BASE units → equivalent scale at mFeatPx layout
        float displayPx = CARD_BASE * neu.mScale;
        neu.mFeatured = true;
        resizeCardView(neu, mFeatPx);
        neu.mScale = displayPx / mFeatPx;
        neu.mVx = neu.mVy = 0;
        neu.mTargetCx = mRoot.getWidth() / 2f;
        neu.mTargetCy = mRoot.getHeight() / 2f;
        neu.mTargetScale = 1f;  // scale=1 means displayed at exactly mFeatPx px
        mRoot.bringChildToFront(neu.mView);
        reassignPlayers();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Change the SwarmCard container's layout size. Children use MATCH_PARENT so they
     * remeasure automatically; RemoteComposePlayer will re-render at the new resolution.
     */
    private static void resizeCardView(CardState c, int sizePx) {
        FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) c.mView.getLayoutParams();
        lp.width = sizePx;
        lp.height = sizePx;
        c.mView.setLayoutParams(lp);
        c.mView.setPivotX(sizePx / 2f);
        c.mView.setPivotY(sizePx / 2f);
    }

    private static String sanitize(String s) {
        s = s.replaceAll("[^A-Za-z0-9_]", "_");
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    private static byte[] readBytes(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int n = fis.read(b);
            return n > 0 ? b : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeBytes(File f, byte[] data) {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
        } catch (IOException ignored) {
        }
    }
}
