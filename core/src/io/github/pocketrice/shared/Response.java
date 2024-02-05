package io.github.pocketrice.shared;

import lombok.Getter;

@Getter
public class Response {
    String msg;
    Object payload;

    public Response(String m, Object obj) {
        msg = m;
        payload = obj;
    }
}
