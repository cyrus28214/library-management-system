<script setup>
import { computed } from 'vue';

const props = defineProps({
    book: Object,
    isEdit: Boolean
});

const model = defineModel();

// 定义emit
const emit = defineEmits(['submit']);

// 表单验证
const formValid = computed(() => 
    props.book?.title &&
    props.book?.category &&
    props.book?.press &&
    props.book?.publishYear &&
    props.book?.author &&
    props.book?.price &&
    props.book?.stock
);

// 关闭对话框
const handleClose = () => {
    model.value = false;
};

// 提交表单
const handleSubmit = () => {
    emit('submit', props.book);
};
</script>

<template>
    <el-dialog 
        v-model="model"
        :title="isEdit ? '编辑图书' : '添加图书'"
        @update:visible="handleClose" 
        width="30%"
    >
        <el-form :model="book" label-width="120px">
            <el-form-item label="图书名称">
                <el-input v-model="book.title" />
            </el-form-item>
            <el-form-item label="图书类别">
                <el-input v-model="book.category" />
            </el-form-item>
            <el-form-item label="出版社">
                <el-input v-model="book.press" />
            </el-form-item>
            <el-form-item label="出版年份">
                <el-input v-model="book.publishYear" type="number" />
            </el-form-item>
            <el-form-item label="作者">
                <el-input v-model="book.author" />
            </el-form-item>
            <el-form-item label="价格">
                <el-input v-model="book.price" type="number" />
            </el-form-item>
            <el-form-item label="库存">
                <el-input v-model="book.stock" type="number" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="handleSubmit" :disabled="!formValid">
                    {{ isEdit ? '保存' : '添加' }}
                </el-button>
                <el-button @click="handleClose">取消</el-button>
            </el-form-item>
        </el-form>
    </el-dialog>
</template> 