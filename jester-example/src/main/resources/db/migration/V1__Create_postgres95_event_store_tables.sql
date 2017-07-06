CREATE TABLE domain_event (
  global_offset   BIGSERIAL PRIMARY KEY,
  event_id        VARCHAR(36)  NOT NULL,
  aggregate_id    VARCHAR(36)  NOT NULL,
  aggregate_type  VARCHAR(100) NOT NULL,
  tag             VARCHAR(100) NOT NULL,
  causation_id    VARCHAR(36)  NOT NULL,
  correlation_id  VARCHAR(36)  NULL,
  event_type      VARCHAR(100) NOT NULL,
  event_version   INT          NOT NULL,
  event_payload   TEXT         NOT NULL,
  event_timestamp TIMESTAMP    NOT NULL,
  sequence_number BIGINT       NOT NULL
);

CREATE INDEX events_for_aggregate_instance_idx ON domain_event (aggregate_type, aggregate_id);

CREATE TABLE aggregate_root (
  aggregate_id      VARCHAR(36)  NOT NULL,
  aggregate_type    VARCHAR(100) NOT NULL,
  aggregate_version BIGINT       NOT NULL,
  aggregate_state   JSONB        NULL,
  PRIMARY KEY (aggregate_id, aggregate_type)
);