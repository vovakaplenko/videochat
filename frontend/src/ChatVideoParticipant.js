
function Participant(name, sendMessageFunction, containerRootElement) {
    console.debug("Creating participant", name);
    var container = document.createElement('div');
    container.id = 'video-container-item-'+name;
    var span = document.createElement('span');
    var video = document.createElement('video');
    var rtcPeer; // see line 80

    container.appendChild(video);
    container.appendChild(span);
    containerRootElement.appendChild(container);

    span.appendChild(document.createTextNode(name));

    video.id = 'video-' + name;
    video.autoplay = true;
    video.controls = false;

    this.getVideoElement = function() {
        return video;
    }

    this.offerToReceiveVideo = function(error, offerSdp, wp){
        if (error) return console.error ("sdp offer error")
        console.debug('Invoking SDP offer callback function');
        var msg =  { type : "receiveVideoFrom",
            senderSessionId : name,
            sdpOffer : offerSdp
        };
        sendMessageFunction(msg);
    }


    this.onIceCandidate = function (candidate, wp) {
        console.debug("Local candidate" + JSON.stringify(candidate));

        var message = {
            type: 'onIceCandidate',
            candidate: candidate,
            fromUserSessionId: name
        };
        sendMessageFunction(message);
    }

    Object.defineProperty(this, 'rtcPeer', { writable: true});

    this.dispose = function() {
        console.log('Disposing participant ' + name);
        this.rtcPeer.dispose();
        container.parentNode.removeChild(container);
    };
}

export default Participant;