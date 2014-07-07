Akka Batching Example
===
![Akka](akka.png)

Overview
===
The purpose of this page is to give an overview of how we could use the Akka framework to implement batching and improve performance of the 'user activity vault' service.

Akka is a toolkit and runtime for building highly concurrent, distributed, and fault tolerant event-driven applications on the JVM.
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

* **Message** - an incoming single request (e.g HTTP request) with some content
* **Batch** - a batch of a number of Messages
* **Flush** - a signal to flush a batch of messages







