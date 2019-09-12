#!/bin/bash

main()
{
  declare folder_path="$( mktemp -d )"
  declare git_repo_url="$( git config --get remote.origin.url )"
  cd "${folder_path}"
  git clone \
    --single-branch \
    --branch"arch-package" \
      "${git_repo_url}"

  makepkg \
    --cleanbuild \
    --clean \
    --skipinteg

  cd -
}

main "${@}"
