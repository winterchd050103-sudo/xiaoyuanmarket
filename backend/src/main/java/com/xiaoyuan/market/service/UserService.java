package com.xiaoyuan.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.dto.ChangePasswordReq;
import com.xiaoyuan.market.dto.LoginReq;
import com.xiaoyuan.market.dto.RegisterReq;
import com.xiaoyuan.market.dto.UpdateProfileReq;
import com.xiaoyuan.market.entity.User;
import com.xiaoyuan.market.mapper.UserMapper;
import com.xiaoyuan.market.security.JwtUtil;
import com.xiaoyuan.market.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private static final String CACHE_KEY = "user:info:";

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 注册（成功后自动登录）
     */
    public LoginVO register(RegisterReq req) {
        long exists = count(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BusinessException("用户名已被占用");
        }
        if (StringUtils.hasText(req.getPhone())) {
            long phoneExists = count(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
            if (phoneExists > 0) {
                throw new BusinessException("手机号已被注册");
            }
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : req.getUsername());
        user.setPhone(StringUtils.hasText(req.getPhone()) ? req.getPhone() : null);
        user.setRole(0);
        user.setStatus(1);
        save(user);
        cacheUser(user);
        return buildLoginVO(user);
    }

    /**
     * 登录
     */
    public LoginVO login(LoginReq req) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        cacheUser(user);
        return buildLoginVO(user);
    }

    public void updateProfile(Long userId, UpdateProfileReq req) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(req.getPhone())) {
            User byPhone = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
            if (byPhone != null && !byPhone.getId().equals(userId)) {
                throw new BusinessException("手机号已被其他账号使用");
            }
        }
        User update = new User();
        update.setId(userId);
        update.setNickname(req.getNickname());
        update.setPhone(StringUtils.hasText(req.getPhone()) ? req.getPhone() : null);
        updateById(update);
        evictUser(userId);
    }

    public void changePassword(Long userId, ChangePasswordReq req) {
        User user = getById(userId);
        if (user == null || !passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(req.getNewPassword()));
        updateById(update);
        evictUser(userId);
    }

    public void updateAvatar(Long userId, String avatarUrl) {
        User update = new User();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        updateById(update);
        evictUser(userId);
    }

    /**
     * 按 id 查询用户（Redis 缓存 1 小时，未命中回源数据库）
     */
    @SuppressWarnings("unchecked")
    public User getByIdCached(Long id) {
        String key = CACHE_KEY + id;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof User u) {
                return u;
            }
        } catch (Exception e) {
            log.warn("redis read failed, fallback to db: {}", e.getMessage());
        }
        User user = getById(id);
        if (user != null) {
            cacheUser(user);
        }
        return user;
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return LoginVO.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }

    private void cacheUser(User user) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY + user.getId(), user, Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("redis write failed: {}", e.getMessage());
        }
    }

    private void evictUser(Long userId) {
        try {
            redisTemplate.delete(CACHE_KEY + userId);
        } catch (Exception e) {
            log.warn("redis delete failed: {}", e.getMessage());
        }
    }
}
