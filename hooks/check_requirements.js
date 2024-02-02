var Q = require('q'),
 fs = require('fs'),
 path = require('path'),
 utils = require('./utils.js'),
 deferral = Q.defer();

module.exports = function (ctx) {

    let CordovaError = ctx.requireCordovaModule('cordova-common').CordovaError; 

    let filePath = utils.getConfigFile(ctx, CordovaError)

    utils.loadConfigFile(filePath).then(function (configuration) {
        if (!utils.isValidConfig(configuration)) {
            deferral.reject(new CordovaError("Invalid HPKP configuration provided"));
        } else {
            deferral.resolve();
        }
    });
    
   
    return deferral.promise;
};

