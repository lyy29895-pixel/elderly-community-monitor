package com.example.demo.exception;

/**
 * 业务异常：用于在业务校验失败时携带自定义响应码与提示信息抛出，
 * 由全局异常处理器统一转换为错误响应。
 *
 * @author 甲
 */
public class BusinessException extends RuntimeException {
    /** 业务响应码 */
    private final String code;

    /**
     * 构造业务异常，响应码默认为 400。
     *
     * @param message 异常提示信息
     */
    public BusinessException(String message) {
        this("400", message);
    }

    /**
     * 构造业务异常并指定响应码。
     *
     * @param code    业务响应码
     * @param message 异常提示信息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务响应码。
     *
     * @return 响应码
     */
    public String getCode() {
        return code;
    }
}
