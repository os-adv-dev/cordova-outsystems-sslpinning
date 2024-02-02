exports.config = {
    maxInstances: 1,
    // ====================
    // Appium Configuration
    // ====================
    // Default port for Appium
    port: 4723,

    // ====================
    // Runner and framework
    // Configuration
    // ====================
    runner: 'local',
    framework: 'jasmine',
    jasmineNodeOpts: {
        compiler: ['ts:ts-node/register'],
        // Updated the timeout to 30 seconds due to possible longer appium calls
        // When using XPATH
        defaultTimeoutInterval: 90000,
        expectationResultHandler: function (passed, assertion) {
            /**
             * only take screenshot if assertion failed
             */
            if (passed) {
                return;
            }

            browser.saveScreenshot(`tests/assertion-errors/assertionError_${assertion.error.message}.png`);
        }

    },
    sync: true,
    logLevels: {
        webdriver: 'warn',
        webdriverio: 'debug',
    },

    deprecationWarnings: true,
    bail: 0,
    baseUrl: '',
    waitforTimeout: 10000,
    connectionRetryTimeout: 30000,
    connectionRetryCount: 1,
    reporters: [
        ['allure',
            {
                disableWebdriverScreenshotsReporting: false,
                outputDir: './allure-results'
            }],
        'spec'
    ],

    // ====================
    // Some hooks
    // ====================
    afterTest: function (test) {
        debugger;
        console.log(test);
        if (!test.passed) {
            browser.takeScreenshot();
        }
    },

    beforeSession: (config, capabilities, specs) => {
        require('ts-node').register({ files: true });
    },

    /**
     * hooks help us execute the repetitive and common utilities
     * of the project.
     */
    onPrepare: function () {
        console.log('<<< TESTS STARTED >>>');
    },

    onComplete: function () {
        console.log('<<< TESTING FINISHED >>>');
    }

};
