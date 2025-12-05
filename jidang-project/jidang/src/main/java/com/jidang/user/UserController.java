package com.jidang.user;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.ui.Model;
import java.security.Principal; // 현재 로그인된 사용자 정보를 가져오는 객체

import com.jidang.Post.PostRepository;

import org.springframework.security.access.prepost.PreAuthorize; // 💡 @PreAuthorize //권한검사-로그인되어있는지 아닌지
import org.springframework.web.bind.annotation.RequestParam;           // 💡 @RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 💡 RedirectAttributes 및 addFlashAttribute-알림메세지

@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final UserDetailsService userSecurityService;
    private final PostRepository postRepository;


    @GetMapping("/signup")  //URL이 GET으로 요청되면 회원 가입을 위한 템플릿을 렌더링
    public String signup(UserCreateForm userCreateForm) {
        return "signup_form";
    }

    @PostMapping("/signup")  //POST로 요청되면 회원 가입을 진행
    public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "signup_form";
        }

        if (!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "signup_form";
        }

        //중복 알림 메세지 출력
        try {
            userService.create(userCreateForm.getUsername(),
                    userCreateForm.getEmail(), userCreateForm.getPassword1());
        }catch(DataIntegrityViolationException e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "signup_form";
        }catch(Exception e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", e.getMessage());
            return "signup_form";
        }

        //회원가입 후 자동 로그인 + 메인페이지로 이동
        UserDetails userDetails = userSecurityService.loadUserByUsername(userCreateForm.getUsername());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // 마이페이지 URL 추가(로그인된 사용자 정보 넘겨줌)
    @GetMapping("/mypage")
    public String mypage(Principal principal, Model model) {
        // 1. Principal 객체에서 현재 로그인된 사용자의 'username'을 가져옴
        String username = principal.getName();

        // 2. UserService를 통해 DB에서 SiteUser 객체를 조회 (UserNotFoundException 처리 필요)
        SiteUser siteUser = this.userService.getUser(username);

        // 3. PostRepository의 countByAuthor 메서드를 사용하여 siteUser가 작성한 게시물 개수를 셉니다.
        long postCount = this.postRepository.countByAuthor(siteUser);

        // 4. Thymeleaf 템플릿으로 'user'라는 이름으로 전달
        model.addAttribute("user", siteUser);

        // 5. 게시물 수를 'postCount'라는 이름으로 Model에 추가
        model.addAttribute("postCount", postCount);

        return "mypage"; // mypage.html 템플릿 반환
    }

    //칭호 선택 처리 메서드
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/selectTitle")
    public String selectTitle(@RequestParam String titleName, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            userService.selectTitle(principal.getName(), titleName);
            redirectAttributes.addFlashAttribute("message", "대표 칭호가 '" + titleName + "'(으)로 설정되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        // 칭호 선택 페이지나 마이페이지로 리다이렉트
        return "redirect:/user/mypage";
    }
}
