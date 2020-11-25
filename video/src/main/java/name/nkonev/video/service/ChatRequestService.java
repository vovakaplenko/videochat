package name.nkonev.video.service;

import name.nkonev.video.dto.out.Typed;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatRequestService {
    @Autowired
    private OkHttpClient okHttpClient;

    public void sendToWebsocketForSession(String sessionId, Typed data) {

    }

}
