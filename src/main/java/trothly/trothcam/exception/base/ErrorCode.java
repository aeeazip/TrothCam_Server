package trothly.trothcam.exception.base;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ErrorCode { // BaseResponseStatus와 같은 역할

    SUCCESS(1000, "요청에 성공하였습니다.", OK),

    /**
     * 2000 : Request 오류
     */
    // Common
    _INTERNAL_SERVER_ERROR(2000, "서버 에러, 관리자에게 문의 바랍니다.", INTERNAL_SERVER_ERROR),
    _BAD_REQUEST(2001, "잘못된 요청입니다.", BAD_REQUEST),
    _UNAUTHORIZED(2002, "권한이 없습니다.", UNAUTHORIZED),

    REQUEST_ERROR(2003, "입력값을 확인해주세요.", BAD_REQUEST),
    EMPTY_JWT(2004, "JWT를 입력해주세요.", BAD_REQUEST),
    INVALID_JWT(2005, "유효하지 않은 JWT입니다.", UNAUTHORIZED),
    INVALID_USER_JWT(2006,"권한이 없는 유저의 접근입니다.", BAD_REQUEST),
    NOT_AGREE_EMAIL(2007, "이메일 동의가 거부되었습니다.", BAD_REQUEST),
    MEMBER_NOT_FOUND(2008, "해당되는 유저를 찾을 수 없습니다.", BAD_REQUEST),
    DUPLICATED_MEMBER(2009, "중복된 WEB 토큰 입니다.", BAD_REQUEST),
    INVALID_PROVIDER(2010, "잘못된 PROVIDER 입니다.", BAD_REQUEST),
    TOKEN_EXPIRED(2011, "토큰 유효시간이 만료되었습니다.", BAD_REQUEST),
    ALREADY_LOGOUT(2012, "이미 로그아웃 되었습니다.", BAD_REQUEST),
    ALREADY_LIKED(2013, "이미 좋아요를 누른 상품입니다.",BAD_REQUEST),
    NOT_LIKED(2014, "좋아요를 누르지 않은 상품입니다.", BAD_REQUEST),
    HISTORIES_NOT_FOUND(2015, "거래 내역이 존재하지 않습니다.", BAD_REQUEST),
    PRODUCT_NOT_FOUND(2016, "인증서가 존재하지 않습니다.", BAD_REQUEST),
    SAME_MEMBER(2017, "상품의 소유자와 현재 구매하려고 하는 사용자가 같습니다.", BAD_REQUEST),
    PRODUCT_IS_NOT_FOUND(2018, "존재하지 않는 상품입니다.", BAD_REQUEST),
    IMAGE_NOT_FOUND(2019, "존재하지 않는 이미지입니다.", BAD_REQUEST),

    /**
     * 3000 : Response 오류
     */
    // Common
    RESPONSE_ERROR(3000, "값을 불러오는데 실패하였습니다.", FORBIDDEN),


    /**
     * 4000 : Database, Server 오류
     */
    DATABASE_ERROR(4000, "데이터베이스 연결에 실패하였습니다.", INTERNAL_SERVER_ERROR),
    SERVER_ERROR(4001, "서버와의 연결에 실패하였습니다.", INTERNAL_SERVER_ERROR);


    /**
     * 5000 : 필요시 만들어서 쓰세요
     */


    /**
     * 6000 : 필요시 만들어서 쓰세요
     */

    private int code;
    private String message;
    private HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
