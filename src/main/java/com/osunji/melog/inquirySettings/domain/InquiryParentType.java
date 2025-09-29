package com.osunji.melog.inquirySettings.domain;

import lombok.Getter;

import java.util.EnumSet;

@Getter
public enum InquiryParentType {
    /** 계정/로그인/회원정보 */
    ACCOUNT(EnumSet.of(
            InquiryChildType.ACCOUNT_PRIVACY,
            InquiryChildType.POLICY,
            InquiryChildType.OTHER // 항상 허용
    )),

    /** 제안/협업/마케팅/콘텐츠 */
    SUGGESTION(EnumSet.of(
            InquiryChildType.USAGE,
            InquiryChildType.BUG,
            InquiryChildType.PARTNERSHIP,
            InquiryChildType.MARKETING,
            InquiryChildType.CONTENT,
            InquiryChildType.OTHER // 항상 허용
    )),

    /** 기타 (사실상 모든 child 허용 가능) */
    OTHER(EnumSet.of(
            InquiryChildType.OTHER
    ));

    private final EnumSet<InquiryChildType> allowedChildren;

    InquiryParentType(EnumSet<InquiryChildType> allowedChildren) {
        this.allowedChildren = allowedChildren;
    }

}

