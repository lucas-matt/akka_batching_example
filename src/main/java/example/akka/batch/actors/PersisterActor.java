package example.akka.batch.actors;

import akka.actor.UntypedActor;
import example.akka.batch.messages.Batch;

public class PersisterActor extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Batch) {
            Batch batch = (Batch) message;
            System.out.println(String.format(
                    "WRITE BATCH %s [%s]",
                    batch.getBatchedContent().size(),
                    getSelf().path()
            ));
        }
    }

}
