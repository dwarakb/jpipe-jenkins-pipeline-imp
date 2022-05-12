package io.stenic.jpipe.plugin

import io.stenic.jpipe.event.Event
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

class SkipCommitPlugin extends Plugin {

    private Boolean doSkip = false;
    private Boolean isResolved = false;


    public Map getSubscribedEvents() {
        return [
            "${Event.PREPARE}": [
                [{ event -> this.doSkipCommit(event, Event.PREPARE) }, 1],
            ],
            "${Event.BUILD}": [
                [{ event -> this.doSkipCommit(event, Event.BUILD) }, -10000],
            ],
            "${Event.TEST}": [
                [{ event -> this.doSkipCommit(event, Event.TEST) }, -10000],
            ],
            "${Event.PUBLISH}": [
                [{ event -> this.doSkipCommit(event, Event.PUBLISH) }, -10000],
            ],
            "${Event.DEPLOY}": [
                [{ event -> this.doSkipCommit(event, Event.DEPLOY) }, -10000],
            ],
        ]
    }

    public Boolean doSkipCommit(Event event, String stageName) {

        if (!this.isResolved) {
            def commitMsg = event.script.sh(script: "git log -n 1 HEAD", returnStdout: true)
            this.doSkip = commitMsg.matches(/(?ms)(.*\[(skip ci|ci skip)\].*)/)
            this.isResolved = true
        }

        // If the last commit includes [ci skip], do not proceed.
        
        if (this.doSkip) {
            Utils.markStageSkippedForConditional(stageName)
            // event.script.currentBuild.description = 'Skipped by [skip ci]'
            // event.script.currentBuild.result = event.script.currentBuild.getPreviousBuild().result

            // try {
            //     // Try doing a cleanup, required the following methods being approved.
            //     // method hudson.model.Run delete
            //     // method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild
            //     event.script.currentBuild.getRawBuild().delete()
            //     event.script.currentBuild.getRawBuild().setResult(hudson.model.Result.fromString(event.script.currentBuild.getPreviousBuild().result))
            // } catch (RejectedAccessException e) {}

            return true
        }

        return false
    }
}
