
package org.kurento.tutorial.groupcall;

import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GroupCallApp {

  @Bean
  public RoomManager roomManager() {
    return new RoomManager();
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }


  public static void main(String[] args) throws Exception {
    SpringApplication.run(GroupCallApp.class, args);
  }

}
