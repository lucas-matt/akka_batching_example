package example.akka.batch;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.Broadcast;
import akka.routing.RoundRobinPool;
import example.akka.batch.actors.BatchActor;
import example.akka.batch.actors.PersisterActor;
import example.akka.batch.messages.Flush;
import example.akka.batch.messages.Message;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        final ActorSystem system = ActorSystem.create("BatchSystem");
        ActorRef persisters = createPersisters(system);
        ActorRef batchers = createBatchers(system, persisters);
        scheduleFlush(system, batchers);
        run(batchers, args);
        system.shutdown();
    }

    private static ActorRef createPersisters(ActorSystem system) {
        RoundRobinPool router = new RoundRobinPool(5);
        ActorRef persister = system.actorOf(Props.create(PersisterActor.class).withRouter(router), "persister");
        return persister;
    }

    private static ActorRef createBatchers(ActorSystem system, ActorRef persister) {
        RoundRobinPool router = new RoundRobinPool(10);
        ActorRef batcher = system.actorOf(Props.create(BatchActor.class, persister, 1000).withRouter(router), "batcher");
        return batcher;
    }

    private static void scheduleFlush(ActorSystem system, final ActorRef batcher) {
        FiniteDuration inital = Duration.create(0, TimeUnit.SECONDS);
        FiniteDuration flushPeriod = Duration.create(5, TimeUnit.SECONDS);
        final Broadcast flushAll = new Broadcast(new Flush());
        Runnable function = new Runnable() {
            @Override
            public void run() {
                batcher.tell(flushAll, null);
            }
        };
        system.scheduler().schedule(inital, flushPeriod, function, system.dispatcher());
    }

    private static void run(ActorRef batcher, String[] args) throws InterruptedException {
        long iterations = 1000000;
        long sleepFor = 0;
        Integer i = 0;
        while (i < iterations) {
            Message message = new Message(i.toString());
            batcher.tell(message, null);
            Thread.sleep(sleepFor);
            i++;
        }
    }

}
