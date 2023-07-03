# Streaming-microservices

---

This is the root module which connects the others into a useful system, should utilize macwired to handle dependency
injection to each module, and each module should *mostly* be wired by function definition, with some macwiring at each
module's root

---

## Cassandra scripts (should probably be its own .md)

> WARNING, this script uses replication 1, which is bad. change that for prod! ALSO use NetworkTopologyStrategy and not
> SimpleStrategy (Managed cassandra handles this, so dw)

```
CREATE KEYSPACE IF NOT EXISTS music_keyspace WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS music_keyspace.offset_store (
                                                        projection_name text,
                                                        partition int,
                                                        projection_key text,
                                                        offset text,
                                                        manifest text,
                                                        last_updated timestamp,
                                                        PRIMARY KEY ((projection_name, partition), projection_key)) WITH default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.projection_management (
                                                                 projection_name text,
                                                                 partition int,
                                                                 projection_key text,
                                                                 paused boolean,
                                                                 last_updated timestamp,
                                                                 PRIMARY KEY ((projection_name, partition), projection_key)) WITH default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.messages (
                                                    persistence_id text,
                                                    partition_nr bigint,
                                                    sequence_nr bigint,
                                                    timestamp timeuuid,
                                                    timebucket text,
                                                    writer_uuid text,
                                                    ser_id int,
                                                    ser_manifest text,
                                                    event_manifest text,
                                                    event blob,
                                                    meta_ser_id int,
                                                    meta_ser_manifest text,
                                                    meta blob,
                                                    tags set<text>,
                                                    PRIMARY KEY ((persistence_id, partition_nr), sequence_nr, timestamp))
    WITH gc_grace_seconds =864000
     AND compaction = {
        'class' : 'SizeTieredCompactionStrategy',
        'enabled' : true,
        'tombstone_compaction_interval' : 86400,
        'tombstone_threshold' : 0.2,
        'unchecked_tombstone_compaction' : false,
        'bucket_high' : 1.5,
        'bucket_low' : 0.5,
        'max_threshold' : 32,
        'min_threshold' : 4,
        'min_sstable_size' : 50
        }
    AND default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.tag_views (
                                                     tag_name text,
                                                     persistence_id text,
                                                     sequence_nr bigint,
                                                     timebucket bigint,
                                                     timestamp timeuuid,
                                                     tag_pid_sequence_nr bigint,
                                                     writer_uuid text,
                                                     ser_id int,
                                                     ser_manifest text,
                                                     event_manifest text,
                                                     event blob,
                                                     meta_ser_id int,
                                                     meta_ser_manifest text,
                                                     meta blob,
                                                     PRIMARY KEY ((tag_name, timebucket), timestamp, persistence_id, tag_pid_sequence_nr))
    WITH gc_grace_seconds =864000
     AND compaction = {
        'class' : 'SizeTieredCompactionStrategy',
        'enabled' : true,
        'tombstone_compaction_interval' : 86400,
        'tombstone_threshold' : 0.2,
        'unchecked_tombstone_compaction' : false,
        'bucket_high' : 1.5,
        'bucket_low' : 0.5,
        'max_threshold' : 32,
        'min_threshold' : 4,
        'min_sstable_size' : 50
        }
    AND default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.tag_write_progress(
                                                             persistence_id text,
                                                             tag text,
                                                             sequence_nr bigint,
                                                             tag_pid_sequence_nr bigint,
                                                             offset timeuuid,
                                                             PRIMARY KEY (persistence_id, tag)) WITH default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.tag_scanning(
                                                       persistence_id text,
                                                       sequence_nr bigint,
                                                       PRIMARY KEY (persistence_id)) WITH default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.metadata(
                                                   persistence_id text PRIMARY KEY,
                                                   deleted_to bigint,
                                                   properties map<text,text>) WITH default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.all_persistence_ids(
    persistence_id text PRIMARY KEY) WITH default_time_to_live = 220752000;

CREATE TABLE IF NOT EXISTS music_keyspace.snapshots (
                                                     persistence_id text,
                                                     sequence_nr bigint,
                                                     timestamp bigint,
                                                     ser_id int,
                                                     ser_manifest text,
                                                     snapshot_data blob,
                                                     snapshot blob,
                                                     meta_ser_id int,
                                                     meta_ser_manifest text,
                                                     meta blob,
                                                     PRIMARY KEY (persistence_id, sequence_nr))
    WITH CLUSTERING ORDER BY (sequence_nr DESC) AND gc_grace_seconds =864000
     AND compaction = {
        'class' : 'SizeTieredCompactionStrategy',
        'enabled' : true,
        'tombstone_compaction_interval' : 86400,
        'tombstone_threshold' : 0.2,
        'unchecked_tombstone_compaction' : false,
        'bucket_high' : 1.5,
        'bucket_low' : 0.5,
        'max_threshold' : 32,
        'min_threshold' : 4,
        'min_sstable_size' : 50
        }
    AND default_time_to_live = 220752000;


```

truncate commands so we don't repeat mistakes

```
truncate messages;
truncate all_persistence_ids;
truncate metadata;
truncate offset_store;
truncate projection_management;
truncate snapshots;
truncate tag_scanning;
truncate tag_views;
truncate tag_write_progress;
```