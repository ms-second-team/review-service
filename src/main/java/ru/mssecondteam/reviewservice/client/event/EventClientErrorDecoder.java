package ru.mssecondteam.reviewservice.client.event;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.mssecondteam.reviewservice.exception.NotFoundException;

public class EventClientErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String s, Response response) {
        switch (response.status()) {
            case 404:
                return new NotFoundException("Event was not found");
            default:
                return new Exception("Unknown error");
        }
    }
}
