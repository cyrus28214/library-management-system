<script setup>
import { Search } from '@element-plus/icons-vue';
import { ref, computed } from 'vue';
import axios from 'axios';

const isShow = ref(false); // 结果表格展示状态
const tableData = ref([]);
const toQuery = ref(''); // 待查询内容(对某一借书证号进行查询)
const toSearch = ref(''); // 待搜索内容(对查询到的结果进行搜索)

const fitlerTableData = computed(() => tableData.value.filter(
    (tuple) =>
        (toSearch.value == '') || // 搜索框为空，即不搜索
        tuple.bookId == toSearch.value || // 图书号与搜索要求一致
        tuple.borrowTime.toString().includes(toSearch.value) || // 借出时间包含搜索要求
        tuple.returnTime.toString().includes(toSearch.value) // 归还时间包含搜索要求
));

const QueryBorrows = async () => {
    tableData.value = [];
    let response = await axios.get('/borrow', { params: { cardId: toQuery.value } }) // 向/borrow发出GET请求，参数为cardId=toQuery
    response.data.payload.items.forEach(borrow => { // 对于每一个借书记录
        tableData.value.push(borrow) // 将它加入到列表项中
    });
}
</script>

<template>
    <el-scrollbar height="100%" style="width: 100%;">

        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">
            借书记录查询
            <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right; ;"
                clearable />
        </div>

        <!-- 查询框 -->
        <div style="width:30%;margin:0 auto; padding-top:5vh;">

            <el-input v-model="toQuery" style="display:inline; " placeholder="输入借书证ID"></el-input>
            <el-button style="margin-left: 10px;" type="primary" @click="QueryBorrows">查询</el-button>

        </div>

        <!-- 结果表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600"
            :default-sort="{ prop: 'borrowTime', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="cardId" label="借书证ID" />
            <el-table-column prop="bookId" label="图书ID" sortable />
            <el-table-column prop="borrowTime" label="借出时间" sortable />
            <el-table-column prop="returnTime" label="归还时间" sortable />
        </el-table>

    </el-scrollbar>
</template>