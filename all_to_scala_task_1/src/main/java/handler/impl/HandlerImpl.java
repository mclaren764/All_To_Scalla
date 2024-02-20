package handler.impl;

import client.Client;
import handler.Handler;
import response.Response;
import status.ApplicationStatusResponse;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HandlerImpl implements Handler {

    private final Client client;
    private int retriesAmount;
    private OffsetDateTime startTime;
    public HandlerImpl(Client client) {
        this.client = client;
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        startTime = OffsetDateTime.now();
        CompletableFuture<Response> future1 = CompletableFuture.supplyAsync(() -> client.getApplicationStatus1(id));
        CompletableFuture<Response> future2 = CompletableFuture.supplyAsync(() -> client.getApplicationStatus2(id));

        CompletableFuture<ApplicationStatusResponse> result = future1.applyToEither(future2, (res) -> handleResponse(res, id));

        try {
            return result.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            OffsetDateTime endTime = OffsetDateTime.now();
            return new ApplicationStatusResponse.Failure(Duration.between(startTime, endTime), retriesAmount);
        }
    }

    private ApplicationStatusResponse handleResponse(Response response, String id) {
        if (response instanceof Response.Success) {
            return new ApplicationStatusResponse.Success(((Response.Success) response).applicationId(), ((Response.Success) response).applicationStatus());
        } else if (response instanceof Response.RetryAfter) {
            retriesAmount ++;
            return handleRetryAfter((Response.RetryAfter) response, id);
        } else if (response instanceof Response.Failure) {
            return handleFailure();
        }
        throw new IllegalArgumentException("Unexpected response type: " + response.getClass().getName());
    }

    private ApplicationStatusResponse handleRetryAfter(Response.RetryAfter retryAfter, String id) {
        try {
            Thread.sleep(retryAfter.delay().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return performOperation(id);
    }

    private ApplicationStatusResponse handleFailure() {
        return new ApplicationStatusResponse.Failure(Duration.between(startTime, OffsetDateTime.now()), retriesAmount);
    }
}
