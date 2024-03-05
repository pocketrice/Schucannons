package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Connection;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MatchData {
    List<Connection> cons;
    boolean aReady, bReady; // Multipurpose! Just reset when done.

    public MatchData(Connection... cons) {
        this.cons = new ArrayList<>();
        this.cons.addAll(List.of(cons));
    }

    public void addCon(Connection con) {
        cons.add(con);
    }

    public void disCon(Connection con) {
        cons.remove(con);
    }

    public boolean hasCon(Connection con) {
        return cons.contains(con);
    }

    public void setA(boolean isReady) {
        aReady = isReady;
    }

    public void setB(boolean isReady) {
        bReady = isReady;
    }

    public boolean setSel(Connection con, boolean isReady) {
        boolean isSet = true;

        switch (cons.indexOf(con)) {
            case 0 -> setA(isReady);
            case 1 -> setB(isReady);
            default -> isSet = false;
        }

        return isSet;
    }
}
