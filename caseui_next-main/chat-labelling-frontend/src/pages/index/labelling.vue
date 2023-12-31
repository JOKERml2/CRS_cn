<template>
  <div style="overflow:auto;padding:10px 30px" :style="{height}">
    <Drawer
      title="请选择推荐的产品或明晰问题提问词"
      closable
      v-model="drawer"
      :mask-closable="false"
      scrollable
      placement="left"
      draggable
      :mask="false"
      :width="45"
    >
      <searchPanel
        :loading="searchPanelLoading"
        v-model="formItem.selectedSearch"
        :activeActions="formItem.action"
        :data="searchResults"
        :searchResultConfig="searchResultConfig"
        :searching="searching"
        @on-change="selectedResultChanged"
        @on-order-change="selectedResultOrderChanged"
        @changeBackupForParent="sendChangeBackup"
        ref="searchPanel"
      ></searchPanel>
    </Drawer>
    <Drawer
      title="请选择过滤器选项"
      closable
      v-model="fdrawer"
      :mask-closable="false"
      scrollable
      placement="right"
      draggable
      :mask="false"
      :width="45"
    >
      <filtersPanel
        :loading="searchPanelLoading"
        v-model="formItem.selectedSearch"
        :activeActions="formItem.action"
        :data="searchResults"
        :searchResultConfig="searchResultConfig"
        :searching="searching"
        @on-filters-change="selectedFiltersChanged"
        ref="filtersPanel"
      ></filtersPanel>
    </Drawer>
    <Form
      :model="formItem"
      :label-width="100"
      @submit.native.prevent
      :disabled="sendDisabled||submitting"
      :rules="rules"
      ref="form"
    >
      <FormItem v-if="role==='sys'" label="查询" prop="state">
        <createSelect
          v-model="formItem.state"
          :canCreate="canCreateState"
          @resetdonotsearch="resetdonotsearch"
          @on-change="stateChanged"
          @states-backup="statesBackup"
          @show-drawer="drawer = true"
          @searchingstate="searchingstate"
        />
        <div class="vertical-spacing"></div> <!-- 垂直间距 -->
        <Button @click="fdrawer=!fdrawer">显示过滤器选项</Button>
        <Button @click="searchDataWithFilters">使用带过滤器选项的搜索</Button>
      </FormItem>
      <FormItem v-if="role==='sys'" label="意图" prop="action">
        <customCascader
          :data="actions.system"
          v-model="formItem.action"
          placeholder="请选择"
          @on-change="actionChanged"
          @showTooltip="showTooltip"
          style="width:60%;display:inline-block"
        ></customCascader>
        <div class="vertical-spacing"></div> <!-- 垂直间距 -->
        <Button @click="drawer=!drawer">显示搜索结果</Button>
      </FormItem>
      <FormItem v-if="role==='cus'" label="意图" prop="action">
        <customCascader
          :data="actions.user"
          v-model="formItem.action"
          placeholder="请选择"
          @on-change="actionChanged"
          @showTooltip="showTooltip"
        ></customCascader>
      </FormItem>
      <FormItem v-if="role==='sys'" label="已选推荐的明晰问题提问词/产品结果" prop="selectedSearch">
        <draggableList v-model="formItem.selectedSearch" />
      </FormItem>
      <FormItem label="回复" prop="response">
        <Input v-model="formItem.response" @on-blur="responseChanged" type="textarea" :rows="4" />
      </FormItem>
      <FormItem style="margin-bottom:0px">
        <Button type="default" @click="submit">发送</Button>
        <Checkbox v-model="formItem.sendAnother" style="margin-left:20px">发送另一条消息</Checkbox>
        <br />
        <a @click="finishConversation" v-if="role==='cus'">结束对话>></a>
      </FormItem>
    </Form>
  </div>
</template>
<script>
import customCascader from '../../components/customCascader'
import helpTooltip from '../../components/helpTooltip'
import searchPanel from './searchPanel'
import filtersPanel from './filtersPanel'
import draggableList from '../../components/draggableList'
import createSelect from '../../components/createSelect'

const calAllow = (arr, actionNames, actionNameIndex) => {
  if (!actionNames[actionNameIndex]) return false
  let a = arr.filter(value => {
    return actionNames[actionNameIndex] === value.name
  })
  if (!a[0]) return false
  if (a[0].children) {
    return calAllow(a[0].children, actionNames, actionNameIndex + 1)
  } else { return a[0] && a[0].allowEmptySearch === true }
}

export default {
  components: {
    helpTooltip,
    searchPanel,
    filtersPanel,
    draggableList,
    customCascader,
    createSelect
  },
  props: {
    searchResultConfig: {},
    conversationId: {
    },
    height: {
      type: String
    },
    sendData: {// 发送数据给后台的方法
      type: Function
    },
    sendDisabled: {// 是否允许发送数据
      type: Boolean
    },
    role: {
      type: String
    },
    finish: {
      type: Function // 用户角色自主结束会话的方法
    },
    actions: {}, // 全部action
    currentState: {}, // 上一轮过后，state是什么（数据库里的state，系统用户没进行修改）
    searchData: {}, // 调用api后搜索结果，每次state改变都会重新搜索,
    bus: {
      type: Object
    },
    showOptionsToUser: false,
    selectedRecommendations: [],
    searching: false,
    searchPanelLoading: false
  },
  created () {
    this.bus.$on('loadMore', (data) => {
      this.searchPanelLoading = false
      this.searchResults = {...data}
    })
  },
  watch: {
    currentState (newValue, oldValue) {
      if (newValue === oldValue) return
      console.log('currentstate')
      console.log(newValue, oldValue)
      this.formItem.state = [...newValue]
      this.stateChanged(this.currentState, true)
    }
  },
  data () {
    return {
      donotsearch: false,
      formItem: {
        selectedSearch: [],
        state: [],
        action: [],
        response: '',
        sendAnother: false,
        statesBackupList: []
      },
      rules: {
        state: [{
          required: false,
          validator: (rule, value, callback) => {
            if (this.formItem.action && !calAllow(this.actions.system, this.formItem.action, 0) && (!value || value.length === 0)) {
              return callback(new Error('请查询!'))
            } else callback()
          },
          trigger: 'blur'
        }],
        action: [{
          required: true,
          validator: (rule, value, callback) => {
            if (!value || value.length === 0) {
              return callback(new Error('请选择一项意图!'))
            } else callback()
          },
          trigger: 'blur'
        }],
        response: [{
          required: true,
          type: 'string',
          message: '请填写回复内容!',
          trigger: 'blur'
        }],
        selectedSearch: [{
          validator: (rule, value, callback) => {
            let active = this.$refs.searchPanel.active
            if (this.formItem.action && !calAllow(this.actions.system, this.formItem.action, 0) && active && active.length > 0 && (!value || value.length === 0)) {
              return callback(new Error('请选择搜索结果!'))
            } else { callback() }
          },
          trigger: 'blur'
        }]
      },
      drawer: false,
      fdrawer: false,
      sendLog: true,
      // searchPanelLoading: false,
      // searching: false,
      searchResults: {},
      actionBackup: [], // 上一次sys action改变后的值
      submitting: false, // 为了避免在发送数据时，用户进行别的操作
      statesBackupList: []
    }
  },
  methods: {
    resetdonotsearch () {
      this.donotsearch = 0
    },
    searchingstate () {
      // this.searching = true
      // this.searchPanelLoading = false
      this.$refs.filtersPanel.resetselectedFilter()
    },
    searchDataWithFilters () {
      // Call the searchDataWithFilters method in the filtersPanel component
      this.drawer = true
      this.$refs.filtersPanel.searchDataWithFilters()
    },
    statesBackup (newStates) {
      this.statesBackupList = newStates
      this.$emit('states-backup', newStates)
    },
    reset () {
      this.sendLog = false // 重置界面期间数据的改变不发log
      this.donotsearch = 10000
      this.formItem = {
        selectedSearch: [],
        state: [],
        action: [],
        response: '',
        sendAnother: false
      }
      this.drawer = false
      this.searchPanelLoading = true
      this.searching = false
      this.searchResults = {}
      this.actionBackup = []
      this.sendLog = true
      this.sendChangeBackup(' ')
      this.$nextTick(() => {
        this.sendLog = true
      })
    },
    submit () {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.submitting = true// 发送数据之前禁止做任何修改
        console.log(this.formItem)
        this.sendData({...this.formItem}, () => {
          console.log('ready to send')
          console.log(this.formItem.selectedSearch)
          console.log(this.formItem.sendAnother)
          console.log(this.formItem.action)
          console.log(this.formItem.state)
          this.reset()
          console.log('aready send')
          console.log(this.formItem.selectedSearch)
          console.log(this.formItem.sendAnother)
          console.log(this.formItem.action)
          console.log(this.formItem.state)
          this.$nextTick(() => {
            this.submitting = false
          })
          console.log('what happened')
        })
      })
    },
    finishConversation () {
      this.$emit('getRecommendInfo')
      this.$Modal.confirm({
        title: '警告！',
        content: '您即将离开对话并且<strong>不会</strong>把您当前的回复内容发送给您的对话对象。是否继续？',
        onOk: () => {
          this.submitting = true// 发送数据之前禁止做任何修改
          this.finish({...this.formItem}, () => {
            this.reset()
            this.$nextTick(() => {
              this.submitting = false
            })
          })
        },
        onCancel: () => {}
      })
    },
    cascaderVisableChange (show) {
      if (show) { this.drawer = false }
    },
    stateChanged (states, force) {
      console.log('state change')
      console.log(this.donotsearch)
      if (this.donotsearch > 0) {
        this.donotsearch -= 1
        return
      }
      if ((this.submitting || this.sendDisabled || this.donotsearch) && !force) return
      // this.searchPanelLoading = true
      // this.searching = true
      this.searchResults = {}
      this.searchData(states || [], (data) => {
        if (data != null) {
          this.searchPanelLoading = false
          this.searching = false
        }
        this.searchResults = {...data}
      })
      // 清空action和selectedSearch
      this.$refs.filtersPanel.selectedFilters = []
      let originSendlog = this.sendLog
      this.sendLog = false
      this.formItem.selectedSearch = []
      this.formItem.action = []
      this.$nextTick(() => {
        this.sendLog = originSendlog
      })
      // 强制刷新一定不来自用户动作，所以不发log
      if (this.sendLog && !this.sendDisabled && this.conversationId && !force) {
        this.$log({
          conversationId: this.conversationId,
          type: 'stateChanged',
          data: {states}
        })
      }
    },
    actionChanged (actions) {
      // 如果是系统角色，改了相应action，对应要清空之前选择的search result
      if (this.role === 'sys' && this.formItem.selectedSearch.length > 0) {
        // actionBackup中存的是上一次sys action改变后的值
      // 比较与当前action的第一级是否相同，如果相同，则selectedSearch不做修改，否则会清空已选result
        if (this.actionBackup[0] !== actions[0]) {
          this.formItem.selectedSearch = []
        }
        this.actionBackup = [...actions]
      }
      if (this.sendLog && !this.sendDisabled && this.conversationId) {
        this.$log({
          conversationId: this.conversationId,
          type: 'actionChanged',
          data: {actions}
        })
      }
    },
    selectedResultChanged (selectedSearch) {
      if (this.sendLog && !this.sendDisabled && this.conversationId) {
        this.$log({
          conversationId: this.conversationId,

          type: 'selectedResultChanged',
          data: {selectedSearch}
        })
      }
    },
    selectedFiltersChanged (selectedFilters) {
      if (this.sendLog && !this.sendDisabled && this.conversationId) {
        this.$log({
          conversationId: this.conversationId,
          type: 'selectedFiltersChanged',
          data: {selectedFilters}
        })
      }
      this.$emit('on-filters', selectedFilters)
    },
    sendChangeBackup (checked) {
      this.$emit('changeBackupForParent', checked)
    },
    selectedResultOrderChanged (selectedSearch) {
      if (this.sendLog && !this.sendDisabled && this.conversationId) {
        this.$log({
          conversationId: this.conversationId,
          type: 'selectedResultOrderChanged',
          data: {selectedSearch}
        })
      }
    },
    responseChanged () {
      if (this.sendLog && !this.sendDisabled && this.conversationId && this.formItem.response) {
        this.$log({
          conversationId: this.conversationId,
          type: 'responseChanged',
          data: {response: this.formItem.response}
        })
      }
    },
    showTooltip (item) {
      if (!this.sendDisabled && this.sendLog && this.conversationId) {
        this.$log({
          conversationId: this.conversationId,

          type: 'showTooltip',
          data: {item: {...item}}
        })
      }
    },
    canCreateState (value) {
      if (value.split(' ').length >= 5) {
        return {
          canCreate: false,
          message: 'Too many words!'
        }
      }
      return {
        canCreate: true
      }
    }
  }
}
</script>
<style>
.vertical-spacing {
  margin-top: 5px; /* 根据需要调整垂直间距的大小 */
}
</style>
