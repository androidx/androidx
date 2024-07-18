/*
 * Copyright (C) 2014 The Android Open Source Project
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

package app.cash.paparazzi

import com.android.ide.common.rendering.api.HardwareConfig
import com.android.ide.common.resources.configuration.CountryCodeQualifier
import com.android.ide.common.resources.configuration.DensityQualifier
import com.android.ide.common.resources.configuration.FolderConfiguration
import com.android.ide.common.resources.configuration.KeyboardStateQualifier
import com.android.ide.common.resources.configuration.LayoutDirectionQualifier
import com.android.ide.common.resources.configuration.LocaleQualifier
import com.android.ide.common.resources.configuration.NavigationMethodQualifier
import com.android.ide.common.resources.configuration.NetworkCodeQualifier
import com.android.ide.common.resources.configuration.NightModeQualifier
import com.android.ide.common.resources.configuration.ScreenDimensionQualifier
import com.android.ide.common.resources.configuration.ScreenOrientationQualifier
import com.android.ide.common.resources.configuration.ScreenRatioQualifier
import com.android.ide.common.resources.configuration.ScreenSizeQualifier
import com.android.ide.common.resources.configuration.TextInputMethodQualifier
import com.android.ide.common.resources.configuration.TouchScreenQualifier
import com.android.ide.common.resources.configuration.UiModeQualifier
import com.android.ide.common.resources.configuration.VersionQualifier
import com.android.resources.Density
import com.android.resources.Keyboard
import com.android.resources.KeyboardState
import com.android.resources.LayoutDirection
import com.android.resources.Navigation
import com.android.resources.NightMode
import com.android.resources.NightMode.NOTNIGHT
import com.android.resources.ScreenOrientation
import com.android.resources.ScreenRatio
import com.android.resources.ScreenSize
import com.android.resources.TouchScreen
import com.android.resources.UiMode
import com.google.android.collect.Maps
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Provides [FolderConfiguration] and [HardwareConfig] for various devices. Also provides utility
 * methods to parse `build.prop` and `attrs.xml` to generate the appropriate maps.
 *
 * Defaults are for a Nexus 4 device.
 */
data class DeviceConfig(
  val screenHeight: Int = 1280,
  val screenWidth: Int = 768,
  val xdpi: Int = 320,
  val ydpi: Int = 320,
  val orientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
  val nightMode: NightMode = NOTNIGHT,
  val density: Density = Density.XHIGH,
  val fontScale: Float = 1f,
  val layoutDirection: LayoutDirection = LayoutDirection.LTR,
  val locale: String = "en",
  val ratio: ScreenRatio = ScreenRatio.NOTLONG,
  val size: ScreenSize = ScreenSize.NORMAL,
  val keyboard: Keyboard = Keyboard.NOKEY,
  val touchScreen: TouchScreen = TouchScreen.FINGER,
  val keyboardState: KeyboardState = KeyboardState.SOFT,
  val softButtons: Boolean = true,
  val navigation: Navigation = Navigation.NONAV,
  val released: String = "November 13, 2012"
) {
  val folderConfiguration: FolderConfiguration
    get() = FolderConfiguration.createDefault()
      .apply {
        densityQualifier = DensityQualifier(density)
        navigationMethodQualifier = NavigationMethodQualifier(navigation)
        screenDimensionQualifier = when {
          screenWidth > screenHeight -> ScreenDimensionQualifier(screenWidth, screenHeight)
          else -> ScreenDimensionQualifier(screenHeight, screenWidth)
        }
        screenRatioQualifier = ScreenRatioQualifier(ratio)
        screenSizeQualifier = ScreenSizeQualifier(size)
        textInputMethodQualifier = TextInputMethodQualifier(keyboard)
        touchTypeQualifier = TouchScreenQualifier(touchScreen)
        keyboardStateQualifier = KeyboardStateQualifier(keyboardState)
        screenOrientationQualifier = ScreenOrientationQualifier(orientation)

        updateScreenWidthAndHeight()
        uiModeQualifier = UiModeQualifier(UiMode.NORMAL)
        nightModeQualifier = NightModeQualifier(nightMode)
        countryCodeQualifier = CountryCodeQualifier()
        layoutDirectionQualifier = LayoutDirectionQualifier(layoutDirection)
        networkCodeQualifier = NetworkCodeQualifier()
        localeQualifier = LocaleQualifier.getQualifier(locale)
        versionQualifier = VersionQualifier()
      }

  val hardwareConfig: HardwareConfig
    get() = HardwareConfig(
      screenWidth, screenHeight, density, xdpi.toFloat(), ydpi.toFloat(), size,
      orientation, null, softButtons
    )

  /**
   * Device specs per:
   * https://android.googlesource.com/platform/tools/base/+/mirror-goog-studio-master-dev/sdklib/src/main/java/com/android/sdklib/devices/nexus.xml
   *
   * Release dates obtained from Wikipedia.
   */

  companion object {
    @JvmField
    val NEXUS_4 = DeviceConfig()

    @JvmField
    val NEXUS_5 = DeviceConfig(
      screenHeight = 1920,
      screenWidth = 1080,
      xdpi = 445,
      ydpi = 445,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.XXHIGH,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 31, 2013"
    )

    @JvmField
    val NEXUS_7 = DeviceConfig(
      screenHeight = 1920,
      screenWidth = 1200,
      xdpi = 323,
      ydpi = 323,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.XHIGH,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.LARGE,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "July 26, 2013"
    )

    @JvmField
    val NEXUS_10 = DeviceConfig(
      screenHeight = 1600,
      screenWidth = 2560,
      xdpi = 300,
      ydpi = 300,
      orientation = ScreenOrientation.LANDSCAPE,
      density = Density.XHIGH,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.XLARGE,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "November 13, 2012"
    )

    @JvmField
    val NEXUS_5_LAND = DeviceConfig(
      screenHeight = 1080,
      screenWidth = 1920,
      xdpi = 445,
      ydpi = 445,
      orientation = ScreenOrientation.LANDSCAPE,
      density = Density.XXHIGH,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 31, 2013"
    )

    @JvmField
    val NEXUS_7_2012 = DeviceConfig(
      screenHeight = 1280,
      screenWidth = 800,
      xdpi = 195,
      ydpi = 200,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.TV,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.LARGE,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "July 13, 2012"
    )

    @JvmField
    val PIXEL_C = DeviceConfig(
      screenHeight = 1800,
      screenWidth = 2560,
      xdpi = 308,
      ydpi = 308,
      orientation = ScreenOrientation.LANDSCAPE,
      density = Density.XHIGH,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.XLARGE,
      keyboard = Keyboard.QWERTY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "December 8, 2015"
    )

    @JvmField
    val PIXEL = DeviceConfig(
      screenHeight = 1920,
      screenWidth = 1080,
      xdpi = 440,
      ydpi = 440,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_420,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 20, 2016"
    )

    @JvmField
    val PIXEL_XL = DeviceConfig(
      screenHeight = 2560,
      screenWidth = 1440,
      xdpi = 534,
      ydpi = 534,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_560,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 20, 2016"
    )

    @JvmField
    val PIXEL_2 = DeviceConfig(
      screenHeight = 1920,
      screenWidth = 1080,
      xdpi = 442,
      ydpi = 443,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_420,
      ratio = ScreenRatio.NOTLONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 19, 2017"
    )

    @JvmField
    val PIXEL_2_XL = DeviceConfig(
      screenHeight = 2880,
      screenWidth = 1440,
      xdpi = 537,
      ydpi = 537,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_560,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 19, 2017"
    )

    @JvmField
    val PIXEL_3 = DeviceConfig(
      screenHeight = 2160,
      screenWidth = 1080,
      xdpi = 442,
      ydpi = 442,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_440,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 18, 2018"
    )

    @JvmField
    val PIXEL_3_XL = DeviceConfig(
      screenHeight = 2960,
      screenWidth = 1440,
      xdpi = 522,
      ydpi = 522,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_560,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 18, 2018"
    )

    @JvmField
    val PIXEL_3A = DeviceConfig(
      screenHeight = 2220,
      screenWidth = 1080,
      xdpi = 442,
      ydpi = 444,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_440,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "May 7, 2019"
    )

    @JvmField
    val PIXEL_3A_XL = DeviceConfig(
      screenHeight = 2160,
      screenWidth = 1080,
      xdpi = 397,
      ydpi = 400,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_400,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "May 7, 2019"
    )

    @JvmField
    val PIXEL_4 = DeviceConfig(
      screenHeight = 2280,
      screenWidth = 1080,
      xdpi = 444,
      ydpi = 444,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_440,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 24, 2019"
    )

    @JvmField
    val PIXEL_4_XL = DeviceConfig(
      screenHeight = 3040,
      screenWidth = 1440,
      xdpi = 537,
      ydpi = 537,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_560,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 24, 2019"
    )

    @JvmField
    val PIXEL_4A = DeviceConfig(
      screenHeight = 2340,
      screenWidth = 1080,
      xdpi = 442,
      ydpi = 444,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_440,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "August 20, 2020"
    )

    @JvmField
    val PIXEL_5 = DeviceConfig(
      screenHeight = 2340,
      screenWidth = 1080,
      xdpi = 442,
      ydpi = 444,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_440,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 15, 2020"
    )

    @JvmField
    val PIXEL_6 = DeviceConfig(
      screenHeight = 2400,
      screenWidth = 1080,
      xdpi = 406,
      ydpi = 411,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_420,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 28, 2021"
    )

    @JvmField
    val PIXEL_6_PRO = DeviceConfig(
      screenHeight = 3120,
      screenWidth = 1440,
      xdpi = 512,
      ydpi = 512,
      orientation = ScreenOrientation.PORTRAIT,
      density = Density.DPI_560,
      ratio = ScreenRatio.LONG,
      size = ScreenSize.NORMAL,
      keyboard = Keyboard.NOKEY,
      touchScreen = TouchScreen.FINGER,
      keyboardState = KeyboardState.SOFT,
      softButtons = true,
      navigation = Navigation.NONAV,
      released = "October 28, 2021"
    )

    private const val TAG_ATTR = "attr"
    private const val TAG_ENUM = "enum"
    private const val TAG_FLAG = "flag"
    private const val ATTR_NAME = "name"
    private const val ATTR_VALUE = "value"

    @Throws(IOException::class)
    fun loadProperties(path: File): Map<String, String> {
      val p = Properties()
      val map = Maps.newHashMap<String, String>()
      p.load(FileInputStream(path))
      for (key in p.stringPropertyNames()) {
        map[key] = p.getProperty(key)
      }
      return map
    }

    @Throws(IOException::class, XmlPullParserException::class)
    fun getEnumMap(path: File): Map<String, Map<String, Int>> {
      val map = mutableMapOf<String, MutableMap<String, Int>>()

      val xmlPullParser = XmlPullParserFactory.newInstance()
        .newPullParser()
      xmlPullParser.setInput(FileInputStream(path), null)
      var eventType = xmlPullParser.eventType
      var attr: String? = null
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          if (TAG_ATTR == xmlPullParser.name) {
            attr = xmlPullParser.getAttributeValue(null, ATTR_NAME)
          } else if (TAG_ENUM == xmlPullParser.name || TAG_FLAG == xmlPullParser.name) {
            val name = xmlPullParser.getAttributeValue(null, ATTR_NAME)
            val value = xmlPullParser.getAttributeValue(null, ATTR_VALUE)
            // Integer.decode cannot handle "ffffffff", see JDK issue 6624867
            val i = (java.lang.Long.decode(value) as Long).toInt()
            require(attr != null)
            var attributeMap: MutableMap<String, Int>? = map[attr]
            if (attributeMap == null) {
              attributeMap = Maps.newHashMap()
              map[attr] = attributeMap
            }
            attributeMap!![name] = i
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (TAG_ATTR == xmlPullParser.name) {
            attr = null
          }
        }
        eventType = xmlPullParser.next()
      }

      return map
    }
  }
}
