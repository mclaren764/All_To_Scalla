package handler;

import status.ApplicationStatusResponse;

public interface Handler {
    ApplicationStatusResponse performOperation(String id);
}