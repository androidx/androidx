const node = document.querySelector('#explore');

const buildFragment = (url: string): Node => {
  const fragment = `
    <tr class="additional_info">
    <th data-key="additional_info"><!---->additional_info<!----></th>
    <!---->
    <td>
      <!---->
      <div class="" data-key="additional_info" data-value="${url}">
        <a href="${url}" target="_blank">Additional Information</a>
      </div><!---->
    </td><!---->
    <td>
      <add-icon-sk data-key="additional_info" data-values="[&quot;${url}&quot;]">
        <svg class="icon-sk-svg"
          xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
          <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
        </svg></add-icon-sk>
    </td><!---->
    </tr>
  `;
  const template = document.createElement('template');
  template.innerHTML = fragment;
  return template.content.querySelector('.additional_info');
};

let DEVICE_NAME: string | null = null;
let BUILD: string | null = null;
let TEST_NAME: string | null = null;

const buildUrl = (): string | null => {
  if (BUILD && TEST_NAME && DEVICE_NAME) {
    const encodedBuild = encodeURIComponent(BUILD);
    const encodedTestName = encodeURIComponent(TEST_NAME);
    const encodedDeviceName = encodeURIComponent(DEVICE_NAME);
    return `https://androidx.dev/tests/artifacts/builds/${encodedBuild}?testName=${encodedTestName}&device=${encodedDeviceName}`;
  }
  return null;
};

const callback = () => {
  const device = document.querySelector("div[data-key='device_name']");
  const test = document.querySelector("div[data-key='test']");
  const logEntry = document.querySelector('#logEntry');
  if (device && test) {
    const deviceName = device.getAttribute('data-value');
    const testName = test.getAttribute('data-value');
    DEVICE_NAME = deviceName;
    TEST_NAME = testName;
  }
  if (logEntry) {
    const content = logEntry.textContent;
    if (content) {
      const matches = content.match(/jump-to-build[/](\d+)/);
      if (matches) {
        BUILD = matches[1];
        updateUrl();
      }
    }
  }
};

const updateUrl = () => {
  observer.disconnect();
  const url = buildUrl();
  if (url) {
    // Populate details
    const details = document.querySelector('div#details table.clickable_values tbody');
    if (details) {
      const existing = details.querySelector('.additional_info');
      if (existing) {
        existing.remove();
      }
      const row = buildFragment(url);
      details.appendChild(row);
    }
  }
  observer.observe(node, {
    subtree: true,
    childList: true,
    attributes: true
  });
};

const observer = new MutationObserver(callback);
observer.observe(node, {
  subtree: true,
  childList: true,
  attributes: true
});
