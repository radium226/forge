#!/bin/bash

set -euo pipefail

main()
{
  declare work_folder_path="${WORK_FOLDER_PATH:=$( mktemp -d )}"
  declare git_repo_url="$( git config --get remote.origin.url )"

  mkdir -p "${WORK_FOLDER_PATH}" || true

  cd "${work_folder_path}"
  if [[ ! -d ".git" ]]; then
    git clone \
      --single-branch \
      --branch "arch-package" \
        "${git_repo_url}" \
        "."
  else
    git fetch --all
    git reset --hard "origin/arch-package"
  fi

  makepkg \
    --cleanbuild \
    --clean \
    --skipinteg \
    --install

  cd -
}

main "${@}"
