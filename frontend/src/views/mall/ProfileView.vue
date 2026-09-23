<template>
  <el-card class="card-shadow">
    <el-tabs v-model="tab">
      <el-tab-pane label="个人资料" name="profile">
        <div class="profile-wrap">
          <div class="avatar-box">
            <el-avatar :size="88" :src="userStore.userInfo?.avatar || ''">
              {{ (userStore.userInfo?.nickname || 'U').slice(0, 1) }}
            </el-avatar>
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadNewAvatar">
              <el-button size="small" style="margin-top: 12px">更换头像</el-button>
            </el-upload>
          </div>
          <el-form :model="profileForm" label-width="80px" style="flex: 1; max-width: 420px">
            <el-form-item label="用户名">
              <el-input :model-value="userStore.userInfo?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" maxlength="50" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" maxlength="11" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="修改密码" name="password">
        <el-form :model="pwdForm" label-width="90px" style="max-width: 420px" label-position="left">
          <el-form-item label="原密码">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="pwdForm.confirm" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="收货地址" name="address">
        <el-button type="primary" style="margin-bottom: 14px" @click="openAddressDialog()">
          <el-icon><Plus /></el-icon>&nbsp;新增地址
        </el-button>
        <div v-if="addresses.length" class="addr-list">
          <div v-for="a in addresses" :key="a.id" class="addr-item" :class="{ default: a.isDefault }">
            <div class="addr-main">
              <b>{{ a.receiver }}</b>
              <span class="text-gray">{{ a.phone }}</span>
              <el-tag v-if="a.isDefault" size="small" type="primary" style="margin-left: 8px">默认</el-tag>
            </div>
            <div class="text-gray" style="margin-top: 6px">{{ a.province }} {{ a.city }} {{ a.district }} {{ a.detail }}</div>
            <div class="addr-actions">
              <el-button v-if="!a.isDefault" size="small" text type="primary" @click="setDefault(a)">设为默认</el-button>
              <el-button size="small" text type="primary" @click="openAddressDialog(a)">编辑</el-button>
              <el-popconfirm title="确定删除该地址吗？" @confirm="removeAddr(a)">
                <template #reference>
                  <el-button size="small" text type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
        <el-empty v-else description="还没有收货地址" :image-size="80" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="addrVisible" :title="addrForm.id ? '编辑地址' : '新增地址'" width="520px">
      <el-form ref="addrRef" :model="addrForm" :rules="addrRules" label-width="80px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="addrForm.receiver" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addrForm.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="addrForm.province" placeholder="如：湖北" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="addrForm.city" placeholder="如：武汉" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="addrForm.district" placeholder="如：洪山区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addrForm.detail" placeholder="街道、门牌号、楼栋宿舍等" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addrForm.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getProfile, updateProfile, changePassword, uploadAvatar,
  getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress
} from '../../api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const tab = ref('profile')
const saving = ref(false)
const savingPwd = ref(false)
const profileForm = reactive({ nickname: '', phone: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const addresses = ref([])
const addrVisible = ref(false)
const addrRef = ref()
const emptyAddr = {
  id: null, receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false
}
const addrForm = reactive({ ...emptyAddr })

const addrRules = {
  receiver: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

async function loadProfile() {
  const user = await getProfile()
  userStore.setProfile(user)
  profileForm.nickname = user.nickname
  profileForm.phone = user.phone || ''
}

async function uploadNewAvatar({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  await uploadAvatar(fd)
  ElMessage.success('头像已更新')
  loadProfile()
}

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile(profileForm)
    ElMessage.success('资料已更新')
    loadProfile()
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await changePassword(pwdForm)
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = pwdForm.newPassword = pwdForm.confirm = ''
  } finally {
    savingPwd.value = false
  }
}

async function loadAddresses() {
  addresses.value = await getAddresses()
}

function openAddressDialog(a) {
  Object.assign(addrForm, emptyAddr)
  if (a) {
    Object.assign(addrForm, { ...a, isDefault: a.isDefault === 1 })
  }
  addrVisible.value = true
}

async function saveAddress() {
  await addrRef.value.validate()
  const payload = { ...addrForm, isDefault: !!addrForm.isDefault }
  if (addrForm.id) {
    await updateAddress(addrForm.id, payload)
  } else {
    await addAddress(payload)
  }
  ElMessage.success('已保存')
  addrVisible.value = false
  loadAddresses()
}

async function setDefault(a) {
  await setDefaultAddress(a.id)
  ElMessage.success('已设为默认')
  loadAddresses()
}

async function removeAddr(a) {
  await deleteAddress(a.id)
  ElMessage.success('已删除')
  loadAddresses()
}

onMounted(() => {
  loadProfile()
  loadAddresses()
})
</script>

<style scoped>
.profile-wrap { display: flex; gap: 48px; align-items: flex-start; }
.avatar-box { display: flex; flex-direction: column; align-items: center; }
.addr-list { display: flex; flex-direction: column; gap: 12px; max-width: 640px; }
.addr-item { border: 1px solid #eee; border-radius: 8px; padding: 14px 16px; }
.addr-item.default { border-color: #a0cfff; background: #f0f9ff; }
.addr-main { display: flex; align-items: center; gap: 10px; }
.addr-actions { margin-top: 8px; }
</style>
