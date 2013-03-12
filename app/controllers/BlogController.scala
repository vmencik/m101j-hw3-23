package controllers

import play.api._
import play.api.mvc._
import models._

import com.mongodb.casbah.Imports._

import play.api.data._
import play.api.data.Forms._

import org.apache.commons.lang3.StringEscapeUtils._

trait BlogControllerApi extends Controller {
  self: UserDAOComponent with SessionDAOComponent with BlogPostDAOComponent =>

  // this is the blog home page 
  def homePage = Action { implicit request =>
    val posts = blogPostDAO.findByDateDescending(10)

    Ok(views.html.blog(currentUser, posts))
  }

  // used to display actual blog post detail page
  def postDetail(permalink: String) = Action { implicit request =>
    println("/post: get " + permalink)

    blogPostDAO.findByPermalink(permalink) match {
      case Some(post) => Ok(views.html.postDetail(currentUser, post))
      case _ => Redirect(routes.BlogController.postNotFound)
    }
  }

  def showSignup = Action {
    Ok(views.html.signup())
  }

  def processSignup = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      withErrors => BadRequest(views.html.signup()),
      signup => {
        val validated = signup.validated
        if (validated.errors.isEmpty) {
          // good user
          if (!userDAO.addUser(signup.username, signup.password, signup.email))
            // duplicate user
            Ok(views.html.signup(validated.copy(errors = SignupErrors(username = "Username already in use, Please choose another"))))
          else {
            // good user, let's start a session
            val sessionId = sessionDAO.startSession(signup.username)
            Redirect(routes.BlogController.welcome).withCookies(
              Cookie("session", sessionId))
          }
        } else Ok(views.html.signup(validated)) // invalid signup
      })
  }

  def showNewPostForm = Action { implicit request =>
    currentUser match {
      case Some(username) => Ok(views.html.newPost(username))
      case _ => Redirect(routes.BlogController.showLogin)
    }
  }

  def processNewPost = Action { implicit request =>
    currentUser match {
      case Some(username) =>
        newPostForm.bindFromRequest.fold(
          withErrors => BadRequest(views.html.newPost(username)),
          newPost => {
            val validated = newPost.validated
            if (validated.errors.isEmpty) {
              val tags = Post.extractTags(newPost.tags)
              // substitute some <p> for the paragraph breaks
              val body = newPost.body.replaceAll("\\r?\\n", "<p>")

              val permalink = blogPostDAO.addPost(newPost.subject, body, tags, username)

              // now redirect to the blog permalink 
              Redirect(routes.BlogController.postDetail(permalink))
            } else Ok(views.html.newPost(username, validated))
          }
        )
      case _ => Redirect(routes.BlogController.showLogin) // only logged in users can post to blog 
    }
  }

  def welcome = Action { implicit request =>
    currentUser match {
      case Some(username) => Ok(views.html.welcome(username))
      case _ => Redirect(routes.BlogController.showSignup)
    }
  }

  def processNewComment = Action { implicit request =>
    commentForm.bindFromRequest.fold(
      withErrors => BadRequest(views.html.postNotFound()),
      comment => {
        blogPostDAO.findByPermalink(comment.permalink) match {
          case Some(post) =>
            // check that comment is good 
            val validated = comment.validated
            if (validated.errors.isEmpty) {
              blogPostDAO.addPostComment(comment.name, comment.email, comment.body, comment.permalink)
              Redirect(routes.BlogController.postDetail(comment.permalink))
            } else Ok(views.html.postDetail(currentUser, post, validated))
          case _ => Ok(views.html.postNotFound()) // bounce this back to the user for correction 
        }
      }
    )
  }

  def showLogin = Action {
    Ok(views.html.login())
  }

  def processLogin = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      withErrors => BadRequest(views.html.login()),
      {
        case (username, password) => userDAO.validateLogin(username, password) match {
          case Some(user) =>
            // valid user, let's log them in
            val sessionId = sessionDAO.startSession(user.get("_id").toString)
            Redirect(routes.BlogController.welcome).withCookies(
              Cookie("session", sessionId))
          case _ => Ok(views.html.login(username, "Invalid Login"))
        }
      })
  }

  def logout = Action { implicit request =>
    request.cookies.get("session") match {
      case Some(sessionCookie) =>
        sessionDAO.endSession(sessionCookie.value)
        Redirect(routes.BlogController.showLogin).discardingCookies(DiscardingCookie("session"))
      case _ => Redirect(routes.BlogController.showLogin)
    }
  }

  def postNotFound = Action {
    Ok(views.html.postNotFound())
  }

  val signupForm = Form(
    mapping(
      "email" -> text,
      "username" -> text,
      "password" -> text,
      "verify" -> text) {
        case (email, username, password, verify) =>
          Signup(escapeHtml4(email), escapeHtml4(username), password, verify)
      } {
        s => Some((s.email, s.username, s.password, s.verify))
      })

  val newPostForm = Form(
    mapping(
      "subject" -> text,
      "body" -> text,
      "tags" -> text
    ) {
        case (subject, body, tags) => Post(escapeHtml4(subject), escapeHtml4(body), escapeHtml4(tags))
      } {
        p => Some((p.subject, p.body, p.tags))
      }
  )

  val commentForm = Form(
    mapping(
      "permalink" -> text,
      "commentName" -> text,
      "commentEmail" -> optional(text),
      "commentBody" -> text
    ) {
        case (permalink, name, email, body) =>
          Comment(permalink, escapeHtml4(name), email.map(escapeHtml4), escapeHtml4(body))
      } {
        c => Some((c.permalink, c.name, c.email, c.body))
      }
  )

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text))

  private def currentUser(implicit request: RequestHeader): Option[String] =
    for {
      sessionCookie <- request.cookies.get("session")
      username <- sessionDAO.findUserNameBySessionId(sessionCookie.value)
    } yield username

}

object BlogController extends BlogControllerApi
  with MongoUserDAOComponent
  with MongoSessionDAOComponent
  with MongoBlogPostDAOComponent

case class Signup(email: String = "", username: String = "", password: String = "", verify: String = "", errors: SignupErrors = SignupErrors()) {

  def validated: Signup = {
    import Signup._
    if ((UserRe findFirstIn username).isEmpty) copy(errors = SignupErrors(username = "invalid username. try just letters and numbers"))
    else if ((PassRe findFirstIn password).isEmpty) copy(errors = SignupErrors(password = "invalid password."))
    else if (password != verify) copy(errors = SignupErrors(verify = "password must match"))
    else if (email != "" && (EmailRe findFirstIn email).isEmpty) copy(errors = SignupErrors(email = "Invalid Email Address"))
    else this
  }

}

object Signup {
  val UserRe = "^[a-zA-Z0-9_-]{3,20}$".r
  val PassRe = "^.{3,20}$".r
  val EmailRe = "^[\\S]+@[\\S]+\\.[\\S]+$".r
}

case class SignupErrors(password: String = "", username: String = "", email: String = "", verify: String = "") {

  def isEmpty = (password + username + email + verify).isEmpty

}

case class Comment(permalink: String = "", name: String = "", email: Option[String] = None, body: String = "", errors: String = "") {

  def validated: Comment =
    if (name == "" || body == "") copy(errors = "Post must contain your name and an actual comment")
    else this

}

case class Post(subject: String = "", body: String = "", tags: String = "", errors: String = "") {

  def validated: Post =
    if (subject == "" || body == "") copy(errors = "post must contain a title and blog entry.")
    else this

}

object Post {

  private val WhitespaceRe = "\\s".r
  private val CommaRe = ",".r

  // tags the tags string and put it into an array
  def extractTags(tags: String): Seq[String] = {
    val tagArray = CommaRe.split(WhitespaceRe.replaceAllIn(tags, ""))
    // let's clean it up, removing the empty string and removing dups
    tagArray.distinct.filterNot(_ == "").toSeq
  }

}