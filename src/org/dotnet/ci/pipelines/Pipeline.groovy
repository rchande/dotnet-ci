package org.dotnet.ci.pipelines;

import jobs.generation.Utilities

// Contains functionality to deal with Jenkins pipelines.
// This class enables us to inform Jenkins about pipelines and set up triggers for those pipeline/parameter combos
// as needed.
class Pipeline {
    private String _pipelineFile
    private String _baseJobName
    PipelineScm _scm

    // Context of the Job DSL to use for creating jobs
    private def _context

    public Pipeline(String baseJobName, String pipelineFile) {
        _pipelineFile = pipelineFile
        _baseJobName = baseJobName
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
    //  pipelineFile - File name relative to root of the repo
    public static Pipeline createPipelineForGithub(String project, String branch, String pipelineFile) {
        String baseName = getDefaultPipelineJobBaseName(pipelineFile)
        return createPipelineForGitHub(project, branch, pipelineFile, baseName)
    }

    // Creates a new pipeline given the pipeline groovy script that
    // will be invoked and a base name for the jobs that will be created
    // Parameters:
    //  pipelineFile - File name relative to root of the repo
    //  baseJobName - Jobs that invoke the pipeline will be created with this base name
    public static Pipeline createPipelineForGitHub(String project, String branch, String pipelineFile, String baseJobName) {
        def newPipeline = new Pipeline(pipelineFile, baseJobName)

        // Create a new source control for the basic setup here
        def sourceControlSettings = SourceControl.createSimpleGitHubSCM(project, branch)
        newPipeline.setSourceControl(sourceControlSettings)
        return newPipeline
    }

    // Creates a new job that triggers based on
    // a Github PR
    public def triggerPipelineOnPR(String context, String triggerPhrase, def parameters = [:]) {
        // Determine the job name
        // Job name is based off the parameters 

        def jobName = getPipelineJobName(_baseJobName, parameters)
        def fullJobName = Utilities.getFullJobName(_baseJobName, true /* is a PR job */)

        // Create the standard pipeline job
        def newJob = createStandardPipelineJob(fullJobName, parameters)
       
        // Set up the source control and triggering
        _scm.emitScmForPR(newJob, _pipelineFile)

        // Set up the triggering
        TriggerBuilder builder = TriggerBuilder.triggerOnPullRequest()
        builder.setGithubContext(context)
    }

    public def triggerPipelineOnPush(def parameters = [:]) {

    }

    public def triggerPipelinePeriodically(String cronString, def parameters = [:]) {

    }

    public def triggerPipelineOnCustom(TriggerBuilder triggerBuilder, def parameters = [:]) {
        // Determine the job name
        // Job name is based off the parameters 

        def jobName = getPipelineJobName(_baseJobName, parameters)
        def fullJobName = Utilities.getFullJobName(_baseJobName, true /* is a PR job */)

        // Create the standard pipeline job
        def newJob = createStandardPipelineJob(fullJobName, parameters)
    }

    private def 

    private def createStandardPipelineJob(String fullJobName, boolean isPR, def parameters = [:]) {
        // Create the new pipeline job
        def newJob = _context.pipelineJob(fullJobName) {            
        }

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