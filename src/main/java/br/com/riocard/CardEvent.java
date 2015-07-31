package br.com.riocard;

/**
 * Created by raphael.costa on 31/07/2015.
 */
public final class CardEvent {
    private final Integer event;
    private final Object[] parameters;

    public CardEvent(Integer event, Object[] parameters) {
        this.event = event;
        this.parameters = parameters;
    }

    public CardEvent(Integer event) {
        this.event = event;
        this.parameters = new Object[]{};
    }
}
