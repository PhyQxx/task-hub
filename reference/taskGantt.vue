<template>
  <l-layout slot="content" align="horizontal">
    <!-- 创建一个 div 容器，用于放置甘特图 -->
    <div :class="['task-gantt',
        { 'task-gantt--editable': localBtnConfig.isPreScheduling || localBtnConfig.showInfluenceLog },{ 'task-gantt--dialogeditable': isShowCheckBoard && manualSchedule },
        { 'task-gantt--splittable': showSplitIcon }]"
        ref="gantt"
      v-loading="loading"></div>
    <!-- 可折叠区域 - 错误信息 -->
    <error-message slot="tail" v-show="errItems?.length" :errItems="errItems" @errorTasks="onErrorTasks"></error-message>
    <l-dialog title="任务明细" size="medium" :visible.sync="tableVisible" :append-to-body="true">
      <!-- 添加日期区间筛选 -->
      <div style="display: flex; align-items: center; margin-bottom: 15px; gap: 10px;">
        <span>日期筛选:</span>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="yyyyMMdd"
          format="yyyy-MM-dd"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          clearable
          @change="handleDateRangeChange"
          style="width: 300px;"
        />
        <el-button
          v-if="dateRange && (dateRange[0] || dateRange[1])"
          @click="clearDateRange"
          size="small"
        >
          清空筛选
        </el-button>
      </div>
      <l-table
        ref="lTable"
        :columns="columns"
        :data="filteredTableData"
      ></l-table>
    </l-dialog>
  </l-layout>
</template>

<script>
import Vue from 'vue';
import dayjs from 'dayjs';
import { Gantt } from '@/components/gantt/codebase/dhtmlxgantt.js';
import * as HussarRouter from '@/utils/HussarRouter';
import '@/components/gantt/codebase/dhtmlxgantt.css';
import ErrorMessage from './errorMessage.vue';
import { workTimeStartAM, workTimeEndAM, workTimeStartPM, workTimeEndPM, sleepStartPM, sleepEndAM} from '../util/index.js';
import { isAfterLimitTime, isBeforeLimitTime, isTimeSpanCrossDay, } from '../util/util.js';
import * as equSchedulePointApi from '#/refactor/api/task-schedule/equSchedulePointApi';

export default {
  name: 'TaskGantt',
  components: { ErrorMessage },
  props: {
    manualSchedule: { // 手动排程显示老化板图标
      type: Boolean,
      default: false
    },
    isShowCheckBoard: { // 是否显示指定检测板图标
      type: Boolean,
      default: false
    },
    data: {
      type: Array,
      default: () => []
    },
    links: {
      type: Array,
      default: () => []
    },
    btnConfig: {
      type: Object,
      default: () => {
        return {};
      }
    },
    fieldMappingDisabled: { // 是否禁用字段映射
      type: Boolean,
      default: false
    },
    fieldMapping: {
      type: Object,
      default: () => {
        return {
          id_property: 'id',
          text_property: 'text',
          start_date_property: 'start_date',
          end_date_property: 'end_date',
          duration_property: 'duration',
          duration_show_property: 'taskDurationHours',
          progress_property: 'progress',
          parent_property: 'parent',
          type_property: 'type',
          ignore_flag_property: 'ignore_flag',
          combine_sign_flag_property: 'combine_sign_flag',
          actual_start_date_property: 'taskStartTime',
          actual_end_date_property: 'taskCompleteTime'
        };
      }
    },
    ganttConfig: {
      type: Object,
      default: () => { }
    },
    currTaskId: {
      type: String
    },
    currTaskDeviceId: {
      type: String
    },
    ganttScale: {
      type: String,
      default: 'day'
    },
    type: {
      type: String,
      default: 'equip'
    },
    // 拖拽移动任务
    dragMove: {
      type: Boolean,
      default: false // 默认不可拖拽
    },
    // 判断任务是否可拖拽移动
    canMove: {
      type: Function,
      default: item => item.group === 'recommend'
    },
    // 是否显示合箱标识
    showCombineSign: {
      type: Boolean,
      default: false
    },
    // 是否显示实际时间线
    showActualLine: {
      type: Boolean,
      default: false
    },
    // 是否显示复选框
    showCheckbox: {
      type: Boolean,
      default: false
    },
    // 通道可编辑
    channelEditable: {
      type: Function
    },
    // 通道下拉选项
    channelOptions: {
      type: Array,
      default: () => []
    },
    // 是否展示更多
    moreColumn: {
      type: Boolean,
      default: false
    },
    moreColumns: {
      type: Array,
      default: () => [],
    },
    // 是否展示延期
    showDelay: {
      type: Boolean,
      default: false
    },
    // 主任务是否聚合展示子任务
    showSubTask: {
      type: Boolean,
      default: false
    },
    // 错误提示 这是第一行\n这是第二行
    // messageErr: {
    //   type: String,
    //   default: ''
    // },
    errItems: {
      type: Array,
      default: () => []
    },
    // 聚合是否展示连续效果
    isShowSubContinuous: {
      type: Boolean,
      default: false
    },
    // 左侧表格是否允许拉伸
    gridResize: {
      type: Boolean,
      default: true
    },
    rangeLines: {
      type: Object,
      default: () => ({
        startLine: '',
        endLine: '',
      })
    },
    customField: {
      type: Object,
      default: () => ({
        main: [],
        common: [],
      })
    },
    // 是否展示午休时间块
    showLunchhour: {
      type: Boolean,
      default: true
    },
    showSplitIcon: {
      type: Boolean,
      default: false
    },
  },
  data: () => {
    return {
      tableVisible: false,
      dateRange: [], // 新增日期区间筛选属性
      filteredTableData: [], // 存储过滤后的数据
      columns: [
        {
          label: '委托单号',
          prop: 'commissionNum',
          width: '150',
          formItemType: 'input'
        },
        {
          label: '任务编号',
          prop: 'taskNo',
          minWidth: '150',
          formItemType: 'input'
        },
        {
          label: '开始时间',
          prop: 'scheduledStartTime',
          width: '150',
          formItemType: 'date'
        },
        {
          label: '结束时间',
          prop: 'scheduledEndTime',
          width: '150',
          formItemType: 'date'
        }
      ],
      tableData: [],
      loading: false,
      delayPopVisible: false, // 顺延提示框
      delayDays: '', // 延期天数
      ganttInstance: null, // 甘特图实例
      currTask: {},
      pickerOptions: {
        disabledDate(time) {
          return time.getTime() < new Date().setHours(0, 0, 0, 0);
        }
      },
      datePickerFormat: 'yyyy-MM-dd HH:mm',
      dateValueFormat: 'YYYY-MM-DD HH:mm:ss',
      dateShowFormat: 'YYYY-MM-DD HH:mm',
      dateActulFormat: 'YYYY-MM-DD HH:mm:ss',
      showPreTask: [],
      keywords: '',
      // statusValue: '', // 状态
      conflictIcon: require('../img/icon-conflict.png'),
      isShowMore: false, // 展示表格数据
      errorTasks: [], // 选择错误类型，对应任务背景色变为橙色
      isTaskDrag: false, // 是否是拖动条形图
      dragTaskId: '',
    };
  },
  computed: {
    localBtnConfig() {
      return { showInfluenceLog: false, hasDevice: true, showToolbar: true, showAdjustDate: true, showRearrangeBtn: true, showPreTaskCheck: true, showSearch: true, showLegend: true, ...this.$options.propsData.btnConfig.default, ...this.btnConfig };
    },
    localFieldMapping() {
      return { ...this.$options.propsData.fieldMapping.default, ...this.fieldMapping };
    },
    ganttData() {
      return this.formatData(this.data)?.data || [];
    },
    scale: {
      get() {
        return this.ganttScale;
      },
      set(val) {
        this.$emit('update:ganttScale', val);
      }
    },
    groupByEquip() {
      return this.type === 'equip';
    },
    groupByComm() {
      return this.type === 'comm';
    },
    // 按照检测项目分组
    groupByItem() {
      return this.type === 'item';
    },
    groupLabel() {
      return this.groupByComm ? '委托' : this.groupByItem ? '检测项目' : '设备';
    }
  },
  watch: {
    tableData: {
      handler(newVal) {
        const self = this;
        this.applyDateRangeFilter(newVal);
      },
      deep: true
    },
    data: {
      handler(newVal) {
        const self = this;
        this.refreshGanttData(newVal);
      },
      deep: true
    },
    customField: {
      handler(newVal) {
        this.refreshGanttData(this.data);
      },
      deep: true
    },
    links: {
      handler() {
        this.refreshGanttData(this.data);
      }
    },
    scale: {
      handler(newVal) {
        this.ganttInstance?.ext.zoom.setLevel(newVal);
        // 解决切换比例后 半选状态和表头全选被重置的问题
        this.showCheckbox && setTimeout(() => {
          const checkboxs = this.ganttInstance?.$root.querySelectorAll('.gantt_row_selector');
          checkboxs && Array.from(checkboxs).forEach(checkbox => {
            this.handleRowCheckboxClick(checkbox);
            this.updateSelectAllCheckbox();
          });
        });
      },
      immediate: true
    },
    currTaskId: {
      handler() {
        this.currTask = this.currTaskId ? (this.ganttData.find(d => d.id === this.currTaskId) || {}) : {};
      },
      immediate: true
    },
    ganttData: {
      handler() {
        this.currTask = this.currTaskId ? (this.ganttData.find(d => d.id === this.currTaskId) || {}) : {};
      },
      deep: true,
      immediate: true
    },
  },
  created() {
  },
  mounted() {
    const self = this;
    // 挂载全局处理函数
    window.handleChannelChange = (event) => {
      const select = event.target;
      const taskId = select.closest('.gantt-channel-select').dataset.taskId;
      const val = select.value;

      const opt = this.channelOptions.find(opt => opt.value === val);
      const data = this.getAllData();
      const task = data.find(d => d.id === taskId);
      if (task) {
        task.equipChannelId = val;
        task.channelCapacity = opt?.channelCapacity;
        task.channelNo = opt?.no;
        const i = task.ids.split('|');
        const ids = [i[0], i[1], opt?.id].join('|');
        task.ids = ids;
        this.$emit('channel-change', this.getGrattData(data));
      }
    };
    // this.queryDictOptions();
    this.initGantt();
    this.$nextTick(() => {
      // 移动消息容器到甘特图内部
      const messageArea = document.querySelector('.gantt_message_area');
      if (messageArea) {
        document.querySelector('.task-gantt').appendChild(messageArea);
      }
    });
  },
  beforeDestroy() {
    const self = this;
    self.ganttInstance.destructor();
    self.ganttInstance = null;
    self.ganttInstance?.$root.removeEventListener('click', self.handleCheckEvent);
    delete window.handleChannelChange;
  },
  methods: {
    /**
     * 处理日期区间变更
     */
    handleDateRangeChange() {
      this.applyDateRangeFilter();
    },

    /**
     * 清空日期区间筛选
     */
    clearDateRange() {
      this.dateRange = [];
      this.filteredTableData = [...this.tableData];
    },
    /**
     * 应用日期区间过滤
     */
    applyDateRangeFilter() {
      // 如果没有设置日期区间，则显示所有数据
      if (!this.dateRange || (!this.dateRange[0] && !this.dateRange[1])) {
        this.filteredTableData = [...this.tableData];
        return;
      }

      const [startDate, endDate] = this.dateRange;

      // 根据选择的日期区间过滤数据
      this.filteredTableData = this.tableData.filter(item => {
        const scheduledStartTime = item.scheduledStartTime;
        // 如果有开始日期，检查任务开始时间或结束时间是否 >= 开始日期
        if (startDate) {
          if (scheduledStartTime) {
            const startTimeFormatted = this.$moment(scheduledStartTime).format('YYYYMMDD');
            if (startTimeFormatted >= startDate && startTimeFormatted <= endDate) {
              return true;
            }
          }
        }
        return false;
      });
    },
    initGantt() {
      const self = this;
      if (self.ganttInstance) return;

      const stopPropagation = function (e) {
        e.stopPropagation();
      };

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
          date_format: '%d-%m-%Y %H:%i', // 日期格式
          xml_date: '%Y-%m-%d %H:%i:%s', // 与后端交互的日期格式
          duration_unit: 'minute', // 任务时长单位
          min_duration: 10 * 60 * 1000, // 可以设置的最小时长（毫秒）
          time_step: 10, // 最小步长（分钟）
          round_dnd_dates: false, // 四舍五入到最接近的刻度标记
          dblclick_create: false, // 双击创建任务
          drag_links: false, // 拖拽创建链接
          drag_progress: false, // 禁止拖拽调整进度
          drag_resize: false, // 拖拽调整任务时长
          drag_move: self.dragMove, // 拖拽移动任务
          columns: [], // 列配置
          // scales: [
          //   {unit: "hour", step: 1, format: "%g %a"},
          //   {unit: "day", step: 1, format: "%j %F, %l"},
          //   {unit: "minute", step: 15, format: "%i"}
          // ],
          layout: {
            css: 'gantt_container',
            rows: [
              {
                cols: [
                  { view: 'grid', id: 'grid', scrollX: 'scrollHor', scrollY: 'scrollVer' },
                  { resizer: true, width: 1 },
                  { view: 'timeline', id: 'timeline', scrollX: 'scrollHor', scrollY: 'scrollVer' },
                  { view: 'scrollbar', scroll: 'y', id: 'scrollVer' }
                ]
              },
              { view: 'scrollbar', scroll: 'x', id: 'scrollHor', height: 20 }
            ]
          },
          drag_timeline: {
            ignore: '.gantt_task_line, .gantt_task_link',
            useKey: false
          },
          show_quick_info: false,
          quickinfo_buttons: [],
          lightbox: null,
          row_height: 40,
          bar_height: 39,
          ...self.ganttConfig
        },
        locale: 'cn',
        templates: {
          task_text: (start, end, task) => {
            if (this.isParent(task)) { // 聚合父任务
              return '';
            }
            if (this.showSubTask && this.isMainTask(task)) { // 有子任务的父任务
              return `<div class="task-content task-show-sub">
                        <div class="task-btn"><i class="task-influence lims-icon-danju"></i></div>
                      </div>`;
            }
            const text = this.getTaskText(task);
            if(this.showSplitIcon) {
              let el = `<div class="task-content">
                        <div class="task-text">${text}</div>`;
                  el += `<div class="task-btn"><i class="task-split lims-icon-chaifen"></i></div>`
                  el += `</div>`;
              return el;
            }
            if (this.showActualLine) { // 是否显示实际时间线-总览
              let el = `<div class="task-content task-actual-line">
                        <div class="task-text">${text}</div>`;
              el += `<div class="task-btn"><i class="task-influence lims-icon-danju"></i></div>`;
              el += `</div>`;
              return el;
            }
            let elem = `<div class="task-content">`;
            if (this.showCombineSign && task.combine_sign_flag) { // 显示合箱标识
              elem += `<div class="task-sign"><span>合箱</span></div>`;
            }
            // 除当前任务之外的其他预排程数据
            if (task.pre && task.pre === 'pre') {
              elem += `<div class="task-sign"><span>预</span></div>`;
            }
            elem += `<div class="task-text">${text}</div>`;
            // if (this.isConflict(task)) {
            //   elem += `<div class="task-btn"><img class="task-edit" src="${this.conflictIcon}" /></div>`;
            // }
            if (this.isPreScheduling(task)) {
              if (this.isConflict(task)) { // 冲突
                elem += `<div class="task-btn"><img class="task-edit" src="${this.conflictIcon}" /></div>`;
              } else { // 编辑
                elem += `<div class="task-btn"><i class="task-edit lims-icon-xianxing_bianji"></i></div>`;
              }
            }
            // 展示检测板条件：预排程的任务（橙色）&& 页面展示
            if (this.isPreScheduling(task) && this.isShowCheckBoard && this.manualSchedule) {
              elem += `<div class="task-btn"><i class="task-checkboard lims-icon-xianxing_qiehuanchengkapian"></i></div>`;
            }
            elem += `</div>`;
            return elem;
          },
          // 条形图右侧文字
          rightside_text: (start, end, task) => {
            // if (self.showDelay && task.group &&  (!task.actual_start_date || !task.actual_end_date)) {
            if (self.showDelay && (!task.actual_start_date || !task.actual_end_date) && task.extensionDays > 0) {
              return `<span style="color:#FF4D4F">延期${task.extensionDays}天</span>`;
            }
            return '';
          },
          task_class: (start, end, task) => {
            if (this.showSubTask && this.isMainTask(task)) {
              const cls = [`task-group-show-sub`];
              if (this.showDelay && (!task.actual_start_date || !task.actual_end_date) && task.extensionDays > 0) {
                cls.push(`task-group-delay`);
              }
              return cls.join(' ');
            }
            if (this.isParent(task)) {
              const cls = [`task-group-parent`];
              if (!this.getChildren(task.id).length) {
                cls.push(`is-empty`);
              }
              return cls.join(' ');
            }
            if (task.group) {
              const cls = [];
              if (task.pre && task.pre === 'pre') {
                cls.push(`task-group-pre`);
              } else {
                cls.push(`task-group-${task.group}`);
              }
              if (this.isConflict(task)) {
                cls.push(`task-group-conflict`);
              }
              if (this.showActualLine) {
                cls.push(`task-group-actual`);
                cls.push(`task-group-actual-${task.group}`);
              }
              // if (this.showDelay && (!task.actual_start_date || !task.actual_end_date)){
              if (this.showDelay && (!task.actual_start_date || !task.actual_end_date) && task.extensionDays > 0) {
                cls.push(`task-group-delay`);
              }
              return cls.join(' ');
            }
            return '';
          },
          task_row_class: (start, end, task) => {
            if (this.errorTasks?.find(r => r[this.localFieldMapping.id_property || 'id'] === task.id)) {
              return 'error-task-row';
            }
            if ((this.currTaskId && task.id === this.currTaskId) || (task.isCurrentData)) {
              return 'current-row';
            }
            return '';
          },
          grid_row_class: (start, end, task) => {
            if (this.errorTasks?.find(r => r[this.localFieldMapping.id_property || 'id'] === task.id)) {
              return 'error-task-row';
            }
            if ((this.currTaskId && task.id === this.currTaskId) || (task.isCurrentData)) {
              return 'current-row';
            }
            return '';
          },
          tooltip_text: () => false,
          quick_info_title: () => '预约详情',
          quick_info_date: () => false,
          quick_info_content: (start, end, task) => {
            if (this.isParent(task)) {
              return false;
            } else {
              const defaultValue = '-';
              const fields = [
                {
                  title: '样品信息',
                  items: [
                    { icon: 'lims-icon-xiangqing-yangpinmingcheng', label: '样品名称', key: 'sampleName' },
                    { icon: 'lims-icon-xiangqing-yangpinshuliang', label: '样品数量', key: 'spaceCost', unit: '（颗）' },
                    { icon: 'lims-icon-xiangqing-yangpinguige', label: '样品规格', key: 'specification' }
                  ]
                },
                {
                  title: '实验信息',
                  items: [
                    { icon: 'lims-icon-xiangqing-shiyanfangan', label: '实验方案', key: 'inspectionItem' },
                    { icon: 'lims-icon-xiangqing-shiyanxiangmu', label: '试验项目', key: 'itemName' },
                    { icon: 'lims-icon-xiangqing-xiangmubianhao', label: '项目编号', key: 'commissionNum' },
                    { icon: 'lims-icon-xiangqing-renwubianhao', label: '任务编号', key: 'taskNo' },
                    { icon: 'lims-icon-xiangqing-shiyantiaojian', label: '试验条件', key: 'conditions' },
                    { icon: 'lims-icon-xiangqing-ceshizhouqi', label: '测试周期', key: 'duration', unit: '（min）' },
                    { icon: 'lims-icon-xiangqing-ceshizhouqi', label: '任务周期', key: 'taskDurationHours', unit: '（h）' },
                    { icon: 'lims-icon-xiangqing-jianceyuan', label: '检测员', key: 'testUser' },
                    { icon: 'lims-icon-xiangqing-shijian', label: '开始时间', key: 'start_date', formatter: v => this.formatDate(v, this.dateShowFormat) },
                    { icon: 'lims-icon-xiangqing-shijian', label: '结束时间', key: 'end_date', formatter: v => this.formatDate(v, this.dateShowFormat) },
                    { icon: 'lims-icon-xiangqing-shijian', label: '实际开始时间', key: this.localFieldMapping.actual_start_date_property || 'taskStartTime', formatter: v => this.formatDate(v, 'YYYY-MM-DD HH:mm:ss') },
                    ...(task.scheduledCompleteTime && task.scheduledCompleteTime !== '' ? [] : [{ icon: 'lims-icon-xiangqing-shijian', label: '实际结束时间', key: this.localFieldMapping.actual_end_date_property || 'taskCompleteTime', formatter: v => this.formatDate(v, 'YYYY-MM-DD HH:mm:ss') }]),
                    { icon: 'lims-icon-xiangqing-shijian', label: '预计结束时间', key: 'scheduledCompleteTime', formatter: v => this.formatDate(v, 'YYYY-MM-DD HH:mm:ss') },
                    { icon: 'lims-icon-xiangqing-ceshizhouqi', label: '静置时间', key: 'reserveTime', unit: '（h）' },
                    { icon: 'lims-icon-xiangqing-shijian', label: '期望开始时间', key: 'expectStartTime', formatter: v => this.formatDate(v, 'YYYY-MM-DD HH:mm:ss') },
                    { icon: 'lims-icon-xiangqing-shijian', label: '期望结束时间', key: 'expectEndTime', formatter: v => this.formatDate(v, 'YYYY-MM-DD HH:mm:ss') },
                    { icon: 'lims-icon-xiangqing-xiangmubianhao', label: '测试板', key: 'testBoardName' },
                    { icon: 'lims-icon-xiangqing-xiangmubianhao', label: '样品重量', key: 'sampleWeight', unit: '（kg）' },
                  ]
                }
              ];
              let textStr = `
                <div class="gantt_quick_info__top">
                  <div class="gantt_quick_info__logo"></div>
                  <div>
                    <div class="gantt_quick_info__row">
                      <span class="gantt_quick_info__main">${task.deviceName || defaultValue}<span class="gantt_quick_info__sub">&emsp;${task.equSpecification || defaultValue}&emsp;${task.deviceId != '99999999' ? (task.deviceNum || defaultValue) : defaultValue}</span></span>
                    </div>
                    <div class="gantt_quick_info__sub">${task.locationName || defaultValue}</div>
                  </div>
                </div>
                <div class="gantt_quick_info__bottom">`;
              for (const group of fields) {
                textStr += `<div>`;
                textStr += `<div class="gantt_quick_info__title">${group.title}</div>`;
                for (const item of group.items) {
                  textStr += `<div class="gantt_quick_info__row">
                    <i class="${item.icon}"></i>
                    <span class="gantt_quick_info__label">${item.label}：</span>
                    <div class="gantt_quick_info__value">${(item.formatter ? item.formatter(task[item.key]) : task[item.key]) || defaultValue}${item.unit || ''}</div>
                  </div>`;
                }
                textStr += `</div>`;
              }
              textStr += `</div>`;
              return textStr;
            }
          }
        },
        events: {
          onTaskDblClick: (id) => {
            return false;
          },
          onTaskSelected: (id) => {
            return false;
          },
          onGanttScroll: () => {
            if (self.ganttInstance.config.show_quick_info) {
              self.ganttInstance.hideQuickInfo();
              self.ganttInstance.config.show_quick_info = false;
            }
            const { unit } = self.ganttInstance?.getScale() || {};
            this.scale = unit;
            return true;
          },
          onQuickInfo: () => {
            self.ganttInstance.$root.querySelector('.gantt_cal_quick_info')?.addEventListener('wheel', stopPropagation, { passive: false });
          },
          onAfterQuickInfo: () => {
            self.ganttInstance.$root.querySelector('.gantt_cal_quick_info')?.removeEventListener('wheel', stopPropagation, { passive: false });
          },
          onTaskClick: (id, e) => {
            if (e.target.classList.contains('gantt_task_progress_wrapper')) {
              this.tableVisible = true;
              const task = self.ganttInstance?.getTask(id);
              const params = {
                deviceId: task.deviceId,
                deviceType: task.deviceType,
                parent: task.parent,
                channelId: task.equipChannelId,
                startTime: this.$moment(task.start_date).format('YYYY-MM-DD HH:mm:ss'),
                endTime: this.$moment(task.end_date).format('YYYY-MM-DD HH:mm:ss')
              };
              equSchedulePointApi.getTasksByEqu(params).then(res => {
                if (res.code === 10000) {
                  this.tableData = res.data;
                }
              });
            }
            if (e.target.classList.contains('task-checkboard')) {
              this.$emit('selectCheckBoard', self.getCallbackParams(id));
              return true;
            }
            if (e.target.classList.contains('task-edit')) {
              self.ganttInstance.hideQuickInfo();
              self.ganttInstance.config.show_quick_info = false;
              this.$emit('adjustPreSchedule', self.getCallbackParams(id));
              return true;
            }
            if (e.target.classList.contains('task-influence')) {
              self.ganttInstance.hideQuickInfo();
              self.ganttInstance.config.show_quick_info = false;
              this.$emit('showInfluenceLog', self.getCallbackParams(id));
              return true;
            }
            if (e.target.classList.contains('task-split')) {
              self.ganttInstance.hideQuickInfo();
              self.ganttInstance.config.show_quick_info = false;
              this.$emit('showSplit', self.getCallbackParams(id));
              return true;
            }
            const task = self.ganttInstance?.getTask(id);
            if (this.isParent(task)) {
              self.ganttInstance.hideQuickInfo();
              self.ganttInstance.config.show_quick_info = false;
              return true;
            }
            const classArr = ['gantt_task_content', 'task-content', 'task-text', 'task-sign'];
            if (classArr.some(cls => e.target.classList.contains(cls))) {
              self.ganttInstance.config.show_quick_info = true;
              self.ganttInstance.showQuickInfo(id);
              return true;
            }
            self.ganttInstance.hideQuickInfo();
            self.ganttInstance.config.show_quick_info = false;
            return true;
          },
          onTaskRowClick: (id, elem) => {
            if (self.showCheckbox && !elem?.classList?.contains('gantt_row_selector')) {
              const checkbox = self.ganttInstance?.$root?.querySelector(`.gantt_row_selector[data-id="${id}"]`);
              checkbox.checked = !checkbox.checked;
              self.handleRowCheckboxClick(checkbox);
              self.updateSelectAllCheckbox();
            }
          },
          onBeforeTaskDrag: (id, mode, e) => {
            if (mode === 'move') {
              const task = self.ganttInstance?.getTask(id);
              if (this.canMove?.(task)) {
                return true;
              }
            }
            return false;
          },
          onAfterTaskDrag: (id, mode, e) => {
            const task = self.ganttInstance?.getTask(id);
            if (dayjs(task.start_date).isBefore(dayjs())) {
              self.$message.warning('计划开始时间不能早于当前时间');
              task.start_date = new Date(task[this.localFieldMapping.start_date_property || 'start_date']);
              task.end_date = dayjs(task.start_date).add(task.duration_show, 'hour').toDate();
              setTimeout(() => {
                self.ganttInstance.refreshTask(id);
              }, 0);
              return true;
            }
            this.$emit('taskAdjust', self.getCallbackParams(id), self.ganttInstance.undo);
            this.isTaskDrag = true;
            this.dragTaskId = id;
            return true;
          },
          onGridHeaderClick: (name, e) => {
            if (e.target.id == 'collGantt' || e.target.id == 'collGanttIcon') {
              self.isShowMore = false;
              self.ganttInstance.config.grid_width = 320;
              self.refreshGanttData(self.data);
            } else if (e.target.id == 'expandGantt' || e.target.id == 'expandGanttIcon') {
              self.isShowMore = true;
              self.refreshGanttData(self.data);
            }
          },
          onGridResizeEnd: () => {
            return self.gridResize;
          },
        }
      });

      // 配置缩放级别
      self.ganttInstance.ext.zoom.init({
        levels: [
          {
            name: 'month',
            scale_height: 40,
            min_column_width: 88,
            scales: [
              {
                unit: 'year',
                format: function (date) {
                  const yearToStr = self.ganttInstance.date.date_to_str('%Y');
                  return yearToStr(date) + '年';
                },
                step: 1
              },
              { unit: 'month', format: '%M', step: 1 }
            ]
          },
          {
            name: 'day',
            scale_height: 40,
            min_column_width: 88,
            scales: [
              {
                unit: 'month',
                format: function (date) {
                  const yearToStr = self.ganttInstance.date.date_to_str('%Y');
                  const monthToStr = self.ganttInstance.date.date_to_str('%M');
                  return yearToStr(date) + '年' + monthToStr(date);
                },
                step: 1
              },
              { unit: 'day', format: '%j日', step: 1 }
            ]
          },
          {
            name: 'hour',
            scale_height: 40,
            min_column_width: 88,
            scales: [
              {
                unit: 'day', // 第一层： 按天显示
                format: function (date) {
                  const dayToStr = self.ganttInstance.date.date_to_str('%j');
                  const monthToStr = self.ganttInstance.date.date_to_str('%M');
                  const yearToStr = self.ganttInstance.date.date_to_str('%Y');
                  return yearToStr(date) + '年' + monthToStr(date) + dayToStr(date) + '日';
                },
                step: 1
              },
              { unit: 'hour', format: '%H:%i', step: 1 }, // 第二层：按小时显示
              // { unit: 'minute', format: '%i', step: 30 } // 第三层：按分钟显示
            ]
          },
          {
            name: 'minute',
            scale_height: 40,
            min_column_width: 88,
            scales: [
              {
                unit: 'hour',
                format: function (date) {
                  const hourToStr = self.ganttInstance.date.date_to_str('%H');
                  const dayToStr = self.ganttInstance.date.date_to_str('%j');
                  const monthToStr = self.ganttInstance.date.date_to_str('%M');
                  const yearToStr = self.ganttInstance.date.date_to_str('%Y');
                  return yearToStr(date) + '年' + monthToStr(date) + dayToStr(date) + '日' + hourToStr(date) + '时';
                },
                step: 1
              },
              { unit: 'minute', format: '%i', step: 10 }
            ]
          }
        ],
        // trigger: 'wheel', // 触发方式为鼠标滚轮
        element: function () {
          return self.ganttInstance?.$root.querySelector('.gantt_task');
        }
      });
      self.ganttInstance.ext.zoom.setLevel(this.scale);
      // self.ganttInstance.ext.inlineEditors.setMapping({
      //   init: function(inlineEditors){
      //     self.ganttInstance.attachEvent("onTaskDblClick", function (id, e) {
      //       const task = this.ganttInstance.getTask(id);
      //       if (this.isParent(item) || !this.channelEditable(task)) {
      //         return true;
      //       }
      //       var cell = inlineEditors.locateCell(e.target);
      //       if (cell && inlineEditors.getEditorConfig(cell.columnName)) {
      //         inlineEditors.startEdit(cell.id, cell.columnName);
      //         return false;
      //       }
      //       return true;
      //     });
      //     self.ganttInstance.attachEvent("onEmptyClick", function () {
      //       inlineEditors.hide();
      //       return true;
      //     });
      //   },
      // });

      // 分组行聚合展示子任务
      self.handleSubTasks(self.ganttInstance);

      // 主任务聚合展示子任务
      self.showSubTask && self.handleSubTasks4MainTask(self.ganttInstance);

      // 延期边框
      self.showDelay && self.handleDelayLine(self.ganttInstance);
      // 任务展示实际时间线
      self.showActualLine && self.handleActualLine(self.ganttInstance);
      // 扣掉午休时间
      self.showLunchhour && self.handleLunchhourTasks(self.ganttInstance);
      // 抠掉夜间休息时间
      self.handleNightRestTasks(self.ganttInstance);
      // self.ganttInstance.message({
      //   text: '使用鼠标滚轮控制缩放级别',
      //   expire: 10000
      // });
      // gantt.ext.inlineEditors.attachEvent("onSave", function(state){
      //   var col = state.columnName;
      //   if(gantt.autoSchedule && (col == "start_date" || col == "end_date" || col == "duration")){
      //     gantt.autoSchedule();
      //   }
      // });
      self.refreshGanttData(self.data);
    },
    /**
     * 抠掉午休时间
     * @param {*} gantt
     */
    handleLunchhourTasks(gantt) {
      const self = this;
      gantt.addTaskLayer((task) => {
        if (task.restFlag !== '1') {
          return false;
        }
        // 休息时间:开始时间小于午休结束时间扣掉午休时间；结束时间大于晚休开始时间扣掉晚休时间
        if (self.isIncludeExtraTime(task)) {
          const rest_height = gantt.config.bar_height;
          const el = document.createElement('div');
          // 跨天
          if (isTimeSpanCrossDay(task.start_date, task.end_date) && isBeforeLimitTime(task.start_date, workTimeEndAM) && (task.extraTimeAm / 60) > 0 ) { // 不跨天并且开始时间小于午休结束时间扣掉午休时间
            // if(new Date(task.end_date).getHours() === 0)
            const sizes = gantt.getTaskPosition(task, this.getLunchStartTime(task.start_date), this.getLunchEndTime(task.start_date, (task.extraTimeAm / 60)));
            let child_el;
            child_el = this.createBox({
              height: rest_height,
              top: sizes.top,
              left: sizes.left,
              width: sizes.width
            }, 'rest_task');
            el.appendChild(child_el);
          }
          //非跨天
          if (!isTimeSpanCrossDay(task.start_date, task.end_date) && isBeforeLimitTime(task.start_date, workTimeEndAM) && (task.extraTimeAm / 60) > 0 ) { // 不跨天并且开始时间小于午休结束时间扣掉午休时间
            // if(new Date(task.end_date).getHours() === 0)
            const sizes = gantt.getTaskPosition(task, this.getLunchStartTime(task.start_date), this.getLunchEndTime(task.start_date, (task.extraTimeAm / 60)));
            let child_el;
            child_el = this.createBox({
              height: rest_height,
              top: sizes.top,
              left: sizes.left,
              width: sizes.width
            }, 'rest_task');
            el.appendChild(child_el);
          }
          if ((task.extraTimePm / 60) > 0) { // 包含晚休时间
            if (isTimeSpanCrossDay(task.start_date, task.end_date)) { // 跨天
              // if (isBeforeLimitTime(task.start_date, workTimeStartPM) && task.extraTimePm > 0) { // 开始时间小于晚休开始时间结束时间大于晚休开始时间扣掉晚休时间
              if (isBeforeLimitTime(task.start_date, workTimeStartPM)) { // 开始时间小于晚休开始时间
                const sizes = gantt.getTaskPosition(task, this.getDinnerStartTime(task.start_date), this.getDinnerEndTime(task.start_date, (task.extraTimePm / 60)));
                let child_el;
                child_el = this.createBox({
                  height: rest_height,
                  top: sizes.top,
                  left: sizes.left,
                  width: sizes.width
                }, 'rest_task');
                el.appendChild(child_el);
              } else {
                const sizes = gantt.getTaskPosition(task, this.getDinnerStartTime(task.end_date), this.getDinnerEndTime(task.end_date, (task.extraTimePm / 60)));
                let child_el;
                child_el = this.createBox({
                  height: rest_height,
                  top: sizes.top,
                  left: sizes.left,
                  width: sizes.width
                }, 'rest_task');
                el.appendChild(child_el);
              }
            } else {
              if (isBeforeLimitTime(task.start_date, workTimeStartPM)) { // 开始时间小于晚休开始时间
                const sizes = gantt.getTaskPosition(task, this.getDinnerStartTime(task.start_date), this.getDinnerEndTime(task.start_date, (task.extraTimePm / 60)));
                let child_el;
                child_el = this.createBox({
                  height: rest_height,
                  top: sizes.top,
                  left: sizes.left,
                  width: sizes.width
                }, 'rest_task');
                el.appendChild(child_el);
              }
            }
          }
          return el;
        }
        return false;
      });
    },
    /**
     * 抠掉夜间休息时间
     */
    handleNightRestTasks(gantt) {
      gantt.addTaskLayer((task) => {
        const rest_height = gantt.config.bar_height;
        const el = document.createElement('div');
        if(task.extraWorkTime > 0) {
          const sizes = gantt.getTaskPosition(task, this.getSleepStartTime(task.start_date), this.getSleepEndTime(task.start_date, (task.extraWorkTime / 60)));
          let child_el;
          child_el = this.createBox({
            height: rest_height,
            top: sizes.top,
            left: sizes.left,
            width: sizes.width
          }, 'rest_task');
          el.appendChild(child_el);
        }
        return el;
      })
    },
    // 获取当天午休开始时间
    getLunchStartTime(dateTime) {
      // debugger
      // 取workTimeStartAM的整数部分作为小时
      const hours = Math.floor(workTimeStartAM);
      // 取workTimeStartAM的小数部分，乘以60作为分钟
      const minutes = Math.round((workTimeStartAM - hours) * 60);
      const time = new Date(dateTime).setHours(hours, minutes, 0, 0);
      return time;
    },
    // 获取当天午休结束时间
    getLunchEndTime(dateTime, extraTime) {
      const time = new Date(dateTime).setHours(workTimeStartAM + extraTime, 0, 0, 0);
      return time;
    },
    // 获取当天晚休开始时间
    getDinnerStartTime(dateTime) {
      const time = new Date(dateTime).setHours(workTimeStartPM, 0, 0, 0);
      return time;
    },
    // 获取当天晚休结束时间
    getDinnerEndTime(dateTime, extraTime) {
      const time = new Date(dateTime).setHours(workTimeStartPM + extraTime, 0, 0, 0);
      return time;
    },
    //获取当天夜间休息开始时间
    getSleepStartTime(dateTime) {
      const time = new Date(dateTime).setHours(sleepStartPM, 0, 0, 0);
      return time;
    },
    //获取当天夜间休息开始时间
    getSleepEndTime(dateTime, extraTime) {
      const time = new Date(dateTime).setHours(sleepStartPM + extraTime, 0, 0, 0);
      return time;
    },
    /**
     * 分组行聚合展示子任务
     */
    handleSubTasks(gantt) {
      gantt.addTaskLayer((task) => {
        // 委托聚合展示连续的色块；设备聚合展示不连续的色块
        if (gantt.hasChild(task.id)) {
          const sub_height = gantt.config.bar_height;
          const el = document.createElement('div');
          const sizes = gantt.getTaskPosition(task);

          const sub_tasks = gantt.getChildren(task.id);
          let child_el;

          for (let i = 0; i < sub_tasks.length; i++) {
            const child = gantt.getTask(sub_tasks[i]);
            const child_sizes = gantt.getTaskPosition(child);

            child_el = this.createBox({
              height: sub_height,
              top: sizes.top,
              left: child_sizes.left,
              width: child_sizes.width
            }, 'child_preview gantt_task_line');
            el.appendChild(child_el);
          }
          return el;
        }
        return false;
      });
    },
    createBox(sizes, class_name) {
      const box = document.createElement('div');
      box.style.cssText = [
        'height:' + sizes.height + 'px',
        'line-height:' + sizes.height + 'px',
        'width:' + sizes.width + 'px',
        'top:' + sizes.top + 'px',
        'left:' + sizes.left + 'px',
        'position:absolute'
      ].join(';');
      box.className = class_name;
      return box;
    },
    /**
     * 主任务聚合展示子任务
     */
    handleSubTasks4MainTask(gantt) {
      gantt.addTaskLayer((task) => {
        let cl = 'task_actual_line';
        if (task.group) {
          cl = `task_actual_line-${task.group}`;
        }
        if (this.isMainTask(task)) {
          const text = this.getTaskText(task);
          const sub_height = gantt.config.bar_height;
          const el = document.createElement('div');
          const sizes = gantt.getTaskPosition(task);
          // 背景
          el.appendChild(this.createBox({
            height: sub_height,
            top: sizes.top,
            left: sizes.left,
            width: sizes.width
          }, `task-main-bg`));

          const sub_tasks = task.sub_task_list;
          let child_el;

          for (let i = 0; i < sub_tasks.length; i++) {
            const child = sub_tasks[i];

            // 计划
            const startDate = this.formatDate(child[this.localFieldMapping.start_date_property || 'start_date']);
            const endDate = this.formatDate(child[this.localFieldMapping.end_date_property || 'end_date']);
            const startX = gantt.posFromDate(new Date(startDate));
            const endX = gantt.posFromDate(new Date(endDate));

            child_el = this.createBox({
              height: sub_height,
              top: sizes.top,
              left: startX,
              width: endX - startX
            }, `task-group-actual task-group-actual-${child.group}`);
            child_el.innerHTML = `<div class="gantt_task_content"><div class="task-content task-actual-line"><div class="task-text">${sub_tasks[i].spaceCost}'颗',${text}</div></div></div>`;
            el.appendChild(child_el);

            // 实际
            const actualStartDate = this.formatDate(child[this.localFieldMapping.actual_start_date_property || 'actual_start_date']);
            const actualEndDate = this.formatDate(child[this.localFieldMapping.actual_end_date_property || 'actual_end_date']);
            if (actualStartDate && actualEndDate) {
              const sub_height = 6;
              const startX = gantt.posFromDate(new Date(actualStartDate));
              const endX = gantt.posFromDate(new Date(actualEndDate));

              child_el = this.createBox({
                height: sub_height,
                top: sizes.top + 33,
                left: startX,
                width: endX - startX
              }, 'task_actual_line gantt_task_line ' + cl);
              el.appendChild(child_el);
            }
          }
          return el;
        }
        return false;
      });
    },
    /**
     * 任务展示实际时间线
     */
    handleActualLine(gantt) {
      gantt.addTaskLayer((task) => {
        let cl = 'task_actual_line';
        if (task.group) {
          cl = `task_actual_line-${task.group}`;
        }
        if (task.operateTimeList && task.operateTimeList.length > 0) { // 多个实际条
          const sub_height = 6;
          const el = document.createElement('div');
          const l = task.operateTimeList;
          for (let i = 0; i < l.length; i++) {
            const sub = l[i];
            const sizes = gantt.getTaskPosition(task, new Date(sub.mountTime), new Date(sub.dismountTime));
            const child_el = this.createBox({
              height: sub_height,
              top: sizes.top + 33,
              left: sizes.left,
              width: sizes.width
            }, 'task_actual_line gantt_task_line ' + cl);
            el.appendChild(child_el);
          }
          return el;
        } else if (!gantt.hasChild(task.id) && task.actual_start_date && task.actual_end_date) { // 子任务
          const sub_height = 6;
          const el = document.createElement('div');
          const sizes = gantt.getTaskPosition(task, new Date(task.actual_start_date), new Date(task.actual_end_date));

          const child_el = this.createBox({
            height: sub_height,
            top: sizes.top + 33,
            left: sizes.left,
            width: sizes.width
          }, 'task_actual_line gantt_task_line ' + cl);
          el.appendChild(child_el);

          return el;
        }
        return false;
      });
    },
    /**
     * 延期
     * @param {*} gantt
     */
    handleDelayLine(gantt) {
      gantt.addTaskLayer((task) => {
        const actStart = task.actual_start_date;
        const actEnd = task.actual_end_date;
        const pStart = task.start_date;
        const pEnd = task.end_date;
        let start = task.start_date;
        let end = task.end_date;
        // 存在实际条
        if (!gantt.hasChild(task.id) && task.actual_start_date && task.actual_end_date && task.extensionDays > 0) {
          if (new Date(actEnd).getTime() > new Date(pStart).getTime) { // 实际条在计划条左侧
            start = actStart;
          } else if (new Date(actStart).getTime() > new Date(pEnd).getTime()) { // 实际条在计划条右侧
            end = actEnd;
          } else { // 实际条和计划条重叠
            start = Math.min(new Date(actStart).getTime(), new Date(pStart).getTime());
            end = Math.max(new Date(actEnd).getTime(), new Date(pEnd).getTime());
          }
          const sub_height = 40;
          const el = document.createElement('div');
          // const elText = document.createElement('span');
          const sizes = gantt.getTaskPosition(task, new Date(start), new Date(end));

          const child_el = this.createBox({
            height: sub_height,
            top: sizes.top,
            left: sizes.left,
            width: sizes.width + 2
          }, 'task_actual_delay_box');
          const text = `<span style="position: absolute;top: ${sizes.top}px;line-height:${sub_height}px;left: ${sizes.left + sizes.width}px;color:#FF4D4F;margin-left:4px;">延期${task.extensionDays}天</span>`;
          // elText.innerHTML = text;
          el.innerHTML = text;
          el.appendChild(child_el);

          return el;
        }
        return false;
      });
    },
    refreshGanttData(data) {
      const self = this;
      if (!self.ganttInstance) return;
      self.ganttInstance.hideQuickInfo();
      self.ganttInstance.config.show_quick_info = false;
      self.ganttInstance.config.grid_width = this.calcGridWidth();
      // self.ganttInstance.config.grid_elastic_columns = false;
      // self.ganttInstance.config.grid_resize = false;
      self.ganttInstance.config.grid_resize = false;
      self.ganttInstance.config.columns = this.getColumns();
      self.ganttInstance.batchUpdate(() => {
        self.ganttInstance.clearAll();
        self.ganttInstance.parse(this.formatData(data));
        // 当前时间线
        const current = dayjs().toDate();
        self.ganttInstance.addMarker({
          start_date: current,
          css: 'current-line',
          text: '当前',
          title: dayjs(current).format('YYYY-MM-DD HH:mm')
        });
      });
      // 处理复选框勾选事件
      self.ganttInstance?.$root.removeEventListener('click', self.handleCheckEvent);
      self.showCheckbox && self.ganttInstance?.$root.addEventListener('click', self.handleCheckEvent);
      // 任务可拖动时间范围线
      if (self.rangeLines?.startLine && self.rangeLines.startLine !== '') {
        self.ganttInstance.addMarker({
          start_date: new Date(self.rangeLines.startLine),
          css: 'rang-line',
          text: 'start',
          title: 'start' + self.rangeLines.startLine,
        });
      }
      if (self.rangeLines?.endLine && self.rangeLines.endLine !== '') {
        self.ganttInstance.addMarker({
          start_date: new Date(self.rangeLines.endLine),
          css: 'rang-line',
          text: 'end',
          title: 'end' + self.rangeLines.endLine,
        });
      }
      // 刷新数据后滚动条滚动到上次拖动的任务位置
      if (this.isTaskDrag && this.dragTaskId !== '') {
        const task = self.ganttInstance?.getTask(this.dragTaskId);
        this.isTaskDrag = false;
        this.dragTaskId = '';
        if (task) {
          const sizes = self.ganttInstance.getTaskPosition(task);
          self.ganttInstance.scrollTo(sizes.left, sizes.top);
        }
      }
    },
    calcGridWidth() {
      let width = 360;
      if (this.showCheckbox) {
        width += 40;
      }
      if (this.channelEditable) {
        // width += 160;
        width += 320;
      }
      return width;
    },
    getColumns() {
      const self = this;
      let expandLabel = `<div style='text-align:left;'>${this.groupLabel}信息</div>`;
      if (self.moreColumn) { // 展示更多
        expandLabel = `<div style='display:flex'><span style='flex-grow: 2;text-align:left;'>${this.groupLabel}信息</span><span id='expandGantt' style='width:34px;text-align: center;cursor: pointer;'><i id='expandGanttIcon' class="lims-icon-xianxing_shuangjiantou_you"></i></span></div>`;
      }
      return [
        ...(this.showCheckbox
? [{
  name: 'selector',
  label: `<input type='checkbox' id='selectAll'>`,
  align: 'center',
  width: 40,
  max_width: 40,
  template: item => {
    return `<input type="checkbox" class="gantt_row_selector" data-id="${item.id}" ${item.selected ? 'checked' : ''}>`;
  }
}]
: []),
        {
          name: 'text',
          label: self.isShowMore ? `<div style='text-align:left;'>${this.groupLabel}信息</div>` : expandLabel,
          tree: true,
          align: 'left',
          width: '*',
          min_width: 320,
          template: item => {
            const defaultValue = '-';
            if (this.isChannel(item) && this.groupByEquip && item.groupType === 'channel') { // 通道展示内容
              return `<div class="equip-info-cell">
                        <div class="equip-info-cell__left">
                          <div>${item.channelNo || defaultValue}</div>
                        </div>
                        <div class="equip-info-cell-capcity__right">通道容量:${item.channelCapacity || defaultValue}颗</div>
                      </div>`;
            } else if (this.isParent(item) && this.groupByEquip && item.groupType === 'device') {
              return `<div class="equip-info-cell">
                        <div class="equip-info-cell__left">
                          <div>${item.deviceId != '99999999' ? (item.deviceNum || defaultValue) : ''}</div>
                          <div>${item.deviceName || defaultValue}</div>
                        </div>
                        <div class="equip-info-cell__right">${item.locationName || defaultValue}</div>
                        <div class="equip-info-cell-capcity__right">设备容量:${item.capacity || defaultValue}颗</div>
                      </div>`;
            } else if (this.isDeviceType(item) && this.groupByEquip && item.groupType === 'deviceType') {
              if (this.type === 'equip' && item.schFlag === '0') {
                return `<div class="equip-info-cell">
                          <div class="equip-info-cell__left">
                            <div>${item.deviceTypeName || defaultValue}</div>
                          </div>
                          <div class="equip-info-cell-icon__right">已禁用</div>
                      </div>`;
              } else {
                return `<div class="equip-info-cell">
                          <div class="equip-info-cell__left">
                            <div>${item.deviceTypeName || defaultValue}</div>
                          </div>
                      </div>`;
              }
            }
            if (this.isParent(item) && this.groupByComm) {
              return `<div class="comm-info-cell">
                        <div>${item.commissionNum || defaultValue}</div>
                      </div>`;
            } else if (this.isParent(item) && this.groupByItem) {
              return `<div class="comm-info-cell">
                        <div>${item.itemName || defaultValue}</div>
                      </div>`;
            }

            if (this.customField &&
              this.customField.main && this.customField.main.length > 0 ||
              this.customField.common && this.customField.common.length > 0) {
              let mainEl = `<span>`;
              this.customField.main?.map((field, index) => {
                mainEl += `${item[field.value] || defaultValue} | `;
              });
              mainEl += `</span>`;
              let commonEl = '';
              this.customField.common?.map((field, index) => {
                if (field.icon) {
                  commonEl += `<i class=${field.icon}></i>`;
                }
                commonEl += `<span>${item[field.value] || defaultValue}${field.unit ? field.unit : ''}</span>`;
              });
              return `<div class="task-info-cell">
                        ${mainEl}
                        ${commonEl}
                      </div>`;
            }

            return `<div class="task-info-cell">
                      <span>${item.inspectionItem || defaultValue}｜${item.itemName || defaultValue}</span>
                      <span>${item.sampleName || defaultValue}</span>
                      <i class="lims-icon-xiangqing-ceshizhouqi"></i><span>${item[this.localFieldMapping.start_date_property] !== '' ? (item.duration_show || defaultValue) : item[this.localFieldMapping.duration_show_property]}h</span>
                      <i class="lims-icon-yangpinshuliang"></i><span>${item.spaceCost || defaultValue}</span>
                      <i class="lims-icon-guigexinghao"></i><span>${item.specification || defaultValue}</span>
                      <span>${item.taskNo || defaultValue}</span>
                    </div>`;
          }
        },
        // ...(this.channelEditable?[{
        //   name: "equipChannelId",
        //   label: "设备通道",
        //   width:80,
        //   align: "center",
        //   resize: false,
        //   editor: {
        //     type: "select",
        //     map_to: "equipChannelId",
        //     options:self.channelOptions,
        //   },
        //   template: task=>{
        //     console.log('self.channelOptions',self.channelOptions)
        //     var value = task.equipChannelId;
        //     var list = self.channelOptions;
        //     for(var i = 0; i < list.length; i++){
        //       if(list[i].key == value){
        //         return list[i].label;
        //       }
        //     }
        //     return "";
        //   },
        // }]:[]),
        ...(this.channelEditable ? [{
          name: 'equipChannelId',
          label: '设备通道',
          align: 'center',
          width: 160,
          resize: false,
          template: item => {
            if (this.isParent(item) || !this.channelEditable(item)) {
              return '';
            }
            const selectId = 'gantt-channel-select_' + item.id;
            const optionsHtml = this.channelOptions.map(opt =>
              `<el-option value="${opt.value}" label="${opt.label}"></el-option>`
            ).join('');

            // 使用data属性存储值，而不是Vue实例
            return `<div id="${selectId}" class="gantt-channel-select" data-task-id="${item.id}" data-value="${item.equipChannelId || ''}">
                      <select onchange="handleChannelChange(event)" style="color:#606266; width: 100%; height: 32px; border:1px solid #DCDFE6; border-radius: 5px">
                        <option value="">请选择</option>
                        ${this.channelOptions.map(opt =>
              `<option value="${opt.value}" ${item.equipChannelId === opt.value ? 'selected' : ''}>${opt.label}</option>`
            ).join('')}
                      </select>
                    </div>`;
            // setTimeout(() => {
            //   new Vue({
            //     el: '#' + selectId,
            //     data: {
            //       value: item.equipChannelId,
            //       options: self.channelOptions
            //     },
            //     methods: {
            //       onChange(val) {
            //         console.log('onChange', self.channelOptions);
            //         const opt = self.channelOptions.find(opt => opt.value === val);
            //         const data = self.getAllData();
            //         const task = data.find(d => d.id === item.id);
            //         task.equipChannelId = val;
            //         task.channelCapacity = opt?.channelCapacity;
            //         task.channelNo = opt?.no;
            //         const i = task.ids.split("|");
            //         const ids = [i[0], i[1], opt.id].join('|');
            //         task.ids = ids;
            //         self.$emit('channel-change', self.getGrattData(data));
            //       }
            //     }
            //   });
            // }, 0);
            // return `<div id="${selectId}" class="gantt-channel-select">
            //           <el-select v-model="value" size="small" @change="onChange" :disabled="options&&options.length>0?false:true">
            //             <el-option v-for='item in options' :key='item.value' :label='item.label' :value='item.value'></el-option>
            //           </el-select>
            //         </div>`;
          }
        }] : []),
        ...(this.isShowMore ? this.moreColumns : [])
      ];
    },
    getTaskText(task) {
      if (task.sub_task_list?.length > 0) {
        return [[task.duration_show, 'h'], [task.conditions, '']].filter(d => this.isNotEmpty(d[0])).map(d => `${d[0]}${d[1]}`).join('｜');
      } else {
        return [[task.spaceCost, '颗'], [task.duration_show, 'h'], [task.conditions, '']].filter(d => this.isNotEmpty(d[0])).map(d => `${d[0]}${d[1]}`).join('｜');
      }
    },
    /**
     * 获取当前任务关联的设备或通道
     */
    getSelectDervice() {
      if (this.currTaskDeviceId !== undefined) {
        return this.currTaskDeviceId ? [this.currTaskDeviceId] : [];
      }
      const curDeviceId = [];
      if (this.currTask.deviceId) curDeviceId.push(this.currTask.deviceId);
      if (this.currTask.equipChannelId) curDeviceId.push(this.currTask.equipChannelId);
      return curDeviceId;
    },
    /**
     * 处理复选框勾选事件
     */
    handleCheckEvent(e) {
      // 处理表头全选
      if (e.target.id === 'selectAll') {
        this.handleSelectAll(e.target.checked);
      }
      // 处理行数据勾选
      if (e.target.classList.contains('gantt_row_selector')) {
        this.handleRowCheckboxClick(e.target);
        this.updateSelectAllCheckbox();
      }
    },
    /**
     * 全选处理
     */
    handleSelectAll(isChecked) {
      this.ganttInstance.eachTask(task => {
        task.selected = isChecked;
        task.indeterminate = false;
        this.ganttInstance.updateTask(task.id);
        this.setHalfChecked(task.id, false);
      });
    },
    /**
     * 设置半选
     */
    setHalfChecked(id, val) {
      this.$nextTick(() => {
        const checkbox = this.ganttInstance?.$root?.querySelector(`.gantt_row_selector[data-id="${id}"]`);
        checkbox && (checkbox.indeterminate = val);
      });
    },
    /**
     * 行复选框点击处理
     */
    handleRowCheckboxClick(checkbox) {
      const taskId = checkbox.getAttribute('data-id');
      const isChecked = checkbox.checked;

      const task = this.ganttInstance.getTask(taskId);
      task.selected = isChecked;
      task.indeterminate = false;
      this.ganttInstance.updateTask(taskId);
      this.setHalfChecked(taskId, false);

      // 级联处理
      this.updateChildrenSelection(taskId, isChecked);
      this.updateParentSelection(task.parent);
    },
    /**
     * 更新表头全选状态
     */
    updateSelectAllCheckbox() {
      const allTasks = this.ganttInstance.getTaskByTime();
      const selectAllCheckbox = this.ganttInstance?.$root.querySelector('#selectAll');

      if (allTasks.every(task => task.selected)) {
        selectAllCheckbox.checked = true;
        selectAllCheckbox.indeterminate = false;
      } else if (allTasks.some(task => task.selected || task.indeterminate)) {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = true;
      } else {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = false;
      }
    },
    /**
     * 更新子节点选择状态
     */
    updateChildrenSelection(parentId, isSelected) {
      const children = this.ganttInstance.getChildren(parentId);
      children.forEach(childId => {
        const child = this.ganttInstance.getTask(childId);
        child.selected = isSelected;
        child.indeterminate = false;
        this.ganttInstance.updateTask(childId);
        this.setHalfChecked(childId, false);
        this.updateChildrenSelection(childId, isSelected);
      });
    },
    /**
     * 更新父节点选择状态
     */
    updateParentSelection(taskId) {
      if (!taskId) return;

      const parent = this.ganttInstance.getTask(taskId);
      const children = this.ganttInstance.getChildren(taskId);

      const allSelected = children.every(id => this.ganttInstance.getTask(id).selected);
      const anySelected = children.some(id => this.ganttInstance.getTask(id).selected);

      if (allSelected) {
        parent.selected = true;
        parent.indeterminate = false;
      } else if (!anySelected) {
        parent.selected = false;
        parent.indeterminate = false;
      } else {
        parent.selected = false;
        parent.indeterminate = true;
      }

      this.ganttInstance.updateTask(taskId);
      this.setHalfChecked(taskId, parent.indeterminate);
      this.updateParentSelection(parent.parent);
    },
    /**
     * 获取所有选中任务
     */
    getSelectedTasks() {
      return this.getGrattData(this.ganttInstance.getTaskByTime().filter(task => task.selected || task.indeterminate));
    },
    isDeviceType(item) {
      return item.type === 'project' && item.parent === 0;
    },
    isParent(item) {
      return item.type === 'project';
    },
    isChannel(item) {
      return item.type === 'project' && item.parent !== 0;
    },
    isPreScheduling(item) {
      return item.group === 'recommend';
    },
    isConflict(item) {
      return item.conflictFlag;
    },
    isMainTask(item) {
      return item.type === 'task' && item.sub_task_list?.length;
    },
    isIncludeExtraTime(item) {
      return (item.extraTimeAm / 60) > 0 || (item.extraTimePm / 60) > 0;
    },
    getAllData() {
      const self = this;
      const { data } = self.ganttInstance.serialize();
      return data;
    },
    getGrattData(data = this.getAllData()) {
      const self = this;
      const onlyTasks = data.filter((item) => !self.isParent(item) && !item.ignore_flag);
      return self.restoreFields(onlyTasks);
    },
    getTaskData(id) {
      const self = this;
      const task = self.ganttInstance.getTask(id);
      return self.restoreFields([task])[0];
    },
    getChildren(id) {
      return this.data.filter(d => d[this.localFieldMapping.parent_property || 'parent'] === id);
    },
    getCallbackParams(id) {
      const self = this;
      return { task: self.getTaskData(id), data: self.getGrattData() };
    },
    unCheckPreTask() {
      this.showPreTask = [];
    },
    isNotEmpty(value) {
      return value || value === 0;
    },
    onStartTimeChange(val) {
      const self = this;
      const task = self.ganttInstance.getTask(this.currTaskId);
      if (dayjs(new Date(val)).isBefore(dayjs())) {
        self.$message.warning('计划开始时间不能早于当前时间');
        this.currTask.start_date = task[this.localFieldMapping.start_date_property || 'start_date'];
        return;
      }
      task.start_date = new Date(val);
      task.end_date = dayjs(task.start_date).add(task.duration_show, 'hour').toDate();
      self.ganttInstance.updateTask(this.currTaskId);
      this.$nextTick(() => {
        this.$emit('taskAdjust', this.getCallbackParams(this.currTaskId));
      });
    },
    formatDate(str, format = this.dateValueFormat) {
      if (!str) {
        return null;
      }
      return dayjs(str).format(format);
    },
    formatData(data) {
      const self = this;
      const obj = {};
      if (this.fieldMappingDisabled) {
        obj.data = data;
      } else {
        obj.data = data.map(item => {
          const newItem = {
            ...item
          };
          newItem.id = item[this.localFieldMapping.id_property || 'id'];
          newItem.text = item[this.localFieldMapping.text_property || 'text'];
          newItem.start_date = this.formatDate(item[this.localFieldMapping.start_date_property || 'start_date']);
          newItem.end_date = this.formatDate(item[this.localFieldMapping.end_date_property || 'end_date']);
          // if (self.showLunchhour && self.isIncludeExtraTime(item)) {
          //   newItem.end_date = this.formatDate(this.getEndtimeAddRest(this.formatDate(item[this.localFieldMapping.end_date_property || 'end_date']), item['extraTime']));
          // } else {
          // newItem.end_date = this.formatDate(item[this.localFieldMapping.end_date_property || 'end_date']);
          // }
          newItem.duration = item[this.localFieldMapping.duration_property || 'duration'];
          newItem.duration_show = item[this.localFieldMapping.duration_show_property || 'duration'];
          newItem.progress = item[this.localFieldMapping.progress_property || 'progress'];
          newItem.parent = item[this.localFieldMapping.parent_property || 'parent'];
          newItem.type = item[this.localFieldMapping.type_property || 'type'];
          newItem.ignore_flag = item[this.localFieldMapping.ignore_flag_property || 'ignore_flag'];
          newItem.combine_sign_flag = item[this.localFieldMapping.combine_sign_flag_property || 'combine_sign_flag'];
          newItem.actual_start_date = this.formatDate(item[this.localFieldMapping.actual_start_date_property || 'actual_start_date'], this.dateActulFormat);
          newItem.actual_end_date = this.formatDate(item[this.localFieldMapping.actual_end_date_property || 'actual_end_date'], this.dateActulFormat);
          newItem.sub_task_list = item[this.localFieldMapping.sub_task_list_property || 'sub_task_list'];
          newItem.channelNo = item[this.localFieldMapping.channelNo_property || 'channelNo'];
          newItem.channelCapacity = item[this.localFieldMapping.channelCapacity_property || 'channelCapacity'];
          return newItem;
        });
      }
      if (this.links?.length) {
        obj.links = this.links;
      }
      return obj;
    },
    getEndtimeAddRest(time, rest = 2) {
      const date = new Date(time); // 当前时间
      date.setHours(date.getHours() + rest); // 将时间加2小时
      return date;
    },
    restoreFields(data) {
      const self = this;
      if (!self.fieldMappingDisabled) { // 还原字段映射
        data = data.map(item => {
          const newItem = Object.keys(item).filter(key => !key.startsWith('$')).reduce((acc, key) => {
            acc[key] = item[key];
            return acc;
          }, {});
          Object.keys(self.localFieldMapping).forEach(key => {
            const sourceKey = key.replace('_property', '');
            if (self.localFieldMapping[key] && self.localFieldMapping[key] !== sourceKey) {
              if (sourceKey !== 'duration') { // 映射后 甘特图会根据end_date重新计算周期，周期不变
                newItem[self.localFieldMapping[key]] = item[sourceKey];
                if ((sourceKey === 'start_date' || sourceKey === 'end_date') && typeof newItem[self.localFieldMapping[key]] === 'object') {
                  newItem[self.localFieldMapping[key]] = this.formatDate(newItem[self.localFieldMapping[key]]);
                }
              }
              // if(sourceKey === 'duration' || sourceKey === 'duration_show') { // 映射后 甘特图会根据end_date重新计算周期，周期不变
              //   newItem[self.localFieldMapping[key]] = item.duration_show;
              // }
              delete newItem[sourceKey];
            }
          });
          return newItem;
        });
      }
      return data;
    },

    onErrorTasks(tasks) {
      this.errorTasks = tasks;
      this.ganttInstance?.render();
    }
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
// .task-gantt-container {
//   width: 100%;
//   height: 100%;
//   display: flex;
//   align-items: stretch;
//   flex-direction: column;
//   overflow: hidden;
.task-gantt {
  height: 100%;
  --dhx-gantt-base-colors-select: #F7FAFF;
  --dhx-gantt-base-colors-hover-color: #F7FAFF;
  --dhx-gantt-grid-scale-background: #DCEEFE;
  --dhx-gantt-timeline-scale-background: #DCEEFE;
  --dhx-gantt-scale-border-horizontal: 1px solid #C7E3FD;
  --dhx-gantt-grid-scale-border-vertical: 1px solid #C7E3FD;
  --dhx-gantt-task-row-border: 1px solid #E9E9E9;
  --dhx-gantt-timeline-scale-border-vertical: 1px solid #E9E9E9;
  --dhx-gantt-default-border: 1px solid #E9E9E9;
  --dhx-gantt-grid-body-background: var(--c-whiteColor);
  --dhx-gantt-task-row-background: transparent;
  --dhx-gantt-task-row-background--odd: transparent;

  ::v-deep .gantt_container {
    border: none;
  }

  ::v-deep .gantt_resizer_x .gantt_resizer_x {
    background-image: linear-gradient(to right, rgba(0, 0, 0, 0.05) 0%, rgba(0, 0, 0, 0) 50%);
    background-size: 50% 100%;
    background-position: center right;
    background-repeat: no-repeat;
  }

  ::v-deep .gantt_grid_scale,
  ::v-deep .gantt_task_scale {
    border-bottom: 1px solid #93C9FB;
  }

  ::v-deep .gantt_task_scale {
    font-size: 12px;
    color: #1F1F1F;
  }

  ::v-deep .gantt_task .gantt_task_scale .gantt_scale_cell.gantt_last_cell {
    border-right-width: 0;
  }

  ::v-deep .gantt_task_vscroll {
    background-color: #DCEEFE;

    .gantt_layout_cell.gantt_ver_scroll {
      background-color: var(--c-whiteColor);
      border-top-color: #93C9FB;
    }
  }

  ::v-deep .gantt_layout_content>.gantt_task {
    background-color: var(--c-whiteColor);
    background-image: url("../img/bg-gantt-data-area.png");
    background-size: cover;
    background-position: right bottom;
    background-repeat: no-repeat;
  }

  ::v-deep .gantt_grid_head_cell {
    padding-left: 8px;
    // text-align: left;
    color: #1F1F1F;

    &:has(#selectAll) {
      padding-left: 0;
      text-align: center;
    }
  }

  ::v-deep .gantt_tree_indent {
    width: 4px;
  }

  ::v-deep .gantt_tree_icon.gantt_blank {
    width: 20px;
  }

  ::v-deep .gantt_task_link div.gantt_link_arrow {
    margin-top: -3px;
  }

  ::v-deep .gantt_line_wrapper {
    z-index: 1;
  }

  ::v-deep .gantt_layout_cell_border_right:not(.gantt_resizer):not(.gantt_resizing) {
    &::after {
      content: '';
      position: absolute;
      top: 0;
      right: -1px;
      width: 1px;
      height: 40px;
      background: #C7E3FD;
    }
  }
}

::v-deep .equip-info-cell {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666666;
  font-family: PingFangSC-Regular;
  font-weight: 400;

  .equip-info-cell__left {
    height: 32px;
    line-height: 16px;
    padding-left: 6px;
    background-image: linear-gradient(90deg, #2794f814 0%, #2794f800 95%, #2794f800 97%, #2794f800 97%);
    border-radius: 2px;
    border-left: 2px solid #2794F8;

    &>div:last-child {
      font-weight: 500;
      color: #1F1F1F;
    }
  }

  .equip-info-cell__right {
    margin-left: 36px;
  }

  .equip-info-cell-capcity__right {
    margin-left: 12px;
  }
  .equip-info-cell-icon__right {
    background-color: #FEEBEB;
    color: red;
    position: absolute;
    right: 4px;
    top: 0;
    line-height: 10px;
    padding: 8px 10px;
    border-radius: 3px;
  }
}

::v-deep .comm-info-cell {
  height: 100%;
  display: flex;
  align-items: center;
  font-family: PingFangSC-Regular;

  &>div {
    height: 26px;
    line-height: 26px;
    padding-left: 6px;
    background-image: linear-gradient(90deg, #2794f814 0%, #2794f800 95%, #2794f800 97%, #2794f800 97%);
    border-radius: 2px;
    border-left: 2px solid #2794F8;
    font-weight: 500;
    color: #1F1F1F;
  }
}

::v-deep .task-info-cell {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #999999;
  font-family: PingFangSC-Regular;
  font-weight: 400;

  &>*:first-child {
    font-size: 14px;
    color: #1F1F1F;
  }

  span+span,
  span+i {
    margin-left: 8px;
  }

  i+span {
    margin-left: 2px;
  }
}

::v-deep .gantt_cal_quick_info {
  width: 360px;
  height: fit-content;
  max-height: calc(100% - 10px);
  padding: 8px 0 0;
  background-image: url("../img/bg-quick-info.png");
  background-size: 100% auto;
  background-position: center top;
  background-repeat: no-repeat;
  box-shadow: 0 6px 16px 0 #00000014, 0 9px 28px 0 #0000000d, 0 12px 48px 16px #00000008;
  border-radius: 8px;

  * {
    background-color: transparent;
  }

  .gantt_cal_qi_title .gantt_cal_qi_tcontent {
    font-weight: 400;

    &::before {
      display: none;
    }
  }

  .gantt_cal_qi_tcontrols {
    padding-right: 10px;
  }

  .gantt_cal_qi_content {
    flex: 1;
    overflow: hidden;
    padding: 8px 16px 16px;
    display: flex;
    flex-direction: column;

    .gantt_quick_info__top {
      flex-shrink: 0;
      min-height: 57px;
      display: flex;
      align-items: center;
      background-image: url("../img/bg-quick-info-top.png");
      background-size: cover;
      background-position: left top;
      background-repeat: no-repeat;
    }

    .gantt_quick_info__logo {
      width: 52px;
      height: 52px;
      flex-shrink: 0;
      margin: 2px 10px 0 4px;
      background-image: url("../img/icon-quick-info-top.png");
      background-size: 100% auto;
      background-position: center;
      background-repeat: no-repeat;
    }

    .gantt_quick_info__bottom {
      flex: 1;
      margin-top: 16px;
      overflow: auto;

      &::-webkit-scrollbar {
        display: none;
      }

      &>div:not(:first-child) {
        margin-top: 16px;
      }
    }

    .gantt_quick_info__main {
      flex: 1;
      line-height: 20px;
      font-weight: 500;
      font-size: 14px;
      color: #1F1F1F;
      display: inline-block;
      word-wrap: break-word;
      overflow-wrap: anywhere;
      white-space: pre-wrap;
    }

    .gantt_quick_info__sub {
      line-height: 16px;
      font-weight: 400;
      font-size: 12px;
      color: #999999;
    }

    .gantt_quick_info__row+.gantt_quick_info__sub {
      margin-top: 4px;
      margin-bottom: 8px;
    }

    .gantt_quick_info__title {
      position: relative;
      padding-left: 8px;
      line-height: 20px;
      font-weight: 400;
      font-size: 14px;
      color: #1F1F1F;

      &:before {
        content: '';
        position: absolute;
        top: 4px;
        left: 0;
        width: 2px;
        height: 12px;
        background: #2794F8;
        border-radius: 1px;
      }
    }

    .gantt_quick_info__row {
      line-height: 20px;
      margin-top: 8px;
      color: #999999;
      font-size: 14px;
      display: flex;

      i {
        margin-right: 4px;
      }

      .gantt_quick_info__value {
        flex: 1;
        color: #666666;
        font-size: 14px;
        display: inline-block;
        word-wrap: break-word;
        overflow-wrap: anywhere;
        white-space: pre-wrap;
      }
    }
  }
}

@mixin task-group-common($color) {
  background-color: $color;
  border-radius: 2px;

  .task-sign span {
    color: $color;
  }
}

/* 定义不同的任务颜色 */
::v-deep .task-group-recommend {
  @include task-group-common(#FF7D00);
}

::v-deep .task-group-scheduled {
  @include task-group-common(#2794F8);
}

::v-deep .task-group-running {
  @include task-group-common(#51BD1C);
}

::v-deep .task-group-finished {
  @include task-group-common(#999999);
}
::v-deep .task-group-pre {
  @include task-group-common(#666666);
}

@mixin task-group-actual-common($color, $colorBg) {
  border-left: 2px solid $color;
  background-color: $colorBg;
  border-radius: 2px;

  .gantt_task_content {
    color: $color;
  }
}

::v-deep .task-group-actual-recommend {
  @include task-group-actual-common(#FF7D00, #FFF2E5);
}

::v-deep .task-group-actual-scheduled {
  @include task-group-actual-common(#2794F8, #E9F4FE);
}

::v-deep .task-group-actual-running {
  @include task-group-actual-common(#51BD1C, #EBF7E8);
}

::v-deep .task-group-actual-finished {
  @include task-group-actual-common(#999999, #F4F4F4);
}

::v-deep .task-group-actual {
  height: 31px !important;
  line-height: 31px !important;
  // margin-top: 8px;
  // border-left: 2px solid #2794F8;
  // @include task-group-common(#E9F4FE);
  // .gantt_task_content {
  //   color: #2794F8;
  // }
}

::v-deep .task-group-conflict {
  border: 1px solid #FFFFFF;
  outline: 2px dashed #EF0505;
}

::v-deep .task-group-delay {
  outline: 1px solid #FF4D4F;
}

::v-deep .task-group-parent {
  background-color: transparent;
  border-radius: 0;

  &.is-empty {
    width: 0 !important;
  }
}

::v-deep .task-group-show-sub {
  background-color: transparent;
  //border: 1px solid #A1A4A6;
}

::v-deep .task-main-bg {
  background: #f5f5f5;
}

::v-deep .task_actual_line {
  background: #2794F8;
  border-radius: 3px;
}

@mixin task_actual_line-common($color) {
  background: $color;
}

::v-deep .task_actual_line-recommend {
  @include task_actual_line-common(#FF7D00);
}

::v-deep .task_actual_line-scheduled {
  @include task_actual_line-common(#2794F8);
}

::v-deep .task_actual_line-running {
  @include task_actual_line-common(#51BD1C);
}

::v-deep .task_actual_line-finished {
  @include task_actual_line-common(#999999);
}

::v-deep .task_actual_delay_box {
  border-radius: 3px;
  border: 1px solid #FF4D4F;
  z-index: 2;
}

::v-deep .current-row {
  background-color: #C7E3FD;
}

::v-deep .error-task-row {
  background-color: #FFF6E6;
}

::v-deep .current-line {
  background-color: #51BD1C;
}

::v-deep .rang-line {
  background-color: #FF4D4F;
}

::v-deep .child_preview {
  box-sizing: border-box;
  position: absolute;
  z-index: 0;
  background-color: #F5F5F5;
  border-radius: 0;
}

::v-deep .gantt_task_line.gantt_project.task-group-parent .gantt_task_content {
  display: none;
}

::v-deep .rest_task {
  z-index: 1;
  background: rgba(0, 0, 0, 0.5);
}

::v-deep .task-name {
  position: relative;
  cursor: pointer;
}

::v-deep .gantt_task_content {
  overflow: visible;

  .task-content {
    width: 100%;
    height: 100%;

    &.task-actual-line .task-text {
      -webkit-line-clamp: 1;
    }
  }

  .task-text {
    z-index: 2;
    cursor: pointer;
    position: relative;
    max-width: 100%;
    line-height: 16px;
    height: auto;
    top: 50%;
    transform: translateY(-50%);
    padding: 0 8px;
    font-size: 12px;
    font-face: PingFangSC;
    font-weight: 400;
    text-align: left;
    white-space: pre-wrap;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: break-word;
  }

  .task-btn {
    z-index: 10;
    position: absolute;
    right: -31px;
    top: 5px;
    border-left: 3px solid transparent;
    display: none;

    i,
    img {
      width: 28px;
      height: 28px;
      padding: 6px;
      background: #FFFFFF;
      border: 1px solid #D9D9D9;
      border-radius: 4px;
      color: var(--c-themeColor);
      display: flex;
      justify-content: center;
      align-items: center;
      cursor: pointer;
    }
      // 拆分
      .task-influence{
        border: 1px solid var(--c-themeColor);
        border-radius: 4px;
        padding: 0 4px;
        height: 28px;
        width: 28px;
        background: #FFFFFF;
        color: var(--c-themeColor);
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;
      }
      .task-split{
        border: 1px solid var(--c-themeColor);
        border-radius: 4px;
        padding: 0 4px;
        height: 28px;
        width: 28px;
        background: #FFFFFF;
        color: var(--c-themeColor);
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;
      }
  }
  .task-sign {
    position: absolute;
    right: 0;
    top: 0;
    max-width: 100%;
    width: 30px;
    height: 30px;
    overflow: hidden;

    span {
      display: inline-block;
      position: absolute;
      right: -11px;
      top: 2px;
      width: 42px;
      height: 14px;
      line-height: 14px;
      font-size: 10px;
      font-family: PingFangSC-Semibold;
      font-weight: 500;
      background-color: #ffffff;
      transform: rotate(-315deg);
    }

    &+.task-text {
      padding-right: 30px;
    }
  }
}

::v-deep .gantt_message_area {
  position: absolute !important;
  bottom: 5px !important;
  top: unset !important;
}

::v-deep .gantt_cal_qi_content {
  //display: none !important;
}

::v-deep .dhx_menu_icon.dhx_gantt_icon {
  display: none !important;
}

::v-deep .dhx_gantt_button_link,
::v-deep .gantt_qi_big_icon {
  padding: 6px 10px !important;
}

::v-deep .gantt-channel-select {
  line-height: 38px;

  .el-select {
    position: relative;
    line-height: 32px;
  }
}

// }
.task-gantt {
  overflow: hidden;
  position: relative;
  width: 100%;
  // height: 100%;

  // flex-grow: 1;
  // flex-shrink: 1;
  // flex: 1;
  // height: calc(100% - 44px);
  &--editable{
    ::v-deep .gantt_task_content {
      &:hover {
        .task-btn {
          display: block;
        }
      }
    }
  }
  &--dialogeditable{
    ::v-deep .gantt_task_content {
      &:hover {
        .task-btn {
          display: block;
        }
      }
    }
  }
  &--splittable {
    ::v-deep .gantt_task_content {
      &:hover {
        .task-btn {
          display: block;
        }
      }
    }
  }
}
</style>
<style lang="scss">
div.gantt_tooltip {
  padding: 6px 8px;
  background: #FFFFFF;
  border: 1px solid #D9D9D9;
  border-radius: 4px;
  cursor: pointer;

  .tooltip-btn {
    color: var(--c-themeColor);
  }
}

.el-message.el-message--error.is-closable,
.el-message.el-message--warning.is-closable {
  white-space: pre-line !important;
}

body>.gantt_message_area {
  display: none;
}
</style>
