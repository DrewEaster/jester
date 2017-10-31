package com.dreweaster.ddd.jester.application.repository.monitoring;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.dreweaster.ddd.jester.application.eventstore.PersistedEvent;
import com.dreweaster.ddd.jester.domain.*;
import io.vavr.collection.List;
import io.vavr.control.Option;

public class DropwizardMetricsAggregateRepositoryReporter implements AggregateRepositoryReporter {

    private MetricRegistry metricRegistry;

    private String metricNamePrefix;

    public DropwizardMetricsAggregateRepositoryReporter(MetricRegistry metricRegistry, String metricNamePrefix) {
        this.metricRegistry = metricRegistry;
        this.metricNamePrefix = metricNamePrefix;
    }

    @Override
    public <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> CommandHandlingProbe<A, C, E, State> createProbe(
            AggregateType<A, C, E, State> aggregateType, AggregateId aggregateId) {

        return new DropwizardMetricsCommandHandlingProbe<>(aggregateType);
    }

    private class DropwizardMetricsCommandHandlingProbe<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements CommandHandlingProbe<A,C,E,State> {

        private AggregateType<A,C,E,State> aggregateType;

        private Option<AggregateRepository.CommandEnvelope<C>> command = Option.none();

        private Option<Timer.Context> commandHandlingTimerContext = Option.none();

        private Option<Timer.Context> loadEventsTimerContext = Option.none();

        private Option<Timer.Context> applyCommandTimerContext = Option.none();

        private Option<Timer.Context> persistEventsTimerContext = Option.none();

        DropwizardMetricsCommandHandlingProbe(AggregateType<A, C, E, State> aggregateType) {
            this.aggregateType = aggregateType;
        }

        @Override
        public void startedHandling(AggregateRepository.CommandEnvelope<C> command) {

            if(this.command.isEmpty()) {
                this.command = Option.of(command);
            }

            if(commandHandlingTimerContext.isEmpty()) {
                commandHandlingTimerContext = Option.of(metricRegistry.timer(commandSpecificMetricName(command, "execution")).time());
            }
        }

        @Override
        public void startedLoadingEvents() {
            if(loadEventsTimerContext.isEmpty()) {
                loadEventsTimerContext = Option.of(metricRegistry.timer(aggregateTypeSpecificMetricName("load-events", "execution")).time());
            }
        }

        @Override
        public void finishedLoadingEvents(List<PersistedEvent<A, E>> previousEvents) {
            loadEventsTimerContext.forEach(Timer.Context::stop);
            metricRegistry.counter(aggregateTypeSpecificMetricName("load-events", "success")).inc();
        }

        @Override
        public void finishedLoadingEvents(Throwable unexpectedException) {
            loadEventsTimerContext.forEach(Timer.Context::stop);
            metricRegistry.counter(aggregateTypeSpecificMetricName("load-events", "failure")).inc();
        }

        @Override
        public void startedApplyingCommand() {
            if(applyCommandTimerContext.isEmpty()) {
                applyCommandTimerContext = Option.of(metricRegistry.timer(aggregateTypeSpecificMetricName("apply-command", "execution")).time());
            }
        }

        @Override
        public void commandApplicationAccepted(List<? super E> events, boolean deduplicated) {
            applyCommandTimerContext.forEach(Timer.Context::stop);
            if(deduplicated) {
                metricRegistry.counter(aggregateTypeSpecificMetricName("apply-command", "success", "deduplicated")).inc();
            } else {
                metricRegistry.counter(aggregateTypeSpecificMetricName("apply-command", "success", "processed")).inc();
            }
        }

        @Override
        public void commandApplicationRejected(Throwable rejection, boolean deduplicated) {
            applyCommandTimerContext.forEach(Timer.Context::stop);
            if(deduplicated) {
                metricRegistry.counter(aggregateTypeSpecificMetricName("apply-command", "rejected", "deduplicated")).inc();
            } else {
                metricRegistry.counter(aggregateTypeSpecificMetricName("apply-command", "rejected", "processed")).inc();
            }
        }

        @Override
        public void commandApplicationFailed(Throwable unexpectedException) {
            applyCommandTimerContext.forEach(Timer.Context::stop);
            metricRegistry.counter(aggregateTypeSpecificMetricName("apply-command", "failed")).inc();
        }

        @Override
        public void startedPersistingEvents(List<? super E> events, long expectedSequenceNumber) {
            if(persistEventsTimerContext.isEmpty()) {
                persistEventsTimerContext = Option.of(metricRegistry.timer(aggregateTypeSpecificMetricName("persist-events", "execution")).time());
            }
        }

        @Override
        public void startedPersistingEvents(List<? super E> events, State state, long expectedSequenceNumber) {
            startedPersistingEvents(events, expectedSequenceNumber);
        }

        @Override
        public void finishedPersistingEvents(List<PersistedEvent<A, E>> persistedEvents) {
            persistEventsTimerContext.forEach(Timer.Context::stop);
            metricRegistry.counter(aggregateTypeSpecificMetricName("persist-events", "success")).inc();
        }

        @Override
        public void finishedPersistingEvents(Throwable unexpectedException) {
            persistEventsTimerContext.forEach(Timer.Context::stop);
            metricRegistry.counter(aggregateTypeSpecificMetricName("persist-events", "failure")).inc();
        }

        @Override
        public void finishedHandling(AggregateRepository.CommandHandlingResult<C, E> result) {

            commandHandlingTimerContext.forEach(Timer.Context::stop);

            command.forEach( command -> {
                if(result instanceof AggregateRepository.SuccessResult) {
                    if(((AggregateRepository.SuccessResult) result).wasDeduplicated()) {
                        metricRegistry.counter(commandSpecificMetricName(command, "result", "success", "deduplicated")).inc();
                    } else {
                        metricRegistry.counter(commandSpecificMetricName(command, "result", "success", "processed")).inc();

                        ((AggregateRepository.SuccessResult<C, E>) result).generatedEvents().forEach(event ->
                            metricRegistry.counter(eventSpecificMetricName(event)).inc()
                        );
                    }
                } else if(result instanceof AggregateRepository.RejectionResult) {
                    if(((AggregateRepository.RejectionResult) result).wasDeduplicated()) {
                        metricRegistry.counter(commandSpecificMetricName(command, "result", "rejection", "deduplicated")).inc();
                    } else {
                        metricRegistry.counter(commandSpecificMetricName(command, "result", "rejection", "processed")).inc();
                    }
                } else if(result instanceof AggregateRepository.ConcurrentModificationResult) {
                    metricRegistry.counter(commandSpecificMetricName(command, "result", "concurrent-modification")).inc();
                }
            });
        }

        @Override
        public void finishedHandling(Throwable unexpectedException) {
            command.forEach( command ->
                metricRegistry.counter(commandSpecificMetricName(command, "result", "failure")).inc()
            );
        }

        private String commandSpecificMetricName(AggregateRepository.CommandEnvelope<C> command, String...names) {
            return MetricRegistry.name(List.of(metricNamePrefix, aggregateType.name(), "commands", command.command().getClass().getSimpleName()).mkString("."), names);
        }

        private String eventSpecificMetricName(E event, String...names) {
            return MetricRegistry.name(List.of(metricNamePrefix, aggregateType.name(), "events", event.getClass().getSimpleName()).mkString("."), names);
        }

        private String aggregateTypeSpecificMetricName(String...names) {
            return MetricRegistry.name(List.of(metricNamePrefix, aggregateType.name(), "command-handling").mkString("."), names);
        }
    }
}
