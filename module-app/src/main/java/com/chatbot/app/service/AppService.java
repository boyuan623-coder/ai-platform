package com.chatbot.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbot.app.dto.AppCreateRequest;
import com.chatbot.app.dto.AppProjectPayload;
import com.chatbot.app.dto.AppUpdateRequest;
import com.chatbot.app.entity.App;
import com.chatbot.app.mapper.AppMapper;
import com.chatbot.common.exception.BusinessException;
import com.chatbot.user.entity.User;
import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppService {

    private final AppMapper appMapper;
    private final UserService userService;

    public App create(String token, AppCreateRequest request) {
        User user = userService.requireUser(token);
        App app = new App();
        app.setUserId(user.getId());
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        app.setCodeType(request.getCodeType() != null ? request.getCodeType() : "HTML");
        app.setCodeContent(request.getCodeContent());
        app.setIsFeatured(false);
        app.setStatus("DRAFT");
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.insert(app);
        return app;
    }

    public App update(String token, Long appId, AppUpdateRequest request) {
        App app = requireOwnedApp(token, appId);
        if (request.getName() != null) app.setName(request.getName());
        if (request.getDescription() != null) app.setDescription(request.getDescription());
        if (request.getCodeType() != null) app.setCodeType(request.getCodeType());
        if (request.getCodeContent() != null) app.setCodeContent(request.getCodeContent());
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        return app;
    }

    public void delete(String token, Long appId) {
        App app = requireOwnedApp(token, appId);
        appMapper.deleteById(app.getId());
    }

    public App getById(String token, Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        User user = userService.requireUser(token);
        if (!app.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException("无权访问该应用");
        }
        return app;
    }

    public Page<App> listMine(String token, int page, int size) {
        User user = userService.requireUser(token);
        return appMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<App>()
                        .eq(App::getUserId, user.getId())
                        .orderByDesc(App::getCreatedAt));
    }

    public List<App> listFeatured(int limit) {
        return appMapper.selectList(new LambdaQueryWrapper<App>()
                .eq(App::getIsFeatured, true)
                .orderByDesc(App::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 50)));
    }

    public Page<App> listAllAdmin(String token, int page, int size) {
        User user = userService.requireUser(token);
        if (!userService.isAdmin(user)) {
            throw new BusinessException("需要管理员权限");
        }
        return appMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<App>().orderByDesc(App::getCreatedAt));
    }

    public App setFeatured(String token, Long appId, boolean featured) {
        User user = userService.requireUser(token);
        if (!userService.isAdmin(user)) {
            throw new BusinessException("需要管理员权限");
        }
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        app.setIsFeatured(featured);
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        return app;
    }

    public App updateDirect(App app) {
        appMapper.updateById(app);
        return app;
    }

    public App saveProject(String token, Long appId, AppProjectPayload payload) {
        App app = requireOwnedApp(token, appId);
        app.setCodeType(payload.getProjectType() != null ? payload.getProjectType().toUpperCase() : "VUE");
        app.setCodeContent(payload.toJson());
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        return app;
    }

    public App getAppEntity(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        return app;
    }

    public App requireOwnedApp(String token, Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        User user = userService.requireUser(token);
        if (!app.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException("无权操作该应用");
        }
        return app;
    }
}
