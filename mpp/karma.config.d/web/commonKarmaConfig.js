
function configLaunchers(config) {
    config.customLaunchers = {
        ChromeForComposeTests: {
            base: "Chrome",
            flags: ["--no-sandbox", "--disable-search-engine-choice-screen"]
        },
        FirefoxForComposeTests: {
            base: "Firefox",
            prefs: {
                'dom.w3c_touch_events.enabled': 1,

                // https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/internals/preferences.html
                // Records the version of the policy notified to the user. This preference is also used on Android, used in tests, it allows to skip the notification check.
                'datareporting.policy.dataSubmissionPolicyBypassNotification': true
            }
        },
        SafariForComposeTests: {
            base: "Safari"
        }
    }

    config.browsers = [];
    if (process.env["jetbrains.androidx.web.tests.enableChrome"]) {
        config.browsers.push("ChromeForComposeTests");
    }
    if (process.env["jetbrains.androidx.web.tests.enableFirefox"]) {
        config.browsers.push("FirefoxForComposeTests");
    }
    if (process.env["jetbrains.androidx.web.tests.enableSafari"]) {
        config.browsers.push("SafariForComposeTests");
    }

    console.log("Browsers: " + config.browsers);
}

exports.configLaunchers = configLaunchers;