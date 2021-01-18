<template>
    <v-virtual-scroll
        :items="items"
        @scroll.native="scrolling"
    />
</template>

<script>
import axios from "axios";
import debounce from "lodash/debounce";

let loading = false;
const fetchMessages = (currentPage) => {
    loading = true;
    return axios.get(`/api/chat/1/message`, {
        params: {
            page: currentPage,
            size: 20,
            reverse: true
        },
    }).then(({ data }) => {
        return data.reverse();
    }).finally(() => {
        loading = false;
    })
}

const getPageData2 = (currentPage) => {
    if (loading) { return Promise.resolve() }
    return fetchMessages(currentPage).then(( data ) => {
        console.log("New arr2", data)
        return data;
    })
}

const initPageData = () => {
    const arr = [];
    fetchMessages(0).then(( data ) => {
        arr.unshift(...data);
        console.log("New arr", arr)
    })
    return arr;
}

export default {
    data () {
        return {
            page: 1,
            items: initPageData()
        }
    },

    methods: {
        onScrollToBottom() {
            console.log("On scroll to bottom")

        },
        onScrollToTop () {
            console.log('at top');
            getPageData2(this.page).then(value => {
                if (value) {
                    this.items = value.concat(this.items);
                    this.page++;
                }
            })
        },
        scrolling (event) {
            const element = event.currentTarget || event.target
            if (element && element.scrollHeight - element.scrollTop === element.clientHeight) {
                this.$emit('scroll-end')
            }
        },
    },
    created () {
        this.scrolling = debounce(this.scrolling, 200)
    },
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