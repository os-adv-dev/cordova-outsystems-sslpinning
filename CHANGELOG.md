# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [5.0.2]
- Feat: Update hooks to consider folders for both O11 and ODC solutions [RMET-2574](https://outsystemsrd.atlassian.net/browse/RMET-2696)

## [5.0.1]
- Fix: Remove reference to jitpack repo [RMET-2944](https://outsystemsrd.atlassian.net/browse/RMET-2944)

## [5.0.0]
- Fix Android Hook [RMET-2574](https://outsystemsrd.atlassian.net/browse/RMET-2574)

## [4.2.9]
- New plugin release to include metadata tag setting compatibility with MABS versions

## [4.2.8]
### Fixes
- Revert changes that matched "id" in plugin.xml with "name" in package.json (broke MABS 6.3 build) [RMET-532](https://outsystemsrd.atlassian.net/browse/RMET-532)

## [4.2.6]
### Fixes
- Implements a cookieHandler to setCookie on responses [RMET-313](https://outsystemsrd.atlassian.net/browse/RMET-313)

## [4.2.2]
## Additions
- Adds a pipeline for publishing the plugin to the private npm registry [RMET-235](https://outsystemsrd.atlassian.net/browse/RMET-235)

## [4.2.1]
## Fixes
- Only post message with Certificate pinner when running on Android versions prior to Android 7

## [4.2.0]
## Feature
- Added conversion between the JSON and the network_security_config.xml .
- The pinning for Android 7 and above is now done natively with that. The versions below still work as before.
- Reorganized js imports for the hooks of the plugin.


## [4.1.1]
### Fixes
- Generates HTTP POST request body setting its type based on content-type header [RNMT-4281](https://outsystemsrd.atlassian.net/browse/RNMT-4281)

## [4.1.0]
### Additions
- Added capability to verify if a configured hash to a given url is valid or not

## [4.0.1]
### Fixes
- Upon redirection, connection must be killed to prevent duplicate HTTP requests for requests whose response has been received [RNMT-3814](https://outsystemsrd.atlassian.net/browse/RNMT-3814)

## [4.0.0]
### Changes
- **BREAKING:** Refactor HTTP clients initialization [RNMT-3581](https://outsystemsrd.atlassian.net/browse/RNMT-3581)
- **BREAKING:** Propagates the new SSLSecurity configurations using the PluginManager postMessage [RNMT-3580](https://outsystemsrd.atlassian.net/browse/RNMT-3580)

## [3.0.1] - 2019-09-18
### Fixes
- Proxy method assignments are no longer redefining the XMLHttpRequestProxy definition [RNMT-3304](https://outsystemsrd.atlassian.net/browse/RNMT-3304)

## [3.0.0] - 2019-09-13
### Additions
- Passes WKWebview authentication challenges to the TrustKit validator [RNMT-3090](https://outsystemsrd.atlassian.net/browse/RNMT-3090)
- Adds an Authentication Challenge handler for WKWebview compatibility [RNMT-3090](https://outsystemsrd.atlassian.net/browse/RNMT-3090)

## [2.2.0] - 2019-07-12
### Additions
- Adds support for HTTP page redirect on iOS [RNMT-3074](https://outsystemsrd.atlassian.net/browse/RNMT-3074)

## [2.1.0] - 2019-04-12
### Changes
- Raised okhttp version to 3.12.1 [RNMT-2404](https://outsystemsrd.atlassian.net/browse/RNMT-2404)
- Update usage of compile to implementation/api in gradle dependencies [RNMT-2655](https://outsystemsrd.atlassian.net/browse/RNMT-2655)

### Removals
- Unnecessary explicit references to okio, okhttp-urlconnection and PersistentCookieJar libraries on Android [RNMT-2404](https://outsystemsrd.atlassian.net/browse/RNMT-2404)

[Unreleased]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/4.1.1...HEAD
[4.1.1]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/4.1.0...4.1.1
[4.1.0]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/4.0.1...4.1.0
[4.0.1]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/4.0.0...4.0.1
[4.0.0]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/3.0.1...4.0.0
[3.0.1]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/3.0.0...3.0.1
[3.0.0]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/2.2.0...3.0.0
[2.2.0]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/2.1.0...2.2.0
[2.1.0]: https://github.com/OutSystems/cordova-outsystems-sslpinning/compare/2.0.0...2.1.0
