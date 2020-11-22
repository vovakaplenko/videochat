package name.nkonev.kurento;

import org.kurento.client.KurentoClient;
import org.kurento.tutorial.groupcall.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;

@RestController
public class CallController {

    private final Logger log = LoggerFactory.getLogger(CallController.class);

    @Autowired
    private RoomManager roomManager;

    @PostConstruct
    public void pc() {
        log.info("RoomService <<<");
    }

    @PostMapping("/joinRoom")
    public void joinRoom(@RequestParam String userSessionId, @RequestParam Long roomId) throws IOException {
        joinRoomInternal(userSessionId, roomId);
    }

    @PostMapping("/receiveVideoFrom")
    public void receiveVideoFrom() {

    }

    @PostMapping("/leaveRoom")
    public void leaveRoom(@RequestParam String userSessionId, @RequestParam Long roomId) throws IOException {
        leaveRoomInternal(userSessionId, roomId);
    }

    @PostMapping("/onIceCandidate")
    public void onIceCandidate() {

    }

    /**
     * room with given id will be created if it is not exists
     * @param sessionId
     * @param roomId
     * @throws IOException
     */
    private void joinRoomInternal(String sessionId, Long roomId) throws IOException {
        log.info("PARTICIPANT {}: trying to join room {}", sessionId, roomId);

        Room room = roomManager.getRoom(roomId, true);
        room.join(sessionId);
    }

    private void leaveRoomInternal(String userSessionId, Long roomId) throws IOException {
        final Room room = roomManager.getRoom(roomId, false);
        if (room == null) {
            log.warn("Room {} is not exists", roomId);
            return;
        }
        room.leave(userSessionId);
        if (room.getParticipants().isEmpty()) {
            roomManager.removeRoom(room);
        }
    }

}
