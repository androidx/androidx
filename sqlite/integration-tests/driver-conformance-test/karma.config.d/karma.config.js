// Add COOP and COEP headers required for OPFS VFS for SQLite WASM
// See: https://sqlite.org/wasm/doc/trunk/persistence.md#vfs-opfs
;(function(config) {
    config.customHeaders = [
        {
          match: '.*',
          name: 'Cross-Origin-Opener-Policy',
          value: 'same-origin'
        },
        {
          match: '.*',
          name: 'Cross-Origin-Embedder-Policy',
          value: 'require-corp'
        }
      ];
})(config);