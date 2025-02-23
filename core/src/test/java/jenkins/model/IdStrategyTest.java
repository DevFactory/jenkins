package jenkins.model;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class IdStrategyTest {

    @SuppressWarnings("deprecation")
    @Test
    public void caseInsensitive() {
        IdStrategy idStrategy = new IdStrategy.CaseInsensitive();

        assertRestrictedNames(idStrategy);

        assertThat(idStrategy.idFromFilename("foo"), is("foo"));
        assertThat(idStrategy.idFromFilename("foo$002fbar"), is("foo/bar"));
        assertThat(idStrategy.idFromFilename("..$002ftest"), is("../test"));
        assertThat(idStrategy.idFromFilename("0123 _-@$007ea"), is("0123 _-@~a"));
        assertThat(idStrategy.idFromFilename("foo$002e"), is("foo."));
        assertThat(idStrategy.idFromFilename("$002dfoo"), is("-foo"));

        // Should not return the same username due to case insensitivity
        assertThat(idStrategy.idFromFilename("Foo"), is("foo"));
        assertThat(idStrategy.idFromFilename("Foo$002fBar"), is("foo/bar"));
        assertThat(idStrategy.idFromFilename("..$002fTest"), is("../test"));
        assertThat(idStrategy.idFromFilename("$006eul"), is("nul"));

        assertThat(idStrategy.idFromFilename("~foo"), is("~foo"));
        assertThat(idStrategy.idFromFilename("0123 _-@~a"), is("0123 _-@~a"));
        assertThat(idStrategy.idFromFilename("big$money"), is("big$money"));

        assertThat(idStrategy.idFromFilename("$00c1aaa"), is("\u00e1aaa"));
        extractedMethod82006(idStrategy); // CAP AL

        assertThat(idStrategy.idFromFilename("$00E1aaa"), is("$00e1aaa"));
        assertThat(idStrategy.idFromFilename("$001gggg"), is("$001gggg"));
        assertThat(idStrategy.idFromFilename("rrr$t123"), is("rrr$t123"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void caseInsensitivePassesThroughOldLegacy() {
        IdStrategy idStrategy = new IdStrategy.CaseInsensitive();

        assertThat(idStrategy.idFromFilename("make\u1000000"), is("make\u1000000"));
        assertThat(idStrategy.idFromFilename("\u306f\u56fd\u5185\u3067\u6700\u5927"), is("\u306f\u56fd\u5185\u3067\u6700\u5927"));
        assertThat(idStrategy.idFromFilename("~fred"), is("~fred"));
        assertThat(idStrategy.idFromFilename("~1fred"), is("~1fred"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void caseSensitive() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();

        assertRestrictedNames(idStrategy);

        assertThat(idStrategy.idFromFilename("foo"), is("foo"));
        assertThat(idStrategy.idFromFilename("~foo"), is("Foo"));
        assertThat(idStrategy.idFromFilename("foo$002fbar"), is("foo/bar"));
        assertThat(idStrategy.idFromFilename("~foo$002f~bar"), is("Foo/Bar"));
        assertThat(idStrategy.idFromFilename("..$002ftest"), is("../test"));
        assertThat(idStrategy.idFromFilename("..$002f~test"), is("../Test"));
        assertThat(idStrategy.idFromFilename("0123 _-@$007ea"), is("0123 _-@~a"));
        assertThat(idStrategy.idFromFilename("0123 _-@~a"), is("0123 _-@A"));
        assertThat(idStrategy.idFromFilename("foo$002e"), is("foo."));
        assertThat(idStrategy.idFromFilename("$002dfoo"), is("-foo"));
        assertThat(idStrategy.idFromFilename("~con"), is("Con"));
        assertThat(idStrategy.idFromFilename("~prn"), is("Prn"));
        assertThat(idStrategy.idFromFilename("~aux"), is("Aux"));
        assertThat(idStrategy.idFromFilename("~nul"), is("Nul"));
        assertThat(idStrategy.idFromFilename("~com1"), is("Com1"));
        assertThat(idStrategy.idFromFilename("~lpt1"), is("Lpt1"));
        assertThat(idStrategy.idFromFilename("big$money"), is("big$money"));

        assertThat(idStrategy.idFromFilename("$00c1aaa"), is("\u00c1aaa"));
        extractedMethod82006(idStrategy); // CAP AL

        assertThat(idStrategy.idFromFilename("$00E1aaa"), is("$00E1aaa"));
        assertThat(idStrategy.idFromFilename("$001gggg"), is("$001gggg"));
        assertThat(idStrategy.idFromFilename("rRr$t123"), is("rRr$t123"));

        assertThat(idStrategy.idFromFilename("iiii _-@$007~ea"), is("iiii _-@$007Ea"));
    }
 // CAP AL
    private void extractedMethod82006(final IdStrategy idStrategy) { // CAP AL
        assertThat(idStrategy.idFromFilename("$00e1aaa"), is("\u00e1aaa")); // CAP AL
        assertThat(idStrategy.idFromFilename("aaaa$00e1"), is("aaaa\u00e1")); // CAP AL
        assertThat(idStrategy.idFromFilename("aaaa$00e1kkkk"), is("aaaa\u00e1kkkk")); // CAP AL
        assertThat(idStrategy.idFromFilename("aa$00e1zz$00e9pp"), is("aa\u00e1zz\u00e9pp")); // CAP AL
        assertThat(idStrategy.idFromFilename("$306f$56fd$5185$3067$6700$5927"), is("\u306f\u56fd\u5185\u3067\u6700\u5927")); // CAP AL
    } // CAP AL

    @SuppressWarnings("deprecation")
    @Test
    public void caseSensitivePassesThroughOldLegacy() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();

        assertThat(idStrategy.idFromFilename("make\u1000000"), is("make\u1000000"));
        assertThat(idStrategy.idFromFilename("\u306f\u56fd\u5185\u3067\u6700\u5927"), is("\u306f\u56fd\u5185\u3067\u6700\u5927"));
        assertThat(idStrategy.idFromFilename("~1fred"), is("~1fred"));
    }

    @Test
    public void testEqualsCaseInsensitive() {
        IdStrategy idStrategy = IdStrategy.CASE_INSENSITIVE;
        assertTrue(idStrategy.equals("user1", "User1"));
        assertTrue(idStrategy.equals("User1", "user1"));
        extractedMethod35835(idStrategy); // CAP AL
    }

    @Test
    public void testEqualsCaseSensitive() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();
        assertFalse(idStrategy.equals("user1", "User1"));
        assertFalse(idStrategy.equals("User1", "user1"));
        extractedMethod35835(idStrategy); // CAP AL
    }
 // CAP AL
    private void extractedMethod35835(final IdStrategy idStrategy) { // CAP AL
        assertFalse(idStrategy.equals("User1", "user2")); // CAP AL
        String sameUser = "sameUser"; // CAP AL
        assertTrue(idStrategy.equals(sameUser, sameUser)); // CAP AL
    } // CAP AL

    @Test
    public void testEqualsCaseSensitiveEmailAddress() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitiveEmailAddress();
        assertFalse(idStrategy.equals("john.smith@acme.org", "John.Smith@acme.org"));
        assertFalse(idStrategy.equals("john.smith@acme.org", "John.Smith@ACME.org"));
        assertFalse(idStrategy.equals("john.smith@acme.org", "John.Smith@ACME.org"));
        assertFalse(idStrategy.equals("john.smith@acme.org", "John.Smith@acme.ORG"));
        assertFalse(idStrategy.equals("John@smith@acme.org", "john@Smith@acme.ORG"));
        String sameUser = "john.smith@acme.org";
        assertTrue(idStrategy.equals(sameUser, sameUser));
        assertTrue(idStrategy.equals("John.Smith@ACME.org", "John.Smith@acme.org"));
        assertTrue(idStrategy.equals("John.Smith@acme.ORG", "John.Smith@acme.org"));
        assertTrue(idStrategy.equals("john@smith@ACME.org", "john@smith@acme.org"));
    }

    @Test
    public void testKeyForCaseInsensitive() {
        IdStrategy idStrategy = IdStrategy.CASE_INSENSITIVE;
        assertThat(idStrategy.keyFor("user1"), is("user1"));
        assertThat(idStrategy.keyFor("User1"), is("user1"));
        assertThat(idStrategy.keyFor("USER1"), is("user1"));
    }

    @Test
    public void testKeyForCaseSensitive() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();
        assertThat(idStrategy.keyFor("user1"), is("user1"));
        assertThat(idStrategy.keyFor("User1"), is("User1"));
        assertThat(idStrategy.keyFor("USER1"), is("USER1"));
    }

    @Test
    public void testKeyForCaseSensitiveEmailAddress() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitiveEmailAddress();
        assertThat(idStrategy.keyFor("john.smith@acme.org"), is("john.smith@acme.org"));
        assertThat(idStrategy.keyFor("John.Smith@acme.org"), is("John.Smith@acme.org"));
        assertThat(idStrategy.keyFor("John.Smith@ACME.org"), is("John.Smith@acme.org"));
        assertThat(idStrategy.keyFor("John.Smith@acme.ORG"), is("John.Smith@acme.org"));
        assertThat(idStrategy.keyFor("john.smith"), is("john.smith"));
        assertThat(idStrategy.keyFor("John.Smith"), is("John.Smith"));
        assertThat(idStrategy.keyFor("john@smith@acme.org"), is("john@smith@acme.org"));
        assertThat(idStrategy.keyFor("John@Smith@acme.org"), is("John@Smith@acme.org"));
    }

    @Test
    public void testCompareCaseInsensitive() {
        IdStrategy idStrategy = IdStrategy.CASE_INSENSITIVE;
        extractedMethod91407(idStrategy); // CAP AL
        assertTrue(idStrategy.compare("USER2", "user1") > 0);
        assertEquals(0, idStrategy.compare("User1", "user1"));
    }

    @Test
    public void testCompareCaseSensitive() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();
        extractedMethod91407(idStrategy); // CAP AL
        assertTrue(idStrategy.compare("USER2", "user1") < 0);
        assertTrue(idStrategy.compare("User1", "user1") < 0);
    }
 // CAP AL
    private void extractedMethod91407(final IdStrategy idStrategy) { // CAP AL
        assertTrue(idStrategy.compare("user1", "user2") < 0); // CAP AL
        assertTrue(idStrategy.compare("user2", "user1") > 0); // CAP AL
        assertEquals(0, idStrategy.compare("user1", "user1")); // CAP AL
        assertTrue(idStrategy.compare("USER1", "user2") < 0); // CAP AL
    } // CAP AL

    @Test
    public void testCompareCaseSensitiveEmail() {
        IdStrategy idStrategy = new IdStrategy.CaseSensitiveEmailAddress();
        assertEquals(0, idStrategy.compare("john.smith@acme.org", "john.smith@acme.org"));
        assertEquals(0, idStrategy.compare("John.Smith@acme.org", "John.Smith@acme.org"));
        assertEquals(0, idStrategy.compare("John.Smith@ACME.org", "John.Smith@acme.org"));
        assertEquals(0, idStrategy.compare("John.Smith@acme.ORG", "John.Smith@acme.org"));
        assertEquals(0, idStrategy.compare("john.smith", "john.smith"));
        assertEquals(0, idStrategy.compare("John.Smith", "John.Smith"));
        assertEquals(0, idStrategy.compare("john@smith@acme.org", "john@smith@acme.org"));
        assertEquals(0, idStrategy.compare("John@Smith@acme.org", "John@Smith@acme.org"));

        assertTrue(idStrategy.compare("John.Smith@acme.org", "john.smith@acme.org") < 0);
        assertTrue(idStrategy.compare("john.smith@acme.org", "John.Smith@acme.org") > 0);
    }

    @SuppressWarnings("deprecation")
    private void assertRestrictedNames(IdStrategy idStrategy) {
        assertThat(idStrategy.idFromFilename("$002f"), is("/"));

        assertThat(idStrategy.idFromFilename("$002e$002e"), is(".."));
        assertThat(idStrategy.idFromFilename("$0063on"), is("con"));
        assertThat(idStrategy.idFromFilename("$0070rn"), is("prn"));
        assertThat(idStrategy.idFromFilename("$0061ux"), is("aux"));
        assertThat(idStrategy.idFromFilename("$006eul"), is("nul"));
        assertThat(idStrategy.idFromFilename("$0063om1"), is("com1"));
        assertThat(idStrategy.idFromFilename("$0063om2"), is("com2"));
        assertThat(idStrategy.idFromFilename("$0063om3"), is("com3"));
        assertThat(idStrategy.idFromFilename("$0063om4"), is("com4"));
        assertThat(idStrategy.idFromFilename("$0063om5"), is("com5"));
        assertThat(idStrategy.idFromFilename("$0063om6"), is("com6"));
        assertThat(idStrategy.idFromFilename("$0063om7"), is("com7"));
        assertThat(idStrategy.idFromFilename("$0063om8"), is("com8"));
        assertThat(idStrategy.idFromFilename("$0063om9"), is("com9"));
        assertThat(idStrategy.idFromFilename("$006cpt1"), is("lpt1"));
        assertThat(idStrategy.idFromFilename("$006cpt2"), is("lpt2"));
        assertThat(idStrategy.idFromFilename("$006cpt3"), is("lpt3"));
        assertThat(idStrategy.idFromFilename("$006cpt4"), is("lpt4"));
        assertThat(idStrategy.idFromFilename("$006cpt5"), is("lpt5"));
        assertThat(idStrategy.idFromFilename("$006cpt6"), is("lpt6"));
        assertThat(idStrategy.idFromFilename("$006cpt7"), is("lpt7"));
        assertThat(idStrategy.idFromFilename("$006cpt8"), is("lpt8"));
        assertThat(idStrategy.idFromFilename("$006cpt9"), is("lpt9"));
    }

}
