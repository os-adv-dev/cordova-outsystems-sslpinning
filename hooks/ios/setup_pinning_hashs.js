/**
 * @description Generates an Object containing the necessary structure
 * for TrustKit configuration.
 */
function genPlistConfiguration(configuration) {
    var tmpPlistConf = {
        "TSKConfiguration": {
            "TSKSwizzleNetworkDelegates": true,
            "TSKPinnedDomains": {}
        }
    };
    for (var i = 0; i < configuration.hosts.length; i++) {
        var hostInfo = configuration.hosts[i];
        var cleanedHashes = [];
        hostInfo.hashes.forEach(function (hash) {
            if (hash.startsWith("sha256/") || hash.startsWith("sha1/")) {
                var cleanHash = hash.substr(hash.indexOf("/") + 1);
                cleanedHashes.push(cleanHash);
            } else {
                cleanedHashes.push(hash);
            }
        });
        var domainObj = {
            "TSKDisableDefaultReportUri":true,
            "TSKEnforcePinning": true,
            "TSKIncludeSubdomains": false,
            "TSKPublicKeyHashes": cleanedHashes
        };
        // Object.defineProperty(tmpPlistConf[0]["TSKPinnedDomains"], hostInfo.host, {});
        tmpPlistConf["TSKConfiguration"]["TSKPinnedDomains"][hostInfo.host] = domainObj;
    }
    return tmpPlistConf;
}

module.exports = function (ctx) {
    var fs = require("fs");
    var path = require("path");
    var xcode = require("xcode");
    var plist = require("plist");
    var Q = require("q");
    var CordovaError = ctx.requireCordovaModule('cordova-common').CordovaError;
    var utils = require("../utils.js");
    
    let filePath = utils.getConfigFile(ctx, CordovaError)
    let projectRoot = ctx.opts.projectRoot;
    utils.loadConfigFile(filePath).then(function (config) {
        utils.loadInfoPlist(ctx, projectRoot).then(function (plistInfo) {
            const newConfigPlist = genPlistConfiguration(config);
            var plistObj = plistInfo.plist;

            Object.keys(newConfigPlist).forEach(function (k) {
                if (!newConfigPlist.propertyIsEnumerable(k)) return;
                plistObj[k] = newConfigPlist[k];
            });

            //Plist lib has a bug that consists in building invalid plist files when some fields are null
            //Our solution consists in iterate all keys an if it is null, replace with an empty string
            Object.keys(plistObj).forEach(function(key) {
                if(plistObj[key] === undefined || plistObj[key] === null) {
                    plistObj[key] = "";
                }
            });

            fs.writeFileSync(plistInfo.path, plist.build(plistObj), {"encoding": 'utf8', "flag": "w"});
        });
    });    
    
};