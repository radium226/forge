SHELL=/bin/bash
.SHELLFLAGS = -e -u -c
.ONESHELL:

SCALA_VERSION = 2.13

.PHONY: clean
clean:
	sbt clean
	rm "packages/arch/emit-hook" || true
	rm "packages/arch/forge.jar" || true
	find "packages/arch" -name "*.pkg.tar.xz" | xargs -I {} rm "{}"


modules/forge/target/scala-$(SCALA_VERSION)/forge.jar:
	sbt assembly

packages/arch/forge.jar: modules/forge/target/scala-$(SCALA_VERSION)/forge.jar
	cp "modules/forge/target/scala-$(SCALA_VERSION)/forge.jar" "packages/arch/forge.jar"

package/arch/emit-hook:
	cp "scripts/emit-hook" "packages/arch/emit-hook"

.PHONY: arch-package
arch-package: packages/arch/forge.jar package/arch/emit-hook
	cd "packages/arch" && \
	makepkg \
		--cleanbuild \
		--clean \
		--skipinteg \
		--force

.PHONY: arch-install
arch-install: arch-package
	find "packages/arch" -name "*.pkg.tar.xz" | xargs -I {} sudo pacman -U "{}" --noconfirm
