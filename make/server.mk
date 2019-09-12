modules/forge/target/scala-2.12/forge.jar:
	sbt "set test in assembly := {}" clean assembly

.PHONY: server
server: modules/forge/target/scala-2.12/forge.jar
	mkdir -p "/tmp/forge"
	java \
		-verbose:jni \
		--class-path="modules/forge/target/scala-2.12/forge.jar" \
		"com.github.radium226.forge.server.Main" \
			--base-folder-path="/tmp/forge" \
			--port="8080"
