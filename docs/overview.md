Akka Batching Example
===
![Akka](akka.png)

Overview
===
The purpose of this page is to give an overview of how we could use the Akka framework to implement batching and improve performance of the 'user activity vault' service.

Akka is a toolkit and runtime for building highly concurrent, distributed, and fault tolerant event-driven applications on the JVM.

It is created and supported by Typesafe (the creators of Scala, and Play framework)

It has the following key attributes :

**Simple Concurrency & Distribution**

Asynchronous and Distributed by design. 

High-level abstractions like Actors, Futures and STM

**Resilient by Design**

Write systems that self-heal. 

Remote and/or local supervisor hierarchies.

**High Performance**

50 million msg/sec on a single machine. 

Small memory footprint; ~2.5 million actors per GB of heap.

**Elastic & Decentralized**

Adaptive load balancing, routing, partitioning and configuration-driven remoting.

**Extensible**

Use Akka Extensions to adapt Akka to fit your needs.


Basic Facts
===

Actors are very lightweight concurrent entities. They process messages asynchronously using an event-driven receive loop. 

* Each actor is essentially a single, asynchronous (non-blocking) worker "thread"

* You communicate with actors using immutable messages

* Each actor has a mailbox (or queue) of incoming messages, which it processes one at a time, in order
 
* As an actor is single threaded you can reason about concurrency more clearly, without having to worry about thread-synchonizaton or locks

* Actors can be pooled and load balanced transparently across a single JVM, multiple JVMs and multiple machines

* Actors are extremely lightweight (you can create ~2.5 million actors per Gb of heap) 

The diagram below shows a simple actor pool setup:

![Actors](actors.jpg)


Batching Example
===

Here is a simple example of an Akka actor system that could be used to batch messages to improve persistence throughput.

The key components being:

* One or more actors to batch incoming messages upto a batch size of N

* One or more actors to persist the batched messages

* A flush mechanism to push through a batch that has been hanging around for some time, and not reached the maximum size
 
* A set of messages to send between actors


Messages
---

First let's consider the messages to be sent:

### Message ###

An incoming single request (e.g HTTP request) with some content
                   

    public class Message {
    
        private final String content;
    
        public Message(String content) {
            this.content = content;
        }
    
        public String getContent() {
            return content;
        }
        
    }


### Batch ###

A batch of a number of Messages


    public class Batch<T> {
    
        private final List<T> batchedContent;
    
        public Batch(List<T> batchedContent) {
            this.batchedContent = batchedContent;
        }
    
        public List<T> getBatchedContent() {
            return batchedContent;
        }
        
    }


### Flush ###

A signal to flush a batch of messages


    public class Flush {}



Batch Actor
---

* An actor that receives many Message events, and batches them
* Once a batch reaches the required size, flushes itself and sends a Batch message onto a Persister
* Or periodically receives a Flush message and clears down


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


Persister Actor
---

* Receives a batch of messages and "persists" to a database


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


Actor System Setup
---

* Creates an actor system
* Sets up Persister Actor pool
* Sets up Batch Actor pool


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



Periodic Flush
---

* Broadcasts a flush message to all members of a pool periodically (every 5 seconds)


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
    

And send some messages ...
---

* Ok, so not an amazing input, but you get the idea :) ...


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


Resources
===

* [This Example](https://github.com/lucas1000001/akka_batching_example)
* [Akka](http://akka.io/)
* [Typesafe](http://www.typesafe.com/)
