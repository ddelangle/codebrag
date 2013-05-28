package com.softwaremill.codebrag.rest

import com.softwaremill.codebrag.service.user.Authenticator
import com.softwaremill.codebrag.AuthenticatableServletSpec
import org.scalatra.auth.Scentry
import com.softwaremill.codebrag.service.data.UserJson
import com.softwaremill.codebrag.dao.UserDAO
import org.mockito.Mockito._
import org.mockito.Matchers._
import com.softwaremill.codebrag.dao.reporting.{CommentFinder}
import com.softwaremill.codebrag.activities.AddCommentActivity
import org.bson.types.ObjectId
import com.softwaremill.codebrag.domain.{InlineCommitComment, EntireCommitComment, Authentication, User}
import org.scalatra.swagger.SwaggerEngine
import org.mockito.ArgumentCaptor
import com.softwaremill.codebrag.service.comments.command.{NewInlineCommitComment, NewEntireCommitComment}
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import com.softwaremill.codebrag.dao.reporting.views.SingleCommentView


class CommentsEndpointSpec extends AuthenticatableServletSpec with BeforeAndAfterEach {

  var commentActivity: AddCommentActivity = _
  var userDao: UserDAO = _
  var commentListFinder: CommentFinder = _

  val user = currentUser(new ObjectId)
  val commitId = new ObjectId

  override def beforeEach() {
    commentActivity = mock[AddCommentActivity]
    userDao = mock[UserDAO]
    commentListFinder = mock[CommentFinder]
  }

  def bindServlet {
    addServlet(new TestableCommentsEndpoint(fakeAuthenticator, fakeScentry, commentActivity, userDao, commentListFinder), "/*")
  }

  "POST /commits/:id/comments" should "respond with HTTP 401 when user is not authenticated" in {
    userIsNotAuthenticated
    post("/123/comments") {
      status should be(401)
    }
  }

  "POST /commits/:id/comments" should "create comment for commit" in {
    // given
    val body = "{\"body\": \"This is comment body\"}"
    val dummyComment = EntireCommitComment(new ObjectId, commitId, user.id, "This is comment body", DateTime.now)
    userIsAuthenticatedAs(UserJson(user))
    when(userDao.findById(user.id)).thenReturn(Some(user))
    when(commentActivity.addCommentToCommit(any[NewEntireCommitComment])).thenReturn(dummyComment)

    // when
    post(s"/$commitId/comments", body, Map("Content-Type" -> "application/json")) {
      // then
      status should be(200)
      val commentArgument = ArgumentCaptor.forClass(classOf[NewEntireCommitComment])
      verify(commentActivity).addCommentToCommit(commentArgument.capture())
      commentArgument.getValue.authorId should equal(user.id)
      commentArgument.getValue.commitId should equal(commitId)
      commentArgument.getValue.message should equal("This is comment body")
    }
  }

  "POST /commits/:id/comments" should "create inline comment for commit" in {
    // given
    val body = "{\"body\": \"This is comment body\", \"fileName\": \"test_file.txt\", \"lineNumber\": 20}"
    val dummyComment = InlineCommitComment(new ObjectId, commitId, user.id, "This is comment body", DateTime.now, "test_file.txt", 20)
    userIsAuthenticatedAs(UserJson(user))
    when(userDao.findById(user.id)).thenReturn(Some(user))
    when(commentActivity.addCommentToCommit(any[NewInlineCommitComment])).thenReturn(dummyComment)

    // when
    post(s"/$commitId/comments", body, Map("Content-Type" -> "application/json")) {
      // then
      status should be(200)
      val commentArgument = ArgumentCaptor.forClass(classOf[NewInlineCommitComment])
      verify(commentActivity).addCommentToCommit(commentArgument.capture())
      commentArgument.getValue.authorId should equal(user.id)
      commentArgument.getValue.commitId should equal(commitId)
      commentArgument.getValue.message should equal("This is comment body")
      commentArgument.getValue.fileName should equal(dummyComment.fileName)
      commentArgument.getValue.lineNumber should equal(dummyComment.lineNumber)
    }
  }

  "POST /commits/:id/comments" should "return created comment in response" in {
    // given
    val body = "{\"body\": \"This is comment body\"}"
    val createdComment = EntireCommitComment(new ObjectId, commitId, user.id, "This is comment body", DateTime.now)
    userIsAuthenticatedAs(UserJson(user))
    when(userDao.findById(user.id)).thenReturn(Some(user))
    when(commentActivity.addCommentToCommit(any[NewEntireCommitComment])).thenReturn(createdComment)

    // when
    post(s"/$commitId/comments", body, Map("Content-Type" -> "application/json")) {
      // then
      status should be(200)
      asJson(AddCommentResponse(SingleCommentView(createdComment.id.toString, user.name, createdComment.message, createdComment.postingTime.toDate)))
    }
  }

  def currentUser(id: ObjectId) = {
    User(id, Authentication.basic("user", "password"), "John Doe", "john@doe.com", "abcde", "avatarUrl")
  }

 class TestableCommentsEndpoint(val authenticator: Authenticator, fakeScentry: Scentry[UserJson], val commentActivity: AddCommentActivity, val userDao: UserDAO, val commentListFinder: CommentFinder) extends CommentsEndpoint {

    override def scentry(implicit request: javax.servlet.http.HttpServletRequest) = fakeScentry

    protected implicit def swagger: SwaggerEngine[_] = new CodebragSwagger
    protected def applicationDescription: String = ""

  }

}