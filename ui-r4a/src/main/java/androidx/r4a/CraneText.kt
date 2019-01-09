package androidx.r4a

import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.engine.text.*
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.window.Locale
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.Composable

val displayText = "Text Demo"
val displayTextChinese = "文本演示"
val displayTextArabic = "عرض النص"
val displayTextHindi = "पाठ डेमो"
val fontSize4: Float = 40.0.toFloat()
val fontSize6: Float = 60.0.toFloat()
val fontSize7: Float = 70.0.toFloat()
val fontSize8: Float = 80.0.toFloat()
val fontSize10: Float = 100.0.toFloat()

@Composable
fun TextDemo() {
    <LinearLayout orientation=LinearLayout.VERTICAL
                  layoutParams=LinearLayout.LayoutParams(
                          LinearLayout.LayoutParams.MATCH_PARENT,
                          LinearLayout.LayoutParams.MATCH_PARENT)>
        <ScrollView>
            <LinearLayout orientation=LinearLayout.VERTICAL>
                <TagLine bust=Math.random() tag="color, fontSize, fontWeight and fontStyle" />
                <TextDemoBasic bust=Math.random() />
                <TagLine bust=Math.random() tag="Chinese, Arabic, and Hindi" />
                <TextDemoLanguage bust=Math.random() />
                <TagLine bust=Math.random() tag="FontFamily: sans-serif, serif, and monospace" />
                <TextDemoFontFamily bust=Math.random() />
                <TagLine bust=Math.random() tag="decoration, decorationColor and decorationStyle" />
                <TextDemoTextDecoration bust=Math.random() />
                <TagLine bust=Math.random() tag="letterSpacing" />
                <TextDemoLetterSpacing bust=Math.random() />
                <TagLine bust=Math.random() tag="wordSpacing" />
                <TextDemoWordSpacing bust=Math.random() />
                <TagLine bust=Math.random() tag="height" />
                <TextDemoHeight bust=Math.random() />
                <TagLine bust=Math.random() tag="background" />
                <TextDemoBackground bust=Math.random() />
                <TagLine bust=Math.random()
                         tag="Locale: Japanese, Simplified and Traditional Chinese" />
                <TextDemoLocale bust=Math.random() />
                <TagLine bust=Math.random() tag="textAlign and textDirection" />
                <TextDemoTextAlign bust=Math.random() />
                <TagLine bust=Math.random() tag="softWrap: on and off" />
                <TextDemoSoftWrap bust=Math.random() />
                <TagLine bust=Math.random() tag="textScaleFactor: default and 2.0" />
                <TextDemoTextScaleFactor bust=Math.random() />
            </LinearLayout>
        </ScrollView>
    </LinearLayout>
}

@Composable
fun TagLine(bust: Double, tag: String) {
    <CraneWrapper>
        <Text text=TextSpan(
                text = "      ",
                style = TextStyle(fontSize = fontSize8)) />
    </CraneWrapper>
    <CraneWrapper>
        <Text text=TextSpan(
                text = tag,
                style = TextStyle(color = Color(0xFFAAAAAA.toInt()), fontSize = fontSize6)) />
    </CraneWrapper>
}

@Composable
fun SecondTagLine(bust: Double, tag: String) {
    <CraneWrapper>
        <Text text=TextSpan(
                text = tag,
                style = TextStyle(color = Color(0xFFAAAAAA.toInt()), fontSize = fontSize4)) />
    </CraneWrapper>
}

@Composable
fun TextDemoBasic(bust: Double) {
    // This group of text widgets show different color, fontSize, fontWeight and fontStyle in
    // English.
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        color = Color(0xFFFF0000.toInt()),
                                        fontSize = fontSize6,
                                        fontWeight = FontWeight.w200,
                                        fontStyle = FontStyle.italic
                                )),
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        color = Color(0xFF00FF00.toInt()),
                                        fontSize = fontSize8,
                                        fontWeight = FontWeight.w500,
                                        fontStyle = FontStyle.normal
                                )),
                        TextSpan(
                                text = displayText,
                                style = TextStyle(
                                        color = Color(0xFF0000FF.toInt()),
                                        fontSize = fontSize10,
                                        fontWeight = FontWeight.w800,
                                        fontStyle = FontStyle.normal
                                ))
                )
        ) />
    </CraneWrapper>
}

@Composable
fun TextDemoLanguage(bust: Double) {
    // This group of text widgets show different color, fontSize, fontWeight and fontStyle in
    // Chinese, Arabic, and Hindi.
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = displayTextChinese + "   ",
                                style = TextStyle(
                                        color = Color(0xFFFF0000.toInt()),
                                        fontSize = fontSize6,
                                        fontWeight = FontWeight.w200,
                                        fontStyle = FontStyle.italic
                                )),
                        TextSpan(
                                text = displayTextArabic + "   ",
                                style = TextStyle(
                                        color = Color(0xFF00FF00.toInt()),
                                        fontSize = fontSize8,
                                        fontWeight = FontWeight.w500,
                                        fontStyle = FontStyle.normal
                                )),
                        TextSpan(
                                text = displayTextHindi,
                                style = TextStyle(
                                        color = Color(0xFF0000FF.toInt()),
                                        fontSize = fontSize10,
                                        fontWeight = FontWeight.w800,
                                        fontStyle = FontStyle.normal
                                ))
                )
        ) />
    </CraneWrapper>
}

@Composable
fun TextDemoFontFamily(bust: Double) {
    // This group of text widgets show different fontFamilies in English.
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        fontFamily = FontFamily("sans-serif")
                                )),
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        fontFamily = FontFamily("serif")
                                )),
                        TextSpan(
                                text = displayText,
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        fontFamily = FontFamily("monospace")
                                ))
                )
        ) />
    </CraneWrapper>
}

@Composable
fun TextDemoTextDecoration(bust: Double) {
    // This group of text widgets show different decoration, decorationColor and decorationStyle.
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = displayText,
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        decoration = TextDecoration.lineThrough
                                )),
                        TextSpan(
                                text = displayText + '\n',
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        decoration = TextDecoration.underline
                                )),
                        TextSpan(
                                text = displayText,
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        decoration = TextDecoration.combine(
                                                listOf(
                                                        TextDecoration.underline,
                                                        TextDecoration.lineThrough))
                                ))
                )
        ) />
    </CraneWrapper>
}

@Composable
fun TextDemoLetterSpacing(bust: Double) {
    // This group of text widgets show different letterSpacing.
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8
                                )),
                        TextSpan(
                                text = displayText,
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        letterSpacing = 0.5.toFloat()
                                ))
                )
        ) />
    </CraneWrapper>
}

@Composable
fun TextDemoWordSpacing(bust: Double) {
    // This group of text widgets show different wordSpacing.
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8
                                )),
                        TextSpan(
                                text = displayText,
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        wordSpacing = 100.0.toFloat()
                                ))
                )
        ) />
    </CraneWrapper>

}

@Composable
fun TextDemoHeight(bust: Double) {
    // This group of text widgets show different height.
    <LinearLayout orientation=LinearLayout.HORIZONTAL>
        <CraneWrapper>
            <Text text=TextSpan(
                    text = displayText + "\n" + displayText + "   ",
                    style = TextStyle(
                            fontSize = fontSize8
                    )) />
        </CraneWrapper>
        <CraneWrapper>
            <Text text=TextSpan(
                    text = displayText + "\n" + displayText,
                    style = TextStyle(
                            fontSize = fontSize8,
                            height = 2.0.toFloat()
                    )) />
        </CraneWrapper>
    </LinearLayout>
}

@Composable
fun TextDemoBackground(bust: Double) {
    // This group of text widgets show different background.
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        background = Color(0xFFFF0000.toInt())
                                )),
                        TextSpan(
                                text = displayText + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        background = Color(0xFF00FF00.toInt())
                                )),
                        TextSpan(
                                text = displayText,
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        background = Color(0xFF0000FF.toInt())
                                ))
                )
        ) />
    </CraneWrapper>
}

@Composable
fun TextDemoLocale(bust: Double) {
    // This group of text widgets show different Locales of the same Unicode codepoint.
    val text = "\u82B1"
    <CraneWrapper>
        <Text text=TextSpan(
                children = listOf(
                        TextSpan(
                                text = text + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        locale = Locale(_languageCode = "ja", _countryCode = "JP")
                                )),
                        TextSpan(
                                text = text + "   ",
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        locale = Locale(_languageCode = "zh", _countryCode = "CN")
                                )),
                        TextSpan(
                                text = text,
                                style = TextStyle(
                                        fontSize = fontSize8,
                                        locale = Locale(_languageCode = "zh", _countryCode = "TW")
                                ))
                )
        ) />
    </CraneWrapper>
}

@Composable
fun TextDemoTextAlign(bust: Double) {
    // This group of text widgets show different TextAligns: LEFT, RIGHT, CENTER, JUSTIFY, START for
    // LTR and RTL, END for LTR and RTL.
    val textSpan = TextSpan(
            text = displayText,
            style = TextStyle(fontSize = fontSize8))
    var text: String = ""
    for (i in 1..10) {
        text = text + displayText + " "
    }
    <LinearLayout orientation=LinearLayout.VERTICAL>
        <SecondTagLine bust=Math.random() tag="textAlgin = TextAlign.LEFT" />
        <CraneWrapper>
            <Text
                text=textSpan
                textAlign=TextAlign.LEFT />
        </CraneWrapper>
        <SecondTagLine bust=Math.random() tag="textAlgin = TextAlign.RIGHT" />
        <CraneWrapper>
            <Text
                text=textSpan
                textAlign=TextAlign.RIGHT />
        </CraneWrapper>
        <SecondTagLine bust=Math.random() tag="textAlgin = TextAlign.CENTER" />
        <CraneWrapper>
            <Text
                text=textSpan
                textAlign=TextAlign.CENTER />
        </CraneWrapper>
        <SecondTagLine bust=Math.random() tag="textAlgin = default and TextAlign.JUSTIFY" />
        <CraneWrapper>
            <Text
                text=TextSpan(
                        text = text,
                        style = TextStyle(
                                fontSize = fontSize8,
                                color = Color(0xFFFF0000.toInt()))) />
        </CraneWrapper>
        <CraneWrapper>
            <Text
                text=TextSpan(
                        text = text,
                        style = TextStyle(
                                fontSize = fontSize8,
                                color = Color(0xFF0000FF.toInt())))
                textAlign=TextAlign.JUSTIFY />
        </CraneWrapper>
        <SecondTagLine bust=Math.random() tag="textAlgin = TextAlign.START for LTR" />
        <CraneWrapper>
            <Text
                text=textSpan
                textAlign=TextAlign.START />
        </CraneWrapper>
        <SecondTagLine bust=Math.random() tag="textAlgin = TextAlign.START for RTL" />
        <CraneWrapper>
            <Text
                text=textSpan
                textDirection=TextDirection.RTL
                textAlign=TextAlign.START />
        </CraneWrapper>
        <SecondTagLine bust=Math.random() tag="textAlgin = TextAlign.END for LTR" />
        <CraneWrapper>
            <Text
                text=textSpan
                textAlign=TextAlign.END />
        </CraneWrapper>
        <SecondTagLine bust=Math.random() tag="textAlgin = TextAlign.END for RTL" />
        <CraneWrapper>
            <Text
                text=textSpan
                textDirection=TextDirection.RTL
                textAlign=TextAlign.END />
        </CraneWrapper>
    </LinearLayout>
}

@Composable
fun TextDemoSoftWrap(bust: Double) {
    // This group of text widgets show difference between softWrap is true and false.
    var text: String = ""
    for (i in 1..10) {
        text = text + displayText
    }

    <LinearLayout orientation=LinearLayout.VERTICAL>
        <CraneWrapper>
            <Text
                text=TextSpan(
                        text = text,
                        style = TextStyle(fontSize = fontSize8, color = Color(0xFFFF0000.toInt()))) />
        </CraneWrapper>
        <CraneWrapper>
            <Text
                text=TextSpan(
                        text = text,
                        style = TextStyle(fontSize = fontSize8, color = Color(0xFF00FF00.toInt())))
                softWrap=false />
        </CraneWrapper>
    </LinearLayout>

}

// TODO(Migration/qqd): Impelement text demo for overflow and maxLines.
@Composable
fun TextDemoOverflow(bust: Double) {

}

@Composable
fun TextDemoMaxLines(bust: Double) {

}

@Composable
fun TextDemoTextScaleFactor(bust: Double) {
    // This group of text widgets show the different textScaleFactor.
    val textSpan = TextSpan(
            text = displayText,
            style = TextStyle(fontSize = fontSize8))
    <LinearLayout orientation=LinearLayout.VERTICAL>
        <CraneWrapper>
            <Text
                text=textSpan />
        </CraneWrapper>
        <CraneWrapper>
            <Text
                text=textSpan
                textScaleFactor=2.0.toFloat() />
        </CraneWrapper>
    </LinearLayout>
}
