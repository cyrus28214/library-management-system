<script setup>
import axios from 'axios';
import { ref, onMounted, computed } from 'vue';
import BookDialog from './BookDialog.vue';

const books = ref([]);

const emptyBook = {
    title: '',
    category: '',
    press: '',
    publishYear: '',
    author: '',
    price: '',
    stock: ''
};
const newBook = ref(emptyBook);
const addBookDialogVisible = ref(false);
const toDeleteBook = ref(null);
const deleteBookDialogVisible = ref(false);
const message = ref('');
const messageDialogVisible = ref(false);

const toEditBook = ref(null);
const editBookDialogVisible = ref(false);

const queryBook = async () => {
    const reponse = await axios.get('/book');
    books.value = reponse.data.payload.results;
    console.log(reponse.data);
}

const addBook = async () => {
    const reponse = await axios.post('/book', {
        title: newBook.value.title,
        category: newBook.value.category,
        press: newBook.value.press,
        publishYear: parseInt(newBook.value.publishYear),
        author: newBook.value.author,
        price: parseFloat(newBook.value.price),
        stock: parseInt(newBook.value.stock)
    });
    if (reponse.data.message !== null) {
        message.value = reponse.data.message;
        messageDialogVisible.value = true;
        console.log(reponse.data.message);
    }
    if (reponse.data.ok) {
        newBook.value = emptyBook;
        addBookDialogVisible.value = false;
        queryBook();
    }
    
}

const editBook = async () => {
    const reponse = await axios.put('/book', {
        bookId: toEditBook.value.bookId,
        category: toEditBook.value.category,
        title: toEditBook.value.title,
        press: toEditBook.value.press,
        publishYear: parseInt(toEditBook.value.publishYear),
        author: toEditBook.value.author,
        price: parseFloat(toEditBook.value.price),
        deltaStock: parseInt(toEditBook.value.deltaStock)
    });
    if (reponse.data.message !== null) {
        message.value = reponse.data.message;
        messageDialogVisible.value = true;
        console.log(reponse.data.message);
    }
    if (reponse.data.ok) {
        editBookDialogVisible.value = false;
        queryBook();
    }
    
}

const onConfirmDeleteBook = async (bookId) => {
    const reponse = await axios.delete('/book', { params: { bookId } });
    console.log(reponse.data);
    if (reponse.data.message !== null) {
        message.value = reponse.data.message;
        messageDialogVisible.value = true;
        console.log(reponse.data.message);
    }
    if (reponse.data.ok) {
        queryBook();
        deleteBookDialogVisible.value = false;
    }
}

onMounted(() => {
    queryBook();
})
</script>
<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%;">
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理</div>
        <div style="margin: 20px 40px 0 40px;">
            <el-button type="primary" @click="addBookDialogVisible = true">添加图书</el-button>

            <el-table :data="books" height="600" :table-layout="'auto'"
                style="width: 100%; max-width: 80vw;">
                <el-table-column prop="bookId" label="图书ID" />
                <el-table-column prop="title" label="图书名称" />
                <el-table-column prop="category" label="图书类别" />
                <el-table-column prop="press" label="出版社" />
                <el-table-column prop="publishYear" label="出版年份" />
                <el-table-column prop="author" label="作者" />
                <el-table-column prop="price" label="价格" />
                <el-table-column prop="stock" label="库存" />
                <el-table-column label="操作">
                    <template #default="scope">
                        <el-button type="primary" @click="toEditBook = {...scope.row, deltaStock: 0}; editBookDialogVisible = true">编辑</el-button>
                        <el-button type="danger" @click="toDeleteBook = scope.row; deleteBookDialogVisible = true">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

        </div>

        <!-- 添加图书对话框 -->
        <BookDialog v-model="addBookDialogVisible" :book="newBook" @submit="addBook" />

        <!-- 编辑图书对话框 -->
        <BookDialog v-model="editBookDialogVisible" isEdit :book="toEditBook" @submit="editBook" />

        <!-- 确认删除图书对话框 -->
        <el-dialog v-model="deleteBookDialogVisible" title="删除图书" width="30%">
            <span>确定删除<span style="font-weight: bold;">"{{ toDeleteBook.title }}"</span>吗？</span>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="deleteBookDialogVisible = false">取消</el-button>
                    <el-button type="danger" @click="onConfirmDeleteBook(toDeleteBook.bookId)">删除</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 信息对话框 -->  
        <el-dialog v-model="messageDialogVisible" width="30%" align-center>
            <span>{{ message }}</span>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="messageDialogVisible = false">确定</el-button>
                </span>
            </template>
        </el-dialog>
    </el-scrollbar>


</template>
