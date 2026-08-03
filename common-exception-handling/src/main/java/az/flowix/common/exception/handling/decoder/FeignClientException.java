package az.flowix.common.exception.handling.decoder;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public class FeignClientException extends BaseException {

    private final String sourceKey;
    private final String sourceTitle;

    public FeignClientException(ErrorCode errorCode, HttpStatus status, String detail,
                                String sourceKey, String sourceTitle) {
        super(errorCode, status, detail, null);
        this.sourceKey = sourceKey;
        this.sourceTitle = sourceTitle;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

}
