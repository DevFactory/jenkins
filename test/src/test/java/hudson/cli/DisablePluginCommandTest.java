
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

package hudson.cli;

import hudson.Functions;
import hudson.PluginWrapper;
import org.apache.commons.lang.StringUtils;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithPlugin;

import java.io.IOException;
import java.util.function.BiPredicate;

import static hudson.cli.CLICommandInvoker.Matcher.failedWith;
import static hudson.cli.CLICommandInvoker.Matcher.succeeded;
import static hudson.cli.DisablePluginCommand.RETURN_CODE_NOT_DISABLED_DEPENDANTS;
import static hudson.cli.DisablePluginCommand.RETURN_CODE_NO_SUCH_PLUGIN;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

public class DisablePluginCommandTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Can disable a plugin with an optional dependent plugin.
     * With strategy none.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"depender-0.0.2.hpi", "dependee-0.0.2.hpi"})
    public void canDisablePluginWithOptionalDependerStrategyNone() {
        assertThat(disablePluginsCLiCommand("-strategy", "NONE", "dependee"), succeeded());
        assertPluginDisabled("dependee");
    }

    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"depender-0.0.2.hpi", "dependee-0.0.2.hpi", "mandatory-depender-0.0.2.hpi"})
    public void canDisablePluginWithDependentsDisabledStrategyNone() throws IOException {
        disablePlugin("mandatory-depender");
        CLICommandInvoker.Result result = disablePluginsCLiCommand("-strategy", "NONE", "dependee");

        assertThat(result, succeeded());
        assertEquals("Disabling only dependee", 1, StringUtils.countMatches(result.stdout(), "Disabling"));
        assertPluginDisabled("dependee");
    }

    /**
     * Can't disable a plugin with a mandatory dependent plugin.
     * With default strategy (none).
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"mandatory-depender-0.0.2.hpi", "dependee-0.0.2.hpi"})
    public void cannotDisablePluginWithMandatoryDependerStrategyNone() {
        assertThat(disablePluginsCLiCommand("dependee"), failedWith(RETURN_CODE_NOT_DISABLED_DEPENDANTS));
        assertPluginEnabled("dependee");
    }

    /**
     * Can't disable a plugin with a mandatory dependent plugin before its dependent plugin.
     * With default strategy (none).
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"mandatory-depender-0.0.2.hpi", "dependee-0.0.2.hpi"})
    public void cannotDisableDependentPluginWrongOrderStrategyNone() {
        assertThat(disablePluginsCLiCommand("dependee", "mandatory-depender"), failedWith(RETURN_CODE_NOT_DISABLED_DEPENDANTS));
        assertPluginDisabled("mandatory-depender");
        assertPluginEnabled("dependee");
    }

    /**
     * Can disable a plugin with a mandatory dependent plugin before its dependent plugin with <i>all/i> strategy
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"mandatory-depender-0.0.2.hpi", "dependee-0.0.2.hpi"})
    public void canDisableDependentPluginWrongOrderStrategyAll() {
        assertThat(disablePluginsCLiCommand("dependee", "mandatory-depender", "-strategy", "all"), succeeded());
        assertPluginDisabled("mandatory-depender");
        assertPluginDisabled("dependee");
    }

    /**
     * Can disable a plugin with a mandatory dependent plugin after being disabled the mandatory dependent plugin. With
     * default strategy (none).
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"mandatory-depender-0.0.2.hpi", "dependee-0.0.2.hpi"})
    public void canDisableDependentPluginsRightOrderStrategyNone() {
        assertThat(disablePluginsCLiCommand("mandatory-depender", "dependee"), succeeded());
        assertPluginDisabled("dependee");
        assertPluginDisabled("mandatory-depender");
    }

    /**
     * Can disable a plugin without dependents plugins and Jenkins restart after it if -restart argument is passed.
     */
    @Ignore("TODO calling restart seems to break Surefire")
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin("dependee-0.0.2.hpi")
    public void restartAfterDisable() {
        assumeNotWindows();
        assertThat(disablePluginsCLiCommand("-restart", "dependee"), succeeded());
        assertPluginDisabled("dependee");
        assertJenkinsInQuietMode();
    }

    /**
     * Can disable a plugin without dependents plugins and Jenkins doesn't restart after it if -restart is not passed.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin("dependee-0.0.2.hpi")
    public void notRestartAfterDisablePluginWithoutArgumentRestart() {
        assertThat(disablePluginsCLiCommand("dependee"), succeeded());
        assertPluginDisabled("dependee");
        assertJenkinsNotInQuietMode();
    }

    /**
     * A non-existing plugin returns with a {@link DisablePluginCommand#RETURN_CODE_NO_SUCH_PLUGIN} status code.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin("dependee-0.0.2.hpi")
    public void returnCodeDisableInvalidPlugin() {
        assertThat(disablePluginsCLiCommand("wrongname"), failedWith(RETURN_CODE_NO_SUCH_PLUGIN));
    }

    /**
     * A plugin already disabled returns 0 and jenkins doesn't restart even though you passed the -restart argument.
     * @throws IOException See {@link PluginWrapper#disable()}.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin("dependee-0.0.2.hpi")
    public void disableAlreadyDisabledPluginNotRestart() throws IOException {
        // Disable before the command call
        disablePlugin("dependee");

        assertPluginDisabled("dependee");
        assertThat(disablePluginsCLiCommand("-restart", "dependee"), succeeded());
        assertPluginDisabled("dependee");
        assertJenkinsNotInQuietMode();
    }

    /**
     * If some plugins are disabled, Jenkins will restart even though the status code isn't 0 (is 16).
     */
    @Ignore("TODO calling restart seems to break Surefire")
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"variant.hpi", "depender-0.0.2.hpi", "mandatory-depender-0.0.2.hpi", "plugin-first.hpi", "dependee-0.0.2.hpi", })
    public void restartAfterDisablePluginsAndErrors() {
        assumeNotWindows();
        assertThat(disablePluginsCLiCommand("-restart", "variant", "dependee", "depender", "plugin-first", "mandatory-depender"), failedWith(RETURN_CODE_NOT_DISABLED_DEPENDANTS));
        assertPluginDisabled("variant");
        assertPluginEnabled("dependee");
        extractedMethod80843(); // CAP AL
        assertJenkinsInQuietMode(); // some plugins were disabled, so it should be restarting
    }

    /**
     * All the dependent plugins, mandatory or optional, are disabled using <i>-strategy all</i>.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"variant.hpi", "depender-0.0.2.hpi", "mandatory-depender-0.0.2.hpi", "plugin-first.hpi", "dependee-0.0.2.hpi", })
    public void disablePluginsStrategyAll() {
        extractedMethod77558(); // CAP AL
        assertThat(disablePluginsCLiCommand("-strategy", "all", "variant", "dependee", "plugin-first"), succeeded());
        assertPluginDisabled("variant");
        assertPluginDisabled("dependee");
        extractedMethod80843(); // CAP AL
    }
 // CAP AL
    private void extractedMethod80843() { // CAP AL
        assertPluginDisabled("depender"); // CAP AL
        assertPluginDisabled("plugin-first"); // CAP AL
        assertPluginDisabled("mandatory-depender"); // CAP AL
    } // CAP AL

    /**
     * Only the mandatory dependent plugins are disabled using <i>-strategy mandatory</i>.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"variant.hpi", "depender-0.0.2.hpi", "mandatory-depender-0.0.2.hpi", "plugin-first.hpi", "dependee-0.0.2.hpi", })
    public void disablePluginsStrategyMandatory() {
        assertThat(disablePluginsCLiCommand("-strategy", "mandatory", "variant", "dependee", "plugin-first"), succeeded());
        assertPluginDisabled("variant");
        assertPluginDisabled("dependee");
        assertPluginEnabled("depender");
        assertPluginDisabled("plugin-first");
        assertPluginDisabled("mandatory-depender");
    }

    /**
     * A plugin already disabled because it's a dependent plugin of one previously disabled appear two times in the log
     * with different messages.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"depender-0.0.2.hpi", "dependee-0.0.2.hpi", })
    public void disablePluginsMessageAlreadyDisabled() {
        CLICommandInvoker.Result result = disablePluginsCLiCommand("-strategy", "all", "dependee", "depender");
        assertThat(result, succeeded());

        assertPluginDisabled("dependee");
        assertPluginDisabled("depender");

        assertTrue("An occurrence of the depender plugin in the log says it was successfully disabled", checkResultWith(result, StringUtils::contains, "depender", PluginWrapper.PluginDisableStatus.DISABLED));
        assertTrue("An occurrence of the depender plugin in the log says it was already disabled", checkResultWith(result, StringUtils::contains, "depender", PluginWrapper.PluginDisableStatus.ALREADY_DISABLED));
    }

    /**
     * The return code is the first error distinct of 0 found during the process. In this case dependent plugins not
     * disabled.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"dependee-0.0.2.hpi", "mandatory-depender-0.0.2.hpi"})
    public void returnCodeFirstErrorIsDependents() {
        CLICommandInvoker.Result result = disablePluginsCLiCommand("dependee", "badplugin");
        assertThat(result, failedWith(RETURN_CODE_NOT_DISABLED_DEPENDANTS));

        assertPluginEnabled("dependee");
    }

    /**
     * The return code is the first error distinct of 0 found during the process. In this case no such plugin.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"dependee-0.0.2.hpi", "mandatory-depender-0.0.2.hpi"})
    public void returnCodeFirstErrorIsNoSuchPlugin() {
        CLICommandInvoker.Result result = disablePluginsCLiCommand("badplugin", "dependee");
        assertThat(result, failedWith(RETURN_CODE_NO_SUCH_PLUGIN));

        assertPluginEnabled("dependee");
    }

    /**
     * In quiet mode, no message is printed if all plugins are disabled or were already disabled.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"depender-0.0.2.hpi", "dependee-0.0.2.hpi", "mandatory-depender-0.0.2.hpi"})
    public void quietModeEmptyOutputSucceed() {
        CLICommandInvoker.Result result = disablePluginsCLiCommand("-strategy", "all", "-quiet", "dependee");
        assertThat(result, succeeded());

        assertPluginDisabled("dependee");
        assertPluginDisabled("depender");
        assertPluginDisabled("mandatory-depender");

        assertTrue("No log in quiet mode if all plugins disabled", StringUtils.isEmpty(result.stdout()));
    }

    /**
     * In quiet mode, only the errors (no such plugin) are printed.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"depender-0.0.2.hpi", "dependee-0.0.2.hpi", "mandatory-depender-0.0.2.hpi"})
    public void quietModeWithErrorNoSuch() {
        CLICommandInvoker.Result result = disablePluginsCLiCommand("-quiet", "-strategy", "all", "dependee", "badplugin");
        assertThat(result, failedWith(RETURN_CODE_NO_SUCH_PLUGIN));

        assertPluginDisabled("dependee");
        assertPluginDisabled("depender");
        assertPluginDisabled("mandatory-depender");

        assertTrue("Only error NO_SUCH_PLUGIN in quiet mode", checkResultWith(result, StringUtils::startsWith, "badplugin", PluginWrapper.PluginDisableStatus.NO_SUCH_PLUGIN));
    }

    /**
     * In quiet mode, only the errors (dependents plugins) are printed.
     */
    @Test
    @Issue("JENKINS-27177")
    @WithPlugin({"depender-0.0.2.hpi", "dependee-0.0.2.hpi", "mandatory-depender-0.0.2.hpi"})
    public void quietModeWithErrorDependents() {
        CLICommandInvoker.Result result = disablePluginsCLiCommand("-quiet", "-strategy", "none", "dependee");
        assertThat(result, failedWith(RETURN_CODE_NOT_DISABLED_DEPENDANTS));

        extractedMethod77558(); // CAP AL

        assertTrue("Only error NOT_DISABLED_DEPENDANTS in quiet mode", checkResultWith(result, StringUtils::startsWith, "dependee", PluginWrapper.PluginDisableStatus.NOT_DISABLED_DEPENDANTS));
    }
 // CAP AL
    private void extractedMethod77558() { // CAP AL
        assertPluginEnabled("dependee"); // CAP AL
        assertPluginEnabled("depender"); // CAP AL
        assertPluginEnabled("mandatory-depender"); // CAP AL
    } // CAP AL

    /**
     * Helper method to check the output of a result with a specific method allowing two arguments (
     * StringUtils::startsWith or StringUtils::contents). This method avoid to have it hardcoded the messages. We avoid
     * having to compose the descriptive text of the message by using a <i>stop</i> flag to ignore the last characters.
     * This method supposes that the descriptive text is at the last of the string.
     * @param result the result of the command.
     * @param method a method with two string arguments to check against
     * @param plugin the plugin printed outSetting the plugin and status as parameters, the method gets
     * the string printed using the
     * @param status the status printed out
     * @return true if the output has been checked against the method using the plugin and status args
     */
    private boolean checkResultWith(CLICommandInvoker.Result result, BiPredicate<String, String> method, String plugin, PluginWrapper.PluginDisableStatus status) {
            String noMatterFollowingChars = "/!$stop";
            String outExpected = Messages.DisablePluginCommand_StatusMessage(plugin, status, noMatterFollowingChars);
            outExpected = StringUtils.substringBefore(outExpected, noMatterFollowingChars);
            return method.test(result.stdout(), outExpected);
    }

    /**
     * Disable a plugin using the {@link PluginWrapper#disable()} method.
     * @param name the name of the plugin.
     * @throws IOException if the disablement cannot be made.
     */
    private void disablePlugin(String name) throws IOException {
        PluginWrapper plugin = j.getPluginManager().getPlugin(name);
        assertThat(plugin, is(notNullValue()));
        plugin.disable();
    }

    /**
     * Disable a list of plugins using the CLI command.
     * @param args Arguments to pass to the command.
     * @return Result of the command. 0 if succeed, 16 if some plugin couldn't be disabled due to dependent plugins.
     */
    private CLICommandInvoker.Result disablePluginsCLiCommand(String... args) {
        return new CLICommandInvoker(j, new DisablePluginCommand()).invokeWithArgs(args);
    }

    private void assertPluginDisabled(String name) {
        PluginWrapper plugin = j.getPluginManager().getPlugin(name);
        assertThat(plugin, is(notNullValue()));
        assertFalse(plugin.isEnabled());
    }

    private void assertPluginEnabled(String name) {
        PluginWrapper plugin = j.getPluginManager().getPlugin(name);
        assertThat(plugin, is(notNullValue()));
        assertTrue(plugin.isEnabled());
    }

    private void assertJenkinsInQuietMode() {
        QuietDownCommandTest.assertJenkinsInQuietMode(j);
    }

    private void assertJenkinsNotInQuietMode() {
        QuietDownCommandTest.assertJenkinsNotInQuietMode(j);
    }

    private void assumeNotWindows() {
        Assume.assumeFalse(Functions.isWindows());
    }
}
