/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     
 *
 *******************************************************************************/ 

package hudson.security;

import hudson.cli.CLICommand;
import hudson.cli.CliManagerImpl;
import hudson.cli.ClientAuthenticationCache;
import hudson.cli.LoginCommand;
import hudson.cli.LogoutCommand;
import hudson.model.Hudson;
import org.springframework.security.Authentication;
import org.eclipse.hudson.cli.CLI;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestExtension;
import junit.framework.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/**
 * @author Kohsuke Kawaguchi
 */
public class CliAuthenticationTest extends HudsonTestCase {
    public void test1() throws Exception {
        // dummy security realm that authenticates when username==password
        hudson.setSecurityRealm(createDummySecurityRealm());

        successfulCommand("test","--username","abc","--password","abc");
    }

    private void successfulCommand(String... args) throws Exception {
        assertEquals(0, command(args));
    }

    private int command(String... args) throws Exception {
        return new CLI(getURL()).execute(args);
    }

    @TestExtension
    public static class TestCommand extends CLICommand {
        @Override
        public String getShortDescription() {
            return "test command";
        }

        @Override
        protected int run() throws Exception {
            Authentication auth = Hudson.getAuthentication();
            Assert.assertNotSame(Hudson.ANONYMOUS,auth);
            Assert.assertEquals("abc", auth.getName());
            return 0;
        }
    }

    @TestExtension
    public static class AnonymousCommand extends CLICommand {
        @Override
        public String getShortDescription() {
            return "makes sure that the command is running as anonymous user";
        }

        @Override
        protected int run() throws Exception {
            Authentication auth = Hudson.getAuthentication();
            Assert.assertSame(Hudson.ANONYMOUS,auth);
            return 0;
        }
    }

    @For({LoginCommand.class, LogoutCommand.class, ClientAuthenticationCache.class})
    public void testLogin() throws Exception {
        hudson.setSecurityRealm(createDummySecurityRealm());

        successfulCommand("login","--username","abc","--password","abc");
        successfulCommand("test"); // now we can run without an explicit credential
        successfulCommand("logout");
        successfulCommand("anonymous"); // now we should run as anonymous
    }
}
