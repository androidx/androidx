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

package androidx.build.lint;

import static com.android.SdkConstants.PLATFORM_WINDOWS;
import static com.android.SdkConstants.VALUE_TRUE;
import static com.android.SdkConstants.currentPlatform;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.checks.infrastructure.TestFile;
import com.android.tools.lint.checks.infrastructure.TestIssueRegistry;
import com.android.tools.lint.checks.infrastructure.TestLintTask;
import com.android.tools.lint.checks.infrastructure.TestMode;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Copied from com/android/tools/lint/checks/AbstractCheckTest.java in Android Studio repo.
 */
public abstract class AbstractCheckTest extends LintDetectorTest {
    @Override
    protected List<Issue> getIssues() {
        return getRegisteredIssuesFromDetector();
    }

    @NonNull
    private List<Issue> getRegisteredIssuesFromDetector() {
        List<Issue> issues = new ArrayList<>();
        Class<? extends Detector> detectorClass = getDetectorInstance().getClass();
        // Get the list of issues from the registry and filter out others, to make sure
        // issues are properly registered
        List<Issue> candidates = new TestIssueRegistry().getIssues();
        for (Issue issue : candidates) {
            if (issue.getImplementation().getDetectorClass() == detectorClass) {
                issues.add(issue);
            }
        }

        return issues;
    }

    @NonNull
    @Override
    public InputStream getTestResource(String relativePath, boolean expectExists) {
        fail("We should not be using file-based resources in the lint builtin unit tests.");
        return null;
    }

    @NonNull
    protected ProjectDescription project() {
        return new ProjectDescription();
    }

    @Override
    @NonNull
    protected TestLintTask lint() {
        // instead of super.lint: don't set issues such that we can compute and compare
        // detector results below
        TestLintTask task = TestLintTask.lint();
        task.testModes(TestMode.DEFAULT);

        // Make sure we have access to compileSdkVersions specified by tests; if not,
        // there's potential flakiness differences based on which SDKs are available
        // when tests are running in the IDE when it's not using specific prebuilts.
        task.requireCompileSdk();

        task.testName(this.getClass().getSimpleName() + "_" + getName());

        task.checkMessage(AbstractCheckTest.super::checkReportedError);

        // We call getIssues() instead of setting task.detector() because the above
        // getIssues call will ensure that we only check issues registered in the class
        task.detector(getDetectorInstance());

        // Now check check the discrepancy to look for unregistered issues and
        // highlight these
        // TODO: Handle problems from getRegisteredIssuesFromDetector and if no fields are found
        // don't assert the below. Basically, let the ISSUE field live outside the detector class
        // (such as in a companion.)
        List<Issue> computedIssues = getRegisteredIssuesFromDetector();
        if (getIssues().equals(computedIssues)) {
            Set<Issue> checkedIssues = Sets.newHashSet(task.getCheckedIssues());
            Set<Issue> detectorIssues = Sets.newHashSet(computedIssues);
            if (!checkedIssues.equals(detectorIssues)) {
                Set<Issue> difference = Sets.symmetricDifference(checkedIssues, detectorIssues);
                fail(
                        "Discrepancy in issues listed in detector class "
                                + getDetectorInstance().getClass().getSimpleName()
                                + " and issues "
                                + "found in the issue registry: "
                                + difference
                                + ". If the issue fields "
                                + "are not meant to be included in the registry, you can rename them to "
                                + "begin with an underscore.");
            }
        }

        task.showSecondaryLintContent(true);
        task.issues(getIssues().toArray(new Issue[0]));
        return task;
    }

    // Snapshot of support library: support-annotations-26.0.0-SNAPSHOT.jar
    // Note: avoiding string concatenation here due to b/180148524.
    // This is the toBase64gzip'ed version of
    // https://dl.google.com/android/maven2/androidx/annotation/annotation/1.3.0-alpha01/annotation-1.3.0-alpha01.jar
    public static String SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP =
            "H4sIAAAAAAAAALS8dVQUXNvGi3R3p3R3d3cNIdI95ABDCxLS0o20tCCdgoQgSHdLSDcijUidwSf1fX2e"
                    + "91vnHNbaa80fg6z12/e+9h3XFqAMA4sMBYWICAUFJQl1/wMN9dsPEmSpympLsiqqybHDQAF++GKNP/QB"
                    + "CeQTEWTh/v2LqpJqinKyWtpsqnInqoMDKsqsbGNoyqxMw4MjtZock9zLG6sb0D/9Yz//VTNHSxcnW8sn"
                    + "//FX//5F3L9/0czR0cnNzM3WyfE/fqd5noCN/AEU1CJkUf7idyQdbR00ga5sFiAzV9eQOFV1aE7M0CWq"
                    + "qsDnX905ur86L/eSi3i/rpJJ4I0LAR+Z9nNR0irPSLzoNTlZxjiUJ3rV/TxMFTTuWcQ94kZ3cnR5jr6M"
                    + "/4o8RoT0qQBWMfoEiEjrMT0NIFGOnUq/ftOuHHY4b1C3NafaeEdRajx1kqlUqxa7EZ1sjb5/0Ocs41DY"
                    + "NpO1Vzf9E5LhBjeWHt2sRUGdjmJ3VsZdThDP2FE3Xp20ICmy4TpB1KSLXEpTxrVPXByJU50XZWh/bMd2"
                    + "7hJWMaolzSWWzELuPIpUzFSnn0HoiAXR2bovIWImM7dRkfe3bxedL6OReBpuviUKftBZ5vIM/kycAQ0N"
                    + "1yYMfvVs9U0KwGp+tbjH48UJnhTe02LBCyTZ92K7auiZPJmkfg9+hDuil3JDAQG7AVm0/wDXzM3J5W+A"
                    + "RdWhJSGAZZdh8Y5Kqzj8cJMAuvrlYJkE7rigUd3UjdQ42p7spnnq4xmW6we3DGtlIQDDVbqr4f1BbuHE"
                    + "y2+3X2HBsSgYr8iHRTJRJnsUs+UmZziReArHmIGJPAVlrdr2tGCize5dPFX3ImtORQvs0jI1FTowiUAR"
                    + "DnPtdvE7mVcvZC4jogovg9pVOXESgIGickz2eFsLrWcYQkrmZA6Gkvxxa9Xtj5w7ZApRagYX8bZuUhee"
                    + "JA6RcGr7ntOrd2ZLONBI92GIr6PfBFCAy7BQm7meZPWDH1LAmCzrp3mlb19eHYQJG6yROT5Ft1aOYCHA"
                    + "i6k2AipL5HR5YhwkAW6nkF+D5knPZg9c2+SK16DChn0l08DBoYkW9Kr09OI/ErYgnAskg9Cdh6yHvyTs"
                    + "9d+iFzriq/to179Hb/r/FL1WOGOveR9DyOIDu3fhos+U1LETugt09LLSk9vdtwnFNaw2S5eClxTXQJLd"
                    + "46dqy0wvw3u5OsipC+uVuBwXrhHICsJj+CtyyPpXAMtU70wLUrghZNe3goIXnuQPoXBq+6HKYdzESAAf"
                    + "UvfFPlxFXzTGdCkjP5/n8iEfKKb/0imxbJi2wzU1MfOO4pZFJafaYeblIGMD9IsmHNCbWVxMhsPFAGE4"
                    + "3Hf1wVzz+PKiDc2ZlZivi81GmtQZsM81PF2WC1bWD1F+RCtaGGlF8rsyUP8arbaNC9DM8i+67yUgdD9I"
                    + "weEe7S0HQOgiIsRmp2uuPWC1fYyzZmtL31gAUv2IfWlP/j10KxAOR7tUz7P6Brkp0iq/xKpDzQi4O6+v"
                    + "zn5AkmYeHqDKNUjDku7Rsr/Vj1NAcK3GRrXWKKkzHE007LeVy9ZswtfftOsDCzYMcuDY8eFmdIGT6DcC"
                    + "k4pOQ1fy0sMVSN4/gS+plNFJ/3jjP6qY3WfpgHJdTdCD2roLzRZBZNtp9GgrKGrf2+dGDt5uAeGajZSz"
                    + "7uaQcnMsf6TzecfKSOKzXMEjqhHMAKSzYKVIw6Iv/hkOBx9GF9VQXrkq8XR6bUDPU58TEdptfmlvI840"
                    + "Kc8322+XMbLwVG1TtMmNFt1ra6fcz7fYaFea6V/B+JGtFzsG370wLEEW1a/YuriY/dfADQyEyO7Y72iZ"
                    + "WOIt0SwpDUVSa1QU+sZ5lf8KXLqIXwduTAJLdPErC+ziPp5HGXE65SH6zAG1HNw67LZ5j72JE3noqvt3"
                    + "u8pgK2B66iyAmw5nGTaeiQwGCdU8zTiN/dZSNSpVPQq25atRBvx3kmGKZfnRzNuwLjzYjb3shKVuave6"
                    + "20KRSOLU8l13b7+ajeIVowIZLlfCFnJnLmBipoIk5eFHcIjON3yXUSC6q4wj+vTmamWkH5Z4PjRoJ5CK"
                    + "GZwm3OkRq/ogAHqJrFFL4shAmaHWwXnSonngEjk3QnRfGKK7Umju9AhqxGro73661KxeVNn966Xm5uby"
                    + "j5fav9D9p0vtT7qK9hP/L+AK/38LtxsC1+8PuO89on8JdzLzKpjKjOIXcPuQgeP3cNcgi+4XcKVsHS2B"
                    + "Lv9VGmRzoP2eUSBgyJ22Y6GTik79gzSs/CQN0plCAi7g/eXgXCKgLPlEuUp4riv1IylB/yFFmpsmXaTQ"
                    + "2iqAPtu2ItsGD+3jEhUUVu56y0ZSA6s4ZF6S5++6TJM+fICIw3p6rsFwcAGamUjQpA4V6+DXT+LbNMrA"
                    + "lga4T7PRVKEnx9Bsx/fikLx1k/SbOLxeurpmI+Ssa1mmXF3J7wmKy+1/SS8mbbEaWkHpg7OLnLyepvLp"
                    + "TYbDy6jcViJClS1c+9DIcTI+z31ODu3BpQBfPG3/+tC31b447lGLvJfwueH452VXgUxmFAg66OV15W5+"
                    + "PxGOWsdCJPuX8JVycgL9a/gi/xW+PAUFdbBvZW7TEfyfifcL8MqJgMwT27izvIq402JH3nhCXQhggLGX"
                    + "edqJpL1sJh5RNSsphyoN6JMNuFuw8QWuUKlFa6EI6tjzjiWQWGSB+9N5Ox3W04rwraZx34Vb9dDtSBHT"
                    + "v8JA5f+owkKGtY6lOYuYpbn1TXy73S4Z5CspvNpJ2fnIo0OhzGocy/NFFkb96Vp0g7Ii4IiRVPRcGLXw"
                    + "Qayg6PtUIbenZjLVdpNOJ9vAwTVyJ1nKzkKOieRH3281UFRuwx7kVgOuElZwy8+WVw+3x+U3I/i79Prb"
                    + "P5CtQYuAt7xgNElgWIPC+yJGze78/PULanZe9h6TH9lmiCDwfoV8Kv+HW03aDATScgcDXf6gW612n5Dd"
                    + "Sa7g4o0Nv8/BTzI2s5WFsgjoABOQlG44SqWwNK/PUh/a4Ys9wwBm1kNFKi9YetKd+J98sWxrExd7ILLM"
                    + "9d4r2CMYRYrbGRh/lMinYnOWzKnYtL7qVzEdAeZlLJ9B8ZtgFuVdqJ322qivZX7/mYGjWsMDvzQLE8NQ"
                    + "h23We5gCiX9qMLgVMwFJ04RZq39p0X/0sbVbAxrajQa7IuLMFFoJk9V+SHPpdFho55Mdjz5oXuPQnZMn"
                    + "Dx37N0MmLjfZmw0Qclv5SdS3Mr2kXo37v8tnCwAkPn0GthO49t2zUdhX4E14QvONMz8cJzP0lKxLQHoP"
                    + "4UdwgvZF27QQaN/+IZeVtgFa2EOi0h3k9ju6+N9z2ZU93HcSY10QdjLEjCzliKYsHIQxs7qsPU/Y2J5u"
                    + "Osb2mtQeInzNj3KNBhiaV6elZVlvLrZc3Zxcwi3D00NklUeEFKeFBk1ajTTXeYp8eErTParGrMezumGF"
                    + "NfdTPZ9L/07v+aVsoRwA87VfUveDJdvOYP0vyUFvOOnHW4c9MNV6Sty9O9SFxrQ692iRVsyl5W94hW4Y"
                    + "FAhAZCMy5yIviWpxpLgk+5WBmhpON0FicnIIpaAc/v1vYiss/qZ5q3FZ4BeXNwEr+16JU8Io2l6EKgfy"
                    + "vlJaH/VIdz720DWbPZ+tNeZPHxaMK7WqeD9XiB/eAycoe8nJhKXEoPwo/nVqe+J0SgEcKJ+pmnE2pstI"
                    + "8pzwSkCCVfmlZJQQuv9PNcNU48MmMwhjZ0glx/pPnF21LO0VHd0k3VSAZq5/8Nbouw9V3yG+RqfGZewX"
                    + "hCGRpCLeVCzxsEFmzwnUh1TCo6Ux0nYojyEaCyWuVC7IYEdXnXjw2vts91Hht883G3jttOesuKz7+rM4"
                    + "CgPVq2+0HZHKHtWfR/P0z7h7x8Vbj1P4wB7HfElIocZmSOr/nJvZG0F/lmOeIbHTkQwFu9nNZRqRK/Fw"
                    + "q09jKRn6pIECDp03uadeuF7eJVtq3tfOVovXISG36fkH2IhFBRLePGG8123ytZJSAWsUaBueeZoXWTYy"
                    + "c3EDN7EJgkbk8j3kWjU4O7IiHz6KTxosaQ3zsBXXlzawguMg+V/HM9QTtYQTtNjVI+8JTk5Gz4ltI8/d"
                    + "sjkQ7xxopNADxqRpRNweG5+mSijxy2nYOodb6qxzQz13B0XCasYmxoglLFevS9Iaw2LgBwlyVxOJ517w"
                    + "+Ldc+P4Pyhq6HJEHdM2e9veXgmZSa9RmJR5kVD+NxTVDmT+qPUH6ca923DcB5P+Sxkk7gZxcIPv0P6dx"
                    + "90pttfPnNTgpc1QVrvpt2PKTtdCQj09bK0zjB9tn+VA9D9QIX1NnFIbvM7IS2ZGVOszxV9Rj2neA8a27"
                    + "Swozi5PT7fqiq9UEgEMRK0LHKgYo3E6RJvmL9YoNSd19H8k6H4nhKMvDAoOTpqWsed5eYgjV6dOSLI2G"
                    + "f7XpAscvceDvGAqtOh7fAJTvWo82ZbG1iY5TxUPXOKwYtHwI3Yh2x3I687eQYq+uxZBtLSsp5CwzUbiV"
                    + "y9Nbb30PrYGo0Z/bO9wocZiMNm/NmnvoYKQDDtCvGXO+ltMmMTYUEwMWaN9RaXwgP/f9Fpwng+bOjmBM"
                    + "bPwfiUaco4vbQwjZ1X+S6nu6Kk6O1v+JF1LeHUpcC4CDg+TV92og5Z0y/vpqR1IhJW3h3/M48vZOOr4d"
                    + "4cS2kUGek7ObI6RldAJyNdFMLwHr4liV7MI89mRTG1i5NBYu7096h7zWjSouDhuaKutKasmH9XwpBran"
                    + "ESt0LiqPnUW/HJePfU7jMUF2+LCfvfI2VTGYNpBLism+wiV98RaBjJh2Ag1PW/7IFTsYtXw1gnNk8z32"
                    + "E4P9MKXGu2h+p8CcuVH161dgUrrJtFWLhXxRGedl+UoccaKvGFjRr08RzGTkIvf0rNb3b5w6P48TpjeH"
                    + "H7lRag6AH30NX6SbR0WlffsU/Cr4uFIHUDib+zh2VeySN0AIHbw9TpAEFzMKdtkv2Flf/0nSraxgdv+n"
                    + "8P2/VSGKtkPjtMczIl9hrjW6p38PX7fNtGFLMbGlTw8WVnnaNZ6tQdGjTsX3jQd7a2JJl1skKVro1+Ob"
                    + "sxIOr6oYNBWEG/aVUFXUV79Xyn+MOhNvPae07WVwTTdnlGipPJ5i5Yn6+FJFZRpr2FXBllV7U/8+T5br"
                    + "s1xDEcKiiCZkO0bppspKdF9Q9RPTXu8hcTLuldzoAV9zbHNph4psHmB1V0hDIpZNJxaejFxU/cnC1oVG"
                    + "L5jtlIXX5fy++ZPMydOAhjABR5uywuUZ+pk8AxUVztNzWf3BByfW3H3QgCyMtadRm1SxOcbVJ1+peID0"
                    + "4kDAi9AXnew/0uXrDug5gHyK+acL08nRDejopmML9PwdMOD7hekbtMr4NaCQU85CcgcvRWc9D++od1HR"
                    + "op4Up8byI28Pef/v12UtvoRzeMbgsPKnHsvNq60TlHbRYepMOYHIUWo3l8ePacUh96Dxapn1Xk/60pdN"
                    + "u0JEAj2HVuNDXfazrsN2/a3kmWE2JP7OnPDomlpdh+cCa3GU2wGjxoxayi53FDu9REYVQZRE6MeZ4CkQ"
                    + "gzavBfvbeavbsUMp7MgzksjijIIRr/hWGtl0rIrkLB01dXEPNGGSgdv5QTeurfHIEufBc1Rf6Fza+rBD"
                    + "/dm7q3mBOD9GBuyDOOdg9LcP2gIJewQu4X4EhgZ2yvq3cJSxdQA6/h+L4jla0N/KNgUVvmrFpbRhkYm0"
                    + "87WTz2TtrGqipF4C52AqABntR6a87BjFnOkpdkspJQOUeh1oazMrljA6oamJ1ALO8Ec69HR4jeikEzHJ"
                    + "aW+mnPIWVfOivlT9LzXxVCaLK/+llzgd/6cntTQ/lG3LvyjbDGi9z79tdYib0I3O8pGqEODqBQwsEDXc"
                    + "fW/nfAow+LmdM9vZ5voSQDMJ+O/tHGJNZ6kQCNko6F+r6Xe6rpBPv+PVVFWfl8D0xdnKByAWJI1LaImh"
                    + "I7Ktt8dPYJOY9XWlWMiWez/n6wuMGiSm3UO8JUbwk/DHNccvQUumejQds9Ry0S2cvbjErj4CpVZt0nlB"
                    + "7GE/S5208FxCzKw+ca8kfu4UqMMQFm3K+80Mi3NXFj8ZcUPqVegyKZnrI2mbLBLbccPmXX18IuaB1pbU"
                    + "qEnZdEapRdUvSdk1fi7M1gmjUSgOqXuvJtxSS7xukqoodNWFE78Vsd4hO/BT4F1j5XdbB7d+yNYl8uAw"
                    + "VwZa+QeAv1LYJ1nvbI/hu0GP7WHpR6JgCzZphPoiRLqzazW0rh8ZV5d4b9C+yFXWjZBhETgy/7Ty/gJ6"
                    + "c1dyFqsjh93cJBOB3EzVrdmMClt8Hz3hJRY3a0lPogiZ+DV3oLgWTdnIU8SwN3ZdOEtGs9M+dWGWIwkv"
                    + "OtVxgNqEjz8753VfhnleYm8NfDmOzeEygysdnExz9mlIqjqnq8g/8sgl9Vm9gDcQYwatojSwXIdEvH/M"
                    + "4zDbYNYT6sRXZ/xTdugp7ND5GfIp/B9ERcZJzclN0RFk6wj845gMqUFz4oaMyJph3oEVo8YaTXdYDN9S"
                    + "BQYEU3SqLDQydaIxMpYTCckYD4VgLEsTORkhRiqCxs9ptoRfNd381k+ORLTGGqbmQYruwvPqVp/jyP3o"
                    + "brGFMG0z4KlcrxE8XVdlXPlVtvJsENzzXVYqILLS3iX3UbbsNupJf7YDACN4Vy5NKn3+LmdkeBtEAh3b"
                    + "qZaAWdEPU9JPAClbWC+ehj4eT3Vc0s2ZK35xTm/0vr7mZGHn8kqsmb8sqq00YzAl0wspua3qKoAvABDv"
                    + "+6yKSeBatLS4z5eRCbtyWEAKpkcSoin0Ajc/8XqKYIPybx14GRczTzNzEPAvWfldhCPbkZhWH5uZXTMw"
                    + "5FbV6TdRJ2QGkAZ3v2fpCWYRPB+sDvurAd+1js3/kb/25NPA4PDWzBXCM4PMe01ZXdHOEVM6YSnKVlPI"
                    + "2bHefaSKE2EzZVxcjy0Q62JxlkDnXZgW91ebmCdLJs1L/gabTDz6hJZIuVY4oNRcVSYFL7O6oEZHcXjR"
                    + "4E4hzDZvLZrbTVqQGMkQl6SbKSMRuKjKSUGz3g25557LTvWYXnMcwtlJEyn6wWJ5bp8FxdlHg0eTV05k"
                    + "5Hy9KOChX/QP1miPfLr1edLM+2mlYcv0bc5sTQk46jOTATQ07f2MI/gYQwewMbv5usf5xSVxbozofvMp"
                    + "EvD9A2J0dDIeMlJfmB8JHxbrCIRC6L6E6ArNLwjLgZzM3DTNHK3/CEjN+4DEzO43kcG6w/PHFMBs7e3I"
                    + "f9XPFm8J7KvhxMIoYElmYTSodQvP9GHHEHumvoZTrIX9AAc7w01oiX/JjW7Cz6e1FaYdL0kTnBIohRwB"
                    + "u6qeS4r9PMuEELbBBlPhQxI7TJB4tULx+gW9CkEAHm1iJXfSmNdZO85YL8XJDtzn1BhMXS0XyjjHPobT"
                    + "J0MEiDW8lGkuuqNbL2D4rGBd1HAZ/Hu/3MQv4fl8an8uMTaeHiHhFTIEjSftPRaHEHFRgYSbYk9aAQgg"
                    + "V8K2oKNiMb6Vn0t6f0EnlGs3phPZvnMM+yLEtwLwWJ+cH27mc1OidUzeetbOpzmBhSk6dwboiQ7k+re2"
                    + "lG/MPj6ycz8o0XrFnxIOkxcd+lnzjSO/0IE0TpoIinfu6VP2SNpNC5oqQaf3B+iNrxAzWT/bi81zjX0J"
                    + "N3inKxLzDgr7ecJzc/iCOF0jmnGFMunNFzad8NgfxWE/NYQfIIM6zk9jP5gijBgl7TvPhsEITB5Q0wis"
                    + "LytkWDio1QlO0d39tI9nJUwa/9YzlYOkK/86CNQ3C6exf4iyAsm1gVJKKdP/6yBQtUGluFQTu9g9uoGz"
                    + "XN+1KQXa2rmoXH9Ut3ySplDC3qLwkVwlbAV1z9eSSW6TW7rrGMUaOFvlCcgZ8aGjG4B9GpU0zTXU6HpC"
                    + "4WaqmQwylBRGDpUOiLwOjDcc7uXyTHyJoeWyFs3H3gW45CQVPRWGTpbERLhGtDgY/RyC1bOr8Y5MZmTt"
                    + "4QHchQYDmO3MzXby9OxLmDgJjg3o6ozeVamsHXiAOM/LBweL7OO5rPY9Fxxs+KAR+5ltizmY6RzgeoAT"
                    + "Eew7mUUqYCsgeA39I1xEaIY88n+RITkXM4v7D/+/DwKvSiLK1FQU9J6WTCstcw1GTzGn76qMmCd00+gI"
                    + "0uA0opFOhCfruU2a5E0o5/nIdo9fSy4XpSL13U+rkusjXA8+fUMgo62O4avIIek90l1+3WiaNw6nt3kQ"
                    + "ceKn4tjStxlN+FrsKvGVmXoOX0LuehiFS9htO4bANGKoEVGLGlAgLjaAHVw+KKJ6eu170ZlVfoT+RjSs"
                    + "llY2JRK5Z7aihvYhYLUZwVOJ4c4O7lGDQ4w36ELQhHrS+Zn0VluOioBUZ191YlFiIsWPgClavQz/aEr/"
                    + "SoXkXcw8bN28/laL/43vYWnVYW1O8PtxuxBpxWEJdCkLsxQLqRTSp5uzf8Or+x0vpBT34Du5PLt98N96"
                    + "/uIFduWfi5xA9rkQvCyDUVPK6dvJ2d000YplNUKczAF77ZyfddO8DG6IPztiA5W5IOUMNKScUUhBGiZk"
                    + "LH+UeXS4Kb7NEe5e54yWJREjB711iWaRPNwHCeEvV5xNX6L5K7sAQAbpy/XYG0DO4JEzJI2EC4pBNabj"
                    + "2h+4at+Kb22jqHLOHWRRPd3//CVovhCJp4EaUs3IGr4dvPngkfhX119emcEV5Dy11jjgFwKAFONG36Ko"
                    + "VqB46d3p0ejDln7kW1HkA0CEsK3+h1pc3t3MxRJoKeX1B95WtU4O3JBY6XGJAE0/fHiZD4AOKXi5ImjG"
                    + "h3B4vFgoiRmGshWar5SyIlqIYTDapSkdCefQ4wl9fNevsixM/O98/B40dlRHVkRKE8hDD2ta0SJlGhrD"
                    + "aTUlGdO574YUAYRa8WzCvPS6eLSJuLN6NPGRaD2n3KORM2OUHSajPk1KqXIR36eR2hUE0NYkzi5Th1R6"
                    + "BZYCl3lNX6x5qB3y7NRQBTutxZ9g2rR+HZt4Llv5kZeuk05nf+cuqGUzLaJU6FqMMJjynfZuQe+j4+rw"
                    + "Sjm87YoTuUTGD3LLLUcRZ9e+Dx5EzuD7N1bBgQTKWOpuX0c6fHpa5pFavWSGNTdbPkB+vkcthYDw7keQ"
                    + "jWzYFxT/0tRQMANZfb8y/0hHvo9OsmSV4PD+NlRlqgsoKCrQZiMfHI+x7Kux70/5Q2etRZ+F0TnWfFHd"
                    + "bL3s9fH194FuJzNsBO++T0PZQiXGMVpnhJeiCtqMG5iRsHN8i9K2icq2JkVU3VWiG54mo87o0IilX064"
                    + "EDw9kWg49NSH+RReiDWlGNXqArViQJST2AUgg289BJSsFb7T7dGPcJOGq1c8xb3whf98HBG85cpj2cI6"
                    + "4xvu8c7yJNTydWzr7NNTHPGi4a5OGwlPxPw2gjaV1ifrosE8/UhyfgpSGaUsM1RP7+bBT3ngyxe7bBra"
                    + "kzQfFyrvs300a8+R8sfYL5U4IyArxvJk3MP+kmgsb9P+gHTukOg1ovxoHRiUC1z5uadxsYhBQAxh+xGy"
                    + "KH7BV9HybxrbrAYtiRt6J2kKgaujS8gOR4ubokP9WwHJg281K0opnaLXOYztxYtC0R7GlZ2LGamaaXnr"
                    + "ejWc2fcfEmvbFlFcibNqihmWVE9gVw7L1rmg21pQMnzKQmmrYpc8VdIH6wjT44YN3CQ7M9g+HWcwSajG"
                    + "FUdcwOWm/G5luc/yXA3ucvJ5xsa6ed1gBMlRDPH4VSbt1FKamlffvoy7+mR5Iof66OVN30QVPYCkD+Cy"
                    + "HCaSK34IM8wbaPYYvlKLCH3dYvlEKCn8cUnM0p1Y4+ILCVd+BwEDgCVcBYp4lYH5nz6LT9/1dRiir03G"
                    + "/6qv2ejwukIQpGyQLI//V1gdXcFAC7f7VBrg4gQGurh50cg6ujvIOrq5/K4J0fFGyve4l/piVWtrario"
                    + "IhAnMmje7jNZwlACJgO37SaDogaH+/fL6vzLI/0kxJmEniYas0x+AA9nXqSd8JosUbyEOgBcVH1ZGe2M"
                    + "l/jU+wzWbmh6wpANaKDMbY+R9cjKgx2gq0RWm2nf8BZ6w4xwHkmUaSjXe0YGJV1AcHryzUpgmbLjOC40"
                    + "fdCsAN5oZARuKUZ3sJoQXrrCMqg86hpvjtrWer1xFdhwYI/c26hR8zD0OH+mjR0cP4Tey9lPnhgtHudX"
                    + "PEVjViWwL4oh1BZmEWBgW8SnPXFxRk+0ZnxJh+6zBqIC6J8wG1n0etRFUa3Gl5bie68JbmEKjOzb1TME"
                    + "7Pmfj2Jizi4nrlUu9p44AqBm8VecacKGeGKw+0HqH+fNG77CSxabkgjvRFdFMtmhJsUXzXdK5p/+lKx1"
                    + "PMd8pA/ZDpv/43bIgcysf9iOETWIsnS/2zCB05DUkOQhtsBkw14bS+xI4OEMgEonVEcpU3Vupv2cMvv4"
                    + "ZBnmK6CbEWFlVAYjI23o+nzeyer7cGthebY9ICA9gDo0arXCtJjHAIO7bdmhL3mR3G5J45xVWs11CT+d"
                    + "chGkB9ss8sSCYYlxw7aCTH0x8jG5UhadEQduqPG2usxMPI1evTYHAc5Qj+aprxObD5nRB3+OSfjc8Rjm"
                    + "7Mz23F0lNlMBvZMOf5TPY+OIev1sjaiKtyJp3IXgkPF8GzbE4ajsK7pOPS5hzHRA/3PQdcCp6I5K3hOa"
                    + "zk9rORbExdyGrrxnotbf2m5pcj9HVqw3lNRRcK7PHOcVJh0KJyAE5EwrM6O0HkXxEKrhGJXDd4Mv1THQ"
                    + "5FCUxsI9k4dHQXFwDJumLGvwsjUEj1lxEecLeEKoGhimGeeLjodRUiWQgPvd/AGi2gDanGe6/hRh0dc/"
                    + "2T0CsvWi9iBbNQr3f9suHTOQO1DbC/x72ZSlraeOo4F7y0YmNMEl8fFCVsZWkrF3VVPGjsgucS5dzZZr"
                    + "LBgV2EUJVApN6X13OIRg4nOmqkTFNna4HHQkTzTSqoIoY55k69J2uZm9IUx/cbpVObIENbisAYW9GZja"
                    + "40BEzXUAPxb+dKEL1w1uh6NbFB/31LmlMuflWl7ggxgMXFQXQ02OLs4K09Q1aSIkeMbEIRvUgEC91/qX"
                    + "N2SRkqqCMRt0SpsHFfUDHIl8WWhvQE4tUekxWhWpeM37MlrDBzGPylL3aPfCDT5VL/TfbtvGEAYl9JfZ"
                    + "NGU2sBif2OtbnVVfvE2IoSfaxfnoxtfzkN71SIrJiYNnnUe0OV3Zxp4xIkEe5ej0dRH3oFhrEvQdOf1k"
                    + "u4+ASxOqZl6L5ImN2nxMcoljpYFj9lUCX7zQVSYlkZpT2lOrOkuhDI3W1blhKxaXEvPyHZcxFjyVmU02"
                    + "Y8o+zjesfufyWLtOHR77/VTSW3DEmvHQso+NZyf9Jd7wk9VqujMmsg9iJIWY96FMD3fZqbk/XcFeJVBU"
                    + "BbA/XsyrN3f9uEjwVf1YiKnikEQkz5woPQovdOeO7ZU5qiIeP1hLlsD70anzxMgQFH/QAqozd5RxaNHJ"
                    + "zdgH5pR+9HAQhvzwabbRUl7EZO+sOLJ5qrCwZzVbdSRb5+W36kLbuVY1fRCwJGwPAfFKZP8C3N4piREm"
                    + "QrpJuLuhVYCKDd0YocFhSM0n1fvSFG0fZjdm5al61IqQ3juBp6q3FohPb2uLcUyCyse/yk9SSK/eum9d"
                    + "U1tO4bBHWUhlTiz3HaNOHI5hxPXQf4tdFv+q3BilyEamscINFxL4SoRyqQTrtuUbdJ9Nsq88AB0Lk4YB"
                    + "afzhc312hfyHqn6S6XBIfYF1iOOvEcfpFJ5ud1VS0/jTHpSO8RFqoqG91TCRznf8am8zjdYUir8IsLOZ"
                    + "iMb03R3F6EBX6+GX8g7gt+ky2F0lQ6uWW7sKFoiJ78AQyTf2NXZ6s02Rscbi3TVrg+aMsKgs+u6nLs6+"
                    + "nDgOLOT8vIL99Uz0v5yf30+NlpE6jiSuqLRBCjQVA5Vs9XNdfXEb1DECExo3kozntA4bAWKq87lsH4cd"
                    + "PazDb+zFvs8taoMdSSZk+jv390UvFoQvsoVPrkZGTKDQNRoANYf97+1RnGBrrMejte23mQM20qeZBDyA"
                    + "hK9H3xt4l1bsz8/BSqMg8DiQoVWoBtSlwHSXESibsG0kszRrXgyo4gi8vdMw/axgP6Kw9uRtHpvlOvED"
                    + "RBZFaTl6rtfTOAp4yvW4kXkWG82osbdHWT1iHVqV3bRtW86aF9AjOzcMA+tRLqphbbvEH52nbJcvmTg6"
                    + "Wwu4p12fvCoOmZt5vWgzi6dTMfst/NT3nP4wV9duSciCipibzQgw2lyoKNfBE6nKjvz29W7jpKUr8tXD"
                    + "bCQvvA2E18zReQVNcgXR3ABrte04oWwAQ5UoZ1e4kD7XNnMnDPcMN+Zs1nCtES9gbqGXz9/8jdgM49yw"
                    + "m4c0UcKlvOOKeJLWiPtb54GToW9vuqtZqzixOB1jo24MmBirhIYBXwzHWeNe6nfrUgd4ci/lEJknrAxj"
                    + "Ji6UBi6jpK9FUAz6pL9BHag7GFMK0aUEU5RS54TyQmMcgmEzLbLaqUELUEQhPbM0SZ3TzyfgEUU6JpCg"
                    + "bEEnwmHWvT2xhMKGXtOHmQWLfMHvUfZXgCBG3KpYc0PyGXxF2/dp6oK6CwTIepZN7Pg1P4/Yi+hmUVgg"
                    + "t+fhP5hGIVWiDNDqj6m6qrIUJILWtExsC88VZXFZZfjf2nxU6pdAfLnN3PvmuUYlF9cwThb95Yz+Kcw3"
                    + "jTFfGb2HdYSbn1xHBDtFT26PvsEvI8uznRuRNgrgKrAWxCg81GNCQAuQGkodVSqVclWWJrIdyM9NoX7w"
                    + "SvtAqAyBeiUpSqIIR65WbJFxCP5jaDVLSdSGFDLpG2qLqWDyKcMznnLDLWiCC85XrwuJHm2M5MzRKdNV"
                    + "VMBF3AH0c3VuaHEmyL7k+moRCBhUHm6AZFLCX+xtuaviNJy+0DLXrJgGefsNOT0Zsr4r+dBAP45qlUvc"
                    + "co3v68AUZ6Ln3cp5Jlh6Ul9ZNs2OHMPeQa82ruiSpOD8IqYZyrT9Qzf/6Ac6mDRxqMhaQ7qqEIc29aa5"
                    + "cP/gOGnykEbUh4jNs1ewP5K2+2p5YAmhDID+9UgHQvrvrcEEYfVODsyeOzpVasy6JE+vzmemuTDUibmS"
                    + "sCFFSvlKz3O5SgzgCpGQ0QORxAhgKJ5hwGVKjqInV62fiPptCV9ef6eNmIE/zVJmMGvKcFrj1m66T2HI"
                    + "of0koUCpmEF1t4b+WDeN/aseKg4T70txf/xObeahWkC2bLVlqjP2RmQBO4bThyDT4m9eeXkBIPmLMVWW"
                    + "sDefuPcb00s+BM90meToqHf6PMShbe5BnV8jMmGiUqDu8sF0Viu8uJXMsCUNURdamdwox+/s/hosYelb"
                    + "8nyejOKNRck8SvYBjeQHZWGBi13URNUDVy7QS6ZMwQbltoS9LJc3E5ttneqGGbmDJGjsywFrvEIdjxJF"
                    + "9qh2rGwnfUlZj66eo9Z24rdkGBAxhZZXG6GZeMTuYI3x2ejiwBGzqNREWBIRVPVyCu6mUnnoJ6ZK7lxp"
                    + "ETJMCCoryJqjn+iO7vKqhMuy2FaXfYjAf8Sq8/OY/c1N6Oy/tU4gewS0Bv7Co/631smPHfLeavn1Sr3v"
                    + "cqqHUt14b+b7mGYtVvPS5AsF1IHzwUpXZ0kYd8AuQ9/4c28hugDvJd10LeLxt/UAElowllX3Lq3qqb66"
                    + "xX1jSi8rtb7dYTnVo2ywpdwP53vnZFWuFiEgw4eO5nvzj49p+MmTEwq31C6hubeBag/lqQMGrgOrlbYs"
                    + "05oHRq44m8665/E4tf3Oe9SDKiSADN8d6kitbRTgMnxUIa4nWb1V7PcOdaU0vfTtj/NLoc3qODb7cuhF"
                    + "KBFl7e4H+M0vQciIuF8POrKgI8T1pdIcIvPJ532HHyM+Il0ebWJnwI4BuGp6joNdrtF+5MvJF8pIDWH7"
                    + "GbIY/4GvC9gJ9NNDgL+ZGYqfHJpeKwm878h/hWxPowejh0iAUcSCxsjIqveXq+/sYffvrr5N6puvPk8R"
                    + "wJET90YRaB7C19IZheE+AH0uHu/uPjfbejBf9MZrXoLf/SIhpKXJ6TZ/+kWYvvtF7CNfyjTbJ4IoS+ZP"
                    + "Y1a4XoYXUgeKSCnZ53LP5VxiCFXp05KYjIZ/BfeCs004sHdUhFYrTk5yU/xax6wYkZjEQ/J7/F7kci1z"
                    + "vpInnAgOMybUKMM/ExbxJjpp7PmSH7cil2prN3V65tkpLt4DWBr+rL7J5YxK0FQKelfb1w7IaYbxR2bw"
                    + "s0cC1rBGwFt6MprE/zZwl9NYheJlDzPeNY5p+ymS31frn90bRoYgi/wXpJWBQPDf2yq4mbI50ExfiUQJ"
                    + "oxFiXNozXhXh05oqz011PeFQ9qJU7k9J+rOt4smLhUHjWMOd6HvySe1Tz9a88DO+GJZ9A4EqcC8ALVEk"
                    + "upjawrz49LnVC5xqTFR6yQouwUJ5wRedqaDCBBVLLg3X0JO2ozrz3G7WIwMbuKR4rt303DpM5wp5vrtq"
                    + "9ARdOeWJ5dPPHJYCRkvtskJFKlds0zOLqUXeVCbDChllAZ9P9w1e5Pa+hE/fbdi6XNg0Wnp66VUi2EZw"
                    + "vNyx4tcOaPO8zPLPt3B762hyUg3I54nPn3n3MvWMg0fnwfVKcF7UCsihaGo4HRUVzpff9L5/zZJbdqnE"
                    + "rDzDh4SnrcF9TAR8j9+Atk9YTGJDOMtbdfHTcL4U803CvSd15R/6VipmXk7u/31AcG/GMf3djIOXolAD"
                    + "X4PJRq6iR1dgaYvmFn7Ni3EL5Z/kvPvgN1fqZ5GJN79Zfi8aEcHEyyztTRLuNrVAKaOsBAlWTZKMplSd"
                    + "oSZ95oABAW41dpuyx94JCTzdQht1vMwB873c5dppesoOKOUxqjVwRPIT30WCrgfWOippmmlotvkIErsl"
                    + "anxtuWR9K4BlpndmBSncLoOLGzOdSdcXchn85sXXpDHZ38gC+iW6EzDMBtEXjQndrwW+ORC1xgAXEmM1"
                    + "elP6VV7oqN+3V7LSJHy3Zl8OMjpDzzS9nEWroXwIWG5FeIfMcGuHBIxRbUMTOHBtlSpehQoT85WMBwen"
                    + "v7Bgd2f/+RmLU46QIyuE7PE/DF/uXU7/mZLMMNdOiiGF54VsF725T0lyMCEpiQIB5+4ZJ9fw87a/UhLA"
                    + "f6Qk2PLo52SkApCUhP0+JXlUcn4AF8FrAWKQBg6hRVsWSJNH16HCJiTb8fd5c5i/soStISMZgncr5SFZ"
                    + "J56GLWsiHAjBFXCUp9PG8dZ2nZqpq8lCyeQ1jyktRDK85AbP4yaElzIHHIsnpUzZVVmaF62J1lXuQdPv"
                    + "4bVlwuFyHfnctFTYvk+5dVFS6Z9JVx2pvPQ0Pvnob1ftoJi+Mvh6ZcBpbOY1dEjyqxmKiWcTQkZXg3Z3"
                    + "NctQOD6rLImM6Di2B1zOMzCE4KojNLfqPolzjPZVVjacXrOML9iPhY9eYh4z5BC+EEV8z3Yu9lP2Ryfy"
                    + "1J7kd5vJr+47VTNbx189bNE+3FvuGIGRF3hyOikRjVCl2pxOqK4s3EEL/FsL1lP0+fcWrIpV9mWvj1+r"
                    + "P1Rjd3UkDEEYWwyhvrMxPtJifxShEA4lnj+jQsF373pisbY+27YG2wYPtXqJIQqr6+/edSRekudLXab/"
                    + "/WHLSqv/KMeIZU0DXOf9w5ZXU3CGuNzjoc2vh6+IPp59/ZaMOpn69bt3PazDdzA/jBKDCj5MPERMmvuU"
                    + "agSzDSkLKRm/SWXplm+eNCq3mYhAZSvJJvSkH43v8T4nx+TgUqAvr4C4vnznbFv4227HDJ/EUSkS4Wnf"
                    + "UOnZD9nJMckVyTdOP8IVODEi/rcpoirQ0f1/enmB9NfLC9rRPx1lJZyrZjgZp1k7g9xZfb8ZInuawnDu"
                    + "e7Cw/Z00RrKTY5xI2t6W43HT0U1zDAGRHLw6EI2Y8cb+4+UFnCNMD48F0Er1LMOmIZHBKK6axxezsc/q"
                    + "NwuPWf5qkvDMnSSCYll+ObMNnIsAbmM/G2HJVBSLq+fH7xYem2aa0AhR/9NJJ5QkeK6k60Ppg9Gdbw96"
                    + "Xku+ow16UY3afMx5CMeWnFaBLer728sLu2a/m75MKmZwluB7x2j3BwGkS6KNidK3BigMNQ7gSYvmkUvs"
                    + "3CgY7cyrKJEVzEf08mp2ajE/Z2p3aIGX9/fbDmTR/wKumpmHrbXZT3PEkftpe/e7ex1+7g3RYdc/EeP9"
                    + "qcM72NvYXrwY3x3sLpiqfNWK79KGr4u4P3+3MwhnPi32EjgD0wHIaXa48mJiFHPOy6a9X98PEku7VcqT"
                    + "22VTiUu3eMaZRWMUiRNnWWuwG5FIJ6KS0/im1PM+quW5qXWPrxsvC2X8NkgMqY/QOjC5Q/hM/32QiNfb"
                    + "IbHM1WxanKJVM7ho9f1BYedQICGT2GVnj7FBzuSL3PUgCpcwtRkMAR0kb1ailhjL2WSMgBF1HasaldP1"
                    + "zxch/reqOW/35khGGGqgH6cF7YvVsj2MW2tFeOPS61T/jGoeXl604SqTHatfoD3X1SSOnjR3dX1Wcaqv"
                    + "j/9HyOrYsvCCEMAw0L+OYDUnRzV3EOhPd40SDCdmD/2Uh0O5w42czLXA6TZ5C2K2efPU6ttm0IvAJww7"
                    + "fElJPPF8BvulXocPfBicqcxFWQhT71Ss/DpRvw/CoR6Dt9uzYa7QSTGLT934lL3kFdRyiG3wLDWN1WYD"
                    + "sQQiuIFdSRcfDFFpXynZdkaN2qktL7yJYk4jbt5/s9zgnq7axOXjJr3M8jK8SRKaWiYChY47c+EaIboO"
                    + "d2ecr85nbg9LoM9JYRY0iOZCcOmu7tJsXOOzgphM3rouBsJhkOcp+npknjm64y22aec3m3ebo77G1nom"
                    + "Cu1ODGsmlehIiJrwCpFbTcYA4xQ936JIjptVrtI/7MYwUcKiXlkg83ASU2DIyMl9le4BNFwdb1Ul1N6S"
                    + "fjD2N/tBoW3+KuNNQIBq8dwhkRJpQ45VoWOhGXC+PyZ58Se7ZOlWOfieO9w/lIr30O8bPH9Kh3LnvSiX"
                    + "N1RP+X2NzvnqvD1GCgE/zTdlriJFjLyaC5ooLADh2Caa333PkJUy8erIodn2hWu/iHSiXn13ICCHwZ+j"
                    + "7pMImBafJoDw+9UKaKSn+pKwpWnZ6c0pCWOuWdTyS/blT54bkvcoMo0zhG+hbjVYqdCuVjP6oVgJI9WW"
                    + "RGkJvjUTGLD2ScOWJKL8EAIgina8DfIOj3hf6GjzsWXGAkIesQvEorXKdryXkuDdanpJhcQkqt/ROm/F"
                    + "8HRh67TKJ310R5/Pi3fewfnp6Jbfi6U5fOks4RiSw5w9oaDlPuqewyd7AUsaHiyrYyzh38n70Etb1EcJ"
                    + "Z/ZyAAWGFp3OelThQ6EXebe12r+9wdfIe6JaIekctAsMR+arLpywTqJ5XWxbYm9r83G6Jh948ZMxsONW"
                    + "7ftE8p/qPwDI3cUM5Pov9d9uMHX6X/Wf3vngInn8O6uV7yNJ1q5VbP6PnlbW2R/TBn93SHkLHIP3V2Ry"
                    + "xNSOeYoCxmlGSXdEkoUiGfZYNvSRM2Wbqu1ktZJL0uiWUruUSrETkQzDYnbsbPGNLjO2zxMFTOKred7h"
                    + "NDJZUX2XbSCr+/DQ/F1OvkzpkzVk71xxcpRMAgIVy7cPBxZUncVG17szQMbhubtaHF6E53B21OLH96/A"
                    + "zQ4Jg0ntgzuzKdpUbrbH3mHABKDYbtrmbTr5iy+wqebUOwhmst4/pA3RAvF/xMdkOFoMEKZN8q8LZJpH"
                    + "/815ibX73XmpQUeas1KwU7CxfkjyI2BMF54UUQhcDkhkk/4K8JMfrXrqWIQPZK+lwzlvWXTfvy8sRZbO"
                    + "tERNDKhyNusfjUvItciRXp4y/AollnTERxzeR8Pm7Th/tfjp02+mGnQ0BHcEa/Rhov4gGiOZmlfmfcuw"
                    + "e9MqZbDS1Cr1Gq6Cmsq8MbQzQpzvi2XYiRx9Buoscl85HH1/hZil7LC+nr0CkelCykCPYFwdKrUS2xMK"
                    + "HgVzPAeQZM9W0INOoEcHgKVoHNMzI8vTwvYd/0KLUmCE+sFh9RKQAU5k3Q0svTt6Yvjy4LXfu86bF1gY"
                    + "orfLzagE4h5PvJPNepPZTy3ji/u+xVJ07W1clF1qM9dKb3KHeuOTlhHZzTtd9HATEXkLuiY4nNDBSMek"
                    + "+JIUeI2TIjNogsY+5jYqXybIEOHtf/zMfYx9VawvWKVZ6/Kc2pwZlnRC6Aj87aeKxa5J4OLfnoZrmnn+"
                    + "mIlIooYuSZreP17OCT9aa8dCJxeN/v40PAB8ZBpaSCmdYgRdy+A/FCTeTgHcB8elglZpfOdtXxapvvs+"
                    + "C1tb6AwP08Kwht6QTGCT3k19Dsuk9I5Rn9fKuxyOTZvYgPtVvF6aswxXDE5pueJq8n1Lw6pgzVbrDj/9"
                    + "M43KFq5BugjigiV3BAsOKaem7h7DRseZ/6gfdhp4jVg/gBQVtqKGJNRbkHZgwchjMba3nH+B5Xnuaq24"
                    + "z249nJJvCY2/zwccISPxZUPe7XrLQ6GE18z3LQ3NnkSgOd0Xf74FcpWcBpIZyB1ZBz2TRrJfoUuA2Vv9"
                    + "uaOV9NiANae3rUMv9kZsRjmYi0kAvIJyHCytnUUqWCIoePtTCu1NhmzuAsFaAP1rO5Mm0Nnd1gXoKgm2"
                    + "/aOzN6AGqbdD76onmWQ+XN28ffg2PkT5MVqOjoZCCV33FFQQkWaRbpGMyW8m1JFiLPxovrQJbtc27ru+"
                    + "pTb/lrYHAh0VQawI8+gehPuUGDgdpmTkXqgnFZrxm/pyr1mERJDqZcmizCbUk1JEV0pySXutGOSWn5m9"
                    + "6n8kHGN51dTrPxKZPS72OHY1iXIiPD87+ktYRe0TEy5LEg3sN11pqLbPWtizsYkiA+Zf9BiJraPuied5"
                    + "MiZcPerElcYmnGGRLy+gT8J30FxUtvbJDc+JinsvriCKDNwLkWjWdidBC/18GvqsJ1YKxzPOuDL4fRRe"
                    + "y+m7fcSDPbzQvj0lsAYz97pSfYbc8lJKYLq6prVxodIrtUVWo6Yt2IOij8KbBE38muW8zXmFJYzucfHh"
                    + "cPChSPqbOOqWNkyzdS8LsjY6OnBpBZnUJ/qNqeS7Bpdgp6yGjFKlb/fkaznUv8AxWcPiVwcDE8Xa8XU7"
                    + "XiPZ05VlVJm936T/cdMYbbuGeH5/wPerPtQfmyYHNHNzdwH+WWveFz++kSFMXwMiiDHk8pE0MFwOt/tM"
                    + "w+lsFA1Uol7BydUUgOpBf9bxKUEKUqtmSy1Dg0+5ha++P6rBfsrGxOZhuI8soI+umOPiPU3aW132QfPC"
                    + "Plcx+cAEKUnhHY3ti9CCaAlOFa42Hm0Q3oNWIFOhRlBBn+bbKpeAm4cTcmg7GtHa9EX25TjU0UqnCsvb"
                    + "6iiXGcVwlrPZRPyDscDM6qdnL8d1sEsPguFCRp0Q3NWXTaupEmxfohKQsw8/PTp1HltPbuwNz9bfT3Za"
                    + "8Boi4Xt3kWfwUrHkQDxYY7RC3u8hmMqdXVX7i69fXZM7YpL1zfXws7ct7Z0h1IMe2niYvUd6q/79giIY"
                    + "x1A9gdGZzq8ao/BitgYjqvyDH5WZLY5bCwSjJbGC9Gdrqm5+Gtcz1/Bas0Gws0DOC+e/oAcAXRxsXe9N"
                    + "+DSaf9af0RrfqyRfuBQ8Gbh3z+m6uwih0xsRYo1pamSwUS1pjUXwCBUVd3Y9Xmz/4QGyoKJzWeY4Ecqa"
                    + "aeP/LYdX1jbSNtx/O4sD3tSkpwHrOLkVEsnnEr/zHk/lSKlGjiYYVc6jqvGNsOrX//QWu5jHH4nVOyE8"
                    + "Bm63lIL9U5giT17DOIvhjXpTan8TrgAiNk4pkShDaWg6frrOcnGGiwjr0HrlgoQcToTeqqXX0gwPNQ7G"
                    + "p+xwRbXmNU8D6Ethf/D8mmzuEmq64deASN+Akyk6ITSFGDgXccAuofRYdMo0Ngkru89pqBbxkTIZe30D"
                    + "oXpOQzpYVeJhZ/yGE55gA1RYqcjjYV7DUJZpAWuARPxRXMYeaZO8JRItqMCWW9VgdsUPaycECJpB/sm6"
                    + "tvhs8YDjd+sK1/8O/7GLrRvwl/QDu3s78gnCWeJhYfTCye/p9zk+fmOU9/vNkKkaldob0Nkys9NyPlT3"
                    + "vd/NBnDXcK7+bC7fntb3kDJg4pvi411n0pIje2k7Mj3bRwUkYq00THQTrwS0uw+nCyUKL5ltxhJVoJkB"
                    + "JrdPjtiUVrHqH7V3T90lnJRkKgaIio/phLhVRhHD81zwQCMsak8gFmRf15P0PpFKYY3Bn2l9JjXucXiu"
                    + "wuIyR1Y5j9275B2UQc6OfFO0tN51YrSPnMnKP3YkXcaivnoNLsnn/TATwBPaOHXckvgJrETtV9oZMjSE"
                    + "gFvMam2Eg9kLtry0kFV7iBBWK2I9jGdYxKJDyM2Q09sy8uFRzAJ1TTD2tKauuhmMgSE7VWtV1JD+yk/p"
                    + "D3OYEfoAhL8WDOQA/M/8fyefoOqEw4kZasKv+KIObm5cEznJrxI7oDeA+tWjGrx4Kb45FlkehPUDNerH"
                    + "MQTn+VT+0TDizyhCCKmVciJkMMjn16y7K3tObnpmnKCMO9yrviy3d+QGUQfMXang0VSN84bWNKR4qTUF"
                    + "WQTYKXCZLSvtJgREy9V6MlZaKitzO6JFqqQd2QfwjqTWxYUoqYaxWZagPAAnTQo1Ys6CeORP3tuNq3y+"
                    + "dMlMl/loUwLHFL2pF/LIJbDQTN+KM4thyNMp5LRJm2gDpp+OaYTI3zYRGRRWyDgZrPaoYph3qGsd+srf"
                    + "iOWOtE7I1lCtFzepKqb1UkUBDqfFvOsGg0uhPuLVuernbH5S5P1BjOeWfEWBTUGXW210zMzpflShB09v"
                    + "/DC9NPi3/aPEUH0CPrzxO6XbBg/NSfoThnyZ08A8Lqc3etfz6Vs7iqicz3nTOI3e6wT2O+Q2PemAqbSE"
                    + "5esE89uWxaZVqxC8J87N+/LvhPcuWZ3nHSuWK02WzZpz9Dcem4V7DrSUruua9RgtNHtEEa5ILbqVmX+e"
                    + "cw51Hz599gJltn2z8rZymXLEIGoSKbYqusdDUCr6EFa9/GP1Zhpv8/S+WTzMtKDsFF+DQVjnCdmbW5wf"
                    + "gyKsq+ViC3IgM+CgoJh+GRSubi62Fm7aTjRaFk5/mGCitc3U5wC4oslSKCklz9VGS1xBgY55GY5qqYHP"
                    + "Q4OLcxXkrk2Z3CrgDWA+sHxUn3sR3mfYADo68ZusucqOlrJCpV0Y2Gm9aHH98jRW3N//Gv4Q2cD4ue7x"
                    + "Cu8nhdSEw+mwhzqpir2ExxocHVwJXFKD7xKxrPwedTOc2HPRpar3pVqKsZoZPUsaKScYD6mBeqNfreb+"
                    + "QGmY58y7qIiX2Fu6vbbFSmcvReMGd0ZlooZ3vmrwiaTTrHu+OUE5Irsy4qGwCoJBSFar0YMOdJ8qg17H"
                    + "zU8ocZVmjtLPnKTFy/fwhqZLoaVCimxJ6L37bAjDeEV9a/FtCYcp7K31W4rS5I/mifUnjAZfyyanTrae"
                    + "qYiVv9aD1ixIGv5SbTJxtRhxkWlbT2fPjx/tbkRjVcaEVsbulPrN42QdUEnAm6CGLDA/jPCAeYxvhwkr"
                    + "VT5c9811QMujMTvackcO/TADnnqcrA+3a4VsCaG2s6U8VcIpQx+UtB38eYqVK+1I09IXy3ASmfuhXWo0"
                    + "XSNezNvVWp3QfMz6NAU7j1CqV7mdAa8155nB1UOIJ2LV46EkCU+CXQNNtYjJnNpdFLj1JNcUkRpZajA4"
                    + "jQhoFZimGIX4ZLQkI0a+0CqtgIYwuv6szvVJ2WOQprDf3TF0ylxPr5PMasXnfUO+eP2cebwCZuZCKRVO"
                    + "CbH1r1VrikLeHPDnInmVCAVh8nSZUYqbV50gG1yClQ84Mgx7jLmMx/tH9Q99pEfwDE2QxoDuqc/w+l/K"
                    + "TfaZFNTPGphIZF6iX+DJpCxNxFNQlxpam8HXiqhJ0w8Q4i/nYZzxcAt7II1cim5gb6a6dXkNi1gyZXsG"
                    + "++TKzZ5c0ne5SLgYSbU9ewHzsleSHSEiA0YcAUOE9NR5lN2Zo11SYtO0aukkMqdUFqGZpWG0NGWR9etU"
                    + "zasQsXqWeo6clA18+CkUxC3+RGL6knbi697xtmKeSt/0pFAeVJCWMturkBenWwPXVOTHO6O877lCtXL9"
                    + "fzwupLHYC/d+P+N/eGTz13H549ZSvk/bsjceweH6RHbLo1K6xAd9apmdfN01h/02nRDdipEEkrapNmP+"
                    + "8dSchQ4bJ498M4O9dknEY/Nm7QwNSs1s5hkrTANaMWGV9MM9hDFuegBIqc+C2VLxPQ5qrJ7Ae/BN4yfn"
                    + "1P0P8dutPKlJ8eZKMjVjm+ERsjk22lk6AvRxF/kapIfMWObmtnQCRFWc1ADb2AJs6duH2GNoTZdGKPNk"
                    + "QvoTiGn7d6C8xknU0pnbuLo8ACwOrhVrz6kv9NMTHIbIK8PcA5vbZrJjSp2CxJ2XSkV73pMFqiO+ebGs"
                    + "GRKbAxsHIO81I2wG3ey9ifNW2VY+vnd2c+hy0Sm2i+kzDAt3vk9h+2s5Ww6eIUS0dyz0WZFQA25GVO1g"
                    + "EQtGx594JXSUmD7QI8bH0ooLg/7GbSk7SUiaYkqklO7xsCcXlZlYh7fMVnemKtcMxyU1Td4WnvWpxuVP"
                    + "Hgfu9RWUJ5C9CYP+9dRRy9b7T3/D913psQ4IxKT4Co/nlfrb2yc8pWBtJSVpc5E4OJ2aAgxo1gfs35pE"
                    + "vj64BuyWYWwHdIkODl9vvblx8vh2P7uBegzGDzJEPeZkwAQUDR4LBhxeyma187mTzlhkg7OQ/S3bNhhf"
                    + "vkPBMhDoR7lm0atJO27ZrvioEBn/IVk8UCJv49saeSUyCtkq61EXexioH7EAbekoeGjenOotvslbNcR1"
                    + "3r4klk1UJdAYw6OFbCfXnrUbe+imqagOURoO6YKvGONurGMZ1MVJvHN2WL1lYqY1R8+e9uLpXDyKHLBP"
                    + "O8GSZ/hUAN+qpTr1sKpkB/jafW6yeIxElRbzQ3e5seSAneoUeCkvOtRRs7aIX2jTFyce1/WbxeNTa0mU"
                    + "SzBsgvLTVuF10VgJj43olEo9GoFXcZl4w9TYlJgP4EoZdFaEugDPEUJgkfckzWBCynPiuio/mxIHGs1e"
                    + "3b54XxV79YAKCz0sLrBOhUJiBQzbw5XZGxe/oh26T/DjntWBR66YIHv25R+mmVqQ0/T3iVucujK0JGH3"
                    + "y7EpVuY7bXgsDTfM1u1cC3rGoIeGem8einYVO3FxDfcLMV9OCZ0++AYYa5bZldzmGLzauhD7Mvjbfy1A"
                    + "TM8WY0jazIBiia/ZQwOomP4Wi9MUh8Y61DhWH8+1RhKgOfGbJUjeiWtapx3J2hJ2fEqffSgQs9e9U4B4"
                    + "vHWcJfmOzKolGxE581t1tZ3SiF0FfPp0hGvAQHShy5TNV8s+Gizx3GOMcaR6YjaiQY9W+rsGWnCbL1Xc"
                    + "2P/TyZnHQ7X+cbxLyF6Wa8u4jVA0LYRBlnCt5SIhSyFLSM0PKaKSUtYyZIukyIjURGXrlp0bEVrppiyp"
                    + "3LRIstTPzNCjr+TU+KuX13mfcebpzMzz/XzeictCrNpGCOs3xHPFKzhRmrZfu35aZM2e/+53ZMbWGimE"
                    + "aysGPFloE2R5utDyAyu/6OWNQrZCF8Qp8/xWVMmsku++dK2WFN+97DFbYYfT84tNFcvziw5V4gY/aXT2"
                    + "HmjjWXSMKdzwwZE/4khD4LsPXjbh/GyDY8alnq1/+fToYrc/dk/pX05Jl1yipUsa+kqVR6QZVxo3IXEQ"
                    + "oui7n6pqvc/Ovhp/0l2B153evizk8mG0L7PYDbea5+SaNBh1KJGSTc7Ul569qpuVUP2JHJn5MOTmeqr0"
                    + "4lsct63NN+Usduu/p9lslty7p1O0ODgSH5zzLJT6T4/zpt1W9SPsRW+rTyo7mX14KKHJvZfpSgyXlObo"
                    + "cfFhZs0OG0lhP5c3qjFPtTRYNWxlFW6Zq+/9MvLUz42pIbWTVHz6AYedEA/V1hn5D6s4zi9ss/tAfK3A"
                    + "6D0lMHL5ZdSQcyEwly+2YaHybJ35Db7+U8utP+zMJ0wYEF2mGhB/JJLTc78amZ3Fzd6KL1kl0DyhQHQN"
                    + "u8sp+urEKWGyIdnGZVcyw3UWZeRKd521EG1NndwDZEgnGg85Jzkci2m2SWKU5rNpAk9fJpE/mYQ+sUhe"
                    + "SYlxeS2t2S822du7I6HOVcx0Ip1+cdUGhvfdpAas7K3t8F9W8Xcpb2Cwz4l1Cdmn/v489sbNhSnKq6LM"
                    + "2YQqWFif6lhSc+6AVnA/TztDQfLwKvEl9XF3IL31lDpyCJ8eWhzLTGs9AVnUy9OXXtG2xp//QHVGv7oz"
                    + "1ocPShw+/PYsCke1LxDlzKdZHIokL01ZvumTy1dCjd5xwJFePK2vKDrCGewR55qTGPDouJbkZVsdD6cl"
                    + "Gbs885SMOmTr+XKEVt/zbDi0jtwqm9PiTCAJEde3UZzvWe7XLTlZ/fAgn3GLfnBb4HqDeME+akaBmXTn"
                    + "YMP+jIrW3j2v+FRp6/fV6KEYm4bay6810j5yF73Va5OiUkafHTvFZh/8j2koWWJfrXhAkGaHNUHY1/mN"
                    + "UrzjFg0Wjf36Ui0bz2f1j7B8vC9t+kRRfF2PgG3wyXYVvxJ6Ogp3M5Up/Ib1RDpKp0fukGw0qa/dSoBd"
                    + "hqJC3GW6u2kIbCVm5iicXjB+eVt/MNO08Hbc4eMOZpqMt9EvnNosW45uXiNcxRwmpLJnoNUgTDqRnBnw"
                    + "tqCAn/7hRurdXfq7aPUm3jOG/MpJNbdHe2I+0bNRUVt5j+CKcdFzH7Xk1m8VfRE3t9gsb136hduRGc88"
                    + "0i2NOnTjq19Kqb/QbjR/F1WX622wqElMzfW4rIIEJa3SIbGqe/we0aXXUZocQ1/F1UaZUo39X0JMLlvU"
                    + "eC7fZlKh7y82L8XSSlc2LdblzRlViYyuwYh7K7qK1efesScKbFsf+OK9zPHSll4NYZ/crHVkN1X/sZYv"
                    + "H1Pnqp5c9HT7WqogMSelRGXzFq3T1eZH9ipuD3rFmrx9hcr4yu0JjGiq5vFLHYlUdeQdMREn3v0E+qY3"
                    + "5JmlF07sfc90W9joPpOf78zEbcGAF6euaErz8wlN+vkMp/j5wp6lh53sU4tNcht8XE+PQ3kuVCOSSL0d"
                    + "R5GgTz1+VEomZsDVSUrYpFzbXSVLvyguNm5NpjyhoL6V/NR7m4iyoMfL8CeharXPnEyMNnhXkMzodj55"
                    + "up0vctdHCd+4TPnNbcGfmyLwofnvflv+binNzvdxLK7v41DA2gk738qC4TKdyor/pbEt0jmhGRUYWl3L"
                    + "qa4zdM5X1oLsJu8/NvKgsVhYVSzfQf6stUJKwvBOvwcuDpTEEpa9spR9nty7+Peee5lY6DXMrc1P6Hsw"
                    + "ILJRW6zEcjDXN7cPpoej3pZWaYxfV7bxz3tyM1xbS3cf9/Fbgt5Ob4vxT+XuX/VEZvQtcHWdMo7wA7wC"
                    + "9r16Z8Q2FfrejXM6Oje5+ZIicyQxwyTWT2tMUWjfAQmXUp3I1Xcqqxrqi57XJ40MMRYtv5i+kqhStMh2"
                    + "zd7olKatDvahl8VyLHwy9dfYzItoVQkN7y/cffvC7i8ERfeI9F62kCYfnYtefP6hgfubtsSw4LeOLSG7"
                    + "JXGwvnpGzvWMNqf22FSfJ+CG8M1cmY6lBtJnJCXJd/XlT3Qa+D5bLJAd7+Gn+Nz4dtO8341uybxtuBhk"
                    + "Hpp/6k4BPs+V8FmngCn9sPw1sdDy5oHyplH9lzUxKaXJbDsaRXhEbhclS1NT/nbuvhHuTMkLYussSjTd"
                    + "mrV/35W8UsNOvehybRx3ONNj5frCtRFlrqxC7AnyeyLe+LEmlG0bVKOYnvNQapGKWOgh5y2015c8d0SA"
                    + "jWnXNLcyd01D+GwaSqud3p5YNJTjyzyzmtmmOU+Uvsytr55De9n3Q0jb6KXfnqRj9fc/1sgcMP52lV8Z"
                    + "X+XvE7svxr1PSNFtTgvLdCCyc2Z2p+C6tE3MvU/8U9Akv4Ez9H2MC2u2aUvwkDFX6Ca7ekN3ozzh4u51"
                    + "Bo/mVysYeC1tLo/sHwuJzreolrPtiGpf4kQkrOjAp5GV2v/0//evurwaq53Op4cvNO48uEjnFudrPO7f"
                    + "vWvmiZwfde9TWF29o1e0XDk9z9TwVF1XUP7DnjrhR6+Cy5z1qK5EuceOQTU4Zp2qUcIDq7UfrDwpYm3K"
                    + "ZjUPvGJ3zk/e5lhT3Ejh86O6uTwxajN6DqqUdvgaG9wsAzRrr+9aKL+6lafYeCorQ5beOhS3zJ7JHj+5"
                    + "TeqBx3218TBuIWgqvMag2UVXXDxWq9sdd6TAo77Vhj3lcDY1wETyenPnhI3HVYokTjwnIqfgdf6Jbn6q"
                    + "7ouUyMy61JsblRjaag5Pwaz2ws+8j/6ia6vljocE3/zTr8yUkOWcVFJ3e2RV0dvQkyyrLAIHjvBev6qV"
                    + "vYKhrU7NkyDlig7YyO8Rqbsk18+8pcMuqU7+TuyEXPmq1316yiQhiX/7NdoEjTYTVum6MZkyKaGlTCgO"
                    + "jUWNtfyDZru8OzJ6ptl4aI9JVTvfnCmCeJL3Tjdaj3qaqP0vk0CHxDmMBrbsd4+YfH0I6PXxWT7+2wtN"
                    + "xhzlWlysHWMpeGOC33LTpfaFC8rU1qkt5Tbhf6Ere3T3Ncsyt90N8nVf1pJx84/JCR0cML+XZa0i5bjk"
                    + "qGDJo43zooNkuhrvrYqI3loipi19XT89MVlH0C7y+E2purweqvrVlYMZ6yO8wuwDvUSEPWxsr1GihhLd"
                    + "XHkuSHPlO1ZmfCq3KRl9d6q5f/+TFzfe6A2eseWpHPIf5jY1/o1JgHlmzz3jMX/Of/Rfff174WHQtT/5"
                    + "yAheOuf75n3sJ/abuux/dJjAN4elzTTlBwQo30dPvWUGwjcqfoiDunmEo0UPMMnnIRL61RGSzDy7bR3S"
                    + "oFIc0f6bi0kwDoHQo42AhqxYrNqQB8XRiEdhw6CRhjioSkY4Fnas4mTIhHJgxHTjwKAKhjjow0W4Ck5M"
                    + "dlwIhJ5YBBzlwmiNhUioREVIZ56fFKRCNDR4IvT8+Vh8npAHnZWIt2MBJoPltBUJHI0I2MiHxdgIedBK"
                    + "iHgEAYyOQoiE3j6E3CyIxeIHedBUh3ilv2Py1kEglKYh4EFhjAo1iIReMYS8LILRMgaRUKSFkAJi2LRa"
                    + "kAiVTojYvhCD4AnioMQI4VRwGJVGEAm1PQiZLIFN4gOJUFSDiMN/YNLWQCAUtiCgDh6TvgUCoaEEAc9K"
                    + "zuorgTDo5Zjy5y7+ZUsHPAm0TaCThEn/snsCngQ6EtBJiEt+2ZgATwKL5Ogke+R+slYO0bBUjNA1hNkr"
                    + "xpAGi7OItmoFlhot5MGSJ+LdXYmt8gmJsNaIiIryP1FyhFjY4UPYNIXZGn2QBTtriDWyGlODDQJhTQsB"
                    + "NylhKG1BHGwiIVyVMrZeEiTC+g0i4lUwlHEgDhZOEG6/Kub6CYTCggWCdqlhqFtMe5FBbwDh7NSxtAim"
                    + "3exAHH7K/xQNbOF4SIT5b0RU0vpxGnzaXQFEmhHpwtrZA86QBlO8iDZPB2OmFyJhxhQhr+v+ROIUYmF+"
                    + "EmGJer+QpoR4mBBE+Cv6v5IXnPb0QQIO8RUNfy4PB8kwRoXIRsY/E6qCXJg3QVx/E2zpE0iEKQlEFDSd"
                    + "LTMBWXB6j1h1Zphm+RAIZ9QIuHoDpok1BMKxLAKSLbAMaSEPDiIR781GrGNJyISTN8TcYIV5DgehcPCE"
                    + "oNnWWMZQkAeHLYjHYvNTo5dpX1rB+ACBI22xDhMgE26YI+Zru9m3z3+0vcj3lUXbXjTa/L3t32nvs2DD"
                    + "GD2bwO8eP9P2sakxCyvtON3xn6zxL/R2W2j/+j/MR0DQGXUAAA==";

    public static TestFile SUPPORT_ANNOTATIONS_JAR =
            base64gzip("libs/support-annotations.jar", SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP);
}
