package org.dotnet.ci.pipelines.scm;

interface PipelineScm {
    String getBranch()
    void emitScmForPR(def job)
    void emitScmForNonPR(def job)
}