package com.softwaremill.codebrag.dao.user

import org.bson.types.ObjectId
import com.softwaremill.codebrag.domain._
import org.joda.time.DateTime
import scala.slick.model.ForeignKeyAction
import com.softwaremill.codebrag.dao.sql.SQLDatabase

trait SQLUserSchema {
  val database: SQLDatabase

  import database.driver.simple._
  import database._

  protected case class SQLAuth(id: ObjectId, provider: String, username: String, usernameLowerCase: String, token: String, salt: String) {
    def toAuth = Authentication(provider, username, usernameLowerCase, token, salt)
  }
  protected def toSQLAuth(id: ObjectId, auth: Authentication) = SQLAuth(id, auth.provider, auth.username, auth.usernameLowerCase,
    auth.token, auth.salt)

  protected case class SQLSettings(id: ObjectId, avatarUrl: String, emailNotificationsEnabled: Boolean,
    dailyUpdatesEmailEnabled: Boolean, appTourDone: Boolean, toReviewStartDate: Option[DateTime]) {
    def toSettings = UserSettings(avatarUrl, emailNotificationsEnabled, dailyUpdatesEmailEnabled, appTourDone, toReviewStartDate)
  }
  protected def toSQLSettings(id: ObjectId, settings: UserSettings) = {
    SQLSettings(
      id,
      settings.avatarUrl,
      settings.emailNotificationsEnabled,
      settings.dailyUpdatesEmailEnabled,
      settings.appTourDone,
      settings.toReviewStartDate
    )
  }

  protected case class SQLLastNotif(id: ObjectId, commits: Option[DateTime], followups: Option[DateTime]) {
    def toLastNotif = LastUserNotificationDispatch(commits, followups)
  }
  protected def toSQLLastNotif(id: ObjectId, lastNotif: LastUserNotificationDispatch) = SQLLastNotif(id,
    lastNotif.commits, lastNotif.followups)

  protected type UserTuple = (ObjectId, String, String, String, Boolean, Boolean, Boolean)

  protected class Auths(tag: Tag) extends Table[SQLAuth](tag, "users_authentications") {
    def userId = column[ObjectId]("user_id", O.PrimaryKey)
    def provider = column[String]("provider")
    def username = column[String]("username")
    def usernameLowerCase = column[String]("username_lowercase")
    def token = column[String]("token")
    def salt = column[String]("salt")

    def * = (userId, provider, username, usernameLowerCase, token, salt) <> (SQLAuth.tupled, SQLAuth.unapply)
  }

  protected val auths = TableQuery[Auths]

  protected class Settings(tag: Tag) extends Table[SQLSettings](tag, "users_settings") {
    def userId = column[ObjectId]("user_id", O.PrimaryKey)
    def avatarUrl = column[String]("avatar_url")
    def emailNotificationsEnabled = column[Boolean]("email_notif")
    def dailyUpdatesEmailEnabled = column[Boolean]("email_daily_updates")
    def appTourDone = column[Boolean]("app_tour_done")
    def toReviewStartDate = column[Option[DateTime]]("to_review_start_date")

    def * = (userId, avatarUrl, emailNotificationsEnabled, dailyUpdatesEmailEnabled, appTourDone, toReviewStartDate) <> (SQLSettings.tupled, SQLSettings.unapply)
  }

  protected val userSettings = TableQuery[Settings]

  protected class LastNotifs(tag: Tag) extends Table[SQLLastNotif](tag, "users_last_notifs") {
    def userId = column[ObjectId]("user_id", O.PrimaryKey)
    def lastCommitsDispatch = column[Option[DateTime]]("last_commits_dispatch")
    def lastFollowupsDispatch = column[Option[DateTime]]("last_followups_dispatch")

    def * = (userId, lastCommitsDispatch, lastFollowupsDispatch) <> (SQLLastNotif.tupled, SQLLastNotif.unapply)
  }

  protected val lastNotifs = TableQuery[LastNotifs]

  protected class Users(tag: Tag) extends Table[UserTuple](tag, "users") {
    def id                = column[ObjectId]("id", O.PrimaryKey)
    def name              = column[String]("name")
    def emailLowerCase    = column[String]("email_lowercase")
    def token             = column[String]("token")
    def regular           = column[Boolean]("regular")
    def admin             = column[Boolean]("admin")
    def active            = column[Boolean]("active")

    def auth = foreignKey("auth_fk", id, auths)(_.userId, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
    def settings = foreignKey("settings_fk", id, userSettings)(_.userId, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
    def lastNotif = foreignKey("last_notifs_fk", id, lastNotifs)(_.userId, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
    def aliases = foreignKey("aliases_fk", id, userAliases)(_.userId, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    def * = (id, name, emailLowerCase, token, regular, admin, active)
  }

  protected val users = TableQuery[Users]

  protected case class SQLUserAlias(id: ObjectId, userId: ObjectId, emailAlias: String) {
    def toUserAlias = UserAlias(id, userId, emailAlias)
  }

  protected def toSQLUserAlias(userAlias: UserAlias) = SQLUserAlias(userAlias.id, userAlias.userId, userAlias.alias)

  protected class UserAliases(tag: Tag) extends Table[SQLUserAlias](tag, "user_aliases") {
    def id = column[ObjectId]("id", O.PrimaryKey)
    def userId = column[ObjectId]("user_id")
    def alias = column[String]("alias")

    def * = (id, userId, alias) <> (SQLUserAlias.tupled, SQLUserAlias.unapply)
  }

  protected val userAliases = TableQuery[UserAliases]

  protected def tuple(user: User): UserTuple = (user.id, user.name, user.emailLowerCase, user.token, true, user.admin, user.active)

  protected val untuple: ((UserTuple, SQLAuth, SQLSettings, SQLLastNotif, List[SQLUserAlias])) => User = {
    case (tuple, sqlAuth, sqlSettings, sqlLastNotif, sqlAliases) =>
      User(tuple._1, sqlAuth.toAuth, tuple._2, tuple._3, tuple._4, tuple._6, tuple._7, sqlSettings.toSettings, sqlLastNotif.toLastNotif, UserAliases(sqlAliases.map(_.toUserAlias).toSet))
  }
}
