package example

import java.nio.channels.Channels

import com.google.protobuf.CodedInputStream
import jnr.unixsocket.{UnixSocketAddress, UnixSocketChannel}

object Hello extends Greeting with App {
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
    val channel = UnixSocketChannel.open(socketAddr)
    System.out.println(s"Connected to socket ${channel.getRemoteSocketAddress}.")
    val os = Channels.newOutputStream(channel)
    person.writeTo(os)
    System.out.println("Message sent.")
    os.flush()

    val ir = Channels.newInputStream(channel)
    val is = CodedInputStream.newInstance(ir)
    val receivedPerson = person.mergeFrom(is)
    System.out.println(s"Person received is the same as sent: ${person == receivedPerson}")
  }

}

trait Greeting {
  lazy val greeting: String = "hello"
}
