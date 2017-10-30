#!/usr/bin/env bash

mvn versions:set
mvn clean deploy
mvn scm:tag
mvn versions:set