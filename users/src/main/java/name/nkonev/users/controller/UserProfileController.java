package name.nkonev.users.controller;

import static name.nkonev.users.Constants.USER_HEADER;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.Valid;
import name.nkonev.users.Constants;
import name.nkonev.users.dto.EditUserDTO;
import name.nkonev.users.dto.LockDTO;
import name.nkonev.users.dto.UserAccountDTO;
import name.nkonev.users.dto.UserRole;
import name.nkonev.users.dto.Wrapper;
import name.nkonev.users.entity.jdbc.UserAccount;
import name.nkonev.users.exception.UserAlreadyPresentException;
import name.nkonev.users.repository.jdbc.UserAccountRepository;
import name.nkonev.users.service.FutureUserDetailsService;
import name.nkonev.users.service.UserAccountConverter;
import name.nkonev.users.service.UserDeleteService;
import name.nkonev.users.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by nik on 08.06.17.
 */
@RequestMapping(Constants.Urls.API)
@RestController
@Transactional
public class UserProfileController {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FutureUserDetailsService blogUserDetailsService;

    @Autowired
    private UserAccountConverter userAccountConverter;

    @Autowired
    private UserDeleteService userDeleteService;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileController.class);


    @GetMapping(value = Constants.Urls.USER)
    public Wrapper<UserAccountDTO> getUsers(
        @RequestParam(value = "page", required=false, defaultValue = "0") int page,
        @RequestParam(value = "size", required=false, defaultValue = "0") int size,
        @RequestParam(value = "searchString", required=false, defaultValue = "") String searchString
    ) {
        PageRequest springDataPage = PageRequest.of(PageUtils.fixPage(page), PageUtils.fixSize(size), Sort.Direction.ASC, "id");
        searchString = searchString.trim();

        final String forDbSearch = "%" + searchString + "%";
        List<UserAccount> resultPage = userAccountRepository.findByUsernameContainsIgnoreCase(springDataPage.getPageSize(), springDataPage.getOffset(), forDbSearch);
        long resultPageCount = userAccountRepository.findByUsernameContainsIgnoreCaseCount(springDataPage.getPageSize(), springDataPage.getOffset(), forDbSearch);

        return new Wrapper<UserAccountDTO>(
            resultPage.stream().map(getConvertToUserAccountDTO()).collect(Collectors.toList()),
            resultPageCount
        );
    }

    private Function<UserAccount, UserAccountDTO> getConvertToUserAccountDTO() {
        return userAccount -> userAccountConverter.convertToUserAccountDTOExtended(null, userAccount);
    }

    @GetMapping(value = Constants.Urls.USER+ Constants.Urls.USER_ID)
    public UserAccountDTO getUser(
        @PathVariable(Constants.PathVariables.USER_ID) Long userId
    ) {
        UserAccount userAccountEntity = userAccountRepository.findById(userId).orElseThrow(() -> new RuntimeException("user with id="+ userId + " not found"));
        return userAccountConverter.convertToUserAccountDTO(userAccountEntity);
    }

    @PostMapping(Constants.Urls.PROFILE)
    public EditUserDTO editProfile(
            @RequestHeader(USER_HEADER) Long currentUserId,
            @RequestBody @Valid EditUserDTO userAccountDTO
    ) {
        if (currentUserId == null) {
            throw new RuntimeException("Not authenticated user can't edit any user account. It can occurs due inpatient refactoring.");
        }

        UserAccount exists = userAccountRepository.findById(currentUserId).orElseThrow(()-> new RuntimeException("Authenticated user account not found in database"));

        // check email already present
        if (exists.getEmail()!=null && !exists.getEmail().equals(userAccountDTO.getEmail()) && userAccountRepository.findByEmail(userAccountDTO.getEmail()).isPresent()) {
            LOGGER.error("editProfile: user with email '{}' already present. exiting...", exists.getEmail());
            return UserAccountConverter.convertToEditUserDto(exists); // we care for email leak...
        }
        // check login already present
        if (!exists.getUsername().equals(userAccountDTO.getLogin()) && userAccountRepository.findByUsername(userAccountDTO.getLogin()).isPresent()) {
            throw new UserAlreadyPresentException("User with login '" + userAccountDTO.getLogin() + "' is already present");
        }

        UserAccountConverter.updateUserAccountEntity(userAccountDTO, exists, passwordEncoder);
        exists = userAccountRepository.save(exists);

        blogUserDetailsService.refreshUserDetails(exists);

        return UserAccountConverter.convertToEditUserDto(exists);
    }

    @PostMapping(Constants.Urls.USER + Constants.Urls.LOCK)
    public void setLocked(@RequestBody LockDTO lockDTO){
        UserAccount userAccount = blogUserDetailsService.getUserAccount(lockDTO.getUserId());
        if (lockDTO.isLock()){
            blogUserDetailsService.killSessions(lockDTO.getUserId());
        }
        userAccount.setLocked(lockDTO.isLock());
        userAccount = userAccountRepository.save(userAccount);
    }

    @DeleteMapping(Constants.Urls.USER)
    public long deleteUser(@RequestParam("userId") long userId){
        return userDeleteService.deleteUser(userId);
    }

    @PostMapping(Constants.Urls.USER + Constants.Urls.ROLE)
    public void setRole(@RequestParam long userId, @RequestParam UserRole role){
        UserAccount userAccount = userAccountRepository.findById(userId).orElseThrow();
        userAccount.setRole(role);
        userAccount = userAccountRepository.save(userAccount);
    }

    @DeleteMapping(Constants.Urls.PROFILE)
    public void selfDeleteUser(@RequestHeader(USER_HEADER) Long currentUserId){
        userDeleteService.deleteUser(currentUserId);
    }

    @DeleteMapping(Constants.Urls.PROFILE+Constants.Urls.FACEBOOK)
    public void selfDeleteBindingFacebook(@RequestHeader(USER_HEADER) Long currentUserId){
        UserAccount userAccount = userAccountRepository.findById(currentUserId).orElseThrow();
        userAccount.getOauthIdentifiers().setFacebookId(null);
        userAccount = userAccountRepository.save(userAccount);
        blogUserDetailsService.refreshUserDetails(userAccount);
    }

    @DeleteMapping(Constants.Urls.PROFILE+Constants.Urls.VKONTAKTE)
    public void selfDeleteBindingVkontakte(@RequestHeader(USER_HEADER) Long currentUserId){
        UserAccount userAccount = userAccountRepository.findById(currentUserId).orElseThrow();
        userAccount.getOauthIdentifiers().setVkontakteId(null);
        userAccount = userAccountRepository.save(userAccount);
        blogUserDetailsService.refreshUserDetails(userAccount);
    }

}