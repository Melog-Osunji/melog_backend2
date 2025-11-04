package com.osunji.melog.global.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

package com.osunji.melog.global.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 💡 DtoMapperUtil
 *
 * DTO 객체를 Map<String, String> 형태로 변환하는 유틸리티 클래스입니다.
 * PATCH 요청 등 "부분 업데이트" 상황에서, DTO의 null 값 필드를 자동으로 제외하고
 * key-value 형식으로 변환할 때 사용됩니다.
 *
 * 예:
 * UserRequest.Profile { nickName="aaa", intro=null, profileImg="bbb" }
 *  → { nickName="aaa", profileImg="bbb" }
 *
 * <p>사용 예시:</p>
 * <pre>
 *     Map<String, String> updates = dtoMapperUtil.toMapWithoutNulls(profileRequest);
 * </pre>
 *
 * <p>Spring Bean으로 등록되어 있으므로, 다른 Service 클래스에서
 * @Autowired 또는 생성자 주입을 통해 바로 사용 가능합니다.</p>
 */
@Component
public class DtoMapperUtil {

    /**
     * ObjectMapper는 Jackson에서 제공하는 직렬화/역직렬화 핵심 객체입니다.
     * Spring Boot에서는 이미 Bean으로 등록되어 있으므로 DI(의존성 주입)가 가능합니다.
     */
    private final ObjectMapper objectMapper;

    /**
     * 생성자 주입 시, ObjectMapper의 직렬화 설정을 수정하여
     * null 필드는 변환 대상에서 제외되도록 설정합니다.
     * (즉, null 값이 Map에 포함되지 않음)
     */
    public DtoMapperUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * DTO를 Map<String, String>으로 변환합니다.
     *
     * <T> 제네릭을 사용하여 어떤 DTO 타입이 들어와도 처리할 수 있도록 설계되었습니다.
     *
     * @param dto 변환할 DTO 객체 (예: UserRequest.Profile)
     * @return null 필드가 제외된 Map<String, String>
     */
    public <T> Map<String, String> toMapWithoutNulls(T dto) {
        return objectMapper.convertValue(dto, new TypeReference<>() {});
    }
}


