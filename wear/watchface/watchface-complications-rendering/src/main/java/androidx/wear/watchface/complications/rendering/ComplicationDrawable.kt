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
package androidx.wear.watchface.complications.rendering

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting
import androidx.wear.watchface.ComplicationHelperActivity
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType.NO_DATA
import androidx.wear.watchface.complications.data.ComplicationType.NO_PERMISSION
import androidx.wear.watchface.complications.data.ComplicationType.RANGED_VALUE
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.rendering.ComplicationRenderer.OnInvalidateListener
import java.io.IOException
import java.time.Instant
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/**
 * A styleable drawable object that draws complicationSlots. You can create a ComplicationDrawable
 * from XML inflation or by using one of the constructor methods.
 *
 * <h3>Constructing a ComplicationDrawable</h3>
 *
 * To construct a ComplicationDrawable programmatically, use the [ComplicationDrawable] constructor.
 * Afterwards, styling attributes you want to modify can be set via set methods.
 *
 * ```
 * val complicationDrawable = ComplicationDrawable(context)
 * complicationDrawable.activeStyle.backgroundColor = backgroundColor
 * complicationDrawable.activeStyle.textColor = textColor
 * ```
 *
 * <h3>Constructing a ComplicationDrawable from XML</h3>
 *
 * Constructing a ComplicationDrawable from an XML file makes it easier to modify multiple styling
 * attributes at once without calling any set methods. You may also use different XML files to
 * switch between different styles your watch face supports.
 *
 * To construct a ComplicationDrawable from a drawable XML file, you may create an XML file in your
 * project's `res/drawable` folder. A ComplicationDrawable with red text and white title in active
 * mode, and white text and white title in ambient mode would look like this:
 * ```
 * <?xml version="1.0" encoding="utf-8"?>
 * <android.support.wearable.complication.rendering.ComplicationDrawable
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * app:textColor="#FFFF0000"
 * app:titleColor="#FFFFFFFF">
 * <ambient
 * app:textColor="#FFFFFFFF" />
 * </android.support.wearable.complication.rendering.ComplicationDrawable>
 * ```
 *
 * A top-level `drawable` tag with the `class` attribute may also be used to construct a
 * ComplicationDrawable from an XML file:
 * ```
 * <?xml version="1.0" encoding="utf-8"?>
 * <drawable
 * class="android.support.wearable.complication.rendering.ComplicationDrawable"
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * app:textColor="#FFFF0000"
 * app:titleColor="#FFFFFFFF">
 * <ambient
 * app:textColor="#FFFFFFFF" />
 * </drawable>
 * ```
 *
 * To inflate a ComplicationDrawable from XML file, use the [.getDrawable] method.
 * ComplicationDrawable needs access to the current context in order to style and draw the
 * complication.
 *
 * ```
 * public void onCreate(SurfaceHolder holder) {
 * ...
 * ComplicationDrawable complicationDrawable = (ComplicationDrawable)
 * getDrawable(R.drawable.complication);
 * complicationDrawable.setContext(WatchFaceService.this);
 * ...
 * }
 * ```
 *
 * <h4>Syntax:</h4>
 *
 * ```
 * <?xml version="1.0" encoding="utf-8"?>
 * <android.support.wearable.complication.rendering.ComplicationDrawable
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * app:backgroundColor="color"
 * app:backgroundDrawable="drawable"
 * app:borderColor="color"
 * app:borderDashGap="dimension"
 * app:borderDashWidth="dimension"
 * app:borderRadius="dimension"
 * app:borderStyle="none|solid|dashed"
 * app:borderWidth="dimension"
 * app:highlightColor="color"
 * app:iconColor="color"
 * app:rangedValuePrimaryColor="color"
 * app:rangedValueProgressHidden="boolean"
 * app:rangedValueRingWidth="dimension"
 * app:rangedValueSecondaryColor="color"
 * app:textColor="color"
 * app:textSize="dimension"
 * app:textTypeface="string"
 * app:titleColor="color"
 * app:titleSize="dimension"
 * app:titleTypeface="string">
 * <ambient
 * app:backgroundColor="color"
 * app:backgroundDrawable="drawable"
 * app:borderColor="color"
 * app:borderDashGap="dimension"
 * app:borderDashWidth="dimension"
 * app:borderRadius="dimension"
 * app:borderStyle="none|solid|dashed"
 * app:borderWidth="dimension"
 * app:highlightColor="color"
 * app:iconColor="color"
 * app:rangedValuePrimaryColor="color"
 * app:rangedValueRingWidth="dimension"
 * app:rangedValueSecondaryColor="color"
 * app:textColor="color"
 * app:textSize="dimension"
 * app:textTypeface="string"
 * app:titleColor="color"
 * app:titleSize="dimension"
 * app:titleTypeface="string" />
 * </android.support.wearable.complication.rendering.ComplicationDrawable>
 * ```
 *
 * Attributes of the top-level tag apply to both active and ambient modes while attributes of the
 * inner `ambient` tag only apply to ambient mode. As an exception, top-level only
 * `rangedValueProgressHidden` attribute applies to both modes, and cannot be overridden in ambient
 * mode. To hide ranged value in only one of the active or ambient modes, you may consider setting
 * `rangedValuePrimaryColor` and `rangedValueSecondaryColor` to [android.graphics.Color.TRANSPARENT]
 * instead.
 *
 * <h3>Drawing a ComplicationDrawable</h3>
 *
 * Depending on the size and shape of the bounds, the layout of the complication may change. For
 * instance, a short text complication with an icon that is drawn on square bounds would draw the
 * icon above the short text, but a short text complication with an icon that is drawn on wide
 * rectangular bounds might draw the icon to the left of the short text instead.
 */
public class ComplicationDrawable : Drawable {
    /** Returns the [Context] used to render the complication. */
    public var context: Context? = null
        private set

    /** Returns complication renderer. */
    @VisibleForTesting
    @get:JvmName("getComplicationRenderer")
    internal var complicationRenderer: ComplicationRenderer? = null
        private set

    /** Returns complication style for active mode. */
    public val activeStyle: ComplicationStyle

    /** Returns complication style for ambient mode. */
    public val ambientStyle: ComplicationStyle

    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val unhighlightRunnable = Runnable {
        isHighlighted = false
        invalidateSelf()
    }
    private val rendererInvalidateListener = OnInvalidateListener { invalidateSelf() }

    /**
     * The time in milliseconds since the epoch used for rendering [ComplicationData] with time
     * dependent text.
     */
    public var currentTime: Instant = Instant.EPOCH

    /** Whether the complication is rendered in ambient mode. */
    public var isInAmbientMode: Boolean = false

    /**
     * Whether the complication, when rendering in ambient mode, should apply a style suitable for
     * low bit ambient mode.
     */
    public var isLowBitAmbient: Boolean = false

    /**
     * Whether the complication, when rendering in ambient mode, should apply a style suitable for
     * display on devices with burn in protection.
     */
    public var isBurnInProtectionOn: Boolean = false

    /**
     * Whether the complication is currently highlighted. This may be called by a watch face when a
     * complication is tapped.
     *
     * If watch face is in ambient mode, highlight will not be visible even if this is set to
     * `true`, because it may cause burn-in or power inefficiency.
     */
    public var isHighlighted: Boolean = false

    private var isInflatedFromXml = false
    private var alreadyStyled = false

    /** Default constructor. */
    public constructor() {
        activeStyle = ComplicationStyle()
        ambientStyle = ComplicationStyle()
    }

    /**
     * Creates a ComplicationDrawable using the given context. If this constructor is used, calling
     * [setContext] may not be necessary.
     *
     * @param context The [Context] used to render the complication.
     */
    public constructor(context: Context) : this() {
        setContext(context)
    }

    public constructor(drawable: ComplicationDrawable) {
        activeStyle = ComplicationStyle(drawable.activeStyle)
        ambientStyle = ComplicationStyle(drawable.ambientStyle)
        noDataText = drawable.noDataText!!.subSequence(0, drawable.noDataText!!.length)
        highlightDuration = drawable.highlightDuration
        currentTime = drawable.currentTime
        bounds = drawable.bounds
        isInAmbientMode = drawable.isInAmbientMode
        isLowBitAmbient = drawable.isLowBitAmbient
        isBurnInProtectionOn = drawable.isBurnInProtectionOn
        isHighlighted = false
        isRangedValueProgressHidden = drawable.isRangedValueProgressHidden
        isInflatedFromXml = drawable.isInflatedFromXml
        alreadyStyled = true
    }

    /**
     * Sets the [Context] used to render the complication. If a context is not set,
     * ComplicationDrawable will throw an [IllegalStateException] if one of [draw], [setBounds], or
     * [setComplicationData] is called.
     *
     * While this can be called from any context, ideally, a
     * [androidx.wear.watchface.WatchFaceService] object should be passed here to allow creating
     * permission dialogs by the [onTap] method, in case current watch face doesn't have the
     * permission to receive complication data.
     *
     * If this ComplicationDrawable is retrieved using [Resources.getDrawable], this method must be
     * called before calling any of the methods mentioned above.
     *
     * If this ComplicationDrawable is not inflated from an XML file, this method will reset the
     * style to match the default values, so if ComplicationDrawable(drawable: ComplicationDrawable)
     * is used to construct a ComplicationDrawable, this method should be called right after.
     *
     * @param context The [Context] used to render the complication.
     */
    public fun setContext(context: Context) {
        if (context == this.context) {
            return
        }
        this.context = context
        if (!isInflatedFromXml && !alreadyStyled) {
            setStyleToDefaultValues(activeStyle, context.resources)
            setStyleToDefaultValues(ambientStyle, context.resources)
        }
        if (!alreadyStyled) {
            highlightDuration =
                context.resources
                    .getInteger(R.integer.complicationDrawable_highlightDurationMs)
                    .toLong()
        }
        complicationRenderer = ComplicationRenderer(this.context, activeStyle, ambientStyle)
        val nonNullComplicationRenderer = complicationRenderer!!
        nonNullComplicationRenderer.setOnInvalidateListener(rendererInvalidateListener)
        if (noDataText == null) {
            noDataText = context.getString(R.string.complicationDrawable_noDataText)
        }
        nonNullComplicationRenderer.setNoDataText(noDataText)
        nonNullComplicationRenderer.isRangedValueProgressHidden = isRangedValueProgressHidden
        nonNullComplicationRenderer.bounds = bounds
    }

    private fun inflateAttributes(r: Resources, parser: XmlPullParser) {
        val a = r.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.ComplicationDrawable)
        isRangedValueProgressHidden =
            a.getBoolean(R.styleable.ComplicationDrawable_rangedValueProgressHidden, false)
        a.recycle()
    }

    private fun inflateStyle(isAmbient: Boolean, r: Resources, parser: XmlPullParser) {
        val a = r.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.ComplicationDrawable)
        val complicationStyle = if (isAmbient) ambientStyle else activeStyle
        if (a.hasValue(R.styleable.ComplicationDrawable_backgroundColor)) {
            complicationStyle.backgroundColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_backgroundColor,
                    r.getColor(R.color.complicationDrawable_backgroundColor, null)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_backgroundDrawable)) {
            complicationStyle.backgroundDrawable =
                a.getDrawable(R.styleable.ComplicationDrawable_backgroundDrawable)
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textColor)) {
            complicationStyle.textColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_textColor,
                    r.getColor(R.color.complicationDrawable_textColor, null)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleColor)) {
            complicationStyle.titleColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_titleColor,
                    r.getColor(R.color.complicationDrawable_titleColor, null)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textTypeface)) {
            complicationStyle.setTextTypeface(
                Typeface.create(
                    a.getString(R.styleable.ComplicationDrawable_textTypeface),
                    Typeface.NORMAL
                )
            )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleTypeface)) {
            complicationStyle.setTitleTypeface(
                Typeface.create(
                    a.getString(R.styleable.ComplicationDrawable_titleTypeface),
                    Typeface.NORMAL
                )
            )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textSize)) {
            complicationStyle.textSize =
                a.getDimensionPixelSize(
                    R.styleable.ComplicationDrawable_textSize,
                    r.getDimensionPixelSize(R.dimen.complicationDrawable_textSize)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleSize)) {
            complicationStyle.titleSize =
                a.getDimensionPixelSize(
                    R.styleable.ComplicationDrawable_titleSize,
                    r.getDimensionPixelSize(R.dimen.complicationDrawable_titleSize)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_iconColor)) {
            complicationStyle.iconColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_iconColor,
                    r.getColor(R.color.complicationDrawable_iconColor, null)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderColor)) {
            complicationStyle.borderColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_borderColor,
                    r.getColor(R.color.complicationDrawable_borderColor, null)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderRadius)) {
            complicationStyle.borderRadius =
                a.getDimensionPixelSize(
                    R.styleable.ComplicationDrawable_borderRadius,
                    r.getDimensionPixelSize(R.dimen.complicationDrawable_borderRadius)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderStyle)) {
            complicationStyle.borderStyle =
                a.getInt(
                    R.styleable.ComplicationDrawable_borderStyle,
                    r.getInteger(R.integer.complicationDrawable_borderStyle)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderDashWidth)) {
            complicationStyle.borderDashWidth =
                a.getDimensionPixelSize(
                    R.styleable.ComplicationDrawable_borderDashWidth,
                    r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashWidth)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderDashGap)) {
            complicationStyle.borderDashGap =
                a.getDimensionPixelSize(
                    R.styleable.ComplicationDrawable_borderDashGap,
                    r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashGap)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderWidth)) {
            complicationStyle.borderWidth =
                a.getDimensionPixelSize(
                    R.styleable.ComplicationDrawable_borderWidth,
                    r.getDimensionPixelSize(R.dimen.complicationDrawable_borderWidth)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValueRingWidth)) {
            complicationStyle.rangedValueRingWidth =
                a.getDimensionPixelSize(
                    R.styleable.ComplicationDrawable_rangedValueRingWidth,
                    r.getDimensionPixelSize(R.dimen.complicationDrawable_rangedValueRingWidth)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValuePrimaryColor)) {
            complicationStyle.rangedValuePrimaryColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_rangedValuePrimaryColor,
                    r.getColor(R.color.complicationDrawable_rangedValuePrimaryColor, null)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValueSecondaryColor)) {
            complicationStyle.rangedValueSecondaryColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_rangedValueSecondaryColor,
                    r.getColor(R.color.complicationDrawable_rangedValueSecondaryColor, null)
                )
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_highlightColor)) {
            complicationStyle.highlightColor =
                a.getColor(
                    R.styleable.ComplicationDrawable_highlightColor,
                    r.getColor(R.color.complicationDrawable_highlightColor, null)
                )
        }
        a.recycle()
    }

    /**
     * Inflates this ComplicationDrawable from an XML resource. This can't be called more than once
     * for each ComplicationDrawable. Note that framework may have called this once to create the
     * ComplicationDrawable instance from an XML resource.
     *
     * @param r Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this ComplicationDrawable
     * @param attrs Base set of attribute values
     * @param theme Ignored by ComplicationDrawable
     */
    @Throws(XmlPullParserException::class, IOException::class)
    public override fun inflate(
        r: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet,
        theme: Resources.Theme?
    ) {
        check(!isInflatedFromXml) { "inflate may be called once only." }
        isInflatedFromXml = true
        var type: Int
        val outerDepth = parser.depth
        // Inflate attributes always shared between active and ambient mode
        inflateAttributes(r, parser)
        // Reset both style builders to default values
        setStyleToDefaultValues(activeStyle, r)
        setStyleToDefaultValues(ambientStyle, r)
        // Attributes of the outer tag applies to both active and ambient styles
        inflateStyle(false, r, parser)
        inflateStyle(true, r, parser)
        while (
            parser.next().also { type = it } != XmlPullParser.END_DOCUMENT &&
                (type != XmlPullParser.END_TAG || parser.depth > outerDepth)
        ) {
            if (type != XmlPullParser.START_TAG) {
                continue
            }

            // Attributes of inner <ambient> tag applies to ambient style only
            val name = parser.name
            if (TextUtils.equals(name, "ambient")) {
                inflateStyle(true, r, parser)
            } else {
                Log.w(
                    "ComplicationDrawable",
                    "Unknown element: $name for ComplicationDrawable $this"
                )
            }
        }
    }

    /**
     * Draws the complication for the last known time. Last known time is derived from
     * ComplicationDrawable#setCurrentTimeMillis(long)}.
     *
     * @param canvas Canvas for the complication to be drawn onto
     */
    public override fun draw(canvas: Canvas) {
        assertInitialized()
        updateStyleIfRequired()
        complicationRenderer?.draw(
            canvas,
            currentTime,
            isInAmbientMode,
            isLowBitAmbient,
            isBurnInProtectionOn,
            isHighlighted
        )
    }

    /**
     * This function is not supported in [ComplicationDrawable].
     *
     * @throws [UnsupportedOperationException] when called.
     */
    public override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
        throw UnsupportedOperationException("setAlpha is not supported in ComplicationDrawable.")
    }

    /**
     * This function is not supported in [ComplicationDrawable]. Use
     * [ComplicationStyle.imageColorFilter] instead to apply color filter to small and large images.
     *
     * @throws [UnsupportedOperationException] when called.
     */
    public override fun setColorFilter(colorFilter: ColorFilter?) {
        throw UnsupportedOperationException(
            "setColorFilter is not supported in ComplicationDrawable."
        )
    }

    @Deprecated("This method is no longer used in graphics optimizations")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    protected override fun onBoundsChange(bounds: Rect) {
        complicationRenderer?.let { it.bounds = bounds }
    }

    /**
     * If the ranged value progress should be hidden when [ComplicationData] is of type
     * [RANGED_VALUE].
     *
     * @attr ref androidx.wear.watchface.complicationSlots.rendering.R
     *   .styleable#ComplicationDrawable_rangedValueProgressHidden
     */
    public var isRangedValueProgressHidden: Boolean = false
        set(rangedValueProgressHidden) {
            field = rangedValueProgressHidden
            complicationRenderer?.isRangedValueProgressHidden = rangedValueProgressHidden
        }

    /**
     * Sets the complication data to be drawn.
     *
     * @param complicationData The [ComplicationData] to set
     * @param loadDrawablesAsync If true any drawables should be loaded asynchronously, otherwise
     *   they will be loaded synchronously.
     */
    public fun setComplicationData(
        complicationData: ComplicationData,
        loadDrawablesAsync: Boolean
    ) {
        this.complicationData = complicationData
        assertInitialized()
        if (loadDrawablesAsync) {
            // Calling nextRenderer.setComplicationData() causes it to render as blank until the
            // async load has completed. To mask this we delay applying the update until the load
            // has completed.
            val nextRenderer = ComplicationRenderer(this.context, activeStyle, ambientStyle)
            nextRenderer.setNoDataText(noDataText)
            nextRenderer.isRangedValueProgressHidden = isRangedValueProgressHidden
            nextRenderer.bounds = bounds
            nextRenderer.setOnInvalidateListener {
                complicationRenderer = nextRenderer
                rendererInvalidateListener.onInvalidate()
                // Replace this InvalidateListener with the normal one.
                nextRenderer.setOnInvalidateListener(rendererInvalidateListener)
            }
            nextRenderer.setComplicationData(complicationData.asWireComplicationData(), true)
        } else {
            complicationRenderer?.setComplicationData(
                complicationData.asWireComplicationData(),
                false
            )
        }
    }

    /**
     * Returns the [ComplicationData] to be drawn by this ComplicationDrawable. This defaults to
     * [NoDataComplicationData].
     */
    public var complicationData: ComplicationData = NoDataComplicationData()
        private set

    init {
        complicationRenderer?.setComplicationData(complicationData.asWireComplicationData(), false)
    }

    /**
     * Sends the tap action for the complication if tap coordinates are inside the complication
     * bounds.
     *
     * This method will also highlight the complication. The highlight duration is 300 milliseconds
     * by default but can be modified using the [.setHighlightDuration] method.
     *
     * If [ComplicationData] has the type [NO_PERMISSION], this method will launch an intent to
     * request complication permission for the watch face. This will only work if the context set by
     * [getDrawable] or the constructor is an instance of WatchFaceService.
     *
     * @param x X coordinate of the tap relative to screen origin
     * @param y Y coordinate of the tap relative to screen origin
     * @return `true` if the action was successful, `false` if complication data is not set, the
     *   complication has no tap action, the tap action (i.e. [android.app.PendingIntent]) is
     *   cancelled, or the given x and y are not inside the complication bounds.
     */
    public fun onTap(@Px x: Int, @Px y: Int): Boolean {
        if (complicationRenderer == null) {
            return false
        }
        val data = complicationRenderer!!.complicationData ?: return false
        if (
            !data.hasTapAction() &&
                data.type !=
                    android.support.wearable.complications.ComplicationData.TYPE_NO_PERMISSION
        ) {
            return false
        }
        if (!bounds.contains(x, y)) {
            return false
        }
        if (
            data.type == android.support.wearable.complications.ComplicationData.TYPE_NO_PERMISSION
        ) {
            // Check if context is an instance of WatchFaceService. We can't use the standard
            // instanceof operator because WatchFaceService is defined in library which depends on
            // this one, hence the reflection hack.
            try {
                if (context!!::class.java.name == "androidx.wear.watchface.WatchFaceService") {
                    context!!.startActivity(
                        ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                context!!,
                                ComponentName(context!!, context!!.javaClass),
                                /* complicationDenied */ null,
                                /* complicationRationale */ null
                            )
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    return false
                }
            } catch (e: ClassNotFoundException) {
                // If watchFaceServiceClass class isn't found we know context can't be an instance
                // of WatchFaceService.
                return false
            }
        } else {
            try {
                data.tapAction!!.send()
            } catch (e: PendingIntent.CanceledException) {
                return false
            }
        }
        if (highlightDuration > 0) {
            isHighlighted = true
            invalidateSelf()
            mainThreadHandler.removeCallbacks(unhighlightRunnable)
            mainThreadHandler.postDelayed(unhighlightRunnable, highlightDuration)
        }
        return true
    }

    /**
     * The duration for the complication to stay highlighted after calling the [onTap] method.
     * Default value is 300 milliseconds. Setting highlight duration to 0 disables highlighting.
     */
    public var highlightDuration: Long = 0
        set(@IntRange(from = 0) highlightDurationMillis: Long) {
            require(highlightDurationMillis >= 0) { "Highlight duration should be non-negative." }
            field = highlightDurationMillis
        }

    /** Builds styles and syncs them with the complication renderer. */
    @JvmName(name = "updateStyleIfRequired")
    internal fun updateStyleIfRequired() {
        if (activeStyle.isDirty || ambientStyle.isDirty) {
            complicationRenderer!!.updateStyle(activeStyle, ambientStyle)
            activeStyle.clearDirtyFlag()
            ambientStyle.clearDirtyFlag()
        }
    }

    /**
     * Throws an exception if the context is not set. This method should be called if any of the
     * member methods do a context-dependent job.
     */
    private fun assertInitialized() {
        checkNotNull(context) {
            "ComplicationDrawable does not have a context. Use setContext(Context) to set it first."
        }
    }

    /**
     * The text to be rendered when [ComplicationData] is of type [NO_DATA]. If `noDataText` is
     * null, an empty text will be rendered.
     */
    public var noDataText: CharSequence? = null
        set(noDataText) {
            field = noDataText?.subSequence(0, noDataText.length) ?: ""
            if (complicationRenderer != null) {
                complicationRenderer!!.setNoDataText(field)
            }
        }

    public companion object {
        /**
         * Creates a ComplicationDrawable from a resource.
         *
         * @param context The [Context] to load the resource from
         * @param id The id of the resource to load
         * @return The [ComplicationDrawable] loaded from the specified resource id or null if it
         *   doesn't exist.
         */
        @JvmStatic
        public fun getDrawable(context: Context, id: Int): ComplicationDrawable? {
            val drawable = context.getDrawable(id) as ComplicationDrawable? ?: return null
            drawable.setContext(context)
            return drawable
        }

        /** Sets the style to default values using resources. */
        @JvmStatic
        internal fun setStyleToDefaultValues(style: ComplicationStyle, r: Resources) {
            style.backgroundColor = r.getColor(R.color.complicationDrawable_backgroundColor, null)
            style.textColor = r.getColor(R.color.complicationDrawable_textColor, null)
            style.titleColor = r.getColor(R.color.complicationDrawable_titleColor, null)
            style.setTextTypeface(
                Typeface.create(
                    r.getString(R.string.complicationDrawable_textTypeface),
                    Typeface.NORMAL
                )
            )
            style.setTitleTypeface(
                Typeface.create(
                    r.getString(R.string.complicationDrawable_titleTypeface),
                    Typeface.NORMAL
                )
            )
            style.textSize = r.getDimensionPixelSize(R.dimen.complicationDrawable_textSize)
            style.titleSize = r.getDimensionPixelSize(R.dimen.complicationDrawable_titleSize)
            style.iconColor = r.getColor(R.color.complicationDrawable_iconColor, null)
            style.borderColor = r.getColor(R.color.complicationDrawable_borderColor, null)
            style.borderWidth = r.getDimensionPixelSize(R.dimen.complicationDrawable_borderWidth)
            style.borderRadius = r.getDimensionPixelSize(R.dimen.complicationDrawable_borderRadius)
            style.borderStyle = r.getInteger(R.integer.complicationDrawable_borderStyle)
            style.borderDashWidth =
                r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashWidth)
            style.borderDashGap =
                r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashGap)
            style.rangedValueRingWidth =
                r.getDimensionPixelSize(R.dimen.complicationDrawable_rangedValueRingWidth)
            style.rangedValuePrimaryColor =
                r.getColor(R.color.complicationDrawable_rangedValuePrimaryColor, null)
            style.rangedValueSecondaryColor =
                r.getColor(R.color.complicationDrawable_rangedValueSecondaryColor, null)
            style.highlightColor = r.getColor(R.color.complicationDrawable_highlightColor, null)
        }
    }
}
