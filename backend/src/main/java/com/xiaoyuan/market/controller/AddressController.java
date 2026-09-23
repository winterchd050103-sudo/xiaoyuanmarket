package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.dto.AddressReq;
import com.xiaoyuan.market.entity.Address;
import com.xiaoyuan.market.security.SecurityUtils;
import com.xiaoyuan.market.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收货地址")
@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "我的地址列表")
    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.ok(addressService.listByUser(SecurityUtils.getUserId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Address> add(@Valid @RequestBody AddressReq req) {
        return Result.ok(addressService.add(SecurityUtils.getUserId(), req));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public Result<Address> update(@PathVariable Long id, @Valid @RequestBody AddressReq req) {
        return Result.ok(addressService.update(SecurityUtils.getUserId(), id, req));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        addressService.remove(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(SecurityUtils.getUserId(), id);
        return Result.ok();
    }
}
