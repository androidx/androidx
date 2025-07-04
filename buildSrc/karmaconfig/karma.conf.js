// Set a fairly long test timeout because some tests in collection
// (specifically insertManyRemoveMany) occasionally take 20+ seconds to complete.
var testTimeoutInMs = 1000 * 30
// disconnect timeout should be longer than test timeout so we don't disconnect before the timeout
// is reported.
var browserDisconnectTimeoutInMs = testTimeoutInMs + 5000
config.set({
  // https://karma-runner.github.io/6.4/config/configuration-file.html
    browserDisconnectTimeout: browserDisconnectTimeoutInMs,
    processKillTimeout: testTimeoutInMs,
    concurrency: 3,
    client: {
      mocha: {
        timeout: testTimeoutInMs
      }
    }
});

// Add 10 second delay exit to ensure log flushing. This is needed for Kotlin to avoid flakiness when
// marking a test as complete. See (b/382336155)
// Remove when https://youtrack.jetbrains.com/issue/KT-73911/ is resolved.
(function() {
  const originalExit = process.exit;
  process.exit = function(code) {
    console.log('Delaying exit for logs...');
    setTimeout(() => {
      originalExit(code);
    }, 10000);
  };
})();
