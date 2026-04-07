package com.taskhub.vo;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private Integer code;
    private T data;
    private String message;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(0);
        resp.setData(data);
        resp.setMessage("success");
        return resp;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(0);
        resp.setData(data);
        resp.setMessage(message);
        return resp;
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}
