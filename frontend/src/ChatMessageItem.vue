<template>
    <v-list-item
        :key="item.id"
        dense
    >
        <v-list-item-avatar v-if="item.owner && item.owner.avatar">
            <v-img :src="item.owner.avatar"></v-img>
        </v-list-item-avatar>
        <v-list-item-content @click="onMessageClick(item)">
            <v-list-item-subtitle>{{getSubtitle(item)}}</v-list-item-subtitle>
            <v-list-item-content class="pre-formatted pa-0">{{item.text}}</v-list-item-content>
        </v-list-item-content>
        <v-list-item-action>
            <v-container class="mb-0 mt-0 pb-0 pt-0">
                <v-icon class="mr-4" v-if="item.canEdit" color="error" @click="deleteMessage(item)" dark small>mdi-delete</v-icon>
                <v-icon v-if="item.canEdit" color="primary" @click="editMessage(item)" dark small>mdi-lead-pencil</v-icon>
            </v-container>
        </v-list-item-action>
    </v-list-item>
</template>

<script>
import bus, {SET_EDIT_MESSAGE} from "./bus";
import axios from "axios";

export default {
    name: 'item-component',
    props: {
        item: {
            type: Object
        },
        chatId: {

        }
    },
    methods: {
        getSubtitle(item) {
            return `${item.owner.login} at ${item.createDateTime}`
        },
        editMessage(dto){
            const editMessageDto = {id: dto.id, text: dto.text};
            bus.$emit(SET_EDIT_MESSAGE, editMessageDto);
        },
        onMessageClick(dto) {
            axios.put(`/api/chat/${this.chatId}/message/read/${dto.id}`);
        },
        deleteMessage(dto){
            axios.delete(`/api/chat/${this.chatId}/message/${dto.id}`)
        },

    }
}
</script>