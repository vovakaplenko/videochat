
package name.nkonev.video;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableScheduling
public class GroupCallApp {

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(GroupCallApp.class, args);
  }

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Scheduled(cron = "${video.liveness.cron}")
  public void f() throws UnknownHostException {
    redisTemplate.opsForValue().set("video::"+InetAddress.getLocalHost().getHostAddress(), "true");
  }

}
