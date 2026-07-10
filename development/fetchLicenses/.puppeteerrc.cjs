// https://pptr.dev/troubleshooting#running-puppeteer-on-google-app-engine

const {join} = require('path');
var cacheDirectory = join(__dirname, 'node_modules', '.puppeteer_cache');
/**
 * @type {import("puppeteer").Configuration}
 */
module.exports = {
  cacheDirectory: cacheDirectory,
};
