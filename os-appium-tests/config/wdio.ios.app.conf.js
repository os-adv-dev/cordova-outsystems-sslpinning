const { config } = require('./wdio.shared.conf');
const { join } = require('path');
// ============
// Capabilities
// ============

config.capabilities = [
    {
        // The defaults you need to have in your config
        automationName: 'XCUITest',
        deviceName: 'iPhone XR',
        maxInstances: 1,
        platformName: 'iOS',
        platformVersion: '12.1',
        orientation: 'PORTRAIT',
        app: join(process.cwd(), ''),
        noReset: true,
        newCommandTimeout: 240,
        specs: [
            './tests/specs/**/SSL_Pinning_ValidHash.spec.ts',
        ],
    },

];

exports.config = config;
