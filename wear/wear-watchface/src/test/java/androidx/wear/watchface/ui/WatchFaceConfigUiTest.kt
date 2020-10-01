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
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.icu.util.Calendar
import android.support.wearable.complications.ComplicationData
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.Complication
import androidx.wear.watchface.CanvasComplicationDrawableRenderer
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFaceTestRunner
import androidx.wear.watchface.createComplicationData
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleCategory
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
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
        ListUserStyleCategory.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        ListUserStyleCategory.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        ListUserStyleCategory.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleCategory = ListUserStyleCategory(
        "color_style_category",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        UserStyleCategory.LAYER_FLAG_WATCH_FACE_BASE
    )

    private val classicStyleOption =
        ListUserStyleCategory.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        ListUserStyleCategory.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        ListUserStyleCategory.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleCategory = ListUserStyleCategory(
        "hand_style_category",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        UserStyleCategory.LAYER_FLAG_WATCH_FACE_UPPER
    )

    private val leftComplication =
        Complication.Builder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(
                complicationDrawableLeft,
                watchState.asWatchState()
            ).apply {
                setData(createComplicationData())
            },
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            Complication.DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET)
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            .build()

    private val rightComplication =
        Complication.Builder(
            RIGHT_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                setData(createComplicationData())
            },
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            Complication.DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK)
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
            .build()

    private val backgroundComplication =
        Complication.Builder(
            BACKGROUND_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                setData(createComplicationData())
            },
            intArrayOf(
                ComplicationData.TYPE_LARGE_IMAGE
            ),
            Complication.DefaultComplicationProviderPolicy()
        ).setDefaultProviderType(ComplicationData.TYPE_LARGE_IMAGE)
            .setBackgroundComplication()
            .build()

    private val calendar = Calendar.getInstance().apply {
        timeInMillis = 1000L
    }

    private val configActivity = WatchFaceConfigActivity()

    private lateinit var userStyleRepository: UserStyleRepository

    private fun initConfigActivity(
        complications: List<Complication>,
        userStyleCategories: List<UserStyleCategory>
    ) {
        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)

        userStyleRepository =
            UserStyleRepository(userStyleCategories)

        val complicationSet = ComplicationsManager(
            complications,
            object : Renderer(surfaceHolder, userStyleRepository, watchState.asWatchState()) {
                override fun renderInternal(calendar: Calendar) {}

                override fun takeScreenshot(calendar: Calendar, drawMode: Int): Bitmap {
                    throw RuntimeException("Not Implemented!")
                }
            })

        val watchFaceComponentName = ComponentName(
            context.packageName,
            context.javaClass.typeName
        )
        WatchFaceConfigActivity.registerWatchFace(
            watchFaceComponentName,
            object : WatchFaceConfigDelegate {
                override fun getUserStyleSchema() =
                    UserStyleSchemaWireFormat(userStyleCategories.map { it.toWireFormat() })

                override fun getUserStyle() = userStyleRepository.userStyle.toWireFormat()

                override fun setUserStyle(userStyle: UserStyleWireFormat) {
                    userStyleRepository.userStyle = UserStyle(userStyle, userStyleCategories)
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
                    drawMode: Int
                ) = watchFaceConfigDelegate.takeScreenshot(drawRect, calendar, drawMode)
            })

        configActivity.init(watchFaceComponentName, fragmentController)
    }

    @After
    fun validate() {
        Mockito.validateMockitoUsage()
    }

    @Test
    fun brieflyHighlightComplicationId_calledWhenComplicationSelected() {
        initConfigActivity(listOf(leftComplication, rightComplication), emptyList())
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
        initConfigActivity(listOf(leftComplication, rightComplication), emptyList())
        val view = ConfigView(context, configActivity)

        // Tap on blank space.
        view.onTap(1, 1)
        verify(watchFaceConfigDelegate, times(0)).brieflyHighlightComplicationId(anyInt())
    }

    @Test
    fun onInitWithOneComplicationCalls_showComplicationConfig() {
        initConfigActivity(listOf(leftComplication), emptyList())

        verify(fragmentController).showComplicationConfig(
            LEFT_COMPLICATION_ID,
            *leftComplication.supportedTypes
        )
    }

    @Test
    fun onInitWithOneBackgroundComplicationCalls_showComplicationConfig() {
        initConfigActivity(listOf(backgroundComplication), emptyList())

        verify(fragmentController).showComplicationConfig(
            BACKGROUND_COMPLICATION_ID,
            *backgroundComplication.supportedTypes
        )
    }

    @Test
    fun onInitWithTwoComplicationsCalls_showComplicationConfigSelectionFragment() {
        initConfigActivity(listOf(leftComplication, rightComplication), emptyList())
        verify(fragmentController).showComplicationConfigSelectionFragment()
    }

    @Test
    fun onInitWithOneNormalAndOneBackgroundComplicationsCalls_showConfigFragment() {
        initConfigActivity(
            listOf(leftComplication, backgroundComplication),
            emptyList()
        )
        verify(fragmentController).showConfigFragment()
    }

    @Test
    fun onInitWithStylesCalls_showConfigFragment() {
        initConfigActivity(listOf(leftComplication), listOf(colorStyleCategory))
        verify(fragmentController).showConfigFragment()
    }

    @Test
    fun onInitWithNoComplicationsAndTwoStylesCalls_showConfigFragment() {
        initConfigActivity(
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory)
        )
        verify(fragmentController).showConfigFragment()
    }

    @Test
    @SuppressWarnings("unchecked")
    fun onInitWithNoComplicationsAndOneStyleCalls_showConfigFragment() {
        initConfigActivity(emptyList(), listOf(colorStyleCategory))

        val styleSchemaCaptor = argumentCaptor<List<UserStyleCategory>>()
        val userStyleCaptor = argumentCaptor<UserStyle>()

        // Note the schema and the style map will have been marshalled & unmarshalled so we can't
        // just test equality.
        verify(fragmentController).showStyleConfigFragment(
            eq(colorStyleCategory.id),
            styleSchemaCaptor.capture(),
            userStyleCaptor.capture()
        )

        assertThat(styleSchemaCaptor.firstValue.size).isEqualTo(1)
        assertThat(styleSchemaCaptor.firstValue.first().id).isEqualTo(colorStyleCategory.id)

        val key = userStyleCaptor.firstValue.options.keys.find { it.id == colorStyleCategory.id }
        assertThat(userStyleCaptor.firstValue.options[key]!!.id).isEqualTo(
            colorStyleCategory.options.first().id
        )
    }

    @Test
    fun styleConfigFragment_onItemClick_modifiesTheStyleCorrectly() {
        initConfigActivity(
            listOf(leftComplication, backgroundComplication),
            listOf(colorStyleCategory, watchHandStyleCategory)
        )
        val categoryIndex = 0
        val styleConfigFragment = StyleConfigFragment.newInstance(
            configActivity.styleSchema[categoryIndex].id,
            configActivity.styleSchema,
            UserStyle(hashMapOf(
                colorStyleCategory to colorStyleCategory.options.first(),
                watchHandStyleCategory to watchHandStyleCategory.options.first()
            ))
        )
        styleConfigFragment.readOptionsFromArguments()
        styleConfigFragment.watchFaceConfigActivity = configActivity

        assertThat(userStyleRepository.userStyle.options[colorStyleCategory]!!.id).isEqualTo(
            redStyleOption.id
        )
        assertThat(userStyleRepository.userStyle.options[watchHandStyleCategory]!!.id).isEqualTo(
            classicStyleOption.id
        )

        styleConfigFragment.onItemClick(configActivity.styleSchema[categoryIndex].options[1])

        assertThat(userStyleRepository.userStyle.options[colorStyleCategory]!!.id).isEqualTo(
            greenStyleOption.id
        )
        assertThat(userStyleRepository.userStyle.options[watchHandStyleCategory]!!.id).isEqualTo(
            classicStyleOption.id
        )
    }
}
