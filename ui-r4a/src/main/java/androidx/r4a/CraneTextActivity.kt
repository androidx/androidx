package androidx.r4a

import android.app.Activity
import android.os.Bundle
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.setContent

class CraneTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            <CraneWrapper>
                <Text text=TextSpan(
                    text = "Text Crane Demo Text Crane Demo Text Crane Demo Text Crane Demo",
                    style = TextStyle(
                        fontFamily = FontFamily("sans-serif"),
                        color = Color(0xFFFF0000.toInt()),
                        fontSize = 100.0
                    )
                ) />
            </CraneWrapper>
        }
    }
}