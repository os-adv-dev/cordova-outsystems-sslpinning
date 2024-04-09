const fs = require('fs');
const path = require('path');

module.exports = function(context) {
    const gradleConfigPath = path.join(context.opts.projectRoot, 'platforms/android/cdv-gradle-config.json');

    fs.readFile(gradleConfigPath, 'utf8', (err, data) => {
        if (err) {
            console.error('❌ Erro ao ler cdv-gradle-config.json:', err);
            throw err;
        }

        let config = JSON.parse(data);

        if (config.IS_GRADLE_PLUGIN_GOOGLE_SERVICES_ENABLED === false) {
            config.IS_GRADLE_PLUGIN_GOOGLE_SERVICES_ENABLED = true;

            fs.writeFile(gradleConfigPath, JSON.stringify(config, null, 2), (err) => {
                if (err) {
                    console.error('❌ Erro to updat the cdv-gradle-config.json:', err);
                    throw err;
                } else {
                    console.log('✅ cdv-gradle-config.json updated ok to enabled the Google Services.');
                }
            });
        } else {
            console.log('✅ Google Services is already set TRUE in cdv-gradle-config.json.');
        }
    });
};
