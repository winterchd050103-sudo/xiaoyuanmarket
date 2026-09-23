package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.entity.Category;
import com.xiaoyuan.market.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品分类")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "启用中的分类列表（公开）")
    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listEnabled());
    }
}
