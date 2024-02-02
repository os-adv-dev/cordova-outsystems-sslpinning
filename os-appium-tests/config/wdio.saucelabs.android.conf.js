const { config } = require('./wdio.shared.conf');
// ============
// Capabilities
// ============

config.capabilities = [
    {
        // The reference to the app
        testobject_app_id: '',
        testobject_api_key: '',
        platformName: 'Android',
        platformVersion: '10', // e.g. 8.1
        idleTimeout: 180,
        maxInstances: 2,
        orientation: 'PORTRAIT',
        newCommandTimeout: 180,
        privateDevicesOnly: false, // use Public or Private Cloud
        enableAnimations: false,
        autoAcceptAlerts: true,
        specs: [
            './tests/specs/**/SSL_Pinning_ValidHash.spec.ts',
        ],
        exclude: [
            './tests/specs/**/SSL_Pinning_InvalidHash.spec.ts',
        ],
    },

    {
        // The reference to the app
        testobject_app_id: '', // find it at SauceLabs App Dashboard
        // The api key that has a reference to the app-project in the TO cloud
        testobject_api_key: '',
        // You can find more info in the Appium Basic Setup section
        platformName: 'Android',
        platformVersion: '10', // e.g. 8.1
        idleTimeout: 180,
        maxInstances: 2,
        orientation: 'PORTRAIT',
        newCommandTimeout: 180,
        privateDevicesOnly: false, // use Public or Private Cloud
        enableAnimations: false,
        autoAcceptAlerts: true,
        specs: [
            './tests/specs/**/SSL_Pinning_InvalidHash**.spec.ts',
        ],
        exclude: [
            './tests/specs/**/SSL_Pinning_ValidHash.spec.ts',
        ],
    },
];

// =========================
// Sauce RDC specific config
// =========================
// The new version of WebdriverIO will:
// - automatically update the job status in the RDC cloud
// - automatically default to the US RDC cloud
config.services = ['sauce'];
// If you need to connect to the US RDC cloud comment the below line of code
config.region = 'eu';
// and uncomment the below line of code
// config.region = 'us';
config.protocol = 'https';
config.host = 'appium.testobject.com';
config.port = 443;
config.path = '/wd/hub';

// This port was defined in the `wdio.shared.conf.js` for appium
// delete config.port;

exports.config = config;
