/*
 * The MIT License
 *
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import jenkins.model.IdStrategy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

public class UserIdMapperTest {

    @Rule
    public TestName name= new TestName();

    @Test
    public void testNonexistentFileLoads() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
    }

    @Test
    public void testEmptyGet() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        assertThat(mapper.getDirectory("anything"), nullValue());
    }

    @Test
    public void testSimplePutGet() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        assertThat(directory, is(mapper.getDirectory(user1)));
    }

    @Test
    public void testMultiple() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory1 = mapper.putIfAbsent(user1, true);
        String user2 = "user2";
        File directory2 = mapper.putIfAbsent(user2, true);
        String user3 = "user3";
        File directory3 = mapper.putIfAbsent(user3, true);
        extractedMethod60957(directory1, mapper, user1, directory2, user2, directory3, user3); // CAP AL
    }

    @Test
    public void testMultipleSaved() throws IOException {
        File usersDirectory = UserIdMigratorTest.createTestDirectory(getClass(), name);
        IdStrategy idStrategy = IdStrategy.CASE_INSENSITIVE;
        UserIdMapper mapper = new TestUserIdMapper(usersDirectory, idStrategy);
        mapper.init();
        String user1 = "user1";
        File directory1 = mapper.putIfAbsent(user1, true);
        String user2 = "user2";
        File directory2 = mapper.putIfAbsent(user2, true);
        String user3 = "user3";
        File directory3 = mapper.putIfAbsent(user3, true);
        mapper = new TestUserIdMapper(usersDirectory, idStrategy);
        mapper.init();
        extractedMethod60957(directory1, mapper, user1, directory2, user2, directory3, user3); // CAP AL
    }
 // CAP AL
    private void extractedMethod60957(final File directory1, final UserIdMapper mapper, final String user1, final File directory2, final String user2, final File directory3, final String user3) { // CAP AL
        assertThat(directory1, is(mapper.getDirectory(user1))); // CAP AL
        assertThat(directory2, is(mapper.getDirectory(user2))); // CAP AL
        assertThat(directory3, is(mapper.getDirectory(user3))); // CAP AL
    } // CAP AL

    @Test
    public void testRepeatPut() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory1 = mapper.putIfAbsent(user1, true);
        File directory2 = mapper.putIfAbsent(user1, true);
        assertThat(directory1, is(directory2));
    }

    @Test
    public void testIsNotMapped() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        assertThat(mapper.isMapped("anything"), is(false));
    }

    @Test
    public void testIsMapped() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        assertThat(mapper.isMapped(user1), is(true));
    }

    @Test
    public void testInitialUserIds() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        assertThat(mapper.getConvertedUserIds(), empty());
    }

    @Test
    public void testSingleUserIds() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        assertThat(mapper.getConvertedUserIds(), hasSize(1));
        assertThat(mapper.getConvertedUserIds().iterator().next(), is(user1));
    }

    @Test
    public void testMultipleUserIds() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        String user2 = "user2";
        File directory2 = mapper.putIfAbsent(user2, true);
        assertThat(mapper.getConvertedUserIds(), hasSize(2));
        assertThat(mapper.getConvertedUserIds(), hasItems(user1, user2));
    }

    @Test
    public void testRemove() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        mapper.remove(user1);
        assertThat(mapper.isMapped(user1), is(false));
    }

    @Test
    public void testRemoveAfterSave() throws IOException {
        File usersDirectory = UserIdMigratorTest.createTestDirectory(getClass(), name);
        IdStrategy idStrategy = IdStrategy.CASE_INSENSITIVE;
        UserIdMapper mapper = new TestUserIdMapper(usersDirectory, idStrategy);
        mapper.init();
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        mapper = new TestUserIdMapper(usersDirectory, idStrategy);
        mapper.init();
        mapper.remove(user1);
        mapper = new TestUserIdMapper(usersDirectory, idStrategy);
        assertThat(mapper.isMapped(user1), is(false));
    }

    @Test
    public void testPutGetCaseInsensitive() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        assertThat(mapper.getDirectory(user1.toUpperCase()), notNullValue());
    }

    @Test
    public void testPutGetCaseSensitive() throws IOException {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();
        UserIdMapper mapper = createUserIdMapper(idStrategy);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        assertThat(mapper.getDirectory(user1.toUpperCase()), nullValue());
    }

    @Test
    public void testIsMappedCaseInsensitive() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        assertThat(mapper.isMapped(user1.toUpperCase()), is(true));
    }

    @Test
    public void testIsMappedCaseSensitive() throws IOException {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();
        UserIdMapper mapper = createUserIdMapper(idStrategy);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        assertThat(mapper.isMapped(user1.toUpperCase()), is(false));
    }

    @Test
    public void testRemoveCaseInsensitive() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        mapper.remove(user1.toUpperCase());
        assertThat(mapper.isMapped(user1), is(false));
    }

    @Test
    public void testRemoveCaseSensitive() throws IOException {
        IdStrategy idStrategy = new IdStrategy.CaseSensitive();
        UserIdMapper mapper = createUserIdMapper(idStrategy);
        String user1 = "user1";
        File directory = mapper.putIfAbsent(user1, true);
        mapper.remove(user1.toUpperCase());
        assertThat(mapper.isMapped(user1), is(true));
    }

    @Test
    public void testRepeatRemove() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory1 = mapper.putIfAbsent(user1, true);
        mapper.remove(user1);
        mapper.remove(user1);
        assertThat(mapper.isMapped(user1), is(false));
    }

    @Test
    public void testClear() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory1 = mapper.putIfAbsent(user1, true);
        mapper.clear();
        assertThat(mapper.isMapped(user1), is(false));
        assertThat(mapper.getConvertedUserIds(), empty());
    }

    @Test
    public void testReload() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "user1";
        File directory1 = mapper.putIfAbsent(user1, true);
        mapper.reload();
        assertThat(mapper.isMapped(user1), is(true));
        assertThat(mapper.getConvertedUserIds(), hasSize(1));
    }

    @Test
    public void testDirectoryFormatBasic() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "1user";
        File directory1 = mapper.putIfAbsent(user1, true);
        assertThat(directory1.getName(), startsWith(user1 + '_'));
    }

    @Test
    public void testDirectoryFormatLongerUserId() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "muchlongeruserid";
        File directory1 = mapper.putIfAbsent(user1, true);
        assertThat(directory1.getName(), startsWith("muchlongeruser_"));
    }

    @Test
    public void testDirectoryFormatAllSuppressedCharacters() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "!@#$%^";
        File directory1 = mapper.putIfAbsent(user1, true);
        assertThat(directory1.getName(), startsWith("_"));
    }

    @Test
    public void testDirectoryFormatSingleCharacter() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = ".";
        File directory1 = mapper.putIfAbsent(user1, true);
        assertThat(directory1.getName(), startsWith("_"));
    }

    @Test
    public void testDirectoryFormatMixed() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        String user1 = "a$b!c^d~e@f";
        File directory1 = mapper.putIfAbsent(user1, true);
        assertThat(directory1.getName(), startsWith("abcdef_"));
    }

    @Test
    public void testXmlFileCorrupted() throws IOException {
        assertThrows(IOException.class, () -> createUserIdMapper(IdStrategy.CASE_INSENSITIVE));
    }

    @Test
    public void testDuplicatedUserId() throws IOException {
        UserIdMapper mapper = createUserIdMapper(IdStrategy.CASE_INSENSITIVE);
        assertThat(mapper.isMapped("user2"), is(true));
        assertThat(mapper.isMapped("user1"), is(true));
    }

    private UserIdMapper createUserIdMapper(IdStrategy idStrategy) throws IOException {
        File usersDirectory = UserIdMigratorTest.createTestDirectory(getClass(), name);
        TestUserIdMapper mapper = new TestUserIdMapper(usersDirectory, idStrategy);
        mapper.init();
        return mapper;
    }

}
