package example.akka.batch.actors;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import example.akka.batch.messages.Batch;
import example.akka.batch.messages.Flush;
import example.akka.batch.messages.Message;

import java.util.ArrayList;
import java.util.List;

public class BatchActor extends UntypedActor {

    private ActorRef persister;

    private int batchSize;

    private List<Message> batch;

    public BatchActor(ActorRef persister, int batchSize) {
        this.batch = new ArrayList<Message>();
        this.persister = persister;
        this.batchSize = batchSize;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Message) {
            batch(message);
        }
        if (message instanceof Flush) {
            flush();
        }
    }

    private void batch(Object message) {
        batch.add((Message) message);
        if (batch.size() >= batchSize) {
            flush();
        }
    }

    private void flush() {
        // Send batch onto persister layer
        Batch b = new Batch(batch);
        persister.tell(b, getSelf());

        // reset batch to empty
        batch = new ArrayList<Message>();
    }

}
