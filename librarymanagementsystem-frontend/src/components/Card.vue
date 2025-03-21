<script setup>
import { Delete, Edit, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import axios from 'axios';
import { ref, onMounted, computed } from 'vue';

const cards = ref([]);

const toSearch = ref(''); // 搜索内容
const types = ref([{ // 借书证类型
    value: 'T',
    label: '教师',
}, {
    value: 'S',
    label: '学生',
}]);

const newCardVisible = ref(false); // 新建借书证对话框可见性
const removeCardVisible = ref(false); // 删除借书证对话框可见性
const toRemove = ref(0); // 待删除借书证号

const emptyCard = {
    name: '',
    department: '',
    type: 'S'
}

const newCardInfo = ref(null);
const modifyCardVisible = ref(false); // 修改信息对话框可见性
const toModifyInfo = ref({});

const message = ref(null);
const messageVisible = computed(() => message.value !== null);

const ConfirmNewCard = async () => {
    // 发出POST请求
    const response = await axios.post("/card", { // 请求体
        name: newCardInfo.value.name,
        department: newCardInfo.value.department,
        type: newCardInfo.value.type
    });

    ElMessage.success("借书证新建成功") // 显示消息提醒
    newCardVisible.value = false // 将对话框设置为不可见
    QueryCards() // 重新查询借书证以刷新页面
}

const ConfirmModifyCard = async () => {
    const reponse = await axios.put('/card', {
        cardId: toModifyInfo.value.cardId,
        name: toModifyInfo.value.name,
        department: toModifyInfo.value.department,
        type: toModifyInfo.value.type
    });
    if (reponse.data.message !== null) {
        console.log(reponse.data.message);
        message.value = reponse.data.message;
    }
    modifyCardVisible.value = false;
    QueryCards();
}

const ConfirmRemoveCard = async () => {
    const reponse = await axios.delete('/card', { params: {
        cardId: toRemove.value
    } });
    if (reponse.data.message !== null) {
        console.log(reponse.data.message);
        message.value = reponse.data.message;
    }
    removeCardVisible.value = false;
    QueryCards();
}

const QueryCards = async () => {
    cards.value = [] // 清空列表
    const response = await axios.get('/card') // 向/card发出GET请求
    response.data.payload.cards.forEach(card => { // 对于每个借书证
        cards.value.push(card) // 将其加入到列表中
    })
}

onMounted(() => {
    QueryCards()
})

</script>


<style scoped>
.cardBox {
    height: 300px;
    width: 200px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    text-align: center;
    text-wrap: nowrap;
    overflow: hidden;
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
}

.newCardBox {
    height: 300px;
    width: 200px;
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    text-align: center;
}
</style>

<template>
    <el-scrollbar height="100%" style="width: 100%;">
        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">借书证管理
            <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable />
        </div>

        <!-- 借书证卡片显示区 -->
        <div style="display: flex;flex-wrap: wrap; justify-content: start;">

            <!-- 借书证卡片 -->
            <div class="cardBox" v-for="card in cards" v-show="card.name.includes(toSearch)" :key="card.id">
                <div>
                    <!-- 卡片标题 -->
                    <div style="font-size: 25px; font-weight: bold;">No. {{ card.cardId }}</div>

                    <el-divider />

                    <!-- 卡片内容 -->
                    <div style="margin-left: 10px; text-align: start; font-size: 16px;">
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">姓名：</span>{{ card.name }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">部门：</span>{{ card.department }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">类型：</span>{{ types.find(type => type.value === card.type).label }}</p>
                    </div>

                    <el-divider />

                    <!-- 卡片操作 -->
                    <div style="margin-top: 10px;">
                        <el-button type="primary" :icon="Edit" @click="toModifyInfo.cardId = card.cardId, toModifyInfo.name = card.name,
                            toModifyInfo.department = card.department, toModifyInfo.type = card.type,
                            modifyCardVisible = true" circle />
                        <el-button type="danger" :icon="Delete" circle
                            @click="toRemove = card.cardId, removeCardVisible = true"
                            style="margin-left: 30px;" />
                    </div>

                </div>
            </div>

            <!-- 新建借书证卡片 -->
            <el-button class="newCardBox"
                @click="newCardInfo = emptyCard; newCardVisible = true">
                <el-icon style="height: 50px; width: 50px;">
                    <Plus style="height: 100%; width: 100%;" />
                </el-icon>
            </el-button>

        </div>


        <!-- 新建借书证对话框 -->
        <el-dialog v-model="newCardVisible" title="新建借书证" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                姓名：
                <el-input v-model="newCardInfo.name" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                部门：
                <el-input v-model="newCardInfo.department" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类型：
                <el-select v-model="newCardInfo.type" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in types" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>

            <template #footer>
                <span>
                    <el-button @click="newCardVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewCard"
                        :disabled="newCardInfo.name.length === 0 || newCardInfo.department.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>


        <!-- 修改信息对话框 -->   
        <el-dialog v-model="modifyCardVisible" :title="'修改信息(借书证ID: ' + toModifyInfo.cardId + ')'" width="30%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                姓名：
                <el-input v-model="toModifyInfo.name" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                部门：
                <el-input v-model="toModifyInfo.department" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类型：
                <el-select v-model="toModifyInfo.type" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in types" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyCardVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyCard"
                        :disabled="toModifyInfo.name.length === 0 || toModifyInfo.department.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 删除借书证对话框 -->  
        <el-dialog v-model="removeCardVisible" title="删除借书证" width="30%">
            <span>确定删除<span style="font-weight: bold;">{{ toRemove }}号借书证</span>吗？</span>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeCardVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveCard">
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 信息对话框 -->  
        <el-dialog :visible="messageVisible" width="30%" align-center>
            <span>{{ message }}</span>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="message = null">确定</el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>