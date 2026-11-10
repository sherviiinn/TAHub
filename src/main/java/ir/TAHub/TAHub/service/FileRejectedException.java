package ir.TAHub.TAHub.service;

/** An uploaded file was not accepted. messageKey says why (shown to the user). */
public class FileRejectedException extends RuntimeException {

    private final String messageKey;

    public FileRejectedException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
