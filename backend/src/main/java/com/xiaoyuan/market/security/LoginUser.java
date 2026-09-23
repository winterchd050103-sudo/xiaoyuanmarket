package com.xiaoyuan.market.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录用户信息（存入 SecurityContext）
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Long id;
    private String username;
    private Integer role;
    private Integer status;

    public boolean isAdmin() {
        return role != null && role == 1;
    }
}
