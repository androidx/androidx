package androidx.ui.foundation.assertions

// / Error class used to report Flutter-specific assertion failures and
// / contract violations.
// /
// / Ctor comment
// / Creates a [FlutterError].
// /
// / See [message] for details on the format that the message should
// / take.
// /
// / Include as much detail as possible in the full error message,
// / including specifics about the state of the app that might be
// / relevant to debugging the error.
class FlutterError(
        // / The message associated with this error.
        // /
        // / The message may have newlines in it. The first line should be a terse
        // / description of the error, e.g. "Incorrect GlobalKey usage" or "setState()
        // / or markNeedsBuild() called during build". Subsequent lines should contain
        // / substantial additional information, ideally sufficient to develop a
        // / correct solution to the problem.
        // /
        // / In some cases, when a FlutterError is reported to the user, only the first
        // / line is included. For example, Flutter will typically only fully report
        // / the first exception at runtime, displaying only the first line of
        // / subsequent errors.
        // /
        // / All sentences in the error should be correctly punctuated (i.e.,
        // / do end the error message with a period).
    message: String
) : AssertionError(message) {

    override fun toString(): String = message.orEmpty()

    companion object {
//
//        /// Called whenever the Flutter framework catches an error.
//        ///
//        /// The default behavior is to call [dumpErrorToConsole].
//        ///
//        /// You can set this to your own function to override this default behavior.
//        /// For example, you could report all errors to your server.
//        ///
//        /// If the error handler throws an exception, it will not be caught by the
//        /// Flutter framework.
//        ///
//        /// Set this to null to silently catch and ignore errors. This is not
//        /// recommended.
//        static FlutterExceptionHandler onError = dumpErrorToConsole;
//
//        var _errorCount = 0;
//
//        /// Resets the count of errors used by [dumpErrorToConsole] to decide whether
//        /// to show a complete error message or an abbreviated one.
//        ///
//        /// After this is called, the next error message will be shown in full.
//        static void resetErrorCount() {
//            _errorCount = 0;
//        }
//
//        /// The width to which [dumpErrorToConsole] will wrap lines.
//        ///
//        /// This can be used to ensure strings will not exceed the length at which
//        /// they will wrap, e.g. when placing ASCII art diagrams in messages.
//        static const int wrapWidth = 100;
//
//        /// Prints the given exception details to the console.
//        ///
//        /// The first time this is called, it dumps a very verbose message to the
//        /// console using [debugPrint].
//        ///
//        /// Subsequent calls only dump the first line of the exception, unless
//        /// `forceReport` is set to true (in which case it dumps the verbose message).
//        ///
//        /// Call [resetErrorCount] to cause this method to go back to acting as if it
//        /// had not been called before (so the next message is verbose again).
//        ///
//        /// The default behavior for the [onError] handler is to call this function.
//        fun dumpErrorToConsole(details: FlutterErrorDetails, forceReport: Boolean = false) {
//            assert(details != null);
//            assert(details.exception != null);
//            val reportError = details.silent != true; // could be null
//            assert(() {
//                // In checked mode, we ignore the "silent" flag.
//                reportError = true;
//                return true;
//            }());
//            if (!reportError && !forceReport)
//                return;
//            if (_errorCount == 0 || forceReport) {
//                final String header = '\u2550\u2550\u2561 EXCEPTION CAUGHT BY ${details.library} \u255E'.toUpperCase();
//                final String footer = '\u2550' * wrapWidth;
//                debugPrint('$header${"\u2550" * (footer.length - header.length)}');
//                final String verb = 'thrown${ details.context != null ? " ${details.context}" : ""}';
//                if (details.exception is NullThrownError) {
//                    debugPrint('The null value was $verb.', wrapWidth: wrapWidth);
//                } else if (details.exception is num) {
//                    debugPrint('The number ${details.exception} was $verb.', wrapWidth: wrapWidth);
//                } else {
//                    String errorName;
//                    if (details.exception is AssertionError) {
//                        errorName = 'assertion';
//                    } else if (details.exception is String) {
//                        errorName = 'message';
//                    } else if (details.exception is Error || details.exception is Exception) {
//                        errorName = '${details.exception.runtimeType}';
//                    } else {
//                        errorName = '${details.exception.runtimeType} object';
//                    }
//                    // Many exception classes put their type at the head of their message.
//                    // This is redundant with the way we display exceptions, so attempt to
//                    // strip out that header when we see it.
//                    final String prefix = '${details.exception.runtimeType}: ';
//                    String message = details.exceptionAsString();
//                    if (message.startsWith(prefix))
//                        message = message.substring(prefix.length);
//                    debugPrint('The following $errorName was $verb:\n$message', wrapWidth: wrapWidth);
//                }
//                Iterable<String> stackLines = (details.stack != null) ? details.stack.toString().trimRight().split('\n') : null;
//                if ((details.exception is AssertionError) && (details.exception is! FlutterError)) {
//                    bool ourFault = true;
//                    if (stackLines != null) {
//                        final List<String> stackList = stackLines.take(2).toList();
//                        if (stackList.length >= 2) {
//                            // TODO(ianh): This has bitrotted and is no longer matching. https://github.com/flutter/flutter/issues/4021
//                            final RegExp throwPattern = new RegExp(r'^#0 +_AssertionError._throwNew \(dart:.+\)$');
//                            final RegExp assertPattern = new RegExp(r'^#1 +[^(]+ \((.+?):([0-9]+)(?::[0-9]+)?\)$');
//                            if (throwPattern.hasMatch(stackList[0])) {
//                                final Match assertMatch = assertPattern.firstMatch(stackList[1]);
//                                if (assertMatch != null) {
//                                    assert(assertMatch.groupCount == 2);
//                                    final RegExp ourLibraryPattern = new RegExp(r'^package:flutter/');
//                                    ourFault = ourLibraryPattern.hasMatch(assertMatch.group(1));
//                                }
//                            }
//                        }
//                    }
//                    if (ourFault) {
//                        debugPrint('\nEither the assertion indicates an error in the framework itself, or we should '
//                                'provide substantially more information in this error message to help you determine '
//                        'and fix the underlying cause.', wrapWidth: wrapWidth);
//                        debugPrint('In either case, please report this assertion by filing a bug on GitHub:', wrapWidth: wrapWidth);
//                        debugPrint('  https://github.com/flutter/flutter/issues/new');
//                    }
//                }
//                if (details.stack != null) {
//                    debugPrint('\nWhen the exception was thrown, this was the stack:', wrapWidth: wrapWidth);
//                    if (details.stackFilter != null) {
//                        stackLines = details.stackFilter(stackLines);
//                    } else {
//                        stackLines = defaultStackFilter(stackLines);
//                    }
//                    for (String line in stackLines)
//                    debugPrint(line, wrapWidth: wrapWidth);
//                }
//                if (details.informationCollector != null) {
//                    final StringBuffer information = new StringBuffer();
//                    details.informationCollector(information);
//                    debugPrint('\n${information.toString().trimRight()}', wrapWidth: wrapWidth);
//                }
//                debugPrint(footer);
//            } else {
//                debugPrint('Another exception was thrown: ${details.exceptionAsString().split("\n")[0].trimLeft()}');
//            }
//            _errorCount += 1;
//        }
//
//        /// Converts a stack to a string that is more readable by omitting stack
//        /// frames that correspond to Dart internals.
//        ///
//        /// This is the default filter used by [dumpErrorToConsole] if the
//        /// [FlutterErrorDetails] object has no [FlutterErrorDetails.stackFilter]
//        /// callback.
//        ///
//        /// This function expects its input to be in the format used by
//        /// [StackTrace.toString()]. The output of this function is similar to that
//        /// format but the frame numbers will not be consecutive (frames are elided)
//        /// and the final line may be prose rather than a stack frame.
//        static Iterable<String> defaultStackFilter(Iterable<String> frames) {
//            const List<String> filteredPackages = const <String>[
//                    'dart:async-patch',
//                    'dart:async',
//                    'package:stack_trace',
//            ];
//            const List<String> filteredClasses = const <String>[
//                    '_AssertionError',
//                    '_FakeAsync',
//                    '_FrameCallbackEntry',
//            ];
//            final RegExp stackParser = new RegExp(r'^#[0-9]+ +([^.]+).* \(([^/\\]*)[/\\].+:[0-9]+(?::[0-9]+)?\)$');
//            final RegExp packageParser = new RegExp(r'^([^:]+):(.+)$');
//            final List<String> result = <String>[];
//            final List<String> skipped = <String>[];
//            for (String line in frames) {
//                final Match match = stackParser.firstMatch(line);
//                if (match != null) {
//                    assert(match.groupCount == 2);
//                    if (filteredPackages.contains(match.group(2))) {
//                        final Match packageMatch = packageParser.firstMatch(match.group(2));
//                        if (packageMatch != null && packageMatch.group(1) == 'package') {
//                            skipped.add('package ${packageMatch.group(2)}'); // avoid "package package:foo"
//                        } else {
//                            skipped.add('package ${match.group(2)}');
//                        }
//                        continue;
//                    }
//                    if (filteredClasses.contains(match.group(1))) {
//                        skipped.add('class ${match.group(1)}');
//                        continue;
//                    }
//                }
//                result.add(line);
//            }
//            if (skipped.length == 1) {
//                result.add('(elided one frame from ${skipped.single})');
//            } else if (skipped.length > 1) {
//                final List<String> where = new Set<String>.from(skipped).toList()..sort();
//                if (where.length > 1)
//                    where[where.length - 1] = 'and ${where.last}';
//                if (where.length > 2) {
//                    result.add('(elided ${skipped.length} frames from ${where.join(", ")})');
//                } else {
//                    result.add('(elided ${skipped.length} frames from ${where.join(" ")})');
//                }
//            }
//            return result;
//        }

        // / Calls [onError] with the given details, unless it is null.
        fun reportError(details: FlutterErrorDetails) {
            TODO("Uncomment once onError is migrated")
//            assert(details != null);
//            assert(details.exception != null);
//            if (onError != null)
//                onError(details);
        }
    }
}