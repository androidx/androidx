/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.impl;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.protolayout.renderer.helper.TestDsl.column;
import static androidx.wear.protolayout.renderer.helper.TestDsl.dynamicFixedText;
import static androidx.wear.protolayout.renderer.helper.TestDsl.layout;
import static androidx.wear.protolayout.renderer.helper.TestDsl.text;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.StateStore;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.ResourceProto.Resources;
import androidx.wear.protolayout.renderer.impl.ProtoLayoutViewInstance.Config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ProtoLayoutViewInstanceTest {
    private static final String RESOURCES_VERSION = "1";
    private static final Resources RESOURCES =
            Resources.newBuilder().setVersion(RESOURCES_VERSION).build();
    private static final String TEXT1 = "text1";
    private static final String TEXT2 = "text2";
    private static final String TEXT3 = "text3";

    private final Context mApplicationContext = getApplicationContext();
    private FrameLayout mRootContainer;

    private ProtoLayoutViewInstance mInstanceUnderTest;

    @Before
    public void setUp() {
        mRootContainer = new FrameLayout(getApplicationContext());
        // This needs to be an attached view to test animations in data pipeline.
        Robolectric.buildActivity(Activity.class).setup().get().setContentView(mRootContainer);
    }

    @Test
    public void adaptiveUpdateRatesDisabled_attach_reinflatesCompletely() throws Exception {
        setupInstance(/* adaptiveUpdateRatesEnabled= */ false);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT2))), RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);

        List<View> layout1 = findViewsWithText(mRootContainer, TEXT1);
        assertThat(layout1).hasSize(1);

        result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT3))), RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);
        assertThat(findViewsWithText(mRootContainer, TEXT1)).containsNoneIn(layout1);
        assertThat(findViewsWithText(mRootContainer, TEXT3)).isNotEmpty();
    }

    @Test
    public void adaptiveUpdateRatesEnabled_attach_appliesDiffOnly() throws Exception {
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT2))), RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);

        List<View> layout1 = findViewsWithText(mRootContainer, TEXT1);
        assertThat(layout1).hasSize(1);

        result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT3))), RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);
        // Assert that only the modified text is reinflated.
        assertThat(findViewsWithText(mRootContainer, TEXT1)).containsExactlyElementsIn(layout1);
        assertThat(findViewsWithText(mRootContainer, TEXT3)).isNotEmpty();
    }

    @Test
    public void reattach_usesCachedLayoutForDiffUpdate() throws Exception {
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT2))), RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);
        List<View> layout1 = findViewsWithText(mRootContainer, TEXT1);
        assertThat(layout1).hasSize(1);

        mInstanceUnderTest.detach(mRootContainer);

        result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT3))), RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);
        // Assert that only the modified text is reinflated.
        assertThat(findViewsWithText(mRootContainer, TEXT1)).containsExactlyElementsIn(layout1);
        assertThat(findViewsWithText(mRootContainer, TEXT3)).isNotEmpty();
    }

    @Test
    public void adaptiveUpdateRatesEnabled_applyingDiffToDetachedContainer_returnsNothing()
            throws Exception {
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);

        // First one that does the full layout update.
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT2))), RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);

        List<View> layout1 = findViewsWithText(mRootContainer, TEXT1);
        assertThat(layout1).hasSize(1);

        // Second one that applies mutation only.
        result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT3))), RESOURCES, mRootContainer);
        // Detach so it can't apply update.
        mInstanceUnderTest.detach(mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(result.isCancelled()).isTrue();
        assertThat(mRootContainer.getChildCount()).isEqualTo(0);
    }

    @Test
    public void adaptiveUpdateRatesEnabled_attach_withDynamicValue_appliesDiffOnly()
            throws Exception {
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);

        // Render the first layout.
        Layout layout1 = layout(column(dynamicFixedText(TEXT1), dynamicFixedText(TEXT2)));
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(layout1, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);

        List<View> textView1 = findViewsWithText(mRootContainer, TEXT1);
        assertThat(textView1).hasSize(1);
        assertThat(findViewsWithText(mRootContainer, TEXT2)).hasSize(1);

        Layout layout2 = layout(column(dynamicFixedText(TEXT1), dynamicFixedText(TEXT3)));
        result = mInstanceUnderTest.renderAndAttach(layout2, RESOURCES, mRootContainer);
        // Make sure future is computing result.
        assertThat(result.isDone()).isFalse();
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);
        // Assert that only the modified text is reinflated.
        assertThat(findViewsWithText(mRootContainer, TEXT1)).containsExactlyElementsIn(textView1);
        assertThat(findViewsWithText(mRootContainer, TEXT2)).isEmpty();
        assertThat(findViewsWithText(mRootContainer, TEXT3)).isNotEmpty();
    }

    @Test
    public void adaptiveUpdateRatesEnabled_ongoingRendering_skipsNewLayout() throws Exception {
        FrameLayout container = new FrameLayout(mApplicationContext);
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result1 =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT2))), RESOURCES, container);
        assertThat(result1.isDone()).isFalse();

        ListenableFuture<Void> result2 =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT3))), RESOURCES, container);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result1);
        assertThat(result2.isCancelled()).isTrue();
        // Assert that only the modified text is reinflated.
        assertThat(findViewsWithText(container, TEXT2)).hasSize(1);
        assertThat(findViewsWithText(container, TEXT3)).isEmpty();
    }

    @Test
    public void attachingToANewContainer_withoutDetach_throws() throws Exception {
        FrameLayout container1 = new FrameLayout(mApplicationContext);
        FrameLayout container2 = new FrameLayout(mApplicationContext);
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(
                        layout(column(text(TEXT1), text(TEXT2))), RESOURCES, container1);

        assertThrows(
                IllegalStateException.class,
                () ->
                        mInstanceUnderTest.renderAndAttach(
                                layout(column(text(TEXT1), text(TEXT2))), RESOURCES, container2));
        shadowOf(Looper.getMainLooper()).idle();

        // Check the result from first attach.
        assertNoException(result);
    }

    @Test
    public void renderingToADetachedContainer_isNoOp() throws Exception {
        FrameLayout container1 = new FrameLayout(mApplicationContext);
        FrameLayout container2 = new FrameLayout(mApplicationContext);
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result1 =
                mInstanceUnderTest.renderAndAttach(layout(text(TEXT1)), RESOURCES, container1);
        mInstanceUnderTest.detach(container1);

        ListenableFuture<Void> result2 =
                mInstanceUnderTest.renderAndAttach(layout(text(TEXT1)), RESOURCES, container2);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(result1.isCancelled()).isTrue();
        assertThat(result2.isDone()).isTrue();
        assertNoException(result2);
        assertThat(findViewsWithText(container1, TEXT1)).isEmpty();
        assertThat(findViewsWithText(container2, TEXT1)).hasSize(1);
    }

    @Test
    public void adaptiveUpdateRatesDisabled_sameLayoutReference_subsequentRendering_isNoOp()
            throws Exception {
        Layout layout = layout(text(TEXT1));
        setupInstance(/* adaptiveUpdateRatesEnabled= */ false);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);

        result = mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);

        shadowOf(Looper.getMainLooper()).idle();
        assertNoException(result);
        assertThat(shadowOf(Looper.getMainLooper()).isIdle()).isTrue();
    }

    @Test
    public void adaptiveUpdateRatesEnabled_afterNoChange_reattach_sameLayoutReference_isNoOp()
            throws Exception {
        Layout layout1 = layout(text(TEXT1));
        Layout layout2 = layout(text(TEXT1));
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(layout1, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);

        // Make sure we have an UnchangedRenderResult
        result = mInstanceUnderTest.renderAndAttach(layout2, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);
        assertThat(findViewsWithText(mRootContainer, TEXT1)).hasSize(1);

        mInstanceUnderTest.detach(mRootContainer);
        assertThat(findViewsWithText(mRootContainer, TEXT1)).isEmpty();
        shadowOf(Looper.getMainLooper()).idle();

        result = mInstanceUnderTest.renderAndAttach(layout2, RESOURCES, mRootContainer);

        assertThat(result.isDone()).isTrue();
        assertNoException(result);
        assertThat(findViewsWithText(mRootContainer, TEXT1)).hasSize(1);
    }

    @Test
    public void fullInflationResultCanBeReused() throws Exception {
        setupInstance(/* adaptiveUpdateRatesEnabled= */ false);
        Layout layout = layout(text(TEXT1));
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();

        assertNoException(result);

        ListenableFuture<?> renderFuture = mInstanceUnderTest.mRenderFuture;

        mInstanceUnderTest.detach(mRootContainer);
        result = mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);

        shadowOf(Looper.getMainLooper()).idle();
        assertNoException(result);
        assertThat(mInstanceUnderTest.mRenderFuture).isSameInstanceAs(renderFuture);
    }

    @Test
    public void adaptiveUpdateRatesDisabled_close_discardsReferencesToPreviousView()
            throws Exception {
        Layout layout = layout(text(TEXT1));
        setupInstance(/* adaptiveUpdateRatesEnabled= */ false);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();
        assertNoException(result);
        List<View> textViews1 = findViewsWithText(mRootContainer, TEXT1);
        assertThat(textViews1).hasSize(1);

        mInstanceUnderTest.close();
        result = mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);

        assertThat(shadowOf(Looper.getMainLooper()).isIdle()).isFalse();
        shadowOf(Looper.getMainLooper()).idle();
        assertNoException(result);
        List<View> textViews2 = findViewsWithText(mRootContainer, TEXT1);
        assertThat(textViews2).hasSize(1);
        assertThat(textViews1.get(0)).isNotSameInstanceAs(textViews2.get(0));
    }

    @Test
    public void detach_clearsHostView() throws Exception {
        Layout layout = layout(text(TEXT1));
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();
        assertNoException(result);
        assertThat(findViewsWithText(mRootContainer, TEXT1)).hasSize(1);

        mInstanceUnderTest.detach(mRootContainer);

        assertThat(mRootContainer.getChildCount()).isEqualTo(0);
    }

    @Test
    public void close_clearsHostView() throws Exception {
        Layout layout = layout(text(TEXT1));
        setupInstance(/* adaptiveUpdateRatesEnabled= */ true);
        ListenableFuture<Void> result =
                mInstanceUnderTest.renderAndAttach(layout, RESOURCES, mRootContainer);
        shadowOf(Looper.getMainLooper()).idle();
        assertNoException(result);
        assertThat(findViewsWithText(mRootContainer, TEXT1)).hasSize(1);

        mInstanceUnderTest.close();

        assertThat(mRootContainer.getChildCount()).isEqualTo(0);
    }

    private void setupInstance(boolean adaptiveUpdateRatesEnabled) {
        FakeExecutorService uiThreadExecutor =
                new FakeExecutorService(new Handler(Looper.getMainLooper()));
        ListeningExecutorService listeningExecutorService =
                MoreExecutors.listeningDecorator(uiThreadExecutor);

        ProtoLayoutViewInstance.Config config =
                new Config.Builder(
                                mApplicationContext,
                                listeningExecutorService,
                                listeningExecutorService,
                                /* clickableIdExtra= */ "CLICKABLE_ID_EXTRA")
                        .setStateStore(new StateStore(ImmutableMap.of()))
                        .setLoadActionListener(nextState -> {})
                        .setAnimationEnabled(true)
                        .setRunningAnimationsLimit(Integer.MAX_VALUE)
                        .setUpdatesEnabled(true)
                        .setAdaptiveUpdateRatesEnabled(adaptiveUpdateRatesEnabled)
                        .setIsViewFullyVisible(false)
                        .build();
        mInstanceUnderTest = new ProtoLayoutViewInstance(config);
    }

    private List<View> findViewsWithText(ViewGroup root, String text) {
        ArrayList<View> views = new ArrayList<>();
        root.findViewsWithText(views, text, View.FIND_VIEWS_WITH_TEXT);
        return views;
    }

    private static void assertNoException(ListenableFuture<Void> result) throws Exception {
        // Assert that result hasn't thrown exception.
        result.get();
    }

    static class FakeExecutorService extends AbstractExecutorService {

        private final Handler mHandler;

        FakeExecutorService(Handler mHandler) {
            this.mHandler = mHandler;
        }

        @Override
        public void shutdown() {}

        @Override
        public List<Runnable> shutdownNow() {
            return ImmutableList.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            mHandler.post(command);
        }
    }
}
