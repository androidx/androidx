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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents.Companion.getClipDataUris
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.ACTION_INTENT_SENDER_REQUEST
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.EXTRA_SEND_INTENT_EXCEPTION
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * A collection of some standard activity call contracts, as provided by android.
 */
class ActivityResultContracts private constructor() {
    /**
     * An [ActivityResultContract] that doesn't do any type conversion, taking raw
     * [Intent] as an input and [ActivityResult] as an output.
     *
     * Can be used with [androidx.activity.result.ActivityResultCaller.registerForActivityResult]
     * to avoid having to manage request codes when calling an activity API for which a
     * type-safe contract is not available.
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

        override fun parseResult(
            resultCode: Int,
            intent: Intent?
        ): ActivityResult = ActivityResult(resultCode, intent)
    }

    /**
     * An [ActivityResultContract] that calls [Activity.startIntentSender].
     *
     * This [ActivityResultContract] takes an [IntentSenderRequest], which must be
     * constructed using an [IntentSenderRequest.Builder].
     *
     * If the call to [Activity.startIntentSenderForResult]
     * throws an [android.content.IntentSender.SendIntentException] the
     * [androidx.activity.result.ActivityResultCallback] will receive an
     * [ActivityResult] with an [Activity.RESULT_CANCELED] `resultCode` and
     * whose intent has the [action][Intent.getAction] of
     * [ACTION_INTENT_SENDER_REQUEST] and an extra [EXTRA_SEND_INTENT_EXCEPTION]
     * that contains the thrown exception.
     */
    class StartIntentSenderForResult :
        ActivityResultContract<IntentSenderRequest, ActivityResult>() {

        companion object {
            /**
             * An [Intent] action for making a request via the
             * [Activity.startIntentSenderForResult] API.
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
            return Intent(ACTION_INTENT_SENDER_REQUEST)
                .putExtra(EXTRA_INTENT_SENDER_REQUEST, input)
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?
        ): ActivityResult = ActivityResult(resultCode, intent)
    }

    /**
     * An [ActivityResultContract] to [request permissions][Activity.requestPermissions]
     */
    class RequestMultiplePermissions :
        ActivityResultContract<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>() {

        companion object {
            /**
             * An [Intent] action for making a permission request via a regular
             * [Activity.startActivityForResult] API.
             *
             * Caller must provide a `String[]` extra [EXTRA_PERMISSIONS]
             *
             * Result will be delivered via [Activity.onActivityResult] with
             * `String[]` [EXTRA_PERMISSIONS] and `int[]`
             * [EXTRA_PERMISSION_GRANT_RESULTS], similar to
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
            input: Array<String>
        ): SynchronousResult<Map<String, Boolean>>? {
            if (input.isEmpty()) {
                return SynchronousResult(emptyMap())
            }
            val allGranted = input.all { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            return if (allGranted) {
                SynchronousResult(input.associate { it to true })
            } else null
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?
        ): Map<String, Boolean> {
            if (resultCode != Activity.RESULT_OK) return emptyMap()
            if (intent == null) return emptyMap()
            val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)
            val grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS)
            if (grantResults == null || permissions == null) return emptyMap()
            val grantState = grantResults.map { result ->
                result == PackageManager.PERMISSION_GRANTED
            }
            return permissions.filterNotNull().zip(grantState).toMap()
        }
    }

    /**
     * An [ActivityResultContract] to [request a permission][Activity.requestPermissions]
     */
    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun createIntent(context: Context, input: String): Intent {
            return RequestMultiplePermissions.createIntent(arrayOf(input))
        }

        @Suppress("AutoBoxing")
        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            if (intent == null || resultCode != Activity.RESULT_OK) return false
            val grantResults =
                intent.getIntArrayExtra(RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS)
            return grantResults?.any { result ->
                result == PackageManager.PERMISSION_GRANTED
            } == true
        }

        override fun getSynchronousResult(
            context: Context,
            input: String
        ): SynchronousResult<Boolean>? {
            val granted = ContextCompat.checkSelfPermission(
                context,
                input
            ) == PackageManager.PERMISSION_GRANTED
            return if (granted) {
                SynchronousResult(true)
            } else {
                // proceed with permission request
                null
            }
        }
    }

    /**
     * An [ActivityResultContract] to
     * [take small a picture][MediaStore.ACTION_IMAGE_CAPTURE] preview, returning it as a
     * [Bitmap].
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     */
    open class TakePicturePreview : ActivityResultContract<Void?, Bitmap?>() {
        @CallSuper
        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Void?
        ): SynchronousResult<Bitmap?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.getParcelableExtra("data")
        }
    }

    /**
     * An [ActivityResultContract] to
     * [take a picture][MediaStore.ACTION_IMAGE_CAPTURE] saving it into the provided
     * content-[Uri].
     *
     * Returns `true` if the image was saved into the given [Uri].
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     */
    open class TakePicture : ActivityResultContract<Uri, Boolean>() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Uri
        ): SynchronousResult<Boolean>? = null

        @Suppress("AutoBoxing")
        final override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK
        }
    }

    /**
     * An [ActivityResultContract] to
     * [take a video][MediaStore.ACTION_VIDEO_CAPTURE] saving it into the provided
     * content-[Uri].
     *
     * Returns a thumbnail.
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     *
     */
    @Deprecated(
        """The thumbnail bitmap is rarely returned and is not a good signal to determine
      whether the video was actually successfully captured. Use {@link CaptureVideo} instead."""
    )
    open class TakeVideo : ActivityResultContract<Uri, Bitmap?>() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Uri
        ): SynchronousResult<Bitmap?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.getParcelableExtra("data")
        }
    }

    /**
     * An [ActivityResultContract] to
     * [take a video][MediaStore.ACTION_VIDEO_CAPTURE] saving it into the provided
     * content-[Uri].
     *
     * Returns `true` if the video was saved into the given [Uri].
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     */
    open class CaptureVideo : ActivityResultContract<Uri, Boolean>() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Uri
        ): SynchronousResult<Boolean>? = null

        @Suppress("AutoBoxing")
        final override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK
        }
    }

    /**
     * An [ActivityResultContract] to request the user to pick a contact from the contacts
     * app.
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
     * An [ActivityResultContract] to prompt the user to pick a piece of content, receiving
     * a `content://` [Uri] for that content that allows you to use
     * [android.content.ContentResolver.openInputStream] to access the raw data. By
     * default, this adds [Intent.CATEGORY_OPENABLE] to only return content that can be
     * represented as a stream.
     *
     * The input is the mime type to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
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
            input: String
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to pick one or more a pieces of
     * content, receiving a `content://` [Uri] for each piece of content that allows
     * you to use [android.content.ContentResolver.openInputStream]
     * to access the raw data. By default, this adds [Intent.CATEGORY_OPENABLE] to only
     * return content that can be represented as a stream.
     *
     * The input is the mime type to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     */
    @RequiresApi(18)
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
            input: String
        ): SynchronousResult<List<Uri>>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
            return intent.takeIf {
                resultCode == Activity.RESULT_OK
            }?.getClipDataUris() ?: emptyList()
        }

        @RequiresApi(18)
        internal companion object {
            internal fun Intent.getClipDataUris(): List<Uri> {
                // Use a LinkedHashSet to maintain any ordering that may be
                // present in the ClipData
                val resultSet = LinkedHashSet<Uri>()
                data?.let { data ->
                    resultSet.add(data)
                }
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
     * An [ActivityResultContract] to prompt the user to open a document, receiving its
     * contents as a `file:/http:/content:` [Uri].
     *
     * The input is the mime types to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     *
     * @see DocumentsContract
     */
    @RequiresApi(19)
    open class OpenDocument : ActivityResultContract<Array<String>, Uri?>() {
        @CallSuper
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT)
                .putExtra(Intent.EXTRA_MIME_TYPES, input)
                .setType("*/*")
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Array<String>
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to open  (possibly multiple)
     * documents, receiving their contents as `file:/http:/content:` [Uri]s.
     *
     * The input is the mime types to filter by, e.g. `image/\*`.
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     *
     * @see DocumentsContract
     */
    @RequiresApi(19)
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
            input: Array<String>
        ): SynchronousResult<List<Uri>>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
            return intent.takeIf {
                resultCode == Activity.RESULT_OK
            }?.getClipDataUris() ?: emptyList()
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to select a directory, returning the
     * user selection as a [Uri]. Apps can fully manage documents within the returned
     * directory.
     *
     * The input is an optional [Uri] of the initial starting location.
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     *
     * @see Intent.ACTION_OPEN_DOCUMENT_TREE
     *
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
            input: Uri?
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    /**
     * An [ActivityResultContract] to prompt the user to select a path for creating a new
     * document, returning the `content:` [Uri] of the item that was created.
     *
     * The input is the suggested name for the new file.
     *
     * This can be extended to override [createIntent] if you wish to pass additional
     * extras to the Intent created by `super.createIntent()`.
     */
    @RequiresApi(19)
    open class CreateDocument : ActivityResultContract<String, Uri?>() {
        @CallSuper
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("*/*")
                .putExtra(Intent.EXTRA_TITLE, input)
        }

        final override fun getSynchronousResult(
            context: Context,
            input: String
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }
}
