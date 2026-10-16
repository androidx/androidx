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

package androidx.photopicker.activity

import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import android.widget.photopicker.PhotoPickerSelectionParams
import android.widget.photopicker.PhotoPickerUiCustomizationParams
import androidx.annotation.RequiresExtension
import androidx.photopicker.activity.PickVisualMedia.MediaCapabilities
import androidx.photopicker.activity.PickVisualMedia.MediaCapabilities.Companion.TYPE_HDR10
import androidx.photopicker.activity.PickVisualMedia.MediaCapabilities.Companion.TYPE_HDR10_PLUS
import androidx.photopicker.activity.PickVisualMedia.MediaCapabilities.Companion.TYPE_HLG10
import androidx.photopicker.activity.params.HighlightAlbumParams
import androidx.photopicker.activity.params.HighlightSearchResultsParams
import androidx.photopicker.activity.params.HighlightType
import androidx.photopicker.activity.params.SelectionParams
import androidx.photopicker.activity.params.UiCustomizationParams
import androidx.photopicker.activity.params.UiCustomizationParams.Companion.ASPECT_RATIO_PORTRAIT_9_16
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PickVisualMediaRequestTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun buildPickVisualMedia() {
        val request =
            PickVisualMediaRequest.Builder().setMediaType(PickVisualMedia.VideoOnly).build()
        assertThat(request.mediaType).isEqualTo(PickVisualMedia.VideoOnly)
    }

    @Test
    fun PickVisualMediaFun() {
        val request = PickVisualMediaRequest(PickVisualMedia.VideoOnly)

        assertThat(request.mediaType).isEqualTo(PickVisualMedia.VideoOnly)
    }

    @Test
    fun testPickVisualMediaRequest_maxItems() {
        val request = PickVisualMediaRequest(mediaType = PickVisualMedia.ImageOnly, maxItems = 5)

        assertThat(request.mediaType).isEqualTo(PickVisualMedia.ImageOnly)
        assertThat(request.maxItems).isEqualTo(5)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU, codeName = "Tiramisu")
    fun testPickMultipleVisualMediaRequest_maxItems() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val defaultMaxItems = PickMultipleVisualMedia.getMaxItems()

        // test boundary (maxItems should be greater than 1)
        assertThrows(IllegalArgumentException::class.java) { PickMultipleVisualMedia(maxItems = 1) }

        // test boundary (maxItems should be less than the defaultMaxItems)
        var request = PickMultipleVisualMedia(maxItems = 1 + defaultMaxItems)
        assertThrows(IllegalArgumentException::class.java) {
            request.createIntent(
                context,
                input = PickVisualMediaRequest(maxItems = 1 + defaultMaxItems),
            )
        }

        // test default
        request = PickMultipleVisualMedia()
        var intent = request.createIntent(context, input = PickVisualMediaRequest())

        assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_MAX)).isTrue()
        assertThat(intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, /* defaultValue= */ 0))
            .isEqualTo(/* expected= */ defaultMaxItems)

        // test given maxItems in PickMultipleVisualMedia
        request = PickMultipleVisualMedia(maxItems = 9)
        intent = request.createIntent(context, input = PickVisualMediaRequest())

        assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_MAX)).isTrue()
        assertThat(intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, /* defaultValue= */ 0))
            .isEqualTo(/* expected= */ 9)

        // test max selection limit updated to minimum of PickMultipleVisualMedia.maxItems and the
        // input PickVisualMediaRequest.maxItems
        intent = request.createIntent(context, input = PickVisualMediaRequest(maxItems = 7))

        assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_MAX)).isTrue()
        assertThat(intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, /* defaultValue= */ 0))
            .isEqualTo(/* expected= */ 7)
    }

    @Test
    fun testPickVisualMediaRequest_accentColor() {
        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.isCustomAccentColorApplied).isEqualTo(false)

        // test given accent color in PickVisualMediaRequest
        request = PickVisualMediaRequest(accentColor = 0xffff0000)
        assertThat(request.isCustomAccentColorApplied).isEqualTo(true)
        assertThat(request.accentColor).isEqualTo(0xffff0000)
    }

    @Test
    fun testPickVisualMediaRequest_defaultTab() {
        val context = InstrumentationRegistry.getInstrumentation().context

        // test default from Builder
        var request = PickVisualMediaRequest.Builder().build()
        assertThat(request.defaultTab).isEqualTo(PickVisualMedia.DefaultTab.PhotosTab)
        assertThat(request.isCustomDefaultTabApplied).isEqualTo(false)

        var singleIntent = PickVisualMedia().createIntent(context, request)
        assertThat(singleIntent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB)).isFalse()

        // test builder setDefaultTab with PhotosTab
        request =
            PickVisualMediaRequest.Builder()
                .setDefaultTab(PickVisualMedia.DefaultTab.PhotosTab)
                .build()
        assertThat(request.defaultTab).isEqualTo(PickVisualMedia.DefaultTab.PhotosTab)
        assertThat(request.isCustomDefaultTabApplied).isEqualTo(true)

        singleIntent = PickVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            assertThat(singleIntent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB)).isTrue()
            assertThat(singleIntent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB, -1))
                .isEqualTo(PickVisualMedia.DefaultTab.PhotosTab.value)
        }

        // test given default tab in PickVisualMediaRequest
        request = PickVisualMediaRequest(defaultTab = PickVisualMedia.DefaultTab.AlbumsTab)
        assertThat(request.defaultTab).isEqualTo(PickVisualMedia.DefaultTab.AlbumsTab)
        assertThat(request.isCustomDefaultTabApplied).isEqualTo(true)

        singleIntent = PickVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            assertThat(singleIntent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB)).isTrue()
            assertThat(singleIntent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB, -1))
                .isEqualTo(PickVisualMedia.DefaultTab.AlbumsTab.value)
        }
    }

    @Test
    fun testPickVisualMediaRequest_isOrderedSelection() {
        val context = InstrumentationRegistry.getInstrumentation().context

        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.isOrderedSelection).isEqualTo(false)

        var multipleIntent = PickMultipleVisualMedia().createIntent(context, request)
        assertThat(multipleIntent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_IN_ORDER)).isFalse()

        // test builder setOrderedSelection(true)
        request = PickVisualMediaRequest.Builder().setOrderedSelection(true).build()
        assertThat(request.isOrderedSelection).isEqualTo(true)

        multipleIntent = PickMultipleVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            assertThat(multipleIntent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_IN_ORDER)).isTrue()
            assertThat(multipleIntent.getBooleanExtra(MediaStore.EXTRA_PICK_IMAGES_IN_ORDER, false))
                .isTrue()
        }

        // test given isOrderedSelection in PickVisualMediaRequest
        request = PickVisualMediaRequest(isOrderedSelection = true)
        assertThat(request.isOrderedSelection).isEqualTo(true)

        multipleIntent = PickMultipleVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            assertThat(multipleIntent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_IN_ORDER)).isTrue()
            assertThat(multipleIntent.getBooleanExtra(MediaStore.EXTRA_PICK_IMAGES_IN_ORDER, false))
                .isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testPickVisualMediaRequest_mediaCapabilities() {
        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.mediaCapabilitiesForTranscoding).isNull()

        // test given mediaCapabilities in PickVisualMediaRequest
        val mediaCapabilities =
            MediaCapabilities.Builder()
                .addSupportedHdrType(TYPE_HLG10)
                .addSupportedHdrType(TYPE_HDR10)
                .addSupportedHdrType(TYPE_HDR10_PLUS)
                .build()
        request = PickVisualMediaRequest(mediaCapabilitiesForTranscoding = mediaCapabilities)
        assertThat(request.mediaCapabilitiesForTranscoding?.supportedHdrTypes)
            .containsExactly(TYPE_HLG10, TYPE_HDR10, TYPE_HDR10_PLUS)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testPickVisualMediaRequest_accentColorAndMediaCapabilities() {
        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.isCustomAccentColorApplied).isEqualTo(false)
        assertThat(request.mediaCapabilitiesForTranscoding).isNull()

        // test given accent color and mediaCapabilities in PickVisualMediaRequest
        val mediaCapabilities =
            MediaCapabilities.Builder()
                .addSupportedHdrType(TYPE_HLG10)
                .addSupportedHdrType(TYPE_HDR10)
                .addSupportedHdrType(TYPE_HDR10_PLUS)
                .build()
        request =
            PickVisualMediaRequest(
                mediaCapabilitiesForTranscoding = mediaCapabilities,
                accentColor = 0xffff0000,
            )
        assertThat(request.isCustomAccentColorApplied).isEqualTo(true)
        assertThat(request.accentColor).isEqualTo(0xffff0000)
        assertThat(request.mediaCapabilitiesForTranscoding?.supportedHdrTypes)
            .containsExactly(TYPE_HLG10, TYPE_HDR10, TYPE_HDR10_PLUS)
    }

    @Test
    fun testPickVisualMediaRequest_locationMetadataAccess_default() {
        val request = PickVisualMediaRequest.Builder().build()
        assertThat(request.isLocationMetadataAccessRequested).isFalse()
    }

    @Test
    fun testPickVisualMediaRequest_locationMetadataAccess_set() {
        val request =
            PickVisualMediaRequest.Builder().setLocationMetadataAccessRequested(true).build()
        assertThat(request.isLocationMetadataAccessRequested).isTrue()
    }

    @Test
    fun testPickVisualMedia_createIntent_locationMetadataAccess() {
        val context = InstrumentationRegistry.getInstrumentation().context

        // 1. Test default: neither extra should be present
        var request = PickVisualMediaRequest.Builder().build()
        var intent = PickVisualMedia().createIntent(context, request)
        assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS)).isFalse()
        assertThat(
                intent.hasExtra(
                    PickVisualMedia
                        .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS
                )
            )
            .isFalse()

        // 2. Test when location metadata access is requested
        request = PickVisualMediaRequest.Builder().setLocationMetadataAccessRequested(true).build()
        intent = PickVisualMedia().createIntent(context, request)

        val isExt23 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 23

        if (PickVisualMedia.isSystemPickerAvailable()) {
            if (isExt23) {
                assertThat(
                        intent.getBooleanExtra(
                            MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS,
                            false,
                        )
                    )
                    .isTrue()
            } else {
                assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS))
                    .isFalse()
            }
            assertThat(
                    intent.hasExtra(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS
                    )
                )
                .isFalse()
        } else if (PickVisualMedia.isSystemFallbackPickerAvailable(context)) {
            assertThat(
                    intent.getBooleanExtra(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS,
                        false,
                    )
                )
                .isTrue()
            assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS)).isFalse()
        } else {
            assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS)).isFalse()
            assertThat(
                    intent.hasExtra(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS
                    )
                )
                .isFalse()
        }
    }

    @Test
    fun testPickVisualMediaRequest_highlightSearchResultsParams() {
        val params =
            HighlightSearchResultsParams.Builder()
                .setHighlightType(HighlightType.EXPANDED)
                .setHighlightSearchTextQuery("cats")
                .build()

        val request =
            PickVisualMediaRequest.Builder().setHighlightSearchResultsParams(params).build()

        assertThat(request.highlightSearchResultsParams).isNotNull()
        assertThat(request.highlightSearchResultsParams?.highlightSearchTextQuery).isEqualTo("cats")
        assertThat(request.highlightSearchResultsParams?.highlightType)
            .isEqualTo(HighlightType.EXPANDED)
    }

    @Test
    fun testPickVisualMediaRequest_highlightAlbumParams() {
        val params =
            HighlightAlbumParams.Builder()
                .setHighlightType(HighlightType.COLLAPSED)
                .setHighlightAlbum(HighlightAlbumParams.ALBUM_FAVORITES)
                .build()

        val request = PickVisualMediaRequest.Builder().setHighlightAlbumParams(params).build()

        assertThat(request.highlightAlbumParams).isNotNull()
        assertThat(request.highlightAlbumParams?.highlightAlbum)
            .isEqualTo(HighlightAlbumParams.ALBUM_FAVORITES)
        assertThat(request.highlightAlbumParams?.highlightType).isEqualTo(HighlightType.COLLAPSED)
    }

    @Test
    fun testPickVisualMediaRequest_mutuallyExclusiveHighlightParams() {
        val searchParams =
            HighlightSearchResultsParams.Builder()
                .setHighlightType(HighlightType.EXPANDED)
                .setHighlightSearchTextQuery("cats")
                .build()

        val albumParams =
            HighlightAlbumParams.Builder()
                .setHighlightType(HighlightType.COLLAPSED)
                .setHighlightAlbum(HighlightAlbumParams.ALBUM_FAVORITES)
                .build()

        assertThrows(IllegalArgumentException::class.java) {
            PickVisualMediaRequest.Builder()
                .setHighlightSearchResultsParams(searchParams)
                .setHighlightAlbumParams(albumParams)
                .build()
        }
    }

    @Test
    fun testPickVisualMedia_createIntent_highlightSearchResultsParams() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val params =
            HighlightSearchResultsParams.Builder()
                .setHighlightType(HighlightType.EXPANDED)
                .setHighlightSearchTextQuery("cats")
                .build()

        val request =
            PickVisualMediaRequest.Builder().setHighlightSearchResultsParams(params).build()

        val intent = PickVisualMedia().createIntent(context, request)
        if (
            PickVisualMedia.isSystemPickerAvailable() &&
                PickVisualMedia.areSystemHighlightParamsAvailable()
        ) {
            val searchBundle =
                intent.getBundleExtra(MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_SEARCH_RESULTS)

            assertThat(searchBundle).isNotNull()
            assertThat(searchBundle?.getInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE))
                .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_EXPANDED)
            assertThat(
                    searchBundle?.getString(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY)
                )
                .isEqualTo("cats")
        }
    }

    @Test
    fun testPickVisualMediaRequest_uiCustomizationParams() {
        val params =
            UiCustomizationParams.Builder().setAspectRatio(ASPECT_RATIO_PORTRAIT_9_16).build()

        val request = PickVisualMediaRequest.Builder().setUiCustomizationParams(params).build()

        assertThat(request.uiCustomizationParams).isNotNull()
        assertThat(request.uiCustomizationParams?.aspectRatio).isEqualTo(ASPECT_RATIO_PORTRAIT_9_16)
    }

    @Test
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun testPickVisualMedia_createIntent_uiCustomizationParams() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val params =
            UiCustomizationParams.Builder().setAspectRatio(ASPECT_RATIO_PORTRAIT_9_16).build()

        val request = PickVisualMediaRequest.Builder().setUiCustomizationParams(params).build()

        val intent = PickVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            if (PickVisualMedia.arePhotoPickerUiCustomizationParamsAvailable()) {
                assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS))
                    .isTrue()
                val parcelable =
                    intent.getParcelableExtra(
                        MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS,
                        PhotoPickerUiCustomizationParams::class.java,
                    )
                assertThat(parcelable).isNotNull()
                assertThat(parcelable?.aspectRatio)
                    .isEqualTo(PhotoPickerUiCustomizationParams.ASPECT_RATIO_PORTRAIT_9_16)
            } else {
                assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS))
                    .isFalse()
            }
        } else if (PickVisualMedia.isSystemFallbackPickerAvailable(context)) {
            assertThat(
                    intent.hasExtra(
                        PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS
                    )
                )
                .isTrue()
            val bundle =
                intent.getParcelableExtra(
                    PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS,
                    Bundle::class.java,
                )
            assertThat(bundle).isNotNull()
            assertThat(
                    bundle?.getInt(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO
                    )
                )
                .isEqualTo(PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_PORTRAIT_9_16)
        }
    }

    @Test
    fun testPickMultipleVisualMedia_createIntent_locationMetadataAccess() {
        val context = InstrumentationRegistry.getInstrumentation().context

        // 1. Test default: neither extra should be present
        var request = PickVisualMediaRequest.Builder().build()
        var intent = PickMultipleVisualMedia().createIntent(context, request)
        assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS)).isFalse()
        assertThat(
                intent.hasExtra(
                    PickVisualMedia
                        .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS
                )
            )
            .isFalse()

        // 2. Test when location metadata access is requested
        request = PickVisualMediaRequest.Builder().setLocationMetadataAccessRequested(true).build()
        intent = PickMultipleVisualMedia().createIntent(context, request)

        val isExt23 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 23

        if (PickVisualMedia.isSystemPickerAvailable()) {
            if (isExt23) {
                assertThat(
                        intent.getBooleanExtra(
                            MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS,
                            false,
                        )
                    )
                    .isTrue()
            } else {
                assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS))
                    .isFalse()
            }
            assertThat(
                    intent.hasExtra(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS
                    )
                )
                .isFalse()
        } else if (PickVisualMedia.isSystemFallbackPickerAvailable(context)) {
            assertThat(
                    intent.getBooleanExtra(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS,
                        false,
                    )
                )
                .isTrue()
            assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS)).isFalse()
        } else {
            assertThat(intent.hasExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS)).isFalse()
            assertThat(
                    intent.hasExtra(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS
                    )
                )
                .isFalse()
        }
    }

    @Test
    fun testPickVisualMedia_extraSystemFallbackRequestLocationMetadataAccess() {
        assertThat(
                PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS
            )
            .isEqualTo(
                "androidx.activity.result.contract.extra.PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS"
            )
    }

    @Test
    fun testPickVisualMedia_createIntent_highlightAlbumParams() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val params =
            HighlightAlbumParams.Builder()
                .setHighlightType(HighlightType.COLLAPSED)
                .setHighlightAlbum(HighlightAlbumParams.ALBUM_CAMERA)
                .build()

        val request = PickVisualMediaRequest.Builder().setHighlightAlbumParams(params).build()

        val intent = PickVisualMedia().createIntent(context, request)
        if (
            PickVisualMedia.isSystemPickerAvailable() &&
                PickVisualMedia.areSystemHighlightParamsAvailable()
        ) {
            val albumBundle = intent.getBundleExtra(MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_ALBUM)

            assertThat(albumBundle).isNotNull()
            assertThat(albumBundle?.getInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE))
                .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_COLLAPSED)
            assertThat(albumBundle?.getString(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_ALBUM_ID))
                .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_CAMERA)
        }
    }

    @Test
    fun testPickMultipleVisualMedia_createIntent_highlightSearchResultsParams() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val params =
            HighlightSearchResultsParams.Builder()
                .setHighlightType(HighlightType.EXPANDED)
                .setHighlightSearchTextQuery("dogs")
                .build()

        val request =
            PickVisualMediaRequest.Builder().setHighlightSearchResultsParams(params).build()

        val intent = PickMultipleVisualMedia().createIntent(context, request)
        if (
            PickVisualMedia.isSystemPickerAvailable() &&
                PickVisualMedia.areSystemHighlightParamsAvailable()
        ) {
            val searchBundle =
                intent.getBundleExtra(MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_SEARCH_RESULTS)

            assertThat(searchBundle).isNotNull()
            assertThat(searchBundle?.getInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE))
                .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_EXPANDED)
            assertThat(
                    searchBundle?.getString(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY)
                )
                .isEqualTo("dogs")
        }
    }

    @Test
    fun testPickMultipleVisualMedia_createIntent_highlightAlbumParams() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val params =
            HighlightAlbumParams.Builder()
                .setHighlightType(HighlightType.COLLAPSED)
                .setHighlightAlbum(HighlightAlbumParams.ALBUM_FAVORITES)
                .build()

        val request = PickVisualMediaRequest.Builder().setHighlightAlbumParams(params).build()

        val intent = PickMultipleVisualMedia().createIntent(context, request)
        if (
            PickVisualMedia.isSystemPickerAvailable() &&
                PickVisualMedia.areSystemHighlightParamsAvailable()
        ) {
            val albumBundle = intent.getBundleExtra(MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_ALBUM)

            assertThat(albumBundle).isNotNull()
            assertThat(albumBundle?.getInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE))
                .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_COLLAPSED)
            assertThat(albumBundle?.getString(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_ALBUM_ID))
                .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_FAVORITES)
        }
    }

    @Test
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testPickVisualMediaRequest_selectionParams() {
        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.selectionParams).isNull()

        // test given selectionParams in PickVisualMediaRequest
        val selectionParams =
            SelectionParams.Builder().setMinMediaItemResolutionInPixels(100L).build()
        request = PickVisualMediaRequest.Builder().setSelectionParams(selectionParams).build()
        assertThat(request.selectionParams).isEqualTo(selectionParams)
    }

    @Test
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun testPickVisualMedia_createIntent_selectionParams() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val minResolution = 1000L
        val maxResolution = 50000L
        val minDuration = Duration.ofSeconds(5)
        val maxDuration = Duration.ofMinutes(2)
        val maxSize = 10 * 1024 * 1024L
        val maxBatchSize = 50 * 1024 * 1024L
        val mimeTypes = listOf("image/png", "video/mp4")

        val selectionParams =
            SelectionParams.Builder()
                .setMinMediaItemResolutionInPixels(minResolution)
                .setMaxMediaItemResolutionInPixels(maxResolution)
                .setMinVideoDuration(minDuration)
                .setMaxVideoDuration(maxDuration)
                .setMaxMediaItemSizeInBytes(maxSize)
                .setMaxSelectionBatchSizeInBytes(maxBatchSize)
                .setMimeTypes(mimeTypes)
                .build()

        val request = PickVisualMediaRequest.Builder().setSelectionParams(selectionParams).build()

        val intentSingle = PickVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            if (PickVisualMedia.arePhotoPickerSelectionParamsAvailable()) {
                assertThat(intentSingle.hasExtra(MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS))
                    .isTrue()
                val parcelable =
                    intentSingle.getParcelableExtra(
                        MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS,
                        PhotoPickerSelectionParams::class.java,
                    )
                assertThat(parcelable).isNotNull()
                assertThat(parcelable?.minMediaItemResolutionInPixels).isEqualTo(minResolution)
                assertThat(parcelable?.maxMediaItemResolutionInPixels).isEqualTo(maxResolution)
                assertThat(parcelable?.minVideoDuration).isEqualTo(minDuration)
                assertThat(parcelable?.maxVideoDuration).isEqualTo(maxDuration)
                assertThat(parcelable?.maxMediaItemSizeInBytes).isEqualTo(maxSize)
                assertThat(parcelable?.maxSelectionBatchSizeInBytes).isEqualTo(maxBatchSize)
                assertThat(parcelable?.mimeTypes).containsExactly("image/png", "video/mp4")
            } else {
                assertThat(intentSingle.hasExtra(MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS))
                    .isFalse()
            }
        }

        val intentMultiple = PickMultipleVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            if (PickVisualMedia.arePhotoPickerSelectionParamsAvailable()) {
                assertThat(intentMultiple.hasExtra(MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS))
                    .isTrue()
                val parcelable =
                    intentMultiple.getParcelableExtra(
                        MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS,
                        PhotoPickerSelectionParams::class.java,
                    )
                assertThat(parcelable).isNotNull()
                assertThat(parcelable?.minMediaItemResolutionInPixels).isEqualTo(minResolution)
                assertThat(parcelable?.maxMediaItemResolutionInPixels).isEqualTo(maxResolution)
                assertThat(parcelable?.minVideoDuration).isEqualTo(minDuration)
                assertThat(parcelable?.maxVideoDuration).isEqualTo(maxDuration)
                assertThat(parcelable?.maxMediaItemSizeInBytes).isEqualTo(maxSize)
                assertThat(parcelable?.maxSelectionBatchSizeInBytes).isEqualTo(maxBatchSize)
                assertThat(parcelable?.mimeTypes).containsExactly("image/png", "video/mp4")
            } else {
                assertThat(intentMultiple.hasExtra(MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS))
                    .isFalse()
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun testPickMultipleVisualMedia_createIntent_uiCustomizationParams() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val params =
            UiCustomizationParams.Builder().setAspectRatio(ASPECT_RATIO_PORTRAIT_9_16).build()

        val request = PickVisualMediaRequest.Builder().setUiCustomizationParams(params).build()

        val intent = PickMultipleVisualMedia().createIntent(context, request)
        if (PickVisualMedia.isSystemPickerAvailable()) {
            if (PickVisualMedia.arePhotoPickerUiCustomizationParamsAvailable()) {
                assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS))
                    .isTrue()
                val parcelable =
                    intent.getParcelableExtra(
                        MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS,
                        PhotoPickerUiCustomizationParams::class.java,
                    )
                assertThat(parcelable).isNotNull()
                assertThat(parcelable?.aspectRatio)
                    .isEqualTo(PhotoPickerUiCustomizationParams.ASPECT_RATIO_PORTRAIT_9_16)
            } else {
                assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS))
                    .isFalse()
            }
        } else if (PickVisualMedia.isSystemFallbackPickerAvailable(context)) {
            assertThat(
                    intent.hasExtra(
                        PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS
                    )
                )
                .isTrue()
            val bundle =
                intent.getParcelableExtra(
                    PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS,
                    Bundle::class.java,
                )
            assertThat(bundle).isNotNull()
            assertThat(
                    bundle?.getInt(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO
                    )
                )
                .isEqualTo(PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_PORTRAIT_9_16)
        }
    }
}
