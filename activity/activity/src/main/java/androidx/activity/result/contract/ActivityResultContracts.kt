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
package androidx.activity.result.contract

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.media.ApplicationMediaCapabilities
import android.media.MediaFeature.HdrType.DOLBY_VISION
import android.media.MediaFeature.HdrType.HDR10
import android.media.MediaFeature.HdrType.HDR10_PLUS
import android.media.MediaFeature.HdrType.HLG
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions.getExtensionVersion
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents.Companion.getClipDataUris
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.ACTION_SYSTEM_FALLBACK_PICK_IMAGES
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_ACCENT_COLOR
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_IN_ORDER
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_LAUNCH_TAB
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.getSystemFallbackPicker
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.ACTION_INTENT_SENDER_REQUEST
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.EXTRA_SEND_INTENT_EXCEPTION
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import kotlin.math.min

/** A collection of some standard activity call contracts, as provided by android. */
class ActivityResultContracts private constructor() {
    /**
     * An [ActivityResultContract] that doesn't do any type conversion, taking raw [Intent] as an
     * input and [ActivityResult] as an output.
     *
     * Can be used with [androidx.activity.result.ActivityResultCaller.registerForActivityResult] to
     * avoid having to manage request codes when calling an activity API for which a type-safe
     * contract is not available.
     */
    class StartActivityForResult : ActivityResultContract<Intent, ActivityResult>() {

        companion object {
            /**
             * Key for the extra containing a [android.os.Bundle] generated from
             * [androidx.core.app.ActivityOptionsCompat.toBundle] or
             * [android.app.ActivityOptions.toBundle].
             *
             * This will override any [androidx.core.app.ActivityOptionsCompat] passed to
             * [androidx.activity.result.ActivityResultLauncher.launch]
             */
            const val EXTRA_ACTIVITY_OPTIONS_BUNDLE =
                "androidx.activity.result.contract.extra.ACTIVITY_OPTIONS_BUNDLE"
        }

        override fun createIntent(context: Context, input: Intent): Intent = input

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult =
            ActivityResult(resultCode, intent)
    }

    /**
     * An [ActivityResultContract] that calls [Activity.startIntentSender].
     *
     * This [ActivityResultContract] takes an [IntentSenderRequest], which must be constructed using
     * an [IntentSenderRequest.Builder].
     *
     * If the call to [Activity.startIntentSenderForResult] throws an
     * [android.content.IntentSender.SendIntentException] the
     * [androidx.activity.result.ActivityResultCallback] will receive an [ActivityResult] with an
     * [Activity.RESULT_CANCELED] `resultCode` and whose intent has the [action][Intent.getAction]
     * of [ACTION_INTENT_SENDER_REQUEST] and an extra [EXTRA_SEND_INTENT_EXCEPTION] that contains
     * the thrown exception.
     */
    class StartIntentSenderForResult :
        ActivityResultContract<IntentSenderRequest, ActivityResult>() {

        companion object {
            /**
             * An [Intent] action for making a request via the [Activity.startIntentSenderForResult]
             * API.
             */
            const val ACTION_INTENT_SENDER_REQUEST =
                "androidx.activity.result.contract.action.INTENT_SENDER_REQUEST"

            /**
             * Key for the extra containing the [IntentSenderRequest].
             *
             * @see ACTION_INTENT_SENDER_REQUEST
             */
            const val EXTRA_INTENT_SENDER_REQUEST =
                "androidx.activity.result.contract.extra.INTENT_SENDER_REQUEST"

            /**
             * Key for the extra containing the [android.content.IntentSender.SendIntentException]
             * if the call to [Activity.startIntentSenderForResult] fails.
             */
            const val EXTRA_SEND_INTENT_EXCEPTION =
                "androidx.activity.result.contract.extra.SEND_INTENT_EXCEPTION"
        }

        override fun createIntent(context: Context, input: IntentSenderRequest): Intent {
            return Intent(ACTION_INTENT_SENDER_REQUEST).putExtra(EXTRA_INTENT_SENDER_REQUEST, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult =
            ActivityResult(resultCode, intent)
    }

    /** An [ActivityResultContract] to [request permissions][Activity.requestPermissions] */
    class RequestMultiplePermissions :
        ActivityResultContract<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>() {

        companion object {
            /**
             * An [Intent] action for making a permission request via a regular
             * [Activity.startActivityForResult] API.
             *
             * Caller must provide a `String[]` extra [EXTRA_PERMISSIONS]
             *
             * Result will be delivered via [Activity.onActivityResult] with `String[]`
             * [EXTRA_PERMISSIONS] and `int[]` [EXTRA_PERMISSION_GRANT_RESULTS], similar to
             * [Activity.onRequestPermissionsResult]
             *
             * @see Activity.requestPermissions
             * @see Activity.onRequestPermissionsResult
             */
            const val ACTION_REQUEST_PERMISSIONS =
                "androidx.activity.result.contract.action.REQUEST_PERMISSIONS"

            /**
             * Key for the extra containing all the requested permissions.
             *
             * @see ACTION_REQUEST_PERMISSIONS
             */
            const val EXTRA_PERMISSIONS = "androidx.activity.result.contract.extra.PERMISSIONS"

            /**
             * Key for the extra containing whether permissions were granted.
             *
             * @see ACTION_REQUEST_PERMISSIONS
             */
            const val EXTRA_PERMISSION_GRANT_RESULTS =
                "androidx.activity.result.contract.extra.PERMISSION_GRANT_RESULTS"

            internal fun createIntent(input: Array<String>): Intent {
                return Intent(ACTION_REQUEST_PERMISSIONS).putExtra(EXTRA_PERMISSIONS, input)
            }
        }

        override fun createIntent(context: Context, input: Array<String>): Intent {
            return createIntent(input)
        }

        override fun getSynchronousResult(
            context: Context,
            input: Array<String>,
        ): SynchronousResult<Map<String, Boolean>>? {
            if (input.isEmpty()) {
                return SynchronousResult(emptyMap())
            }
            val allGranted =
                input.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) ==
                        PackageManager.PERMISSION_GRANTED
                }
            return if (allGranted) {
                SynchronousResult(input.associate { it to true })
            } else null
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Map<String, Boolean> {
            if (resultCode != Activity.RESULT_OK) return emptyMap()
            if (intent == null) return emptyMap()
            val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)
            val grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS)
            if (grantResults == null || permissions == null) return emptyMap()
            val grantState =
                grantResults.map { result -> result == PackageManager.PERMISSION_GRANTED }
            return permissions.filterNotNull().zip(grantState).toMap()
        }
    }

    /** An [ActivityResultContract] to [request a permission][Activity.requestPermissions] */
    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun createIntent(context: Context, input: String): Intent {
            return RequestMultiplePermissions.createIntent(arrayOf(input))
        }

        @Suppress("AutoBoxing")
        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            if (intent == null || resultCode != Activity.RESULT_OK) return false
            val grantResults =
                intent.getIntArrayExtra(RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS)
            return grantResults?.any { result -> result == PackageManager.PERMISSION_GRANTED } ==
                true
        }

        override fun getSynchronousResult(
            context: Context,
            input: String,
        ): SynchronousResult<Boolean>? {
            val granted =
                ContextCompat.checkSelfPermission(context, input) ==
                    PackageManager.PERMISSION_GRANTED
            return if (granted) {
                SynchronousResult(true)
            } else {
                // proceed with permission request
                null
            }
        }
    }

    /**
     * An [ActivityResultContract] to [take small a picture][MediaStore.ACTION_IMAGE_CAPTURE]
     * preview, returning it as a [Bitmap].
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class TakePicturePreview : ActivityResultContract<Void?, Bitmap?>() {
        @CallSuper
        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Void?,
        ): SynchronousResult<Bitmap?>? = null

        @Suppress("DEPRECATION")
        final override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.getParcelableExtra("data")
        }
    }

    /**
     * An [ActivityResultContract] to [take a picture][MediaStore.ACTION_IMAGE_CAPTURE] saving it
     * into the provided content-[Uri].
     *
     * Returns `true` if the image was saved into the given [Uri].
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class TakePicture : ActivityResultContract<Uri, Boolean>() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Uri,
        ): SynchronousResult<Boolean>? = null

        @Suppress("AutoBoxing")
        final override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK
        }
    }

    /**
     * An [ActivityResultContract] to [take a video][MediaStore.ACTION_VIDEO_CAPTURE] saving it into
     * the provided content-[Uri].
     *
     * Returns a thumbnail.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    @Deprecated(
        """The thumbnail bitmap is rarely returned and is not a good signal to determine
      whether the video was actually successfully captured. Use {@link CaptureVideo} instead."""
    )
    open class TakeVideo : ActivityResultContract<Uri, Bitmap?>() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            return Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Uri,
        ): SynchronousResult<Bitmap?>? = null

        @Suppress("DEPRECATION")
        final override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.getParcelableExtra("data")
        }
    }

    /**
     * An [ActivityResultContract] to [take a video][MediaStore.ACTION_VIDEO_CAPTURE] saving it into
     * the provided content-[Uri].
     *
     * Returns `true` if the video was saved into the given [Uri].
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class CaptureVideo : ActivityResultContract<Uri, Boolean>() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            return Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Uri,
        ): SynchronousResult<Boolean>? = null

        @Suppress("AutoBoxing")
        final override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK
        }
    }

    /**
     * An [ActivityResultContract] to request the user to pick a contact from the contacts app.
     *
     * The result is a `content:` [Uri].
     *
     * @see ContactsContract
     */
    class PickContact : ActivityResultContract<Void?, Uri?>() {
        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(Intent.ACTION_PICK).setType(ContactsContract.Contacts.CONTENT_TYPE)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to pick a piece of content, receiving a
     * `content://` [Uri] for that content that allows you to use
     * [android.content.ContentResolver.openInputStream] to access the raw data. By default, this
     * adds [Intent.CATEGORY_OPENABLE] to only return content that can be represented as a stream.
     *
     * The input is the mime type to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class GetContent : ActivityResultContract<String, Uri?>() {
        @CallSuper
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: String,
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to pick one or more a pieces of content,
     * receiving a `content://` [Uri] for each piece of content that allows you to use
     * [android.content.ContentResolver.openInputStream] to access the raw data. By default, this
     * adds [Intent.CATEGORY_OPENABLE] to only return content that can be represented as a stream.
     *
     * The input is the mime type to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class GetMultipleContents :
        ActivityResultContract<String, List<@JvmSuppressWildcards Uri>>() {
        @CallSuper
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(input)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: String,
        ): SynchronousResult<List<Uri>>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.getClipDataUris()
                ?: emptyList()
        }

        internal companion object {
            internal fun Intent.getClipDataUris(): List<Uri> {
                // Use a LinkedHashSet to maintain any ordering that may be
                // present in the ClipData
                val resultSet = LinkedHashSet<Uri>()
                data?.let { data -> resultSet.add(data) }
                val clipData = clipData
                if (clipData == null && resultSet.isEmpty()) {
                    return emptyList()
                } else if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if (uri != null) {
                            resultSet.add(uri)
                        }
                    }
                }
                return ArrayList(resultSet)
            }
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to open a document, receiving its contents as
     * a `file:/http:/content:` [Uri].
     *
     * The input is the mime types to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     *
     * @see DocumentsContract
     */
    open class OpenDocument : ActivityResultContract<Array<String>, Uri?>() {
        @CallSuper
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT)
                .putExtra(Intent.EXTRA_MIME_TYPES, input)
                .setType("*/*")
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Array<String>,
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to open (possibly multiple) documents,
     * receiving their contents as `file:/http:/content:` [Uri]s.
     *
     * The input is the mime types to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     *
     * @see DocumentsContract
     */
    open class OpenMultipleDocuments :
        ActivityResultContract<Array<String>, List<@JvmSuppressWildcards Uri>>() {
        @CallSuper
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT)
                .putExtra(Intent.EXTRA_MIME_TYPES, input)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .setType("*/*")
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Array<String>,
        ): SynchronousResult<List<Uri>>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.getClipDataUris()
                ?: emptyList()
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to select a directory, returning the user
     * selection as a [Uri]. Apps can fully manage documents within the returned directory.
     *
     * The input is an optional [Uri] of the initial starting location.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     *
     * @see Intent.ACTION_OPEN_DOCUMENT_TREE
     * @see DocumentsContract.buildDocumentUriUsingTree
     * @see DocumentsContract.buildChildDocumentsUriUsingTree
     */
    @RequiresApi(21)
    open class OpenDocumentTree : ActivityResultContract<Uri?, Uri?>() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri?): Intent {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input != null) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
            }
            return intent
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Uri?,
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to select a path for creating a new document
     * of the given [mimeType], returning the `content:` [Uri] of the item that was created.
     *
     * The input is the suggested name for the new file.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class CreateDocument(private val mimeType: String) :
        ActivityResultContract<String, Uri?>() {

        @Deprecated(
            "Using a wildcard mime type with CreateDocument is not recommended as it breaks " +
                "the automatic handling of file extensions. Instead, specify the mime type by " +
                "using the constructor that takes an concrete mime type (e.g.., " +
                "CreateDocument(\"image/png\")).",
            ReplaceWith("CreateDocument(\"todo/todo\")"),
        )
        constructor() : this("*/*")

        @CallSuper
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_TITLE, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: String,
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to use the
     * [Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker) to
     * select a single image, video, or other type of visual media.
     *
     * This contract always prefers the system framework provided Photo Picker available via
     * [MediaStore.ACTION_PICK_IMAGES] when it is available, but will also provide a fallback on
     * devices that it is not available to ensure a consistent API surface across all Android API 19
     * or higher devices.
     *
     * The priority order for handling the Photo Picker is:
     * 1. The system framework provided [MediaStore.ACTION_PICK_IMAGES].
     * - An OEM can provide a system app that implements [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] to
     *   provide a consistent Photo Picker to older devices.
     * - [Intent.ACTION_OPEN_DOCUMENT] is used as a final fallback on all Android API 19 or higher
     *   devices.
     *
     * The input is a [PickVisualMediaRequest].
     *
     * The output is a `Uri` when the user has selected a media or `null` when the user hasn't
     * selected any item. Keep in mind that `Uri` returned by the photo picker isn't writable.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class PickVisualMedia : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
        companion object {
            /**
             * Check if the current device has support for the photo picker by checking the running
             * Android version or the SDK extension version.
             *
             * Note that this does not check for any Intent handled by
             * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
             */
            @SuppressLint("NewApi")
            @Deprecated(
                message =
                    "This method is deprecated in favor of isPhotoPickerAvailable(context) " +
                        "to support the picker provided by updatable system apps",
                replaceWith = ReplaceWith("isPhotoPickerAvailable(context)"),
            )
            @JvmStatic
            fun isPhotoPickerAvailable(): Boolean {
                return isSystemPickerAvailable()
            }

            /**
             * In cases where the system framework provided [MediaStore.ACTION_PICK_IMAGES] Photo
             * Picker cannot be implemented, OEMs or system apps can provide a consistent Photo
             * Picker experience to those devices by creating an Activity that handles this action.
             * This app must also include [Intent.CATEGORY_DEFAULT] in the activity's intent filter.
             *
             * Only system apps can implement this action - any non-system apps will be ignored when
             * searching for the activities that handle this Intent.
             *
             * Note: this should not be used directly, instead relying on the selection logic done
             * by [createIntent] to create the correct Intent for the current device.
             */
            @Suppress("ActionValue") /* Don't include SYSTEM_FALLBACK in the action */
            const val ACTION_SYSTEM_FALLBACK_PICK_IMAGES =
                "androidx.activity.result.contract.action.PICK_IMAGES"

            internal const val GMS_ACTION_PICK_IMAGES =
                "com.google.android.gms.provider.action.PICK_IMAGES"
            internal const val GMS_EXTRA_PICK_IMAGES_MAX =
                "com.google.android.gms.provider.extra.PICK_IMAGES_MAX"

            /**
             * Extra that will be sent by [PickMultipleVisualMedia] to an Activity that handles
             * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates that maximum number of photos the
             * user should select.
             *
             * If this extra is not present, only a single photo should be selectable.
             *
             * If this extra is present but equal to [Int.MAX_VALUE], then no limit should be
             * enforced.
             */
            @Suppress("ActionValue") /* Don't include SYSTEM_FALLBACK in the extra */
            const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX =
                "androidx.activity.result.contract.extra.PICK_IMAGES_MAX"

            /**
             * Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an
             * Activity that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates the
             * preferred default tab of the picker.
             *
             * If this extra is not present, the default tab of the picker will be used.
             */
            @Suppress("ActionValue")
            /* Don't include SYSTEM_FALLBACK in the extra */
            const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_LAUNCH_TAB =
                "androidx.activity.result.contract.extra.PICK_IMAGES_LAUNCH_TAB"

            /**
             * Extra that will be sent by [PickMultipleVisualMedia] to an Activity that handles
             * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates allowing the user to control the
             * order in which images are returned to the calling app.
             */
            @Suppress("ActionValue")
            /* Don't include SYSTEM_FALLBACK in the extra */
            const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_IN_ORDER =
                "androidx.activity.result.contract.extra.PICK_IMAGES_IN_ORDER"

            /**
             * Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an
             * Activity that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates the
             * preferred accent color of the picker.
             *
             * If this extra is not present, the default accent color of the picker will be used.
             */
            @Suppress("ActionValue")
            /* Don't include SYSTEM_FALLBACK in the extra */
            const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_ACCENT_COLOR =
                "androidx.activity.result.contract.extra.PICK_IMAGES_ACCENT_COLOR"

            /**
             * Check if the current device has support for the photo picker by checking the running
             * Android version, the SDK extension version or the picker provided by a system app
             * implementing [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
             */
            @SuppressLint("NewApi")
            @JvmStatic
            fun isPhotoPickerAvailable(context: Context): Boolean {
                return isSystemPickerAvailable() || isSystemFallbackPickerAvailable(context)
            }

            /**
             * Check if the current device has support for the system framework provided photo
             * picker by checking the running Android version or the SDK extension version.
             *
             * Note that this does not check for any Intent handled by
             * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
             */
            @SuppressLint("NewApi")
            @JvmStatic
            internal fun isSystemPickerAvailable(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    true
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // getExtension is seen as part of Android Tiramisu only while the SdkExtensions
                    // have been added on Android R
                    getExtensionVersion(Build.VERSION_CODES.R) >= 2
                } else {
                    false
                }
            }

            @JvmStatic
            internal fun isSystemFallbackPickerAvailable(context: Context): Boolean {
                return getSystemFallbackPicker(context) != null
            }

            @Suppress("DEPRECATION")
            @JvmStatic
            internal fun getSystemFallbackPicker(context: Context): ResolveInfo? {
                return context.packageManager.resolveActivity(
                    Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES),
                    PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY,
                )
            }

            internal fun getVisualMimeType(input: VisualMediaType): String? {
                return when (input) {
                    is ImageOnly -> "image/*"
                    is VideoOnly -> "video/*"
                    is SingleMimeType -> input.mimeType
                    is ImageAndVideo -> null
                }
            }
        }

        /** Represents filter input type accepted by the photo picker. */
        sealed interface VisualMediaType

        /** [VisualMediaType] object used to filter images only when using the photo picker. */
        object ImageOnly : VisualMediaType

        /** [VisualMediaType] object used to filter video only when using the photo picker. */
        object VideoOnly : VisualMediaType

        /** [VisualMediaType] object used to filter images and video when using the photo picker. */
        object ImageAndVideo : VisualMediaType

        /**
         * [VisualMediaType] class used to filter a single mime type only when using the photo
         * picker.
         */
        class SingleMimeType(val mimeType: String) : VisualMediaType

        /**
         * Represents the media capabilities of an application.
         *
         * This class allows you to specify the media capabilities that your application can handle,
         * such as the HDR type of the media. By providing this information to
         * [PickVisualMediaRequest], the photo picker can provide a more appropriate media format
         * when possible.
         *
         * @see PickVisualMediaRequest.Builder.setMediaCapabilitiesForTranscoding
         */
        class MediaCapabilities internal constructor() {

            companion object {
                /** Defines the type of HDR (high dynamic range). */
                @Retention(AnnotationRetention.SOURCE)
                @IntDef(TYPE_HLG10, TYPE_HDR10, TYPE_HDR10_PLUS, TYPE_DOLBY_VISION)
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @Target(
                    AnnotationTarget.TYPE,
                    AnnotationTarget.PROPERTY,
                    AnnotationTarget.VALUE_PARAMETER,
                )
                annotation class HdrType

                /** HDR type for HLG10. */
                const val TYPE_HLG10 = 0
                /** HDR type for HDR10. */
                const val TYPE_HDR10 = 1
                /** HDR type for HDR10+. */
                const val TYPE_HDR10_PLUS = 2
                /** HDR type for Dolby-Vision. */
                const val TYPE_DOLBY_VISION = 3
            }

            var supportedHdrTypes: Set<@HdrType Int> = emptySet()
                internal set

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            internal fun toApplicationMediaCapabilities(): ApplicationMediaCapabilities {
                return ApplicationMediaCapabilities.Builder()
                    .apply {
                        addSupportedVideoMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC)
                        supportedHdrTypes.forEach {
                            when (it) {
                                TYPE_HLG10 -> addSupportedHdrType(HLG)
                                TYPE_HDR10 -> addSupportedHdrType(HDR10)
                                TYPE_HDR10_PLUS -> addSupportedHdrType(HDR10_PLUS)
                                TYPE_DOLBY_VISION -> addSupportedHdrType(DOLBY_VISION)
                            }
                        }
                    }
                    .build()
            }

            /** A builder for constructing [MediaCapabilities] instances. */
            class Builder {

                private var supportedHdrTypes: MutableSet<@HdrType Int> = mutableSetOf()

                /**
                 * Adds the supported HDR (High Dynamic Range) types for media capabilities.
                 *
                 * @param hdrType A supported HDR type from the [HdrType].
                 * @return This Builder.
                 * @throws IllegalArgumentException if an invalid hdrType is provided.
                 */
                fun addSupportedHdrType(hdrType: @HdrType Int): Builder {
                    this.supportedHdrTypes.add(hdrType)
                    return this
                }

                /**
                 * Build the MediaCapabilities specified by this builder.
                 *
                 * @return the newly constructed MediaCapabilities.
                 */
                fun build(): MediaCapabilities =
                    MediaCapabilities().apply {
                        this.supportedHdrTypes = this@Builder.supportedHdrTypes
                    }
            }
        }

        /** Represents filter input type accepted by the photo picker. */
        abstract class DefaultTab private constructor() {
            abstract val value: Int

            /**
             * [DefaultTab] object used to open the picker in Photos tab (also the default if no
             * value is provided).
             */
            object PhotosTab : DefaultTab() {
                override val value = MediaStore.PICK_IMAGES_TAB_IMAGES
            }

            /** [DefaultTab] object used to open the picker in Albums tab. */
            object AlbumsTab : DefaultTab() {
                override val value = MediaStore.PICK_IMAGES_TAB_ALBUMS
            }
        }

        @CallSuper
        override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
            // Check if Photo Picker is available on the device
            return if (isSystemPickerAvailable()) {
                Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                    type = getVisualMimeType(input.mediaType)
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB, input.defaultTab.value)

                    if (input.isCustomAccentColorApplied) {
                        putExtra(MediaStore.EXTRA_PICK_IMAGES_ACCENT_COLOR, input.accentColor)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        input.mediaCapabilitiesForTranscoding?.let { capabilities ->
                            putExtra(
                                MediaStore.EXTRA_MEDIA_CAPABILITIES,
                                capabilities.toApplicationMediaCapabilities(),
                            )
                        }
                    }
                }
            } else if (isSystemFallbackPickerAvailable(context)) {
                val fallbackPicker = checkNotNull(getSystemFallbackPicker(context)).activityInfo
                Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                    setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                    type = getVisualMimeType(input.mediaType)
                    putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_LAUNCH_TAB, input.defaultTab.value)

                    if (input.isCustomAccentColorApplied) {
                        putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_ACCENT_COLOR, input.accentColor)
                    }
                }
            } else {
                // For older devices running KitKat and higher and devices running Android 12
                // and 13 without the SDK extension that includes the Photo Picker, rely on the
                // ACTION_OPEN_DOCUMENT intent
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = getVisualMimeType(input.mediaType)

                    if (type == null) {
                        // ACTION_OPEN_DOCUMENT requires to set this parameter when launching the
                        // intent with multiple mime types
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                    }
                }
            }
        }

        @Suppress("InvalidNullabilityOverride")
        final override fun getSynchronousResult(
            context: Context,
            input: PickVisualMediaRequest,
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent
                .takeIf { resultCode == Activity.RESULT_OK }
                ?.run {
                    // Check both the data URI and ClipData since the fallback picker
                    // may only return results through getClipDataUris()
                    data ?: getClipDataUris().firstOrNull()
                }
        }
    }

    /**
     * An [ActivityResultContract] to use the
     * [Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker) to
     * select multiple images, videos, or other types of visual media.
     *
     * This contract always prefers the system framework provided Photo Picker available via
     * [MediaStore.ACTION_PICK_IMAGES] when it is available, but will also provide a fallback on
     * devices that it is not available to provide a consistent API surface across all Android API
     * 19 or higher devices.
     *
     * The priority order for handling the Photo Picker is:
     * 1. The system framework provided [MediaStore.ACTION_PICK_IMAGES].
     * - An OEM can provide a system app that implements
     *   [PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES] to provide a consistent Photo Picker
     *   to older devices. These system apps may handle the
     *   [PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX] extra to respect the [maxItems]
     *   passed to this contract.
     * - [Intent.ACTION_OPEN_DOCUMENT] is used as a final fallback on all Android API 19 or higher
     *   devices. This Intent does not allow limiting the max items the user selects.
     *
     * The constructor accepts one parameter [maxItems] to limit the number of selectable items when
     * using the photo picker to return. When launching the activity, the minimum of [maxItems] and
     * input [PickVisualMediaRequest.maxItems] is set as the limit.
     *
     * The input is a [PickVisualMediaRequest].
     *
     * The output is a list `Uri` of the selected media. It can be empty if the user hasn't selected
     * any items. Keep in mind that `Uri` returned by the photo picker aren't writable.
     *
     * This can be extended to override [createIntent] if you wish to pass additional extras to the
     * Intent created by `super.createIntent()`.
     */
    open class PickMultipleVisualMedia(private val maxItems: Int = getMaxItems()) :
        ActivityResultContract<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>() {

        init {
            require(maxItems > 1) { "Max items must be higher than 1" }
        }

        @CallSuper
        @SuppressLint("NewApi")
        override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
            // Check to see if the photo picker is available
            return if (PickVisualMedia.isSystemPickerAvailable()) {
                Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                    type = PickVisualMedia.getVisualMimeType(input.mediaType)
                    val currentMaxItems = min(maxItems, input.maxItems)

                    require(
                        currentMaxItems > 1 && currentMaxItems <= MediaStore.getPickImagesMaxLimit()
                    ) {
                        "Max items must be greater than 1 and lesser than or equal to " +
                            "MediaStore.getPickImagesMaxLimit()"
                    }

                    putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, currentMaxItems)
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB, input.defaultTab.value)
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_IN_ORDER, input.isOrderedSelection)

                    if (input.isCustomAccentColorApplied) {
                        putExtra(MediaStore.EXTRA_PICK_IMAGES_ACCENT_COLOR, input.accentColor)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        input.mediaCapabilitiesForTranscoding?.let { capabilities ->
                            putExtra(
                                MediaStore.EXTRA_MEDIA_CAPABILITIES,
                                capabilities.toApplicationMediaCapabilities(),
                            )
                        }
                    }
                }
            } else if (PickVisualMedia.isSystemFallbackPickerAvailable(context)) {
                val fallbackPicker = checkNotNull(getSystemFallbackPicker(context)).activityInfo
                Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                    setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                    type = PickVisualMedia.getVisualMimeType(input.mediaType)

                    val currentMaxItems = min(maxItems, input.maxItems)
                    require(currentMaxItems > 1) { "Max items must be greater than 1" }

                    putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX, currentMaxItems)
                    putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_LAUNCH_TAB, input.defaultTab.value)
                    putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_IN_ORDER, input.isOrderedSelection)

                    if (input.isCustomAccentColorApplied) {
                        putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_ACCENT_COLOR, input.accentColor)
                    }
                }
            } else {
                // For older devices running KitKat and higher and devices running Android 12
                // and 13 without the SDK extension that includes the Photo Picker, rely on the
                // ACTION_OPEN_DOCUMENT intent
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = PickVisualMedia.getVisualMimeType(input.mediaType)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                    if (type == null) {
                        // ACTION_OPEN_DOCUMENT requires to set this parameter when launching the
                        // intent with multiple mime types
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                    }
                }
            }
        }

        @Suppress("InvalidNullabilityOverride")
        final override fun getSynchronousResult(
            context: Context,
            input: PickVisualMediaRequest,
        ): SynchronousResult<List<@JvmSuppressWildcards Uri>>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.getClipDataUris()
                ?: emptyList()
        }

        internal companion object {
            /**
             * The system photo picker has a maximum limit of selectable items returned by
             * [MediaStore.getPickImagesMaxLimit()] On devices supporting picker provided via
             * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES], the limit may be ignored if it's higher than
             * the allowed limit. On devices not supporting the photo picker, the limit is ignored.
             *
             * @see MediaStore.EXTRA_PICK_IMAGES_MAX
             */
            @SuppressLint("NewApi")
            internal fun getMaxItems() =
                if (PickVisualMedia.isSystemPickerAvailable()) {
                    MediaStore.getPickImagesMaxLimit()
                } else {
                    Integer.MAX_VALUE
                }
        }
    }
}
