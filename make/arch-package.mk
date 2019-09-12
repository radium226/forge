WORK_FOLDER_PATH := /tmp/forge/arch-package

.PHONY: arch-package
arch-package:
	WORK_FOLDER_PATH="$(WORK_FOLDER_PATH)" ./make/scripts/arch-package.sh
