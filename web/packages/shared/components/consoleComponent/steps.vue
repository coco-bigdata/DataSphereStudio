<template>
  <div class="we-steps">
    <Row class="we-steps-row-first">
      <Col
        span="2"
        class="we-steps-title"
      >{{ $t('message.common.steps.title') + '：'}}</Col>
      <Col
        span="22"
        class="we-steps-child-col"
      >
        <div
          class="we-steps-child"
          v-for="(child, index) in renderList"
          :key="index">
          <span class="we-steps-child-wrapper" v-if="child.value==='Running'">
            <Progress
              style="width: 150px"
              :percent="percent"
              status="active"
              hide-info
            />
            <span
              style="cursor: pointer;max-width: max-content;"
              class="we-steps-label"
              :class="getClasses(child)"
            >{{ child.label }}({{percent}}%)</span>
          </span>
          <span v-else class="we-steps-child-wrapper">
            <span
              class="we-steps-circle"
              :class="getClasses(child)"
              v-if="!child.isFinish">
            </span>
            <Icon
              v-else
              size="25"
              :color="getIconColor(child, index)"
              :type="getIconType(child)"></Icon>
            <Tooltip
              placement="right"
              theme="light"
              v-if="child.value==='FailedToGetResult'">
              <span
                style="cursor: pointer;"
                class="we-steps-label"
                :class="getClasses(child)">{{ child.label }}</span>
              <div
                slot="content"
                style="padding: 6px 10px;">
                <p style="font-weight: bold;line-height: 24px;">{{ $t('message.common.detail') }}</p>
                <p
                  v-for="(p, index1) in hoverList"
                  :key="index1"
                  :style="{'color': p.includes($t('message.common.Failed')) ? 'red' : '#67c23a'}"
                  style="line-height: 24px;">{{ p }}</p>
              </div>
            </Tooltip>
            <span
              v-else
              style="cursor: pointer;"
              class="we-steps-label"
              :class="getClasses(child)"
            >{{ child.label }}</span>
          </span>
          <Icon
            :color="getArrowColor(child, index)"
            type="md-arrow-round-forward"
            size="26"
            v-if="index !== renderList.length - 1 && child.isFinish"/>
        </div>
        <div v-if="costTime" style="padding-left: 10px">
          <span>{{$t('message.common.process')}}</span>
          <span class="progress-costtime">{{ costTime }}</span>
        </div>
      </Col>
    </Row>
  </div>
</template>
<script>
import { cloneDeep } from 'lodash';
export default {
  props: {
    stepList: {
      type: Array,
      default: () => [],
    },
    percent: {
      type: Number,
      default: 0
    },
    costTime: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // completed 是完成状态，完成状态后不会有其他状态产生；
      // special 是结果集衍生状态，用于判断结果集三个步骤中哪个步骤发生错误；
      // failure 是错误状态，用于标记错误，红色高亮
      // retry 是重试状态，需要在前面追加申请资源失败或执行失败两个状态
      stepsInfo: [
        { label: this.$t('message.common.steps.stepsInfo.Submitted'), value: 'Submitted', status: ['running'], isFinish: false },
        // websocket不返回Inited，所以把第一个接收的execute接口作为Inited
        // 这里是为了防止http方式去请求时，或者长时间未返回状态时，前台去主动请求到Inited状态的情况
        { label: this.$t('message.common.steps.stepsInfo.Inited'), value: 'Inited', status: ['running'], isFinish: false },
        { label: '...', value: 'ellipsis', status: ['running'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.Scheduled'), value: 'Scheduled', status: ['running'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.Running'), value: 'Running', status: ['running'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.Succeed'), value: 'Succeed', status: ['running'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.ResultLoading'), value: 'ResultLoading', status: ['running'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.Completed'), value: 'Completed', status: ['completed'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.Failed'), value: 'Failed', status: ['completed', 'failure'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.Cancelled'), value: 'Cancelled', status: ['completed', 'warning'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.FailedToGetResult'), value: 'FailedToGetResult', status: ['completed', 'failure'], isFinish: true },
        { label: this.$t('message.common.steps.stepsInfo.FailedToGetResultPath'), value: 'FailedToGetResultPath', status: ['special'], step: 1 },
        { label: this.$t('message.common.steps.stepsInfo.FailedToGetResultList'), value: 'FailedToGetResultList', status: ['special'], step: 2 },
        { label: this.$t('message.common.steps.stepsInfo.FailedToGetResultFirst'), value: 'FailedToGetResultFirst', status: ['special'], step: 3 },
        { label: this.$t('message.common.steps.stepsInfo.WaitForRetry'), value: 'WaitForRetry', status: ['retry'], isFinish: false },
        { label: this.$t('message.common.steps.stepsInfo.FailedToApply'), value: 'FailedToApply', status: ['failure'], isFinish: true },
        { label: this.$t('message.common.steps.stepsInfo.FailedToExecute'), value: 'FailedToExecute', status: ['failure'], isFinish: true }
      ],
      renderList: [],
      hoverList: [],
    };
  },
  watch: {
    stepList: {
      handler() {
        this.renderSteps();
      },
      deep: true,
    },
  },
  mounted() {
    this.renderSteps();
  },
  methods: {
    renderSteps() {
      this.stepListData = [ ...this.stepList ]
      this.renderList = [];
      const IS_FINISH = { isFinish: true };
      const IS_LOADING = { isLoading: false };
      const RETRY_STATUS = 'WaitForRetry';
      const FAILED_TO_GET_RESULT_STATUS = 'FailedToGetResult';
      const SCHEDULED_STATUS = 'Scheduled';
      const stepsInfoValue = this.stepsInfo.map(item => item.value)
      if(this.stepListData && this.stepListData.toString() && this.stepListData[0] !== 'Submitted' && this.stepListData[0] !== 'ellipsis') {
        let index = this.stepsInfo.findIndex((i) => i.value === this.stepListData[0]);
        let list = stepsInfoValue.slice(0, index + 1);
        if(!this.stepListData.includes(SCHEDULED_STATUS)) list.splice(2, 1)
        this.stepListData = list
      }
      this.stepListData.forEach((status, index) => {
        // 要放每个步骤都不是地址引用，否则会出现isFinish这个状态错误
        const step = cloneDeep(this.findStep(status));
        if (step) {
          // 如果是特殊态的情况，要设置hover的列表，渲染的时候要显示它们的父状态：FailedToGetResult
          if (step.status.indexOf('special') !== -1) {
            const FailedToGetResult = this.findStep(FAILED_TO_GET_RESULT_STATUS);
            this.renderList.push(FailedToGetResult);
            this.setHoverList(step);
          // 如果是重试态，则要在之前插入失败的状态
          } else if (step.value === RETRY_STATUS) {
            const prevOne = this.stepListData[index - 1];
            const completionValue = prevOne === SCHEDULED_STATUS ? 'FailedToApply' : 'FailedToExecute';
            const completion = this.findStep(completionValue);
            this.renderList.push(completion);
            this.renderList.push(Object.assign(step, IS_FINISH, IS_LOADING));
          } else {
            this.renderList.push(Object.assign(step, IS_FINISH, IS_LOADING));
          }
          // 最后一个状态
          if (index === this.stepListData.length - 1) {
          // 如果属于运行态，就在最后插入它的下一个状态
            if (step.status.indexOf('running') !== -1) {
              step.isLoading = true;
              const findIndex = this.stepsInfo.findIndex((i) => i.value === status);
              if (findIndex !== -1) {
                this.renderList.push(this.stepsInfo[findIndex + 1]);
              }
            // 如果是重试态，就在后面插入“资源申请”中这个状态
            } else if (step.value === RETRY_STATUS) {
              step.isLoading = true;
              const scheduled = this.findStep(SCHEDULED_STATUS);
              this.renderList.push(scheduled);
            }
          }
        }
      });
    },
    findStep(arg) {
      return this.stepsInfo.find((i) => i.value === arg);
    },
    setHoverList(item) {
      switch (item.step) {
        case 1:
          this.hoverList = [`1.${item.label}`];
          break;
        case 2:
          this.hoverList = [this.$t('message.common.steps.hoverList.first'), `2.${item.label}`];
          break;
        case 3:
          this.hoverList = [this.$t('message.common.steps.hoverList.first'), this.$t('message.common.steps.hoverList.second'), `3.${item.label}`];
          break;
      }
    },
    getArrowColor(child, index) {
      const nextOne = this.renderList[index + 1];
      return this.getIconColor(nextOne);
    },
    getIconColor(child) {
      const COMPLETED_COLOR = '#67c23a';
      const ERROR_COLOR = '#ed4014';
      const CALCELLED_COLOR = '#ddd';
      const UN_FINISHED_COLOR = 'orange';
      if (child.status.indexOf('failure') !== -1) {
        return ERROR_COLOR;
      } else if (child.status.indexOf('warning') !== -1) {
        return CALCELLED_COLOR;
      } else if (child.isFinish && !child.isLoading) {
        return COMPLETED_COLOR;
      }
      return UN_FINISHED_COLOR;
    },
    getIconType(child) {
      const FINISHED = 'md-checkmark-circle';
      const FAILED = 'md-close-circle';
      const WARNING = 'md-alert';
      const LOADING = 'ios-navigate';
      if (child.status.indexOf('failure') !== -1) {
        return FAILED;
      } else if (child.status.indexOf('warning') !== -1) {
        return WARNING;
      } else if (child.isFinish && child.isLoading) {
        return LOADING;
      }
      return FINISHED;
    },
    getClasses(child) {
      let className = '';
      if (child.label === '...') {
        className = 'ellipsis '
      }
      if (child.status.indexOf('failure') !== -1) {
        className += 'error';
      } else if (child.status.indexOf('warning') !== -1) {
        className += 'cancelled';
      } else if (child.isFinish && !child.isLoading) {
        className += 'completed';
      } else if (child.isFinish && child.isLoading) {
        className += 'loading';
      }
      return className;
    },
  },
};
</script>
<style lang="scss" scoped>
@import '@dataspherestudio/shared/common/style/variables.scss';
.we-steps {
  .we-steps-row-first {
    width: 100%;
    padding: 15px 0;
    display: flex;
    align-items: center;
    .we-steps-title {
      text-align: center;
      @include font-color($workspace-title-color, $dark-workspace-title-color);
    }
    .we-steps-child-col {
      height: 50px;
      display: inline-flex;
      justify-content: flex-start;
      align-items: center;
      .we-steps-child {
        display: flex;
        align-items: center;
        .we-steps-child-wrapper {
            display: inline-flex;
            flex-direction: column;
            align-items: center;
            padding: 0 8px;
            .we-steps-circle {
              width: 20px;
              height: 20px;
              border-radius: 10px;
              display: inline-block;
              background: $subsidiary-color;
              text-align: center;
              .we-steps-circle-checkmark {
                @include font-color(#fff, #000);
              }
              &.loading {
                background: orange;
              }
              &.completed {
                background: #67c23a;
              }
              &.error {
                background: #ed4014;
              }
              &.cancelled {
                background: #ddd
              }
          }
          .we-steps-label {
            display: inline-block;
            margin-top: 6px;
            &.ellipsis {
              font-weight: bold;
              font-size: 19px;
              display: inline-block;
              line-height: 17px;
              height: 17px;
            }
            &.loading {
              color: orange;
            }
            &.completed {
              color: #67c23a;
            }
            &.error {
              color: #ed4014;
            }
            &.cancelled {
              color: $pink-color
            }
          }
        }
      }
    }
  }
  .progress-costtime {
    color: $success-color;
  }
}
</style>

