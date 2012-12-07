package one.xmpp.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.util.Arrays;

import org.junit.Test;

import junit.framework.Assert;

public class CharSequenceUtilsTest {

    @Test
    public void testUnescapeXml() {
        Assert.assertEquals("test1<&>test2", CharSequenceUtils.unescapeXml("test1&lt;&amp;&gt;test2").toString());
        Assert.assertEquals("test1\r\ntest2", CharSequenceUtils.unescapeXml("test1&#13;&#10;test2").toString());
        Assert.assertEquals("test1\r\ntest2", CharSequenceUtils.unescapeXml("test1&#x0d;&#x0a;test2").toString());
        Assert.assertEquals("test1&'\"test2", CharSequenceUtils.unescapeXml("test1&amp;&apos;&quot;test2").toString());
    }

    @Test
    public void decodeUtf8() throws Exception {

        try {
            CharSequenceUtils.decodeFromUtf8(ByteBuffer.wrap(new byte[] { -30, 4, 113, 32, 116, 121, 112, 101, 61, 34,
                    103, 101, 116, 34, 32, 116, 111, 61, 34, 52, 52, 51, 49, 50, 55, 50, 54, 57, 50, 48, 52, 64, 111,
                    100, 110, 111, 107, 108, 97, 115, 115, 110, 105, 107, 105, 46, 114, 117, 34, 32, 105, 100, 61, 34,
                    49, 48, 54, 56, 49, 48, 54, 53, 55, 49, 34, 32, 62, 106, 79, 71, 78, 106, 77, 87, 85, 53, 77, 106,
                    99, 50, 90, 109, 77, 51, 77, 87, 77, 120, 89, 109, 78, 109, 89, 121, 90, 48, 98, 50, 116, 108, 98,
                    106, 48, 48, 78, 68, 77, 120, 77, 106, 99, 121, 78, 106, 107, 121, 77, 68, 82, 102, 78, 68, 107,
                    121, 89, 109, 86, 105, 90, 71, 90, 109, 89, 50, 81, 121, 79, 71, 89, 121, 90, 84, 90, 105, 78, 122,
                    107, 53, 78, 68, 73, 53, 77, 106, 74, 109, 78, 87, 89, 122, 90, 106, 89, 61 }));
            Assert.fail();
        } catch (MalformedInputException exc) {
            // expected
        }
    }

    @Test
    public void encodeUtf8() throws Exception {
        CharSequenceUtils.encodeToUtf8("(ړײ)Екатерина снова Агапова");

        // http://www.odnoklassniki.ru/profile/446348901528
        CharSequenceUtils.encodeToUtf8("ܢܜܔܔÐմмÃ ЧαВчиαНидŻеܢܜܔܔ");
        CharSequenceUtils.encodeToUtf8("GUF 乙ʍ ♫ ❷❷❽♫ ґყφ");
    }

    @Test
    public void testCharsetDecoders() throws Exception {

        // make sure single decoder can decoder less than full UTF-8 codepoint

        CharsetDecoder characterDecoder = Charset.forName("UTF-8").newDecoder();

        byte[] b2 = "\u00a2".getBytes();
        Assert.assertTrue(Arrays.equals(new byte[] { (byte) 0x0c2, (byte) 0x0a2 }, b2));

        // decode whole codepoint at once
        CharBuffer charBuffer = characterDecoder.decode(ByteBuffer.wrap(new byte[] { b2[0], b2[1] }));
        Assert.assertEquals("\u00a2", charBuffer.toString());

        // decode whole codepoint by part
        characterDecoder.reset();
        CharBuffer resultBuffer = CharBuffer.allocate(100);
        CoderResult coderResult;

        ByteBuffer source = ByteBuffer.allocate(100);
        source.limit(1);
        source.put(0, b2[0]);

        coderResult = characterDecoder.decode(source, resultBuffer, false);
        Assert.assertEquals(CoderResult.UNDERFLOW, coderResult);
        Assert.assertEquals(0, source.position());

        source.limit(2);
        source.put(1, b2[1]);

        coderResult = characterDecoder.decode(source, resultBuffer, true);
        Assert.assertEquals(2, source.position());
        Assert.assertEquals(1, resultBuffer.position());
        Assert.assertEquals(false, coderResult.isError());

        resultBuffer.flip();
        Assert.assertEquals("\u00a2", resultBuffer.toString());
    }

}
