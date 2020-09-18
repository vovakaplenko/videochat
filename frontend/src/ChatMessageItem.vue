<template>
    <v-list-item
        :key="source.id"
        dense
    >
        <v-list-item-avatar v-if="source.owner && source.owner.avatar">
            <v-img :src="source.owner.avatar"></v-img>
        </v-list-item-avatar>
        <v-list-item-content @click="onMessageClick(source)">
            <v-list-item-subtitle>{{getSubtitle(source)}}</v-list-item-subtitle>
            <v-list-item-content class="pre-formatted pa-0">{{source.text}}</v-list-item-content>
        </v-list-item-content>
        <v-list-item-action>
            <v-container class="mb-0 mt-0 pb-0 pt-0">
                <v-icon class="mr-4" v-if="source.canEdit" color="error" @click="deleteMessage(source)" dark small>mdi-delete</v-icon>
                <v-icon v-if="source.canEdit" color="primary" @click="editMessage(source)" dark small>mdi-lead-pencil</v-icon>
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
        index: { // index of current item
            type: Number
        },
        source: { // here is: {uid: 'unique_1', text: 'abc'}
            type: Object,
            default () {
                return {}
            }
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