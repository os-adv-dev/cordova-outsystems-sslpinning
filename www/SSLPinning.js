var exec = cordova.require('cordova/exec');

module.exports = {
  
  checkCertificate: function checkCertificate(successCallback,errorCallback,url) {
    exec(successCallback, errorCallback, "SSLPinningPlugin", 'checkCertificate', [url]);
  },
};