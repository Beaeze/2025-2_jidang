package com.jidang.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import com.jidang.DataNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SiteUser create(String username, String email, String password) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setJoinDate(LocalDateTime.now()); //가입 시간

        this.userRepository.save(user);
        return user;
    }

    //SiteUser를 조회
    public SiteUser getUser(String username) {
        Optional<SiteUser> siteUser = this.userRepository.findByusername(username);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("siteuser not found");
        }
    }


    //사용자가 칭호를 선택하여 착용
    @Transactional
    public SiteUser selectTitle(String username, String selectedTitleName) {
        SiteUser user = getUser(username); // 기존 메서드로 사용자 조회 (없으면 예외 발생)

        // 1. 칭호 보유 여부 검증 (필수)
        // 사용자가 해당 칭호를 실제로 획득했는지 확인합니다.
        if (!user.getTitles().contains(selectedTitleName)) {
            throw new RuntimeException("사용자가 획득하지 않은 칭호입니다.");
        }

        // 2. 대표 칭호 설정
        user.setSelectedTitle(selectedTitleName);

        // 3. 변경 사항 저장 (JPA의 @Transactional 덕분에 user 객체의 변경 사항이 자동 반영됩니다.)
        return userRepository.save(user);
    }
}

