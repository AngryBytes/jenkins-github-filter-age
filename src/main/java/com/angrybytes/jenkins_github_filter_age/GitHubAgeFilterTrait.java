package com.angrybytes.jenkins_github_filter_age;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import jenkins.plugins.git.GitRefSCMHead;
import jenkins.plugins.git.GitTagSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Selection;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSourceRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;

public class GitHubAgeFilterTrait extends SCMSourceTrait {

    /**
     * The maximum age in days.
     */
    private final int maxAgeDays;

    /**
     * Stapler constructor.
     *
     * @param maxAgeDays The maximum age in days.
     */
    @DataBoundConstructor
    public GitHubAgeFilterTrait(int maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }

    /**
     * Gets the maximum age in days.
     *
     * @return The maximum age in days.
     */
    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        context.withFilter(new SCMHeadFilter() {
            @Override
            public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head)
                    throws IOException, InterruptedException {
                GHRepository repo = ((GitHubSCMSourceRequest) request).getRepository();

                String ref;
                if (BranchSCMHead.class.isInstance(head)) {
                  ref = repo.getRef("heads/" + ((BranchSCMHead) head).getName()).getRef();
                } else if (GitTagSCMHead.class.isInstance(head)) {
                  ref = repo.getRef("tags/" + ((GitTagSCMHead) head).getName()).getRef();
                } else if (GitRefSCMHead.class.isInstance(head)) {
                  ref = ((GitRefSCMHead) head).getRef();
                } else {
                  throw new AssertionError("Unrecognized GitHub head");
                }

                Instant commitDate = repo.getCommit(ref).getCommitDate().toInstant();
                return Duration.between(commitDate, Instant.now()).toDays() > getMaxAgeDays();
            }
        });
    }

    /**
     * Our descriptor.
     */
    @Symbol("githubAgeFilter")
    @Extension
    @Selection
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.GitHubAgeFilterTrait_displayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitHubSCMSource.class;
        }

    }

}
