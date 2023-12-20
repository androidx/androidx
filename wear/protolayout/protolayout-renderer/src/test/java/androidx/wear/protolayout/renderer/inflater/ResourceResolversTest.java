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

package androidx.wear.protolayout.renderer.inflater;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;
import androidx.wear.protolayout.proto.ResourceProto.AndroidAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AndroidImageResourceByContentUri;
import androidx.wear.protolayout.proto.ResourceProto.AndroidImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AndroidSeekableAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AnimatedImageFormat;
import androidx.wear.protolayout.proto.ResourceProto.ImageResource;
import androidx.wear.protolayout.proto.ResourceProto.InlineImageResource;
import androidx.wear.protolayout.proto.ResourceProto.Resources;
import androidx.wear.protolayout.proto.TriggerProto.OnVisibleTrigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.AndroidAnimatedImageResourceByResIdResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.AndroidImageResourceByContentUriResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.AndroidImageResourceByResIdResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.AndroidSeekableAnimatedImageResourceByResIdResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.InlineImageResourceResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;
import androidx.wear.protolayout.renderer.test.R;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class ResourceResolversTest {
    @Rule public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock private InlineImageResourceResolver mInlineImageResolver;
    @Mock private AndroidImageResourceByResIdResolver mImageByResIdResolver;
    @Mock private AndroidAnimatedImageResourceByResIdResolver mAnimatedImageByResIdResolver;

    @Mock
    private AndroidSeekableAnimatedImageResourceByResIdResolver
            mSeekableAnimatedImageByResIdResolver;

    @Mock private AndroidImageResourceByContentUriResolver mContentUriResolver;

    private final Drawable mTestDrawable = new VectorDrawable();
    private final ListenableFuture<Drawable> mFutureDrawable =
            Futures.immediateFuture(mTestDrawable);

    private static final InlineImageResource INLINE_IMAGE =
            InlineImageResource.newBuilder().setHeightPx(123).setWidthPx(456).build();
    private static final AndroidImageResourceByResId ANDROID_IMAGE_BY_RES_ID =
            AndroidImageResourceByResId.newBuilder().setResourceId(R.drawable.android_24dp).build();
    private static final AndroidAnimatedImageResourceByResId ANDROID_AVD_BY_RES_ID =
            AndroidAnimatedImageResourceByResId.newBuilder()
                    .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                    .setResourceId(R.drawable.android_animated_24dp)
                    .setStartTrigger(
                            Trigger.newBuilder()
                                    .setOnVisibleTrigger(OnVisibleTrigger.getDefaultInstance()))
                    .build();
    private static final AndroidSeekableAnimatedImageResourceByResId
            ANDROID_SEEKABLE_AVD_BY_RES_ID =
                    AndroidSeekableAnimatedImageResourceByResId.newBuilder()
                            .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                            .setResourceId(R.drawable.android_animated_24dp)
                            .setProgress(
                                    DynamicFloat.newBuilder()
                                            .setFixed(
                                                    FixedFloat.newBuilder().setValue(0.5f).build())
                                            .build())
                            .build();
    private static final AndroidImageResourceByContentUri CONTENT_URI_IMAGE =
            AndroidImageResourceByContentUri.newBuilder()
                    .setContentUri("content://foo/bar")
                    .build();

    private static final String INLINE_IMAGE_RESOURCE_ID = "inline";
    private static final String ANDROID_IMAGE_BY_RES_ID_RESOURCE_ID = "androidImageById";
    private static final String ANDROID_AVD_BY_RES_ID_RESOURCE_ID = "androidAVDById";
    private static final String ANDROID_SEEKABLE_AVD_BY_RES_ID_RESOURCE_ID =
            "androidSeekableAVDById";
    private static final String CONTENT_URI_RESOURCE_ID = "contentUriImage";

    @Test
    public void inlineImageRequestRoutedToCorrectAccessor() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        when(mInlineImageResolver.getDrawableOrThrow(any())).thenReturn(mTestDrawable);

        // Check future's value to keep compiler happy.
        assertThat(resolvers.getDrawable(INLINE_IMAGE_RESOURCE_ID).get())
                .isSameInstanceAs(mTestDrawable);

        verify(mInlineImageResolver).getDrawableOrThrow(INLINE_IMAGE);
        verify(mImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mSeekableAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mContentUriResolver, never()).getDrawable(any());
    }

    @Test
    public void imageByResIdRequestRoutedToCorrectAccessor() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        when(mImageByResIdResolver.getDrawableOrThrow(any())).thenReturn(mTestDrawable);

        // Check future's value to keep compiler happy.
        assertThat(resolvers.getDrawable(ANDROID_IMAGE_BY_RES_ID_RESOURCE_ID).get())
                .isSameInstanceAs(mTestDrawable);

        verify(mInlineImageResolver, never()).getDrawableOrThrow(any());
        verify(mImageByResIdResolver).getDrawableOrThrow(ANDROID_IMAGE_BY_RES_ID);
        verify(mAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mSeekableAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mContentUriResolver, never()).getDrawable(any());
    }

    @Test
    public void animatedImageByResIdRequestRoutedToCorrectAccessor() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        when(mAnimatedImageByResIdResolver.getDrawableOrThrow(any())).thenReturn(mTestDrawable);

        // Check future's value to keep compiler happy.
        assertThat(resolvers.getDrawable(ANDROID_AVD_BY_RES_ID_RESOURCE_ID).get())
                .isSameInstanceAs(mTestDrawable);

        verify(mInlineImageResolver, never()).getDrawableOrThrow(any());
        verify(mImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mAnimatedImageByResIdResolver).getDrawableOrThrow(ANDROID_AVD_BY_RES_ID);
        verify(mSeekableAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mContentUriResolver, never()).getDrawable(any());
    }

    @Test
    public void seekableAnimatedImageByResIdRequestRoutedToCorrectAccessor() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        when(mSeekableAnimatedImageByResIdResolver.getDrawableOrThrow(any()))
                .thenReturn(mTestDrawable);

        // Check future's value to keep compiler happy.
        assertThat(resolvers.getDrawable(ANDROID_SEEKABLE_AVD_BY_RES_ID_RESOURCE_ID).get())
                .isSameInstanceAs(mTestDrawable);

        verify(mInlineImageResolver, never()).getDrawableOrThrow(any());
        verify(mImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mSeekableAnimatedImageByResIdResolver)
                .getDrawableOrThrow(ANDROID_SEEKABLE_AVD_BY_RES_ID);
        verify(mContentUriResolver, never()).getDrawable(any());
    }

    @Test
    public void contentUriImageRoutedToCorrectAccessor() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        when(mContentUriResolver.getDrawable(any())).thenReturn(mFutureDrawable);

        // Check future's value to keep compiler happy.
        assertThat(resolvers.getDrawable(CONTENT_URI_RESOURCE_ID))
                .isSameInstanceAs(mFutureDrawable);

        verify(mInlineImageResolver, never()).getDrawableOrThrow(any());
        verify(mImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mSeekableAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mContentUriResolver).getDrawable(CONTENT_URI_IMAGE);
    }

    @Test
    public void throwsExceptionWithNonExistentResourceId() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        ListenableFuture<Drawable> future = resolvers.getDrawable("does_not_exist");

        verify(mInlineImageResolver, never()).getDrawableOrThrow(any());
        verify(mImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mSeekableAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mContentUriResolver, never()).getDrawable(any());

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    public void throwsIfAccessorIsMissing() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        // No inline image resolver.
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        ListenableFuture<Drawable> future = resolvers.getDrawable(INLINE_IMAGE_RESOURCE_ID);

        verify(mInlineImageResolver, never()).getDrawableOrThrow(any());
        verify(mImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mSeekableAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mContentUriResolver, never()).getDrawable(any());

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    public void inlineImageCantHavePlaceholder() {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(
                                buildResources(
                                        /* httpPlaceholderResourceId= */ INLINE_IMAGE_RESOURCE_ID))
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        assertThat(resolvers.hasPlaceholderDrawable(INLINE_IMAGE_RESOURCE_ID)).isFalse();
    }

    @Test
    public void imageByResCantHavePlaceholder() {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(
                                buildResources(
                                        /* httpPlaceholderResourceId= */ INLINE_IMAGE_RESOURCE_ID))
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        assertThat(resolvers.hasPlaceholderDrawable(ANDROID_IMAGE_BY_RES_ID_RESOURCE_ID)).isFalse();
    }

    @Test
    public void animatedImageByResCantHavePlaceholder() {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(
                                buildResources(
                                        /* httpPlaceholderResourceId= */ INLINE_IMAGE_RESOURCE_ID))
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        assertThat(resolvers.hasPlaceholderDrawable(ANDROID_AVD_BY_RES_ID_RESOURCE_ID)).isFalse();
    }

    @Test
    public void contentUriCantHavePlaceholder() {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(
                                buildResources(
                                        /* httpPlaceholderResourceId= */ INLINE_IMAGE_RESOURCE_ID))
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        assertThat(resolvers.hasPlaceholderDrawable(CONTENT_URI_RESOURCE_ID)).isFalse();
    }

    @Test
    public void getPlaceholderDrawableThrowsIfResourceCannotHavePlaceholder() throws Exception {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        // No inline image resolver.
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        assertThrows(
                ResourceAccessException.class,
                () -> resolvers.getPlaceholderDrawableOrThrow(INLINE_IMAGE_RESOURCE_ID));

        verify(mInlineImageResolver, never()).getDrawableOrThrow(any());
        verify(mImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
        verify(mSeekableAnimatedImageByResIdResolver, never()).getDrawableOrThrow(any());
    }

    @Test
    public void canImageBeTinted_onlyReturnsTrueForAndroidResources() {
        ResourceResolvers resolvers =
                ResourceResolvers.builder(buildResources())
                        .setInlineImageResourceResolver(mInlineImageResolver)
                        .setAndroidImageResourceByResIdResolver(mImageByResIdResolver)
                        .setAndroidAnimatedImageResourceByResIdResolver(
                                mAnimatedImageByResIdResolver)
                        .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                                mSeekableAnimatedImageByResIdResolver)
                        .setAndroidImageResourceByContentUriResolver(mContentUriResolver)
                        .build();

        assertThat(resolvers.canImageBeTinted(ANDROID_IMAGE_BY_RES_ID_RESOURCE_ID)).isTrue();
        assertThat(resolvers.canImageBeTinted(ANDROID_AVD_BY_RES_ID_RESOURCE_ID)).isTrue();
        assertThat(resolvers.canImageBeTinted(ANDROID_SEEKABLE_AVD_BY_RES_ID_RESOURCE_ID)).isTrue();
        assertThat(resolvers.canImageBeTinted(CONTENT_URI_RESOURCE_ID)).isFalse();
        assertThat(resolvers.canImageBeTinted(INLINE_IMAGE_RESOURCE_ID)).isFalse();
    }

    private static Resources buildResources() {
        return buildResources(null);
    }

    private static Resources buildResources(@Nullable String httpPlaceholderResourceId) {
        Resources.Builder builder =
                Resources.newBuilder()
                        .putIdToImage(
                                INLINE_IMAGE_RESOURCE_ID,
                                ImageResource.newBuilder().setInlineResource(INLINE_IMAGE).build())
                        .putIdToImage(
                                ANDROID_IMAGE_BY_RES_ID_RESOURCE_ID,
                                ImageResource.newBuilder()
                                        .setAndroidResourceByResId(ANDROID_IMAGE_BY_RES_ID)
                                        .build())
                        .putIdToImage(
                                ANDROID_AVD_BY_RES_ID_RESOURCE_ID,
                                ImageResource.newBuilder()
                                        .setAndroidAnimatedResourceByResId(ANDROID_AVD_BY_RES_ID)
                                        .build())
                        .putIdToImage(
                                ANDROID_SEEKABLE_AVD_BY_RES_ID_RESOURCE_ID,
                                ImageResource.newBuilder()
                                        .setAndroidSeekableAnimatedResourceByResId(
                                                ANDROID_SEEKABLE_AVD_BY_RES_ID)
                                        .build())
                        .putIdToImage(
                                CONTENT_URI_RESOURCE_ID,
                                ImageResource.newBuilder()
                                        .setAndroidContentUri(CONTENT_URI_IMAGE)
                                        .build());

        return builder.build();
    }
}
