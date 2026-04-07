# dhtmlxGantt Vue3 实际参考

来源：`taskGantt.vue`（2501行，Hussar系统生产环境代码）

## 初始化

```javascript
// mounted()
initGantt() {
  if (self.ganttInstance) return
  self.ganttInstance = Gantt.getGanttInstance({
    container: self.$refs.gantt,
    plugins: {
      drag_timeline: true,
      undo: true,
      tooltip: true,
      quick_info: true,
      marker: true
    },
    config: {
      date_format: '%d-%m-%Y %H:%i',
      xml_date: '%Y-%m-%d %H:%i:%s',
      duration_unit: 'minute',
      min_duration: 10 * 60 * 1000,
      time_step: 10,
      round_dnd_dates: false,
      dblclick_create: false,
      drag_links: false,
      drag_progress: false,
      drag_resize: false,
      drag_move: self.dragMove,    // 拖拽移动任务
      columns: [],                    // 列配置
      layout: {
        css: 'gantt_container',
        rows: [
          { cols: [
            { view: 'grid', id: 'grid', scrollX: 'scrollHor', scrollY: 'scrollVer' },
            { resizer: true, width: 1 },
            { view: 'timeline', id: 'timeline', scrollX: 'scrollHor', scrollY: 'scrollVer' },
            { view: 'scrollbar', scroll: 'y', id: 'scrollVer' }
          ]},
          { view: 'scrollbar', scroll: 'x', id: 'scrollHor', height: 20 }
        ]
      },
      drag_timeline: { ignore: '.gantt_task_line, .gantt_task_link', useKey: false },
      show_quick_info: false,
      lightbox: null,
      row_height: 40,
      bar_height: 39,
    }
  })
}

// beforeDestroy
self.ganttInstance.destructor()
self.ganttInstance = null
```

## 数据加载

```javascript
// 加载数据
this.ganttInstance.parse({
  data: tasks,      // 任务数组
  links: dependencies  // 依赖关系数组
})

// 获取数据
const data = this.ganttInstance.serialize()
```

## 拖拽事件

```javascript
// 拖拽前
this.ganttInstance.attachEvent("onBeforeTaskDrag", (id, mode, e) => {
  // mode: "move" | "resize" | "progress"
  return true
})

// 拖拽后
this.ganttInstance.attachEvent("onAfterTaskDrag", (id, mode, e) => {
  const task = this.ganttInstance.getTask(id)
  // 调用 API 保存
})
```

## 列配置示例

```javascript
columns: [
  { label: '任务名称', prop: 'taskName', width: 200, tree: true },
  { label: '开始时间', prop: 'startDate', width: 100 },
  { label: '工期(天)', prop: 'duration', width: 80 },
  { label: '负责人', prop: 'assigneeName', width: 100,
    template: (task) => task.assigneeName || '未分配'
  }
]
```
