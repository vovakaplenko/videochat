<template>
    <v-container class="ma-0 pa-0" id="chatViewContainer" fluid>
        <splitpanes class="default-theme" horizontal style="height: 100%">
            <pane v-if="isAllowedVideo()">
                <ChatVideo :chatDto="chatDto"/>
            </pane>
            <pane max-size="90" size="80">
                    <virtual-list style="overflow-y: auto; height: 100%"
                        ref="vsl"
                        :data-key="'id'"
                        :data-sources="items"
                        :data-component="itemComponent"
                        :estimate-size="70"
                        :extra-props="{chatId: chatId}"
                        v-on:totop="infiniteHandler"
                    >
                        <div slot="footer" class="loader"></div>
                    </virtual-list>
            </pane>
            <pane max-size="70" size="20">
                <MessageEdit :chatId="chatId"/>
            </pane>
        </splitpanes>
    </v-container>
</template>

<script>
    import axios from "axios";
    import {
        findIndex,
        pageSize, replaceInArray
    } from "./InfinityListMixin";
    import Vue from 'vue'
    import bus, {
      CHANGE_PHONE_BUTTON,
      CHANGE_TITLE, CHAT_DELETED,
      CHAT_EDITED,
      MESSAGE_ADD,
      MESSAGE_DELETED,
      MESSAGE_EDITED,
      USER_TYPING,
      VIDEO_LOCAL_ESTABLISHED
    } from "./bus";
    import {phoneFactory, titleFactory} from "./changeTitle";
    import MessageEdit from "./MessageEdit";
    import {root_name, videochat_name} from "./routes";
    import ChatVideo from "./ChatVideo";
    import {getData, getProperData} from "./centrifugeConnection";
    import {mapGetters} from "vuex";
    import {GET_USER} from "./store";
    import { Splitpanes, Pane } from 'splitpanes'
    import 'splitpanes/dist/splitpanes.css'
    import VirtualList from 'vue-virtual-scroll-list'
    import ChatMessageItem from "./ChatMessageItem"

    export default {
        data() {
            return {
                page: 0,
                items: [],
                itemsTotal: 0,
                itemComponent: ChatMessageItem,

                chatMessagesSubscription: null,
                chatDto: {
                    participantIds:[]
                },
            }
        },
        computed: {
            chatId() {
                return this.$route.params.id
            },
            pageHeight () {
                return document.body.scrollHeight
            },
            ...mapGetters({currentUser: GET_USER})
        },
        methods: {
            // not working until you will change this.items list
            reloadItems() {
                // TODO implement
                console.warn("TODO implement reloadItems()");
            },
            searchStringChanged() {
                this.items = [];
                this.page = 0;
                this.reloadItems();
            },



            addItem(dto) {
                console.log("Adding item", dto);
                this.items.push(dto);
                this.$forceUpdate();
            },
            changeItem(dto) {
                console.log("Replacing item", dto);
                replaceInArray(this.items, dto);
                this.$forceUpdate();
            },
            removeItem(dto) {
                console.log("Removing item", dto);
                const idxToRemove = findIndex(this.items, dto);
                this.items.splice(idxToRemove, 1);
                this.$forceUpdate();
            },


            onVideoChangesHeight() {
                console.log("Adjusting height after video has been shown");
                this.$forceUpdate();
            },

            infiniteHandler() {
                axios.get(`/api/chat/${this.chatId}/message`, {
                    params: {
                        page: this.page,
                        size: pageSize,
                        reverse: true
                    },
                }).then(({ data }) => {
                    const list = data;
                    if (list.length) {
                        this.page += 1;
                        this.items.unshift(...list.reverse());
                    }
                });
            },

            onNewMessage(dto) {
                if (dto.chatId == this.chatId) {
                    this.addItem(dto);
                    this.scrollDown();
                } else {
                    console.log("Skipping", dto)
                }
            },
            onDeleteMessage(dto) {
                if (dto.chatId == this.chatId) {
                    this.removeItem(dto);
                } else {
                    console.log("Skipping", dto)
                }
            },
            onEditMessage(dto) {
                if (dto.chatId == this.chatId) {
                    this.changeItem(dto);
                } else {
                    console.log("Skipping", dto)
                }
            },
            scrollDown() {
                Vue.nextTick(()=>{
                    var myDiv = document.getElementById("messagesScroller");
                    console.log("myDiv.scrollTop", myDiv.scrollTop, "myDiv.scrollHeight", myDiv.scrollHeight);
                    myDiv.scrollTop = myDiv.scrollHeight;
                });
            },
            getInfo() {
                return axios.get(`/api/chat/${this.chatId}`).then(({ data }) => {
                    console.log("Got info about chat", data);
                    bus.$emit(CHANGE_TITLE, titleFactory(data.name, false, data.canEdit, data.canEdit ? this.chatId: null));
                    this.chatDto = data;
                });
            },
            onChatChange(dto) {
                if (dto.id == this.chatId) {
                    this.getInfo()
                }
            },
            onChatDelete(dto) {
                this.$router.push(({ name: root_name}))
            },
            isAllowedVideo() {
                return this.currentUser && this.$router.currentRoute.name == videochat_name && this.chatDto && this.chatDto.participantIds && this.chatDto.participantIds.length
            },

        },
        mounted() {
            this.chatMessagesSubscription = this.centrifuge.subscribe("chatMessages"+this.chatId, (message) => {
                // actually it's used for tell server about presence of this client.
                // also will be used as a global notification, so we just log it
                const data = getData(message);
                console.debug("Got global notification", data);
                const properData = getProperData(message)
                if (data.type === "user_typing") {
                    bus.$emit(USER_TYPING, properData);
                }
            });

            bus.$emit(CHANGE_TITLE, titleFactory(`Chat #${this.chatId}`, false, true, null));

            this.getInfo();
            bus.$emit(CHANGE_PHONE_BUTTON, phoneFactory(true, true))
            bus.$on(MESSAGE_ADD, this.onNewMessage);
            bus.$on(MESSAGE_DELETED, this.onDeleteMessage);
            bus.$on(CHAT_EDITED, this.onChatChange);
            bus.$on(CHAT_DELETED, this.onChatDelete);
            bus.$on(MESSAGE_EDITED, this.onEditMessage);
            bus.$on(VIDEO_LOCAL_ESTABLISHED, this.onVideoChangesHeight);

            this.infiniteHandler();
        },
        beforeDestroy() {
            bus.$off(MESSAGE_ADD, this.onNewMessage);
            bus.$off(MESSAGE_DELETED, this.onDeleteMessage);
            bus.$off(CHAT_EDITED, this.onChatChange);
            bus.$off(CHAT_DELETED, this.onChatDelete);
            bus.$off(MESSAGE_EDITED, this.onEditMessage);
            bus.$off(VIDEO_LOCAL_ESTABLISHED, this.onVideoChangesHeight);

            this.chatMessagesSubscription.unsubscribe();
        },
        destroyed() {
            bus.$emit(CHANGE_PHONE_BUTTON, phoneFactory(false))
        },
        components: {
            MessageEdit,
            ChatVideo,
            Splitpanes, Pane,
            'virtual-list': VirtualList
        }
    }
</script>

<style scoped lang="stylus">
    .pre-formatted {
      white-space pre-wrap
    }

    #chatViewContainer {
        height: calc(100vh - 100px)
    }

</style>