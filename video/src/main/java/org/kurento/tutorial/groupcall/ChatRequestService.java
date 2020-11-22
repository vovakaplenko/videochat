package org.kurento.tutorial.groupcall;

import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatRequestService {
    @Autowired
    private OkHttpClient okHttpClient;

    public void sendToWebsocketForSession(String sessionId, JsonObject data) {

    }

    public void sendToWebsocketForSessionMulticast(Long roomId, JsonObject data) {

    }
}
