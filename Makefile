SHELL=/bin/bash
.SHELLFLAGS = -e -u -c
.ONESHELL:

PORT := 1234
URL := http://localhost:$(PORT)
FOLDER_PATH := /tmp/archlinux

.PHONY: clean
clean:
	sbt clean

forge/target/scala-2.12/maven.jar:
	sbt "maven/assembly"

.PHONY: maven-e2e-test
maven-e2e-test:
	set -m
	sbt "clean" "maven/assembly"
	java -jar "./forge/target/scala-2.12/maven.jar" --folder="$(FOLDER_PATH)" --port="1234" 2>&1 &
	trap "kill %1" 0
	cd "e2e-test/maven"
	mvn --settings "./settings.xml" \
	 	clean \
	 	deploy \
			-DaltDeploymentRepository="e2e::default::$(URL)/maven2"
	cd -

	find "$(FOLDER_PATH)" -type "f"

.PHONY: pacman-e2e-test
pacman-e2e-test:
	set -m
	sbt "clean" "maven/assembly"
	java -jar "./forge/target/scala-2.12/maven.jar" --folder="$(FOLDER_PATH)" --port="1234" 2>&1 &
	trap "kill %1" 0

	cd "e2e-test/pacman"
	makepkg --force

	curl -u "root:root" --upload-file "./fake-1-1-any.pkg.tar.xz" "$(URL)/archlinux/radium226"
	cd -

	find "$(FOLDER_PATH)" -type "f"

.PHONY: e2e-test
e2e-test: maven-e2e-test pacman-e2e-test

.PHONY: start
start: forge/target/scala-2.12/maven.jar
	java -jar "forge/target/scala-2.12/maven.jar" --folder="$(FOLDER_PATH)" --port=$(PORT)