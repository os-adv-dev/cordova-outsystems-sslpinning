import {DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_INTERVAL} from '../Constants';
import * as AndroidUtils from '../helpers/AndroidUtils';
import * as IosUtils from '../helpers/IOSUtils';

const SELECTORS = {
    ANDROID: {
        PERMISSION_ALLOW_BUTTON : 'id/permission_allow_button',
        PERMISSION_DENY_BUTTON: '*//android.widget.Button[@resource-id="com.android.packageinstaller:id/permission_deny_button"]',
    },

    IOS: {
        PERMISSION_DIALOG: '*//XCUIElementTypeAlert',
        PERMISSION_ALLOW_BUTTON: '*//XCUIElementTypeButton[@name="Allow"]',
        PERMISSION_DENY_BUTTON: '*//XCUIElementTypeButton[@name="Don’t Allow"]',
    },
};

class PermissionAlert {

    /**
     * Wait for the alert to exist
     */
    public static waitForIsShown(isShown = true, driver): void {
        this.getPermissionButton(true, driver).waitForExist(DEFAULT_TIMEOUT, !isShown);
    }

    /**
     * Check if exists the alert
     */
    public static isShown(driver): boolean {
        const permissionButton = this.getPermissionButton(true, driver);
        if (permissionButton === undefined) {
            return false;
        } else {
            return permissionButton.isDisplayed();
        }
    }

    /**
     * Allow or deny a permission request
     *
     * @param {boolean} allow
     */
    // public static allowPermission(allow = true, driver): void {
    //     const selectors = driver.isAndroid ? SELECTORS.ANDROID : SELECTORS.IOS;
    //     const buttonSelector = allow ? selectors.PERMISSION_ALLOW_BUTTON : selectors.PERMISSION_DENY_BUTTON;
    //     const permissionButton = $(buttonSelector);
    //     permissionButton.click();
    // }

    public static allowPermission(allow = true, driver): void {
       this.getPermissionButton(allow, driver).click();
    }

    public static getPermissionButton(allow = true, driver): WebdriverIO.Element {
        const selectors = driver.isAndroid ? SELECTORS.ANDROID : SELECTORS.IOS;
        const buttonSelector = allow ? selectors.PERMISSION_ALLOW_BUTTON : selectors.PERMISSION_DENY_BUTTON;
        return driver.isAndroid ?
            AndroidUtils.getElemByPartialId(buttonSelector, false) :
            IosUtils.getElemByXPath(buttonSelector, true);
    }

    /**
     * Get the alert text
     *
     * @return {string}
     */
    public static text(driver): string {
        return driver.getAlertText();
    }

}

export default PermissionAlert;
