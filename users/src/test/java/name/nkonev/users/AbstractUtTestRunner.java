package name.nkonev.users;

/**
 * Created by nik on 27.05.17.
 */

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import name.nkonev.users.repository.redis.UserConfirmationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {UsersApplication.class, UtConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = MockMvcPrint.LOG_DEBUG)
@AutoConfigureRestDocs(outputDir = TestConstants.RESTDOCS_SNIPPETS_DIR)
@Transactional
public abstract class AbstractUtTestRunner {

    @Autowired
    protected MockMvc mockMvc;

    @Configuration
    public static class UtConfig {

        @Autowired
        private RedisServerCommands redisServerCommands;

        @Bean(destroyMethod = "close")
        public DefaultStringRedisConnection defaultStringRedisConnection(
            RedisConnectionFactory redisConnectionFactory){
            return new DefaultStringRedisConnection(redisConnectionFactory.getConnection());
        }

        @PostConstruct
        public void dropRedis(){
            redisServerCommands.flushDb();
        }
    }

    @Autowired
    protected UserConfirmationTokenRepository userConfirmationTokenRepository;

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    protected RestTemplate restTemplate;

    @Value("${local.management.port}")
    protected int mgmtPort;

    @Autowired
    protected AbstractServletWebServerFactory abstractConfigurableEmbeddedServletContainer;

    public String urlWithContextPath(){
        return ContextPathHelper.urlWithContextPath(abstractConfigurableEmbeddedServletContainer);
    }

    @Value(CommonTestConstants.USER)
    protected String username;

    @Value(CommonTestConstants.PASSWORD)
    protected String password;

    @Autowired
    protected ObjectMapper objectMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUtTestRunner.class);

    public PostDTO getPost(long postId) throws Exception {
        MvcResult getPostRequest = mockMvc.perform(
                get(Constants.Urls.API+ Constants.Urls.POST+"/"+postId)
        )
                .andExpect(status().isOk())
                .andReturn();
        String getStr = getPostRequest.getResponse().getContentAsString();
        LOGGER.debug(getStr);
        return objectMapper.readValue(getStr, PostDTO.class);
    }


    protected String buildCookieHeader(HttpCookie... cookies) {
        return String.join("; ", Arrays
            .stream(cookies).map(httpCookie -> httpCookie.toString()).collect(Collectors.toList()));
    }

    /**
     * This method changes in runtime with ReflectionUtils Spring Security Csrf Filter .with(csrf()) so it ignores any CSRF token
     * @param xsrf
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    protected String getSession(String xsrf, String username, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                post(SecurityConfig.API_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(USERNAME_PARAMETER, username)
                        .param(PASSWORD_PARAMETER, password)
                        .with(csrf())
        ).andDo(mvcResult1 -> {
            LOGGER.info(mvcResult1.getResponse().getContentAsString());
        })
                .andExpect(status().isOk())
                .andReturn();

        return mvcResult.getResponse().getCookie(CommonTestConstants.COOKIE_SESSION).getValue();
    }

    @BeforeEach
    public void before() {
        userConfirmationTokenRepository.deleteAll();
    }
}
