package org.dotnet.ci.pipelines;

interface PipelineScm {
    void emitScmForPR(def job)
    void emitScmForNonPR(def job)
}