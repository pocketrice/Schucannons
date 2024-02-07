package io.github.pocketrice.shared;

import lombok.Getter;

@Getter
public class Request {
    String msg;
    Object payload;
    public Request() {
        this(null, null);
    }

    public Request(String m, Object obj) {
        msg = m;
        payload = obj;
    }
}
