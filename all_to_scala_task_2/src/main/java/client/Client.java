package client;

import handler.impl.HandlerImpl;

public interface Client {
    //блокирующий метод для чтения данных
    HandlerImpl.Event readData();

    //блокирующий метод для отправки данных
    HandlerImpl.Result sendData(HandlerImpl.Address dest, HandlerImpl.Payload payload);
}