const fs = require('fs');
const path = require('path');

module.exports = function(context) {
    const gradleConfigPath = path.join(context.opts.projectRoot, 'platforms/android/cdv-gradle-config.json');

    fs.readFile(gradleConfigPath, (err, data) => {
        if (err) throw err;

        const config = JSON.parse(data);
        config.IS_GRADLE_PLUGIN_GOOGLE_SERVICES_ENABLED = true;

        fs.writeFile(gradleConfigPath, JSON.stringify(config, null, 2), (err) => {
            if (err) {
                console.log('❌ --cdv-gradle-config.json error: '+err);
                throw err;
            } else {
                console.log('✅ --cdv-gradle-config.json has been updated to enable Google Services.');
            }
        });
    });
};