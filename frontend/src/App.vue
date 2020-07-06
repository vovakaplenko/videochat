<template>
    <v-app>
        <!-- https://vuetifyjs.com/en/components/application/ -->
        <v-navigation-drawer
                left
                app
                :clipped="true"
                v-model="drawer"
        >
            <template v-slot:prepend>
                <v-list-item two-line v-if="currentUser">
                    <v-list-item-avatar>
                        <img :src="currentUser.avatar">
                    </v-list-item-avatar>

                    <v-list-item-content>
                        <v-list-item-title>{{currentUser.login}}</v-list-item-title>
                        <v-list-item-subtitle>Logged In</v-list-item-subtitle>
                    </v-list-item-content>
                </v-list-item>
            </template>

            <v-divider></v-divider>

            <v-list dense>
                <v-list-item
                        v-for="item in appBarItems"
                        :key="item.title"
                        @click="item.clickFunction"
                >
                    <v-list-item-icon>
                        <v-icon>{{ item.icon }}</v-icon>
                    </v-list-item-icon>

                    <v-list-item-content>
                        <v-list-item-title>{{ item.title }}</v-list-item-title>
                    </v-list-item-content>
                </v-list-item>
            </v-list>
        </v-navigation-drawer>


        <v-app-bar
                color="indigo"
                dark
                app
                :clipped-left="true"
        >
            <v-app-bar-nav-icon @click="toggleLeftNavigation"></v-app-bar-nav-icon>

            <v-btn icon @click="createChat">
                <v-icon>mdi-plus-circle-outline</v-icon>
            </v-btn>

            <v-spacer></v-spacer>
            <v-toolbar-title>Chats</v-toolbar-title>
            <v-spacer></v-spacer>

            <v-card light>
                <v-text-field prepend-icon="mdi-magnify" hide-details single-line v-model="searchChatString"></v-text-field>
            </v-card>
        </v-app-bar>


        <v-main>
            <v-container>
                <v-alert
                        dismissible
                        v-model="showAlert"
                        prominent
                        type="error"
                >
                    <v-row align="center">
                        <v-col class="grow">{{lastError}}</v-col>
                    </v-row>
                </v-alert>
                <LoginModal/>
                <ChatEdit/>
                <router-view/>
            </v-container>
        </v-main>
    </v-app>
</template>

<script>
    import axios from 'axios';
    import LoginModal from "./LoginModal";
    import {mapGetters} from 'vuex'
    import {FETCH_USER_PROFILE, GET_USER, UNSET_USER} from "./store";
    import bus, {CHAT_SEARCH_CHANGED, LOGGED_OUT, OPEN_CHAT_EDIT} from "./bus";
    import ChatEdit from "./ChatEdit";
    import debounce from "lodash/debounce";

    export default {
        data () {
            return {
                appBarItems: [
                    { title: 'Home', icon: 'mdi-home-city', clickFunction: ()=>{} },
                    { title: 'My Account', icon: 'mdi-account', clickFunction: ()=>{} },
                    { title: 'Logout', icon: 'mdi-logout', clickFunction: this.logout },
                ],
                drawer: true,
                lastError: "",
                showAlert: false,
                searchChatString: ""
            }
        },
        components:{
            LoginModal,
            ChatEdit
        },
        methods:{
            toggleLeftNavigation() {
                this.$data.drawer = !this.$data.drawer;
            },
            logout(){
                console.log("Logout");
                axios.post(`/api/logout`).then(({ data }) => {
                    this.$store.commit(UNSET_USER);
                    bus.$emit(LOGGED_OUT, null);
                });
            },
            onError(errText){
                this.showAlert = true;
                this.lastError = errText;
            },
            createChat() {
                bus.$emit(OPEN_CHAT_EDIT, null);
            },
            doSearch(searchString) {
                if (!searchString || searchString === "") {
                    bus.$emit(CHAT_SEARCH_CHANGED, "");
                    return;
                }

                bus.$emit(CHAT_SEARCH_CHANGED, searchString);
            },

        },
        computed: {
            ...mapGetters({currentUser: GET_USER}), // currentUser is here, 'getUser' -- in store.js
        },
        mounted() {
            this.$store.dispatch(FETCH_USER_PROFILE);
        },
        created() {
            this.doSearch = debounce(this.doSearch, 700);
        },
        watch: {
            searchChatString (searchString) {
                this.doSearch(searchString);
            },
        },

    }
</script>

<style lang="stylus">
    @import '~typeface-roboto/index.css'
</style>