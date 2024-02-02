var fs = require('fs'),
    path = require('path'),
    xml2js = require('xml2js'),
    utils = require('../utils.js');


/**
 * Validates if the platform is Android
 * @param {object} context Cordova context
 * @returns {boolean} true if the platform is Android
 */
function isPlatformAndroid(context) {
    let platform = context.opts.plugin.platform;
    return platform === 'android';
}

function createDomainConfig(hostInfo){
    let domainConfig = {'domain-config' : []}
    let pinSet = {'pin-set' : []};
    let pins = [];
    let domain = {'domain':{ '$':{includeSubdomains:true} , _:hostInfo.host}};
    domainConfig['domain-config'].push(domain);
    for(let j in hostInfo.hashes){
        let hash = hostInfo.hashes[j];
        if(pins.indexOf(hash) === -1){
            pins.push(hash);
            let SHA = hash.replace('sha256/','');
            pinSet["pin-set"].push({'pin' : { '$':{digest:'SHA-256'} , _:SHA}});
        }
    }
    domainConfig['domain-config'].push(pinSet);    
    return domainConfig
}


function setupNetworkConfig(context) {
    console.log('Enabling network config option');

    let projectRoot = context.opts.projectRoot;
    let config = path.join(projectRoot, 'res', 'android', 'xml', 'network_security_config.xml');

    if (fs.existsSync(config)) {
        fs.readFile(config, 'utf8', function (err, data) {
            if (err) {
                throw new Error('Unable to find network_security_config.xml: ' + err);
            }

            let filePath = utils.getConfigFile(ctx, CordovaError)
            
            utils.loadConfigFile(filePath).then(function (configuration) {
                let builder = new xml2js.Builder();
                let parser = new xml2js.Parser({explicitArray: false});
                let domains = [];
                
                for(let i in configuration.hosts){
                    let domainConfig = createDomainConfig(configuration.hosts[i]);
                    domains.push(domainConfig)
                }
                
                parser.parseString(data, function(err,result){
                    let base = {'base-config' : result['network-security-config']['base-config']}; 
                    domains.unshift(base);
                    result['network-security-config'] = domains;
                    let finalXML = builder.buildObject(result);
                    fs.writeFile(config, finalXML, 'utf-8', function(err){
                        if(err){
                            throw new Error("Error writing file: "+err);
                        }
                    });
                })
            });
            });
        
    }
}


module.exports = function(context) {
    return new Promise(function(resolve) {

        if (isPlatformAndroid(context)) {
            setupNetworkConfig(context);
        }

        return resolve();
    });
};