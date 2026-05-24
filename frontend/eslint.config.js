import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tsParser from '@typescript-eslint/parser'

const browserGlobals = {
  window: 'readonly',
  document: 'readonly',
  navigator: 'readonly',
  localStorage: 'readonly',
  sessionStorage: 'readonly',
  console: 'readonly',
  setTimeout: 'readonly',
  clearTimeout: 'readonly',
  setInterval: 'readonly',
  clearInterval: 'readonly',
  fetch: 'readonly',
  URLSearchParams: 'readonly',
  URL: 'readonly',
  location: 'readonly',
  history: 'readonly',
  alert: 'readonly',
  confirm: 'readonly',
  prompt: 'readonly',
  requestAnimationFrame: 'readonly',
  cancelAnimationFrame: 'readonly',
  Element: 'readonly',
  HTMLElement: 'readonly',
  Event: 'readonly',
  CustomEvent: 'readonly',
  MouseEvent: 'readonly',
  KeyboardEvent: 'readonly',
  MessageEvent: 'readonly',
  WebSocket: 'readonly',
  Blob: 'readonly',
  FileReader: 'readonly',
  FormData: 'readonly',
  XMLHttpRequest: 'readonly',
  AbortController: 'readonly',
  performance: 'readonly',
  crypto: 'readonly',
}

export default [
  {
    ignores: ['dist/**', 'node_modules/**', '**/*.d.ts'],
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tsParser,
      globals: browserGlobals,
    },
    rules: {
      ...js.configs.recommended.rules,
      'no-console': 'warn',
      'no-unused-vars': 'off',
    },
  },
  ...pluginVue.configs['flat/recommended'].map(config => ({
    ...config,
    files: ['**/*.vue'],
  })),
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tsParser,
      },
      globals: browserGlobals,
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/max-attributes-per-line': 'off',
      'no-console': 'warn',
    },
  },
]
