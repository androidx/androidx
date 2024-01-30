// rollup.config.js
import json from '@rollup/plugin-json';
import typescript from '@rollup/plugin-typescript';

export default {
  input: 'src/content-scripts.ts',
  output: {
    file: 'dist/assets/content-scripts.js',
    sourcemap: true,
    format: 'iife'
  },
  plugins: [
    typescript({
      sourceMap: true
    }),
    json()
  ]
};
