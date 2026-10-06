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

package androidx.web.compose

import android.animation.StateListAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ActionMode
import android.view.OnReceiveContentListener
import android.view.PointerIcon
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewPropertyAnimator
import android.view.WindowInsetsAnimation
import android.view.animation.Animation
import android.view.contentcapture.ContentCaptureSession
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.ui.Modifier
import androidx.web.WebContent
import androidx.web.WebContentView

/**
 * Exposes [android.webkit.WebView] APIs not yet available directly on [WebContent] when presenting
 * via [WebSurface].
 *
 * When used with [WebSurface], instances of [WebViewBridge] must be created within the
 * `bridgeFactory` lambda using the [Context] provided to that factory.
 *
 * Because presentation, layout, and input are managed by [WebSurface] in Compose,
 * [android.view.View] methods are deprecated on this class and should be replaced with Compose
 * [Modifier]s or [WebContent] APIs.
 *
 * @see WebSurface
 * @see WebContent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class WebViewBridge : WebContentView {
    public constructor(context: Context) : super(context)

    // =========================================================================================
    // 1. Deprecated View APIs that are already supported via Compose Modifiers or WebContent.
    // =========================================================================================

    @Deprecated(
        message =
            "WebViewBridge should not be inflated from XML. Use WebSurface in Compose instead.",
        level = DeprecationLevel.WARNING,
    )
    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    @Deprecated(
        message =
            "WebViewBridge should not be inflated from XML. Use WebSurface in Compose instead.",
        level = DeprecationLevel.WARNING,
    )
    public constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    @Deprecated(
        message =
            "WebViewBridge lifecycle is managed by WebSurface and WebContent. Call WebContent.close() to permanently destroy the web engine.",
        level = DeprecationLevel.WARNING,
    )
    override fun destroy() {
        super.destroy()
    }

    @Deprecated(
        message = "Use conditional composition or Modifier.alpha instead of View visibility.",
        level = DeprecationLevel.WARNING,
    )
    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
    }

    @RequiresApi(29)
    @Deprecated(
        message = "Use conditional composition or Modifier.alpha instead of View visibility.",
        level = DeprecationLevel.WARNING,
    )
    override fun setTransitionVisibility(visibility: Int) {
        super.setTransitionVisibility(visibility)
    }

    @Deprecated(
        message = "Use Modifier.alpha instead of View alpha.",
        level = DeprecationLevel.WARNING,
    )
    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
    }

    @RequiresApi(29)
    @Deprecated(
        message = "Use Modifier.alpha instead of View transition alpha.",
        level = DeprecationLevel.WARNING,
    )
    override fun setTransitionAlpha(alpha: Float) {
        super.setTransitionAlpha(alpha)
    }

    @Deprecated(
        message =
            "Use Compose animation APIs and Modifier.graphicsLayer on WebSurface instead of ViewPropertyAnimator.",
        level = DeprecationLevel.WARNING,
    )
    override fun animate(): ViewPropertyAnimator {
        return super.animate()
    }

    @Deprecated(
        message =
            "Use Compose animation APIs and Modifier.graphicsLayer on WebSurface instead of View animations.",
        level = DeprecationLevel.WARNING,
    )
    override fun startAnimation(animation: Animation?) {
        super.startAnimation(animation)
    }

    @Deprecated(
        message =
            "Use Compose animation APIs and Modifier.graphicsLayer on WebSurface instead of View animations.",
        level = DeprecationLevel.WARNING,
    )
    override fun setAnimation(animation: Animation?) {
        super.setAnimation(animation)
    }

    @Deprecated(
        message =
            "Use Compose animation APIs and Modifier.graphicsLayer on WebSurface instead of View animations.",
        level = DeprecationLevel.WARNING,
    )
    override fun clearAnimation() {
        super.clearAnimation()
    }

    @RequiresApi(29)
    @Deprecated(
        message = "Use Modifier.graphicsLayer on WebSurface instead of View animation matrix.",
        level = DeprecationLevel.WARNING,
    )
    override fun setAnimationMatrix(matrix: Matrix?) {
        super.setAnimationMatrix(matrix)
    }

    @Deprecated(
        message = "Use Compose animation APIs on WebSurface instead of View StateListAnimator.",
        level = DeprecationLevel.WARNING,
    )
    override fun setStateListAnimator(stateListAnimator: StateListAnimator?) {
        super.setStateListAnimator(stateListAnimator)
    }

    @Deprecated(
        message = "Use Modifier.offset or Modifier.graphicsLayer instead of View position.",
        level = DeprecationLevel.WARNING,
    )
    override fun setX(x: Float) {
        super.setX(x)
    }

    @Deprecated(
        message = "Use Modifier.offset or Modifier.graphicsLayer instead of View position.",
        level = DeprecationLevel.WARNING,
    )
    override fun setY(y: Float) {
        super.setY(y)
    }

    @Deprecated(
        message = "Use Modifier.shadow or Modifier.graphicsLayer instead of View z-order.",
        level = DeprecationLevel.WARNING,
    )
    override fun setZ(z: Float) {
        super.setZ(z)
    }

    @Deprecated(
        message = "Use Modifier.offset or Modifier.graphicsLayer instead of View translation.",
        level = DeprecationLevel.WARNING,
    )
    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX)
    }

    @Deprecated(
        message = "Use Modifier.offset or Modifier.graphicsLayer instead of View translation.",
        level = DeprecationLevel.WARNING,
    )
    override fun setTranslationY(translationY: Float) {
        super.setTranslationY(translationY)
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View translation.",
        level = DeprecationLevel.WARNING,
    )
    override fun setTranslationZ(translationZ: Float) {
        super.setTranslationZ(translationZ)
    }

    @Deprecated(
        message = "Use Modifier.shadow or Modifier.graphicsLayer instead of View elevation.",
        level = DeprecationLevel.WARNING,
    )
    override fun setElevation(elevation: Float) {
        super.setElevation(elevation)
    }

    @RequiresApi(28)
    @Deprecated(
        message = "Use Modifier.shadow or Modifier.graphicsLayer instead of View shadow color.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOutlineSpotShadowColor(color: Int) {
        super.setOutlineSpotShadowColor(color)
    }

    @RequiresApi(28)
    @Deprecated(
        message = "Use Modifier.shadow or Modifier.graphicsLayer instead of View shadow color.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOutlineAmbientShadowColor(color: Int) {
        super.setOutlineAmbientShadowColor(color)
    }

    @Deprecated(
        message = "Use Modifier.rotate or Modifier.graphicsLayer instead of View rotation.",
        level = DeprecationLevel.WARNING,
    )
    override fun setRotation(rotation: Float) {
        super.setRotation(rotation)
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View rotation.",
        level = DeprecationLevel.WARNING,
    )
    override fun setRotationX(rotationX: Float) {
        super.setRotationX(rotationX)
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View rotation.",
        level = DeprecationLevel.WARNING,
    )
    override fun setRotationY(rotationY: Float) {
        super.setRotationY(rotationY)
    }

    @Deprecated(
        message = "Use Modifier.scale or Modifier.graphicsLayer instead of View scale.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScaleX(scaleX: Float) {
        super.setScaleX(scaleX)
    }

    @Deprecated(
        message = "Use Modifier.scale or Modifier.graphicsLayer instead of View scale.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScaleY(scaleY: Float) {
        super.setScaleY(scaleY)
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View pivot.",
        level = DeprecationLevel.WARNING,
    )
    override fun setPivotX(pivotX: Float) {
        super.setPivotX(pivotX)
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View pivot.",
        level = DeprecationLevel.WARNING,
    )
    override fun setPivotY(pivotY: Float) {
        super.setPivotY(pivotY)
    }

    @RequiresApi(28)
    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View pivot.",
        level = DeprecationLevel.WARNING,
    )
    override fun resetPivot() {
        super.resetPivot()
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View camera distance.",
        level = DeprecationLevel.WARNING,
    )
    override fun setCameraDistance(distance: Float) {
        super.setCameraDistance(distance)
    }

    @RequiresApi(31)
    @Deprecated(
        message = "Use Modifier.graphicsLayer or Modifier.blur instead of View RenderEffect.",
        level = DeprecationLevel.WARNING,
    )
    override fun setRenderEffect(renderEffect: RenderEffect?) {
        super.setRenderEffect(renderEffect)
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View layer type.",
        level = DeprecationLevel.WARNING,
    )
    override fun setLayerType(layerType: Int, paint: Paint?) {
        super.setLayerType(layerType, paint)
    }

    @Deprecated(
        message = "Use Modifier.graphicsLayer instead of View layer paint.",
        level = DeprecationLevel.WARNING,
    )
    override fun setLayerPaint(paint: Paint?) {
        super.setLayerPaint(paint)
    }

    @RequiresApi(29)
    @Deprecated(
        message =
            "View force dark is not supported by WebSurface. Configure dark mode via WebSettings or Compose theme instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun setForceDarkAllowed(allow: Boolean) {
        super.setForceDarkAllowed(allow)
    }

    @Deprecated(
        message = "Use Modifier.background instead of View background.",
        level = DeprecationLevel.WARNING,
    )
    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
    }

    @Deprecated(
        message = "Use Modifier.background instead of View background.",
        level = DeprecationLevel.WARNING,
    )
    override fun setBackgroundResource(resid: Int) {
        super.setBackgroundResource(resid)
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Use Modifier.background instead of View background.",
        level = DeprecationLevel.WARNING,
    )
    override fun setBackgroundDrawable(background: Drawable?) {
        super.setBackgroundDrawable(background)
    }

    @Deprecated(
        message = "Use Modifier.background instead of View background tint.",
        level = DeprecationLevel.WARNING,
    )
    override fun setBackgroundTintList(tint: ColorStateList?) {
        super.setBackgroundTintList(tint)
    }

    @Deprecated(
        message = "Use Modifier.background instead of View background tint mode.",
        level = DeprecationLevel.WARNING,
    )
    override fun setBackgroundTintMode(tintMode: PorterDuff.Mode?) {
        super.setBackgroundTintMode(tintMode)
    }

    @RequiresApi(29)
    @Deprecated(
        message = "Use Modifier.background instead of View background tint blend mode.",
        level = DeprecationLevel.WARNING,
    )
    override fun setBackgroundTintBlendMode(blendMode: BlendMode?) {
        super.setBackgroundTintBlendMode(blendMode)
    }

    @Deprecated(
        message = "Use Modifier.drawWithContent on WebSurface instead of View foreground.",
        level = DeprecationLevel.WARNING,
    )
    override fun setForeground(foreground: Drawable?) {
        super.setForeground(foreground)
    }

    @Deprecated(
        message = "Use Modifier.drawWithContent on WebSurface instead of View foreground gravity.",
        level = DeprecationLevel.WARNING,
    )
    override fun setForegroundGravity(gravity: Int) {
        super.setForegroundGravity(gravity)
    }

    @Deprecated(
        message = "Use Modifier.drawWithContent on WebSurface instead of View foreground tint.",
        level = DeprecationLevel.WARNING,
    )
    override fun setForegroundTintList(tint: ColorStateList?) {
        super.setForegroundTintList(tint)
    }

    @Deprecated(
        message =
            "Use Modifier.drawWithContent on WebSurface instead of View foreground tint mode.",
        level = DeprecationLevel.WARNING,
    )
    override fun setForegroundTintMode(tintMode: PorterDuff.Mode?) {
        super.setForegroundTintMode(tintMode)
    }

    @RequiresApi(29)
    @Deprecated(
        message =
            "Use Modifier.drawWithContent on WebSurface instead of View foreground tint blend mode.",
        level = DeprecationLevel.WARNING,
    )
    override fun setForegroundTintBlendMode(blendMode: BlendMode?) {
        super.setForegroundTintBlendMode(blendMode)
    }

    @Deprecated(
        message = "Use Modifier.padding on WebSurface instead of View padding.",
        level = DeprecationLevel.WARNING,
    )
    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
    }

    @Deprecated(
        message = "Use Modifier.padding on WebSurface instead of View padding.",
        level = DeprecationLevel.WARNING,
    )
    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
    }

    @Deprecated(
        message = "Use Compose size and layout Modifiers on WebSurface instead of LayoutParams.",
        level = DeprecationLevel.WARNING,
    )
    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
    }

    @Deprecated(
        message =
            "Use Modifier.sizeIn or Modifier.defaultMinSize on WebSurface instead of View minimum width.",
        level = DeprecationLevel.WARNING,
    )
    override fun setMinimumWidth(minWidth: Int) {
        super.setMinimumWidth(minWidth)
    }

    @Deprecated(
        message =
            "Use Modifier.sizeIn or Modifier.defaultMinSize on WebSurface instead of View minimum height.",
        level = DeprecationLevel.WARNING,
    )
    override fun setMinimumHeight(minHeight: Int) {
        super.setMinimumHeight(minHeight)
    }

    @Deprecated(
        message = "Use Modifier.offset on WebSurface instead of View offset.",
        level = DeprecationLevel.WARNING,
    )
    override fun offsetLeftAndRight(offset: Int) {
        super.offsetLeftAndRight(offset)
    }

    @Deprecated(
        message = "Use Modifier.offset on WebSurface instead of View offset.",
        level = DeprecationLevel.WARNING,
    )
    override fun offsetTopAndBottom(offset: Int) {
        super.offsetTopAndBottom(offset)
    }

    @Deprecated(
        message = "Use Modifier.zIndex or composition order instead of View.bringToFront.",
        level = DeprecationLevel.WARNING,
    )
    override fun bringToFront() {
        super.bringToFront()
    }

    @Deprecated(
        message =
            "Use Modifier.clip or Modifier.clipToBounds on WebSurface instead of View clip bounds.",
        level = DeprecationLevel.WARNING,
    )
    override fun setClipBounds(clipBounds: Rect?) {
        super.setClipBounds(clipBounds)
    }

    @Deprecated(
        message = "Use Modifier.clip on WebSurface instead of View clipping.",
        level = DeprecationLevel.WARNING,
    )
    override fun setClipToOutline(clipToOutline: Boolean) {
        super.setClipToOutline(clipToOutline)
    }

    @Deprecated(
        message = "Use Modifier.clip or Modifier.graphicsLayer instead of View outline provider.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOutlineProvider(provider: ViewOutlineProvider?) {
        super.setOutlineProvider(provider)
    }

    @Deprecated(
        message = "Use Modifier.clip or Modifier.graphicsLayer instead of View outline.",
        level = DeprecationLevel.WARNING,
    )
    override fun invalidateOutline() {
        super.invalidateOutline()
    }

    @Deprecated(
        message =
            "Use Modifier.clip or Modifier.clipToBounds on WebSurface instead of ViewGroup clipping.",
        level = DeprecationLevel.WARNING,
    )
    override fun setClipChildren(clipChildren: Boolean) {
        super.setClipChildren(clipChildren)
    }

    @Deprecated(
        message =
            "Use Modifier.padding and Modifier.clipToBounds on WebSurface instead of ViewGroup clipToPadding.",
        level = DeprecationLevel.WARNING,
    )
    override fun setClipToPadding(clipToPadding: Boolean) {
        super.setClipToPadding(clipToPadding)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun addView(child: View?) {
        super.addView(child)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun addView(child: View?, index: Int) {
        super.addView(child, index)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun addView(child: View?, width: Int, height: Int) {
        super.addView(child, width, height)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        super.addView(child, params)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun updateViewLayout(view: View?, params: ViewGroup.LayoutParams?) {
        super.updateViewLayout(view, params)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeView(view: View?) {
        super.removeView(view)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeViewInLayout(view: View?) {
        super.removeViewInLayout(view)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeViewsInLayout(start: Int, count: Int) {
        super.removeViewsInLayout(start, count)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeViewAt(index: Int) {
        super.removeViewAt(index)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeViews(start: Int, count: Int) {
        super.removeViews(start, count)
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeAllViews() {
        super.removeAllViews()
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeAllViewsInLayout() {
        super.removeAllViewsInLayout()
    }

    @Deprecated(
        message =
            "Child Views added to WebViewBridge are not drawn or interactive in WebSurface. Use a Compose layout (such as Box) to overlay UI on WebSurface instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener?) {
        super.setOnHierarchyChangeListener(listener)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setVerticalScrollBarEnabled(verticalScrollBarEnabled: Boolean) {
        super.setVerticalScrollBarEnabled(verticalScrollBarEnabled)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setHorizontalScrollBarEnabled(horizontalScrollBarEnabled: Boolean) {
        super.setHorizontalScrollBarEnabled(horizontalScrollBarEnabled)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScrollBarStyle(style: Int) {
        super.setScrollBarStyle(style)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScrollbarFadingEnabled(fadeScrollbars: Boolean) {
        super.setScrollbarFadingEnabled(fadeScrollbars)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScrollBarDefaultDelayBeforeFade(scrollBarDefaultDelayBeforeFade: Int) {
        super.setScrollBarDefaultDelayBeforeFade(scrollBarDefaultDelayBeforeFade)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScrollBarFadeDuration(scrollBarFadeDuration: Int) {
        super.setScrollBarFadeDuration(scrollBarFadeDuration)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScrollBarSize(scrollBarSize: Int) {
        super.setScrollBarSize(scrollBarSize)
    }

    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setVerticalScrollbarPosition(position: Int) {
        super.setVerticalScrollbarPosition(position)
    }

    @RequiresApi(29)
    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setVerticalScrollbarThumbDrawable(drawable: Drawable?) {
        super.setVerticalScrollbarThumbDrawable(drawable)
    }

    @RequiresApi(29)
    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setVerticalScrollbarTrackDrawable(drawable: Drawable?) {
        super.setVerticalScrollbarTrackDrawable(drawable)
    }

    @RequiresApi(29)
    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setHorizontalScrollbarThumbDrawable(drawable: Drawable?) {
        super.setHorizontalScrollbarThumbDrawable(drawable)
    }

    @RequiresApi(29)
    @Deprecated(
        message = "View scrollbars are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setHorizontalScrollbarTrackDrawable(drawable: Drawable?) {
        super.setHorizontalScrollbarTrackDrawable(drawable)
    }

    @Deprecated(
        message = "View fading edges are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled: Boolean) {
        super.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled)
    }

    @Deprecated(
        message = "View fading edges are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled: Boolean) {
        super.setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled)
    }

    @Deprecated(
        message = "View fading edges are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setFadingEdgeLength(length: Int) {
        super.setFadingEdgeLength(length)
    }

    @Deprecated(
        message = "View scroll indicators are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScrollIndicators(indicators: Int) {
        super.setScrollIndicators(indicators)
    }

    @Deprecated(
        message = "View scroll indicators are not drawn by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun setScrollIndicators(indicators: Int, mask: Int) {
        super.setScrollIndicators(indicators, mask)
    }

    @Deprecated(
        message = "Use Compose pointer input Modifiers on WebSurface instead of touch listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOnTouchListener(l: OnTouchListener?) {
        super.setOnTouchListener(l)
    }

    @Deprecated(
        message =
            "Use Compose pointer input Modifiers on WebSurface to filter obscured touches instead of View.setFilterTouchesWhenObscured.",
        level = DeprecationLevel.WARNING,
    )
    override fun setFilterTouchesWhenObscured(enabled: Boolean) {
        super.setFilterTouchesWhenObscured(enabled)
    }

    @Deprecated(
        message = "Use Modifier.clickable or pointer input Modifiers instead of click listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }

    @Deprecated(
        message = "Use Modifier.clickable on WebSurface instead of View clickable.",
        level = DeprecationLevel.WARNING,
    )
    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
    }

    @Deprecated(
        message = "Use Modifier.clickable on WebSurface instead of View performClick.",
        level = DeprecationLevel.WARNING,
    )
    override fun performClick(): Boolean {
        return super.performClick()
    }

    @Deprecated(
        message = "Use Modifier.clickable on WebSurface instead of View callOnClick.",
        level = DeprecationLevel.WARNING,
    )
    override fun callOnClick(): Boolean {
        return super.callOnClick()
    }

    @Deprecated(
        message = "Use Compose context menu APIs instead of View context menu listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOnCreateContextMenuListener(l: OnCreateContextMenuListener?) {
        super.setOnCreateContextMenuListener(l)
    }

    @Deprecated(
        message = "Use Compose context menu APIs instead of View context menus.",
        level = DeprecationLevel.WARNING,
    )
    override fun showContextMenu(): Boolean {
        return super.showContextMenu()
    }

    @Deprecated(
        message = "Use Compose context menu APIs instead of View context menus.",
        level = DeprecationLevel.WARNING,
    )
    override fun showContextMenu(x: Float, y: Float): Boolean {
        return super.showContextMenu(x, y)
    }

    @Deprecated(
        message =
            "Control interaction and enabled semantics via Compose Modifiers on WebSurface instead of View.setEnabled.",
        level = DeprecationLevel.WARNING,
    )
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
    }

    @Deprecated(
        message = "Use Modifier.keepScreenOn on WebSurface instead of View keepScreenOn.",
        level = DeprecationLevel.WARNING,
    )
    override fun setKeepScreenOn(keepScreenOn: Boolean) {
        super.setKeepScreenOn(keepScreenOn)
    }

    @RequiresApi(26)
    @Deprecated(
        message = "Use Compose Tooltip APIs on WebSurface instead of View tooltip text.",
        level = DeprecationLevel.WARNING,
    )
    override fun setTooltipText(tooltipText: CharSequence?) {
        super.setTooltipText(tooltipText)
    }

    @RequiresApi(35)
    @Deprecated(
        message =
            "Use Modifier.preferredFrameRate on WebSurface instead of View requestedFrameRate.",
        level = DeprecationLevel.WARNING,
    )
    override fun setRequestedFrameRate(frameRate: Float) {
        super.setRequestedFrameRate(frameRate)
    }

    @RequiresApi(35)
    @Deprecated(
        message =
            "Use Modifier.preferredFrameRate on WebSurface instead of View frameContentVelocity.",
        level = DeprecationLevel.WARNING,
    )
    override fun setFrameContentVelocity(pixelsPerSecond: Float) {
        super.setFrameContentVelocity(pixelsPerSecond)
    }

    @Deprecated(
        message = "View zoom picker is not supported by WebSurface.",
        level = DeprecationLevel.WARNING,
    )
    override fun invokeZoomPicker() {
        super.invokeZoomPicker()
    }

    @Deprecated(
        message =
            "Use Compose WindowInsets Modifiers on WebSurface instead of View fitsSystemWindows.",
        level = DeprecationLevel.WARNING,
    )
    override fun setFitsSystemWindows(fitSystemWindows: Boolean) {
        super.setFitsSystemWindows(fitSystemWindows)
    }

    @Deprecated(
        message =
            "Use Compose WindowInsets Modifiers on WebSurface instead of View window insets listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListener?) {
        super.setOnApplyWindowInsetsListener(listener)
    }

    @RequiresApi(30)
    @Deprecated(
        message =
            "Use Compose WindowInsets Modifiers on WebSurface instead of View window insets animation callbacks.",
        level = DeprecationLevel.WARNING,
    )
    override fun setWindowInsetsAnimationCallback(callback: WindowInsetsAnimation.Callback?) {
        super.setWindowInsetsAnimationCallback(callback)
    }

    @RequiresApi(29)
    @Deprecated(
        message =
            "Use Modifier.systemGestureExclusion on WebSurface instead of View system gesture exclusion rects.",
        level = DeprecationLevel.WARNING,
    )
    override fun setSystemGestureExclusionRects(rects: MutableList<Rect>) {
        super.setSystemGestureExclusionRects(rects)
    }

    @Deprecated(
        message =
            "Use Modifier.onSizeChanged or Modifier.onGloballyPositioned on WebSurface instead of View layout listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun addOnLayoutChangeListener(listener: OnLayoutChangeListener?) {
        super.addOnLayoutChangeListener(listener)
    }

    @Deprecated(
        message =
            "Use Modifier.onSizeChanged or Modifier.onGloballyPositioned on WebSurface instead of View layout listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeOnLayoutChangeListener(listener: OnLayoutChangeListener?) {
        super.removeOnLayoutChangeListener(listener)
    }

    @Deprecated(
        message =
            "Use DisposableEffect or bridgeRelease on WebSurface instead of View attach state listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun addOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
        super.addOnAttachStateChangeListener(listener)
    }

    @Deprecated(
        message =
            "Use DisposableEffect or bridgeRelease on WebSurface instead of View attach state listeners.",
        level = DeprecationLevel.WARNING,
    )
    override fun removeOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
        super.removeOnAttachStateChangeListener(listener)
    }

    // =========================================================================================
    // 2. View APIs that currently do NOT work via either View or Compose.
    // TODO: Support overscroll, nested scrolling, non-touch pointer/hover/mouse events, and
    // accessibility semantics in WebSurface / WebSurfaceChromium and deprecate these methods.
    // =========================================================================================

    override fun setOverScrollMode(mode: Int) {
        super.setOverScrollMode(mode)
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        super.setNestedScrollingEnabled(enabled)
    }

    override fun setOnContextClickListener(l: OnContextClickListener?) {
        super.setOnContextClickListener(l)
    }

    override fun setContextClickable(contextClickable: Boolean) {
        super.setContextClickable(contextClickable)
    }

    override fun setOnGenericMotionListener(l: OnGenericMotionListener?) {
        super.setOnGenericMotionListener(l)
    }

    override fun setOnHoverListener(l: OnHoverListener?) {
        super.setOnHoverListener(l)
    }

    override fun setPointerIcon(pointerIcon: PointerIcon?) {
        super.setPointerIcon(pointerIcon)
    }

    @RequiresApi(26)
    override fun setOnCapturedPointerListener(l: OnCapturedPointerListener?) {
        super.setOnCapturedPointerListener(l)
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
    }

    @RequiresApi(30)
    override fun setStateDescription(stateDescription: CharSequence?) {
        super.setStateDescription(stateDescription)
    }

    @RequiresApi(28)
    override fun setAccessibilityPaneTitle(accessibilityPaneTitle: CharSequence?) {
        super.setAccessibilityPaneTitle(accessibilityPaneTitle)
    }

    override fun setAccessibilityDelegate(delegate: AccessibilityDelegate?) {
        super.setAccessibilityDelegate(delegate)
    }

    override fun setImportantForAccessibility(mode: Int) {
        super.setImportantForAccessibility(mode)
    }

    override fun setAccessibilityLiveRegion(mode: Int) {
        super.setAccessibilityLiveRegion(mode)
    }

    @RequiresApi(28)
    override fun setAccessibilityHeading(isHeading: Boolean) {
        super.setAccessibilityHeading(isHeading)
    }

    @RequiresApi(28)
    override fun setScreenReaderFocusable(screenReaderFocusable: Boolean) {
        super.setScreenReaderFocusable(screenReaderFocusable)
    }

    @RequiresApi(34)
    override fun setAccessibilityDataSensitive(accessibilityDataSensitive: Int) {
        super.setAccessibilityDataSensitive(accessibilityDataSensitive)
    }

    override fun setAccessibilityTraversalBefore(beforeId: Int) {
        super.setAccessibilityTraversalBefore(beforeId)
    }

    override fun setAccessibilityTraversalAfter(afterId: Int) {
        super.setAccessibilityTraversalAfter(afterId)
    }

    override fun setLabelFor(id: Int) {
        super.setLabelFor(id)
    }

    // =========================================================================================
    // 3. View APIs that currently DO work via the headless View, but NOT yet via Compose.
    // TODO: Support base background color, scrolling, long-press/selection ActionMode, key input,
    // drag-and-drop, rich content, stylus handwriting, focus, autofill, and content capture in
    // WebSurface / WebContent and deprecate these methods.
    // =========================================================================================

    override fun setBackgroundColor(color: Int) {
        super.setBackgroundColor(color)
    }

    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, y)
    }

    override fun scrollBy(x: Int, y: Int) {
        super.scrollBy(x, y)
    }

    override fun setScrollX(value: Int) {
        super.setScrollX(value)
    }

    override fun setScrollY(value: Int) {
        super.setScrollY(value)
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return super.canScrollHorizontally(direction)
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return super.canScrollVertically(direction)
    }

    override fun pageUp(top: Boolean): Boolean {
        return super.pageUp(top)
    }

    override fun pageDown(bottom: Boolean): Boolean {
        return super.pageDown(bottom)
    }

    override fun flingScroll(vx: Int, vy: Int) {
        super.flingScroll(vx, vy)
    }

    override fun setOnScrollChangeListener(l: OnScrollChangeListener?) {
        super.setOnScrollChangeListener(l)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        super.setOnLongClickListener(l)
    }

    override fun setLongClickable(longClickable: Boolean) {
        super.setLongClickable(longClickable)
    }

    override fun performLongClick(): Boolean {
        return super.performLongClick()
    }

    override fun performLongClick(x: Float, y: Float): Boolean {
        return super.performLongClick(x, y)
    }

    override fun setOnKeyListener(l: OnKeyListener?) {
        super.setOnKeyListener(l)
    }

    @RequiresApi(28)
    override fun addOnUnhandledKeyEventListener(listener: OnUnhandledKeyEventListener?) {
        super.addOnUnhandledKeyEventListener(listener)
    }

    @RequiresApi(28)
    override fun removeOnUnhandledKeyEventListener(listener: OnUnhandledKeyEventListener?) {
        super.removeOnUnhandledKeyEventListener(listener)
    }

    override fun setOnDragListener(l: OnDragListener?) {
        super.setOnDragListener(l)
    }

    @RequiresApi(31)
    override fun setOnReceiveContentListener(
        mimeTypes: Array<out String>?,
        listener: OnReceiveContentListener?,
    ) {
        super.setOnReceiveContentListener(mimeTypes, listener)
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        return super.startActionMode(callback)
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return super.startActionMode(callback, type)
    }

    @RequiresApi(33)
    override fun setAutoHandwritingEnabled(enabled: Boolean) {
        super.setAutoHandwritingEnabled(enabled)
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        super.setOnFocusChangeListener(l)
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        return super.requestFocus(direction, previouslyFocusedRect)
    }

    override fun clearFocus() {
        super.clearFocus()
    }

    @RequiresApi(26)
    override fun restoreDefaultFocus(): Boolean {
        return super.restoreDefaultFocus()
    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
    }

    @RequiresApi(26)
    override fun setFocusable(focusable: Int) {
        super.setFocusable(focusable)
    }

    override fun setFocusableInTouchMode(focusableInTouchMode: Boolean) {
        super.setFocusableInTouchMode(focusableInTouchMode)
    }

    @RequiresApi(26)
    override fun setFocusedByDefault(isFocusedByDefault: Boolean) {
        super.setFocusedByDefault(isFocusedByDefault)
    }

    @RequiresApi(26)
    override fun setDefaultFocusHighlightEnabled(defaultFocusHighlightEnabled: Boolean) {
        super.setDefaultFocusHighlightEnabled(defaultFocusHighlightEnabled)
    }

    override fun setNextFocusLeftId(nextFocusLeftId: Int) {
        super.setNextFocusLeftId(nextFocusLeftId)
    }

    override fun setNextFocusRightId(nextFocusRightId: Int) {
        super.setNextFocusRightId(nextFocusRightId)
    }

    override fun setNextFocusUpId(nextFocusUpId: Int) {
        super.setNextFocusUpId(nextFocusUpId)
    }

    override fun setNextFocusDownId(nextFocusDownId: Int) {
        super.setNextFocusDownId(nextFocusDownId)
    }

    override fun setNextFocusForwardId(nextFocusForwardId: Int) {
        super.setNextFocusForwardId(nextFocusForwardId)
    }

    @RequiresApi(26)
    override fun setKeyboardNavigationCluster(isCluster: Boolean) {
        super.setKeyboardNavigationCluster(isCluster)
    }

    @RequiresApi(26)
    override fun setNextClusterForwardId(nextClusterForwardId: Int) {
        super.setNextClusterForwardId(nextClusterForwardId)
    }

    @RequiresApi(26)
    override fun setImportantForAutofill(mode: Int) {
        super.setImportantForAutofill(mode)
    }

    @RequiresApi(26)
    override fun setAutofillHints(vararg autofillHints: String?) {
        super.setAutofillHints(*autofillHints)
    }

    @RequiresApi(30)
    override fun setImportantForContentCapture(mode: Int) {
        super.setImportantForContentCapture(mode)
    }

    @RequiresApi(29)
    override fun setContentCaptureSession(contentCaptureSession: ContentCaptureSession?) {
        super.setContentCaptureSession(contentCaptureSession)
    }
}
