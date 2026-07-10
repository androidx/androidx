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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles

public class RemoteComposeContextAndroid : RemoteComposeContext {

    public val painter: Painter
        get() {
            if (mRemoteWriter !is RemoteComposeWriterAndroid) {
                throw (Exception("This RemoteComposeContext is not an Android one"))
            }
            return (mRemoteWriter as RemoteComposeWriterAndroid).painter
        }

    public constructor(
        creationDisplayInfo: CreationDisplayInfo,
        contentDescription: String,
        profile: Profile,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(creationDisplayInfo, contentDescription, profile, null)) {
        content()
    }

    public constructor(
        profile: Profile,
        vararg tags: RemoteComposeWriter.HTag,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(profile, *tags)) {
        content()
    }

    public constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        platform: RcPlatformServices,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(width, height, contentDescription, platform)) {
        content()
    }

    public constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        apiLevel: Int,
        profiles: Int,
        platform: RcPlatformServices,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(
        RemoteComposeWriterAndroid(width, height, contentDescription, apiLevel, profiles, platform)
    ) {
        content()
    }

    public constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(width, height, contentDescription, profile) {
        content()
    }

    public constructor(
        platform: RcPlatformServices,
        vararg tags: RemoteComposeWriter.HTag,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(platform, *tags)) {
        content()
    }

    public constructor(
        platform: RcPlatformServices,
        apiLevel: Int,
        vararg tags: RemoteComposeWriter.HTag,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(platform, apiLevel, *tags)) {
        content()
    }

    public fun addBitmap(image: Bitmap): Int {
        return mRemoteWriter.addBitmap(image)
    }

    public fun drawBitmap(image: Bitmap, contentDescription: String) {
        mRemoteWriter.drawBitmap(image, image.width, image.height, contentDescription)
    }

    public fun createCirclePath(x: Float, y: Float, rad: Float): RemotePath {
        return RemotePath.createCirclePath(mRemoteWriter, x, y, rad)
    }

    public fun drawRoundRect(
        x: Number,
        y: Number,
        w: Number,
        h: Number,
        radX: Number,
        radY: Number,
    ) {
        drawRoundRect(
            x.toFloat(),
            y.toFloat(),
            w.toFloat(),
            h.toFloat(),
            radX.toFloat(),
            radY.toFloat(),
        )
    }

    public fun drawRect(x: Number, y: Number, w: Number, h: Number) {
        drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
    }

    public fun drawLine(x1: Number, y1: Number, x2: Number, y2: Number) {
        drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
    }

    public fun save(content: RemoteComposeContextAndroid.() -> Unit) {
        save()
        content()
        restore()
    }

    public fun rotate(angle: Number) {
        rotate(angle.toFloat())
    }

    public fun rotate(angle: Number, x: Number, y: Number) {
        rotate(angle.toFloat(), x.toFloat(), y.toFloat())
    }

    public fun scale(sx: Number, sy: Number, centerX: Number, centerY: Number) {
        scale(sx.toFloat(), sy.toFloat(), centerX.toFloat(), centerX.toFloat())
    }

    public fun drawArc(
        left: Number,
        top: Number,
        right: Number,
        bottom: Number,
        startAngle: Number,
        sweepAngle: Number,
    ) {
        drawArc(
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            startAngle.toFloat(),
            sweepAngle.toFloat(),
        )
    }

    public fun drawSector(
        left: Number,
        top: Number,
        right: Number,
        bottom: Number,
        startAngle: Number,
        sweepAngle: Number,
    ) {
        drawSector(
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            startAngle.toFloat(),
            sweepAngle.toFloat(),
        )
    }

    public fun drawTextAnchored(
        id: Int,
        x: Number,
        y: Number,
        panX: Number,
        panY: Number,
        flags: Int,
    ) {
        drawTextAnchored(id, x.toFloat(), y.toFloat(), panX.toFloat(), panY.toFloat(), flags)
    }

    public fun drawTextAnchored(
        str: String,
        x: Number,
        y: Number,
        panX: Number,
        panY: Number,
        flags: Int,
    ) {
        drawTextAnchored(str, x.toFloat(), y.toFloat(), panX.toFloat(), panY.toFloat(), flags)
    }

    public fun loop(from: Float, step: Float, until: Float, content: RcFloatArgumentCallback) {
        val indexId = createID(0)
        mRemoteWriter.startLoop(indexId, from, step, until)
        content.run(rf(Utils.asNan(indexId)))
        endLoop()
    }

    public fun loop(
        fromN: Number,
        stepN: Number,
        untilN: Number,
        content: RcFloatArgumentCallback,
    ) {
        val from = fromN as? Float ?: fromN.toFloat()
        val step = stepN as? Float ?: stepN.toFloat()
        val until = untilN as? Float ?: untilN.toFloat()

        val indexId = createID(0)
        mRemoteWriter.startLoop(indexId, from, step, until)
        content.run(rf(Utils.asNan(indexId)))
        endLoop()
    }

    public fun createTextFromFloat(value: RFloat, before: Int, after: Int, flags: Int): Int {
        return mRemoteWriter.createTextFromFloat(value.toFloat(), before, after, flags)
    }

    /** Begin a global scope The section is moved above the root in the doc */
    public fun beginGlobal() {
        mRemoteWriter.beginGlobal()
    }

    /** End a global scope The section is moved above the root in the doc */
    public fun endGlobal() {
        mRemoteWriter.endGlobal()
    }

    /**
     * This support skipping sections of code if a condition is true The bytes between are never
     * seen by the parser
     *
     * @param type the type of condition
     * @param value the value to compare against
     */
    public fun skip(type: Short, value: Int, content: RemoteComposeContextAndroid.() -> Unit) {
        val pos = mRemoteWriter.beginSkip(type, value)
        content()
        mRemoteWriter.endSkip(pos)
    }
}
