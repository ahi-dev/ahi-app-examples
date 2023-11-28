# AHI MultiScan Web - FaceScan Tutorial

This tutorial shows you how to implement AHI's MultiScan SDK for Web. The example will include a full FaceScan process.

[GitHub source code](https://github.com/ahi-dev/ahi-app-examples/tree/23.9/trunk/Web)

[SDK Reference Docs](https://ref.advancedhumanimaging.io/js/ahi/AHIMultiScan.html)

## Environment

Before you start building your web form, you will need to set up your development environment.

You will need:
- an IDE/text editor,
- a web server,
- a web browser

## Prepare

To get started we will create several files called in our development folder:
- `index.html`to create form input page - ***instructions not provided***
- `indexscript.js` to setup and initialize MultiScan
- `styles.css` for styling - ***instructions not provided***
- `results.html` to handle scan results passed back from MultiScan SDK
- `resultsScript.js` to create results page - ***instructions not provided***

**Note:** Refer to [FaceScan Schema](https://www.ahi.tech/knowledge-base/facescan-v22-0-schemas) to view the user input requirements for the form page.

### There are three parts to implementing MultiScan Web SDK:
1. Setting up the system
2. Initializing the service
3. Handling the results

The form input and results display pages are not a part of this SDK. We recommend that developers build these pages according to their use case and include a scan guide for users to achieve best results.

## 1. Setup MultiScan SDK

In the first instance we will configure our code to call the AHI service and set-up MultiScan. This is done by adding the following code to `index.html` before the closing `BODY` tag

```html
<script
  type="text/javascript"
  src="https://sdk.advancedhumanimaging.io/js/23.9/ahi.js"
>
</script>
```

 With the javascipt now being loaded we need to turn our attention to `indexscript.js`.

 The following code needs to be added to the file:

```javascript
/**
 * Initializes the AHIMultiScan object and authorizes the user.
 * @returns {Promise} A Promise that resolves when the setup and authorization are successful, and 
 * rejects if there's an error.
 */
function initScan() {
  multi = new AHIMultiScan();
  multi
    .setup(
      JSON.stringify({
        TOKEN:
          "<token>",
      })
    )
    .then(() => {
      console.log("[Setup] Completed successfully.");
      multi
        .userAuthorize("<user_id>", "<salt>", JSON.stringify(["<claim_1>", "<claim_2>", ...]))
        .then(() => {
          console.log("[Auth] Completed successfully.");
        })
        .catch((err) => {
          console.log("[Auth] Failed.");
        });
    })
    .catch((error) => {
      console.error("[Setup] FAILED: An error occurred:", error);
    });
}
```

The above code requires that you add a `<TOKEN>` to the MultiScan set up. Please ensure that you have a valid MultiScan token prior to continuing. If you do not have a token please contact AHI via your primary contact to arrange. It also requires that you supply: `<userID>`, `<salt>`, and `<claims>`.  

Details on the authorization requirements for these fields can be found [here](https://ref.advancedhumanimaging.io/js/ahi/AHIMultiScan.html#userAuthorize).


An example may look like:

```javascript
.userAuthorize("USER1234", "Company12345", JSON.stringify(["13101978",]))
```

Where `"USER1234"` is the user's id, `"Company12345"` is a salt value unique to you your company, and `"13101978"` (D/M/Y) represents the user's join date (helps to harden the security, and should be a value unique to the user but not changeable).

### Note to Developer: User Claims

An array of user claims is reccommended for user authorization in order to provide additional information about the user beyond their username and identifier. These claims can be used to verify that the user is who they claim to be.

Here are a few examples of user claims that do not change and can be used for user authorization:

- **Date Created**: The date an account was created (and won't change).
- **Date of Birth**: A user's birthday (provided it cannot be changed later).

It is generally recommended to use claims that do not change over time for user authorization, as using claims that can change, such as an email address or password, can result in authorization issues if the user updates their information.

### Note to Developer: AuthN vs. AuthZ

AHI make a clear distinction of what is user authentication (authN) and user authorization (authZ).

**Authentication (authN)** is the process of confirming and validating that the user is who they say they are, through mechanisms such as username/password, keys, or badges.

**Authorization (authZ)** is the process of granting an authenticated user access to services and data they have permission to.

As AHI provide a service and not a user base, AHI technology is only concerned with authorization of a partner's authentic users. Therefore, AHI only provide the mechanisms for authorizing a user to the AHI scan technology, and not the authentication or management of user.

Therefore Authenication is left as an exercise for the developer in this example, and we will focus on Authorization.


## 2. Initialize Scan

One you have a MultiScan Object and your user is authorised you are ready to initialize a FaceScan.

FaceScan has a set of requirements that need to be adhered to in order for a successful Scan session. The following schema specifies the expected parameters that the App must pass to MultiScan `initiateScan()` method: [FaceScan Schema - Input](https://www.ahi.tech/knowledge-base/facescan-v22-0-schemas).

### Validation

It is important to note that the schema gives the type and validation rules required for each field.

Implementing the validation for these requirements is left to the developer for a production system. For our didactical example we will use HTML5's built in validation. We recommend you do not solely rely on this method and implement additional validation as per your preferred framework.

### Submitting Form

To initalise the form an `onSubmit` action will need to be added.

```html
<form onsubmit="return postValidateForm(event)">
  <h2>Advanced Health Intelligence</h2>
  <h3>New FaceScan</h3>
  <!-- Age -->
  <div>
    <label
      for="age"
      title="Web Scan is not intended for children under 13 years old."
    >
      Age:
    </label>

    <input
      type="number"
      id="age"
      name="age"
      required
      min="13"
      max="120"
      pattern="\d*"
      placeholder="Enter your current age in years"
    />
  </div>
  <!-- ... remaining form code ... -->
</form>
```

In this instance we call `postValidateForm` as we want to satisfy the `MIN` / `MAX` requirements for BMI in the schema. As we can't easily achieve this using the HTML5 validation we call our own custom code.

Add the custom code for `postValidateForm` function to `indexscript.js`. This is for example purposes and we recommend that the developer implement validation according to their own use case and error messaging system as required.

```javascript
/**
* Validates form input values and calculates BMI if input is valid.
* This is a requirement for the Web Measurement Service.
* @param {Event} event - The form submit event object
*/
function postValidateForm(event) {
  const weight = parseFloat(document.querySelector("#weight").value);
  const height = parseFloat(document.querySelector("#height").value);

  const submitButton = document.querySelector("#submit-button");
  submitButton.disabled = true;

  if (!isBMIValid(weight, height)) {
    alert(
      "BMI is not valid. Must be between 10 and 60.\nPlease check and try again."
    );
    submitButton.disabled = false;
    return;
  }
  // Start the Web Measurment Service
  callScan(event);
}

/**
* Validates if BMI is within acceptable range.
* @param {number} weight - The weight of the person in kg.
* @param {number} height - The height of the person in cm.
* @returns {boolean} - Returns true if BMI is between 10 and 6, otherwise false.
*/
function isBMIValid(weight, height) {
  const bmi = weight / Math.pow(height / 100, 2);
  return bmi >= 10 && bmi <= 65;
}
```

`postValidateForm` pulls validated **height** and **weight** values from the form to calculate BMI. If this is not within range it will alert the user.

If the form passes all validation tests then `callScan(event)` is actioned.

### Web Measurement Service

If form validation is successful, the `callScan()` method includes all the params we need to conduct a successful FaceScan.

```javascript
const params = {
  cb: callback_domain + callback_url,
  kg_ent_weight: parseInt(weightInput.value),
  cm_ent_height: parseInt(heightInput.value),
  enum_ent_sex: sexInput.value,
  yr_ent_age: parseInt(ageInput.value),
  bool_ent_smoker: smokerInput.value === "yes",
  bool_ent_hypertension: hypertensionInput.value === "yes",
  bool_ent_bloodPressureMedication:
    bloodPressureMedicationInput.value === "yes",
  enum_ent_diabetic: diabeticInput.value,
};
  try {
    const stringifyParams = JSON.stringify(params);
    // Call the FaceScan
    multi.initiateScan("face", stringifyParams);
  } catch (error) {
    console.error("[Scan] An Error occurred:", error);
    throw new Error(error.message);
  }
 }
```

It is important to note that `params` contains a callback url key `cb` in addition to the metrics required for the scan.

This is the URL that you wish results to be returned to. Once returned, it is the developer's responsibility to decode and interpret these results, although AHI have guidance on how this can be achieved.

At this point we submit the form and hand control over to the MultiScan SDK and allow the user to complete their Scan.

Assuming the process is successful we will expect a callback from the service.

## 3. Handling Results

See [FaceScan Schema - Output](https://www.ahi.tech/knowledge-base/facescan-v22-0-schemas)

Scan results are returned as a Base64 queryString within the URL.

The developer has to extrapolate and intepret these raw outputs according to their needs.

The sample code below is added to `resultsScript.js`. It simply iterates through the returned results and outputs them to the page.

```javascript
/**
* Decodes the base64-encoded `result` parameter value from the URL and parsing its content to JSON, then storing it in `scanResult`.
* @type {string}
*/
const scanResult = atob(
  new URLSearchParams(window.location.search).get("result")
);

/**
* Creating a document fragment to be used as a temporary container.
* @type {DocumentFragment}
*/
const resultList = document.createDocumentFragment();

/**
* Looping through each property from the parsed JSON object in `scanResult`, creating a list item element for each one and appending text content that represents the key-value pair to it.
*/
for (const [property, value] of Object.entries(JSON.parse(scanResult))) {
  const listItem = document.createElement("li");
  listItem.textContent = `${property}: ${value}`;
  resultList.appendChild(listItem);
}

/**
* Selecting the element with ID `JSONResults` and appending the list items to it.
*/
document.querySelector("#JSONResults").appendChild(resultList);
```

## Demo

View the AHI styled demo for an example of the full FaceScan Web process. 

[Live Demo](https://ahi-lab.github.io/ahi-app-web/)
