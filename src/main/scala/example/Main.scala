package example

import java.io.InputStream
import java.nio.channels.Channels
import java.nio.{ByteBuffer, ByteOrder}

import jnr.unixsocket.{UnixSocketAddress, UnixSocketChannel}
import models.Person

import scala.util.{Failure, Success, Try}

object Main extends App {
  val socketPath = args(0)
  val name = args(1)
  val age = args(2).toInt
  val person = models.Person(name, age)

  val path = new java.io.File(socketPath)
  if(!path.exists) {
    System.err.println("Socket does not exist.")
    System.exit(-1);
  } else {
    val socketAddr = new UnixSocketAddress(path)
    for {
      channel <- openChannel(socketAddr)
      _ = println(s"Connected to socket ${channel.getRemoteSocketAddress}.")
      _ <- writePerson(channel, person)
      receivedPerson <- read(channel)
      _ = println(s"Person received: $receivedPerson")
      _ = println(s"Person received is the same as sent: ${person == receivedPerson}")
    } yield ()
  }

  def openChannel(socketAddr: UnixSocketAddress): Try[UnixSocketChannel] =
    Try(UnixSocketChannel.open(socketAddr))

  private def readSize(is: InputStream): Try[Int] = {
    val stream = Stream.continually(is.read())
    try {
      val byteBuff = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
      val _ = stream.take(4).foreach(byteBuff.putInt(_))
      Success(byteBuff.getInt(0))
    } catch {
      case ex: Exception => Failure(ex)
    }
  }

  private def readPerson(is: InputStream, size: Int): Try[Person] = {
    val stream = Stream.continually(is.read())
    Try(Person.parseFrom(stream.take(size).map(el => el.asInstanceOf[Byte]).toArray))
  }

  def read(channel: UnixSocketChannel): Try[Person] = {
    val is = Channels.newInputStream(channel)
    for {
      size <- readSize(is)
      person <- readPerson(is, size)
    } yield person
  }

  def writePerson(channel: UnixSocketChannel, person: Person): Try[Unit] = for {
    os <- Try(Channels.newOutputStream(channel))
    size = person.serializedSize
    bb <- Try(ByteBuffer.allocate(4 + size)).map(_.order(ByteOrder.LITTLE_ENDIAN))
    _ <- Try(bb.putInt(size))
    _ <- Try(bb.put(person.toByteArray))
    _ <- Try(os.write(bb.array()))
  } yield ()

}