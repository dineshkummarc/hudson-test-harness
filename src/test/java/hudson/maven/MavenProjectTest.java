/*******************************************************************************
 *
 * Copyright (c) 2004-2010, Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *   
 *       
 *
 *******************************************************************************/ 

package hudson.maven;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import hudson.tasks.Maven.MavenInstallation;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.File;

import org.eclipse.hudson.legacy.maven.plugin.MavenModuleSet;

/**
 * @author huybrechts
 */
public class MavenProjectTest extends HudsonTestCase {

    public void testOnMaster() throws Exception {
        MavenModuleSet project = createSimpleProject();
        project.setGoals("validate");

        buildAndAssertSuccess(project);
    }

    private MavenModuleSet createSimpleProject() throws Exception {
        return createProject("/simple-projects.zip");
    }

    private MavenModuleSet createProject(final String scmResource) throws Exception {
        MavenModuleSet project = createMavenProject();
        MavenInstallation mi = configureDefaultMaven();
        project.setScm(new ExtractResourceSCM(getClass().getResource(
                scmResource)));
        project.setMaven(mi.getName());
        return project;
    }

    public void testOnSlave() throws Exception {
        MavenModuleSet project = createSimpleProject();
        project.setGoals("validate");
        project.setAssignedLabel(createSlave().getSelfLabel());

        buildAndAssertSuccess(project);
    }

    /**
     * Check if the generated site is linked correctly.
     */
    @Bug(3497)
    public void testSiteBuild() throws Exception {
        MavenModuleSet project = createSimpleProject();
        project.setGoals("site");

        buildAndAssertSuccess(project);

        // this should succeed
        HudsonTestCase.WebClient wc = new WebClient();
        wc.getPage(project,"site");
        try {
            wc.getPage(project,"site/no-such-file");
            fail("should have resulted in 404");
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(404,e.getStatusCode());
        }
    }

    /**
     * Check if the generated site is linked correctly for multi module projects.
     */
    public void testMultiModuleSiteBuild() throws Exception {
        MavenModuleSet project = createProject("maven-multimodule-site.zip");
        project.setGoals("site");

        buildAndAssertSuccess(project);

        // this should succeed
        HudsonTestCase.WebClient wc = new WebClient();
        wc.getPage(project, "site");
        wc.getPage(project, "site/core");
        wc.getPage(project, "site/client");
    }

    /**
     * Check if the the site goal will work when run from a slave.
     */
    @Bug(5943)
    public void testMultiModuleSiteBuildOnSlave() throws Exception {
        MavenModuleSet project = createProject("maven-multimodule-site.zip");
        project.setGoals("site");
        project.setAssignedLabel(createSlave().getSelfLabel());

        buildAndAssertSuccess(project);

        // this should succeed
        HudsonTestCase.WebClient wc = new WebClient();
        wc.getPage(project, "site");
        wc.getPage(project, "site/core");
        wc.getPage(project, "site/client");
    }

    @Bug(6779)
    public void testDeleteSetBuildDeletesModuleBuilds() throws Exception {
        MavenModuleSet project = createProject("maven-multimod.zip");
        project.setGoals("install");
        buildAndAssertSuccess(project);
        buildAndAssertSuccess(project.getModule("org.jvnet.hudson.main.test.multimod:moduleB"));
        buildAndAssertSuccess(project);
        assertEquals(2, project.getBuilds().size()); // Module build does not add a ModuleSetBuild
        project.getFirstBuild().delete();
        // A#1, B#1 and B#2 should all be deleted too
        assertEquals(1, project.getModule("org.jvnet.hudson.main.test.multimod:moduleA").getBuilds().size());
        assertEquals(1, project.getModule("org.jvnet.hudson.main.test.multimod:moduleB").getBuilds().size());
    }
}
