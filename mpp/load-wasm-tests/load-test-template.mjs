const karmaLoaded = window.__karma__.loaded.bind(window.__karma__);
window.__karma__.loaded = function() {}

// {module-name} is expected to be replaced by gradle Copy task (see AndroidXComposeMultiplatformExtensionImpl)
import { instantiate } from './{module-name}-wasm-js-test.uninstantiated.mjs';

await wasmSetup;
(await instantiate({ skia: Module['asm'] })).exports.startUnitTests();
karmaLoaded();