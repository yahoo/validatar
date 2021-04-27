#!/usr/bin/env bash

set -e

export GPG_TTY=$(tty)

openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in screwdriver/pubring.gpg.enc -out screwdriver/pubring.gpg -d
openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in screwdriver/secring.gpg.enc -out screwdriver/secring.gpg -d

mvn clean deploy --settings screwdriver/settings.xml -DskipTests=true -Possrh -Prelease

rm -rf screwdriver/*.gpg
