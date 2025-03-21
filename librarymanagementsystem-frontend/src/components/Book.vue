<script setup>
import axios from 'axios';
import { ref, onMounted, computed } from 'vue';
import BookDialog from './BookDialog.vue';
import { Delete, Edit, Tickets, TopRight, BottomLeft, Plus } from '@element-plus/icons-vue';

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

const toAdjustStockBook = ref(null);
const deltaStock = ref(0);
const adjustStockDialogVisible = ref(false);

const importBookDialogVisible = ref(false);
const uploadRef = ref(null);
const fileContent = ref(null);

const toBorrowBook = ref(null);
const borrowBookDialogVisible = ref(false);

const toReturnBook = ref(null);
const returnBookDialogVisible = ref(false);

const handleApiMessage = (reponse) => {
    if (reponse.data.message !== null) {
        message.value = reponse.data.message;
        messageDialogVisible.value = true;
        console.log(reponse.data.message);
    }
}

const queryBook = async () => {
    const reponse = await axios.get('/book');
    handleApiMessage(reponse);
    if (reponse.data.ok) {
        books.value = reponse.data.payload.results;
    }
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
    handleApiMessage(reponse);
    if (reponse.data.ok) {
        newBook.value = emptyBook;
        addBookDialogVisible.value = false;
        queryBook();
    }
}

const editBook = async () => {
    const reponse = await axios.put('/book/info', {
        bookId: toEditBook.value.bookId,
        category: toEditBook.value.category,
        title: toEditBook.value.title,
        press: toEditBook.value.press,
        publishYear: parseInt(toEditBook.value.publishYear),
        author: toEditBook.value.author,
        price: parseFloat(toEditBook.value.price)
    });
    handleApiMessage(reponse);
    if (reponse.data.ok) {
        editBookDialogVisible.value = false;
        queryBook();
    }
}

const adjustStock = async () => {
    const reponse = await axios.put('/book/stock', {
        bookId: toAdjustStockBook.value.bookId,
        deltaStock: parseInt(deltaStock.value)
    });
    handleApiMessage(reponse);
    if (reponse.data.ok) {
        adjustStockDialogVisible.value = false;
        queryBook();
    }
}

const borrowBook = async () => {
    const reponse = await axios.post('/borrow', {
        bookId: toBorrowBook.value.bookId,
        cardId: toBorrowBook.value.cardId,
        borrowTime: new Date().getTime()
    });
    handleApiMessage(reponse);
    if (reponse.data.ok) {
        borrowBookDialogVisible.value = false;
        queryBook();
    }
}

const returnBook = async () => {
    const reponse = await axios.delete('/borrow', { params: {
        bookId: toReturnBook.value.bookId,
        cardId: toReturnBook.value.cardId,
        returnTime: new Date().getTime()
    }});
    handleApiMessage(reponse);
    if (reponse.data.ok) {
        returnBookDialogVisible.value = false;
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

const uploadFile = (file) => {
    if (!file || !file.raw) return;
    
    const reader = new FileReader();
    reader.onload = (e) => {
        try {
            const content = JSON.parse(e.target.result);
            if (!Array.isArray(content)) {
                message.value = "文件格式错误，文件内容必须是一个数组";
                messageDialogVisible.value = true;
                fileContent.value = null;
                uploadRef.value.clearFiles();
                return;
            }
            fileContent.value = content;
        } catch (error) {
            message.value = "文件格式错误，请上传有效的JSON文件";
            messageDialogVisible.value = true;
            fileContent.value = null;
            uploadRef.value.clearFiles();
        }
    };
    reader.readAsText(file.raw);
}

const importBooks = async () => {
    if (!fileContent.value) {
        message.value = "请先上传有效的JSON文件";
        messageDialogVisible.value = true;
        return;
    }
    
    try {
        const response = await axios.post('/book/batch', {
            books: fileContent.value
        });
        if (response.data.message !== null) {
            message.value = response.data.message;
            messageDialogVisible.value = true;
        }
        if (response.data.ok) {
            importBookDialogVisible.value = false;
            fileContent.value = null;
            queryBook();
        }
    } catch (error) {
        message.value = "导入失败：" + (error.response?.data?.message || error.message);
        messageDialogVisible.value = true;
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
            <div style="margin-bottom: 10px;">
                <el-button type="primary" @click="addBookDialogVisible = true">添加图书</el-button>
                <el-button type="success" @click="importBookDialogVisible = true">批量导入</el-button>
            </div>

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
                <el-table-column label="操作" width="240">
                    <template #default="scope">
                        <el-space wrap>
                            <el-button 
                                type="primary" 
                                size="small" 
                                :icon="TopRight"
                                :disabled="scope.row.stock <= 0"
                                @click="toBorrowBook = {...scope.row}; borrowBookDialogVisible = true">
                                借阅
                            </el-button>
                            <el-button 
                                type="success" 
                                size="small" 
                                :icon="BottomLeft"
                                @click="toReturnBook = {...scope.row}; returnBookDialogVisible = true">
                                归还
                            </el-button>
                            <el-dropdown>
                                <el-button type="info" size="small" plain>
                                    管理
                                </el-button>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="toEditBook = {...scope.row}; editBookDialogVisible = true">
                                            <el-icon><Edit /></el-icon> 编辑信息
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="toAdjustStockBook = {...scope.row, deltaStock: 0}; adjustStockDialogVisible = true">
                                            <el-icon><Tickets /></el-icon> 调整库存
                                        </el-dropdown-item>
                                        <el-dropdown-item divided @click="toDeleteBook = scope.row; deleteBookDialogVisible = true">
                                            <el-icon><Delete /></el-icon> 删除
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </el-space>
                    </template>
                </el-table-column>
            </el-table>

        </div>

        <!-- 添加图书对话框 -->
        <BookDialog v-model="addBookDialogVisible" :book="newBook" @submit="addBook" />

        <!-- 编辑图书信息对话框 -->
        <BookDialog v-model="editBookDialogVisible" isEdit :book="toEditBook" :stockMode="false" @submit="editBook" />

        <!-- 调整库存对话框 -->
        <el-dialog v-model="adjustStockDialogVisible" title="调整库存" width="30%">
            <template v-if="toAdjustStockBook">
                <div style="margin-bottom: 10px;">图书：{{ toAdjustStockBook.title }}</div>
                <div style="margin-bottom: 10px;">当前库存：{{ toAdjustStockBook.stock }}</div>
                <el-form>
                    <el-form-item label="库存变化">
                        <el-input v-model="deltaStock" type="number" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="adjustStock">保存</el-button>
                        <el-button @click="adjustStockDialogVisible = false">取消</el-button>
                    </el-form-item>
                </el-form>
            </template>
        </el-dialog>

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

        <!-- 批量导入对话框 -->
        <el-dialog v-model="importBookDialogVisible" title="批量导入图书" width="30%">
            <el-form>
                <el-form-item label="选择文件">
                    <el-upload
                        action=""
                        :auto-upload="false"
                        accept=".json"
                        :limit="1"
                        :on-change="uploadFile"
                        ref="uploadRef">
                        <el-button type="primary">选择JSON文件</el-button>
                    </el-upload>
                </el-form-item>
                <div v-if="fileContent" style="margin-bottom: 10px;">
                    <p>文件已加载，共 {{ fileContent.length }} 条记录</p>
                </div>
                <el-form-item>
                    <el-button type="primary" @click="importBooks" :disabled="!fileContent">导入</el-button>
                    <el-button @click="importBookDialogVisible = false">取消</el-button>
                </el-form-item>
            </el-form>
        </el-dialog>

        <!-- 借书对话框 -->
        <el-dialog v-model="borrowBookDialogVisible" title="借阅图书" width="30%">
            <template v-if="toBorrowBook">
                <div style="margin-bottom: 10px;">图书：{{ toBorrowBook.title }}</div>
                <div style="margin-bottom: 10px;">作者：{{ toBorrowBook.author }}</div>
                <div style="margin-bottom: 10px;">库存：{{ toBorrowBook.stock }}</div>
                <el-form>
                    <el-form-item label="借书证ID">
                        <el-input v-model="toBorrowBook.cardId" type="number" placeholder="请输入借书证ID" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" :disabled="!toBorrowBook.cardId" @click="borrowBook">确认借阅</el-button>
                        <el-button @click="borrowBookDialogVisible = false">取消</el-button>
                    </el-form-item>
                </el-form>
            </template>
        </el-dialog>

        <!-- 还书对话框 -->
        <el-dialog v-model="returnBookDialogVisible" title="归还图书" width="30%">
            <template v-if="toReturnBook">
                <div style="margin-bottom: 10px;">图书：{{ toReturnBook.title }}</div>
                <div style="margin-bottom: 10px;">作者：{{ toReturnBook.author }}</div>
                <el-form>
                    <el-form-item label="借书证ID">
                        <el-input v-model="toReturnBook.cardId" type="number" placeholder="请输入借书证ID" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" :disabled="!toReturnBook.cardId" @click="returnBook">确认归还</el-button>
                        <el-button @click="returnBookDialogVisible = false">取消</el-button>
                    </el-form-item>
                </el-form>
            </template>
        </el-dialog>
    </el-scrollbar>


</template>
