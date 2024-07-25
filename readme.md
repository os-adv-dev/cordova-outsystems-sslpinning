# cordova-outsystems-sslpinning

Enables SSL Pinning for cordova applications.

*Note:* Only communications to the configured hosts will be _pinned_. If any request happens
to any domain that is not configured on the configuration file,
it will successfully be made without any pinning.

## How to use

In order to enable pinning the following steps should be followed:

1. [Generate SSL ping for the application's server](#generating-pkp-Hash).
2. [Place the configuration file on your project in the correct folder](#configuration-file).
3. Install this plugin.


### Plugin Validations

On installation, a _before_plugin_installation_ [hook](./hooks/check_requirements.js) is executed in order to validate:

a. If the configuration file exists on the expected folder
b. If the provided configuration file is in the valid format and if each host has at least 2 associated hashes.

If any of aforementioned rules fails, the plugin will fail the installation and a CordovaError is thrown.

### Generating PKP Hash

*Warning*
Using the wrong hashes will block all network communications on the application to the server.
Also, choosing which certificates to pin to is important for the same reason, server keys rotation might render the 
application unusable.


Hashes are expected to be the base64-encoded SHA-256 of a certificate’s Subject Public Key Info.

In order to manually generate the hash use the provided python script [get_pin_from_certificate.py](./get_pin_from_certificate.py)

#### Usage

``` shell
python get_pin_from_certificate.py ca.pem
python get_pin_from_certificate.py --type DER ca.der
```

Example output

``` shell
CERTIFICATE INFO
----------------
subject= /C=US/O=GeoTrust Inc./CN=GeoTrust Global CA
issuer= /C=US/O=GeoTrust Inc./CN=GeoTrust Global CA
SHA1 Fingerprint=DE:28:F4:A4:FF:E5:B9:2F:A3:C5:03:D1:A3:49:A7:F9:96:2A:82:12

TRUSTKIT CONFIGURATION
----------------------
kTSKPublicKeyHashes: @[@"h6801m+z8v3zbgkRHpq6L29Esgfzhj89C1SyUCOQmqU="] // You will also need to configure a backup pin
kTSKPublicKeyAlgorithms: @[kTSKAlgorithmRsa2048]
```

Copy the value under `kTSKPublicKeyHashes` to your configuration file.

#### Configuration File

Requirements:

- file extension *MUST* be .json
- Each host *MUST* have at least a 2 hashes set.
- Each hash *MUST* be prefixed with either `sha256/` or `sha1/` (* sha1 is Android ONLY and not fully tested*)

The configuration file is expected to have the following structure:

```json
{
    "hosts": [{
        "host": "google.com",
        "hashes": [
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
            ]
    },{
        "host": "outsystems.com",
        "hashes": [
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
            ]
    }]
}
```

And the file *must* be placed under:


To for use Fallback URL To JSON SSL Pinning you should pass these paramenters in extensibility global properties example below:
```json
{
    "preferences": {
        "global": [
            {
                "name": "com.outsystems.experts.ssl.remote.fallback_url",
                "value": "https://mocki.io/v1/ae3a37af-b194-4a5d-aea6-7d0004820772"
            },
            {
                "name": "com.outsystems.experts.ssl.remote.force_use_fallback_url",
                "value": "false"
            }
        ]
    }
}
```

`cordova_root_folder/www/${CFG_FILE_PATH}`

Where [`${CFG_FILE_PATH}`](https://github.com/OutSystems/cordova-outsystems-sslpinning/blob/master/plugin.xml#L10) is set with a default value of `pinning`.

## Dependencies

- Android:
  - OKHttp Framework
- iOS:
  - TrustKit Framework (Manually built into dynamic framework)


### iOS Quirks

Every HTTP/HTTPS request made on the context of the application will get pinned.

### Android Quirks

Given the limitations presented by the Android platform, only Webview's requests are pinned.
This means that any type of network communication outside Webview's scope *won't be pinned*.

The Keep-Alive response header is not used by the connection pool of the okttpclient. This is due to the fact that the keep-alive is shared accross all the connection pool and using the value provided in the response of a single connection would imply recreating the entire pool. To overcome this limitation, the user can customize this time using a preference

``` <preference name="sslpinning-connection-keep-alive" value="15" /> ```


### Building TrustKit

[Detailed information on how to build TrustKit as a dynamic library.](./documentation/build_trustkit.md)

Credits
=======

- TrustKit authors and collaborators, [https://github.com/datatheorem/TrustKit](https://github.com/datatheorem/TrustKit)
- OkHTTP authors and collaborators, [https://github.com/square/okhttp](https://github.com/square/okhttp)

LICENSE
=======

This plugin is released under MIT License. See [LICENSE](./LICENSE) for details.