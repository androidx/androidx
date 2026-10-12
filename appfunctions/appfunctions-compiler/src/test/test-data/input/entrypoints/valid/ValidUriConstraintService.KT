package com.testdata

import android.net.Uri
import android.os.PatternMatcher
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionPatternMatcher
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import androidx.appfunctions.AppFunctionUriValueConstraint

@AppFunctionServiceEntryPoint("ValidUriService", "valid_uri_service")
abstract class ValidUriConstraintService : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithContentUri(
        @AppFunctionUriValueConstraint(allowedSchemes = ["content"]) fileUri: Uri
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithMultiSchemeUri(
        @AppFunctionUriValueConstraint(allowedSchemes = ["content", "file"]) multiUri: Uri
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithUnconstrainedUri(
        plainUri: Uri
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithStringConstraintNoEnumValues(
        @AppFunctionStringValueConstraint(
            patternMatchers =
                [AppFunctionPatternMatcher("[a-z]+", PatternMatcher.PATTERN_ADVANCED_GLOB)],
            format = "custom",
        )
        stringParam: String
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithAllStringPatternKinds(
        @AppFunctionStringValueConstraint(
            patternMatchers =
                [
                    AppFunctionPatternMatcher("exact", PatternMatcher.PATTERN_LITERAL),
                    AppFunctionPatternMatcher("pre", PatternMatcher.PATTERN_PREFIX),
                    AppFunctionPatternMatcher("post", PatternMatcher.PATTERN_SUFFIX),
                    AppFunctionPatternMatcher("a.*b", PatternMatcher.PATTERN_SIMPLE_GLOB),
                    AppFunctionPatternMatcher("[0-9]+", PatternMatcher.PATTERN_ADVANCED_GLOB),
                    // A type unknown to this library version, e.g. from a newer SDK.
                    AppFunctionPatternMatcher("future", 42),
                ]
        )
        stringParam: String
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithSerializableValueConstraints(
        param: SerializableWithValueConstraints
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    @AppFunctionUriValueConstraint(allowedSchemes = ["content"])
    fun functionWithUriReturn(): Uri {
        return Uri.EMPTY
    }
}

@AppFunctionSerializable(isDescribedByKDoc = false)
data class SerializableWithValueConstraints(
    @AppFunctionUriValueConstraint(allowedSchemes = ["content"])
    val contentUri: Uri,
    @AppFunctionStringValueConstraint(
        patternMatchers = [AppFunctionPatternMatcher("[0-9]+", PatternMatcher.PATTERN_ADVANCED_GLOB)],
        format = "numeric",
    )
    val numericString: String,
)
