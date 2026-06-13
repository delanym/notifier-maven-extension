package io.github.delanym.maven.notifier.maven;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Captures build metadata from the {@link MavenSession} during session start,
 * making it available later when composing the notification message. Also
 * probes git for branch name and working-tree dirty state.
 */
@NullMarked
public final class BuildContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildContext.class);

    private final List<String> goals;
    private final List<String> selectedProjects;
    private final @Nullable String resumeFrom;
    private final @Nullable String makeBehavior;
    private final File baseDirectory;
    private final int projectCount;
    private final @Nullable String firstProjectName;
    private final @Nullable String gitBranch;
    private final boolean gitDirty;

    private BuildContext(
            List<String> goals,
            List<String> selectedProjects,
            @Nullable String resumeFrom,
            @Nullable String makeBehavior,
            File baseDirectory,
            int projectCount,
            @Nullable String firstProjectName,
            @Nullable String gitBranch,
            boolean gitDirty) {
        this.goals = goals;
        this.selectedProjects = selectedProjects;
        this.resumeFrom = resumeFrom;
        this.makeBehavior = makeBehavior;
        this.baseDirectory = baseDirectory;
        this.projectCount = projectCount;
        this.firstProjectName = firstProjectName;
        this.gitBranch = gitBranch;
        this.gitDirty = gitDirty;
    }

    /**
     * Captures context from the live Maven session. Runs git commands against
     * the base directory to detect branch and dirty state.
     */
    public static BuildContext from(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        File baseDir = request.getMultiModuleProjectDirectory();
        if (baseDir == null) {
            baseDir = new File(request.getBaseDirectory());
        }

        List<String> selected = request.getSelectedProjects();
        int projectCount = session.getProjects().size();
        String firstName = projectCount > 0
                ? session.getProjects().getFirst().getName()
                : null;

        return new BuildContext(
                request.getGoals(),
                selected != null ? selected : List.of(),
                request.getResumeFrom(),
                request.getMakeBehavior(),
                baseDir,
                projectCount,
                firstName,
                detectGitBranch(baseDir),
                detectGitDirty(baseDir)
        );
    }

    public List<String> goals() {
        return goals;
    }

    public boolean hasProjectList() {
        return !selectedProjects.isEmpty();
    }

    public List<String> selectedProjects() {
        return selectedProjects;
    }

    public @Nullable String resumeFrom() {
        return resumeFrom;
    }

    public boolean hasResumeFrom() {
        return resumeFrom != null && !resumeFrom.isBlank();
    }

    public @Nullable String makeBehavior() {
        return makeBehavior;
    }

    public boolean hasMakeBehavior() {
        return makeBehavior != null && !makeBehavior.isBlank();
    }

    public File baseDirectory() {
        return baseDirectory;
    }

    public int projectCount() {
        return projectCount;
    }

    public @Nullable String firstProjectName() {
        return firstProjectName;
    }

    public @Nullable String gitBranch() {
        return gitBranch;
    }

    public boolean gitDirty() {
        return gitDirty;
    }

    private static @Nullable String detectGitBranch(File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
            pb.directory(workDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            return new String(process.getInputStream().readAllBytes()).trim();
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Could not detect git branch: {}", e.getMessage());
            return null;
        }
    }

    private static boolean detectGitDirty(File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
            pb.directory(workDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            if (process.exitValue() != 0) {
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes()).trim();
            return !output.isEmpty();
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Could not detect git dirty state: {}", e.getMessage());
            return false;
        }
    }
}
