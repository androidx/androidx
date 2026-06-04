
function configLaunchers(config) {
    config.customLaunchers = {
        ChromeForComposeTests: {
            base: "Chrome",
            flags: [
                "--no-sandbox",
                "--disable-search-engine-choice-screen",

                // "CI-stabilizer" :D - otherwise webgl2 context is null:
                "--disable-setuid-sandbox",
                "--enable-webgl",
                "--ignore-gpu-blocklist",
                "--in-process-gpu"
            ]
        },
        ChromiumForComposeTests: {
            base: "Chromium"
        },
        FirefoxForComposeTests: {
            base: "Firefox",
            prefs: {
                'dom.w3c_touch_events.enabled': 1,

                // https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/internals/preferences.html
                // Records the version of the policy notified to the user. This preference is also used on Android, used in tests, it allows to skip the notification check.
                'datareporting.policy.dataSubmissionPolicyBypassNotification': true,

                // The properties to stabilize the tests running in Firefox on CI:
                'layout.frame_rate': 30, // less CPU-heavy
                'dom.timeout.background_throttling_max_budget': -1,
                'dom.min_background_timeout_value': 4,
                'privacy.reduceTimerPrecision': false,
                'dom.suspend_inactive.enabled': false
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
    if (process.env["jetbrains.androidx.web.tests.enableChromium"]) {
        config.browsers.push("ChromiumForComposeTests");
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