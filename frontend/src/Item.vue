<template>
    <div class="item-inner">
        <div class="head">
            <span class="index"># {{ source.id }}</span>
        </div>
        <div class="desc">{{ source.text }}</div>
    </div>
</template>

<script>

export default {
    name: 'infinite-loading-item',

    props: {
        source: {
            type: Object,
            default () {
                return {}
            }
        }
    }
}
</script>

<style lang="stylus" scoped>
.item-inner {
    .head {
        font-weight: 500;
    }
    .index {
        margin-right: 1em;
    }
    .name {
        margin-left: 1em;
    }
    .desc {
        padding-top: .5em;
        text-align: justify;
    }
}
</style>