package models
import java.security.MessageDigest
import java.security.SecureRandom

import com.mongodb.DBObject
import com.mongodb.MongoException
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject

import play.api.Play.current
import se.radley.plugin.salat._
import sun.misc.BASE64Encoder

trait UserDAOComponent {

  def userDAO: UserDAO

  trait UserDAO {

    def addUser(username: String, password: String, email: String): Boolean

    def validateLogin(username: String, password: String): Option[DBObject]

  }

}

trait MongoUserDAOComponent extends UserDAOComponent {

  override val userDAO = new MongoUserDAO(mongoCollection("users"))

  class MongoUserDAO(users: MongoCollection) extends UserDAO {

    private val random = new SecureRandom()

    override def addUser(username: String, password: String, email: String): Boolean = {
      val passwordHash = makePasswordHash(password, Integer.toString(random.nextInt()))

      val user = MongoDBObject.newBuilder
      user += "_id" -> username
      user += "password" -> passwordHash

      if (email != "") {
        // if there is an email address specified, add it to the document too.
        user += "email" -> email
      }

      try {
        // insert the document into the user collection here
        users += user.result
        true
      } catch {
        case e: MongoException.DuplicateKey =>
          false
      }
    }

    override def validateLogin(username: String, password: String): Option[DBObject] = {
      val maybeUser: Option[DBObject] = users.findOne(MongoDBObject("_id" -> username))

      maybeUser match {
        case Some(user) =>
          val hashedAndSalted = user.get("password").toString
          val salt = hashedAndSalted.split(",")(1)
          if (hashedAndSalted == makePasswordHash(password, salt)) maybeUser
          else None
        case _ => None
      }
    }

    private def makePasswordHash(password: String, salt: String) = {
      val saltedAndHashed = password + "," + salt
      val digest = MessageDigest.getInstance("MD5")
      digest.update(saltedAndHashed.getBytes())
      val encoder = new BASE64Encoder()
      val hashedBytes = (new String(digest.digest(), "UTF-8")).getBytes()
      encoder.encode(hashedBytes) + "," + salt
    }

  }

}