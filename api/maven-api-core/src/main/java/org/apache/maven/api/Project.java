/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;

/**
 * Interface representing a Maven project which can be created using the
 * {@link org.apache.maven.api.services.ProjectBuilder ProjectBuilder} service.
 * Such objects are immutable and plugin that wish to modify such objects
 * need to do so using the {@link org.apache.maven.api.services.ProjectManager
 * ProjectManager} service.
 * <p>
 * Projects are created using the {@code ProjectBuilder} from a POM file
 * (usually named {@code pom.xml}) on the file system.
 * The {@link #getPomPath()} will point to the POM file and the
 * {@link #getBasedir()} to the directory parent containing the
 * POM file.
 * </p>
 *
 * @since 4.0.0
 * @see org.apache.maven.api.services.ProjectManager
 * @see org.apache.maven.api.services.ProjectBuilder
 */
@Experimental
public interface Project {

    /**
     * {@return the project groupId}.
     */
    @Nonnull
    String getGroupId();

    /**
     * {@return the project artifactId}.
     */
    @Nonnull
    String getArtifactId();

    /**
     * {@return the project version}.
     */
    @Nonnull
    String getVersion();

    /**
     * {@return the project packaging}.
     * <p>
     * Note: unlike in legacy code, logical checks against string representing packaging (returned by this method)
     * are NOT recommended (code like {@code "pom".equals(project.getPackaging)} must be avoided). Use method
     * {@link #getArtifacts()} to gain access to POM or build artifact.
     *
     * @see #getArtifacts()
     */
    @Nonnull
    Packaging getPackaging();

    /**
     * {@return the project language}. It is by default determined by {@link #getPackaging()}.
     *
     * @see #getPackaging()
     */
    @Nonnull
    default Language getLanguage() {
        return getPackaging().language();
    }

    /**
     * {@return the project POM artifact}, which is the artifact of the POM of this project. Every project have a POM
     * artifact, even if the existence of backing POM file is NOT a requirement (i.e. for some transient projects).
     *
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    default ProducedArtifact getPomArtifact() {
        return getArtifacts().get(0);
    }

    /**
     * {@return the project main artifact}, which is the artifact produced by this project build, if applicable.
     * This artifact MAY be absent if the project is actually not producing any main artifact (i.e. "pom" packaging).
     *
     * @see #getPackaging()
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    default Optional<ProducedArtifact> getMainArtifact() {
        List<ProducedArtifact> artifacts = getArtifacts();
        return artifacts.size() == 2 ? Optional.of(artifacts.get(1)) : Optional.empty();
    }

    /**
     * {@return the project artifacts as immutable list}. Elements are the project POM artifact and the artifact
     * produced by this project build, if applicable. Hence, the returned list may have one or two elements
     * (never less than 1, never more than 2), depending on project packaging.
     * <p>
     * The list's first element is ALWAYS the project POM artifact. Presence of second element in the list depends
     * solely on the project packaging.
     *
     * @see #getPackaging()
     * @see #getPomArtifact()
     * @see #getMainArtifact()
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    List<ProducedArtifact> getArtifacts();

    /**
     * {@return the project model}.
     */
    @Nonnull
    Model getModel();

    /**
     * Shorthand method.
     *
     * @return the build element of the project model
     */
    @Nonnull
    default Build getBuild() {
        Build build = getModel().getBuild();
        return build != null ? build : Build.newInstance();
    }

    /**
     * Returns the path to the pom file for this project.
     * A project is usually read from a file named {@code pom.xml},
     * which contains the {@linkplain #getModel() model} in an XML form.
     * When a custom {@code org.apache.maven.api.spi.ModelParser} is used,
     * the path may point to a non XML file.
     * <p>
     * The POM path is also used to define the {@linkplain #getBasedir() base directory}
     * of the project.
     *
     * @return the path of the pom
     * @see #getBasedir()
     */
    @Nonnull
    Path getPomPath();

    /**
     * Returns the project base directory, i.e. the directory containing the project.
     * A project is usually read from the file system and this will point to
     * the directory containing the POM file.
     *
     * @return the path of the directory containing the project
     */
    @Nonnull
    Path getBasedir();

    /**
     * {@return the project direct dependencies (directly specified or inherited)}.
     */
    @Nonnull
    List<DependencyCoordinates> getDependencies();

    /**
     * {@return the project managed dependencies (directly specified or inherited)}.
     */
    @Nonnull
    List<DependencyCoordinates> getManagedDependencies();

    /**
     * {@return the project ID, usable as key}.
     */
    @Nonnull
    default String getId() {
        return getModel().getId();
    }

    /**
     * Returns a boolean indicating if the project is the top level project for
     * this reactor build.  The top level project may be different from the
     * {@code rootDirectory}, especially if a subtree of the project is being
     * built, either because Maven has been launched in a subdirectory or using
     * a {@code -f} option.
     *
     * @return {@code true} if the project is the top level project for this build
     */
    boolean isTopProject();

    /**
     * Returns a boolean indicating if the project is a root project,
     * meaning that the {@link #getRootDirectory()} and {@link #getBasedir()}
     * points to the same directory, and that either {@link Model#isRoot()}
     * is {@code true} or that {@code basedir} contains a {@code .mvn} child
     * directory.
     *
     * @return {@code true} if the project is the root project
     * @see Model#isRoot()
     */
    boolean isRootProject();

    /**
     * Gets the root directory of the project, which is the parent directory
     * containing the {@code .mvn} directory or flagged with {@code root="true"}.
     *
     * @return the root directory of the project
     * @throws IllegalStateException if the root directory could not be found
     * @see Session#getRootDirectory()
     */
    @Nonnull
    Path getRootDirectory();

    /**
     * Returns project parent project, if any.
     * <p>
     * Note that the model may have a parent defined, but an empty parent
     * project may be returned if the parent comes from a remote repository,
     * as a {@code Project} must refer to a buildable project.
     *
     * @return an optional containing the parent project
     * @see Model#getParent()
     */
    @Nonnull
    Optional<Project> getParent();

    /**
     * Returns all profiles defined in this project.
     * <p>
     * This method returns only the profiles defined directly in the current project's POM
     * and does not include profiles from parent projects.
     *
     * @return a non-null, possibly empty list of profiles defined in this project
     * @see Profile
     * @see #getEffectiveProfiles()
     */
    @Nonnull
    List<Profile> getDeclaredProfiles();

    /**
     * Returns all profiles defined in this project and all of its parent projects.
     * <p>
     * This method traverses the parent hierarchy and includes profiles defined in parent POMs.
     * The returned list contains profiles from the current project and all of its ancestors in
     * the project inheritance chain.
     *
     * @return a non-null, possibly empty list of all profiles from this project and its parents
     * @see Profile
     * @see #getDeclaredProfiles()
     */
    @Nonnull
    List<Profile> getEffectiveProfiles();

    /**
     * Returns all active profiles for the current project build.
     * <p>
     * Active profiles are those that have been explicitly activated through one of the following means:
     * <ul>
     *   <li>Command line activation using the -P flag</li>
     *   <li>Maven settings activation in settings.xml via &lt;activeProfiles&gt;</li>
     *   <li>Automatic activation via &lt;activation&gt; conditions</li>
     *   <li>The default active profile (marked with &lt;activeByDefault&gt;true&lt;/activeByDefault&gt;)</li>
     * </ul>
     * <p>
     * The active profiles control various aspects of the build configuration including but not
     * limited to dependencies, plugins, properties, and build resources.
     *
     * @return a non-null, possibly empty list of active profiles for this project
     * @see Profile
     * @see #getEffectiveActiveProfiles()
     */
    @Nonnull
    List<Profile> getDeclaredActiveProfiles();

    /**
     * Returns all active profiles for this project and all of its parent projects.
     * <p>
     * This method traverses the parent hierarchy and collects all active profiles from
     * the current project and its ancestors. Active profiles are those that meet the
     * activation criteria through explicit activation or automatic conditions.
     * <p>
     * The combined set of active profiles from the entire project hierarchy affects
     * the effective build configuration.
     *
     * @return a non-null, possibly empty list of all active profiles from this project and its parents
     * @see Profile
     * @see #getDeclaredActiveProfiles()
     */
    @Nonnull
    List<Profile> getEffectiveActiveProfiles();
}
