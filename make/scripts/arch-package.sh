#!/bin/bash

set -euo pipefail

main()
{
  declare work_folder_path="${WORK_FOLDER_PATH:-$( mktemp -d )}"
  declare git_repo_url="$( git config --get remote.origin.url )"

  mkdir -p "${WORK_FOLDER_PATH}"

  cd "${work_folder_path}"
  git clone \
    --single-branch \
    --branch "arch-package" \
      "${git_repo_url}" \
      "."

  makepkg \
    --cleanbuild \
    --clean \
    --skipinteg

  cd -
}

main "${@}"
