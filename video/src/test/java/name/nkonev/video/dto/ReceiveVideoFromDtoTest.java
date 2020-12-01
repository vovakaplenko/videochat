package name.nkonev.video.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import name.nkonev.video.dto.in.ReceiveVideoFromDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReceiveVideoFromDtoTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:/receive.json")
    private Resource data;

    @Test
    public void testDeserialization() throws IOException {
        try(InputStream is = data.getInputStream();) {
            final ReceiveVideoFromDto receiveVideoFromDto = objectMapper.readValue(is, ReceiveVideoFromDto.class);
            System.out.println(receiveVideoFromDto);
            Assertions.assertTrue(receiveVideoFromDto.getSdpOffer().toString().contains("THIS_IS_SDPARTA"));
        }
    }
}
