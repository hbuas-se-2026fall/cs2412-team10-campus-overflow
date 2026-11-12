import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import prettier from 'eslint-config-prettier';

// Lab 5 / Step 2：前端代码质量基础设施（SM 维护）
// 规则取向：TypeScript 严格模式无 any（25% 评分）、react-hooks 规则（Hooks 25%）、格式完全交给 Prettier。
export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'coverage', 'src/api/generated/**'] },

  // 构建脚本与配置文件：Node 环境
  {
    files: ['**/*.{js,mjs,cjs}'],
    ...js.configs.recommended,
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.node },
    },
  },

  // 业务源码：TypeScript + react-hooks
  ...tseslint.configs.recommended.map((cfg) => ({ ...cfg, files: ['**/*.{ts,tsx}'] })),
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.browser },
    },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs.recommended.rules,
      // 评分点：TS 严格模式，禁止 any 逃逸
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],
      'no-console': ['error', { allow: ['warn', 'error'] }],
    },
  },

  // 关闭与 Prettier 冲突的格式类规则，格式由 prettier --check 负责
  prettier,
);
