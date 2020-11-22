
package name.nkonev.kurento;

import name.nkonev.ChatRequestService;
import name.nkonev.entity.RoomEntity;
import name.nkonev.repository.RoomRepository;
import okhttp3.OkHttpClient;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.internal.server.KurentoServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Optional;

@Component
public class RoomManager {

  private final Logger log = LoggerFactory.getLogger(RoomManager.class);

  @Autowired
  private KurentoClient kurento;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private ChatRequestService chatRequestService;

  public Room getRoom(Long roomId, boolean create) {
    log.debug("Searching for room {}", roomId);
    RoomEntity room = roomRepository.findById(roomId).orElse(null);

    if (room == null) {
      if (!create) { return null; }
      log.debug("Room {} not existent. Will create now!", roomId);
      room = new RoomEntity(roomId, new HashSet<>());
      room = roomRepository.save(room);
    }
    log.debug("Room {} found!", roomId);
    MediaPipeline mediaPipeline = kurento.getById(room.getMediaPipelineId(), MediaPipeline.class);
    if (!checkMediaPipeline(mediaPipeline)) {
      mediaPipeline = getMediaPipeline();
      room.setMediaPipelineId(mediaPipeline.getId());
      room = roomRepository.save(room);
    }
    return new Room(room, mediaPipeline, chatRequestService);
  }

  /**
   * Removes a room from the list of available rooms.
   *
   * @param room
   *          the room to be removed
   */
  public void removeRoom(Room room) {
    room.close();
    log.info("Room {} removed and closed", room.getRoomId());
    roomRepository.deleteById(room.getRoomEntity().getRoomId());
  }


  private MediaPipeline getMediaPipeline() {
    var newMediaPipeline = kurento.createMediaPipeline();
    log.info("Created new MediaPipeline with id: {}", newMediaPipeline.getId());
    return newMediaPipeline;
  }

  private boolean checkMediaPipeline(MediaPipeline gotMediaPipeline) {
    try {
      gotMediaPipeline.getLatencyStats();
      log.info("Successfully got exists MediaPipeline by id: {}", gotMediaPipeline.getId());
      return true;
    } catch (KurentoServerException e) {
      if (e.getCode() == 40101) {
        log.info("MediaPipeline by id: {} not found, will create new", gotMediaPipeline.getId());
        return false;
      }
      throw e;
    }
  }
}
