package com.example.demo.common;

/**
 * 统一接口响应包装体：以响应码、响应信息与泛型数据三段式结构返回给前端。
 *
 * @param <T> 响应数据类型
 * @author 甲
 */
public class Result<T> {
    // 响应码
    /** 响应码（如 200、400、500） */
    private String code;
    // 响应信息
    /** 响应提示信息 */
    private String msg;
    // 响应数据
    /** 响应业务数据 */
    private T data;

    /**
     * 获取响应码。
     *
     * @return 响应码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置响应码。
     *
     * @param code 响应码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取响应提示信息。
     *
     * @return 响应信息
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置响应提示信息。
     *
     * @param msg 响应信息
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 获取响应数据。
     *
     * @return 响应数据
     */
    public T getData() {
        return data;
    }

    /**
     * 设置响应数据。
     *
     * @param data 响应数据
     */
    public void setData(T data) {
        this.data = data;
    }

    /** 无参构造，构造空响应体。 */
    public Result() {
    }

    /**
     * 携带数据的构造。
     *
     * @param data 响应数据
     */
    public Result(T data) {
        this.data = data;
    }

    // 成功响应（无数据，如 DELETE/仅状态接口）
    /**
     * 构造无数据的成功响应。
     *
     * @return 码为 200、信息为“操作成功”的空响应
     */
    public static Result<Void> success() {
        Result<Void> result = new Result<>();
        result.setCode("200");
        result.setMsg("操作成功");
        return result;
    }

    // 成功响应（带数据）
    /**
     * 构造携带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 码为 200、信息为“操作成功”的响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>(data);
        result.setCode("200");
        result.setMsg("操作成功");
        return result;
    }

    // 失败响应（自定义信息）
    /**
     * 构造失败响应，响应码固定为 500。
     *
     * @param msg 失败提示信息
     * @return 码为 500 的失败响应
     */
    public static Result<?> error(String msg) {
        Result<Object> result = new Result<>();
        result.setCode("500");
        result.setMsg(msg);
        return result;
    }

    // 失败响应（自定义状态码和信息）
    /**
     * 构造失败响应，允许自定义响应码与提示信息。
     *
     * @param code 失败响应码
     * @param msg  失败提示信息
     * @return 含指定响应码与信息的失败响应
     */
    public static Result<?> error(String code, String msg) {
        Result<Object> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
