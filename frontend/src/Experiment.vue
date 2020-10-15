<template>
    <v-card
        flat
        class="py-12"
    >
        <v-card-text>
            <v-row
                align="center"
                justify="center"
            >
                <v-col cols="12">
                    <p class="text-center">
                        {{ toggle_exclusive }}
                    </p>
                    <video playsInline :poster="img"></video>
                </v-col>
                <v-btn-toggle
                    v-model="toggle_exclusive"
                    mandatory
                >
                    <v-btn>
                        <v-icon>mdi-format-align-left</v-icon>
                    </v-btn>
                    <v-btn>
                        <v-icon>mdi-format-align-center</v-icon>
                    </v-btn>
                    <v-btn>
                        <v-icon>mdi-format-align-right</v-icon>
                    </v-btn>
                </v-btn-toggle>
            </v-row>
        </v-card-text>
    </v-card>
</template>

<script>
export default {
    data () {
        return {
            toggle_exclusive: undefined,
            //lolo: "https://cdn3.iconfinder.com/data/icons/england-flat/340/detective_sherlock_holmes_glass_magnifying_mystery_pipe-512.png"
        }
    },
    computed: {
        img() {
            switch (this.toggle_exclusive) {
                case 0:
                    return "https://cdn3.iconfinder.com/data/icons/england-flat/340/detective_sherlock_holmes_glass_magnifying_mystery_pipe-512.png"
                case 1:
                    return "https://cdn3.iconfinder.com/data/icons/england-flat/340/knight_medieval_horseback_armor_horse_history_warrior_lance-512.png"
                case 2:
                    return "https://cdn3.iconfinder.com/data/icons/england-flat/340/arthur_king_sword_excalibur_medieval_knight_middle-256.png"
            }
        }
    }
}
</script>