/**
 * @description Validates a given configuration object.
 * 
 * Example of valid configuration:
 * {
 *  "hosts": [{
 *      "host": "expertsp10-dev.outsystemsenterprise.com",
 *      "hashes": [
 *          "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
 *          "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
 *      ]
 *  }]
 * }*/

var Q = require('q'),
    fs = require('fs'),
    path = require('path'),
    CordovaError = require('cordova-common').CordovaError,
    plist = require("plist"),
    deferral = Q.defer();

// Name of the preference on plugin.xml
const PREF_NAME = "CFG_FILE_PATH";

function isValidConfig(config) {
    if (config === undefined) {
        return false;
    }

    if (((config["hosts"] !== undefined) && (config["hosts"] instanceof Array))) {
        if (config.hosts.length === 0) {
            return false;
        }
    } else {
        return false;
    }

    for (var index in config.hosts) {
        var hostObj = config.hosts[index];
        if (hostObj["host"] !== undefined && typeof hostObj["host"] === "string") {
            if (hostObj.host.length === 0) {
                return false;
            }
        } else {
            return false;
        }
        
        if (hostObj["hashes"] !== undefined && hostObj["hashes"] instanceof Array) {
            if (hostObj.hashes.length < 2) {
                return false;
            }
            for (var hashObj in hostObj.hashes)  {
                if (typeof hashObj !== 'string') {
                    return false;
                }

                if (hashObj.length === 0) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }
    return true;
}

/**
 * Returns a promise of a configuration object
 */
function loadConfigFile(filePath) {
    let configuration = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    deferral.resolve(configuration);

    return deferral.promise;
}

/**
 * @returns Promise with PlistInfo object containing path to the plist file and the plist object
 */
function loadInfoPlist(ctx, projectRoot) {

    // Path to ios platform folder
    var platformsPath = path.join(projectRoot, "platforms", "ios");

    var xcodeproj_dir = fs.readdirSync(platformsPath).filter(function (e) {
        return e.match(/\.xcodeproj$/i);
    })[0];

    // Ensure the hook is running for ios 
    if (!xcodeproj_dir) throw new CordovaError('The provided path "' + platformsPath + '" is not a Cordova iOS project.');

    var xcodeproj = path.join(platformsPath, xcodeproj_dir);
    var projName = xcodeproj.substring(xcodeproj.lastIndexOf(path.sep) + 1, xcodeproj.indexOf('.xcodeproj'));
    var cordovaproj = path.join(platformsPath, projName);

    var plistFile = path.join(cordovaproj, projName + '-Info.plist');
    var infoPlist = plist.parse(fs.readFileSync(plistFile, 'utf8'));

    // Return a fulfilled promise with infoPlist
    return Q({"path":plistFile, "plist":infoPlist});
}

function getPlatformPath(ctx){
    let projectRoot = ctx.opts.projectRoot;
    let platform = ctx.opts.platforms[0];
    let configFolder = "pinning/";

    let platformPath;

    if(platform == "android"){
        platformPath = path.join(projectRoot, `platforms/${platform}/www`, configFolder);
    } else {
        let appName = getAppName(ctx)
        platformPath = path.join(projectRoot, `platforms/${platform}/${appName}/Resources/www`, configFolder);   
    }
        
    if(!fs.existsSync(platformPath)){
        platformPath = path.join(projectRoot, "www", configFolder);
        if(!fs.existsSync(platformPath)){
            throw new CordovaError('No HPKP configuration found.');
        } 
    }
    
    return platformPath

}

function getConfigFile(ctx, CordovaError){
    let cfgFolderPath = getPlatformPath(ctx)
    let cfgFileName = fs.readdirSync(cfgFolderPath).filter(function (e) {
        return e.match(/\.json$/i);
    });
    
    if(cfgFileName == 0){
        throw new CordovaError('No HPKP configuration found.');
    }
    let destFilePath = cfgFolderPath + `${cfgFileName[0]}`

    return destFilePath;
}

function getAppName(context) {
    let ConfigParser = context.requireCordovaModule("cordova-lib").configparser;
    let config = new ConfigParser("config.xml");
    return config.name();
}

module.exports.PREF_NAME = PREF_NAME;
module.exports.isValidConfig = isValidConfig;
module.exports.loadConfigFile = loadConfigFile;
module.exports.loadInfoPlist = loadInfoPlist;
module.exports.getConfigFile = getConfigFile;
module.exports.getAppName = getAppName;