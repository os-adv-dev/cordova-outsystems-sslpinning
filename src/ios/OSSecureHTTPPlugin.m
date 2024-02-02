//
//  OSSecureHTTPPlugin.m
//  OutSystems
//
//  Created by João Gonçalves on 16/12/16.
//
//

#import "OSSecureHTTPPlugin.h"
#import "OSSecureHTTPProtocol.h"
#import <TrustKit/TrustKit.h>

@implementation OSSecureHTTPPlugin

-(void) pluginInitialize {
    [NSURLProtocol registerClass: [OSSecureHTTPProtocol class]];
}
-(void)checkCertificate:(CDVInvokedUrlCommand*) command{
    if([command.arguments count]<0){
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No Arguments sent!"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"GET"];
    
    NSString* url = [command.arguments objectAtIndex:0];
        
    [request setURL:[NSURL URLWithString:url]];
    
    [NSURLConnection sendAsynchronousRequest:request queue:[[NSOperationQueue alloc] init] completionHandler:^(NSURLResponse * _Nullable response, NSData * _Nullable data, NSError * _Nullable connectionError) {
        if (connectionError == nil) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }else{
            NSInteger code = [connectionError code];
            NSDictionary * errorDic;
            if (code == -1012 && data == nil) {

                errorDic = @{@"Code":@"1",@"Message":@"SSLPinning found a issue with the configured certificate for the url!"};
            }else {
                errorDic = @{@"Code":@"2",@"Message":@"SSLPinning found some problem with the request!"};
            }
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorDic];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

- (void)webView:(WKWebView *)webView didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition disposition, NSURLCredential * _Nullable credential))completionHandler{
    
    TSKPinningValidator *pinningValidator = [[TrustKit sharedInstance] pinningValidator];
    // Pass the authentication challenge to the TrustKit validator; if the validation fails, the connection will be blocked
    if (![pinningValidator handleChallenge:challenge completionHandler:completionHandler])
    {
        // TrustKit did not handle this challenge: perhaps it was not for server trust
        // or the domain was not pinned. Fall back to the default behavior
        completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, nil);
    }
}

@end
