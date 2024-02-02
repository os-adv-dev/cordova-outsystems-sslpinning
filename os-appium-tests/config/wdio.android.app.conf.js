const { join } = require('path');
const { config } = require('./wdio.shared.conf');
const waitforTimeout = 30 * 60000;
const commandTimeout = 30 * 60000;
// ============
// Capabilities
// ============
// For all capabilities please check
// http://appium.io/docs/en/writing-running-appium/caps/#general-capabilities
config.capabilities = [
    {
        // The defaults you need to have in your config
        platformName: 'Android',
        deviceName: 'Nexus',
        app: join(process.cwd(), ''),
        waitforTimeout: waitforTimeout,
        commandTimeout: commandTimeout,
        newCommandTimeout: 30 * 60000,
        locationServicesEnabled: true,
        locationServicesAuthorized: true,
        fullReset: true,
        specs: [
            './tests/specs/**/SSL_Pinning_ValidHash.spec.ts'
        ],
        exclude: [
            './tests/specs/**/SSL_Pinning_InvalidHash.spec.ts'
        ],
    },
];

exports.config = config;
