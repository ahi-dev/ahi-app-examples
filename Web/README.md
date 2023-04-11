# AHI MultiScan Web - FaceScan Tutorial

This tutorial shows you how to implement AHI's MultiScan SDK for Web. The example will include a full FaceScan process.

[GitHub source code](https://github.com/ahi-dev/ahi-app-examples/tree/22.0/trunk/Web)

[SDK Reference Docs](https://ref.advancedhumanimaging.io/js/ahi/AHIMultiScan.html)

## Environment

Before you start building your web form, you will need to set up your development environment.

You will need:
- an IDE/text editor,
- a web server,
- a web browser

## Set-up

To get started we will create several files called in our development folder:
- `index.html`
- `styles.css`
- `indexscript.js`

Our initial plan is as follows:
- Add scripts to `index.html`.
- Create a HTML form.
- Add basic styling.
- Test and get ready to handle results.

## Adding Scripts

The AHI Web MultiScan has two components. Setting up the system, and submitting to the service.

In the first instance we will configure our code to call the AHI service and set-up MultiScan. This is done by adding the following code before the closing `BODY` tag.

```html
<script
  type="text/javascript"
  src="https://sdk.advancedhumanimaging.io/js/22.0/ahi.js"
>
</script>
```

 With the javascipt now being loaded we need to turn our attention to `indexscript.js`.

 The following code needs to be added to the file:

```javascript
/**
 * Initializes the AHIMultiScan object and authorizes the user for SDKTEST with an empty permissions array.
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

The above code requires that you add a `<token>` to the MultiScan setup (provided by AHI to the client developer). It also requires that the `<user_id>` and `<salt>` values be supplied, with optional set of `<claims>`. 

Details on the requirements for these fields can be found [here](https://ref.advancedhumanimaging.io/js/ahi/AHIMultiScan.html#userAuthorize).


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

**Please ensure that you have a valid Multiscan token prior to continuing.**

If you do not have a token please contact AHI via your primary contact to arrange.

## Initialize Scan

One you have a MultiScan Object and your user is authorised you are ready to initialize a FaceScan.

FaceScan has a set of requirements that need to be adhered to in order for a successful Scan session. The following schema specifies the expected parameters that the App must pass to MultiScan `initiateScan()` method: [MultiScan SDK FaceScan Schema](https://docs.advancedhumanimaging.io/v22.0/Reference/SDK/FaceScan/Schemas/).

### Note to Devleoper: Validation

It is important to note that the schema gives the type and validation rules required for each field.

Implementing the validation for these requirements is left to the developer for a production system. For our didactical example we will use HTML5's built in validation. We recommend you do not solely rely on this method and implement additional validation as per your preferred framework.

## Submitting Form

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
  <!-- ... form code ... -->
</form>
```

In this instance we call `postValidateForm` as we want to satisfy the `MIN` / `MAX` requirements for BMI in the schema. As we can't easily achieve this using the HTML5 validation we call our own custom code.

`postValidateForm` pulls validated **height** and **weight** values from the form to calculate BMI. If this is not within range it will alert the user.

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

Again, this is didactical and we recommend that the developer implement validation according to their own use case and error messaging system as required.

If the form passes all validation tests then `callScan(event)` is actioned.

## Web Measurement Service

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
```

It is important to note that `params` contains a callback url key `cb` in addition to the metrics required for the scan.

This is the URL that you wish results to be returned to. Once returned, it is the developer's responsibility to decode and interpret these results, although AHI have guidance on how this can be achieved.

At this point we submit the form and hand control over to the MultiScan SDK and allow the user to complete their Scan.

Assuming the process is successful we will expect a callback from the service.

## Handling Results

Scan results are returned as a Base64 queryString within the URL.

The results page in the demo simply iterates through the returned results and outputs them to the page. Extrapolating the results and interpreting them is left as an exercise for the developer.

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

### Example Demo

With the above information you should have the core knowledge to work with MultiScan Web and build interfaces that work with various frameworks as required.

[Live Demo](https://ahi-lab.github.io/ahi-app-web/)
