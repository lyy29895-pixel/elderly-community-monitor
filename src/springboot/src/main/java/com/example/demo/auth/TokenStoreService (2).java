package com.example.demo.auth;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录 Token 存储：进程内 Map（单机课设演示）。
 * 一个用户只保留最新 Token；服务重启后全部登录态失效。
 *
 * @author 甲
 */
@Service
public class TokenStoreService {

    private final ConcurrentHashMap<Integer, String> memoryStore = new ConcurrentHashMap<>();

    /**
     * 保存（或覆盖）指定用户的最新登录 Token。
     *
     * @param userId 用户 ID
     * @param token  登录后签发的 JWT
     */
    public void saveToken(Integer userId, String token) {
        memoryStore.put(userId, token);
    }

    /**
     * 校验传入 Token 是否为该用户当前有效的登录 Token。
     *
     * @param userId 用户 ID
     * @param token  待校验的 JWT
     * @return 一致返回 true；token 为空或不匹配返回 false
     */
    public boolean matches(Integer userId, String token) {
        return token != null && token.equals(memoryStore.get(userId));
    }

    /**
     * 清除指定用户的登录态（注销或被新登录顶替）。
     *
     * @param userId 用户 ID
     */
    public void clear(Integer userId) {
        memoryStore.remove(userId);
    }
}
