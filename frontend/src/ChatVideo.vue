<template>
    <v-col cols="12" class="ma-0 pa-0" id="video-container">
        <!--<div class="video-container-element video-container-element-my">
            <video id="localVideo" autoPlay playsInline></video>
            <p class="video-container-element-caption">{{ currentUser.login }}</p>
        </div>
        <div class="video-container-element" v-for="(item, index) in properParticipants" :key="item.id">
            <video :id="getRemoteVideoId(item.id)" autoPlay playsInline :class="otherParticipantsClass" :poster="getAvatar(item)"></video>
            <p class="video-container-element-caption">{{ getLogin(item) }}</p>
        </div>-->
    </v-col>
</template>

<script>
    import {mapGetters} from "vuex";
    import {GET_USER} from "./store";
    import bus, {CHANGE_PHONE_BUTTON} from "./bus";
    import {phoneFactory} from "./changeTitle";
    import axios from "axios";
    import Participant from "./ChatVideoParticipant"
    import {WebRtcPeer} from "kurento-utils"

    const NOTIFY_ABOUT_JOIN = 'notifyAboutJoin';
    const LEAVE_ROOM = 'notifyAboutJoin';

    // input events
    const VIDEO_EXISTING_PARTICIPANTS = 'videoExistingParticipants'
    const VIDEO_NEW_PARTICIPANT_ARRIVED = 'videoNewParticipantArrived'
    const VIDEO_ICE_CANDIDATE = 'videoIceCandidate'
    const VIDEO_PARTICIPANT_LEFT = 'videoParticipantLeft'
    const VIDEO_RECEIVE_VIDEO_ANSWER = 'videoReceiveVideoAnswer'

    export default {
        data() {
            return {
                prevVideoPaneSize: null,

                pcConfig: null,

                participants: {},
                container: null,
            }
        },
        props: ['chatDto'],
        computed: {
            chatId() {
                const v = this.$route.params.id
                return parseInt(v);
            },
            ...mapGetters({currentUser: GET_USER}),
        },
        methods: {
            getWebRtcConfiguration() {
                const localPcConfig = {
                    iceServers: []
                };
                axios.get("/api/chat/public/webrtc/config").then(({data}) => {
                    for (const srv of data) {
                        localPcConfig.iceServers.push({
                            'urls': srv
                        });
                        this.pcConfig = localPcConfig;
                        console.log("Configured WebRTC servers", this.pcConfig);
                    }
                    this.registerMe();
                })
            },
            getLogin(participant) {
                return participant.login;
            },
            getAvatar(participant) {
                return participant.avatar;
            },

            registerMe() {
                var message = {
                    type : 'joinRoom',
                }
                this.sendMessage(message);
            },
            sendMessage(message) {
                message = {...message, chatId: this.chatId};
                console.debug('Sending message: ' + JSON.stringify(message));
                this.centrifuge.rpc(message).then(function(data){
                    console.debug("RPC response data: " + JSON.stringify(data));
                }, function(err) {
                    console.debug("RPC error: " + JSON.stringify(err));
                });
            },
            onExistingParticipants(msg) {
                const constraints = {
                    audio : true,
                    video : {
                        mandatory : {
                            maxWidth : 320,
                            maxFrameRate : 15,
                            minFrameRate : 15
                        }
                    }
                };
                const name = this.currentUser.id;
                console.log(name + " registered in room " + this.chatId);
                const participant = new Participant(name, this.sendMessage, this.container);
                this.participants[name] = participant;
                const video = participant.getVideoElement();

                const options = {
                    localVideo: video,
                    mediaConstraints: constraints,
                    onicecandidate: participant.onIceCandidate.bind(participant)
                }
                participant.rtcPeer = new WebRtcPeer.WebRtcPeerSendonly(options,
                    function (error) {
                        if (error) {
                            return console.error(error);
                        }
                        this.generateOffer(participant.offerToReceiveVideo.bind(participant));
                    });
                bus.$emit(CHANGE_PHONE_BUTTON, phoneFactory(true, false));
                this.sendMessage({type: NOTIFY_ABOUT_JOIN})
                msg.participantSessions.forEach(this.receiveVideo);
            },
            receiveVideo(sender) {
                console.log("receive video for participant", sender)
                let maybeParticipant = this.participants[sender];
                if (!maybeParticipant) {
                    maybeParticipant = new Participant(sender, this.sendMessage, this.container);
                    this.participants[sender] = maybeParticipant;
                } else {
                    console.warn("Receiving video from myself isn't allowed, return")
                    return
                }
                var participant = maybeParticipant;
                var video = participant.getVideoElement();

                var options = {
                    remoteVideo: video,
                    onicecandidate: participant.onIceCandidate.bind(participant)
                }
                participant.rtcPeer = new WebRtcPeer.WebRtcPeerRecvonly(options,
                    function (error) {
                        if(error) {
                            return console.error(error);
                        }
                        this.generateOffer(participant.offerToReceiveVideo.bind(participant));
                    });
            },
            onNewParticipantArrived(request) {
                console.debug("User "+request.userSessionId + " arrived")
                this.receiveVideo(request.userSessionId);
            },
            receiveVideoResponse(result) {
                const maybeParticipant = this.participants[result.userSessionId]
                if (!maybeParticipant) {
                    console.warn("userSessionId " + result.userSessionId + " for receiveVideoResponse still not present", this.participants)
                } else {
                    maybeParticipant.rtcPeer.processAnswer(result.sdpAnswer, function (error) {
                        if (error) return console.error(error);
                    });
                }
            },
            handleIceCandidate(parsedMessage) {
                const maybeParticipant = this.participants[parsedMessage.userSessionId];
                if (!maybeParticipant) {
                    console.warn("userSessionId " + parsedMessage.userSessionId + " for handleIceCandidate still not present", this.participants)
                } else {
                    maybeParticipant.rtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
                        if (error) {
                            console.error("Error adding candidate: " + error);
                            return;
                        }
                    });
                }
            },
            onParticipantLeft(request) {
                console.log('Participant ' + request.userSessionId + ' left');
                var participant = this.participants[request.userSessionId];
                participant.dispose();
                delete this.participants[request.userSessionId];
            },
            hangup(){
                this.sendMessage({
                    type : LEAVE_ROOM
                });

                for(const key in this.participants) {
                    this.participants[key].dispose();
                }
            }
        },

        mounted() {
            // this.localVideo = document.querySelector('#localVideo');
            this.container = document.getElementById('video-container');

            /*
             * https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Connectivity
             * https://www.html5rocks.com/en/tutorials/webrtc/basics/
             * https://codelabs.developers.google.com/codelabs/webrtc-web/#4
             * https://habr.com/ru/company/Voximplant/blog/417869/
             */
            bus.$on(VIDEO_EXISTING_PARTICIPANTS, this.onExistingParticipants);
            bus.$on(VIDEO_NEW_PARTICIPANT_ARRIVED, this.onNewParticipantArrived)
            bus.$on(VIDEO_RECEIVE_VIDEO_ANSWER, this.receiveVideoResponse);
            bus.$on(VIDEO_ICE_CANDIDATE, this.handleIceCandidate);
            bus.$on(VIDEO_PARTICIPANT_LEFT, this.onParticipantLeft);

            this.getWebRtcConfiguration();
        },

        beforeDestroy() {
            console.log("Cleaning up");
            this.hangup();
            bus.$emit(CHANGE_PHONE_BUTTON, phoneFactory(true, true));

            this.prevVideoPaneSize=null;
            this.pcConfig = null;
            this.participants = {};

            bus.$off(VIDEO_EXISTING_PARTICIPANTS, this.onExistingParticipants);
            bus.$off(VIDEO_NEW_PARTICIPANT_ARRIVED, this.onNewParticipantArrived);
            bus.$off(VIDEO_RECEIVE_VIDEO_ANSWER, this.receiveVideoResponse);
            bus.$off(VIDEO_ICE_CANDIDATE, this.handleIceCandidate);
            bus.$off(VIDEO_PARTICIPANT_LEFT, this.onParticipantLeft);
        },

    }
</script>

<style scoped lang="stylus">
    #video-container {
        display: flex;
        flex-direction: row;
        overflow-x: auto;
        overflow-y: hidden;
        height 100%
    }

    .video-container-element {
        display flex
        flex-direction column
        object-fit: scale-down;
        height 100% !important
        width 100% !important
    }

    .video-container-element-my {
        background #b3e7ff
    }

    .video-container-element:nth-child(even) {
        background #d5fdd5;
    }

    video {
        //object-fit: scale-down;
        //width 100% !important
        height 100% !important // todo its
    }

    .video-container-element-caption {
        top -1.8em
        left 2em
        text-shadow: -2px 0 white, 0 2px white, 2px 0 white, 0 -2px white;
        position: relative;
    }
</style>