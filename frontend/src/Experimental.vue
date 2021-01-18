<template>
    <virtual-list
        class="list-infinite"
        :data-key="'id'"
        :data-sources="items"
        :data-component="itemComponent"
        :estimate-size="20"
        v-on:totop="onScrollToTop"
        v-on:tobottom="onScrollToBottom"
    >
        <div slot="footer" class="loading-spinner">Loading ...</div>
    </virtual-list>
</template>

<script>
    import VirtualList from "vue-virtual-scroll-list"
    import axios from "axios";
    import Item from './Item'

    const fetchMessages = (currentPage) => {
        return axios.get(`/api/chat/1/message`, {
            params: {
                page: currentPage,
                size: 20,
                //reverse: true
            },
        })
    }

    const getPageData2 = (currentPage) => {
        return fetchMessages(currentPage).then(({ data }) => {
            console.log("New arr2", data)
            return data;
        })
    }

    const initPageData = () => {
        const arr = [];
        fetchMessages(0).then(({ data }) => {
            arr.push(...data);
            console.log("New arr", arr)
        })
        return arr;
    }

    export default {
        data () {
            return {
                itemComponent: Item,
                page: 0,
                items: initPageData()
            }
        },

        methods: {
            onScrollToBottom() {
                console.log("On scroll to bottom")
                getPageData2(++this.page).then(value => {
                    this.items = this.items.concat(value);
                })
            },
            onScrollToTop () {
                console.log('at top')
            },

        },
        components: {
            VirtualList
        }
    }
</script>

<style lang="stylus">
.list-infinite {
    width: 100%;
    height: 500px;
    border: 2px solid;
    border-radius: 3px;
    overflow-y: auto;
    border-color: dimgray;
    position: relative;
}
</style>