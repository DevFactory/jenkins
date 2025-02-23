package hudson.model;

import hudson.util.Secret;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ParameterDefinitionTest {

    @Test
    public void compareStringParameterDefinition() throws Exception {
        StringParameterDefinition spd = new StringParameterDefinition("spd", "default");
        StringParameterDefinition spdSame = new StringParameterDefinition("spd", "default");
        StringParameterDefinition spdOther = new StringParameterDefinition("spdOther", "default");

        StringParameterDefinition spd1 = new StringParameterDefinition("spd", "default", "desc");
        StringParameterDefinition spdSame1 = new StringParameterDefinition("spd", "default", "desc");
        StringParameterDefinition spdOther1 = new StringParameterDefinition("spd", "otherDefault", "desc");
        StringParameterDefinition spdOtherDesc = new StringParameterDefinition("spd", "default", "spdOtherDesc");

        assertEquals(spd, spdSame);
        assertEquals(spd1, spdSame1);

        assertNotEquals(spd, spdOther);
        assertNotEquals(spd1, spdOther1);
        assertNotEquals(spd, spd1);
        assertNotEquals(spd1, spdOtherDesc);
        assertNotEquals(spdOther, spdOther1);

        TextParameterDefinition tpd = new TextParameterDefinition("tpd", "default", "desc");
        TextParameterDefinition tpdSame = new TextParameterDefinition("tpd", "default", "desc");
        TextParameterDefinition tpdOther = new TextParameterDefinition("spd", "default", "desc");

        assertEquals(tpd, tpdSame);
        assertNotEquals(tpd, tpdOther);
        assertNotEquals(tpdOther, spd1);

        ParameterDefinition pd = new StringParameterDefinition("spd", "default", "desc") {
            public void newMethod() {
            }
        };
        ExtendStringParameterDefinition epd = new ExtendStringParameterDefinition("spd", "default", "desc");
        assertNotEquals(pd, spd1);
        assertNotEquals(epd, spd1);
    }

    @Test
    public void compareBooleanParameterDefinition() {
        BooleanParameterDefinition bpd = new BooleanParameterDefinition("bpd", false, "desc");
        BooleanParameterDefinition bpdSame = new BooleanParameterDefinition("bpd", false, "desc");
        BooleanParameterDefinition bpdOther = new BooleanParameterDefinition("bpd", true, "desc");
        StringParameterDefinition spd = new StringParameterDefinition("spd", "false", "desc");

        BooleanParameterDefinition pd = new BooleanParameterDefinition("bpd", false, "desc") {
            public void newMethod() {
            }
        };
        extractedMethod30868(bpd, bpdSame, bpdOther, spd, pd); // CAP AL
    }

    @Test
    public void compareChoiceParameterDefinition() {
        ChoiceParameterDefinition cpd = new ChoiceParameterDefinition("bpd", new String[]{"1", "2", "3"}, "desc");
        ChoiceParameterDefinition cpdSame = new ChoiceParameterDefinition("bpd", new String[]{"1", "2", "3"}, "desc");
        ChoiceParameterDefinition cpdOther = new ChoiceParameterDefinition("bpd", new String[]{"1", "3", "2"}, "desc");
        ExtendStringParameterDefinition epd = new ExtendStringParameterDefinition("bpd", "1,2,3", "desc");
        ChoiceParameterDefinition pd = new ChoiceParameterDefinition("bpd", new String[]{"1", "2", "3"}, "desc") {
            public void newMethod() {
            }
        };

        extractedMethod30868(cpd, cpdSame, cpdOther, pd, epd); // CAP AL
    }

    @Test
    public void comparePasswordParameterDefinition() {
        PasswordParameterDefinition ppd = new PasswordParameterDefinition("ppd", Secret.fromString("password"), "desc");
        PasswordParameterDefinition ppdSame = new PasswordParameterDefinition("ppd", Secret.fromString("password"), "desc");
        PasswordParameterDefinition ppdOther = new PasswordParameterDefinition("ppd", Secret.fromString("password1"), "desc");
        ExtendStringParameterDefinition epd = new ExtendStringParameterDefinition("ppd", "password", "desc");
        PasswordParameterDefinition pd = new PasswordParameterDefinition("ppd", Secret.fromString("password"), "desc") {
            public void newMethod() {
            }
        };

        extractedMethod30868(ppd, ppdSame, ppdOther, pd, epd); // CAP AL
    }

    @Test
    public void compareFileParameterDefinition() {
        FileParameterDefinition fpd = new FileParameterDefinition("fpd", "desc");
        FileParameterDefinition fpdSame = new FileParameterDefinition("fpd", "desc");
        FileParameterDefinition fpdOther = new FileParameterDefinition("fpdOther", "desc");
        ExtendStringParameterDefinition epd = new ExtendStringParameterDefinition("fpd", "", "desc");
        FileParameterDefinition pd = new FileParameterDefinition("fpd", "desc") {
            public void newMethod() {
            }
        };

        extractedMethod30868(fpd, fpdSame, fpdOther, epd, pd); // CAP AL
    }

    @Test
    public void compareRunParameterDefinition() {
        RunParameterDefinition rpd = new RunParameterDefinition("rpd", "project", "desc", RunParameterDefinition.RunParameterFilter.ALL);
        RunParameterDefinition rpdSame = new RunParameterDefinition("rpd", "project", "desc", RunParameterDefinition.RunParameterFilter.ALL);
        RunParameterDefinition rpdOther = new RunParameterDefinition("rpd", "project1", "desc", RunParameterDefinition.RunParameterFilter.STABLE);

        ExtendStringParameterDefinition epd = new ExtendStringParameterDefinition("rpd", "project", "desc");
        RunParameterDefinition pd = new RunParameterDefinition("rpd", "project", "desc", RunParameterDefinition.RunParameterFilter.ALL) {
            public void newMethod() {
            }
        };

        extractedMethod30868(rpd, rpdSame, rpdOther, epd, pd); // CAP AL
    }
 // CAP AL
    private void extractedMethod30868(final ParameterDefinition rpd, final ParameterDefinition rpdSame, final ParameterDefinition rpdOther, final SimpleParameterDefinition epd, final ParameterDefinition pd) { // CAP AL
        assertEquals(rpd, rpdSame); // CAP AL
        assertNotEquals(rpd, rpdOther); // CAP AL
        assertNotEquals(rpd, epd); // CAP AL
        assertNotEquals(rpd, pd); // CAP AL
    } // CAP AL

    @Test
    public void compareChildsWithoutEqualsAndHashCode() {
        ExtendStringParameterDefinition epd = new ExtendStringParameterDefinition("epd", "default", "desc");
        ExtendStringParameterDefinition epdSame = new ExtendStringParameterDefinition("epd", "default", "desc");
        ExtendStringParameterDefinition epdOther = new ExtendStringParameterDefinition("epd", "default1", "desc");

        assertEquals(epd, epdSame);
        assertNotEquals(epd, epdOther);

        ParameterDefinition pd = new StringParameterDefinition("spd", "default", "desc") {
            public void newMethod() {
            }
        };

        ParameterDefinition pdOther = new StringParameterDefinition("spd", "default", "desc") {
            public void newMethod() {
            }
        };

        assertNotEquals(pd, pdOther);
    }

    static class ExtendStringParameterDefinition extends StringParameterDefinition {
        public ExtendStringParameterDefinition(String name, String defaultValue, String description, boolean trim) {
            super(name, defaultValue, description, trim);
        }

        public ExtendStringParameterDefinition(String name, String defaultValue, String description) {
            super(name, defaultValue, description);
        }

        public ExtendStringParameterDefinition(String name, String defaultValue) {
            super(name, defaultValue);
        }
    }
}
