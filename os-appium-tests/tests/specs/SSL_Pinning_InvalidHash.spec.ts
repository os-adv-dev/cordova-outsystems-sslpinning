import 'jasmine';
import * as Context from '../helpers/Context';
import {DEFAULT_TIMEOUT, MAIN_PAGE, OTHER_SCREEN} from '../Constants';
import * as MainScreen from '../screenobjects/MainScreen';
import * as OtherScreen from '../screenobjects/OtherScreen';
import PermissionAlert from "../helpers/PermissionAlert";

describe('[TestSuite, Description("Hash the public keys of the certificates - invalid Hash")]', () => {

    const waitForScreen = (title: string) => {

        MainScreen.getTitle().waitForDisplayed(DEFAULT_TIMEOUT);
        const screenTitle: string = MainScreen.getTitle().getText();
        expect(screenTitle).toContain(title);
    };

beforeAll(() => {
        // Wait for webview to load
        Context.waitForNativeContextLoaded();
        Context.waitForWebViewContextLoaded();
        // Switch the context to WEBVIEW
        Context.switchToContext(Context.CONTEXT_REF.WEBVIEW);
        // Wait for Home Screen
        Context.switchToContext(Context.CONTEXT_REF.NATIVE);
        if (browser.isIOS && PermissionAlert.isShown(browser) === true) {
            PermissionAlert.allowPermission(true, browser);
        }
        // Switch the context to WEBVIEW
       Context.switchToContext(Context.CONTEXT_REF.WEBVIEW);

}
);
    beforeEach(() => {

            // Wait for webview to load
            Context.waitForWebViewContextLoaded();

            // Switch the context to WEBVIEW
            Context.switchToContext(Context.CONTEXT_REF.WEBVIEW);

            // Wait for Home Screen
            waitForScreen(MainScreen.SCREENTITLES.HOME_SCREEN);
        }
    );


    afterAll(() => {
        browser.closeApp();
    });

    it('[Test, Description("Should not display date/time when the user clicks "Get Date Time using Action",  Priority="P0", ID= "SS0006"]', () => {

        let button: any;
        let messageError: any;

        button = MainScreen.getDateTimeAction();
        button.waitForDisplayed();
        button.click();
        browser.pause(3000);
        messageError = MainScreen.getFeedbackMsgCard();

        expect(messageError.getText()).toContain(MAIN_PAGE.FAILURE_MESSAGE);

    });

    it('[Test, Description("Should not display date/time when the user clicks "Get Date Time using REST",  Priority="P0, ID= "SS0007"]', () => {

        let button: any;
        let  messageError: any;

        button = MainScreen.getDateTimeUsingRest();
        button.waitForDisplayed();
        button.click();
        browser.pause(3000);

        messageError = MainScreen.getFeedbackMsgCard();
        expect(messageError.getText()).toContain(MAIN_PAGE.FAILURE_MESSAGE);

    });

    it('[Test, Description("Should display date/time when the user clicks "Get Date Time using XHR",  Priority="P0", ID= "SS0008"]', () => {

        let button: any;
        let messageError: any;

        button = MainScreen.getDateTimeUsingXHR();
        button.waitForDisplayed();
        button.click();
        browser.pause(3000);

        messageError = MainScreen.getFeedbackMsgCard();
        expect(messageError.getText()).toContain(MAIN_PAGE.ERROR_MESSAGE);
        messageError.click();

    });

    it('[Test, Description("Should display the other screen inside the app and the link won´t open" Change to Other Screen",  Priority="P0", ID= "SS0009"]', () => {

        let button: any;
        let backButton:any;

        button = MainScreen.changeOtherScreen();
        button.waitForDisplayed();
        button.click();

        let bOneContent: any;
        bOneContent = OtherScreen.getContent();
        const result = bOneContent.getText();
        expect(result.length === 0);

        backButton = OtherScreen.backMainPage();
        backButton.waitForDisplayed();
        backButton.click();

    });

    it('[Test, Description("Should appear an error message with the content "There was an error processing your request",  Priority="P0", ID= "SS0010"]', () => {


        let buttonRedirect: any;
        let result: any;

        buttonRedirect = MainScreen.redirectURL();
        buttonRedirect.waitForDisplayed();
        buttonRedirect.click();
        browser.pause(3000);

        if (browser.isAndroid) {
            const header = $(OTHER_SCREEN.ANDROID_ERROR_INVALID_RESPONSE);
            header.waitForDisplayed();
            result = header.getText();
            expect(result).toContain(MAIN_PAGE.ANDROID_MESSAGE_ERROR);
            return;
        }
            const messageError = $(MAIN_PAGE.IOS_WEB_PAGE_ERROR);
            messageError.waitForDisplayed();
            result = messageError.getText();
            expect(result).toContain(MAIN_PAGE.IOS_MESSAGE_ERROR);
    });

});
