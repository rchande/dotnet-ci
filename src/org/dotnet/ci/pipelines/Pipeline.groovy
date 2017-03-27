package org.dotnet.ci.pipelines;

import jobs.generation.Utilities
import org.dotnet.ci.triggers.GithubTriggerBuilder

// Contains functionality to deal with Jenkins pipelines.
// This class enables us to inform Jenkins about pipelines and set up triggers for those pipeline/parameter combos
// as needed.
class Pipeline {
    private String _pipelineFile
    private String _baseJobName
    PipelineScm _scm

    // Context of the Job DSL to use for creating jobs
    private def _context

    private Pipeline(def context, String baseJobName, String pipelineFile) {
        _pipelineFile = pipelineFile
        _baseJobName = baseJobName
        _context = context
    }

    public setSourceControl(SourceControl sourceControl) {
        _sourceControl = sourceControl
    }

    private static String getDefaultPipelineJobBaseName(String pipelineFile) {
        // Strip off anything after a .
        int lastDot = pipelineFile.indexOf('.');

        if (lastDot != -1) {
            // Has extension
            assert lastDot != 0
            return pipelineFile.substring(0, lastDot + 1)
        }
        else {
            // No extension
            return pipelineFile
        }
    }

    // Determines a full job name for a pipeline job from the base job and parameter set
    // 
    private static String getFullPipelineJobName(String baseJobName, def parameters = [:]) {

    }

    // Creates a new pipeline given the pipeline groovy script that
    // will be invoked.  A base job name is derived from the pipeline file name
    // Parameters:
    //  context - Context used to construct new pipelines.  Pass 'this' from groovy file.
    //  project - GitHub project that the pipeline lives in.
    //  branch - Branch that the project lives in
    //  pipelineFile - File name relative to root of the repo
    public static Pipeline createPipelineForGithub(def context, String project, String branch, String pipelineFile) {
        String baseName = getDefaultPipelineJobBaseName(pipelineFile)
        return createPipelineForGitHub(context, project, branch, pipelineFile, baseName)
    }

    // Creates a new pipeline given the pipeline groovy script that
    // will be invoked and a base name for the jobs that will be created
    // Parameters:
    //  context - Context used to construct new pipelines.  Pass 'this' from groovy file.
    //  project - GitHub project that the pipeline lives in.
    //  branch - Branch that the project lives in
    //  pipelineFile - File name relative to root of the repo
    //  baseJobName - Jobs that invoke the pipeline will be created with this base name
    public static Pipeline createPipelineForGitHub(def context, String project, String branch, String pipelineFile, String baseJobName) {
        def newPipeline = new Pipeline(pipelineFile, baseJobName)

        // Create a new source control for the basic setup here
        def sourceControlSettings = SourceControl.createSimpleGitHubSCM(project, branch)
        newPipeline.setSourceControl(sourceControlSettings)
        return newPipeline
    }

    // Triggers a puipeline on every Github PR.
    // Parameters:
    //  context - The context that appears for the status check in the Github UI
    //  parameter - Optional set of key/value pairs of string parameters that will be passed to the pipeline
    public def triggerPipelineOnEveryGithubPR(String context, def parameters = [:]) {
        // Create the default trigger phrase based on the context
        return triggerPipelineOnPR(context, null, parameters)
    }

    // Triggers a puipeline on every Github PR, with a custom trigger phrase.
    // Parameters:
    //  context - The context that appears for the status check in the Github UI
    //  triggerPhrase - The trigger phrase that can relaunch the pipeline
    //  parameters - Optional set of key/value pairs of string parameters that will be passed to the pipeline
    public def triggerPipelineOnEveryGithubPR(String context, String triggerPhrase, def parameters = [:]) {
        // Determine the job name
        // Job name is based off the parameters 

        def jobName = getPipelineJobName(_baseJobName, parameters)
        def fullJobName = Utilities.getFullJobName(_baseJobName, true /* is a PR job */)

        // Create the standard pipeline job
        def newJob = createStandardPipelineJob(fullJobName, parameters)
       
        // Set up the source control and triggering
        _scm.emitScmForPR(newJob, _pipelineFile)

        // Set up the triggering
        GithubTriggerBuilder builder = GithubTriggerBuilder.triggerOnPullRequest()
        builder.setGithubContext(context)
    }

    // Triggers a pipeline on a Github PR when the specified phrase is commented.
    // Parameters:
    //  context - The context that appears for the status check in the Github UI
    //  triggerPhrase - The trigger phrase that can relaunch the pipeline
    //  parameters - Optional set of key/value pairs of string parameters that will be passed to the pipeline
    public def triggerPipelineOnGithubPRComment(String context, String triggerPhrase, def parameters = [:]) {
        // Create the trigger event and call the helper API
        GithubTriggerBuilder builder = GithubTriggerBuilder.triggerOnPullRequest()
        builder.setGithubContext(context)
        builder.setCustomTriggerPhrase(triggerPhrase)
        builder.triggerOnlyOnComment()
        triggerPipelineOnGithubEvent
    }

    // Triggers a puipeline on a Github PR, using 
    public def triggerPipelineOnGithubPRComment(String context, def parameters = [:]) {
        // Create the default trigger phrase based on the context
        return triggerPipelineOnPR(context, null, parameters)
    }

    public def triggerPipelineOnGithubPush(def parameters = [:]) {

    }

    public def triggerPipelinePeriodically(String cronString, def parameters = [:]) {

    }

    public def triggerPipelineOnGithubEvent(GithubTriggerBuilder triggerBuilder, def parameters = [:]) {
        // Determine the job name
        // Job name is based off the parameters 

        def jobName = getPipelineJobName(_baseJobName, parameters)
        def fullJobName = Utilities.getFullJobName(_baseJobName, true /* is a PR job */)

        // Create the standard pipeline job
        def newJob = createStandardPipelineJob(fullJobName, parameters)
    }

    private def createStandardPipelineJob(String fullJobName, boolean isPR, def parameters = [:]) {
        // Create the new pipeline job
        def newJob = _context.pipelineJob(fullJobName) {}

        // Most options are set up in the pipeline itself.
        // We really only need to set up the retention policy
        Utilities.addRetentionPolicy(newJob, isPR)

        // Disable the job if this is a test generation
        if (GenerationSettings.isTestGeneration()) {
            job.with {
                disabled(true)
            }
        }

        // Return the new job
        return newJob
    }
}