package name.nkonev.video.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import name.nkonev.video.dto.out.IceCandidateDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kurento.client.IceCandidate;

public class IceCandidateDtoTest {

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        IceCandidate iceCandidate  = new IceCandidate("candidate1", "sdpMid1", 1);
        IceCandidateDto iceCandidateDto = new IceCandidateDto("ses1", iceCandidate);
        final String s = objectMapper.writeValueAsString(iceCandidateDto);
        Assertions.assertEquals("{\"userSessionId\":\"ses1\",\"candidate\":{\"candidate\":\"candidate1\",\"sdpMid\":\"sdpMid1\",\"sdpMLineIndex\":1},\"type\":\"iceCandidate\"}", s);
    }
}
