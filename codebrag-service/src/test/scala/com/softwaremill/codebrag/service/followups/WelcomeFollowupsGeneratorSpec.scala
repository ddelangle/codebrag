package com.softwaremill.codebrag.service.followups

import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import com.softwaremill.codebrag.dao._
import com.softwaremill.codebrag.domain._
import org.mockito.Mockito._
import com.softwaremill.codebrag.domain.builder.{CommitInfoAssembler, UserAssembler}
import com.softwaremill.codebrag.dao.events.NewUserRegistered
import org.mockito.ArgumentCaptor
import com.softwaremill.codebrag.domain.Comment
import com.softwaremill.codebrag.domain.Followup
import scala.Some
import com.softwaremill.codebrag.domain.Like

class WelcomeFollowupsGeneratorSpec extends FlatSpec with MockitoSugar with ShouldMatchers with BeforeAndAfterEach {

  var internalUserDao: MongoInternalUserDAO = _
  var commentsDao: CommitCommentDAO = _
  var likesDao: LikeDAO = _
  var followupsDao: FollowupDAO = _
  var commitInfoDao: CommitInfoDAO = _

  var generator: WelcomeFollowupsGenerator = _
  
  val internalUser = InternalUser(InternalUser.WelcomeFollowupsAuthorName)
  val newUser = UserAssembler.randomUser.get
  val registeredUserData = NewUserRegistered(newUser)

  override def beforeEach() {
    internalUserDao = mock[MongoInternalUserDAO]
    commentsDao = mock[CommitCommentDAO]
    likesDao = mock[LikeDAO]
    followupsDao = mock[FollowupDAO]
    commitInfoDao = mock[CommitInfoDAO]
    generator = new WelcomeFollowupsGenerator(internalUserDao, commentsDao, likesDao, followupsDao, commitInfoDao)

    when(internalUserDao.findByName(InternalUser.WelcomeFollowupsAuthorName)).thenReturn(Some(internalUser))
  }
  
  it should "do nothing when user has no commits" in {
    // given
    when(commitInfoDao.findLastCommitsAuthoredByUser(registeredUserData, 2)).thenReturn(List.empty)
    
    // when
    generator.createWelcomeFollowupFor(registeredUserData)

    // then
    verifyZeroInteractions(commentsDao, likesDao, followupsDao)
  }

  it should "generate like and comment for single commit if user has only one commit" in {
    // given
    val oneCommitList = List(CommitInfoAssembler.randomCommit.get)
    when(commitInfoDao.findLastCommitsAuthoredByUser(registeredUserData, 2)).thenReturn(oneCommitList)

    // when
    generator.createWelcomeFollowupFor(registeredUserData)

    // then
    val like = verifyLikeCreatedFor(oneCommitList.head)
    val comment = verifyCommentCreatedFor(oneCommitList.head)
    verifyFollowupsCreatedFor(comment, like, newUser)
  }

  it should "generate like and comment for different commit accordingly" in {
    // given
    val twoCommitsList = List(CommitInfoAssembler.randomCommit.get, CommitInfoAssembler.randomCommit.get)
    when(commitInfoDao.findLastCommitsAuthoredByUser(registeredUserData, 2)).thenReturn(twoCommitsList)

    // when
    generator.createWelcomeFollowupFor(registeredUserData)

    // then
    val like = verifyLikeCreatedFor(twoCommitsList(0))
    val comment = verifyCommentCreatedFor(twoCommitsList(1))
    verifyFollowupsCreatedFor(comment, like, newUser)
  }




  def verifyCommentCreatedFor(commit: CommitInfo): Comment = {
    val commentArgument = ArgumentCaptor.forClass(classOf[Comment])
    verify(commentsDao).save(commentArgument.capture())
    commentArgument.getValue.authorId should be(internalUser.id)
    commentArgument.getValue.commitId should be(commit.id)
    commentArgument.getValue
  }

  def verifyLikeCreatedFor(commit: CommitInfo): Like = {
    val likeArgument = ArgumentCaptor.forClass(classOf[Like])
    verify(likesDao).save(likeArgument.capture())
    likeArgument.getValue.authorId should be(internalUser.id)
    likeArgument.getValue.commitId should be(commit.id)
    likeArgument.getValue
  }

  def verifyFollowupsCreatedFor(comment: Comment, like: Like, user: User) {
    val followupArguments = ArgumentCaptor.forClass(classOf[Followup])
    verify(followupsDao, times(2)).createOrUpdateExisting(followupArguments.capture())

    val likeFollowup = followupArguments.getAllValues().get(0)
    likeFollowup.receivingUserId should be(user.id)
    likeFollowup.reaction.id should be(like.id)

    val commentFollowup = followupArguments.getAllValues().get(1)
    commentFollowup.receivingUserId should be(user.id)
    commentFollowup.reaction.id should be(comment.id)
  }
}