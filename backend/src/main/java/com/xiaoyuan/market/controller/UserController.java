package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.dto.ChangePasswordReq;
import com.xiaoyuan.market.dto.UpdateProfileReq;
import com.xiaoyuan.market.security.LoginUser;
import com.xiaoyuan.market.security.SecurityUtils;
import com.xiaoyuan.market.service.FileService;
import com.xiaoyuan.market.service.UserService;
import com.xiaoyuan.market.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileService fileService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/profile")
    public Result<UserVO> profile() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return Result.ok(UserVO.from(userService.getById(loginUser.getId())));
    }

    @Operation(summary = "修改昵称 / 手机号")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileReq req) {
        userService.updateProfile(SecurityUtils.getUserId(), req);
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        userService.changePassword(SecurityUtils.getUserId(), req);
        return Result.ok();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String url = fileService.saveImage(file);
        userService.updateAvatar(SecurityUtils.getUserId(), url);
        return Result.ok(url);
    }
}
