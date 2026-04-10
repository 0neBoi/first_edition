package com.studyhelper.config;

import com.studyhelper.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.warn("请求异常", e);
        return Result.fail(e.getMessage() != null ? e.getMessage() : "服务器异常");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.fail("文件大小超过限制");
    }
}
