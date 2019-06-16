pkgver="r11.90af915"
pkgrel="1"

pkgname=("forge-git")

arch=("any")

pkgver() {
  cd "scraping"
  printf "r%s.%s" "$(git rev-list --count HEAD)" "$(git rev-parse --short HEAD)"
}

makedepends=(
  "scala"
  "sbt"
)

depends=(
    "java-runtime"
)

source=(
    "git+http://https://github.com/radium226/forge.git"
    "forged"
    "systemd.service"
    "sysusers.conf"
    "tmpfiles.conf"
)

build() {
    cd "forge"
    sbt "maven/assembly"
}

package() {
    install -Dm0644 \
      "${srcdir}/forge/forge/target/scala-2.12/forge.jar" \
      "${pkgdir}/usr/lib/forge/share/forge.jar"

    install -Dm0755 \
      "${srcdir}/forged" \
      "${pkgdir}/usr/lib/forge/bin/forged"

    install -Dm0644 \
      "${startdir}/systemd.service" \
      "${pkgdir}/usr/lib/systemd/system/forge.service"

    install -Dm0644 \
      "${startdir}/sysusers.conf" \
      "${pkgdir}/usr/lib/sysusers.d/forge.conf"

    install -Dm0644 \
      "${startdir}/tmpfiles.conf" \
      "${pkgdir}/usr/lib/tmpfiles.d/forge.conf"
}
