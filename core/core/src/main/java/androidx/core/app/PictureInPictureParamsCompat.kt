/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.app

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * Helper class to access the framework [android.app.PictureInPictureParams].
 *
 * [Builder] is offered for a fluent API.
 */
public class PictureInPictureParamsCompat(
    /**
     * This field indicates the PiP-able state of the application, and it will be translated to
     * [PictureInPictureParams.isAutoEnterEnabled] on API 31+.
     */
    public val isEnabled: Boolean = true,
    /**
     * Sets the aspect ratio. This aspect ratio is defined as the desired width / height, and does
     * not change upon device rotation.
     *
     * Compatibility notes: this field is used on API 26+
     */
    public val aspectRatio: Rational? = null,
    /**
     * Sets the user actions. If there are more than
     * [ComponentActivity.getMaxNumPictureInPictureActions] actions, then the input list will be
     * truncated to that number.
     *
     * Compatibility notes: this field is used on API 26+
     */
    public val actions: List<RemoteAction> = emptyList(),
    /**
     * Sets the window-coordinate bounds of an activity transitioning to picture-in-picture. The
     * bounds is the area of an activity that will be visible in the transition to
     * picture-in-picture mode. For the best effect, these bounds should also match the aspect ratio
     * in the arguments.
     *
     * In Android 12+ these bounds are also reused to improve the exit transition from
     * picture-in-picture mode. See the
     * [Picture-in-Picture doc](https://developer.android.com/develop/ui/views/picture-in-picture#set-sourcerecthint)
     * for more details.
     *
     * Compatibility notes: this field is used on API 26+
     */
    public val sourceRectHint: Rect? = null,
    /**
     * Sets whether the system can seamlessly resize the window while the activity is in
     * picture-in-picture mode. This should normally be the case for video content and when it's set
     * to `false`, system will perform transitions to overcome the artifacts due to resize.
     *
     * Compatibility notes: this field is used on API 31+
     */
    public val isSeamlessResizeEnabled: Boolean = false,
    /**
     * Sets a close action that should be invoked before the default close PiP action. The custom
     * action must close the activity quickly using [ComponentActivity.finish]. Otherwise, the
     * system will forcibly close the PiP as if no custom close action was provided.
     *
     * If the action matches one set via [actions] it may be shown in place of that custom action in
     * the menu.
     *
     * Compatibility notes: this field is used on API 33+
     */
    public val closeAction: RemoteAction? = null,
    /**
     * Sets the aspect ratio for the expanded picture-in-picture mode. The aspect ratio is defined
     * as the desired width / height. The aspect ratio cannot be changed from horizontal to vertical
     * or vertical to horizontal while the PIP is shown. Any such changes will be ignored.
     *
     * Setting the expanded ratio shows the activity's support for expanded mode.
     *
     * Compatibility notes: this field is used on API 33+
     */
    public val expandedAspectRatio: Rational? = null,
    /**
     * Sets a title for the picture-in-picture window, which may be displayed by the system to give
     * the user information about what this PIP is generally being used for.
     *
     * Compatibility notes: this field is used on API 33+
     */
    public val title: CharSequence? = null,
    /**
     * Sets a subtitle for the picture-in-picture window, which may be displayed by the system to
     * give the user more detailed information about what this PIP is displaying.
     *
     * Compatibility notes: this field is used on API 33+
     */
    public val subTitle: CharSequence? = null,
) {
    /**
     * Provides the [PictureInPictureParams] represented by this object.
     *
     * This method is not supported on devices running SDK < 26 since the platform class will not be
     * available.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @RequiresApi(Build.VERSION_CODES.O)
    @NonNull
    public fun toPictureInPictureParams(): PictureInPictureParams {
        val pictureInPictureParams: PictureInPictureParams
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pictureInPictureParams =
                Api33Impl.create(
                    aspectRatio = aspectRatio,
                    actions = actions,
                    sourceRectHint = sourceRectHint,
                    autoEnterEnabled = isEnabled,
                    seamlessResizeEnabled = isSeamlessResizeEnabled,
                    expandedAspectRatio = expandedAspectRatio,
                    closeAction = closeAction,
                    title = title,
                    subTitle = subTitle,
                )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pictureInPictureParams =
                Api31Impl.create(
                    aspectRatio = aspectRatio,
                    actions = actions,
                    sourceRectHint = sourceRectHint,
                    autoEnterEnabled = isEnabled,
                    seamlessResizeEnabled = isSeamlessResizeEnabled,
                )
        } else {
            pictureInPictureParams =
                Api26Impl.create(
                    aspectRatio = aspectRatio,
                    actions = actions,
                    sourceRectHint = sourceRectHint,
                )
        }
        return pictureInPictureParams
    }

    /** Builder class for [PictureInPictureParamsCompat]. */
    public class Builder {
        private var enabled: Boolean = true
        private var aspectRatio: Rational? = null
        private var actions: List<RemoteAction> = emptyList()
        private var sourceRectHint: Rect? = null
        private var seamlessResizeEnabled: Boolean = false
        private var closeAction: RemoteAction? = null
        private var expandedAspectRatio: Rational? = null
        private var title: CharSequence? = null
        private var subTitle: CharSequence? = null

        /** Sets [PictureInPictureParamsCompat.isEnabled]. */
        public fun setEnabled(enabled: Boolean): Builder = apply { this.enabled = enabled }

        /** Sets [PictureInPictureParamsCompat.aspectRatio]. */
        public fun setAspectRatio(aspectRatio: Rational?): Builder = apply {
            this.aspectRatio = aspectRatio
        }

        /** Sets [PictureInPictureParamsCompat.actions]. */
        public fun setActions(actions: List<RemoteAction>): Builder = apply {
            this.actions = actions
        }

        /** Sets [PictureInPictureParamsCompat.sourceRectHint]. */
        public fun setSourceRectHint(sourceRectHint: Rect?): Builder = apply {
            this.sourceRectHint = sourceRectHint
        }

        /** Sets [PictureInPictureParamsCompat.isSeamlessResizeEnabled]. */
        public fun setSeamlessResizeEnabled(seamlessResizeEnabled: Boolean): Builder = apply {
            this.seamlessResizeEnabled = seamlessResizeEnabled
        }

        /** Sets [PictureInPictureParamsCompat.closeAction]. */
        public fun setCloseAction(closeAction: RemoteAction?): Builder = apply {
            this.closeAction = closeAction
        }

        /** Sets [PictureInPictureParamsCompat.expandedAspectRatio]. */
        public fun setExpandedAspectRatio(expandedAspectRatio: Rational?): Builder = apply {
            this.expandedAspectRatio = expandedAspectRatio
        }

        /** Sets [PictureInPictureParamsCompat.title]. */
        public fun setTitle(title: CharSequence?): Builder = apply { this.title = title }

        /** Sets [PictureInPictureParamsCompat.subTitle]. */
        public fun setSubTitle(subTitle: CharSequence?): Builder = apply {
            this.subTitle = subTitle
        }

        /** Builds [PictureInPictureParamsCompat] instance. */
        public fun build(): PictureInPictureParamsCompat {
            return PictureInPictureParamsCompat(
                isEnabled = enabled,
                aspectRatio = aspectRatio,
                actions = actions,
                sourceRectHint = sourceRectHint,
                isSeamlessResizeEnabled = seamlessResizeEnabled,
                closeAction = closeAction,
                expandedAspectRatio = expandedAspectRatio,
                title = title,
                subTitle = subTitle,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private object Api26Impl {
        @JvmStatic
        fun create(
            aspectRatio: Rational?,
            actions: List<RemoteAction>,
            sourceRectHint: Rect?,
        ): PictureInPictureParams {
            return PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setActions(actions)
                .setSourceRectHint(sourceRectHint)
                .build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31Impl {
        @JvmStatic
        fun create(
            aspectRatio: Rational?,
            actions: List<RemoteAction>,
            sourceRectHint: Rect?,
            autoEnterEnabled: Boolean,
            seamlessResizeEnabled: Boolean,
        ): PictureInPictureParams {
            return PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setActions(actions)
                .setSourceRectHint(sourceRectHint)
                .setAutoEnterEnabled(autoEnterEnabled)
                .setSeamlessResizeEnabled(seamlessResizeEnabled)
                .build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33Impl {
        @JvmStatic
        fun create(
            aspectRatio: Rational?,
            actions: List<RemoteAction>,
            sourceRectHint: Rect?,
            autoEnterEnabled: Boolean,
            seamlessResizeEnabled: Boolean,
            expandedAspectRatio: Rational?,
            closeAction: RemoteAction?,
            title: CharSequence?,
            subTitle: CharSequence?,
        ): PictureInPictureParams {
            return PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setActions(actions)
                .setSourceRectHint(sourceRectHint)
                .setAutoEnterEnabled(autoEnterEnabled)
                .setSeamlessResizeEnabled(seamlessResizeEnabled)
                .setExpandedAspectRatio(expandedAspectRatio)
                .setCloseAction(closeAction)
                .setTitle(title)
                .setSubtitle(subTitle)
                .build()
        }
    }
}
