function proxyMethod(_this, methodName) {
    return function () {
        return _this._proxy[methodName].apply(_this._proxy, arguments);
    };
}

function proxyProperty(_this, propertyName, writable) {
    var descriptor = {
        configurable: true,
        get: function () {
            var res = _this._proxy[propertyName];
            return res;
        }
    };

    if (writable) {
        descriptor.set = function (val) {
            _this._proxy[propertyName] = val;
        };
    }

    Object.defineProperty(_this, propertyName, descriptor);
}

function proxyProgressEventHandler(_this, eventName, handler) {
    return function (pe) {
        var new_pe = new ProgressEvent(eventName);
        new_pe.target = _this;
        handler.call(_this, new_pe);
    };
}

function proxyEventProperty(_this, eventName) {
    var eventPropertyName = "on" + eventName;
    var descriptor = {
        configurable: true,
        get: function () {
            return _this._proxy[eventPropertyName];
        },
        set: function (handler) {
            _this._proxy[eventPropertyName] = proxyProgressEventHandler(_this, eventName, handler);
        }
    };

    Object.defineProperty(_this, eventPropertyName, descriptor);
}

function XMLHttpRequestProxy(originalXHR) {

    var self = this;
    this._proxy = new originalXHR();

    /* Proxy events */
    ['loadstart', 'progress', 'abort', 'error', 'load', 'timeout', 'loadend'].forEach(function (elem) {
        proxyEventProperty(self, elem);
    });
    /* Proxy read/write properties */
    ['onreadystatechange', 'timeout', 'withCredentials'].forEach(function (elem) {
        proxyProperty(self, elem, true);
    });
    /* Proxy read-only properties */
    ['upload', 'readyState', 'status', 'statusText', 'responseText', 'responseXML'].forEach(function (elem) {
        proxyProperty(self, elem);
    });

    /* Proxy methods */
    ['open', 'setRequestHeader', 'send', 'abort', 'getResponseHeader', 'getAllResponseHeaders', 'overrideMimeType', 'addEventListener', 'removeEventListener'].forEach(function (elem) {
        self[elem] = proxyMethod(self, elem);
    });

}

XMLHttpRequestProxy.prototype.addEventListener = function (eventName, handler) {
    this._proxy.addEventListener(eventName, proxyProgressEventHandler(this, eventName.toLowerCase(), handler));
};

module.exports = XMLHttpRequestProxy;
