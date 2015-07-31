package br.com.riocard;

import com.riocard.oscar.cmrouter.AsyncCardProcessor;
import com.riocard.oscar.cmrouter.CardProcessorEventHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by raphael.costa on 31/07/2015.
 */
public class Adapter implements CardProcessorEventHandler, Observable.OnSubscribe<CardEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(Adapter.class);
    private AsyncCardProcessor asyncProcessor;
    private Subscriber<? super CardEvent> subscriber;

    public Adapter(AsyncCardProcessor asyncProcessor) {
        this.asyncProcessor = asyncProcessor;
    }

    //recebe comandos do browser
    public void submit(String operation, long waitTimeout) {
        LOG.info("Java <<< JS: submit({}, {})", operation, waitTimeout);
        for (String op : StringUtils.split(operation, '|')) {
            asyncProcessor.submitFullyAsync(op, waitTimeout);
        }
    }

    //Manda mensagens de volta para o browser
    @Override
    public void handleEvent(int event, Object[] parameters) {
        subscriber.onNext(new CardEvent(event, parameters));
    }

    public void cancelWait() {
        LOG.info("Java <<< JS: cancelWait()");
        asyncProcessor.cancelWait();
    }

    public void resume() {
        LOG.info("Java <<< JS: resume()");
        asyncProcessor.resume();
    }

    @Override
    public void call(Subscriber<? super CardEvent> subscriber) {
        this.subscriber = subscriber;
    }
}
