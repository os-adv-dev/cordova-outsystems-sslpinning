//
//  OSSecureHTTPProtocol.m
//  OutSystems
//
//  Created by João Gonçalves on 16/12/16.
//
//

#import "OSSecureHTTPProtocol.h"
#import "OSLogger.h"

@interface OSSecureHTTPProtocol() <NSURLConnectionDelegate, NSURLConnectionDataDelegate>

@property (atomic, strong, readwrite) NSThread *clientThread;
@property (atomic, copy, readwrite) NSArray* modes;
@property (nonatomic, strong) NSURLConnection *connection;

@end

@implementation OSSecureHTTPProtocol

static NSString * kRecursiveRequestFlagProperty = @"com.outsystems.OSSecureHTTPProtocol";

+ (BOOL)canInitWithRequest:(NSURLRequest *) request {
    
    BOOL shouldAccept;
    NSURL *url;
    NSString* scheme;
    
    shouldAccept = (request != nil);
    
    if(shouldAccept) {
        url = [request URL];
    }
    
    if(shouldAccept) {
        shouldAccept = ([self propertyForKey:kRecursiveRequestFlagProperty inRequest:request] == nil);
    }
    
    if(shouldAccept) {
        scheme = [[url scheme] lowercaseString];
        shouldAccept = (scheme != nil);
    }
    
    if(shouldAccept) {
        shouldAccept = [scheme isEqualToString:@"http"] || [scheme isEqualToString:@"https"];
    }
    
    if(shouldAccept) {
        if([NSURLProtocol propertyForKey:@"downloadResourceAsyncKEY" inRequest:request]) {
            [[OSLogger sharedInstance] logDebug:@"OSSecureHTTPProtocol loading from downloadResourceAsyncKEY" withModule:@"OSSSLPinning"];
            [[OSLogger sharedInstance] logDebug:[NSString stringWithFormat:@"Should accept request URL: %@", [[request URL] absoluteString]] withModule:@"OSSSLPinning"];
        }
    }
    
    return shouldAccept;
}

+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request {
    return request;
}

- (void)startLoading {
    NSMutableURLRequest *recursiveRequest;
    NSMutableArray *calculatedModes;
    NSString *currentMode;
    
    // At this point we kick off the process of loading the URL via NSURLSession.
    // The thread that calls this method becomes the client thread.
    
    calculatedModes = [NSMutableArray array];
    [calculatedModes addObject:NSDefaultRunLoopMode];
    currentMode = [[NSRunLoop currentRunLoop] currentMode];
    if( (currentMode != nil) && ! [currentMode isEqual:NSDefaultRunLoopMode]) {
        [calculatedModes addObject:currentMode];
    }
    
    self.modes = calculatedModes;
    
    recursiveRequest = [[self request] mutableCopy];
    
    [[self class] setProperty:@YES forKey:kRecursiveRequestFlagProperty inRequest:recursiveRequest];
    
    self.clientThread = [NSThread currentThread];

    // Execute the request
    self.connection = [NSURLConnection connectionWithRequest:recursiveRequest delegate:self];
}

- (void)stopLoading {
    if(self.connection) {
        [self.connection cancel];
        self.connection = nil;
    } else {
        [[OSLogger sharedInstance] logDebug:@"Unable to stop loading" withModule:@"OSSSLPinning"];
    }
}


#pragma mark NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    [[self client] URLProtocol:self didReceiveResponse:response cacheStoragePolicy:NSURLCacheStorageNotAllowed];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    [[self client] URLProtocol:self didLoadData:data];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
    [[self client] URLProtocolDidFinishLoading:self];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
    [[self client] URLProtocol:self didFailWithError:error];
}

- (NSURLRequest *)connection:(NSURLConnection *)connection willSendRequest:(NSURLRequest *)request redirectResponse:(NSURLResponse *)response {
    if (response) {
        [self.client URLProtocol:self wasRedirectedToRequest:request redirectResponse:response];
        [connection cancel];
    }
    
    return request;
}

@end
