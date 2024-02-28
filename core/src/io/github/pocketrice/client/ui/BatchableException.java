package io.github.pocketrice.client.ui;

public class BatchableException extends Exception {
    public BatchableException(String errMsg) {
        super(errMsg);
    }

    public BatchableException(String errMsg, Throwable err) {
        super(errMsg, err);
    }
}
