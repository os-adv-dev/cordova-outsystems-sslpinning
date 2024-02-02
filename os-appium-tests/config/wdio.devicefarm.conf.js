const { config } = require('./wdio.shared.conf');

// ============
// Specs
// ============
config.specs = [
    './dist/specs/**/*.spec.js',
];

config.capabilities = [
    {
        // Read the reset strategies very well, they differ per platform, see
        // http://appium.io/docs/en/writing-running-appium/other/reset-strategies/
        noReset: true,
        newCommandTimeout: 240,
    },
];

exports.config = config;
