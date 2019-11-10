pkgver=r64.eb40a64
pkgrel=1

pkgname=("forge-git")

arch=("any")

pkgver() {
  cd "forge"
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
    "git+https://github.com/radium226/forge.git"
    "git+https://github.com/radium226/http4s-fastcgi.git"
    "git+https://gitlab.com/self-hosting/system-scala.git"
    "git+https://github.com/radium226/config-scala.git"
    "forged"
    "forge-WRONG_NAME"
    "systemd.service"
    "sysusers.conf"
    "tmpfiles.conf"
    "forged.conf"
    "emit-hook"
)

build() {
    cd "forge"
    sbt "assembly" <"/dev/null"
}

package() {
    install -Dm0644 \
      "${srcdir}/forge/modules/forge/target/scala-2.13/forge.jar" \
      "${pkgdir}/usr/lib/forge/share/forge.jar"

    install -Dm0755 \
      "${srcdir}/emit-hook" \
      "${pkgdir}/usr/lib/forge/bin/emit-hook"

    install -Dm0755 \
      "${srcdir}/forged" \
      "${pkgdir}/usr/lib/forge/bin/forged"

    install -Dm0755 \
      "${srcdir}/forge-WRONG_NAME" \
      "${pkgdir}/usr/bin/forge"

    install -Dm0644 \
      "${startdir}/systemd.service" \
      "${pkgdir}/usr/lib/systemd/system/forge.service"

    install -Dm0644 \
      "${startdir}/sysusers.conf" \
      "${pkgdir}/usr/lib/sysusers.d/forge.conf"

    install -Dm0644 \
      "${startdir}/tmpfiles.conf" \
      "${pkgdir}/usr/lib/tmpfiles.d/forge.conf"

    install -Dm0644 \
      "${startdir}/forged.conf" \
      "${pkgdir}/etc/forged.conf"

    install -Dm0644 \
      "${startdir}/pam" \
      "${pkgdir}/etc/pam.d/forge"
}
