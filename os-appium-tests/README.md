# SSLPinning tests implementation

## Description

Implementation of tests to validate the correct behaviour of the file named pinning\pinning.json with the hash of the 
public keys of the certificates.
Using that file containing the correct hash of public key, the user will be able to interact with sample app. 
If the hash is not valid, when the user clicks to get desired information won´t get any answer.

## Running Tests In SSLPinning  Project

## Setup
1. Have Appium installed
2. Have a Node version installed below v12.0.0 (not including)
3. Run npm install to get the required dependencies

## Run Tests locally

To run the tests:
1. Start Appium server:
    * Either by using the command line or launching the server with the appium desktop application
    * Have a device connected or emulator available
2. Run the command `npm run android`

After the tests run, you can then generate a report with allure: `npm run report`

The generated report will be located in the **_allure-report_** folder

## AWS Device Farm

To run the tests in the device farm follow the steps: 
1. Create a new run in AWS
2. Upload your application (either _.ipa_ or _.apk_). Hit Next
3. Upload the test bundle:
    * Run the command `npm run bundleAws` and upload the generated `awsTests.zip` file
    * Hit Next
4. Use the contents of the [/awsAndroid.yml](awsConfiguration.yml) file for the script configuration


