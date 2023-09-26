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
