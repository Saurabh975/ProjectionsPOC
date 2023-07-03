#!/bin/bash
echo "==================     Help for cqlsh    ========================="
echo "DESCRIBE tables            : Prints all tables in the current keyspace"
echo "DESCRIBE keyspaces         : Prints all keyspaces in the current cluster"
echo "DESCRIBE <table_name>      : Prints table detail information"
echo "help                       : for more cqlsh commands"
echo "help [cqlsh command]       : Gives information about cqlsh commands"
echo "quit                       : quit"
echo "=================================================================="
docker exec -it cassandraProjectionsPOC cqlsh

#To create Keyspace
#CREATE KEYSPACE music_keyspace WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

#To see the created Keyspace
#DESC KEYSPACES;

#CREATE TABLE music_keyspace.music_by_genre (
 #  genre text,
 #  music text,
 #  PRIMARY KEY (genre, music)
 #);

 #INSERT INTO  music_keyspace.music_by_genre (genre, music) VALUES (?, ?)