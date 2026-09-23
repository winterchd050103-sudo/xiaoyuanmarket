package com.xiaoyuan.market.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录结果：JWT Token + 用户信息
 */
@Data
@Builder
public class LoginVO {

    private String token;

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Integer role;
}
