package name.nkonev.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RedisHash
public class RoomEntity {
    @Id
    private Long roomId;
    private String mediaPipelineId;
    private Set<String> participants = new HashSet<>();

    public RoomEntity(Long roomId, Set<String> participants) {
        this.roomId = roomId;
        this.participants = participants;
    }

    public RoomEntity() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<String> participants) {
        this.participants = participants;
    }

    public String getMediaPipelineId() {
        return mediaPipelineId;
    }

    public void setMediaPipelineId(String mediaPipelineId) {
        this.mediaPipelineId = mediaPipelineId;
    }
}
