package com.xiaoyuan.market.service;

import cn.hutool.core.io.FileUtil;
import com.xiaoyuan.market.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * 本地文件上传服务
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${market.upload-dir}")
    private String uploadDir;

    /**
     * 保存图片，返回可访问的相对 URL：/upload/yyyyMM/uuid.ext
     */
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = FileUtil.extName(original).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg/jpeg/png/gif/webp 格式图片");
        }
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String filename = java.util.UUID.randomUUID().toString().replace("-", "") + "." + ext;
        java.io.File dir = new java.io.File(uploadDir, month);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建上传目录失败");
        }
        try {
            file.transferTo(new java.io.File(dir, filename).getAbsoluteFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
        return "/api/upload/" + month + "/" + filename;
    }
}
