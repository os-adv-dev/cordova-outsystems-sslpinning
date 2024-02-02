// Borrowed from https://github.com/driverdan/node-XMLHttpRequest

var pluginId = "cordova-outsystems-sslpinning";
var Base64 = cordova.require(pluginId + ".Base64");
cordova.require(pluginId + ".url-polyfill");

var HTTP_STATUS_CODES = {
    100: 'Continue',
    101: 'Switching Protocols',
    102: 'Processing',
    200: 'OK',
    201: 'Created',
    202: 'Accepted',
    203: 'Non-Authoritative Information',
    204: 'No Content',
    205: 'Reset Content',
    206: 'Partial Content',
    207: 'Multi-Status',
    208: 'Already Reported',
    226: 'IM Used',
    300: 'Multiple Choices',
    301: 'Moved Permanently',
    302: 'Found',
    303: 'See Other',
    304: 'Not Modified',
    305: 'Use Proxy',
    307: 'Temporary Redirect',
    308: 'Permanent Redirect',
    400: 'Bad Request',
    401: 'Unauthorized',
    402: 'Payment Required',
    403: 'Forbidden',
    404: 'Not Found',
    405: 'Method Not Allowed',
    406: 'Not Acceptable',
    407: 'Proxy Authentication Required',
    408: 'Request Timeout',
    409: 'Conflict',
    410: 'Gone',
    411: 'Length Required',
    412: 'Precondition Failed',
    413: 'Payload Too Large',
    414: 'URI Too Long',
    415: 'Unsupported Media Type',
    416: 'Range Not Satisfiable',
    417: 'Expectation Failed',
    418: 'I\'m a teapot',
    421: 'Misdirected Request',
    422: 'Unprocessable Entity',
    423: 'Locked',
    424: 'Failed Dependency',
    425: 'Unordered Collection',
    426: 'Upgrade Required',
    428: 'Precondition Required',
    429: 'Too Many Requests',
    431: 'Request Header Fields Too Large',
    451: 'Unavailable For Legal Reasons',
    500: 'Internal Server Error',
    501: 'Not Implemented',
    502: 'Bad Gateway',
    503: 'Service Unavailable',
    504: 'Gateway Timeout',
    505: 'HTTP Version Not Supported',
    506: 'Variant Also Negotiates',
    507: 'Insufficient Storage',
    508: 'Loop Detected',
    509: 'Bandwidth Limit Exceeded',
    510: 'Not Extended',
    511: 'Network Authentication Required'
};

function NativeXMLHttpRequest() {
    "use strict";
    var callbackId;
    var instanceId = new Date().getTime();

    /**
     * Private variables
     */
    var self = this;

    var _response;

    var descriptor = {
        configurable: true,
        enumerable: true,
        get: function () {
            // TODO(jppg): return response in the appropriate type depending
            // on the value of responseType
            return _response;
        }
    };

    Object.defineProperty(this, "response", descriptor);

    // Request settings
    var settings = {};

    // Disable header blacklist.
    // Not part of XHR specs.
    var disableHeaderCheck = false;

    // Set some default headers
    var defaultHeaders = {
        "Accept": "*/*",
    };

    var headers = {};
    var headersCase = {};

    // These headers are not user setable.
    var forbiddenRequestHeaders = [
        "accept-charset",
        "accept-encoding",
        "access-control-request-headers",
        "access-control-request-method",
        "connection",
        "content-length",
        "content-transfer-encoding",
        "cookie",
        "cookie2",
        "date",
        "expect",
        "host",
        "keep-alive",
        "origin",
        "referer",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade",
        "via",
        "user-agent"
    ];

    // These request methods are not allowed
    var forbiddenRequestMethods = [
        "TRACE",
        "TRACK",
        "CONNECT"
    ];

    // Send flag
    var sendFlag = false;
    // Error flag, used when errors occur or abort is called
    var errorFlag = false;

    // Event listeners
    var listeners = {};

    /**
     * Constants
     */

    this.UNSENT = 0;
    this.OPENED = 1;
    this.HEADERS_RECEIVED = 2;
    this.LOADING = 3;
    this.DONE = 4;
    
    // Custom state added only to ignore (for now) blob URL because it doesn't supported on this implementation
    this.BLOB_ABORTED = 99;

    /**
     * Public vars
     */

    // Current state
    this.readyState = this.UNSENT;

    // default ready state change handler in case one is not set or is set late
    this.onreadystatechange = null;

    // Result & response
    this.responseText = "";
    this.responseXML = "";
    this.status = null;
    this.statusText = null;

    // Whether cross-site Access-Control requests should be made using
    // credentials such as cookies or authorization headers
    this.withCredentials = false;

    /**
     * Private methods
     */

    /**
     * Check if the specified header is allowed.
     *
     * @param string header Header to validate
     * @return boolean False if not allowed, otherwise true
     */
    var isAllowedHttpHeader = function (header) {
        return disableHeaderCheck || (header && forbiddenRequestHeaders.indexOf(header.toLowerCase()) === -1);
    };

    /**
     * Check if the specified method is allowed.
     *
     * @param string method Request method to validate
     * @return boolean False if not allowed, otherwise true
     */
    var isAllowedHttpMethod = function (method) {
        return (method && forbiddenRequestMethods.indexOf(method) === -1);
    };

    /**
    * Strip URL to make sure it doesn't contain browser APIs
    *
    * Example: blob:https://myServer.outsystemsenterprise.com/c08b5e8f-04ed-4dfd-a552-6947e3df4d45
    * blob is not a protocol. A Blob object represents a file-like object of immutable, raw data
    * Since cookies are shared between the native part and the webview, by allowing the https
    * request to go through the native part will properly return the content.
    *
    * @see https://developer.mozilla.org/en-US/docs/Web/API/Blob
    * @param string url The URL to parse
    * @return boolean The fixed url or the original if no changes are required
    */
    var stripBrowserAPIs = function (url) {
        // We cannot just directly replace the blob: since it's possible to have it in the url (https://...notrelatedblob:443/
        return url.indexOf("blob:") === 0 ? url.substring("blob:".length) : url;
    };

    /**
     * Public methods
     */

    /**
     * Open the connection. Currently supports local server requests.
     *
     * @param string method Connection method (eg GET, POST)
     * @param string url URL for the connection.
     * @param boolean async Asynchronous connection. Default is true.
     * @param string user Username for basic authentication (optional)
     * @param string password Password for basic authentication (optional)
     */
    this.open = function (method, url, async, user, password) {
        // jppg: don't really think abort should be called here at all!
        // this.abort();
        errorFlag = false;

        // Check for valid request method
        if (!isAllowedHttpMethod(method)) {
            OutSystemsNative.Logger.logError("SecurityError: Request method not allowed", "OSSSLPinning");
            throw new Error("SecurityError: Request method not allowed");
        }

        settings = {
            "method": method,
            "url": url.toString(),
            "async": (typeof async !== "boolean" ? true : async),
            "user": user || null,
            "password": password || null
        };

        if (url.indexOf("blob:") === 0) {
            setState(this.BLOB_ABORTED);
        } else {
            setState(this.OPENED);
        }

    };

    /**
     * Disables or enables isAllowedHttpHeader() check the request. Enabled by default.
     * This does not conform to the W3C spec.
     *
     * @param boolean state Enable or disable header checking.
     */
    this.setDisableHeaderCheck = function (state) {
        disableHeaderCheck = state;
    };

    /**
     * Sets a header for the request or appends the value if one is already set.
     *
     * @param string header Header name
     * @param string value Header value
     */
    this.setRequestHeader = function (header, value) {
        if (this.readyState !== this.OPENED) {
            OutSystemsNative.Logger.logError("INVALID_STATE_ERR: setRequestHeader can only be called when state is OPEN", "OSSSLPinning");
            throw new Error("INVALID_STATE_ERR: setRequestHeader can only be called when state is OPEN");
        }
        if (!isAllowedHttpHeader(header)) {
            OutSystemsNative.Logger.logError("Refused to set unsafe header \"" + header + "\"", "OSSSLPinning");
            console.warn("Refused to set unsafe header \"" + header + "\"");
            return;
        }
        if (sendFlag) {
            OutSystemsNative.Logger.logError("INVALID_STATE_ERR: send flag is true", "OSSSLPinning");
            throw new Error("INVALID_STATE_ERR: send flag is true");
        }
        header = headersCase[header.toLowerCase()] || header;
        headersCase[header.toLowerCase()] = header;
        headers[header] = headers[header] ? headers[header] + ', ' + value : value;
    };

    /**
     * Gets a header from the server response.
     *
     * @param string header Name of header to get.
     * @return string Text of the header or null if it doesn't exist.
     */
    this.getResponseHeader = function (header) {
        if (typeof header === "string" &&
            this.readyState > this.OPENED &&
            _response &&
            _response.headers &&
            _response.headers[header.toLowerCase()] &&
            !errorFlag
        ) {
            return _response.headers[header.toLowerCase()];
        }

        return null;
    };

    /**
     * Gets all the response headers.
     *
     * @return string A string with all response headers separated by CR+LF
     */
    this.getAllResponseHeaders = function () {
        if (this.readyState < this.HEADERS_RECEIVED) {
            return null;
        }
        if (errorFlag) {
            return "";
        }
        var result = _response.headers || "";
        // exclude Set-Cookie headers
        result = result.replace(/set-cookie2?:[^\n]*(\n|$)/ig, "");
        
        // strip the last \r\n
        return result.substr(0, result.length - 1);
    };

    /**
     * Gets a request header
     *
     * @param string name Name of header to get
     * @return string Returns the request header or empty string if not set
     */
    this.getRequestHeader = function (name) {
        if (typeof name === "string" && headersCase[name.toLowerCase()]) {
            return headers[headersCase[name.toLowerCase()]];
        }

        return "";
    };

    /**
     * Sends the request to the server.
     *
     * @param string data Optional data to send as request body.
     */
    this.send = function (data) {
        if (this.readyState === this.BLOB_ABORTED) {
            successAbort();
            return;
        }

        if (this.readyState !== this.OPENED) {
            OutSystemsNative.Logger.logError("INVALID_STATE_ERR: Failed to execute 'send' on 'XMLHttpRequest': The object's state must be OPENED.", "OSSSLPinning");
            throw new Error("INVALID_STATE_ERR: Failed to execute 'send' on 'XMLHttpRequest': The object's state must be OPENED.");
        }

        if (sendFlag) {
            OutSystemsNative.Logger.logError("INVALID_STATE_ERR: send has already been called", "OSSSLPinning");
            throw new Error("INVALID_STATE_ERR: send has already been called");
        }

        var ssl = false,
            local = false;

        // https://dev.w3.org/cvsweb/~checkout~/2006/webapi/XMLHttpRequest/Overview.html?content-type=text/html;%20charset=utf-8#xmlhttprequest-base-url
        // https://tools.ietf.org/html/rfc3986#section-4.2
        var url;
        var baseURL = new URL(document.baseURI);
        var host = window.location.host;
        try {
            url = new URL(settings.url);
            // (Version/4.0 Chrome/30.0.0.0) doesn't throw exception when creating an URL with
            // an undefined param
            if(url.href === undefined) {
                OutSystemsNative.Logger.logError("Failed to execute 'send': Invalid URL", "OSSSLPinning");
                throw new Error("Failed to execute 'send': Invalid URL");
            }
        } catch (error) {
            if (settings.url) {
                //Supported only for chrome 41
                //if (settings.url.startsWith("/"){
                if (settings.url.indexOf("/") === 0) {
                    url = new URL(window.location.protocol + "//" + window.location.host + settings.url);
                } else {
                    //url = new URL(baseURL.href + settings.url);
                    var pathName = window.location.pathname;
                    if(pathName.indexOf("/") === 0) {
                      pathName = pathName.substring(1, pathName.length);
                    }
                    var res = pathName.split("/");
                    if(res.length > 0) {
                      pathName = res[0];
                    }
                    url = new URL(window.location.origin + "/" + pathName + "/" + settings.url);
                }
            }
        }

        // Determine the server
        switch (url.protocol) {
            case "https:":
                ssl = true;
                // SSL & non-SSL both need host, no break here.
            case "http:":
                host = url.hostname;
                break;

            case "file:":
                local = true;
                break;

            case undefined:
            case null:
            case "":
                host = url.hostname;
                break;

            default:
                OutSystemsNative.Logger.logError("Failed to execute 'send': URL protocol not supported.", "OSSSLPinning");
                throw new Error("Failed to execute 'send': URL protocol not supported.");
        }

        // Load files off the local filesystem (file://)
        if (local) {
            // TODO(jppg): Load using cordova resource loader
            OutSystemsNative.Logger.logError("Native XHR file:// scheme not supported", "OSSSLPinning");
            throw new Error("Native XHR file:// scheme not supported");
        }

        // Default to port 80. If accessing localhost on another port be sure
        // to use http://localhost:port/path
        var port = url.port || (ssl ? 443 : 80);
        // Add query string if one is used
        var uri = url.pathname + (url.search ? url.search : "");

        // Set the defaults if they haven't been set
        for (var name in defaultHeaders) {
            if (!headersCase[name.toLowerCase()]) {
                headers[name] = defaultHeaders[name];
            }
        }

        // Set the Host header or the server may reject the request
        headers.Host = host;
        if (!((ssl && port === 443) || port === 80)) {
            headers.Host += ":" + url.port;
        }

        // Set Basic Auth if necessary
        if (settings.user) {
            if (typeof settings.password === "undefined") {
                settings.password = "";
            }

            var authBase64 = Base64.encode(settings.user + ":" + settings.password);
            headers.Authorization = "Basic " + authBase64;
        }

        // Set content length header
        if (settings.method === "GET" || settings.method === "HEAD") {
            data = null;
        } else if (data) {
            headers["Content-Length"] = data.length;

            if (!headersCase["content-type"]) {
                headers["Content-Type"] = "text/plain;charset=UTF-8";
            }
        } else if (settings.method === "POST") {
            // For a post with no data set Content-Length: 0.
            // This is required by buggy servers that don't meet the specs.
            headers["Content-Length"] = 0;
        }

        // Setting UserAgent
        headers["User-Agent"] = window.navigator.userAgent;
        
        var options = {
            host: host,
            port: port,
            path: uri,
            method: settings.method,
            headers: headers,
            agent: false,
            withCredentials: self.withCredentials
        };

        // Reset error flag
        errorFlag = false;

        // Request is being sent, set send flag
        sendFlag = true;

        // As per spec, this is called here for historical reasons.
        self.dispatchEvent("readystatechange");

        // Handler for the response
        var responseHandler = function responseHandler(resp) {
            if (resp.event === "load") {
                _response = resp.arg;
                self.callbackId = resp.callbackid;
                setState(self.HEADERS_RECEIVED);
                self.status = _response.statusCode;
                self.statusText = HTTP_STATUS_CODES[_response.statusCode];
                self.responseText = _response.data;

                if (sendFlag) {
                    setState(self.LOADING);
                }

                if (sendFlag) {
                    // Discard the end event if the connection has been aborted
                    setState(self.DONE);
                    sendFlag = false;
                }
            }
        };

        // Error handler for the request
        var errorHandler = function errorHandler(error) {
            if (error.event === "timeout") {
                self.handleTimeout();
            } else if (error.event === "error") {
                self.handleError(error.arg);
            } else if (error.event === "abort") {
                successAbort();
            }
        };
        if (self.timeout === undefined) {
            self.timeout = 0;
        }
        cordova.exec(responseHandler,
            errorHandler,
            "NativeXMLHttpRequestPlugin",
            "send", [
                url.href,
                settings.method.toLowerCase(),
                options.headers,
                data,
                true,
                self.timeout,
                instanceId
            ]
        );

        self.dispatchEvent("loadstart");

    };

    /**
     * Called when an error is encountered to deal with it.
     */
    this.handleError = function (error) {
        this.status = 0;
        this.statusText = error;

        this.responseText = error.stack;
        errorFlag = true;
        setState(this.DONE);
        this.dispatchEvent('error');
        this.dispatchEvent("loadend");
    };

    this.handleTimeout = function () {
        this.status = 0;
        this.statusText = "";
        this.responseText = "";
        this.responseXML = null;
        // this.responseType = "error"; // TODO(jppg): not sure if this is spec compliant.
        errorFlag = true;
        setState(this.DONE);
        this.dispatchEvent("timeout");
        this.dispatchEvent("loadend");
    };

    var successAbort = function () {
        headers = defaultHeaders;
        self.status = 0;
        self.responseText = "";
        self.responseXML = "";

        errorFlag = true;

        if (self.readyState !== self.UNSENT &&
            (self.readyState !== self.OPENED || sendFlag) &&
            self.readyState !== self.DONE) {
            sendFlag = false;
            setState(self.DONE);
        }
        self.readyState = self.UNSENT;
        self.dispatchEvent('abort');
    };

    /**
     * Aborts a request.
     */
    this.abort = function () {
        cordova.exec(null, null, "NativeXMLHttpRequestPlugin", "abort", [instanceId]);
    };

    /**
     * Adds an event listener. Preferred method of binding to events.
     */
    this.addEventListener = function (event, callback) {
        if (!(event in listeners)) {
            listeners[event] = [];
        }
        // Currently allows duplicate callbacks. Should it?
        listeners[event].push(callback);
    };

    /**
     * Remove an event callback that has already been bound.
     * Only works on the matching funciton, cannot be a copy.
     */
    this.removeEventListener = function (event, callback) {
        if (event in listeners) {
            // Filter will return a new array with the callback removed
            listeners[event] = listeners[event].filter(function (ev) {
                return ev !== callback;
            });
        }
    };

    /**
     * Dispatch any events, including both "on" methods and events attached using addEventListener.
     */
    this.dispatchEvent = function (type) {
        var pseudoEvent = {
            'type': type,
            'target': self,
            'currentTarget': self,
            'eventPhase': 2,
            'bubbles': false,
            'cancelable': false,
            'timeStamp': new Date().getTime(),
            'stopPropagation': function () {}, // There is no flow
            'preventDefault': function () {}, // There is no default action
            'initEvent': function () {} // Original event object should be initialized
        };

        if (typeof self["on" + type] === "function") {
            self["on" + type](pseudoEvent);
        }
        if (type in listeners) {
            for (var i = 0, len = listeners[type].length; i < len; i++) {
                listeners[type][i].call(self);
            }
        }
    };

    /**
     * Changes readyState and calls onreadystatechange.
     *
     * @param int state New state
     */
    var setState = function (state) {
        if (state == self.LOADING || self.readyState !== state) {
            self.readyState = state;

            if (settings.async || self.readyState < self.OPENED || self.readyState === self.DONE) {
                self.dispatchEvent("readystatechange");
            }

            if (self.readyState === self.DONE && !errorFlag) {
                self.dispatchEvent("load");
                self.dispatchEvent("loadend");
            }
        }
    };
}

var XMLHttpRequestProxy = cordova.require(pluginId + ".XMLHttpRequestProxy");
var originalXMLHttpRequest = cordova.require('cordova/modulemapper').getOriginalSymbol(window, 'XMLHttpRequest');

var ua = navigator.userAgent;
if( ua.indexOf("Android") >= 0 )
{
    var androidversion = parseInt(ua.slice(ua.indexOf("Android")+8));
    if (androidversion < 7)
    {
        exports.XMLHttpRequest = function () {
        return new XMLHttpRequestProxy(NativeXMLHttpRequest);
        };
    }
}