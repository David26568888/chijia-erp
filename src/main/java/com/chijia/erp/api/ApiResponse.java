package com.chijia.erp.api;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 奇家五金進銷存系統 - 全局統一 API 回覆格式
 * 靈魂思想：讓不論是成功、失敗、列表或物件，回傳前端的 JSON 骨架永遠維持一致。
 */

@Setter
@Getter
public class ApiResponse<T> {
    private boolean success;      // 業務執行是否成功
    private int code;             // 狀態碼 (與 HTTP 或自訂商務代碼對齊)
    private String message;       // 提示訊息 (如 "操作成功"、"庫存不足" 等)
    private T data;               // 實際承載的資料主體 (泛型)
    private LocalDateTime timestamp; // 報文生成時間 (方便前台排查 log)

    // 預設建構子
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // 全參數建構子
    public ApiResponse(boolean success, int code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // ==================== 🛠️ 快捷靜態工廠方法 ====================

    // 💡 成功回傳 (一般查詢、不需特定訊息)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "操作成功", data);
    }

    // 💡 成功回傳 (自訂提示訊息，常用於新增/修改/狀態切換)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 200, message, data);
    }

    // 💡 失敗回傳 (傳入自訂錯誤碼與錯誤訊息)
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}