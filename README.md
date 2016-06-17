# Jester
#### A simple DDD, CQRS and Eventsourcing library for Java

Jester was dreamed up to be a simple, lightweight library to support building Java applications based on DDD/CQRS/Eventsourcing techniques. There's definitely an emphasis here on _library_ - Jester is not intended to be a bulky _framework_. The objective is to provide a set of simple abstractions that enable developers to overcome the typical, practical challenges commonly faced when building DDD applications.

Jester takes inspiration in places from Lightbend's (Lagom Framework)[http://lagomframework.com], especially in the way Aggregate behaviours are specified. However, it differs considerably under the covers, aiming to provide a much simpler operational model (e.g. no need to run an Akka cluster). Lagom is a large, opinionated framework that covers the end-to-end creation of reactive microservices - Jester focuses only on your domain model. In fact, it would be perfectly possible to use Jester within a Lagom application as a drop in replacement for Lagom's persistent entity and persistent read side support.

Jester's design is sympathetic to those wishing to follow the (hexagonal architecture)[http://alistair.cockburn.us/Hexagonal+architecture] pattern (aka Ports and Adapters). Jester allows you to keep your domain model isolated from the outer application and infrastructure layers, thus preventing technical concerns leaking into the domain, and making it very simple to test your business invariants in clear, concise ways.

## User Manual

### Persistence

#### Eventsourcing

### Aggregates

#### Defining behaviour

##### Commands

##### Events

##### Testing

### Command Handling

#### Executing commands

#### Idempotency

##### Deduplication strategies

### The Read-side (CQRS)

#### Aggregate event streams

#### Creating read models

#### Publishing events to messaging middleware

### Future

#### Aggregate state snapshots

#### Process managers

#### Event store implementations

#### Event schema evolutions


