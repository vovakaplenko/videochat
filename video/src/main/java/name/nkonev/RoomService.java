package name.nkonev;

import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.internal.server.KurentoServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class RoomService {

    private final Logger log = LoggerFactory.getLogger(RoomService.class);

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private KurentoClient kurento;

    @PostConstruct
    public void pc() {
        log.info("RoomService <<<");
    }

    private MediaPipeline getMediaPipeline(String roomName) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(roomName))
                .map(Object::toString)
                .map(id -> {
                    MediaPipeline gotMediaPipeline = kurento.getById(id, MediaPipeline.class);
                    try {
                        gotMediaPipeline.getLatencyStats();
                        log.info("Successfully got exists MediaPipeline by id: {}", id);
                        return gotMediaPipeline;
                    } catch (KurentoServerException e) {
                        if (e.getCode() == 40101) {
                            log.info("MediaPipeline by id: {} not found, creating new", id);
                            return null;
                        }
                        throw e;
                    }
                })
                .orElseGet(()->{
                    MediaPipeline newMediaPipeline = kurento.createMediaPipeline();
                    redisTemplate.opsForValue().set(roomName, newMediaPipeline.getId());
                    log.info("Created new MediaPipeline with id: {}", newMediaPipeline.getId());
                    return newMediaPipeline;
                });
    }
}
