package name.nkonev.video.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import name.nkonev.video.GroupCallApp;
import name.nkonev.video.dto.in.JoinRoomDto;
import name.nkonev.video.dto.out.ExistsParticipantsDto;
import name.nkonev.video.dto.out.Typed;
import name.nkonev.video.service.ChatRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = GroupCallApp.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = MockMvcPrint.LOG_DEBUG)
public class CallHandlerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatRequestService chatRequestService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CallHandlerTest.class);

    @Test
    public void testJoin() throws Exception {

        String sessionId = UUID.randomUUID().toString();

        Mockito.doNothing().when(chatRequestService).sendToWebsocketForSession(Mockito.eq(sessionId), Mockito.any(Typed.class));

        JoinRoomDto joinRoomDto = new JoinRoomDto(sessionId, 1L);

        mockMvc.perform(
                post("/joinRoom")
                        .content(objectMapper.writeValueAsString(joinRoomDto))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andDo(mvcResult -> {
            LOGGER.info(mvcResult.getResponse().getContentAsString());
            Mockito.verify(chatRequestService).sendToWebsocketForSession(Mockito.eq(sessionId), Mockito.eq(new ExistsParticipantsDto(Collections.emptyList())));
        });
    }
}
