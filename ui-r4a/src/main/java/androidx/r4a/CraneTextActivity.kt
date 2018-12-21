package androidx.r4a

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.r4a.setContent

class CraneTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            <ScrollView>
                <CraneWrapper>
                    <Text
                        text=TextSpan(
                                text = "Text Crane Demo Text Crane Demo Text Crane Demo",
                                style = TextStyle(
                                        fontFamily = FontFamily("sans-serif"),
                                        color = Color(0xFFFF0000.toInt()),
                                        fontSize = 100.0f,
                                        fontWeight = FontWeight.w700
                                )
                        )
                        textAlign=TextAlign.CENTER
                        textDirection=TextDirection.LTR
                        softWrap=true
                        overflow=TextOverflow.FADE
                        textScaleFactor=2.3f
                        maxLines=3 />
                </CraneWrapper>
            </ScrollView>
        }
    }
}