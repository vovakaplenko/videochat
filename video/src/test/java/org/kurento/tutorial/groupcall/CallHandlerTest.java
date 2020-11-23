package org.kurento.tutorial.groupcall;

import com.google.gson.JsonObject;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = GroupCallApp.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = MockMvcPrint.LOG_DEBUG)
public class CallHandlerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    private ChatRequestService chatRequestService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CallHandlerTest.class);

    @Test
    public void testJoin() throws Exception {

        String sessionId = UUID.randomUUID().toString();

        Mockito.doNothing().when(chatRequestService).sendToWebsocketForSession(Mockito.eq(sessionId), Mockito.any(JsonObject.class));

        mockMvc.perform(
                post("/joinRoom")
                        .param("userSessionId", sessionId)
                        .param("roomId", "1")
        ).andDo(mvcResult -> {
            LOGGER.info(mvcResult.getResponse().getContentAsString());
            Mockito.verify(chatRequestService).sendToWebsocketForSession(Mockito.eq(sessionId), Mockito.any(JsonObject.class));
        });
    }
}
