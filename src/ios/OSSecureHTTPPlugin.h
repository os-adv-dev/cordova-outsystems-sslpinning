//
//  OSSecureHTTPPlugin.h
//  OutSystems
//
//  Created by João Gonçalves on 16/12/16.
//
//

#import <Cordova/CDVPlugin.h>
#import <WebKit/WKWebView.h>

@interface OSSecureHTTPPlugin : CDVPlugin

- (void)webView:(WKWebView *)webView didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition disposition, NSURLCredential * _Nullable credential))completionHandler;

@end
