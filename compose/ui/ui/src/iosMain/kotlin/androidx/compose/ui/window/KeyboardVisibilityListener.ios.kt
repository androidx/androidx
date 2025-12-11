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

package androidx.compose.ui.window

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSValue
import platform.UIKit.CGRectValue
import platform.UIKit.UIKeyboardAnimationCurveUserInfoKey
import platform.UIKit.UIKeyboardAnimationDurationUserInfoKey
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIKeyboardWillChangeFrameNotification
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UIViewAnimationOptions
import platform.darwin.NSObject

internal interface KeyboardVisibilityObserver {
    fun keyboardWillShow(
        targetFrame: CValue<CGRect>,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    )

    fun keyboardWillHide(
        targetFrame: CValue<CGRect>,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    )

    fun keyboardWillChangeFrame(
        targetFrame: CValue<CGRect>,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    )
}

internal object KeyboardVisibilityListener {
    private val listener = NativeKeyboardVisibilityListener()
    private var initOnce = false
    fun initialize() {
        if (initOnce) { return }
        initOnce = true
        listener.startKeyboardChangesObserving()
    }

    fun addObserver(observer: KeyboardVisibilityObserver) = listener.observers.add(observer)

    fun removeObserver(observer: KeyboardVisibilityObserver) = listener.observers.remove(observer)

    val keyboardFrame: CValue<CGRect> get() = listener.keyboardFrame
}

private class NativeKeyboardVisibilityListener : NSObject() {
    val observers = mutableSetOf<KeyboardVisibilityObserver>()

    fun startKeyboardChangesObserving() {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::keyboardWillShow.name + ":"),
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::keyboardWillHide.name + ":"),
            name = UIKeyboardWillHideNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::keyboardWillChangeFrame.name + ":"),
            name = UIKeyboardWillChangeFrameNotification,
            `object` = null
        )
    }

    var keyboardFrame = CGRectZero.readValue()
        private set

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun keyboardWillShow(arg: NSNotification) {
        observers.forEach {
            it.keyboardWillShow(arg.endFrame, arg.duration, arg.animationOptions)
        }
        keyboardFrame = arg.endFrame
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun keyboardWillHide(arg: NSNotification) {
        observers.forEach {
            it.keyboardWillHide(CGRectZero.readValue(), arg.duration, arg.animationOptions)
        }
        keyboardFrame = CGRectZero.readValue()
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun keyboardWillChangeFrame(arg: NSNotification) {
        observers.forEach {
            it.keyboardWillChangeFrame(arg.endFrame, arg.duration, arg.animationOptions)
        }
        keyboardFrame = arg.endFrame
    }

    // Be aware that animation parameters may not correspond to the real visual keyboard
    // animation parameters.
    private val NSNotification.duration: Double
        get() {
            return userInfo?.get(UIKeyboardAnimationDurationUserInfoKey) as? Double ?: 0.0
        }

    private val NSNotification.endFrame: CValue<CGRect>
        get() {
            val value = userInfo?.get(UIKeyboardFrameEndUserInfoKey) as? NSValue
            return value?.CGRectValue() ?: CGRectZero.readValue()
        }

    private val NSNotification.animationOptions: UIViewAnimationOptions
        get() {
            val value = userInfo?.get(UIKeyboardAnimationCurveUserInfoKey) as? NSNumber
            return value?.unsignedIntegerValue() as UIViewAnimationOptions
        }
}
