package handler.impl;

import client.Client;
import handler.Handler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class HandlerImpl implements Handler {
    private final Client client;
    private final ExecutorService executorService;
    private final BlockingQueue<Event> queue;

    public record Payload(String origin, byte[] data) {
    }

    public record Address(String datacenter, String nodeId) {
    }

    public record Event(List<Address> recipients, Payload payload) {
    }

    public enum Result {ACCEPTED, REJECTED};

    public HandlerImpl(Client client, int threadCount) {
        this.client = client;
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(1);
    }

    @Override
    public void performOperation() {
        executorService.execute(() -> {
            Event readEvent = client.readData();
            try {
                queue.put(readEvent);
                if (!queue.isEmpty()) {
                    try {
                        Event eventToSend = queue.take();
                        for (Address address : eventToSend.recipients()) {
                            Result result = client.sendData(address, eventToSend.payload());
                            if (result == Result.ACCEPTED) {
                                System.out.printf("Операция отправки данных адресату %s считается завершенной", address);
                            } else {
                                System.out.printf("Операцию отправки для адресата %s следует повторить после задержки 'timeout()'", address);
                                try {
                                    Thread.sleep(timeout().toMillis());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}