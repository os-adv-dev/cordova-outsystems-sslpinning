import 'jasmine';
import * as Context from '../helpers/Context';
import {DEFAULT_TIMEOUT, MAIN_PAGE, OTHER_SCREEN} from '../Constants';
import * as MainScreen from '../screenobjects/MainScreen';
import * as OtherScreen from '../screenobjects/OtherScreen';
import PermissionAlert from "../helpers/PermissionAlert";


describe('[TestSuite, Description("Hash the public keys of the certificates - valid Hash")]', () => {

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

    });

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

    it('[Test, Description("Should display date/time when the user clicks "Get Date Time using Action",  Priority="P0", ID= "SS0001"]', () => {

        let button: any;
        let defaultTimeZoneStringFormat: any;
        let atualTimeZoneStringFormat: any;
        defaultTimeZoneStringFormat = MainScreen.getTimeZone().getText();
        const defaultTimeZone = Date.parse(defaultTimeZoneStringFormat);

        browser.pause(3000);

        button = MainScreen.getDateTimeAction();
        button.waitForDisplayed();
        button.click();

        browser.pause(3000);

        atualTimeZoneStringFormat = MainScreen.getTimeZone().getText();
        const atualTimeZone = Date.parse(atualTimeZoneStringFormat);
        expect(atualTimeZone - defaultTimeZone > 0.0001);

    });

    it('[Test, Description("Should display date/time when the user clicks "Get Date Time using REST",  Priority="P0", ID= "SS0002"]', () => {

        let button: any;
        let defaultTimeZoneStringFormat: any;
        let atualTimeZoneStringFormat: any;
        defaultTimeZoneStringFormat = MainScreen.getTimeZone().getText();
        const defaultTime = Date.parse(defaultTimeZoneStringFormat);

        browser.pause(3000);

        button = MainScreen.getDateTimeUsingRest();
        button.waitForDisplayed();
        button.click();

        browser.pause(3000);

        atualTimeZoneStringFormat = MainScreen.getTimeZone().getText();
        const atualTimeZone = Date.parse(atualTimeZoneStringFormat);
        expect(atualTimeZone - defaultTime > 0.0001);

    });

    it('[Test, Description("Should display date/time when the user clicks "Get Date Time using XHR",  Priority="P0", ID= "SS0003"]', () => {

        let button: any;
        let defaultTimeZoneStringFormat: any;
        defaultTimeZoneStringFormat = MainScreen.getTimeZone().getText();
        const defaultTime = Date.parse(defaultTimeZoneStringFormat);
        let atualTimeZoneStringFormat: any;

        browser.pause(3000);

        button = MainScreen.getDateTimeUsingXHR();
        button.waitForDisplayed();
        button.click();

        browser.pause(3000);

        atualTimeZoneStringFormat = MainScreen.getTimeZone().getText();
        const atualTimeZone = Date.parse(atualTimeZoneStringFormat);
        expect(atualTimeZone - defaultTime > 0.0001);

    });

    it('[Test, Description("Should be displayed the outsystems login webpage inside the app, in another screen,  Priority="P0", ID= "SS0004"]', () => {

        let button: any;

        button = MainScreen.changeOtherScreen();
        button.waitForDisplayed();
        button.click();

        const outSystemsPageText = OtherScreen.getTitle().getText();
        expect(outSystemsPageText).toContain(OTHER_SCREEN.TEXT);

        const frameId = $('#iframe');
        frameId.waitForDisplayed();

        const url = frameId.getAttribute(OtherScreen.MENU_SELECTORS.SRC);
        expect(url).toContain(OTHER_SCREEN.EXPECTED_URL);

        const backButton = OtherScreen.backMainPage();
        backButton.waitForDisplayed();
        backButton.click();
        browser.reset();

    });

    it('[Test, Description("Should be be displayed the outsystems login webpage in a screen outside the app,  Priority="P0", ID= "SS0005"]', () => {

        let buttonRedirect: any;
        buttonRedirect = MainScreen.redirectURL();

        buttonRedirect.waitForDisplayed();
        buttonRedirect.click();

        let nativeAppContext = browser.getContexts()[0];
        Context.switchToContext(nativeAppContext);
        const classNameAndPartialText = OtherScreen.getWelcomeMessage();

        expect(classNameAndPartialText).toContain(OTHER_SCREEN.WELCOME_MESSAGE);

    });

});
