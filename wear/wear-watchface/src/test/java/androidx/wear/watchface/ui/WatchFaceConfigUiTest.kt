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

package androidx.wear.watchface.ui

import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.icu.util.Calendar
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.CanvasComplicationDrawable
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFaceTestRunner
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.createIdAndComplicationData
import androidx.wear.watchface.data.RenderParametersWireFormat
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.robolectric.annotation.Config

private const val LEFT_COMPLICATION_ID = 1000
private const val RIGHT_COMPLICATION_ID = 1001
private const val BACKGROUND_COMPLICATION_ID = 1111
private const val INTERACTIVE_UPDATE_RATE_MS = 16L

@Config(manifest = Config.NONE)
@RunWith(WatchFaceTestRunner::class)
class WatchFaceConfigUiTest {

    companion object {
        val ONE_HUNDRED_BY_ONE_HUNDRED_RECT = Rect(0, 0, 100, 100)
    }

    private val watchFaceConfigDelegate = Mockito.mock(WatchFaceConfigDelegate::class.java)
    private val fragmentController = Mockito.mock(FragmentController::class.java)
    private val surfaceHolder = Mockito.mock(SurfaceHolder::class.java)
    private val watchState = MutableWatchState()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val complicationDrawableLeft = ComplicationDrawable(context)
    private val complicationDrawableRight = ComplicationDrawable(context)

    private val redStyleOption =
        ListUserStyleSetting.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        ListUserStyleSetting.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        ListUserStyleSetting.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleSetting = ListUserStyleSetting(
        "color_style_setting",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        listOf(Layer.BASE_LAYER)
    )

    private val classicStyleOption =
        ListUserStyleSetting.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        ListUserStyleSetting.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        ListUserStyleSetting.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleSetting = ListUserStyleSetting(
        "hand_style_setting",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        listOf(Layer.TOP_LAYER)
    )

    private val leftComplication =
        Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableLeft,
                watchState.asWatchState()
            ).apply {
                idAndData = createIdAndComplicationData(LEFT_COMPLICATION_ID)
            },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val rightComplication =
        Complication.createRoundRectComplicationBuilder(
            RIGHT_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                idAndData = createIdAndComplicationData(RIGHT_COMPLICATION_ID)
            },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK),
            ComplicationBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val backgroundComplication =
        Complication.createBackgroundComplicationBuilder(
            BACKGROUND_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                idAndData = createIdAndComplicationData(BACKGROUND_COMPLICATION_ID)
            },
            listOf(
                ComplicationType.PHOTO_IMAGE
            ),
            DefaultComplicationProviderPolicy()
        ).setDefaultProviderType(ComplicationType.PHOTO_IMAGE)
            .build()

    private val calendar = Calendar.getInstance().apply {
        timeInMillis = 1000L
    }

    private val configActivity = WatchFaceConfigActivity()

    private lateinit var userStyleRepository: UserStyleRepository

    private fun initConfigActivity(
        complications: List<Complication>,
        userStyleSchema: UserStyleSchema
    ) {
        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)

        userStyleRepository = UserStyleRepository(userStyleSchema)

        val complicationSet = ComplicationsManager(
            complications,
            userStyleRepository,
            object : Renderer.CanvasRenderer(
                surfaceHolder,
                userStyleRepository,
                watchState.asWatchState(),
                CanvasType.SOFTWARE,
                INTERACTIVE_UPDATE_RATE_MS
            ) {
                override fun render(canvas: Canvas, bounds: Rect, calendar: Calendar) {}
            }
        )

        val watchFaceComponentName = ComponentName(
            context.packageName,
            context.javaClass.typeName
        )
        WatchFaceConfigActivity.registerWatchFace(
            watchFaceComponentName,
            object : WatchFaceConfigDelegate {
                override fun getUserStyleSchema() = userStyleSchema.toWireFormat()

                override fun getUserStyle() = userStyleRepository.userStyle.toWireFormat()

                override fun setUserStyle(userStyle: UserStyleWireFormat) {
                    userStyleRepository.userStyle = UserStyle(userStyle, userStyleSchema)
                }

                override fun getBackgroundComplicationId() =
                    complicationSet.getBackgroundComplication()?.id

                override fun getComplicationsMap() = complicationSet.complications

                override fun getCalendar() = calendar

                override fun getComplicationIdAt(tapX: Int, tapY: Int) =
                    complicationSet.getComplicationAt(tapX, tapY)?.id

                override fun brieflyHighlightComplicationId(complicationId: Int) {
                    watchFaceConfigDelegate.brieflyHighlightComplicationId(complicationId)
                }

                override fun takeScreenshot(
                    drawRect: Rect,
                    calendar: Calendar,
                    renderParameters: RenderParametersWireFormat
                ) = watchFaceConfigDelegate.takeScreenshot(drawRect, calendar, renderParameters)
            }
        )

        configActivity.init(watchFaceComponentName, fragmentController)
    }

    @After
    fun validate() {
        Mockito.validateMockitoUsage()
    }

    @Test
    fun brieflyHighlightComplicationId_calledWhenComplicationSelected() {
        initConfigActivity(
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        val view = ConfigView(context, configActivity)

        // Tap left complication.
        view.onTap(30, 50)
        verify(watchFaceConfigDelegate).brieflyHighlightComplicationId(LEFT_COMPLICATION_ID)

        // Tap right complication.
        view.onTap(70, 50)
        verify(watchFaceConfigDelegate).brieflyHighlightComplicationId(RIGHT_COMPLICATION_ID)
    }

    @Test
    fun brieflyHighlightComplicationId_notCalledWhenBlankSpaceTapped() {
        initConfigActivity(
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        val view = ConfigView(context, configActivity)

        // Tap on blank space.
        view.onTap(1, 1)
        verify(watchFaceConfigDelegate, times(0)).brieflyHighlightComplicationId(anyInt())
    }

    @Test
    fun onInitWithOneComplicationCalls_showComplicationConfig() {
        initConfigActivity(listOf(leftComplication), UserStyleSchema(emptyList()))

        verify(fragmentController).showComplicationConfig(
            LEFT_COMPLICATION_ID,
            *ComplicationType.toWireTypes(leftComplication.supportedTypes)
        )
    }

    @Test
    fun onInitWithOneBackgroundComplicationCalls_showComplicationConfig() {
        initConfigActivity(listOf(backgroundComplication), UserStyleSchema(emptyList()))

        verify(fragmentController).showComplicationConfig(
            BACKGROUND_COMPLICATION_ID,
            *ComplicationType.toWireTypes(backgroundComplication.supportedTypes)
        )
    }

    @Test
    fun onInitWithTwoComplicationsCalls_showComplicationConfigSelectionFragment() {
        initConfigActivity(
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        verify(fragmentController).showComplicationConfigSelectionFragment()
    }

    @Test
    fun onInitWithOneNormalAndOneBackgroundComplicationsCalls_showConfigFragment() {
        initConfigActivity(
            listOf(leftComplication, backgroundComplication),
            UserStyleSchema(emptyList())
        )
        verify(fragmentController).showConfigFragment()
    }

    @Test
    fun onInitWithStylesCalls_showConfigFragment() {
        initConfigActivity(listOf(leftComplication), UserStyleSchema(listOf(colorStyleSetting)))
        verify(fragmentController).showConfigFragment()
    }

    @Test
    fun onInitWithNoComplicationsAndTwoStylesCalls_showConfigFragment() {
        initConfigActivity(
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting))
        )
        verify(fragmentController).showConfigFragment()
    }

    @Test
    @SuppressWarnings("unchecked")
    fun onInitWithNoComplicationsAndOneStyleCalls_showConfigFragment() {
        initConfigActivity(emptyList(), UserStyleSchema(listOf(colorStyleSetting)))

        val styleSchemaCaptor = argumentCaptor<UserStyleSchema>()
        val userStyleCaptor = argumentCaptor<UserStyle>()

        // Note the schema and the style map will have been marshalled & unmarshalled so we can't
        // just test equality.
        verify(fragmentController).showStyleConfigFragment(
            eq(colorStyleSetting.id),
            styleSchemaCaptor.capture(),
            userStyleCaptor.capture()
        )

        assertThat(styleSchemaCaptor.firstValue.userStyleSettings.size).isEqualTo(1)
        assertThat(styleSchemaCaptor.firstValue.userStyleSettings.first().id)
            .isEqualTo(colorStyleSetting.id)

        val key =
            userStyleCaptor.firstValue.selectedOptions.keys.find { it.id == colorStyleSetting.id }
        assertThat(userStyleCaptor.firstValue.selectedOptions[key]!!.id).isEqualTo(
            colorStyleSetting.options.first().id
        )
    }

    @Test
    fun styleConfigFragment_onItemClick_modifiesTheStyleCorrectly() {
        initConfigActivity(
            listOf(leftComplication, backgroundComplication),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting))
        )
        val settingIndex = 0
        val styleConfigFragment = StyleConfigFragment.newInstance(
            configActivity.styleSchema.userStyleSettings[settingIndex].id,
            configActivity.styleSchema,
            UserStyle(
                hashMapOf(
                    colorStyleSetting to colorStyleSetting.options.first(),
                    watchHandStyleSetting to watchHandStyleSetting.options.first()
                )
            )
        )
        styleConfigFragment.readOptionsFromArguments()
        styleConfigFragment.watchFaceConfigActivity = configActivity

        assertThat(userStyleRepository.userStyle.selectedOptions[colorStyleSetting]!!.id)
            .isEqualTo(
                redStyleOption.id
            )
        assertThat(userStyleRepository.userStyle.selectedOptions[watchHandStyleSetting]!!.id)
            .isEqualTo(
                classicStyleOption.id
            )

        styleConfigFragment.onItemClick(
            configActivity.styleSchema.userStyleSettings[settingIndex].options[1]
        )

        assertThat(userStyleRepository.userStyle.selectedOptions[colorStyleSetting]!!.id)
            .isEqualTo(
                greenStyleOption.id
            )
        assertThat(userStyleRepository.userStyle.selectedOptions[watchHandStyleSetting]!!.id)
            .isEqualTo(
                classicStyleOption.id
            )
    }
}
