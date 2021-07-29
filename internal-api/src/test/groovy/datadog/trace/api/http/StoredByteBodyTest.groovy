package datadog.trace.api.http

import spock.lang.Specification

import java.nio.charset.Charset

class StoredByteBodyTest extends Specification {
  StoredBodyListener listener = Mock()
  StoredByteBody storedByteBody = new StoredByteBody(listener)

  void 'basic test with no buffer extension'() {
    when:
    storedByteBody.appendData((int) 'a') // not "as int"

    then:
    listener.onBodyStart(storedByteBody)

    when:
    storedByteBody.appendData([(int)'a']* 127 as byte[], 0, 127)
    storedByteBody.maybeNotify()

    then:
    listener.onBodyEnd(storedByteBody)
    storedByteBody.get() as String == 'a' * 128
  }

  void 'stores at most 1 MB'() {
    when:
    storedByteBody.appendData((int) 'a')

    then:
    listener.onBodyStart(storedByteBody)

    when:
    // last byte ignored
    storedByteBody.appendData([(int)'a']* 1024 * 1024 as byte[], 0, 1024 * 1024)
    // ignored
    storedByteBody.appendData(0)
    // ignored
    storedByteBody.appendData([0] as byte[], 0, 1)

    then:
    storedByteBody.get() as String == 'a' * (1024 * 1024)
  }

  void 'ignores invalid integers given to appendData'() {
    when:
    storedByteBody.appendData(-1)
    storedByteBody.appendData(256)

    then:
    storedByteBody.get() as String == ''
  }

  void 'well formed utf8 data'() {
    when:
    def data = '\u00E1\u0800\uD800\uDC00'.getBytes(Charset.forName('UTF-8'))
    storedByteBody.appendData(data, 0, data.length)

    then:
    storedByteBody.get() as String == '\u00E1\u0800\uD800\uDC00'
  }

  void 'non UTF8 data with specified encoding'() {
    when:
    def data = 'á'.getBytes(Charset.forName('UTF-8'))
    storedByteBody.setCharset(Charset.forName('ISO-8859-1'))
    storedByteBody.appendData(data, 0, data.length)

    then:
    storedByteBody.get() as String == '\u00C3\u00A1'
  }

  void 'fallback to latin1 on first byte'() {
    when:
    storedByteBody.appendData([0xFF] as byte[], 0, 1)

    then:
    storedByteBody.get() as String == '\u00FF'
  }

  void 'fallback to latin1 on second byte'() {
    when:
    storedByteBody.appendData([0xC3, 0xC3] as byte[], 0, 2)
    then:
    storedByteBody.get() as String == '\u00C3\u00C3'
  }

  void 'fallback to latin1 on third byte'() {
    when:
    storedByteBody.appendData([0xE0, 0x80, 0x7F] as byte[], 0, 3)
    then:
    storedByteBody.get() as String == '\u00E0\u0080\u007F'
  }

  void 'fallback to latin1 on fourth byte'() {
    when:
    storedByteBody.appendData([0xF0, 0x80, 0x80, 0x7F] as byte[], 0, 4)
    then:
    storedByteBody.get() as String == '\u00F0\u0080\u0080\u007F'
  }

  void 'fallback to latin on unfinished 2 byte sequences'() {
    when:
    storedByteBody.appendData(0xC3)
    storedByteBody.maybeNotify()

    then:
    storedByteBody.get() as String == '\u00C3'
  }

  void 'fallback to latin on unfinished 3 byte sequences'() {
    when:
    storedByteBody.appendData([0xE0, 0xA0] as byte[], 0, 2)
    storedByteBody.maybeNotify()

    then:
    storedByteBody.get() as String == '\u00E0\u00A0'
  }

  void 'fallback to latin on unfinished 4 byte sequences'() {
    when:
    storedByteBody.appendData([0xF0, 0x90, 0x80] as byte[], 0, 3)
    storedByteBody.maybeNotify()

    then:
    storedByteBody.get() as String == '\u00F0\u0090\u0080'
  }

  void 'utf-8 data can be reencoded as latin1'() {
    def bytes = ("á" * 16).getBytes(Charset.forName('UTF-8'))

    when:
    storedByteBody.appendData(bytes, 0, bytes.length)

    then:
    storedByteBody.get() as String == 'á' * 16

    when:
    2.times { storedByteBody.appendData(0x80) }

    then:
    storedByteBody.get() as String == '\u00C3\u00A1' * 16 + "\u0080\u0080"
  }
}
