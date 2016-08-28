CREATE TABLE domain_event (
  global_offset   BIGSERIAL PRIMARY KEY,
  aggregate_id    VARCHAR(36)  NOT NULL,
  aggregate_type  VARCHAR(100) NOT NULL,
  command_id      VARCHAR(36)  NOT NULL,
  event_type      VARCHAR(100) NOT NULL,
  event_payload   TEXT         NOT NULL,
  event_timestamp TIMESTAMP    NOT NULL,
  sequence_number BIGINT       NOT NULL
);

CREATE INDEX events_for_aggregate_instance_idx ON domain_event (aggregate_type, aggregate_id);

CREATE TABLE aggregate_root (
  aggregate_id      VARCHAR(36)  NOT NULL,
  aggregate_type    VARCHAR(100) NOT NULL,
  aggregate_version BIGINT       NOT NULL,
  PRIMARY KEY (aggregate_id, aggregate_type)
);