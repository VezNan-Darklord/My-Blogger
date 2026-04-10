package csulzc.My_Personal_Blogger.api.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private T data;
    private String message;
    private Integer code;

    public static <T> Result<T> success(T data) {
        return new Result<>(data, "success", 200);
    }

    public static <T> Result<T> success()
    {
        return new Result<>(null, "success", 200);
    }

    public static <T> Result<T> success(T data, String message)
    {
        return new Result<>(data, message, 200);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(null, message, code);
    }

    // 失败（使用HTTP状态码语义）
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(data, message, code);
    }
}
