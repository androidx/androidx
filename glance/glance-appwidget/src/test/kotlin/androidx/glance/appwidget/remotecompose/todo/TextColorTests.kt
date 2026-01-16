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

package androidx.glance.appwidget.remotecompose.todo

import androidx.glance.appwidget.remotecompose.BaseRemoteComposeTest
import kotlin.test.Ignore
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** These are incomplete tests split from TranslateTextTest */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TextColorTests : BaseRemoteComposeTest() {

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateTextStyle_color_dynamicColor() =
        fakeCoroutineScope.runTest { TODO("write a test for dynamic color") }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateTextStyle_color_dayNightColorProvider() =
        fakeCoroutineScope.runTest {
            //            val string = "day=red, night=blue"
            //            val nightColor: Color = Color.White
            //            val dayColor: Color = Color.Red
            //
            //            val dayNightProvider = DayNightColorProvider(day = dayColor, night =
            // nightColor)
            //
            //            val composable: @Composable () -> Unit = {
            //                Box(modifier = GlanceModifier.size(100.dp, 100.dp)) {
            //                    Text(string, style = TextStyle(color = dayNightProvider))
            //                }
            //            }
            //
            //            val originalRoot = runTestingComposition(composable)
            //            val normalizedRoot =
            //                (originalRoot.copy() as RemoteViewsRoot).also {
            // normalizeCompositionTree(it) }
            //
            //            val rcContext: RemoteComposeContext =
            //                (translateEmittableTreeToRemoteCompose(
            //                        context = context,
            //                        remoteViewRoot = normalizedRoot,
            //                        appWidgetId = appWidgetId,
            //                    )
            //                        as GlanceToRemoteComposeTranslation.Single)
            //                    .remoteComposeContext
            //
            //            val remoteComposeBuffer =
            //                RemoteComposeBuffer(RemoteComposeState()).apply {
            //                    buffer = rcContext.buffer.buffer
            //                    setPlatform(Platform.None)
            //                }
            //
            //            val coreDocument =
            //                CoreDocument().apply {
            //                    height = 555 // arbitrary
            //                    width = 555 // arbitrary
            //
            //                    initFromBuffer(remoteComposeBuffer)
            //                }
            //
            //            val debugContext = GlanceDebugCreationContext()
            //            coreDocument.initializeContext(debugContext)
            //
            //            debugPrintDoc(coreDocument)
            //
            //            // TODO: getSimpleLeaf won't work here because there will be two
            // TEXT_LAYOUT elements
            //            // that
            //            // each have visibility operations.
            //            coreDocument.paint(debugContext, Theme.LIGHT)
            //            val rcDayColor = (getSimpleLeaf(coreDocument) as TextLayout).mColor
            //
            //            coreDocument.paint(debugContext, Theme.DARK)
            //            val rcNightColor = (getSimpleLeaf(coreDocument) as TextLayout).mColor
            //
            //            assertEquals(dayColor.toArgb(), rcDayColor)
            //
            //            // TODO: i'm not sure what the right way to test this is
            //            assertEquals(nightColor.toArgb(), rcNightColor)

            fail("TODO: test text color with day/night color providers")
        }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateTextStyle_color_resourceColorProvider() =
        fakeCoroutineScope.runTest {
            //            val string = "android.R.color.holo_blue_bright"
            //            val colorRes = R.color.holo_blue_bright
            //            val colorProvider = ColorProvider(colorRes)
            //
            //            val resolvedColor: Color = Color(context.getColor(colorRes))
            //
            //            val textNode = testTextWithStyle(string, TextStyle(color = colorProvider))
            //
            //            assertColorMatchesRCColorString(resolvedColor, textNode)
            fail("TODO: test colors")
        }
}
