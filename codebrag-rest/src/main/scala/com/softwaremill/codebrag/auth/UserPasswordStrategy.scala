package com.softwaremill.codebrag.auth

import org.scalatra.ScalatraBase
import org.scalatra.auth.ScentryStrategy
import com.softwaremill.codebrag.service.user.Authenticator
import com.softwaremill.codebrag.service.data.UserJson
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

class UserPasswordStrategy(protected val app: ScalatraBase, login: String, password: String, val authenticator: Authenticator) extends ScentryStrategy[UserJson] {

  override def name: String = UserPassword.name

  override def isValid(implicit request: HttpServletRequest) = {
    !login.isEmpty && !password.isEmpty
  }

  override def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    authenticator.authenticate(login, password)
  }

}

object UserPassword {

  val name = "UserPassword"

}
