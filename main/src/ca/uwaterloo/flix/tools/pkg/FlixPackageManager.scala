/*
 * Copyright 2023 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.uwaterloo.flix.tools.pkg

import ca.uwaterloo.flix.tools.Packager.getLibraryDirectory
import ca.uwaterloo.flix.tools.pkg.Dependency.FlixDependency
import ca.uwaterloo.flix.tools.pkg.github.GitHub
import ca.uwaterloo.flix.util.Result.{Err, Ok, ToOk}
import ca.uwaterloo.flix.util.Result

import java.io.{IOException, PrintStream}
import java.nio.file.{Files, Path, StandardCopyOption}
import scala.util.Using

object FlixPackageManager {

  // TODO: Move functionality from "Packager" in here.
  
  //TODO: tests
  //TODO: comments

  /**
    * Installs all the Flix dependencies for a Manifest.
    */
  def installAll(manifest: Manifest, path: Path)(implicit out: PrintStream): Result[Unit, PackageError] = {
    val flixDeps = findFlixDependencies(manifest)

    for(dep <- flixDeps) {
      val depName: String = s"${dep.username}/${dep.projectName}"
      out.println(s"Installing $depName")
      install(depName, Some(dep.version), path) match {
        case Ok(_) => out.println(s"Installation of $depName completed")
        case Err(e) => out.println(s"Installation of $depName failed"); return Err(e)
      }
    }
    ().toOk
  }

  /**
    *  Installs a flix package from the Github `project`.
    *
    * `project` must be of the form `<owner>/<repo>`
    *
    * The package is installed at `lib/<owner>/<repo>`
    */
  def install(project: String, version: Option[SemVer], p: Path)(implicit out: PrintStream): Result[Unit, PackageError] = {
    val proj = GitHub.parseProject(project) match {
      case Ok(p) => p
      case Err(e) => return Err(e)
    }
    val release = (version match {
      case None => GitHub.getLatestRelease(proj)
      case Some(ver) => GitHub.getSpecificRelease(proj, ver)
    }) match {
      case Ok(release) => release
      case Err(e) => return Err(e)
    }

    val assets = release.assets.filter(_.name.endsWith(".fpkg"))
    val lib = getLibraryDirectory(p)
    val assetFolder = lib.resolve(proj.owner).resolve(proj.repo).resolve(s"ver${release.version.toString()}")

    // create the asset directory if it doesn't exist
    Files.createDirectories(assetFolder)

    // download each asset to the folder
    for (asset <- assets) {
      val path = assetFolder.resolve(asset.name)
      if(!Files.exists(path)) {
        try {
          Using(GitHub.downloadAsset(asset)) {
            stream => Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING)
          }
        } catch {
          case _: IOException => return Err(PackageError.DownloadError(s"Error occured while downloading ${asset.name}"))
        }
        out.println(s"Installation of ${asset.name} completed")
      } else {
        out.println(s"${asset.name} already exists")
      }
    }
    ().toOk
  }

  /**
    * Finds the Flix dependencies in a Manifest.
    */
  private def findFlixDependencies(manifest: Manifest): List[FlixDependency] = {
    manifest.dependencies.filter(dep => dep.isInstanceOf[FlixDependency]).map(dep => dep.asInstanceOf[FlixDependency])
  }

}
