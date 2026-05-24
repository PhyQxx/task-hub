/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '*.css' {
  const css: string
  export default css
}

declare module 'dhtmlx-gantt' {
  const gantt: any
  export default gantt
  export const gantt: any
}
