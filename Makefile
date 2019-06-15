SHELL=/bin/bash
.SHELLFLAGS = -e -u -c
.ONESHELL:

PORT := 1234
REPOSITORY_URL := http://localhost:$(PORT)/maven2
FOLDER_PATH := $(shell mktemp -d)

.PHONY: e2e-test
e2e-test:
	set -m
	sbt "clean" "maven/assembly"
	java -jar "./maven/target/scala-2.12/maven.jar" --folder="$(FOLDER_PATH)" --port="1234" 2>&1 &
	trap "kill %1" 0
	cd "e2e-test/fake"
	mvn --settings "./settings.xml" \
	 	clean \
	 	deploy \
			-DaltDeploymentRepository="e2e::default::$(REPOSITORY_URL)"
	cd -
	find "$(FOLDER_PATH)" -type "f"
