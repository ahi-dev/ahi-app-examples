let multi = null;
const callback_url = "results.html";
const callback_domain = "http://127.0.01:5500/"; // Used for local test

/**
 * Initializes the AHIMultiScan object and authorizes the user for SDKTEST with an empty permissions array.
 * @returns {Promise} A Promise that resolves when the setup and authorization are successful, and rejects if there's an error.
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
        .userAuthorize("<user_id>", "<salt>", JSON.stringify(["<claims>"]))
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

/**
 * Initiates a face scan with the parameters gathered from the form.
 */
function callScan(event) {
  event.preventDefault(); // Prevents page from refreshing on form submission

  const weightInput = document.querySelector("#weight");
  const heightInput = document.querySelector("#height");
  const sexInput = document.querySelector("#biological-sex");
  const ageInput = document.querySelector("#age");
  const smokerInput = document.querySelector("#smoker");
  const hypertensionInput = document.querySelector("#hypertension");
  const bloodPressureMedicationInput = document.querySelector("#bloodpressure");
  const diabeticInput = document.querySelector("#diabetic");

  // FaceScan Schema :

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
