import * as Context from '../helpers/Context';

export const MenuSelectors = {
    GET_DATE_TIME_USING_ACTION: 'btn_GetDateTimeUsingAction',
    GET_DATE_TIME_USING_REST: 'btn_GetDateTimeUsingREST',
    GET_DATE_TIME_USING_XHR: 'btn_GetDateTimeUsingXHR',
    RESET_SERVER_DATE_TIME: 'btn_resetDateTime',
    CHANGE_TO_OTHER_SCREEN: 'btn_ChangeToOtherScreen',
    REDIRECT_TO_URL: 'btn_RedirectURL',
    LOG_MESSAGE: 'btn_LogMessage',
    SERVER_DATE_TIME: 'timeZone',
    SERVER_DATE_TIME_VALUE: '//*[@id="timeZone"]',
    SERVER_DATE_TIME_VALUE_TEST: '//*[@id="timeZone"])[0].textContent',
    REQUEST_MESSAGE_ERROR: 'Request failed with an error',
    TIME_ZONE: 'timeZone',
    OTHER_SCREEN: 'Other Screen',
    B1_TITLE: 'b1-Title',
    FEED_BACK_MESSAGE: 'feedbackMessageContainer',
};
const SELECTORS = {
    ANDROID: {
        OUT_SYSTEMS_WEB_PAGE: '//*[contains(@text, "Welcome")]',

    },
    IOS: {
        OUT_SYSTEMS_WEB_PAGE: '//XCUIElementTypeStaticText[@name="Welcome"]',

    }
};

export function getWelcomeMessage(driver) {
    let selector = driver.isAndroid ? SELECTORS.ANDROID.OUT_SYSTEMS_WEB_PAGE : SELECTORS.IOS.OUT_SYSTEMS_WEB_PAGE;
    return (selector);
}

export function redirectURL() {
    return Context.getElemBySelector('#' + MenuSelectors.REDIRECT_TO_URL);
}
export function changeOtherScreen() {
     return Context.getElemBySelector('#' + MenuSelectors.CHANGE_TO_OTHER_SCREEN);
}
export function getDateTimeAction(): WebdriverIO.Element {
    return Context.getElemBySelector('#' + MenuSelectors.GET_DATE_TIME_USING_ACTION);
}

export function getDateTimeUsingRest(): WebdriverIO.Element {
    return Context.getElemBySelector('#' + MenuSelectors.GET_DATE_TIME_USING_REST);
}

export function getDateTimeUsingXHR(): WebdriverIO.Element {
    return Context.getElemBySelector('#' + MenuSelectors.GET_DATE_TIME_USING_XHR);
}

export function getTimeZone(): WebdriverIO.Element {
    return Context.getElemBySelector("#" +  MenuSelectors.B1_TITLE);
}
export function getTitle(): WebdriverIO.Element {
    return Context.getElemBySelector('#' + MenuSelectors.B1_TITLE);
}
export function getFeedbackMsgCard(): WebdriverIO.Element {
    return Context.getElemBySelector('#' +  MenuSelectors.FEED_BACK_MESSAGE);
}

export const SCREENTITLES = {
    HOME_SCREEN: 'SSL Pinning Sample App'
};
