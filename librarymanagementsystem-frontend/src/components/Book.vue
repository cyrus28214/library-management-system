<!-- TODO: YOUR CODE HERE -->
<script setup>
import axios from 'axios';
import { ref, onMounted } from 'vue';
const books = ref([]);

const queryBook = async () => {
    const reponse = await axios.get('/book');
    books.value = reponse.data.payload.results;
    console.log(reponse.data);
}

onMounted(() => {
    queryBook();
})
</script>
<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%;">
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理</div>
        <el-table :data="books" height="600" :table-layout="'auto'"
        style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="bookId" label="图书ID" />
            <el-table-column prop="title" label="图书名称" />
            <el-table-column prop="category" label="图书类别" />
            <el-table-column prop="press" label="出版社" />
            <el-table-column prop="publishYear" label="出版年份" />
            <el-table-column prop="author" label="作者" />
            <el-table-column prop="price" label="价格" />
            <el-table-column prop="stock" label="库存" />
        </el-table>
    </el-scrollbar>
</template>
