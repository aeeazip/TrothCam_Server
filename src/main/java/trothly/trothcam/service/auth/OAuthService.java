package trothly.trothcam.service.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import trothly.trothcam.dto.auth.TokenDto;
import trothly.trothcam.dto.auth.apple.LoginReqDto;
import trothly.trothcam.dto.auth.apple.LoginResDto;
//import trothly.trothcam.dto.auth.apple.RefreshTokenReqDto;
import trothly.trothcam.dto.auth.apple.RefreshTokenReqDto;
import trothly.trothcam.dto.auth.google.GoogleOauthToken;
import trothly.trothcam.dto.auth.google.GoogleUser;
import trothly.trothcam.exception.base.*;
import trothly.trothcam.exception.custom.InvalidProviderException;
import trothly.trothcam.auth.apple.AppleOAuthUserProvider;
import trothly.trothcam.domain.member.*;
//import trothly.trothcam.exception.custom.InvalidTokenException;
import trothly.trothcam.exception.custom.InvalidTokenException;
import trothly.trothcam.service.JwtService;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AppleOAuthUserProvider appleOAuthUserProvider;

    private final MemberRepository memberRepository;
//    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    // 구글 로그인
    private final GoogleOauth googleOauth;
    private final HttpServletResponse response;


    // 애플 로그인
    @Transactional
    public LoginResDto appleLogin(LoginReqDto loginReqDto) throws BaseException {
        // identity token으로 email값 얻어오기
        String email = appleOAuthUserProvider.getEmailFromToken(loginReqDto.getIdToken());
        log.info(email);

        // email이 null인 경우 : email 동의 안한 경우
        if(email == null)
            throw new BaseException(ErrorCode.NOT_AGREE_EMAIL);

        Optional<Member> getMember = memberRepository.findByEmail(email);
        Member member;
        if(getMember.isPresent()){  // 이미 회원가입한 회원인 경우
            member = getMember.get();
            if(!member.getProvider().equals(Provider.APPLE))   // 이미 회원가입했지만 APPLE이 아닌 다른 소셜 로그인 사용
                throw new InvalidProviderException("GOOGLE로 회원가입한 회원입니다.");
        } else {    // 아직 회원가입 하지 않은 회원인 경우
            member = memberRepository.save(
                    Member.builder()
                        .email(email)
                        .provider(Provider.APPLE)
                        .build()
            );
        }

        // accessToken, refreshToken 발급
        String newAccessToken = jwtService.encodeJwtToken(new TokenDto(member.getId()));
        String newRefreshToken = jwtService.encodeJwtRefreshToken(member.getId());

        // redis에 refreshToken 저장
//        redisTemplate.opsForValue().set(member.getId().toString(), newRefreshToken, 14L, TimeUnit.SECONDS);
//        log.info("redis에 저장된 refreshToken : " + newRefreshToken + "\nmember.getId : " + member.getId().toString());

        // DB에 refreshToken 저장
        member.updateRefreshToken(newRefreshToken);
        memberRepository.save(member);
        return new LoginResDto(newAccessToken, newRefreshToken);
    }

    // refreshToken으로 accessToken 발급하기
    @Transactional
    public LoginResDto regenerateAccessToken(RefreshTokenReqDto refreshTokenReqDto) throws BaseException {
        Long memberId = jwtService.getMemberIdFromJwtToken(refreshTokenReqDto.getRefreshToken());
        log.info("memberId : " + memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당되는 member_id를 찾을 수 없습니다."));

        String refreshToken = refreshTokenReqDto.getRefreshToken();
        if(!refreshToken.equals(member.getRefreshToken()))
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");


        String newRefreshToken = jwtService.encodeJwtRefreshToken(memberId);
        String newAccessToken = jwtService.encodeJwtToken(new TokenDto(memberId));

        member.updateRefreshToken(newRefreshToken);
        memberRepository.save(member);

        return new LoginResDto(newAccessToken, newRefreshToken);
    }

    // 구글 로그인
    // 사용자 로그인 페이지 제공 단계 - url 반환
    public void request(String socialLoginType) throws IOException {
        String redirectURL = googleOauth.getOauthRedirectURL();

        response.sendRedirect(redirectURL);
    }

    // code -> accessToken 받아오기
    // accessToken -> 사용자 정보 받아오기
    @Transactional
    public LoginResDto oauthLogin(String socialLoginType, String code) throws JsonProcessingException {
        // 구글로 일회성 코드를 보내 액세스 토큰이 담긴 응답객체를 받아옴
        ResponseEntity<String> accessTokenResponse = googleOauth.requestAccessToken(code);

        // 응답 객체가 JSON 형식으로 되어 있으므로, 이를 deserialization 해서 자바 객체에 담음 (+ 로그 출력됨)
        GoogleOauthToken oAuthToken = googleOauth.getAccessToken(accessTokenResponse);

        // 액세스 토큰을 다시 구글로 보내 구글에 저장된 사용자 정보가 담긴 응답 객체를 받아옴
        ResponseEntity<String> userInfoResponse = googleOauth.requestUserInfo(oAuthToken);

        // 다시 JSON 형식의 응답 객체를 deserialization 해서 자바 객체에 담음
        GoogleUser googleUser = googleOauth.getUserInfo(userInfoResponse);
        String email = googleUser.getEmail();
        log.info(googleUser.getEmail());

        if(email == null)
            throw new BaseException(ErrorCode.NOT_AGREE_EMAIL);

        Optional<Member> getMember = memberRepository.findByEmail(email);
        Member member;
        if(getMember.isPresent()){  // 이미 회원가입한 회원인 경우
            member = getMember.get();
            if(!member.getProvider().equals(Provider.GOOGLE))   // 이미 회원가입했지만 GOOGLE이 아닌 다른 소셜 로그인 사용
                throw new InvalidProviderException("APPLE로 회원가입한 회원입니다.");
        } else {    // 아직 회원가입 하지 않은 회원인 경우
            member = memberRepository.save(
                    Member.builder()
                            .email(email)
                            .provider(Provider.GOOGLE)
                            .build()
            );
        }

        // accessToken, refreshToken 발급
        String newAccessToken = jwtService.encodeJwtToken(new TokenDto(member.getId()));
        String newRefreshToken = jwtService.encodeJwtRefreshToken(member.getId());

        // redis에 refreshToken 저장
//        redisTemplate.opsForValue().set(member.getId().toString(), newRefreshToken, 14L, TimeUnit.SECONDS);
//        log.info("redis에 저장된 refreshToken : " + newRefreshToken + "\nmember.getId : " + member.getId().toString());

        // DB에 refreshToken 저장
        member.updateRefreshToken(newRefreshToken);
        memberRepository.save(member);
        return new LoginResDto(newAccessToken, newRefreshToken);

    }
}
