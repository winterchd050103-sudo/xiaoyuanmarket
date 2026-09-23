package com.xiaoyuan.market.security;

import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.common.ResultCode;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户
 */
public class SecurityUtils {

    public static LoginUser getLoginUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser;
    }

    public static Long getUserId() {
        return getLoginUser().getId();
    }
}
