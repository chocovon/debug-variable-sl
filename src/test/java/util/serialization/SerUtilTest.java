package util.serialization;

import common.GenCodeRequest;
import common.Settings;
import org.junit.Assert;
import org.junit.Test;
import util.SerUtil;

public class SerUtilTest {
    @Test
    public void testParse() throws Exception {
        GenCodeRequest genCodeRequest = new GenCodeRequest();
        genCodeRequest.setSettings(new Settings());
        genCodeRequest.getSettings().setFormat("test@value");
        genCodeRequest.getSettings().setSkipNulls(false);
        genCodeRequest.getSettings().setPrettyFormat(true);
        genCodeRequest.setVariableName("hello\n\"world\"");

        String serString = SerUtil.writeValueAsString(genCodeRequest);
        GenCodeRequest unmarshal = SerUtil.parseObject(serString, GenCodeRequest.class);
        Assert.assertEquals("test@value", unmarshal.getSettings().getFormat());
        Assert.assertEquals("hello\n\"world\"", unmarshal.getVariableName());
        Assert.assertEquals(unmarshal.getSettings().isSkipNulls(), false);
        Assert.assertEquals(unmarshal.getSettings().isPrettyFormat(), true);
    }
}