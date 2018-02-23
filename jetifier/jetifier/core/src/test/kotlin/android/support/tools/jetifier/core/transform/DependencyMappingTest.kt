package android.support.tools.jetifier.core.transform

import android.support.tools.jetifier.core.Processor
import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.map.TypesMap
import android.support.tools.jetifier.core.transform.pom.PomDependency
import android.support.tools.jetifier.core.transform.pom.PomRewriteRule
import android.support.tools.jetifier.core.transform.proguard.ProGuardTypesMap
import com.google.common.truth.Truth
import org.junit.Test

class DependencyMappingTest {

    @Test
    fun mapTest_oneToOne_shouldMap() {
        MappingTester
            .testRewrite(
                from = "hello:world:1.0.0",
                to = setOf("hi:all:2.0.0"),
                rules = setOf(
                    PomRewriteRule(
                        from = PomDependency(groupId = "hello", artifactId = "world"),
                        to = setOf(
                            PomDependency(groupId = "hi", artifactId = "all", version = "2.0.0")
                        )
                    ))
            )
    }

    @Test
    fun mapTest_oneToTwo_shouldMap() {
        MappingTester
            .testRewrite(
                from = "hello:world:1.0.0",
                to = setOf("hi:all:2.0.0", "hey:all:3.0.0"),
                rules = setOf(
                    PomRewriteRule(
                        from = PomDependency(groupId = "hello", artifactId = "world"),
                        to = setOf(
                            PomDependency(groupId = "hi", artifactId = "all", version = "2.0.0"),
                            PomDependency(groupId = "hey", artifactId = "all", version = "3.0.0")
                        )
                    ))
            )
    }

    @Test
    fun mapTest_oneToNone_shouldMapToEmpty() {
        MappingTester
            .testRewrite(
                from = "hello:world:1.0.0",
                to = setOf(),
                rules = setOf(
                    PomRewriteRule(
                        from = PomDependency(groupId = "hello", artifactId = "world"),
                        to = setOf()
                    ))
            )
    }

    @Test
    fun mapTest_oneToNull_ruleNotFound_returnNull() {
        MappingTester
            .testRewrite(
                from = "hello:world:1.0.0",
                to = null,
                rules = setOf(
                    PomRewriteRule(
                        from = PomDependency(groupId = "hello", artifactId = "me", version = "1.0"),
                        to = setOf()
                    ))
            )
    }

    object MappingTester {

        fun testRewrite(
            from: String,
            to: Set<String>?,
            rules: Set<PomRewriteRule>
        ) {
            val config = Config(
                restrictToPackagePrefixes = emptyList(),
                rewriteRules = emptyList(),
                slRules = emptyList(),
                pomRewriteRules = rules,
                typesMap = TypesMap.EMPTY,
                proGuardMap = ProGuardTypesMap.EMPTY,
                packageMap = PackageMap.EMPTY
            )

            val processor = Processor.createProcessor(config)
            val result = processor.mapDependency(from)

            if (to == null) {
                Truth.assertThat(result).isNull()
            } else {
                Truth.assertThat(result).containsExactlyElementsIn(to)
            }
        }
    }
}