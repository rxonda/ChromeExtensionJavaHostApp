package br.com.riocard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by raphael.costa on 29/07/2015.
 */
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private final Gson serializer;
    private final AtomicBoolean interrompe;

    public Application() {
        this.serializer = new GsonBuilder().create();
        this.interrompe = new AtomicBoolean(false);
    }

    public static void main(String[] args) {
        LOG.debug("Iniciando a aplicacao...");

        final Application app = new Application();
        ConnectableObservable<String> obs = app.getObservable();
        obs.observeOn(Schedulers.computation())
                .subscribe(new Observer<String>() {
                    public void onCompleted() {}

                    public void onError(Throwable throwable) {}

                    public void onNext(String s) {
                        Text received = app.serializer.fromJson(s, Text.class);
                        LOG.debug("Host recebeu " + received.getText());
                        try {
                            app.sendMessage(String.format("A extensao enviou a msg: %s", s));
                        } catch (IOException e) {
                            LOG.debug("Erro inesperado dentro da iteracao", e);
                        }
                    }
                });

        obs.connect();

        while (!app.interrompe.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        System.exit(0);
    }

    private ConnectableObservable<String> getObservable() {
        ConnectableObservable<String> observable = Observable.create(new Observable.OnSubscribe<String>() {
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onStart();
                try {
                    while (true) {
                        String _s = readMessage(System.in);
                        subscriber.onNext(_s);
                    }
                } catch (InterruptedIOException ioe) {
                    LOG.debug("Conexao foi interrompida");
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).publish();

        observable.subscribe(new Observer<String>() {
            public void onCompleted() {
                LOG.debug("Fim da aplicacao");
                interrompe.set(true);
            }

            public void onError(Throwable throwable) {
                LOG.debug("Erro inesperado!", throwable);
                interrompe.set(true);
            }

            public void onNext(String s) {}
        });

        return observable;
    }

    private String readMessage(InputStream in) throws IOException {
        byte[] b = new byte[4];
        in.read(b);

        int size = getInt(b);
        LOG.debug(String.format("O tamanho eh %d", size));

        if (size == 0) {
            throw new InterruptedIOException("Comunicacao interrompida");
        }

        b = new byte[size];
        in.read(b);

        return new String(b, "UTF-8");
    }

    private void sendMessage(String message) throws IOException {
        Text text = new Text(message);
        String resposta = serializer.toJson(text);
        System.out.write(getBytes(resposta.length()));
        System.out.write(resposta.getBytes("UTF-8"));
        System.out.flush();
    }

    public int getInt(byte[] bytes) {
        return (bytes[3] << 24) & 0xff000000 |
                (bytes[2] << 16) & 0x00ff0000 |
                (bytes[1] << 8) & 0x0000ff00 |
                (bytes[0] << 0) & 0x000000ff;
    }

    public byte[] getBytes(int length) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (length & 0xFF);
        bytes[1] = (byte) ((length >> 8) & 0xFF);
        bytes[2] = (byte) ((length >> 16) & 0xFF);
        bytes[3] = (byte) ((length >> 24) & 0xFF);
        return bytes;
    }
}
