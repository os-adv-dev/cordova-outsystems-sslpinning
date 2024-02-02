import * as Context from '../helpers/Context';


export const MENU_SELECTORS = {

    BACK_BUTTON: 'b2-Back',
    SRC: 'src',
    WELCOME_MESSAGE: 'Welcome',
    B1_CONTENT: 'b1-Content',
    B1_TITLE: 'b1-Title',

};


export const SELECTORS = {
    ANDROID: {
        OUT_SYSTEMS_WEB_PAGE: '//*[contains(@text, "Welcome")]',

    },
    IOS: {
        OUT_SYSTEMS_WEB_PAGE: '//XCUIElementTypeStaticText[@name="Welcome"]',
    }
};

export function getWelcomeMessage() {
    let selector = browser.isAndroid ? SELECTORS.ANDROID.OUT_SYSTEMS_WEB_PAGE : SELECTORS.IOS.OUT_SYSTEMS_WEB_PAGE;
    return (selector);
}

export function backMainPage() {
    return Context.getElemBySelector('#' + MENU_SELECTORS.BACK_BUTTON);
}

export function getTitle() {
    return Context.getElemBySelector('#' + MENU_SELECTORS.B1_TITLE);
}

export function getContent() {
    return Context.getElemBySelector('#' + MENU_SELECTORS.B1_CONTENT);
}
