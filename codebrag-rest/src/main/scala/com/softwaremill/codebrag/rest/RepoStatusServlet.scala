package com.softwaremill.codebrag.rest

import com.typesafe.scalalogging.slf4j.Logging
import com.softwaremill.codebrag.service.user.Authenticator
import com.softwaremill.codebrag.domain.RepositoryStatus
import com.softwaremill.codebrag.dao.repositorystatus.RepositoryStatusDAO
import com.softwaremill.codebrag.repository.config.RepoData
import com.softwaremill.codebrag.repository.Repository

class RepoStatusServlet(val authenticator: Authenticator, repository: Repository, repoStatusDao: RepositoryStatusDAO) extends JsonServletWithAuthentication with Logging {

  get("/") {
    getRepositoryStatus(repository.repoData)
  }

  private def getRepositoryStatus(repoData: RepoData): Map[String, RepositoryStatus] = {
    repoStatusDao.getRepoStatus(repoData.repoName) match {
      case Some(status) => Map("repoStatus" -> status)
      case None => {
        logger.debug(s"No status found for ${repoData.repoName}, assuming it is first run and repo is being cloned at the moment.")
        Map("repoStatus" -> RepositoryStatus.notReady(repoData.repoName))
      }
    }
  }

}

object RepoStatusServlet {
  val Mapping = "repoStatus"
}