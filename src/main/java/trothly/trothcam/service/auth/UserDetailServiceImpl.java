package trothly.trothcam.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import trothly.trothcam.domain.member.MemberRepository;
import trothly.trothcam.exception.custom.BadRequestException;

import static trothly.trothcam.exception.base.ErrorCode.MEMBER_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class UserDetailServiceImpl implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        System.out.println("로그인한 memberId : " + memberId);
        return (UserDetails) memberRepository.findById(Long.parseLong(memberId))
                .orElseThrow(() -> new BadRequestException(MEMBER_NOT_FOUND));
    }
}
