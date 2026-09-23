package com.xiaoyuan.market.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息（不含密码）
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private Integer role;

    private Integer status;

    private LocalDateTime createTime;

    public static UserVO from(com.xiaoyuan.market.entity.User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhone(u.getPhone());
        vo.setRole(u.getRole());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }
}
