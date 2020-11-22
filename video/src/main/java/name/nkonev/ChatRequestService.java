package name.nkonev;

import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatRequestService {
    @Autowired
    private OkHttpClient okHttpClient;

    public void sendToChatsBackWebsocket(String sessionId, JsonObject response) {
        // TODO implement
    }
}
