package org.magemello.sys.node.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.http.HttpStatus;

public class Response {

    private String message;

    private HttpStatus status;

    @JsonCreator
    public Response() {
    }

    public Response(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Response{" +
                "message='" + message + '\'' +
                ", status=" + status +
                '}';
    }
}
