package com.github.radium226.forge.model

import com.github.radium226.forge.project.Project

case class Hook[F[_]](project: Project[F], sha1: Sha1)
