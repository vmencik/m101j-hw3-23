package models
import com.mongodb.DBObject

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject

import play.api.Play.current
import se.radley.plugin.salat._

trait BlogPostDAOComponent {

  def blogPostDAO: BlogPostDAO

  trait BlogPostDAO {

    def findByPermalink(permalink: String): Option[DBObject]

    def findByDateDescending(limit: Int): Seq[DBObject]

    def addPost(title: String, body: String, tags: Seq[String], username: String): String

    def addPostComment(name: String, email: Option[String], body: String, permalink: String): Unit

  }

}

trait MongoBlogPostDAOComponent extends BlogPostDAOComponent {

  override val blogPostDAO = new MongoBlogPostDAO(mongoCollection("posts"))

  class MongoBlogPostDAO(posts: MongoCollection) extends BlogPostDAO {

    // Return a single post corresponding to a permalink 
    override def findByPermalink(permalink: String): Option[DBObject] = {
      // XXX HW 3.2,  Work Here 
      val post: Option[DBObject] = ???

      post
    }

    // Return a list of posts in descending order. Limit determines
    // how many posts are returned. 
    override def findByDateDescending(limit: Int): Seq[DBObject] = {
      // XXX HW 3.2,  Work Here
      // Return a list of DBObjects, each one a post from the posts collection
      val result: Seq[DBObject] = ???

      result
    }

    override def addPost(title: String, body: String, tags: Seq[String], username: String): String = {
      println("inserting blog entry " + title + " " + body)

      val permalink = title
        .replaceAll("\\s", "_") // whitespace becomes _
        .replaceAll("\\W", "") // get rid of non alphanumeric 
        .toLowerCase()

      val post = MongoDBObject.newBuilder
      // XXX HW 3.2, Work Here
      // Remember that a valid post has the following keys:
      // author, body, permalink, tags, comments, date
      //
      // A few hints:
      // - Don't forget to create an empty list of comments
      // - for the value of the date key, today's datetime is fine.
      // - tags are already in list form that implements suitable interface.
      // - we created the permalink for you above.

      // Build the post object and insert it 

      permalink
    }

    // White space to protect the innocent 

    // Append a comment to a blog post 
    override def addPostComment(name: String, email: Option[String], body: String, permalink: String): Unit = {

      // XXX HW 3.3, Work Here
      // Hints:
      // - email is optional. Check for that.
      // - best solution uses an update command to the database and a suitable
      //   operator to append the comment on to any existing list of comments 

    }

  }
}

